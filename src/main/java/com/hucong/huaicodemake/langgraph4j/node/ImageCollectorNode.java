package com.hucong.huaicodemake.langgraph4j.node;

import com.hucong.huaicodemake.langgraph4j.ai.ImageCollectionPlanService;
import com.hucong.huaicodemake.langgraph4j.ai.ImageCollectionService;
import com.hucong.huaicodemake.langgraph4j.model.ImageCollectionPlan;
import com.hucong.huaicodemake.langgraph4j.model.enums.ImageCategoryEnum;
import com.hucong.huaicodemake.langgraph4j.model.ImageResource;
import com.hucong.huaicodemake.langgraph4j.state.WorkflowContext;
import com.hucong.huaicodemake.langgraph4j.tools.ImageSearchTool;
import com.hucong.huaicodemake.langgraph4j.tools.LogoGeneratorTool;
import com.hucong.huaicodemake.langgraph4j.tools.MermaidDiagramTool;
import com.hucong.huaicodemake.langgraph4j.tools.UndrawIllustrationTool;
import com.hucong.huaicodemake.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片收集节点
 * 使用AI进行工具调用，收集不同类型的图片
 */
@Slf4j
public class ImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            // 从状态中获取工作流上下文，包含整个工作流执行过程中的共享数据
            WorkflowContext context = WorkflowContext.getContext(state);

            // 获取用户的原始需求提示词，后续所有任务都基于此进行
            String originalPrompt = context.getOriginalPrompt();

            // 创建列表用于存储收集到的所有图片资源
            List<ImageResource> collectedImages = new ArrayList<>();

            try {
                // ==================== 第一步：制定图片收集计划 ====================
                // 通过 Spring 容器获取图片收集计划服务，该服务负责分析用户需求制定收集计划
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);

                // 根据原始需求生成图片收集计划，包含多种类型的图片收集任务
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);
                log.info("获取到图片收集计划，开始并发执行");

                // ==================== 第二步：并发执行各种图片收集任务 ====================
                // 创建 CompletableFuture 列表，用于存储所有异步任务的 Future 对象
                List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();

                // ------------------- 并发执行内容图片搜索 -------------------
                // 如果计划中包含内容图片搜索任务，则为每个任务创建异步执行单元
                if (plan.getContentImageTasks() != null) {
                    // 从 Spring 容器获取图片搜索工具，负责在互联网上搜索相关图片
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);

                    // 遍历所有内容搜索任务，为每个任务创建异步执行单元
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                imageSearchTool.searchContentImages(task.query())));
                    }
                }

                // ------------------- 并发执行插画图片搜索 -------------------
                // 如果计划中包含插画搜索任务，则为每个任务创建异步执行单元
                if (plan.getIllustrationTasks() != null) {
                    // 从 Spring 容器获取 undraw 插画搜索工具
                    UndrawIllustrationTool illustrationTool = SpringContextUtil.getBean(UndrawIllustrationTool.class);

                    // 遍历所有插画任务，为每个任务创建异步执行单元
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                illustrationTool.searchIllustrations(task.query())));
                    }
                }

                // ------------------- 并发执行架构图生成 -------------------
                // 如果计划中包含架构图生成任务，则为每个任务创建异步执行单元
                if (plan.getDiagramTasks() != null) {
                    // 从 Spring 容器获取 Mermaid 图表生成工具
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);

                    // 遍历所有架构图任务，为每个任务创建异步执行单元
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                diagramTool.generateMermaidDiagram(task.mermaidCode(), task.description())));
                    }
                }

                // ------------------- 并发执行 Logo 生成 -------------------
                // 如果计划中包含 Logo 生成任务，则为每个任务创建异步执行单元
                if (plan.getLogoTasks() != null) {
                    // 从 Spring 容器获取 Logo 生成工具
                    LogoGeneratorTool logoTool = SpringContextUtil.getBean(LogoGeneratorTool.class);

                    // 遍历所有 Logo 任务，为每个任务创建异步执行单元
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                logoTool.generateLogos(task.description())));
                    }
                }

                // ==================== 第三步：等待所有任务完成并收集结果 ====================
                // 创建组合 Future 对象，等待所有异步任务完成后才继续执行
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));
                allTasks.join();

                // 遍历所有 Future 对象，收集每个任务的执行结果
                for (CompletableFuture<List<ImageResource>> future : futures) {
                    List<ImageResource> images = future.get();
                    if (images != null) {
                        collectedImages.addAll(images);
                    }
                }
                log.info("并发图片收集完成，共收集到 {} 张图片", collectedImages.size());
            } catch (Exception e) {
                // 捕获所有异常，记录错误日志
                log.error("图片收集失败：{}", e.getMessage(), e);
            }

            // ==================== 第四步：更新工作流状态 ====================
            // 设置当前工作流步骤为"图片收集"，用于追踪执行进度
            context.setCurrentStep("图片收集");

            // 将收集到的图片列表保存到上下文中，供后续节点使用
            context.setImageList(collectedImages);

            // 保存更新后的上下文到状态中，返回给下一个节点使用
            return WorkflowContext.saveContext(context);
        });
    }
}


