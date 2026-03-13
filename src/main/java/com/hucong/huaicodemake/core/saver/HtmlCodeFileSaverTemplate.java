package com.hucong.huaicodemake.core.saver;

import com.hucong.huaicodemake.ai.model.HtmlCodeResult;
import com.hucong.huaicodemake.exception.ErrorCode;
import com.hucong.huaicodemake.exception.ThrowUtils;
import com.hucong.huaicodemake.model.enums.CodeGenTypeEnum;

public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    /**
     * HTML代码保存模板
     */
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    /**
     * 验证输入参数
     *
     * @param result 输入参数
     */
    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        ThrowUtils.throwIf(result.getHtmlCode() == null, ErrorCode.SYSTEM_ERROR, "HTML代码不能为空");
    }

    /**
     * 保存文件
     *
     * @param result      输入参数
     * @param baseDirPath 根目录路径
     */
    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }


}
