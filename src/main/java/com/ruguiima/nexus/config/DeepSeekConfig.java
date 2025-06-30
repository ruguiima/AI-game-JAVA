package com.ruguiima.nexus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@ConfigurationProperties(prefix = "deepseek")
@Data
public class DeepSeekConfig {
    // 系统配置
    private String apiKey;
    private String baseUrl;
    private int maxMessages;
    private String systemMessage;
    private boolean stream;        // 是否启用流式输出
    
    // 默认模型配置
    private String model = "deepseek-chat";
    private Double temperature = 0.2;
    private Integer maxTokens = 500;
    
    /**
     * 获取默认的模型设置
     */
    public ModelSettings getDefaultModelSettings() {
        return ModelSettings.builder()
                .model(this.model != null ? this.model : "deepseek-chat")
                .temperature(this.temperature != null ? this.temperature : 0.2)
                .maxTokens(this.maxTokens != null ? this.maxTokens : 500)
                .build();
    }
    
    /**
     * 基于用户设置创建模型配置，未设置的使用默认值
     */
    public ModelSettings createUserModelSettings(String userModel, Double userTemperature, Integer userMaxTokens) {
        return ModelSettings.builder()
                .model(userModel != null ? userModel : (this.model != null ? this.model : "deepseek-chat"))
                .temperature(userTemperature != null ? userTemperature : (this.temperature != null ? this.temperature : 0.2))
                .maxTokens(userMaxTokens != null ? userMaxTokens : (this.maxTokens != null ? this.maxTokens : 500))
                .build();
    }
    
    /**
     * 模型设置内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelSettings {
        private String model;
        private Double temperature;
        private Integer maxTokens;
        
        public static ModelSettings defaultSettings() {
            return ModelSettings.builder()
                    .model("deepseek-chat")
                    .temperature(0.2)
                    .maxTokens(500)
                    .build();
        }
    }
}
