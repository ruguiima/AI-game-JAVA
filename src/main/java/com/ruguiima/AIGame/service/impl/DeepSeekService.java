package com.ruguiima.AIGame.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.io.IOException;
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
     * 调用DeepSeek的聊天接口 - 非流式输出
     * @param messages 消息列表
     * @return AI回复内容
     */
    public String createChatCompletion(List<ChatMessage> messages) {
        try {
            String url = config.getBaseUrl() + "/v1/chat/completions";
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());
            
            // 创建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());
            
            // 添加消息
            ArrayNode messagesArray = requestBody.putArray("messages");
            for (ChatMessage message : messages) {
                ObjectNode messageObject = messagesArray.addObject();
                messageObject.put("role", message.getRole());
                messageObject.put("content", message.getContent());
            }
            
            // 发送请求
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            // 解析响应
            JsonNode responseBody = objectMapper.readTree(response.getBody());
            String content = responseBody
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
            
            return content;
        } catch (Exception e) {
            log.error("调用DeepSeek API出错", e);
            throw new RuntimeException("调用AI服务失败", e);
        }
    }
    
    /**
     * 调用DeepSeek的聊天接口 - 流式输出
     * @param session 聊天会话
     * @param userMessage 用户消息
     * @param callback 回调函数
     */
    public void createChatCompletionStream(ChatSession session, String userMessage, StreamCallback callback) {
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
            if (historySize > 0) {
                List<Message> recentMessages = sessionMessages.subList(startIdx, historySize);
                
                for (Message msg : recentMessages) {
                    ChatMessage chatMsg = ChatMessage.fromMessage(msg);
                    messages.add(chatMsg);
                }
            }
            
            // 如果最后一条不是用户的消息，添加当前用户消息
            if (historySize == 0 || !sessionMessages.get(historySize-1).getRole().equals("user")) {
                messages.add(new ChatMessage("user", userMessage));
            }
            
            // 调用流式API
            createChatCompletionStream(messages, callback);
            
        } catch (Exception e) {
            log.error("准备流式响应请求时出错", e);
            callback.onError(e);
        }
    }
    
    /**
     * 调用DeepSeek的聊天接口 - 流式输出
     * @param messages 消息列表
     * @param callback 回调函数
     */
    public void createChatCompletionStream(List<ChatMessage> messages, StreamCallback callback) {
        try {
            String url = config.getBaseUrl() + "/v1/chat/completions";
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());
            
            // 创建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("stream", true);  // 启用流式输出
            
            // 添加消息
            ArrayNode messagesArray = requestBody.putArray("messages");
            for (ChatMessage message : messages) {
                ObjectNode messageObject = messagesArray.addObject();
                messageObject.put("role", message.getRole());
                messageObject.put("content", message.getContent());
            }
            
            // 发送请求
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
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
                                        JsonNode delta = choices.get(0).path("delta");
                                        if (delta.has("content")) {
                                            String content = delta.path("content").asText();
                                            if (content != null && !content.isEmpty()) {
                                                callback.onToken(content);
                                                fullResponse.append(content);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("解析流式响应数据出错: {}", jsonData, e);
                                }
                            }
                        }
                    } catch (IOException e) {
                        callback.onError(e);
                    }
                    
                    // 响应完成
                    String finalResponse = fullResponse.toString();
                    callback.onComplete(finalResponse);
                    return null;
                }
            );
        } catch (Exception e) {
            log.error("调用DeepSeek API流式输出出错", e);
            callback.onError(e);
        }
    }
}
