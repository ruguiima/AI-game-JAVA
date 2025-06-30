package com.ruguiima.AIGame.service.impl;

import com.ruguiima.AIGame.model.entity.ChatSession;
import com.ruguiima.AIGame.model.entity.Message;
import com.ruguiima.AIGame.model.entity.User;
import com.ruguiima.AIGame.model.vo.MessageVO;
import com.ruguiima.AIGame.repository.ChatSessionRepository;
import com.ruguiima.AIGame.service.ChatSessionService;

import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private ChatSessionRepository chatSessionRepository;
    
    // 使用内存存储临时会话数据
    private final Map<String, ChatSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<Long, String> userCurrentSessionMap = new ConcurrentHashMap<>(); // 用户ID -> 当前会话ID
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
    public ChatSession createSession(String sessionName, User user) {
        // 创建与用户关联的会话，直接保存到数据库
        ChatSession session = new ChatSession(sessionName, user);
        return chatSessionRepository.save(session);
    }
    
    @Override
    public ChatSession getSession(String sessionId) {
        // 先从内存中查找，如果没有再从数据库查找
        ChatSession session = sessionMap.get(sessionId);
        if (session == null) {
            return chatSessionRepository.findById(sessionId).orElse(null);
        }
        return session;
    }
    
    @Override
    public List<ChatSession> getAllSessions() {
        // 获取内存中的所有会话（临时会话）
        return sessionMap.values().stream()
                .sorted(Comparator.comparing(ChatSession::getLastUpdatedTime).reversed())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ChatSession> getUserSessions(User user) {
        // 从数据库获取用户的所有会话
        if (user == null) {
            return new ArrayList<>();
        }
        return chatSessionRepository.findByUserOrderByLastUpdatedTimeDesc(user);
    }
    
    @Override
    public ChatSession addMessage(String sessionId, String role, String content) {
        ChatSession session;
        
        // 处理临时会话的情况
        if (sessionId == null && tempSession != null) {
            session = tempSession;
        } else if (sessionId != null) {
            session = sessionMap.get(sessionId);
            if (session == null) {
                session = chatSessionRepository.findById(sessionId).orElse(null);
            }
        } else {
            // 创建临时会话
            tempSession = new ChatSession("新对话");
            session = tempSession;
        }
        
        if (session == null) {
            return null;
        }
        
        // 添加消息
        Message message = new Message(role, content);
        session.addMessage(message);
        
        // 如果是用户的第一条消息，将会话从临时状态转为正式状态
        if ("user".equals(role) && !sessionMap.containsValue(session) && session.getUser() == null) {
            // 设置会话名称为用户消息内容（截断）
            String sessionName = content;
            if (sessionName.length() > 20) {
                sessionName = sessionName.substring(0, 20) + "...";
            }
            session.setSessionName(sessionName);
            
            // 添加到会话列表（临时存储，无用户关联）
            sessionMap.put(session.getSessionId(), session);
            currentSessionId = session.getSessionId();
            tempSession = null; // 清除临时会话引用
        } else if (session.getUser() != null) {
            // 有用户关联的会话，保存到数据库
            chatSessionRepository.save(session);
        }
        
        return session;
    }
    
    @Override
    public ChatSession addMessage(String sessionId, String role, String content, User user) {
        if (user == null) {
            return addMessage(sessionId, role, content);
        }
        
        ChatSession session;
        
        // 如果有临时会话且sessionId为null，使用临时会话
        if (sessionId == null && tempSession != null && tempSession.getUser() != null && 
            tempSession.getUser().getId().equals(user.getId())) {
            session = tempSession;
        }
        // 根据会话ID和用户查找会话
        else if (sessionId != null) {
            session = chatSessionRepository.findBySessionIdAndUser(sessionId, user);
        } else {
            // 创建新临时会话
            session = new ChatSession("新对话");
            session.setUser(user);
            tempSession = session;
        }
        
        // 添加消息
        Message message = new Message(role, content);
        session.addMessage(message);
        
        // 如果是用户的第一条消息，设置会话名称并保存到数据库
        if ("user".equals(role)) {
            // 如果是临时会话的第一条用户消息
            if (tempSession == session) {
                String sessionName = content;
                if (sessionName.length() > 20) {
                    sessionName = sessionName.substring(0, 20) + "...";
                }
                session.setSessionName(sessionName);
                
                // 保存到数据库
                session = chatSessionRepository.save(session);
                
                // 清除临时会话引用
                tempSession = null;
            } 
            // 如果是已存在会话的新消息，但会话名称还是默认的
            else if ("新对话".equals(session.getSessionName()) && session.getMessages().size() <= 2) {
                String sessionName = content;
                if (sessionName.length() > 20) {
                    sessionName = sessionName.substring(0, 20) + "...";
                }
                session.setSessionName(sessionName);
            }
            
            // 保存到数据库
            chatSessionRepository.save(session);
        } else if (session.getSessionId() != null) {
            // AI消息，如果会话已经在数据库中，则更新
            chatSessionRepository.save(session);
        }
        
        // 更新当前用户的活动会话
        if (session.getSessionId() != null) {
            userCurrentSessionMap.put(user.getId(), session.getSessionId());
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
        else if (currentSessionId != null) {
            ChatSession session = sessionMap.get(currentSessionId);
            if (session == null) {
                session = chatSessionRepository.findById(currentSessionId).orElse(null);
            }
            if (session != null) {
                return session;
            }
        }
        // 创建新的临时会话
        tempSession = new ChatSession("新对话");
        return tempSession;
    }
    
    @Override
    public ChatSession getCurrentSession(User user) {
        if (user == null) {
            return getCurrentSession();
        }
        
        // 如果有临时会话且与当前用户关联，返回临时会话
        if (tempSession != null && tempSession.getUser() != null && 
            tempSession.getUser().getId().equals(user.getId())) {
            return tempSession;
        }
        
        // 获取用户当前会话ID
        String userSessionId = userCurrentSessionMap.get(user.getId());
        
        if (userSessionId != null) {
            // 查询数据库获取该会话
            ChatSession session = chatSessionRepository.findBySessionIdAndUser(userSessionId, user);
            if (session != null) {
                return session;
            }
        }
        
        // 用户没有当前会话或会话不存在，创建新临时会话
        ChatSession newTempSession = new ChatSession("新对话");
        newTempSession.setUser(user);
        tempSession = newTempSession;
        return tempSession;
    }
    
    @Override
    public ChatSession setCurrentSession(String sessionId) {
        // 设置指定ID的会话为当前会话
        if (sessionMap.containsKey(sessionId)) {
            currentSessionId = sessionId;
            tempSession = null; // 清除临时会话
            return sessionMap.get(sessionId);
        }
        
        // 检查数据库中是否存在该会话
        ChatSession session = chatSessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            currentSessionId = sessionId;
            tempSession = null;
            return session;
        }
        
        return null;
    }
    
    @Override
    public ChatSession setCurrentSession(String sessionId, User user) {
        if (user == null) {
            return setCurrentSession(sessionId);
        }
        
        // 查找用户的会话
        ChatSession session = chatSessionRepository.findBySessionIdAndUser(sessionId, user);
        if (session != null) {
            // 更新用户当前会话ID
            userCurrentSessionMap.put(user.getId(), sessionId);
            return session;
        }
        
        return null;
    }
    
    @Override
    public ChatSession saveSession(ChatSession session) {
        if (session == null) {
            return null;
        }
        
        // 保存到数据库
        return chatSessionRepository.save(session);
    }
    
    @Override
    public void setTempSession(ChatSession session) {
        // 设置临时会话
        this.tempSession = session;
        this.currentSessionId = null; // 表示当前使用的是临时会话
    }
}
