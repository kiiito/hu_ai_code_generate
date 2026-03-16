package com.hucong.huaicodemake.service;

import com.hucong.huaicodemake.model.dto.chathistory.ChatHistoryQueryRequest;
import com.hucong.huaicodemake.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.hucong.huaicodemake.model.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/kiiito">程序员hucong</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话消息
     *
     * @param appId       应用 id
     * @param message     消息内容
     * @param messageType 消息类型
     * @param UserId      用户id
     * @return 是否添加成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long UserId);

    /**
     * 分页获取应用下的对话消息
     *
     * @param appId          应用 id
     * @param pageSize       每页大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser      登录用户
     * @return 对话消息列表
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 加载应用下的对话消息到内存
     *
     * @param appId      应用 id
     * @param chatMemory 聊天内存
     * @param maxCount   最大数量
     * @return 加载数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 根据应用id删除对话消息
     *
     * @param appId 应用id
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 获取查询条件
     *
     * @param chatHistoryQueryRequest 对话历史查询条件
     * @return QueryWrapper 查询条件
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
