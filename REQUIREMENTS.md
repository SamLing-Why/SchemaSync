# 轻量级数据字典管理工具 - 需求说明书

## 文档信息

- **项目名称**: SchemaSync - 轻量级数据字典管理工具
- **文档版本**: v1.1
- **创建日期**: 2026-04-26
- **适用范围**: 敏捷开发场景下的数据库变更管理

---

## 一、项目概述

### 1.1 背景

在敏捷开发过程中,数据库变更管理往往成为瓶颈。传统的变更管理流程复杂、代价高昂,难以适应快速迭代的开发节奏。为了解决这一问题,需要开发一个轻量级的数据字典管理工具,实现数据库结构的可视化管理、版本对比和自动化脚本生成。

### 1.2 目标

1. **简化数据库变更管理**: 通过数据字典版本对比,快速识别数据库结构变化
2. **降低变更成本**: 自动生成变更脚本,减少人工编写错误
3. **提升协作效率**: 标准化的数据字典格式,便于团队沟通和文档管理
4. **轻量化部署**: 最小化依赖,支持快速部署和使用

### 1.3 适用场景

- 敏捷开发团队的日常数据库变更管理
- 多环境数据库结构同步(开发→测试→生产)
- 数据库结构版本控制和审计
- 数据库文档自动生成

---

## 二、功能性需求

### 2.1 核心功能概览

| 功能模块     | 功能编号 | 功能名称         | 优先级 |
| ------------ | -------- | ---------------- | ------ |
| 数据源管理   | F-01     | 数据库连接配置   | P0     |
| 数据字典导出 | F-02     | 全量导出表结构   | P0     |
| 版本对比     | F-03     | 两版本差异分析   | P0     |
| 变更脚本生成 | F-04     | DDL脚本自动生成  | P0     |
| 扩展支持     | F-05     | 多数据库类型支持 | P0     |

### 2.2 功能详细说明

#### F-01: 数据库连接配置

**功能描述**: 支持配置和管理多个数据库连接,作为数据字典导出的数据源。

**功能要求**:

1. 支持配置以下连接参数:
   - 数据库类型(MySQL/Oracle/OceanBase/TDSQL/GaussDB/GoldenDB)
   - 主机地址、端口
   - 数据库名称
   - 用户名、密码(加密存储)
   - 连接参数(字符集、超时时间等)

2. 连接测试功能:
   - 验证连接参数是否正确
   - 显示数据库版本信息
   - 显示可访问的Schema列表

3. 连接管理:
   - 支持保存多个连接配置
   - 支持配置别名和分组
   - 支持导入/导出配置文件

**输入**: 数据库连接参数
**输出**: 连接状态、数据库元信息
**异常处理**: 连接失败、权限不足、网络超时等

---

#### F-02: 全量导出表结构

**功能描述**: 连接数据库后,导出指定数据库的所有表及其字段的详细信息,生成数据字典文件。

**功能要求**:

1. 导出内容:
   - **概述信息**:
   - 数据库类型
   - 数据库版本
   - 数据库名称
   - 数据库实例名称
   - 导出的日期时间
   - 工具版本
   - **表级别信息**:
   - 表名
   - 表注释/说明
   - 表类型(普通表/视图/临时表)
   - 创建时间、更新时间
   - 存储引擎(MySQL)
   - 字符集和排序规则
   - **字段级别信息**:
   - 表名
   - 字段名称
   - 数据类型(不含长度、精度)
   - 长度
   - 精度(数值型字段)
   - 是否允许NULL
   - 默认值
   - 是否主键
   - 是否自增
   - 字段注释/说明
   - 字符集(字段级别)
   - **索引信息**:
   - 表名
   - 索引名称
   - 索引类型(主键/唯一/普通/全文)
   - 索引字段及顺序
   - 索引备注
   - **约束信息**:
   - 表名
   - 外键约束(引用表、引用字段、级联规则)
   - 唯一约束
   - 检查约束
   - **视图定义**(如适用):
   - 视图SQL定义
