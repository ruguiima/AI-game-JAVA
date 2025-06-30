#!/bin/bash

# AI 聊天应用快速启动脚本

echo "🚀 AI 聊天应用启动脚本"
echo "========================="

# 检查 Java 版本
echo "检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装或未配置到 PATH"
    echo "请安装 Java 17 或更高版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 版本过低: $JAVA_VERSION"
    echo "请安装 Java 17 或更高版本"
    exit 1
fi

echo "✅ Java 版本: $JAVA_VERSION"

# 检查配置文件
echo "检查配置文件..."

if [ ! -f "src/main/resources/application-local.yaml" ]; then
    echo "❌ 缺少本地配置文件: application-local.yaml"
    echo "请在 src/main/resources/ 目录下创建 application-local.yaml 文件"
    echo "参考以下配置模板:"
    echo ""
    echo "spring:"
    echo "  datasource:"
    echo "    url: jdbc:mysql://localhost:3306/ai_chatting_web?useSSL=false&serverTimezone=UTC&characterEncoding=utf8"
    echo "    username: root"
    echo "    password: your_database_password"
    echo ""
    echo "deepseek:"
    echo "  api-key: your_deepseek_api_key"
    echo ""
    exit 1
fi

echo "✅ 配置文件检查完成"

# 检查数据库连接
echo "检查数据库连接..."
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}

if ! nc -z "$DB_HOST" "$DB_PORT" 2>/dev/null; then
    echo "❌ 无法连接到数据库: $DB_HOST:$DB_PORT"
    echo "请确保 MySQL 服务正在运行"
    echo "或者使用 docker-compose 启动数据库:"
    echo "  docker-compose up -d mysql"
    exit 1
fi

echo "✅ 数据库连接正常"

# 启动应用
echo ""
echo "🎯 启动应用..."
echo "应用启动后可访问: http://localhost:8080"
echo "使用配置文件: application-local.yaml"
echo ""

# 使用 Maven Wrapper 启动，指定本地配置
if [ -f "./mvnw" ]; then
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
else
    mvn spring-boot:run -Dspring-boot.run.profiles=local
fi
