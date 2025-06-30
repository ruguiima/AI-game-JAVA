package com.ruguiima.AIGame.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelSettings {
    private String model;
    private Double temperature;
    private Integer maxTokens;
    
    // 创建默认设置
    public static ModelSettings defaultSettings() {
        return ModelSettings.builder()
                .model("deepseek-chat")
                .temperature(0.2)
                .maxTokens(500)
                .build();
    }
}