2. 导出范围控制:
   - 支持选择特定Schema/数据库
   - 支持按表名模式过滤(支持通配符)
   - 支持排除特定表

3. 导出文件格式:
   - JSON格式(推荐,结构化,便于程序处理)
   - Excel格式(便于非技术人员查看)

**数据字典文件结构示例(JSON)**:

```json
{
  "metadata": {
    "exportTime": "2026-04-26T10:30:00Z",
    "databaseType": "MySQL",
    "databaseVersion": "8.0.32",
    "databaseName": "mydb",
    "schemaName": "public",
    "toolVersion": "1.0.0"
  },
  "tables": [
    {
      "tableName": "users",
      "tableComment": "用户表",
      "tableType": "BASE TABLE",
      "engine": "InnoDB",
      "charset": "utf8mb4",
      "createTime": "2026-01-15T08:00:00Z",
      "updateTime": "2026-04-20T14:30:00Z",
      "columns": [
        {
          "columnName": "id",
          "dataType": "BIGINT",
          "length": 20,
          "precision": null,
          "scale": null,
          "nullable": false,
          "defaultValue": null,
          "isPrimaryKey": true,
          "isAutoIncrement": true,
          "comment": "用户ID",
          "ordinalPosition": 1
        },
        {
          "columnName": "username",
          "dataType": "VARCHAR",
          "length": 100,
          "precision": null,
          "scale": null,
          "nullable": false,
          "defaultValue": null,
          "comment": "用户名",
          "ordinalPosition": 2
        }
      ],
      "indexes": [
        {
          "indexName": "idx_username",
          "indexType": "UNIQUE",
          "columns": ["username"],
          "isUnique": true,
          "comment": "用户名唯一索引"
        }
      ],
      "foreignKeys": [
        {
          "constraintName": "fk_dept_id",
          "columnName": "department_id",
          "referencedTable": "departments",
          "referencedColumn": "id",
          "onUpdate": "CASCADE",
          "onDelete": "RESTRICT"
        }
      ]
    }
  ]
}
```

**输入**: 数据库连接、导出范围配置、输出格式
**输出**: 数据字典文件
**异常处理**: 权限不足、表不存在、导出中断等

---

#### F-03: 两版本差异分析

**功能描述**: 对比两个版本的数据字典文件,识别数据库结构的差异,生成差异报告。

**功能要求**:

1. 差异类型识别:

   **表级别差异**:
   - 新增表
   - 删除表
   - 表属性变更(注释、引擎、字符集等)

   **字段级别差异**:
   - 新增字段
   - 删除字段
   - 字段属性变更:
     - 数据类型变更
     - 长度变更
     - 精度/小数位数变更
     - NULL约束变更
     - 默认值变更
     - 注释变更
     - 字段位置变更(如适用)

   **索引差异**:
   - 新增索引
   - 删除索引
   - 索引变更(字段、类型)

   **约束差异**:
   - 新增外键
   - 删除外键
   - 外键变更
   - 其他约束变更

2. 差异详细记录:
   - 变更类型(ADD/DROP/ALTER)
   - 变更对象(表名、字段名等)
   - 变更前值(oldValue)
   - 变更后值(newValue)
   - 变更影响评估(破坏性变更标记)

3. 差异报告格式:
   - JSON格式(结构化,便于程序处理)
   - Excel格式(便于人工审查和分发)

4. 差异统计:
   - 新增表数量
   - 删除表数量
   - 修改表数量
   - 字段变更总数
   - 破坏性变更数量

5. 输入文件支持:
   - 支持上传2个JSON文件进行对比
   - 支持上传2个Excel文件进行对比

**差异文件结构示例(JSON)**:

