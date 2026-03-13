//package com.hucong.huaicodemake.ai;
//
//import com.hucong.huaicodemake.BaseSpringTest;
//import com.hucong.huaicodemake.ai.model.HtmlCodeResult;
//import jakarta.annotation.Resource;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//
//class AiCodeGeneratorServiceTest extends BaseSpringTest {
//
//    @Resource
//    private AiCodeGeneratorService aiCodeGeneratorService;
//    @Test
//    void generateHtmlCode() {
//        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做一个个人介绍网站，介绍我是程序员hucong,代码行数不超过100行");
//        Assertions.assertNotNull(result);
//    }
//
//    @Test
//    void generateMultiFileCode() {
//        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做一个留言板,代码行数不超过100行");
//        Assertions.assertNotNull(result);
//    }
//}