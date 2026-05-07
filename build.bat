@echo off
chcp 65001 >nul
REM ========================================
REM SchemaSync 一键打包脚本 (简化版)
REM ========================================

echo.
echo ========================================
echo   SchemaSync 打包工具
echo ========================================
echo.

REM 检查Java环境
echo [1/5] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Java环境,请先安装JDK 8+
    pause
    exit /b 1
)
echo [成功] Java环境正常

REM 检查Maven
echo.
echo [2/5] 检查Maven环境...
call mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Maven,请先安装Maven 3.6+
    pause
    exit /b 1
)
echo [成功] Maven环境正常

REM 清理并打包
echo.
echo [3/5] 清理并打包(包含前端)...
cd /d "%~dp0schemasync-backend"
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [错误] 打包失败
    pause
    exit /b 1
)
echo [成功] 打包完成

REM 创建部署包
echo.
echo [4/5] 创建部署目录...
cd /d "%~dp0"
if not exist deploy mkdir deploy
echo [成功] 部署目录就绪

REM 复制文件
echo.
echo [5/6] 复制部署文件...
copy /Y schemasync-backend\target\schemasync-backend-1.0.0-SNAPSHOT.jar deploy\schemasync.jar >nul
if exist schemasync-backend\src\main\resources\application.yml (
    copy /Y schemasync-backend\src\main\resources\application.yml deploy\application.yml >nul
)
echo [成功] 文件复制完成

REM 创建启动脚本
echo.
echo [6/6] 生成启动脚本和文档...

REM 使用PowerShell创建文件（避免批处理转义问题）
powershell -Command "if (!(Test-Path 'deploy\start.bat')) { '@echo off`nchcp 65001 >nul`nREM SchemaSync Start Script`njava -jar -Xms256m -Xmx512m schemasync.jar --spring.config.location=application.yml`npause' | Out-File -FilePath 'deploy\start.bat' -Encoding ASCII }"
powershell -Command "if (!(Test-Path 'deploy\start.sh')) { '#!/bin/bash`n# SchemaSync Start Script`njava -jar -Xms256m -Xmx512m schemasync.jar --spring.config.location=application.yml' | Out-File -FilePath 'deploy\start.sh' -Encoding ASCII }"
powershell -Command "if (!(Test-Path 'deploy\DEPLOY.md')) { '# SchemaSync Deploy Guide`n`n## Requirements`n- JDK 8+`n- 512MB+ RAM`n`n## Usage`n- Windows: start.bat`n- Linux: ./start.sh' | Out-File -FilePath 'deploy\DEPLOY.md' -Encoding UTF8 }"

echo [成功] 启动脚本和文档生成完成

echo.
echo ========================================
echo   打包成功!
echo ========================================
echo.
echo 部署包位置: deploy\
echo   - schemasync.jar
echo   - application.yml
echo   - start.bat
echo   - start.sh
echo   - DEPLOY.md
echo.
echo 部署说明:
echo   1. 将 deploy 目录复制到服务器
echo   2. Windows: 双击 start.bat
echo   3. Linux:   chmod +x start.sh ^&^& ./start.sh
echo.
echo 访问地址:
echo   http://服务器IP:8080
echo.
echo ========================================
echo.

pause
