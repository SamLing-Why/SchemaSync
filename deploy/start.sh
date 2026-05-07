#!/bin/bash
# ========================================
# SchemaSync 启动脚本 (Linux)
# ========================================

echo ""
echo "========================================"
echo "  SchemaSync 启动中..."
echo "========================================"
echo ""
echo "访问地址: http://localhost:8999"
echo ""

# 使用nohup后台运行
nohup java -jar -Xms256m -Xmx512m schemasync.jar --spring.config.location=application.yml > app.log 2>&1 &

echo "应用已在后台启动"
echo "PID: $!"
echo "日志: tail -f app.log"
echo ""
echo "停止服务: kill $! 或 pkill -f schemasync.jar"
