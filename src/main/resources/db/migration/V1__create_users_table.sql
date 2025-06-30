-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    
    -- 个人信息字段
    nickname VARCHAR(100),
    gender VARCHAR(10),
    birthday DATE,
    avatar_url VARCHAR(500),
    
    -- 时间字段
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP NULL,
    
    -- 模型设置字段
    preferred_model VARCHAR(50) DEFAULT 'deepseek-chat',
    response_length VARCHAR(20) DEFAULT 'short',
    creativity_level VARCHAR(20) DEFAULT 'precise',
    max_tokens INT DEFAULT 500,
    temperature DECIMAL(3,2) DEFAULT 0.2
);

-- 创建索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- 添加检查约束
ALTER TABLE users ADD CONSTRAINT chk_gender CHECK (gender IN ('male', 'female', 'other') OR gender IS NULL);
ALTER TABLE users ADD CONSTRAINT chk_preferred_model CHECK (preferred_model IN ('deepseek-chat', 'deepseek-reasoner'));
ALTER TABLE users ADD CONSTRAINT chk_response_length CHECK (response_length IN ('short', 'medium', 'long'));
ALTER TABLE users ADD CONSTRAINT chk_creativity_level CHECK (creativity_level IN ('precise', 'balanced', 'creative'));
ALTER TABLE users ADD CONSTRAINT chk_max_tokens CHECK (max_tokens > 0 AND max_tokens <= 4096);
ALTER TABLE users ADD CONSTRAINT chk_temperature CHECK (temperature >= 0.0 AND temperature <= 2.0);
