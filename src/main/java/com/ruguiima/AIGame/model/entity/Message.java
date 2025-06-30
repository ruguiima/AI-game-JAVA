package com.ruguiima.AIGame.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String role;        // user 或 ai
    private String content;     // 消息内容
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time; // 消息时间
    
    public Message(String role, String content) {
        this.role = role;
        this.content = content;
        this.time = LocalDateTime.now();
    }
}
