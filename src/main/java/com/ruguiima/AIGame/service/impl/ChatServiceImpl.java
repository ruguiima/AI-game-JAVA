package com.ruguiima.AIGame.service.impl;

import org.springframework.stereotype.Service;

import com.ruguiima.AIGame.service.ChatService;

@Service
public class ChatServiceImpl implements ChatService {
    @Override
    public String getMessage(String userMessage) {
        String aiResponse = callAiApi(userMessage);
        return aiResponse;

    }

    public String callAiApi(String userMessage) {
        // Simulate an AI response
        return "AI response";
    }
}
