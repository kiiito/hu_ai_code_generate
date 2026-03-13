//package com.hucong.huaicodemake.core;
//
//import com.hucong.huaicodemake.BaseSpringTest;
//import com.hucong.huaicodemake.model.enums.CodeGenTypeEnum;
//import jakarta.annotation.Resource;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import reactor.core.publisher.Flux;
//
//import java.io.File;
//import java.util.List;
//
//class AiCodeGeneratorFacadeTest extends BaseSpringTest {
//
//    @Resource
//    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
//    @Test
//    void generateAndSaveCode() {
//        File file = aiCodeGeneratorFacade.generateAndSaveCode("给我生成一个登录页面，代码50行", CodeGenTypeEnum.MULTI_FILE);
//        Assertions.assertNotNull(file);
//    }
//
//    @Test
//    void generateAndSaveCodeStream() {
//        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("给我生成一个简单的手风琴动漫风格的图片展示网页", CodeGenTypeEnum.HTML);
//        // 阻塞等待所有数据收集完成
//        List<String> result = codeStream.collectList().block();
//        // 验证结果
//        Assertions.assertNotNull(result);
//        String completeContent = String.join("", result);
//        Assertions.assertNotNull(completeContent);
//    }
//
//}