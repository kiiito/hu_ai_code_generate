package com.hucong.huaicodemake.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class VueProjectBuilder {


    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return false;
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return false;
        }
        log.info("开始构建 Vue 项目: {}", projectPath);
        // 执行 npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 执行失败");
            return false;
        }
        // 执行 npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 执行失败");
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }

    /**
     * 异步构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     */
    public void buildProjectAsync(String projectPath) {
        // 使用 Java 虚拟线程（Virtual Thread）创建异步任务来执行构建操作
        // 虚拟线程是轻量级线程，适合执行大量异步任务，不会阻塞平台线程
        Thread.ofVirtual()
                // 为线程命名，使用时间戳作为后缀以确保唯一性，便于日志追踪和调试
                .name("vue-builder-" + System.currentTimeMillis())
                // 启动虚拟线程并执行构建任务
                .start(() -> {
                    try {
                        // 调用同步构建方法执行 Vue 项目的完整构建流程
                        // 包括：检查项目目录、执行 npm install、执行 npm run build、验证 dist 目录生成
                        buildProject(projectPath);
                    } catch (Exception e) {
                        // 捕获构建过程中可能抛出的所有异常，避免异常传播导致线程意外终止
                        // 记录错误日志，包含异常信息以便排查问题
                        log.error("异步构建 vue 项目异常：{}", e.getMessage());
                    }
                });
    }



    /**
     * 执行 npm install 命令
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300); // 5分钟超时
    }

    /**
     * 执行 npm run build 命令
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180); // 3分钟超时
    }


    /**
     * 判断当前操作系统是否为 Windows
     *
     * @return true 表示 Windows，false 表示其他操作系统
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * 构建命令
     *
     * @param baseCommand 基础命令
     * @return 构建后的命令
     */
    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }


    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );

            // 使用线程等待进程完成，设置超时
            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;

            while (process.isAlive()) {
                if (System.currentTimeMillis() - startTime > timeoutMillis) {
                    log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                    process.destroyForcibly();
                    return false;
                }
                Thread.sleep(100); // 每100毫秒检查一次
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }


}
