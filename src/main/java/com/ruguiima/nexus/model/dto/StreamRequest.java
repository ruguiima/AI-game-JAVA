package com.ruguiima.nexus.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamRequest {
    private String sessionId; // 会话ID，可为空（创建新会话）
    private String message;   // 用户消息
    private Map<String, Object> modelSettings; // 模型设置（可选，从前端传递但后端使用用户数据库设置）
}
