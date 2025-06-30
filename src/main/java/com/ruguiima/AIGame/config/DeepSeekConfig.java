package com.ruguiima.AIGame.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "deepseek")
@Data
public class DeepSeekConfig {
    private String apiKey;
    private String baseUrl;
    private String model;
    private int maxMessages;
    private String systemMessage;
    private boolean stream;        // 是否启用流式输出
    private float temperature;     // 温度参数
    private int maxTokens;         // 最大token数
}
