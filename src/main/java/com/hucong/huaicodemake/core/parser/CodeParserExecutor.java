package com.hucong.huaicodemake.core.parser;

import com.hucong.huaicodemake.ai.model.HtmlCodeResult;
import com.hucong.huaicodemake.exception.BusinessException;
import com.hucong.huaicodemake.exception.ErrorCode;
import com.hucong.huaicodemake.model.enums.CodeGenTypeEnum;
/**
 * 解析器执行器
 */
public class CodeParserExecutor {
    public static final HtmlCodeParser htmlCodeResult = new HtmlCodeParser();
    public static final MultiFileCodeParser multiFileCodeResult = new MultiFileCodeParser();

    /**
     * 执行解析器
     * @param codeContent 原始代码内容
     * @param codeGenTypeEnum 代码生成类型
     * @return 解析结果
     */
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenTypeEnum){
        // 根据类型执行对应的解析器
        return switch (codeGenTypeEnum){
            case HTML -> htmlCodeResult.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeResult.parseCode(codeContent);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的生成类型");
        };
    }
}
