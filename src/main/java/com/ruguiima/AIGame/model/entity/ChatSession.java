package com.ruguiima.AIGame.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {
    private String sessionId;
    private String sessionName;
    private LocalDateTime createdTime;
    private LocalDateTime lastUpdatedTime;
    private List<Message> messages;
    
    public ChatSession(String sessionName) {
        this.sessionId = UUID.randomUUID().toString();
        this.sessionName = sessionName;
        this.createdTime = LocalDateTime.now();
        this.lastUpdatedTime = LocalDateTime.now();
        this.messages = new ArrayList<>();
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
