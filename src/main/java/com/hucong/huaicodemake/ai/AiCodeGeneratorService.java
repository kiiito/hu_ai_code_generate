package com.hucong.huaicodemake.ai;

import com.hucong.huaicodemake.ai.model.HtmlCodeResult;
import com.hucong.huaicodemake.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * @author: hucong
 * @time: 2023/10/23
 **/
public interface AiCodeGeneratorService {
    /**
     * 生成html代码
     *
     * @param userMessage 用户输入信息
     * @return html代码
     * @SystemMessage 系统提示信息
     */
    @SystemMessage(fromResource = "prompt/html-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多个文件代码
     *
     * @param userMessage 用户输入信息
     * @return 多个文件代码
     */
    @SystemMessage(fromResource = "prompt/multi-file-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);


    /**
     * 生成html代码 流式代码输出
     *
     * @param userMessage 用户输入信息
     * @return html代码
     * @SystemMessage 系统提示信息
     */
    @SystemMessage(fromResource = "prompt/html-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多个文件代码 流式代码输出
     *
     * @param userMessage 用户输入信息
     * @return 多个文件代码
     */
    @SystemMessage(fromResource = "prompt/multi-file-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

    /**
     * 创建Vue项目代码 流式代码输出
     *
     * @param appId       应用id
     * @param userMessage 用户输入信息
     * @return 创建Vue项目代码
     */
    @SystemMessage(fromResource = "prompt/vue-project-prompt.txt")
    Flux<String> generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}
