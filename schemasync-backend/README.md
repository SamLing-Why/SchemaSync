# SchemaSync Backend

轻量级数据字典管理工具 - 后端服务

## 项目简介

SchemaSync 是一个面向敏捷开发的轻量级数据字典管理工具,支持:

- ✅ 数据字典导出 (JSON/Excel)
- ✅ 版本差异对比
- ✅ DDL变更脚本自动生成
- ✅ 支持 MySQL/Oracle/OceanBase/TDSQL/GaussDB/GoldenDB

## 技术栈

- **开发语言**: Java 8
- **核心框架**: Spring Boot 2.7.18
- **构建工具**: Maven 3.6+
- **数据库驱动**: MySQL 8.0, Oracle 21.9, PostgreSQL 42.6
- **Excel处理**: Apache POI 5.2.5
- **JSON处理**: Jackson / Fastjson2
- **CLI工具**: Picocli 4.7.5

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+

### 构建项目

```bash
# 克隆项目
git clone <repository-url>
cd schemasync-backend

# 编译打包
mvn clean package -DskipTests

# 开发环境运行
mvn spring-boot:run

# 或直接运行jar
java -jar target/schemasync-backend-1.0.0-SNAPSHOT.jar
```

### 配置数据源

编辑启动目录下的 `schemasync-config.json`:

```json
{
  "version": "1.0.0",
  "dataSources": [
    {
      "id": "ds-001",
      "name": "my-mysql",
      "type": "mysql",
      "host": "localhost",
      "port": 3306,
      "database": "test_db",
      "username": "root",
      "password": "your_password",
      "charset": "utf8mb4",
      "timeout": 30
    }
  ]
}
```

### API文档

启动后访问: http://localhost:8080/swagger-ui.html

## 工程结构

```
schemasync-backend/
├── src/main/java/com/schemasync/
│   ├── config/                 # 配置类
│   ├── controller/             # REST控制器
│   ├── service/                # 业务服务层
│   ├── adapter/                # 数据库适配器
│   ├── model/                  # 数据模型
│   │   ├── dict/               # 数据字典模型
│   │   ├── diff/               # 差异模型
│   │   └── config/             # 配置模型
│   ├── formatter/              # 格式化处理
│   ├── generator/              # DDL生成器
│   ├── differ/                 # 差异对比引擎
│   ├── cli/                    # 命令行接口
│   └── util/                   # 工具类
└── src/main/resources/
    ├── application.yml         # 应用配置
    └── schemasync-config.json  # 数据源配置模板
```

## 主要功能

### 1. 数据字典导出

```bash
POST /api/export
{
  "configName": "my-mysql",
  "format": "json",
  "database": "test_db"
}
```

### 2. 版本对比

```bash
POST /api/diff
# multipart/form-data 上传两个数据字典文件
```

### 3. 差异化DDL脚本生成

```bash
POST /api/generate
{
  "diff": {...},
  "databaseType": "mysql",
  "includeRollback": true
}
```

## 开发指南

### 添加新的数据库适配器

1. 实现 `DatabaseAdapter` 接口
2. 继承 `AbstractDatabaseAdapter` 抽象类
3. 使用 `@Component` 注解注册

示例:

```java
@Component
public class MyDatabaseAdapter extends AbstractDatabaseAdapter {

    @Override
    public String getDatabaseType() {
        return "MYDB";
    }

    // 实现其他方法...
}
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=SchemaExportServiceTest
```

## 部署

### JAR包部署

```bash
# 打包
mvn clean package -DskipTests

# 运行
java -jar target/schemasync-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker部署

```bash
# 构建镜像
docker build -t schemasync:latest .

# 运行
docker run -d -p 8080:8080 -v /data/config:/app/config schemasync:latest
```

## 许可证

Apache License 2.0

## 联系方式

- 项目地址: <repository-url>
- 问题反馈: <issues-url>

---

**SchemaSync Team** © 2026
