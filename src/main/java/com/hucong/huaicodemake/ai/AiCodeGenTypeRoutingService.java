package com.hucong.huaicodemake.ai;

import com.hucong.huaicodemake.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

/**
 * AI 代码生成类型路由服务
 *
 * @author hucong
 * @date 2023/07/05
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 路由代码生成类型(根据用户需求智能选择代码生成类型)
     *
     * @param userMessage 用户消息
     * @return {@link CodeGenTypeEnum}
     */
    @SystemMessage(fromResource = "prompt/routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userMessage);
}
