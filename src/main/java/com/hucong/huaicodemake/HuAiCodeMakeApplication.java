package com.hucong.huaicodemake;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
@MapperScan("com.hucong.huaicodemake.mapper")
public class HuAiCodeMakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HuAiCodeMakeApplication.class, args);
    }

}
