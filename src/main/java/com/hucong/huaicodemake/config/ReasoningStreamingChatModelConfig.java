package com.hucong.huaicodemake.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;
    private String apiKey;


    public StreamingChatModel reasoningStreamingChatModel(){
        //为了测试方便 临时选择快速模型
        final String modelName = "deepseek-chat";
        final int maxTokens = 8192;

        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(true)
                .logRequests(true)
                .build();

    }
}
