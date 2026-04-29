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

# 打包后端(包含前端)
echo ""
echo "[4/5] 打包后端(包含前端资源)..."
cd ../schemasync-backend || exit 1
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "[错误] 打包失败"
    exit 1
fi

# 创建部署包
echo ""
echo "[5/5] 创建部署包..."
cd .. || exit 1
mkdir -p deploy

# 复制JAR文件
cp schemasync-backend/target/schemasync-backend-1.0.0-SNAPSHOT.jar deploy/schemasync.jar
if [ $? -ne 0 ]; then
    echo "[错误] 复制JAR文件失败"
    exit 1
fi

# 复制配置文件
if [ -f schemasync-backend/src/main/resources/application.yml ]; then
    cp schemasync-backend/src/main/resources/application.yml deploy/application.yml
fi

echo ""
echo "========================================"
echo "  打包成功!"
echo "========================================"
echo ""
echo "JAR文件: deploy/schemasync.jar"
echo "配置文件: deploy/application.yml"
echo "启动脚本: deploy/start.sh"
echo ""
echo "部署步骤:"
echo "  1. 将 deploy 目录复制到服务器"
echo "  2. Windows: 双击 start.bat"
echo "  3. Linux:   chmod +x start.sh && ./start.sh"
echo ""
echo "访问地址:"
echo "  应用主页: http://localhost:8080"
echo "  API文档:  http://localhost:8080/api-docs"
echo ""
echo "========================================"
echo ""
