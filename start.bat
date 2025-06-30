@echo off
setlocal enabledelayedexpansion
REM AI 聊天应用快速启动脚本

echo 🚀 AI 聊天应用启动脚本
echo =========================

REM 检查 Java 版本
echo 检查 Java 环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java 未安装或未配置到 PATH
    echo 请安装 Java 17 或更高版本
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_STRING=%%g
)
set JAVA_VERSION_STRING=%JAVA_VERSION_STRING:"=%
for /f "delims=. tokens=1" %%v in ("%JAVA_VERSION_STRING%") do (
    if %%v LSS 17 (
        echo ❌ Java 版本过低: %%v
        echo 请安装 Java 17 或更高版本
        pause
        exit /b 1
    )
)

echo ✅ Java 版本检查通过

REM 检查配置文件
echo 检查配置文件...

if not exist "src\main\resources\application-local.yaml" (
    echo ❌ 缺少本地配置文件: application-local.yaml
    echo 请在 src\main\resources\ 目录下创建 application-local.yaml 文件
    echo 参考以下配置模板:
    echo.
    echo spring:
    echo   datasource:
    echo     url: jdbc:mysql://localhost:3306/ai_chatting_web?useSSL=false^&serverTimezone=UTC^&characterEncoding=utf8
    echo     username: root
    echo     password: your_database_password
    echo.
    echo deepseek:
    echo   api-key: your_deepseek_api_key
    echo.
    pause
    exit /b 1
)

echo ✅ 配置文件检查完成

REM 检查和设置 JAVA_HOME
echo 检查 JAVA_HOME 环境变量...
if "%JAVA_HOME%"=="" (
    echo ⚠️  JAVA_HOME 未设置，尝试自动检测 Java 安装路径...
    
    REM 尝试从注册表获取 Java 安装路径
    for /f "tokens=2*" %%a in ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\JDK" /s /v JavaHome 2^>nul ^| findstr "JavaHome"') do (
        set "DETECTED_JAVA_HOME=%%b"
        goto :java_found
    )
    
    REM 如果注册表方法失败，尝试常见安装目录
    for %%d in ("C:\Program Files\Java\jdk*" "C:\Program Files\OpenJDK\jdk*" "C:\Program Files (x86)\Java\jdk*") do (
        if exist "%%d\bin\java.exe" (
            set "DETECTED_JAVA_HOME=%%d"
            goto :java_found
        )
    )
    
    echo ❌ 无法自动检测 Java 安装路径
    echo 请手动设置 JAVA_HOME 环境变量，或使用以下命令：
    echo   set JAVA_HOME=C:\Path\To\Your\Java\Installation
    echo   然后重新运行此脚本
    pause
    exit /b 1
    
    :java_found
    set "JAVA_HOME=!DETECTED_JAVA_HOME!"
    echo ✅ 检测到 Java 安装路径: !JAVA_HOME!
) else (
    echo ✅ JAVA_HOME 已设置: %JAVA_HOME%
)

REM 启动应用
echo.
echo 🎯 启动应用...
echo 应用启动后可访问: http://localhost:8080
echo 使用配置文件: application-local.yaml
echo.

REM 使用 Maven Wrapper 启动，指定本地配置
if exist "mvnw.cmd" (
    mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
) else (
    mvn spring-boot:run -Dspring-boot.run.profiles=local
)

pause
