package com.ruguiima.AIGame.service;

import com.ruguiima.AIGame.model.entity.ChatSession;
import com.ruguiima.AIGame.model.vo.MessageVO;

import java.util.List;

public interface ChatSessionService {
    /**
     * 创建新的聊天会话
     * @param sessionName 会话名称
     * @return 新创建的会话
     */
    ChatSession createSession(String sessionName);
    
    /**
     * 根据会话ID获取会话
     * @param sessionId 会话ID
     * @return 会话对象，如果不存在返回null
     */
    ChatSession getSession(String sessionId);
    
    /**
     * 获取所有会话列表
     * @return 所有会话列表
     */
    List<ChatSession> getAllSessions();
    
    /**
     * 向指定会话添加消息
     * @param sessionId 会话ID
     * @param role 发送者角色（user或ai）
     * @param content 消息内容
     * @return 更新后的会话
     */
    ChatSession addMessage(String sessionId, String role, String content);
    
    /**
     * 将会话中的消息转换为前端显示用的VO对象列表
     * @param session 会话对象
     * @return MessageVO列表
     */
    List<MessageVO> convertToMessageVOList(ChatSession session);
    
    /**
     * 获取当前活动会话，如果没有则创建一个新会话
     * @return 当前活动会话
     */
    ChatSession getCurrentSession();
    
    /**
     * 设置当前活动会话
     * @param sessionId 会话ID
     * @return 设置的活动会话
     */
    ChatSession setCurrentSession(String sessionId);
}
