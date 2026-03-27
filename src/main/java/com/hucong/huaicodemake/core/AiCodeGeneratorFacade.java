package com.hucong.huaicodemake.core;

import cn.hutool.json.JSONUtil;
import com.hucong.huaicodemake.ai.AiCodeGeneratorService;
import com.hucong.huaicodemake.ai.AiCodeGeneratorServiceFactory;
import com.hucong.huaicodemake.ai.model.HtmlCodeResult;
import com.hucong.huaicodemake.ai.model.MultiFileCodeResult;
import com.hucong.huaicodemake.ai.model.message.AiResponseMessage;
import com.hucong.huaicodemake.ai.model.message.ToolExecutedMessage;
import com.hucong.huaicodemake.ai.model.message.ToolRequestMessage;
import com.hucong.huaicodemake.constant.AppConstant;
import com.hucong.huaicodemake.core.builder.VueProjectBuilder;
import com.hucong.huaicodemake.core.parser.CodeParserExecutor;
import com.hucong.huaicodemake.core.saver.CodeFileSaverExecutor;
import com.hucong.huaicodemake.exception.BusinessException;
import com.hucong.huaicodemake.exception.ErrorCode;
import com.hucong.huaicodemake.exception.ThrowUtils;
import com.hucong.huaicodemake.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * 核心代码生成器 采用门面模式
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 生成代码并保存 统一入口
     *
     * @param userMessage     用户输入信息
     * @param codeGenTypeEnum 代码生成类型
     * @param appId           应用id
     * @return 生成的代码文件
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        // 1 获取ai代码生成服务 根据appId获取AiCodeGeneratorService实例
        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 生成代码并保存 统一入口(流式)
     * <p>
     * yield 关键字，用于返回一个流，并继续执行后面的代码
     *
     * @param userMessage     用户输入信息
     * @param codeGenTypeEnum 代码生成类型
     * @param appId           应用id
     * @return 生成的代码文件
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        // 1 获取ai代码生成服务 根据appId获取AiCodeGeneratorService实例
        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                       //同步构建项目 构建 Vue 项目
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath);
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

    /**
     * 批量生成代码并保存 统一入口
     *
     * @param codeStream      代码流
     * @param codeGenTypeEnum 批量生成类型
     * @param appId           应用id
     * @return 生成的代码文件
     */

    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        //字符串拼接器 用于当流式返回所有代码之后，在保存代码
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            //实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            //流式完成后，保存代码
            try {
                String completeCode = codeBuilder.toString();
                //执行解析器解析为对象
                Object parsedCode = CodeParserExecutor.executeParser(completeCode, codeGenTypeEnum);

                //执行保存器保存代码
                File saveDir = CodeFileSaverExecutor.executeSaver(parsedCode, codeGenTypeEnum, appId);
                log.info("文件创建完成，目录为：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存代码失败:{}", e.getMessage());
            }
        });
    }

//    /**
//     * 生成html代码并保存
//     *
//     * @param userMessage 用户输入信息
//     * @return 生成的代码文件
//     */
//
//    private File generateAndSaveHtmlCode(String userMessage) {
//        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
//        return CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
//    }
//
//
//    /**
//     * 生成多个文件代码并保存
//     *
//     * @param userMessage 用户输入信息
//     * @return 生成的代码文件
//     */
//    private File generateAndSaveMultiFileCode(String userMessage) {
//        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
//        return CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
//    }
//
//    /**
//     * 批量多文件生成代码并保存(流式)
//     *
//     * @param userMessage 用户输入信息
//     * @return 生成的代码文件
//     */
//    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
//        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
//        return processCodeStream(result, CodeGenTypeEnum.MULTI_FILE);
//    }
//
//    /**
//     * 批量单个文件生成代码并保存(流式)
//     *
//     * @param userMessage 用户输入信息
//     * @return 生成的代码文件
//     */
//
//    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
//        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
//        return processCodeStream(result, CodeGenTypeEnum.HTML);
//    }


}
