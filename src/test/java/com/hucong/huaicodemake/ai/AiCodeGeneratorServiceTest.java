package com.hucong.huaicodemake.ai;

import com.hucong.huaicodemake.BaseSpringTest;
import com.hucong.huaicodemake.ai.model.HtmlCodeResult;
import com.hucong.huaicodemake.core.AiCodeGeneratorFacade;
import com.hucong.huaicodemake.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

class AiCodeGeneratorServiceTest extends BaseSpringTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
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

//    @Test
//    void generateVueProjectCodeStream() {
//        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
//                "简单的任务记录网站，总代码量不超过 200 行",
//                CodeGenTypeEnum.VUE_PROJECT, 1L);
//        // 阻塞等待所有数据收集完成
//        List<String> result = codeStream.collectList().block();
//        // 验证结果
//        Assertions.assertNotNull(result);
//        String completeContent = String.join("", result);
//        Assertions.assertNotNull(completeContent);
//    }

}