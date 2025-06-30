package com.ruguiima.AIGame.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruguiima.AIGame.config.DeepSeekConfig;
import com.ruguiima.AIGame.model.dto.ChatMessage;
import com.ruguiima.AIGame.model.entity.ChatSession;
import com.ruguiima.AIGame.model.entity.Message;
import com.ruguiima.AIGame.service.ChatService;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    
    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private DeepSeekConfig config;
    
    @Override
    public String getMessage(String userMessage) {
        try {
            log.debug("接收用户消息: {}", userMessage);
            
            // 创建简单单轮对话
            List<ChatMessage> messages = new ArrayList<>();
            
            // 添加系统消息
            messages.add(new ChatMessage("system", config.getSystemMessage()));
            
            // 添加用户消息
            messages.add(new ChatMessage("user", userMessage));
            
            return deepSeekService.createChatCompletion(messages);
        } catch (Exception e) {
            log.error("调用DeepSeek API出错", e);
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }
    
    /**
     * 基于聊天会话调用DeepSeek API
     * @param session 聊天会话，包含历史消息
     * @param userMessage 用户最新消息
     * @return AI回复内容
     */
    public String getMessage(ChatSession session, String userMessage) {
        try {
            log.debug("处理会话消息，会话ID: {}, 用户消息: {}", 
                    session.getSessionId(), userMessage);
            
            List<ChatMessage> messages = new ArrayList<>();
            
            // 添加系统消息
            messages.add(new ChatMessage("system", config.getSystemMessage()));
            
            // 转换历史消息并添加到请求中，限制历史消息数量
            List<Message> sessionMessages = session.getMessages();
            int historySize = sessionMessages.size();
            int startIdx = Math.max(0, historySize - config.getMaxMessages());
            
            // 如果有历史消息，添加到请求
            if (historySize > 0) {
                List<Message> recentMessages = sessionMessages.subList(startIdx, historySize);
                
                for (Message msg : recentMessages) {
                    ChatMessage chatMsg = ChatMessage.fromMessage(msg);
                    messages.add(chatMsg);
                }
            }
            
            // 如果最后一条不是用户的消息，添加当前用户消息
            if (historySize == 0 || !sessionMessages.get(historySize-1).getRole().equals("user")) {
                messages.add(new ChatMessage("user", userMessage));
            }
            
            return deepSeekService.createChatCompletion(messages);
        } catch (Exception e) {
            log.error("处理会话消息出错", e);
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }
    

}
