package com.hucong.huaicodemake.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.hucong.huaicodemake.constant.AppConstant;
import com.hucong.huaicodemake.core.AiCodeGeneratorFacade;
import com.hucong.huaicodemake.exception.BusinessException;
import com.hucong.huaicodemake.exception.ErrorCode;
import com.hucong.huaicodemake.exception.ThrowUtils;
import com.hucong.huaicodemake.model.dto.app.AppQueryRequest;
import com.hucong.huaicodemake.model.entity.User;
import com.hucong.huaicodemake.model.enums.ChatHistoryMessageTypeEnum;
import com.hucong.huaicodemake.model.enums.CodeGenTypeEnum;
import com.hucong.huaicodemake.model.vo.AppVO;
import com.hucong.huaicodemake.model.vo.UserVO;
import com.hucong.huaicodemake.service.ChatHistoryService;
import com.hucong.huaicodemake.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.hucong.huaicodemake.model.entity.App;
import com.hucong.huaicodemake.mapper.AppMapper;
import com.hucong.huaicodemake.service.AppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/kiiito">程序员hucong</a>
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 获取应用列表 脱敏后的应用信息
     *
     * @param appId 应用id
     * @return List<AppVO>
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id错误");
        ThrowUtils.throwIf(StrUtil.isBlank( message), ErrorCode.PARAMS_ERROR, "请输入内容");
        // 2 获取应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3 权限校验 是否是当前登录用户的应用
        if (!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户没有权限");
        }
        // 4 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        // 5 调用ai生成代码之前保存用户发送的信息
        chatHistoryService.addChatMessage(appId,message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6 调用ai生成流式代码
        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 7 收集AI响应的内容，并完成后保存到数据库
        StringBuffer aiResponseBuffer = new StringBuffer();
       return contentFlux.map(chunk->{
            //实时AI响应收集内容
            aiResponseBuffer.append(chunk);
            return chunk;
        }).doOnComplete(()->{
            //流式返回后，保存AI消息到数据库中
            String aiResponse  = aiResponseBuffer.toString();
            chatHistoryService.addChatMessage(appId,aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        }).doOnError(error->{
            //如果流式返回错误，也要保存AI失败消息到数据库中
            chatHistoryService.addChatMessage(appId,error.getMessage(), ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        });
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3 权限校验 仅本人能部署自己的应用
        if (!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户没有权限");
        }
        // 4 检查是否已经有 deployKey
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)){
            deployKey = RandomUtil.randomString(6);
        }
        // 5 获取代码生成类型 获取原始代码生成路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.SYSTEM_ERROR, "代码生成路径不存在，请先生成应用");
        // 7 复制文件路径到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try{
            FileUtil.copyContent(sourceDir,new File(deployDirPath), true);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }
        // 8 更新数据库
        App updateApp  = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 9 返回可访问的URL 地址
        return String.format("%s/%s",AppConstant.CODE_DEPLOY_HOST,deployKey);
    }


    /**
     * 获取单条数据 脱敏后的应用信息
     *
     * @param app
     * @return AppVO
     */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 应用查询条件
     * @return QueryWrapper 查询条件
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 获取应用列表 脱敏后的应用信息
     *
     * @param appList 应用列表
     * @return List<AppVO>
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean removeById(Serializable id) {
        //校验
        if (id == null){
            return false;
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0){
            return false;
        }
        //先删除关联的对话历史
        try{
            chatHistoryService.deleteByAppId(appId);
        }catch (Exception e){
            log.error("删除应用关联的对话历史失败：" + e.getMessage());
        }
        //删除应用
        return super.removeById(appId);
    }

}
