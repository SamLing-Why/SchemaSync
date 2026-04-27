#!/bin/bash
# ========================================
# SchemaSync 一键打包脚本
# 将前后端打包成一个可执行的JAR文件
# ========================================

echo ""
echo "========================================"
echo "  SchemaSync 打包工具"
echo "========================================"
echo ""

# 检查Java环境
echo "[1/5] 检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "[错误] 未找到Java环境,请先安装JDK 8+"
    exit 1
fi
echo "[成功] Java环境正常: $(java -version 2>&1 | head -n 1)"

# 检查Maven
echo ""
echo "[2/5] 检查Maven环境..."
if ! command -v mvn &> /dev/null; then
    echo "[错误] 未找到Maven,请先安装Maven 3.6+"
    exit 1
fi
echo "[成功] Maven环境正常: $(mvn -version 2>&1 | head -n 1)"

# 清理旧构建
echo ""
echo "[3/5] 清理旧构建..."
cd schemasync-backend || exit 1
mvn clean
if [ $? -ne 0 ]; then
    echo "[错误] 清理失败"
    exit 1
fi
echo "[成功] 清理完成"

# 构建前端
echo ""
echo "[4/5] 构建前端项目..."
cd ../schemasync-frontend || exit 1
npm install
if [ $? -ne 0 ]; then
    echo "[错误] 前端依赖安装失败"
    exit 1
fi

npm run build
if [ $? -ne 0 ]; then
    echo "[错误] 前端构建失败"
    exit 1
fi
echo "[成功] 前端构建完成"

# 打包后端(包含前端)
echo ""
echo "[5/5] 打包后端(包含前端资源)..."
cd ../schemasync-backend || exit 1
mvn package -DskipTests
if [ $? -ne 0 ]; then
    echo "[错误] 打包失败"
    exit 1
fi

echo ""
echo "========================================"
echo "  打包成功!"
echo "========================================"
echo ""
echo "JAR文件位置: schemasync-backend/target/schemasync-backend-1.0.0-SNAPSHOT.jar"
echo ""
echo "启动命令:"
echo "  java -jar schemasync-backend/target/schemasync-backend-1.0.0-SNAPSHOT.jar"
echo ""
echo "访问地址:"
echo "  后端API: http://localhost:8080/api"
echo "  前端页面: http://localhost:8080"
echo ""
echo "========================================"
echo ""
