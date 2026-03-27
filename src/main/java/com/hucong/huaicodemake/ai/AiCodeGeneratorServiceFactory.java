package com.hucong.huaicodemake.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hucong.huaicodemake.ai.guardrail.PromptSafetyInputGuardrail;
import com.hucong.huaicodemake.ai.tools.*;
import com.hucong.huaicodemake.exception.BusinessException;
import com.hucong.huaicodemake.exception.ErrorCode;
import com.hucong.huaicodemake.model.enums.CodeGenTypeEnum;
import com.hucong.huaicodemake.service.ChatHistoryService;
import com.hucong.huaicodemake.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {
    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;


    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;


    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，cacheKey: {}, 原因: {}", key, cause);
            })
            .build();


    /**
     * 根据appId进行创建不同的AiCodeGeneratorService实例(为了兼容老应用，直接定义为HTML)
     *
     * @param appId 应用id
     * @return AiCodeGeneratorService实例
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        // 从缓存中获取实例 如果appId没有，则调用创建方法createAiCodeGeneratorService方法创建实例并缓存
        //return serviceCache.get(appId, this::createAiCodeGeneratorService);
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        // 从缓存中获取实例 如果appId没有，则调用创建方法createAiCodeGeneratorService方法创建实例并缓存
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 创建AiCodeGeneratorService实例
     *
     * @param appId 应用id
     * @return AiCodeGeneratorService实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("创建AI服务实例，appId: {}", appId);
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        //从数据库中加载历史对话到对话记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        return switch (codeGenType) {
            // VUE_PROJECT 类型：构建支持 Vue 项目生成的 AI 服务实例
            // 使用推理型流式聊天模型，支持文件写入工具，并处理幻觉工具调用
            case VUE_PROJECT -> {
                //使用多例模式的streamingChatModel解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
               yield  AiServices.builder(AiCodeGeneratorService.class)
                        // 配置标准聊天模型，用于非流式响应
                        .chatModel(chatModel)
                        // 配置推理型流式聊天模型，支持实时输出和深度推理能力
                        .streamingChatModel(reasoningStreamingChatModel)
                        // 提供对话记忆管理器，为每个 memoryId 返回对应的 chatMemory 实例
                        // 用于维护多轮对话的上下文信息
                        .chatMemoryProvider(memoryId -> chatMemory)
                        // 注册文件写入工具，允许 AI 调用该工具将生成的代码保存到文件系统
                        .tools(toolManager.getAllTool())
                        // 配置幻觉工具名称策略：当 AI 错误地调用了不存在的工具时
                        // 返回一个错误提示消息，告知 AI 该工具不存在
                        // toolExecutionRequest 包含工具执行请求的详细信息
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "Error: there is no tool called " + toolExecutionRequest.name()))
                       .inputGuardrails(new PromptSafetyInputGuardrail())//添加提示词护轨
                        // 构建并返回配置完成的 AI 服务实例
                        .build();
            }
            case HTML, MULTI_FILE -> {
                //使用多例模式的streamingChatModel解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
               yield  AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                       .inputGuardrails(new PromptSafetyInputGuardrail())//添加提示词护轨
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型" + codeGenType.getValue());
        };
    }


    /**
     * 创建AiCodeGeneratorService实例
     *
     * @return AiCodeGeneratorService实例
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }

    /**
     * 构建缓存key
     *
     * @param appId       应用id
     * @param codeGenType 生成类型
     * @return 缓存key
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }
}
