package com.ruguiima.nexus.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式响应VO，用于SSE数据传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamResponseVO {
    private String token;           // 当前token
    private String sessionId;       // 会话ID
    private Boolean done;           // 是否完成
    private String fullText;        // 完整文本（仅在完成时有值）
    private String sessionName;     // 会话名称（仅在新会话时有值）
    private Boolean isNewSession;   // 是否是新会话（仅在新会话时有值）
    private String error;           // 错误信息（仅在出错时有值）
    
    // 便捷构造方法
    public static StreamResponseVO token(String token, String sessionId) {
        StreamResponseVO vo = new StreamResponseVO();
        vo.setToken(token);
        vo.setSessionId(sessionId);
        vo.setDone(false);
        return vo;
    }
    
    public static StreamResponseVO newSessionToken(String token, String sessionId, String sessionName) {
        StreamResponseVO vo = new StreamResponseVO();
        vo.setToken(token);
        vo.setSessionId(sessionId);
        vo.setSessionName(sessionName);
        vo.setIsNewSession(true);
        vo.setDone(false);
        return vo;
    }
    
    public static StreamResponseVO complete(String sessionId, String fullText) {
        StreamResponseVO vo = new StreamResponseVO();
        vo.setToken("");
        vo.setSessionId(sessionId);
        vo.setFullText(fullText);
        vo.setDone(true);
        return vo;
    }
    
    public static StreamResponseVO error(String errorMessage) {
        StreamResponseVO vo = new StreamResponseVO();
        vo.setError(errorMessage);
        vo.setDone(true);
        return vo;
    }
}
