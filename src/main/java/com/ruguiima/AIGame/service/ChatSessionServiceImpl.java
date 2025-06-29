package com.ruguiima.AIGame.service;

import com.ruguiima.AIGame.model.entity.ChatSession;
import com.ruguiima.AIGame.model.entity.Message;
import com.ruguiima.AIGame.model.vo.MessageVO;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {
    
    // 使用内存存储会话数据（在实际应用中可能需要持久化到数据库）
    private final Map<String, ChatSession> sessionMap = new ConcurrentHashMap<>();
    private String currentSessionId;
    private ChatSession tempSession; // 临时会话，未添加到会话列表中
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @Override
    public ChatSession createSession(String sessionName) {
        // 创建临时会话，不加入到会话列表中
        tempSession = new ChatSession(sessionName);
        currentSessionId = null; // 表示当前使用临时会话
        return tempSession;
    }
    
    @Override
    public ChatSession getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }
    
    @Override
    public List<ChatSession> getAllSessions() {
        return sessionMap.values().stream()
                .sorted(Comparator.comparing(ChatSession::getLastUpdatedTime).reversed())
                .collect(Collectors.toList());
    }
    
    @Override
    public ChatSession addMessage(String sessionId, String role, String content) {
        ChatSession session;
        
        // 处理临时会话的情况
        if (sessionId == null && tempSession != null) {
            session = tempSession;
        } else if (sessionId != null) {
            session = sessionMap.get(sessionId);
        } else {
            // 创建临时会话
            tempSession = new ChatSession("新对话");
            session = tempSession;
        }
        
        // 添加消息
        Message message = new Message(role, content);
        session.addMessage(message);
        
        // 如果是用户的第一条消息，将会话从临时状态转为正式状态（添加到会话列表）
        if ("user".equals(role) && !sessionMap.containsValue(session)) {
            // 设置会话名称为用户消息内容（截断）
            String sessionName = content;
            if (sessionName.length() > 20) {
                sessionName = sessionName.substring(0, 20) + "...";
            }
            session.setSessionName(sessionName);
            
            // 添加到会话列表
            sessionMap.put(session.getSessionId(), session);
            currentSessionId = session.getSessionId();
            tempSession = null; // 清除临时会话引用
        }
        
        return session;
    }
    
    @Override
    public List<MessageVO> convertToMessageVOList(ChatSession session) {
        if (session == null || session.getMessages() == null) {
            return new ArrayList<>();
        }
        
        return session.getMessages().stream()
                .map(message -> new MessageVO(
                        message.getRole(),
                        message.getContent(),
                        message.getTime().format(TIME_FORMATTER)))
                .collect(Collectors.toList());
    }
    
    @Override
    public ChatSession getCurrentSession() {
        // 如果有临时会话，返回临时会话
        if (currentSessionId == null && tempSession != null) {
            return tempSession;
        } 
        // 如果有当前会话ID且存在于会话列表中，返回该会话
        else if (currentSessionId != null && sessionMap.containsKey(currentSessionId)) {
            return sessionMap.get(currentSessionId);
        } 
        // 创建新的临时会话
        else {
            tempSession = new ChatSession("新对话");
            return tempSession;
        }
    }
    
    @Override
    public ChatSession setCurrentSession(String sessionId) {
        // 设置指定ID的会话为当前会话
        if (sessionMap.containsKey(sessionId)) {
            currentSessionId = sessionId;
            tempSession = null; // 清除临时会话
            return sessionMap.get(sessionId);
        }
        return null;
    }
}
