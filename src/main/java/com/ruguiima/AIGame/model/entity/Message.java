package com.ruguiima.AIGame.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String role;        // user 或 ai
    private String content;     // 消息内容
    private LocalDateTime time; // 消息时间
    
    public Message(String role, String content) {
        this.role = role;
        this.content = content;
        this.time = LocalDateTime.now();
    }
}
