package com.ruguiima.AIGame.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // 个人信息字段
    @Column(name = "nickname")
    private String nickname;

    @Column(name = "gender")
    private String gender; // male, female, other

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    // 模型设置字段
    @Column(name = "preferred_model", columnDefinition = "VARCHAR(50) DEFAULT 'deepseek-chat'")
    private String preferredModel = "deepseek-chat"; // deepseek-chat, deepseek-reasoner

    @Column(name = "response_length", columnDefinition = "VARCHAR(20) DEFAULT 'short'")
    private String responseLength = "short"; // short, medium, long

    @Column(name = "creativity_level", columnDefinition = "VARCHAR(20) DEFAULT 'precise'")
    private String creativityLevel = "precise"; // precise, balanced, creative

    @Column(name = "max_tokens", columnDefinition = "INT DEFAULT 500")
    private Integer maxTokens = 500;

    @Column(name = "temperature", columnDefinition = "DECIMAL(3,2) DEFAULT 0.2")
    private Double temperature = 0.2;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        
        // 设置模型设置的默认值
        if (preferredModel == null) {
            preferredModel = "deepseek-chat";
        }
        if (responseLength == null) {
            responseLength = "short";
        }
        if (creativityLevel == null) {
            creativityLevel = "precise";
        }
        if (maxTokens == null) {
            maxTokens = 500;
        }
        if (temperature == null) {
            temperature = 0.2;
        }
    }
}
