# NEXUS ———— 接入大模型api的定制化聊天应用

NEXUS是一个基于 Spring Boot 的 AI 聊天应用，集成了 DeepSeek API，支持用户注册、登录、聊天会话管理和个性化模型设置。

## 功能特性

- 🔐 用户注册和登录系统
- 💬 与 AI 的实时聊天（支持流式输出）
- 📝 聊天会话管理和历史记录
- ⚙️ 个性化模型设置（模型选择、回复长度、创意程度）
- 👤 用户个人资料管理
- 🎨 现代化的响应式 UI 设计

## 技术栈

- **后端**: Spring Boot 3.x, Spring Data JPA, MySQL
- **前端**: HTML5, CSS3, JavaScript (原生)
- **数据库**: MySQL 8.0+
- **AI服务**: DeepSeek API
- **构建工具**: Maven
- **数据库迁移**: Flyway

## 快速开始

### 前置要求

1. Java 17 或更高版本
2. MySQL 8.0 或更高版本
3. Maven 3.6 或更高版本
4. DeepSeek API 密钥（从 [DeepSeek 官网](https://platform.deepseek.com/) 获取）

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/ruguiima/AI-game-JAVA.git
   cd AI-game-JAVA
   ```

2. **配置数据库**
   ```sql
   CREATE DATABASE ai_chatting_web CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **配置本地配置文件**

   在`src/main/resources`下新建`application-local.yaml`配置您自己的数据库信息和`api-key`，如：
   ```yaml
   spring:
      datasource:
         url: jdbc:mysql://localhost:3306/ai_chatting_web?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8
         username: root
         password: your_database_password

      deepseek:
         api-key: your_deepseek_api_key_here
   ```

4. **运行应用**
   
   **方式一：使用启动脚本（推荐）**
   ```bash
   # Windows
   start.bat
   
   # Linux/MacOS
   ./start.sh
   ```
   
   **方式二：手动启动**
   ```bash
   # 开发模式
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   
   # 或者构建后运行
   ./mvnw clean package -DskipTests
   java -jar target/AI-game-JAVA-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
   ```

5. **访问应用**
   
   打开浏览器访问: http://localhost:8080

## 配置说明

### 数据库配置

应用使用 Flyway 进行数据库版本管理，首次启动时会自动创建所需的表结构。

### DeepSeek API 配置

需要在 DeepSeek 官网注册账号并获取 API 密钥：
1. 访问 [DeepSeek 平台](https://platform.deepseek.com/)
2. 注册账号并获取 API 密钥
3. 将密钥配置到 `application-local.yaml` 文件中，或设置环境变量 `DEEPSEEK_API_KEY`

### 环境变量支持

除了配置文件，项目还支持通过环境变量覆盖配置：
- `DB_URL` - 数据库连接URL
- `DB_USERNAME` - 数据库用户名  
- `DB_PASSWORD` - 数据库密码
- `DEEPSEEK_API_KEY` - DeepSeek API密钥

**注意**: `application-local.yaml` 文件包含敏感信息，请勿提交到版本控制系统。

### 支持的模型

- `deepseek-chat`: 标准聊天模型
- `deepseek-reasoner`: 深度思考模型（会显示推理过程）

## 项目结构

```
src/
├── main/
│   ├── java/com/ruguiima/nexus/
│   │   ├── config/          # 配置类
│   │   ├── controller/      # 控制器
|   |   ├── converter/       # 转换器
│   │   ├── model/          # 数据模型
│   │   │   ├── dto/        # 数据传输对象
│   │   │   ├── entity/     # 实体类
│   │   │   └── vo/         # 视图对象
│   │   ├── repository/     # 数据访问层
│   │   └── service/        # 服务层
│   └── resources/
│       ├── db/migration/   # 数据库迁移脚本
│       ├── static/         # 静态资源
│       └── templates/      # 模板文件
```

## 路由说明

### 用户相关
- `POST /api/users/register` - 用户注册
- `POST /users/login` - 用户登录
- `GET /users/logout` - 用户登出

### 聊天相关  
- `POST /api/chat/stream` - 发送消息（流式响应）
- `GET /sessions/{sessionId}` - 获取聊天会话
- `GET /sessions` - 获取用户的所有会话

### 模型设置
- `GET /api/model-settings` - 获取用户模型设置
- `POST /api/model-settings` - 更新用户模型设置

### 用户信息相关
- `GET /api/user/profile` - 获取当前用户信息
- `POST /api/user/profile` - 更新当前用户信息
- `POST /api/user/avatar` - 上传用户头像

## 开发

### 运行测试
```bash
mvn test
```

### 构建生产版本
```bash
mvn clean package -Pproduction
```

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查 MySQL 服务是否启动
   - 验证数据库连接信息是否正确
   - 确保数据库已创建

2. **DeepSeek API 调用失败**
   - 检查 API 密钥是否正确
   - 验证网络连接
   - 检查 API 额度是否充足

3. **应用启动失败**
   - 检查 Java 版本是否为 17+
   - 验证环境变量是否正确设置
   - 查看日志文件定位具体错误

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。
