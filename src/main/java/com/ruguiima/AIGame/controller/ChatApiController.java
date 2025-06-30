package com.ruguiima.AIGame.controller;

import com.ruguiima.AIGame.model.dto.StreamRequest;
import com.ruguiima.AIGame.model.entity.ChatSession;
import com.ruguiima.AIGame.model.entity.User;
import com.ruguiima.AIGame.model.vo.StreamResponseVO;
import com.ruguiima.AIGame.service.ChatSessionService;
import com.ruguiima.AIGame.service.DeepSeekService;
import com.ruguiima.AIGame.service.ModelSettings;
import com.ruguiima.AIGame.service.SessionService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@Slf4j
public class ChatApiController {
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatSessionService chatSessionService;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private DeepSeekService deepSeekService;
    
    /**
     * 处理聊天消息并返回流式响应
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody StreamRequest request, HttpSession httpSession) {
        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时
        
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(httpSession)) {
            try {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "未登录");
                emitter.send(error);
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        // 获取当前登录用户
        User currentUser = sessionService.getCurrentUser(httpSession);
        String userMessage = request.getMessage();
        String sessionId = request.getSessionId();
        
        // 异步处理流式响应
        CompletableFuture.runAsync(() -> {
            try {
                ChatSession session;
                
                // 使用临时会话或根据ID获取会话
                if (sessionId == null || sessionId.isEmpty()) {
                    // 添加用户消息到临时会话或创建新临时会话
                    session = chatSessionService.addMessage(null, "user", userMessage, currentUser);
                } else {
                    session = chatSessionService.getSession(sessionId);
                    if (session != null && session.getUser() != null && 
                            session.getUser().getId().equals(currentUser.getId())) {
                        // 添加消息到用户自己的会话
                        session = chatSessionService.addMessage(sessionId, "user", userMessage, currentUser);
                    } else {
                        // 会话不存在或不属于当前用户，创建新临时会话
                        session = chatSessionService.addMessage(null, "user", userMessage, currentUser);
                    }
                }

                // 保存用户消息的会话ID
                final String finalSessionId = session.getSessionId();

                // 创建回调函数处理流式输出
                DeepSeekService.StreamCallback callback = new DeepSeekService.StreamCallback() {
                    private StringBuilder fullResponse = new StringBuilder();
                    private boolean isFirstToken = true;

                    @Override
                    public void onToken(String token) {
                        try {
                            StreamResponseVO response;
                            
                            // 如果是第一个token，添加会话信息
                            if (isFirstToken) {
                                ChatSession currentSession = chatSessionService.getSession(finalSessionId);
                                if (currentSession != null) {
                                    response = StreamResponseVO.newSessionToken(token, finalSessionId, currentSession.getSessionName());
                                } else {
                                    response = StreamResponseVO.token(token, finalSessionId);
                                }
                                isFirstToken = false;
                            } else {
                                response = StreamResponseVO.token(token, finalSessionId);
                            }
                            
                            // 使用正确的SSE消息格式发送
                            String jsonData = objectMapper.writeValueAsString(response);
                            emitter.send(SseEmitter.event()
                                .data(jsonData, MediaType.APPLICATION_JSON)
                                .id(String.valueOf(System.currentTimeMillis()))
                                .name("message"));
                            
                            fullResponse.append(token);
                        } catch (IOException e) {
                            // 发送失败，可能是客户端已断开连接
                            log.error("发送token失败", e);
                        }
                    }

                    @Override
                    public void onComplete(String fullText) {
                        try {
                            StreamResponseVO response = StreamResponseVO.complete(finalSessionId, fullText);
                            
                            // 使用正确的SSE消息格式发送
                            String jsonData = objectMapper.writeValueAsString(response);
                            emitter.send(SseEmitter.event()
                                .data(jsonData, MediaType.APPLICATION_JSON)
                                .id(String.valueOf(System.currentTimeMillis()))
                                .name("message"));
                            
                            // 将完整的AI回复添加到会话
                            chatSessionService.addMessage(finalSessionId, "ai", fullText, currentUser);
                            
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("完成消息发送失败", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        try {
                            Map<String, Object> data = new HashMap<>();
                            data.put("error", "生成回复时出错: " + t.getMessage());
                            
                            // 使用正确的SSE消息格式发送
                            String jsonData = objectMapper.writeValueAsString(data);
                            emitter.send(SseEmitter.event()
                                .data(jsonData, MediaType.APPLICATION_JSON)
                                .id(String.valueOf(System.currentTimeMillis()))
                                .name("error"));
                            
                            // 即使出错，也要保存已生成的部分回复
                            String partialResponse = fullResponse.toString();
                            if (!partialResponse.isEmpty()) {
                                chatSessionService.addMessage(
                                    finalSessionId, 
                                    "ai",
                                    partialResponse + "\n\n[生成出错，响应不完整]", 
                                    currentUser
                                );
                            }
                            
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("错误消息发送失败", e);
                            emitter.completeWithError(e);
                        }
                    }
                };
                
                // 获取用户的模型设置
                ModelSettings modelSettings;
                try {
                    // 获取用户设置，如果为null则使用默认值
                    String model = currentUser.getPreferredModel() != null ? 
                                  currentUser.getPreferredModel() : "deepseek-chat";
                    Double temperature = currentUser.getTemperature() != null ? 
                                        currentUser.getTemperature() : 0.2;
                    Integer maxTokens = currentUser.getMaxTokens() != null ? 
                                       currentUser.getMaxTokens() : 500;
                    
                    modelSettings = ModelSettings.builder()
                            .model(model)
                            .temperature(temperature)
                            .maxTokens(maxTokens)
                            .build();
                } catch (Exception e) {
                    // 如果获取用户设置失败，使用默认设置
                    modelSettings = ModelSettings.defaultSettings();
                    log.warn("获取用户模型设置失败，使用默认设置", e);
                }
                
                // 调用流式API（使用用户的模型设置）
                deepSeekService.createChatCompletionStream(session, userMessage, modelSettings, callback);
                
            } catch (Exception e) {
                try {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "处理请求时出错: " + e.getMessage());
                    emitter.send(error);
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 为流式API提供会话信息的端点
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSessionInfo(@PathVariable String sessionId, HttpSession httpSession) {
        // 检查用户是否已登录
        if (!sessionService.isLoggedIn(httpSession)) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        
        User currentUser = sessionService.getCurrentUser(httpSession);
        ChatSession session = chatSessionService.getSession(sessionId);
        
        if (session == null || session.getUser() == null || 
                !session.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(404).body(Map.of("error", "会话不存在或无权访问"));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("sessionName", session.getSessionName());
        
        return ResponseEntity.ok(response);
    }
}
