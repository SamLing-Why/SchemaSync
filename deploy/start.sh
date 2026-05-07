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

java -jar -Xms256m -Xmx512m schemasync.jar --spring.config.location=application.yml

echo ""
echo "服务已停止"