```json
{
  "diffMetadata": {
    "generatedTime": "2026-04-26T11:00:00Z",
    "sourceVersion": "v1.0-20260420",
    "targetVersion": "v1.1-20260426",
    "sourceFile": "schema_v1.0.json",
    "targetFile": "schema_v1.1.json",
    "toolVersion": "1.0.0"
  },
  "summary": {
    "tablesAdded": 2,
    "tablesDropped": 1,
    "tablesModified": 3,
    "columnsAdded": 5,
    "columnsDropped": 2,
    "columnsModified": 4,
    "indexesAdded": 3,
    "indexesDropped": 1,
    "foreignKeysAdded": 2,
    "foreignKeysDropped": 0,
    "breakingChanges": 3
  },
  "changes": [
    {
      "changeType": "TABLE_ADD",
      "tableName": "user_sessions",
      "severity": "NON_BREAKING",
      "details": {
        "tableComment": "用户会话表",
        "columns": 5,
        "indexes": 2
      }
    },
    {
      "changeType": "COLUMN_MODIFY",
      "tableName": "users",
      "columnName": "email",
      "severity": "BREAKING",
      "details": {
        "property": "length",
        "oldValue": 100,
        "newValue": 255,
        "impact": "扩展字段长度,可能影响索引"
      }
    },
    {
      "changeType": "COLUMN_DROP",
      "tableName": "users",
      "columnName": "legacy_field",
      "severity": "BREAKING",
      "details": {
        "oldDefinition": {
          "dataType": "VARCHAR",
          "length": 50,
          "nullable": true,
          "comment": "旧字段"
        },
        "impact": "删除字段,可能导致数据丢失"
      }
    }
  ]
}
```

**破坏性变更定义**:

- 删除表/字段(数据丢失风险)
- 缩小字段长度(数据截断风险)
- 变更数据类型(可能需要数据转换)
- 删除索引(性能影响)
- 添加NOT NULL约束(需要处理现有NULL值)

**输入**: 两个版本的数据字典文件(JSON或Excel)
**输出**: 差异报告文件(JSON或Excel)
**异常处理**: 文件格式不匹配、版本过旧、文件损坏等

---

#### F-04: DDL脚本自动生成

**功能描述**: 基于差异文件,自动生成数据库变更DDL脚本,将旧版本结构升级到新版本。

**功能要求**:

1. 生成的DDL语句类型:

   **表操作**:

   ```sql
   CREATE TABLE table_name (...);
   DROP TABLE table_name;
   ALTER TABLE table_name COMMENT '新注释';
   ```

   **字段操作**:

   ```sql
   ALTER TABLE table_name ADD COLUMN column_name datatype ...;
   ALTER TABLE table_name DROP COLUMN column_name;
   ALTER TABLE table_name MODIFY COLUMN column_name new_datatype ...;
   ALTER TABLE table_name CHANGE COLUMN old_name new_name datatype ...;
   ALTER TABLE table_name ALTER COLUMN column_name SET DEFAULT ...;
   ```

   **索引操作**:

   ```sql
   CREATE INDEX idx_name ON table_name(column_name);
   CREATE UNIQUE INDEX idx_name ON table_name(column_name);
   DROP INDEX idx_name ON table_name;
   ```

   **约束操作**:

   ```sql
   ALTER TABLE table_name ADD CONSTRAINT fk_name FOREIGN KEY (...) REFERENCES ...;
   ALTER TABLE table_name DROP FOREIGN KEY fk_name;
   ```

2. 脚本生成策略:
   - **执行顺序优化**:
     - 先创建新表(无外键依赖)
     - 再添加字段和索引
     - 最后添加外键约束
     - 删除操作放在最后
   - **破坏性变更保护**:
     - 对破坏性变更添加警告注释
     - 可选生成回滚脚本
     - 可选生成分步执行脚本
   - **数据库方言适配**:
     - 根据目标数据库类型生成对应语法
     - 处理不同数据库的语法差异

