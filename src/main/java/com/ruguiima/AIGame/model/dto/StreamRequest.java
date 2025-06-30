package com.ruguiima.AIGame.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamRequest {
    private String sessionId; // 会话ID，可为空（创建新会话）
    private String message;   // 用户消息
}
