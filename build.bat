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

REM 查找并复制最新打包的JAR
for %%f in (schemasync-backend\target\schemasync-backend-*.jar) do (
    echo [信息] 找到JAR文件: %%~nxf
    echo [信息] 文件大小: %%~zf 字节
    copy /Y "%%f" deploy\schemasync.jar >nul
    if %errorlevel% neq 0 (
        echo [错误] 复制JAR文件失败
        pause
        exit /b 1
    )
    echo [成功] 已复制: %%~nxf
    goto :JAR_COPIED
)

echo [错误] 找不到JAR文件
echo [提示] 请确认打包是否成功
pause
exit /b 1

:JAR_COPIED

REM 生成外置配置文件（从JAR包中提取已处理的配置）
echo [信息] 生成外置配置文件...
if exist schemasync-backend\target\schemasync-backend-*.jar (
    REM 从JAR包中提取application.yml（Maven已替换占位符）
    for %%f in (schemasync-backend\target\schemasync-backend-*.jar) do (
        powershell -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; $jar = '%%f'; $entryName = 'BOOT-INF/classes/application.yml'; $zip = [System.IO.Compression.ZipFile]::OpenRead($jar); $entry = $zip.Entries | Where-Object { $_.FullName -eq $entryName }; if ($entry) { [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, 'deploy\application.yml', $true); Write-Host '[成功] 配置文件已从JAR包提取' } else { Write-Host '[警告] 未在JAR中找到配置文件' }; $zip.Dispose()"
    )
) else (
    echo [警告] 未找到JAR文件
)

echo [成功] 文件复制完成

REM 创建启动脚本
echo.
echo [6/6] 生成启动脚本和文档...

REM 创建start.bat
if not exist deploy\start.bat (
    (
        echo @echo off
        echo chcp 65001 ^>nul
        echo REM SchemaSync Start Script
        echo java -jar -Xms256m -Xmx512m schemasync.jar --spring.config.location=application.yml
        echo pause
    ) > deploy\start.bat
)

REM 创建start.sh
if not exist deploy\start.sh (
    (
        echo #!/bin/bash
        echo # ========================================
        echo # SchemaSync 启动脚本 ^(Linux^)
        echo # ========================================
        echo.
        echo echo ""
        echo echo "========================================"
        echo echo "  SchemaSync 启动中..."
        echo echo "========================================"
        echo echo ""
        echo echo "访问地址: http://localhost:8999"
        echo echo ""
        echo.
        echo # 使用nohup后台运行
        echo nohup java -jar -Xms256m -Xmx512m schemasync.jar --spring.config.location=application.yml ^> app.log 2^>^&1 ^&
        echo.
        echo echo "应用已在后台启动"
        echo echo "PID: $!"
        echo echo "日志: tail -f app.log"
        echo echo ""
        echo echo "停止服务: kill $! 或 pkill -f schemasync.jar"
    ) > deploy\start.sh
)

REM 创建DEPLOY.md
if not exist deploy\DEPLOY.md (
    (
        echo # SchemaSync Deploy Guide
        echo.
        echo ## Requirements
        echo - JDK 8+
        echo - 512MB+ RAM
        echo.
        echo ## Usage
        echo - Windows: start.bat
        echo - Linux: ./start.sh
    ) > deploy\DEPLOY.md
)

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
echo   http://服务器IP:8999
echo.
echo ========================================
echo.

pause
