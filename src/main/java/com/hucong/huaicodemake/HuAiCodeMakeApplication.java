package com.hucong.huaicodemake;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching//开启缓存
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.hucong.huaicodemake.mapper")
public class HuAiCodeMakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HuAiCodeMakeApplication.class, args);
    }

}
