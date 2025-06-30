package com.ruguiima.AIGame.service;

import com.ruguiima.AIGame.model.entity.ChatSession;

public interface ChatService {
    /**
     * 处理单条用户消息，不考虑聊天上下文
     */
    String getMessage(String userMessage);
    
    /**
     * 处理基于会话的用户消息，考虑历史聊天记录
     */
    String getMessage(ChatSession session, String userMessage);
}
