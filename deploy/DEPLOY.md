# SchemaSync 部署说明

## 环境要求

- **JDK**: 8 或更高版本
- **内存**: 建议 512MB+
- **端口**: 8999 (可在 application.yml 中修改)

## 部署步骤

### Windows

1. 双击 `start.bat`

或命令行启动:

```bat
java -jar -Xms256m -Xmx512m schemasync.jar --spring.config.location=application.yml
```

### Linux

1. 添加执行权限: `chmod +x start.sh`
2. 运行: `./start.sh` (后台运行)

查看日志:

```bash
tail -f app.log
```

停止服务:

```bash
# 方式1: 使用PID
kill <PID>

# 方式2: 使用进程名
pkill -f schemasync.jar
```

## 配置说明

编辑 `application.yml` 修改配置:

```yaml
server:
  port: 8999 # 修改端口

schemasync:
  default-output-dir: ./output # 默认输出目录
```

## 访问地址

- **前端**: http://服务器IP:8999
- **API**: http://服务器IP:8999/api
- **健康检查**: http://服务器IP:8999/actuator/health

## 日志文件

日志位于 `logs/schemasync.log`

## 支持数据库

- MySQL 8.0+
- Oracle 11g+
- PostgreSQL 9.6+
- OpenGauss 5.0+
- OceanBase (MySQL兼容)
- TDSQL (MySQL兼容)
- GoldenDB (MySQL兼容)
