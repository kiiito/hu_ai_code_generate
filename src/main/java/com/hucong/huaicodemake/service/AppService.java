package com.hucong.huaicodemake.service;

import com.hucong.huaicodemake.model.dto.app.AppQueryRequest;
import com.hucong.huaicodemake.model.entity.User;
import com.hucong.huaicodemake.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.hucong.huaicodemake.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/kiiito">程序员hucong</a>
 */
public interface AppService extends IService<App> {

    /**
     * 异步生成应用截图
     *
     * @param appId        应用id
     * @param appDeployUrl 应用部署地址
     */
    void generateScreenshotAsync(Long appId, String appDeployUrl);

    /**
     * 获取应用信息 脱敏
     *
     * @param app 应用
     * @return 应用信息
     */
    AppVO getAppVO(App app);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 应用查询条件
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取应用信息列表 脱敏
     *
     * @param appList 应用列表
     * @return 应用信息列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 获取应用信息列表 脱敏
     *
     * @param appId 应用id
     * @return 应用信息列表
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     *
     * @param appId 应用id
     * @return 应用信息列表
     */
    String deployApp(Long appId, User loginUser);
}
