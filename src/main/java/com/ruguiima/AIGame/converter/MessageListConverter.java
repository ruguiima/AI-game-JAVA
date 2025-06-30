package com.ruguiima.AIGame.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ruguiima.AIGame.model.entity.Message;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class MessageListConverter implements AttributeConverter<List<Message>, String> {
    private final ObjectMapper objectMapper;

    public MessageListConverter() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // 支持Java 8时间类型
    }

    @Override
    public String convertToDatabaseColumn(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "[]";
        }
        
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "[]";
        }
    }

    @Override
    public List<Message> convertToEntityAttribute(String json) {
        if (json == null || json.equals("[]")) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<Message>>() {});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
