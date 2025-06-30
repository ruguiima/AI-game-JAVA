package com.ruguiima.AIGame.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ruguiima.AIGame.converter.MessageListConverter;

@Entity
@Table(name = "chat_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {
    @Id
    private String sessionId;
    
    private String sessionName;
    
    @Column(name = "created_time")
    private LocalDateTime createdTime;
    
    @Column(name = "last_updated_time")
    private LocalDateTime lastUpdatedTime;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = MessageListConverter.class)
    private List<Message> messages;
    
    public ChatSession(String sessionName) {
        this.sessionId = UUID.randomUUID().toString();
        this.sessionName = sessionName;
        this.createdTime = LocalDateTime.now();
        this.lastUpdatedTime = LocalDateTime.now();
        this.messages = new ArrayList<>();
        this.user = null; // 未指定用户
    }
    
    public ChatSession(String sessionName, User user) {
        this.sessionId = UUID.randomUUID().toString();
        this.sessionName = sessionName;
        this.createdTime = LocalDateTime.now();
        this.lastUpdatedTime = LocalDateTime.now();
        this.messages = new ArrayList<>();
        this.user = user;
    }
    
    public void addMessage(Message message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        this.lastUpdatedTime = LocalDateTime.now();
    }
    
    public String getFirstUserMessageContent() {
        if (messages != null && !messages.isEmpty()) {
            for (Message message : messages) {
                if ("user".equals(message.getRole())) {
                    return message.getContent();
                }
            }
        }
        return "新对话";
    }
}
