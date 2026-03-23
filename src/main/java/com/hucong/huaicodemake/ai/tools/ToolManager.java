package com.hucong.huaicodemake.ai.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ToolManager {
    /**
     * 工具映射表
     */
    private final Map<String, BaseTool> toolMap = new HashMap<>();
    /**
     * 工具列表
     */
    @Resource
    private BaseTool[] tools;

    /**
     * 初始化工具列表
     */
    @PostConstruct
    public void initTools() {
        for(BaseTool tool : tools){
            toolMap.put(tool.getToolName(), tool);
            log.info("注册工具：{}->{}", tool.getToolName(), tool.getDisplayName());
        }
        log.info("工具管理器初始化完成，共注册 {} 个工具", toolMap.size());
    }

    /**
     * 根据工具名称获取工具
     * @param toolName 工具名称
     * @return 工具
     */
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    /**
     * 获取所有工具
     * @return 所有工具
     */
    public BaseTool[] getAllTool() {
        return tools;
    }
}
