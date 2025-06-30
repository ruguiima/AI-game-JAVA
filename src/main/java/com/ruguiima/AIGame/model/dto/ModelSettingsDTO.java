package com.ruguiima.AIGame.model.dto;

import lombok.Data;

@Data
public class ModelSettingsDTO {
    private String model;           // deepseek-chat, deepseek-reasoner
    private String responseLength;  // short, medium, long
    private String creativity;      // precise, balanced, creative
    private Integer maxTokens;      // 500, 1000, 2000
    private Double temperature;     // 0.2, 0.5, 0.8
}
