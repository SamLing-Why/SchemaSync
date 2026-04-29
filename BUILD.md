# 构建脚本说明

## ✅ 脚本清单

| 脚本文件 | 用途 | 平台 |
|---------|------|------|
| **build.bat** | 一键打包脚本 | Windows |
| **build.sh** | 一键打包脚本 | Linux/Mac |

## 🚀 快速开始

### Windows

```batch
# 在项目根目录双击运行
build.bat

# 或在CMD中运行
cd E:\codeWorkSpace\spring\SchemaSync
build.bat
```

### Linux/Mac

```bash
cd /path/to/SchemaSync
chmod +x build.sh
./build.sh
```

## 📝 构建流程

```
[1/5] 检查Java环境...      ✓
[2/5] 检查Maven环境...     ✓
[3/5] 清理并打包(包含前端)... ✓
[4/5] 创建部署目录...       ✓
[5/5] 复制部署文件...       ✓
```

## 📦 生成产物

构建完成后，`deploy/` 目录包含：

```
deploy/
├── schemasync.jar (54.35 MB)  # 完整应用
├── application.yml            # 外置配置
├── start.bat                  # Windows启动脚本
├── start.sh                   # Linux启动脚本
├── DEPLOY.md                  # 部署文档
└── 打包清单.txt               # 打包清单
```

## ⚙️ 脚本特性

1. **自动环境检查**
   - 检查Java环境 (JDK 8+)
   - 检查Maven环境 (Maven 3.6+)

2. **自动化构建**
   - 自动清理旧构建
   - Maven打包（包含前端构建）
   - 前端由 `frontend-maven-plugin` 自动处理

3. **自动部署包生成**
   - 自动创建 `deploy/` 目录
   - 复制并重命名JAR为 `schemasync.jar`
   - 复制 `application.yml` 配置文件

## 🎯 部署说明

构建完成后：

1. **Windows服务器**
   ```batch
   # 复制 deploy 目录到服务器
   # 双击 start.bat 启动
   ```

2. **Linux服务器**
   ```bash
   # 复制 deploy 目录到服务器
   chmod +x start.sh
   ./start.sh
   ```

3. **访问地址**
   - 应用主页: http://服务器IP:8080
   - API文档: http://服务器IP:8080/api-docs
   - 健康检查: http://服务器IP:8080/actuator/health

## 📋 环境要求

**构建环境**：
- JDK: 8 或更高版本
- Maven: 3.6 或更高版本
- Node.js: 16 或更高版本（由Maven自动管理）

**运行环境**：
- JDK: 8 或更高版本
- 内存: 至少 512MB
- 磁盘: 至少 200MB

## ⚠️ 注意事项

1. **编码问题**
   - build.bat 已设置 UTF-8 编码 (`chcp 65001`)
   - 建议在CMD中运行，PowerShell可能有显示问题

2. **路径问题**
   - 脚本使用 `%~dp0` 确保路径正确
   - 可以在任何目录运行，只要在项目根目录即可

3. **首次构建**
   - 首次构建会下载前端依赖，可能需要几分钟
   - Maven会下载依赖包，确保网络连接正常

## 🐛 故障排查

**构建失败**：
1. 检查Java版本：`java -version`
2. 检查Maven版本：`mvn -version`
3. 查看Maven输出错误信息

**无法启动**：
1. 检查端口占用：`netstat -ano | findstr 8080`
2. 查看日志：`logs/schemasync.log`
3. 检查配置文件：`deploy/application.yml`

## 📝 版本信息

- **当前版本**: 1.0.0-SNAPSHOT
- **最后更新**: 2026-04-29
- **Git提交**: 03dca90
