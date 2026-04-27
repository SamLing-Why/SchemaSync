# SchemaSync 快速启动指南

## ✅ 第一步功能已完成

核心导出功能链路已实现:

- MySQL数据库适配器 ✅
- JSON格式化器 ✅
- 导出服务 ✅
- REST API接口 ✅
- 配置管理 ✅

## 🚀 快速测试

### 1. 编译项目

```bash
cd schemasync-backend
mvn clean compile
```

### 2. 配置数据源

编辑 `src/main/resources/schemasync-config.json`:

```json
{
  "version": "1.0.0",
  "dataSources": [
    {
      "id": "ds-001",
      "name": "test-mysql",
      "type": "mysql",
      "host": "localhost",
      "port": 3306,
      "database": "your_database",
      "username": "your_username",
      "password": "your_password",
      "charset": "utf8mb4",
      "timeout": 30
    }
  ]
}
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

或直接运行:

```bash
java -jar target/schemasync-backend-1.0.0-SNAPSHOT.jar
```

### 4. 测试API

#### 4.1 访问Swagger文档

打开浏览器: http://localhost:8080/swagger-ui.html

#### 4.2 使用curl测试

**获取所有配置:**

```bash
curl http://localhost:8080/api/config/datasources
```

**导出数据字典:**

```bash
curl -X POST "http://localhost:8080/api/export?configName=test-mysql&database=your_database&format=json" \
  -o schema_export.json
```

**查看导出的JSON:**

```bash
cat schema_export.json | jq .
```

### 5. API接口列表

#### 配置管理

- `GET /api/config/datasources` - 获取所有配置
- `GET /api/config/datasources/{id}` - 获取单个配置
- `POST /api/config/datasources` - 新增配置
- `PUT /api/config/datasources/{id}` - 更新配置
- `DELETE /api/config/datasources/{id}` - 删除配置
- `POST /api/config/datasources/test` - 测试连接

#### 数据导出

- `POST /api/export?configName={name}&database={db}&format=json` - 导出数据字典

## 📊 当前完成度

### 后端: ~45%

- ✅ 项目基础 (100%)
- ✅ 数据模型 (100%)
- ✅ 导出功能 (100%) - **可运行!**
- ✅ 配置管理 (100%) - **可运行!**
- ⏳ 对比功能 (0%)
- ⏳ DDL生成 (0%)

### 前端: 0%

- ⏳ 尚未开始

## 🎯 下一步

### 第二步: 差异对比功能

1. 实现DefaultSchemaDiffer (对比算法)
2. 实现SchemaDiffService (对比服务)
3. 实现DiffController (对比接口)
4. 测试对比功能

### 第三步: DDL生成

1. 实现MySQLDDLGenerator
2. 实现DDLGenerationService
3. 实现GenerateController

### 第四步: 前端开发

1. 初始化Vue项目
2. 开发页面
3. 对接API

## 📝 注意事项

1. **数据库权限**: 需要对INFORMATION_SCHEMA的SELECT权限
2. **密码加密**: 配置中的密码会自动AES加密存储
3. **配置文件**: 默认读取启动目录下的schemasync-config.json
4. **日志查看**: 日志输出到logs/schemasync.log

## 🐛 常见问题

**Q: 连接数据库失败?**
A: 检查配置中的host、port、username、password是否正确

**Q: 导出的数据为空?**
A: 检查database参数是否正确,数据库中是否有表

**Q: Swagger打不开?**
A: 确认应用已启动,访问http://localhost:8080/swagger-ui.html

## 💡 提示

- 使用Postman或Swagger UI测试更方便
- 查看日志文件排查问题: `tail -f logs/schemasync.log`
- 导出大型数据库可能需要一些时间,请耐心等待

---

**SchemaSync Team** © 2026
