@echo off
REM ========================================
REM SchemaSync 一键打包脚本
REM 将前后端打包成一个可执行的JAR文件
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
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Maven,请先安装Maven 3.6+
    pause
    exit /b 1
)
echo [成功] Maven环境正常

REM 清理旧构建
echo.
echo [3/5] 清理旧构建...
cd schemasync-backend
call mvn clean
if %errorlevel% neq 0 (
    echo [错误] 清理失败
    pause
    exit /b 1
)
echo [成功] 清理完成

REM 构建前端
echo.
echo [4/5] 构建前端项目...
cd ..\schemasync-frontend
call npm install
if %errorlevel% neq 0 (
    echo [错误] 前端依赖安装失败
    pause
    exit /b 1
)

call npm run build
if %errorlevel% neq 0 (
    echo [错误] 前端构建失败
    pause
    exit /b 1
)
echo [成功] 前端构建完成

REM 打包后端(包含前端)
echo.
echo [5/5] 打包后端(包含前端资源)...
cd ..\schemasync-backend
call mvn package -DskipTests
if %errorlevel% neq 0 (
    echo [错误] 打包失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo   打包成功!
echo ========================================
echo.
echo JAR文件位置: schemasync-backend\target\schemasync-backend-1.0.0-SNAPSHOT.jar
echo.
echo 启动命令:
echo   java -jar schemasync-backend\target\schemasync-backend-1.0.0-SNAPSHOT.jar
echo.
echo 访问地址:
echo   后端API: http://localhost:8080/api
echo   前端页面: http://localhost:8080
echo.
echo ========================================
echo.

pause