3. 脚本输出格式:
   - 纯SQL文件(.sql)
   - 带执行顺序编号的脚本文件
   - 可选生成回滚脚本

4. 脚本内容要求:
   - 添加详细注释说明每个变更
   - 标注破坏性变更
   - 添加事务控制(如数据库支持)
   - 添加执行前检查语句

**生成脚本示例**:

```sql
-- ============================================
-- SchemaSync 自动生成的数据库变更脚本
-- 源版本: v1.0-20260420
-- 目标版本: v1.1-20260426
-- 生成时间: 2026-04-26 11:30:00
-- 数据库类型: MySQL 8.0
-- 变更统计: 新增2表, 删除1表, 修改3表
-- 破坏性变更: 3处(请仔细审查)
-- ============================================

-- 建议在非生产环境先执行测试
-- 执行前请备份数据库

START TRANSACTION;

-- ============================================
-- 1. 新增表
-- ============================================

-- 新增表: user_sessions (用户会话表)
CREATE TABLE IF NOT EXISTS `user_sessions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_token` VARCHAR(255) NOT NULL COMMENT '会话令牌',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_session_token` (`session_token`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';

-- 新增表: audit_logs (审计日志表)
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- ============================================
-- 2. 修改表结构
-- ============================================

-- [警告: 破坏性变更] 修改字段: users.email 长度变更
-- 影响: 扩展字段长度,可能影响索引性能
ALTER TABLE `users` MODIFY COLUMN `email` VARCHAR(255) NOT NULL COMMENT '邮箱地址';

-- 新增字段: users.phone
ALTER TABLE `users` ADD COLUMN `phone` VARCHAR(20) NULL COMMENT '手机号码' AFTER `email`;

-- [警告: 破坏性变更] 删除字段: users.legacy_field
-- 影响: 字段将被永久删除,请确认数据已迁移
-- ALTER TABLE `users` DROP COLUMN `legacy_field`;  -- 请手动取消注释确认

-- ============================================
-- 3. 索引变更
-- ============================================

-- 新增索引: users.idx_phone
CREATE INDEX `idx_phone` ON `users` (`phone`);

-- 删除索引: users.idx_old_field
DROP INDEX `idx_old_field` ON `users`;

-- ============================================
-- 4. 外键约束
-- ============================================

-- 新增外键: user_sessions.user_id -> users.id
ALTER TABLE `user_sessions`
ADD CONSTRAINT `fk_session_user`
FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- ============================================
-- 5. 删除表(最后执行)
-- ============================================

-- [警告: 破坏性变更] 删除表: old_temp_table
-- 影响: 表及其所有数据将被永久删除
-- DROP TABLE IF EXISTS `old_temp_table`;  -- 请手动取消注释确认

COMMIT;

-- ============================================
-- 执行完成后请验证:
-- 1. 检查表结构是否正确
-- 2. 检查数据是否完整
-- 3. 检查应用功能是否正常
-- ============================================
```

**输入**: 差异文件、目标数据库类型、生成选项
**输出**: DDL脚本文件、可选回滚脚本
**异常处理**: 无法生成合法SQL、语法不支持等

---

#### F-05: 多数据库类型支持

**功能描述**: 工具需支持多种主流数据库,并提供扩展机制以便未来支持更多数据库。

**支持的数据库**:

1. MySQL 5.7 / 8.0
2. Oracle 12c+
3. OceanBase 3.x / 4.x
4. TDSQL (腾讯分布式数据库)
5. GaussDB (华为高斯数据库)
6. GoldenDB (中兴分布式数据库)

**数据库扩展架构要求**:

- 采用插件化/策略模式设计
- 定义统一的数据库适配器接口
- 每种数据库实现独立的适配器
- 适配器负责:
  - 连接管理
  - 元数据查询(SQL方言适配)
  - DDL语句生成
  - 数据类型映射

---

### 2.3 辅助功能

#### F-07: 命令行接口(CLI)

**功能描述**: 提供命令行工具,便于集成到CI/CD流程。

**功能要求**:

1. 支持的命令:

   ```bash
   # 导出数据库结构
   schemasync export --config db.json --output schema_v1.json --format json

   # 对比两个版本(支持JSON和Excel)
   schemasync diff --old schema_v1.json --new schema_v2.json --output diff.json --input-format json

   # 生成变更脚本
   schemasync generate --diff diff.json --output migration.sql --db-type mysql

   # 测试数据库连接
   schemasync test-connection --config db.json
   ```

2. 参数配置:
   - 支持命令行参数
   - 支持配置文件(JSON,存储在当前启动目录)
   - 支持环境变量

3. 退出码规范:
   - 0: 成功
   - 1: 一般错误
   - 2: 参数错误
   - 3: 数据库连接失败

---

## 三、技术选型与非功能性需求

### 3.1 技术栈

**后端技术**:

- **开发语言**: Java
- **JDK版本**: JDK 8
- **构建工具**: Maven 3.6+
- **核心框架**: Spring Boot 2.7.x
- **数据库连接**:
  - MySQL: MySQL Connector/J 8.0
  - Oracle: Oracle JDBC Driver (ojdbc8)
  - OceanBase: 兼容MySQL协议,使用MySQL Connector/J
  - TDSQL: 兼容MySQL协议,使用MySQL Connector/J
  - GaussDB: 兼容PostgreSQL协议,使用PostgreSQL JDBC Driver
  - GoldenDB: 兼容MySQL协议,使用MySQL Connector/J
- **Excel处理**: Apache POI 5.x
- **JSON处理**: Jackson / Fastjson2
- **CLI框架**: Spring Shell / Picocli

**前端技术**:

- **框架**: Vue.js 2.x/3.x
- **UI组件库**: Element Plus / Ant Design Vue
- **构建工具**: Vue CLI / Vite
- **状态管理**: Vuex / Pinia
- **路由管理**: Vue Router

**开发工具**:

- **IDE**: IntelliJ IDEA / VS Code
- **版本控制**: Git
- **接口测试**: Postman / Swagger

### 3.2 性能需求

| 指标     | 要求           | 说明                        |
| -------- | -------------- | --------------------------- |
| 导出性能 | ≤10秒/100表    | 100个表以内的数据库结构导出 |
| 对比性能 | ≤5秒/500表差异 | 两个版本各500表的差异分析   |
| 脚本生成 | ≤2秒           | 基于差异文件生成DDL脚本     |
| 内存占用 | ≤256MB         | 正常运行时内存占用          |
| 启动时间 | ≤3秒           | 工具启动到可用状态          |

### 3.3 可靠性需求

1. **数据完整性**:
   - 导出过程不丢失任何表/字段/索引信息
   - 对比过程准确识别所有差异
   - 生成的DDL脚本可执行且结果正确

2. **异常处理**:
   - 所有异常需有明确的错误提示
   - 网络中断可重试(至少3次)
   - 文件操作失败有回滚机制

3. **容错能力**:
   - 部分表导出失败不影响其他表
   - 支持断点续传(大库导出场景)

### 3.4 可维护性需求

1. **代码质量**:
   - 代码注释覆盖率 ≥80%
   - 单元测试覆盖率 ≥70%
   - 遵循统一的代码规范

2. **日志记录**:
   - 记录关键操作日志
   - 支持日志级别配置(DEBUG/INFO/WARN/ERROR)
   - 日志包含时间戳、操作类型、结果状态

3. **版本管理**:
   - 遵循语义化版本号(Semantic Versioning)
   - 提供CHANGELOG记录变更

### 3.5 轻量化部署需求

#### 3.5.1 部署方式

**方式一: 独立可执行文件(推荐)**

- 打包为单一可执行文件
- 无需安装运行时环境
- 支持平台:
  - Windows (x64)
  - Linux (x64, ARM64)
  - macOS (x64, ARM64)

**方式二: Docker容器**

- 提供官方Docker镜像
- 镜像大小 ≤100MB
- 支持docker-compose快速启动

**方式三: 包管理器安装**

- 提供主流包管理器安装方式
- 支持自动化安装和更新

#### 3.5.2 依赖最小化

1. **运行时依赖**:
   - 无外部服务依赖(数据库除外)
   - 不需要安装JVM/.NET等重型运行时
   - 数据库驱动按需加载
   - **不依赖本地数据库**,数据源配置存储在启动目录的JSON文件中
   - 导出的数据字典和差异文件不做本地持久化存储,由用户自行管理

2. **安装包大小**:
   - 独立可执行文件 ≤50MB
   - 包含所有必需的数据库驱动

3. **系统要求**:
   - 最低内存: 128MB
   - 最低磁盘空间: 100MB
   - 支持的操作系统:
     - Windows 10+
     - Linux (内核3.10+)
     - macOS 10.15+

#### 3.5.3 无侵入性

- 不需要在目标数据库中创建任何表或对象
- 只需要SELECT权限即可导出(仅读权限)
- 不影响目标数据库的性能和运行

### 3.6 可扩展性需求

#### 3.6.1 数据库扩展

1. **扩展机制**:
   - 插件化架构,支持动态加载数据库驱动
   - 提供SDK和开发文档,便于社区贡献
   - 定义清晰的适配器接口规范

2. **扩展开发指南**:
   - 提供模板项目
   - 提供示例实现
   - 提供测试框架

3. **扩展发布**:
   - 支持独立发布数据库适配器插件
   - 提供插件市场/仓库(未来规划)

#### 3.6.2 功能扩展

1. **输出格式扩展**:
   - 支持自定义输出格式插件
   - 模板引擎支持

2. **集成扩展**:
   - CI/CD集成(GitLab CI, GitHub Actions, Jenkins)
   - 版本控制系统集成(Git hook)
   - 项目管理工具集成(Jira, Confluence)

### 3.7 安全性需求

1. **敏感信息保护**:
   - 数据库密码加密存储(使用AES-256或更高级加密)
   - 配置文件权限控制
   - 不在日志中输出密码等敏感信息

2. **脚本执行安全**:
   - 生成的DDL脚本默认包含确认注释
   - 破坏性变更需手动确认
   - 提供回滚脚本

3. **数据安全**:
   - 仅读取元数据,不读取业务数据
   - 导出文件不包含实际数据记录
   - 临时文件及时清理

### 3.8 兼容性需求

1. **向后兼容**:
   - 新版本能读取旧版本生成的数据字典文件
   - 配置文件格式变更提供迁移工具

2. **数据库版本兼容**:
   - 明确标注支持的数据库版本范围
   - 对不支持的特性有降级处理

### 3.9 可用性需求

1. **文档完善**:
   - 快速开始指南
   - 详细使用文档
   - API参考文档(如提供SDK)
   - 常见问题FAQ

2. **错误提示友好**:
   - 错误信息清晰,包含原因和建议解决方案
   - 提供错误码和文档链接

3. **社区支持**:
   - 问题反馈渠道
   - 讨论区/论坛
   - 贡献指南

---

## 四、测试策略

### 4.1 单元测试

1. **覆盖范围**:
   - 数据模型序列化和反序列化
   - 差异对比算法
   - DDL生成逻辑
   - 格式转换
   - 配置解析

2. **测试工具**:
   - 使用主流单元测试框架
   - 支持代码覆盖率统计

### 4.2 集成测试

1. **数据库集成测试**:
   - 使用Docker启动测试数据库
   - 创建测试Schema
   - 验证导出准确性
   - 验证生成的DDL可执行

2. **端到端测试**:
   - 完整流程测试: 导出→修改→对比→生成→执行

### 4.3 兼容性测试

1. **数据库版本测试**:
   - MySQL 5.7, 8.0
   - Oracle 12c, 19c
   - OceanBase 3.x, 4.x
   - TDSQL(最新版本)
   - GaussDB(最新版本)
   - GoldenDB(最新版本)

2. **操作系统测试**:
   - Windows 10/11
   - Ubuntu 20.04/22.04
   - macOS 12+

### 4.4 性能测试

1. **大数据量测试**:
   - 1000个表的数据库
   - 500个表的差异对比
   - 内存和CPU监控

---

## 五、风险和应对策略

| 风险                                   | 影响 | 概率 | 应对策略                                                     |
| -------------------------------------- | ---- | ---- | ------------------------------------------------------------ |
| 数据库元数据查询不完整                 | 高   | 中   | 充分测试各版本数据库,建立元数据查询测试用例,特别是国产数据库 |
| DDL脚本执行失败                        | 高   | 中   | 提供回滚脚本,破坏性变更默认注释,建议先测试                   |
| 大数据量导出性能差                     | 中   | 低   | 优化查询,支持并发,分页查询                                   |
| 不同数据库语法差异大(特别是国产数据库) | 高   | 高   | 完善的适配器测试,明确标注支持范围,与数据库厂商获取技术文档   |
| 社区贡献质量参差不齐                   | 低   | 中   | 严格的代码审查,自动化测试,贡献指南                           |

---

## 六、成功标准

1. **功能完整性**: 所有P0功能实现并通过测试
2. **性能达标**: 满足性能指标要求
3. **用户友好**: 清晰的文档,友好的错误提示
4. **扩展性**: 至少6种数据库支持(MySQL/Oracle/OceanBase/TDSQL/GaussDB/GoldenDB),扩展机制验证可行
5. **轻量化**: 安装包≤50MB,内存≤256MB,无需重型运行时
6. **可靠性**: 核心功能单元测试覆盖率≥70%,无严重bug

---

## 七、附录

### 7.1 术语表

| 术语         | 定义                                                       |
| ------------ | ---------------------------------------------------------- |
| 数据字典     | 数据库结构的元数据描述,包含表、字段、索引等信息            |
| 破坏性变更   | 可能导致数据丢失或应用故障的变更,如删除字段、缩小长度      |
| 数据库适配器 | 实现统一接口,屏蔽不同数据库差异的组件                      |
| DDL          | Data Definition Language,数据定义语言(CREATE/ALTER/DROP)   |
| Schema       | 数据库的逻辑结构,在MySQL中通常指database,在PG中指namespace |

### 7.2 参考资料

- MySQL Information Schema: https://dev.mysql.com/doc/refman/8.0/en/information-schema.html
- Oracle Data Dictionary: https://docs.oracle.com/en/database/oracle/oracle-database/19/refrn/index.html
- OceanBase文档: https://www.oceanbase.com/docs
- TDSQL文档: https://cloud.tencent.com/document/product/557
- GaussDB文档: https://support.huaweicloud.com/gaussdb/index.html
- GoldenDB文档: 联系中兴通讯获取
- Semantic Versioning: https://semver.org/
- Twelve-Factor App: https://12factor.net/

### 7.3 版本历史

| 版本 | 日期       | 变更说明                             | 作者 |
| ---- | ---------- | ------------------------------------ | ---- |
| v1.0 | 2026-04-26 | 初始版本                             | -    |
| v1.1 | 2026-04-26 | 清理技术细节,精简功能,更新数据库类型 | -    |

---

## 文档审批

| 角色       | 姓名 | 签字 | 日期 |
| ---------- | ---- | ---- | ---- |
| 产品经理   |      |      |      |
| 技术负责人 |      |      |      |
| 架构师     |      |      |      |

---

**文档结束**

_本需求说明书为开发团队提供设计和开发的指导,如有变更请及时更新文档并通知相关人员。_
