package com.hucong.huaicodemake.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * @author: hucong
 * 结构化html代码结果
 */
@Description("生成 HTML 代码文件的结果")
@Data
public class HtmlCodeResult {
    /**
     * HTML代码
     */
    @Description("HTML代码")
    private String htmlCode;
    /**
     * 生成代码的描述
     */
    @Description("生成代码的描述")
    private String description;
}

