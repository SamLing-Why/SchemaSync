# SchemaSync 后端工程结构说明

## 已创建的文件和目录

### 根目录

```
schemasync-backend/
├── pom.xml                          ✅ Maven配置文件
├── README.md                        ✅ 项目说明文档
├── .gitignore                       ✅ Git忽略配置
└── src/
```

### 源代码目录

```
src/main/java/com/schemasync/
├── SchemaSyncApplication.java       ✅ Spring Boot启动类
├── config/                          ✅ 配置类目录
│   ├── WebConfig.java               ✅ Web配置(CORS)
│   └── SwaggerConfig.java           ✅ Swagger配置
├── model/                           ✅ 数据模型目录
│   ├── dict/                        ✅ 数据字典模型
│   │   ├── ExportMetadata.java      ✅ 导出元数据
│   │   ├── ColumnDefinition.java    ✅ 字段定义
│   │   ├── IndexDefinition.java     ✅ 索引定义
│   │   ├── ForeignKeyDefinition.java✅ 外键定义
│   │   ├── TableDefinition.java     ✅ 表定义
│   │   └── SchemaDictionary.java    ✅ 数据字典
│   ├── diff/                        ✅ 差异模型
│   │   ├── ChangeType.java          ✅ 变更类型枚举
│   │   ├── Severity.java            ✅ 严重程度枚举
│   │   ├── SchemaChange.java        ✅ 变更项
│   │   ├── DiffSummary.java         ✅ 差异统计
│   │   ├── DiffMetadata.java        ✅ 差异元数据
│   │   └── SchemaDiff.java          ✅ 数据字典差异
│   └── config/                      ✅ 配置模型
│       └── DataSourceConfig.java    ✅ 数据源配置
└── adapter/                         ✅ 数据库适配器目录
    ├── DatabaseAdapter.java         ✅ 适配器接口
    └── ExportOptions.java           ✅ 导出选项
```

### 资源文件目录

```
src/main/resources/
├── application.yml                  ✅ 应用配置
├── application-dev.yml              ✅ 开发环境配置
├── application-prod.yml             ✅ 生产环境配置
└── schemasync-config.json           ✅ 数据源配置模板
```

## 待创建的目录和文件

### Controller层 (REST控制器)

```
src/main/java/com/schemasync/controller/
├── ExportController.java            ⏳ 导出接口
├── DiffController.java              ⏳ 对比接口
├── GenerateController.java          ⏳ 脚本生成接口
└── ConfigController.java            ⏳ 配置管理接口
```

### Service层 (业务服务)

```
src/main/java/com/schemasync/service/
├── SchemaExportService.java         ⏳ 导出服务
├── SchemaDiffService.java           ⏳ 对比服务
├── DDLGenerationService.java        ⏳ DDL生成服务
└── ConfigService.java               ⏳ 配置管理服务
```

### Adapter实现 (数据库适配器)

```
src/main/java/com/schemasync/adapter/
├── AbstractDatabaseAdapter.java     ⏳ 抽象基类
├── DatabaseAdapterFactory.java      ⏳ 适配器工厂
├── MySQLAdapter.java                ⏳ MySQL适配器
├── OracleAdapter.java               ⏳ Oracle适配器
├── OceanBaseAdapter.java            ⏳ OceanBase适配器
├── TDSQLAdapter.java                ⏳ TDSQL适配器
├── GaussDBAdapter.java              ⏳ GaussDB适配器
└── GoldenDBAdapter.java             ⏳ GoldenDB适配器
```

### Formatter (格式化器)

```
src/main/java/com/schemasync/formatter/
├── SchemaFormatter.java             ⏳ 格式化器接口
├── JsonFormatter.java               ⏳ JSON格式化
└── ExcelFormatter.java              ⏳ Excel格式化
```

### Generator (DDL生成器)

```
src/main/java/com/schemasync/generator/
├── DDLGenerator.java                ⏳ DDL生成器接口
├── AbstractDDLGenerator.java        ⏳ 抽象基类
├── DDLGeneratorFactory.java         ⏳ 生成器工厂
├── MySQLDDLGenerator.java           ⏳ MySQL DDL生成
└── OracleDDLGenerator.java          ⏳ Oracle DDL生成
```

### Differ (差异对比)

```
src/main/java/com/schemasync/differ/
├── SchemaDiffer.java                ⏳ 对比器接口
└── DefaultSchemaDiffer.java         ⏳ 默认实现
```

### CLI (命令行接口)

```
src/main/java/com/schemasync/cli/
├── SchemaSyncShell.java             ⏳ Shell主类
├── ExportCommand.java               ⏳ 导出命令
├── DiffCommand.java                 ⏳ 对比命令
├── GenerateCommand.java             ⏳ 生成命令
└── TestConnectionCommand.java       ⏳ 测试连接命令
```

### Util (工具类)

```
src/main/java/com/schemasync/util/
├── CryptoUtil.java                  ⏳ 加密工具
├── FileUtil.java                    ⏳ 文件工具
├── SqlUtil.java                     ⏳ SQL工具
└── DateUtil.java                    ⏳ 日期工具
```

### 测试目录

```
src/test/java/com/schemasync/
├── controller/                      ⏳ 控制器测试
├── service/                         ⏳ 服务测试
├── adapter/                         ⏳ 适配器测试
├── formatter/                       ⏳ 格式化器测试
├── generator/                       ⏳ 生成器测试
└── differ/                          ⏳ 对比器测试

src/test/resources/
├── test-config.json                 ⏳ 测试配置
└── test-data/                       ⏳ 测试数据
    ├── mysql-sample.json
    ├── oracle-sample.json
    └── diff-sample.json
```

## 当前状态

✅ 已完成:

- Maven项目配置(pom.xml)
- Spring Boot启动类
- 核心配置类(Web、Swagger)
- 完整的数据模型(dict、diff、config)
- 数据库适配器接口
- 应用配置文件(开发/生产环境)
- README和.gitignore

⏳ 待开发:

- Controller层(4个控制器)
- Service层(4个服务)
- 数据库适配器实现(6种数据库)
- 格式化器(JSON、Excel)
- DDL生成器
- 差异对比引擎
- CLI命令行工具
- 工具类
- 单元测试

## 下一步建议

1. **优先实现**:
   - ConfigService (配置管理)
   - SchemaExportService (导出服务)
   - MySQLAdapter (MySQL适配器)
   - JsonFormatter (JSON格式化)

2. **然后实现**:
   - ExportController (导出接口)
   - SchemaDiffService (对比服务)
   - DefaultSchemaDiffer (对比引擎)

3. **最后实现**:
   - DDLGenerationService (DDL生成)
   - 其他数据库适配器
   - CLI命令行工具
   - ExcelFormatter

## 编译和运行

```bash
# 进入项目目录
cd schemasync-backend

# 编译项目
mvn clean compile

# 打包
mvn clean package -DskipTests

# 运行
java -jar target/schemasync-backend-1.0.0-SNAPSHOT.jar

# 或使用Maven插件运行
mvn spring-boot:run
```

启动后访问: http://localhost:8080/swagger-ui.html 查看API文档
