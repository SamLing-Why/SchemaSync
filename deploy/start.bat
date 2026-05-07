@echo off
chcp 65001 >nul
REM ========================================
REM SchemaSync 启动脚本 (Windows)
REM ========================================

echo.
echo ========================================
echo   SchemaSync 启动中...
echo ========================================
echo.
echo 访问地址: http://localhost:8999
echo.

java -jar -Xms256m -Xmx512m schemasync.jar --spring.config.location=application.yml

echo.
echo 服务已停止
pause
