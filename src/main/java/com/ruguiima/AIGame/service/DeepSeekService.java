package com.ruguiima.AIGame.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruguiima.AIGame.config.DeepSeekConfig;
import com.ruguiima.AIGame.model.dto.ChatMessage;
import com.ruguiima.AIGame.model.entity.ChatSession;
import com.ruguiima.AIGame.model.entity.Message;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DeepSeekService {

    /**
     * 流式回调接口，用于处理流式响应
     */
    public interface StreamCallback {
        /**
         * 接收单个token
         */
        void onToken(String token);
        
        /**
         * 响应完成
         */
        void onComplete(String fullText);
        
        /**
         * 发生错误
         */
        void onError(Throwable t);
    }

    @Autowired
    private DeepSeekConfig config;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 调用DeepSeek的聊天接口 - 流式输出
     * @param session 聊天会话
     * @param userMessage 用户消息
     * @param callback 回调函数
     */
    
    /**
     * 调用DeepSeek的聊天接口 - 流式输出（支持自定义模型设置）
     * @param messages 消息列表
     * @param modelSettings 模型设置
     * @param callback 回调函数
     */
    public void createChatCompletionStream(List<ChatMessage> messages, 
                                         DeepSeekConfig.ModelSettings modelSettings, 
                                         StreamCallback callback) {
        try {
            String url = config.getBaseUrl() + "/v1/chat/completions";
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());
            
            // 创建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelSettings.getModel());
            requestBody.put("temperature", modelSettings.getTemperature());
            requestBody.put("max_tokens", modelSettings.getMaxTokens());
            requestBody.put("stream", true);  // 启用流式输出
            
            // 添加消息
            ArrayNode messagesArray = requestBody.putArray("messages");
            for (ChatMessage message : messages) {
                ObjectNode messageObject = messagesArray.addObject();
                // 映射角色：ai -> assistant，其他保持不变
                String role = "ai".equals(message.getRole()) ? "assistant" : message.getRole();
                messageObject.put("role", role);
                messageObject.put("content", message.getContent());
            }
            
            // 使用自定义ResponseExtractor来处理流式响应
            StringBuilder fullResponse = new StringBuilder();
            restTemplate.execute(
                url, 
                org.springframework.http.HttpMethod.POST,
                req -> {
                    req.getHeaders().addAll(headers);
                    req.getBody().write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                },
                response -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String jsonData = line.substring(6).trim();
                                
                                // 跳过[DONE]行
                                if (jsonData.equals("[DONE]")) {
                                    continue;
                                }
                                
                                try {
                                    JsonNode chunk = objectMapper.readTree(jsonData);
                                    JsonNode choices = chunk.path("choices");
                                    
                                    if (choices.isArray() && choices.size() > 0) {
                                        JsonNode choice = choices.get(0);
                                        JsonNode delta = choice.path("delta");
                                        
                                        // 处理标准内容
                                        if (delta.has("content")) {
                                            String content = delta.path("content").asText();
                                            if (content != null && !content.isEmpty() && !"null".equals(content)) {
                                                callback.onToken(content);
                                                fullResponse.append(content);
                                            }
                                        }
                                        
                                        // 处理reasoning内容（deepseek-reasoner模型专用）
                                        if (delta.has("reasoning")) {
                                            String reasoning = delta.path("reasoning").asText();
                                            // 跳过推理内容，不显示给用户
                                            // 可以在这里记录日志，但不传递给前端
                                            if (reasoning != null && !reasoning.isEmpty() && !"null".equals(reasoning)) {
                                                log.debug("推理内容: {}", reasoning);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("解析流式响应数据出错: {}", jsonData, e);
                                }
                            }
                        }
                        
                        // 调用完成回调
                        callback.onComplete(fullResponse.toString());
                    } catch (Exception e) {
                        log.error("处理流式响应出错", e);
                        callback.onError(e);
                    }
                    return null;
                }
            );
        } catch (Exception e) {
            log.error("调用DeepSeek API出错", e);
            callback.onError(e);
        }
    }

    /**
     * 调用DeepSeek的聊天接口 - 流式输出（支持自定义模型设置）
     * @param session 聊天会话
     * @param userMessage 用户消息
     * @param modelSettings 模型设置
     * @param callback 回调函数
     */
    public void createChatCompletionStream(ChatSession session, String userMessage, 
                                         DeepSeekConfig.ModelSettings modelSettings, StreamCallback callback) {
        try {
            // 准备消息列表
            List<ChatMessage> messages = new ArrayList<>();
            
            // 添加系统消息
            messages.add(new ChatMessage("system", config.getSystemMessage()));
            
            // 转换历史消息并添加到请求中，限制历史消息数量
            List<Message> sessionMessages = session.getMessages();
            int historySize = sessionMessages.size();
            int startIdx = Math.max(0, historySize - config.getMaxMessages());
            
            // 如果有历史消息，添加到请求
            for (int i = startIdx; i < historySize; i++) {
                Message msg = sessionMessages.get(i);
                // 映射角色：ai -> assistant，其他保持不变
                String role = "ai".equals(msg.getRole()) ? "assistant" : msg.getRole();
                messages.add(new ChatMessage(role, msg.getContent()));
            }
            
            // 添加当前用户消息
            messages.add(new ChatMessage("user", userMessage));
            
            // 调用API
            createChatCompletionStream(messages, modelSettings, callback);
        } catch (Exception e) {
            log.error("处理聊天请求时出错", e);
            callback.onError(e);
        }
    }

    /**
     * 设置模型参数
     */
}
