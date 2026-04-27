# SchemaSync - 轻量级数据字典管理工具

## 📖 项目简介

SchemaSync是一个轻量级数据字典管理工具,用于敏捷开发过程中的数据库变更管理。支持数据字典导出、版本差异对比、DDL脚本生成三大核心功能,帮助团队高效管理数据库 schema 变更。

---

## ✨ 核心功能

### 1. 数据字典导出

- 支持6种数据库: MySQL, Oracle, OceanBase, TDSQL, GaussDB, GoldenDB
- 导出格式: JSON, Excel
- 完整元数据: 表/字段/索引/外键信息

### 2. 版本差异对比

- 支持JSON/Excel文件对比
- 智能识别: 表/字段/索引/外键变更
- 破坏性变更标注

### 3. DDL脚本生成

- 自动生成变更SQL
- 包含回滚脚本
- 事务控制支持
- 破坏性变更保护

### 4. 数据源配置

- 可视化配置管理
- 连接测试
- 密码AES加密

---

## 🏗️ 技术架构

### 后端技术栈

- **框架**: Spring Boot 2.7.18
- **JDK**: 8
- **数据库**: MySQL (元数据存储)
- **连接池**: HikariCP
- **JSON**: Jackson
- **Excel**: Apache POI
- **加密**: AES-128

### 前端技术栈

- **框架**: Vue 3 (Composition API)
- **UI库**: Element Plus
- **构建**: Vite 5
- **HTTP**: Axios
- **路由**: Vue Router 4

### 架构模式

- **策略模式**: DatabaseAdapter接口,6种数据库实现
- **工厂模式**: DatabaseAdapterFactory自动识别
- **Builder模式**: Lombok @Builder

---

## 📦 项目结构

```
SchemaSync/
├── REQUIREMENTS.md          # 需求文档
├── DESIGN.md               # 设计文档
├── DATABASE_ADAPTERS.md    # 数据库适配器文档
├── PROJECT_SUMMARY.md      # 项目总结
├── SYSTEM_STARTUP.md       # 系统启动指南
├── QUICKSTART.md          # 快速开始
│
├── schemasync-backend/     # 后端项目
│   ├── src/main/java/
│   │   └── com/schemasync/
│   │       ├── adapter/        # 数据库适配器(6种)
│   │       ├── config/         # 配置管理
│   │       ├── controller/     # REST控制器
│   │       ├── differ/         # 差异对比
│   │       ├── formatter/      # 格式化器(JSON/Excel)
│   │       ├── generator/      # DDL生成
│   │       ├── model/          # 数据模型
│   │       ├── service/        # 业务服务
│   │       └── util/           # 工具类
│   └── src/test/java/        # 单元测试(48个)
│
└── schemasync-frontend/    # 前端项目
    ├── src/
    │   ├── api/              # API接口
    │   ├── components/       # 组件
    │   ├── router/           # 路由
    │   └── views/            # 页面(4个)
    └── package.json
```

---

## 🚀 快速开始

### 环境要求

- JDK 8+
- Node.js 16+
- Maven 3.6+

### 启动后端

```bash
cd schemasync-backend
mvn spring-boot:run
# 访问: http://localhost:8080
```

### 启动前端

```bash
cd schemasync-frontend
npm install
npm run dev
# 访问: http://localhost:3000
```

### 配置数据源

1. 访问前端页面
2. 进入"数据源配置"
3. 新增数据源,选择数据库类型
4. 填写连接信息,测试连接
5. 保存配置

### 导出数据字典

1. 进入"数据字典导出"
2. 选择数据源
3. 输入数据库名
4. 选择格式(JSON/Excel)
5. 点击下载

---

## 📊 数据库支持

| 数据库    | 协议           | JDBC驱动                 | 默认端口 | 状态 |
| --------- | -------------- | ------------------------ | -------- | ---- |
| MySQL     | MySQL原生      | com.mysql.cj.jdbc.Driver | 3306     | ✅   |
| Oracle    | Oracle原生     | oracle.jdbc.OracleDriver | 1521     | ✅   |
| OceanBase | MySQL协议      | com.mysql.cj.jdbc.Driver | 2883     | ✅   |
| TDSQL     | MySQL协议      | com.mysql.cj.jdbc.Driver | 3306     | ✅   |
| GaussDB   | PostgreSQL协议 | org.postgresql.Driver    | 5432     | ✅   |
| GoldenDB  | MySQL协议      | com.mysql.cj.jdbc.Driver | 3306     | ✅   |

---

## 📈 项目统计

### 代码统计

| 模块         | 行数       | 说明         |
| ------------ | ---------- | ------------ |
| 后端核心     | ~2,000     | 业务逻辑     |
| 数据库适配器 | ~1,909     | 6种数据库    |
| 前端代码     | ~800       | Vue3页面     |
| 测试代码     | ~1,710     | 单元测试     |
| **总计**     | **~6,419** | **完整项目** |

### 测试覆盖

- **单元测试**: 48个
- **通过率**: 100%
- **编译状态**: ✅ 成功

---

## 📚 文档索引

| 文档                                         | 说明                 |
| -------------------------------------------- | -------------------- |
| [REQUIREMENTS.md](REQUIREMENTS.md)           | 需求规格说明书       |
| [DESIGN.md](DESIGN.md)                       | 系统设计文档         |
| [DATABASE_ADAPTERS.md](DATABASE_ADAPTERS.md) | 数据库适配器详细说明 |
| [SYSTEM_STARTUP.md](SYSTEM_STARTUP.md)       | 系统启动和部署指南   |
| [QUICKSTART.md](QUICKSTART.md)               | 快速入门教程         |

---

## 🎯 设计亮点

### 1. 策略模式 + 工厂模式

```java
// 统一接口
public interface DatabaseAdapter {
    String getDatabaseType();
    SchemaDictionary exportSchema(...);
}

// 6种实现,自动注册
@Component public class MySQLAdapter implements DatabaseAdapter { ... }
@Component public class OracleAdapter implements DatabaseAdapter { ... }
// ... 其他4种

// 工厂自动识别
factory.getAdapter("OCEANBASE");  // ✅ 返回OceanBaseAdapter
```

### 2. 开闭原则 (OCP)

- ✅ 对扩展开放: 新增数据库只需实现接口
- ✅ 对修改封闭: 现有代码无需修改

### 3. 零侵入设计

- ✅ 不依赖目标数据库
- ✅ 配置文件本地化
- ✅ 导出不持久化

---

## 🔐 安全特性

- 密码AES-128加密存储
- 连接超时控制
- SQL注入防护(Prepared Statement)
- SSL连接支持(云数据库)

---

## 📝 许可证

Copyright © 2026 SchemaSync Team

---

**最后更新**: 2026-04-26
