package com.ruguiima.AIGame.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String role;
    private String content;
    
    // 从Message实体转换为ChatMessage对象
    public static ChatMessage fromMessage(com.ruguiima.AIGame.model.entity.Message message) {
        return new ChatMessage(
            // 将"user"和"ai"角色转换为OpenAI API格式
            message.getRole().equals("ai") ? "assistant" : message.getRole(), 
            message.getContent()
        );
    }
}
