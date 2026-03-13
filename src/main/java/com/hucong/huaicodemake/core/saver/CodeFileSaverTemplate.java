package com.hucong.huaicodemake.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.hucong.huaicodemake.exception.ErrorCode;
import com.hucong.huaicodemake.exception.ThrowUtils;
import com.hucong.huaicodemake.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文件保存器模板
 */
public abstract class CodeFileSaverTemplate<T> {

    // 文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 模板方法 保存代码标准流程
     *
     * @param result 代码结果对象
     * @return 保存的目录对象
     */
    public final File saveCode(T result) {
        //1 验证输入
        validateInput(result);
        //2 构建唯一目录
        String baseDirPath = buildUniqueDir();
        //3 保存文件（具体实现交给子类）
        saveFiles(result, baseDirPath);
        //4 返回保存的目录对象
        return new File(baseDirPath);
    }

    /**
     * 验证输入 (可允许子类覆写)
     *
     * @param result 输入参数
     */
    protected void validateInput(T result) {
        ThrowUtils.throwIf(result == null, ErrorCode.PARAMS_ERROR, "代码结果不能为空");
    }

    /**
     * 保存文件 交给子类去实现
     *
     * @param result      输入参数
     * @param baseDirPath 根目录路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);

    /**
     * 构建唯一目录路径：tmp/code_output/bizType_雪花ID
     */
    protected String buildUniqueDir() {
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 获取代码类型 交给子类去实现
     */
    protected abstract CodeGenTypeEnum getCodeType();


    /**
     * 写入单个文件的工具类方法
     *
     * @param dirPath  目录路径
     * @param filename 文件名
     * @param content  文件内容
     */
    public void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }
}
