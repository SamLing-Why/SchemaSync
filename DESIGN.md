# SchemaSync - 设计文档

## 文档信息

- **项目名称**: SchemaSync - 轻量级数据字典管理工具
- **文档版本**: v1.2
- **创建日期**: 2026-04-26
- **最后更新**: 2026-04-29
- **基于文档**: REQUIREMENTS.md v1.1
- **当前版本**: 1.0.0-SNAPSHOT
- **Git提交**: 03dca90

---

## 项目概述

SchemaSync是一个轻量级数据字典管理工具，支持多数据库适配器的数据字典导出、版本对比和DDL脚本生成功能。

### 核心特性

1. **多数据库支持**：MySQL、Oracle、OceanBase、TDSQL、GaussDB、GoldenDB
2. **数据字典导出**：支持JSON和Excel格式
3. **版本对比**：对比两个版本的数据字典，生成差异报告
4. **DDL生成**：根据差异生成增量DDL脚本
5. **前后端一体化**：打包为单个JAR包，便于部署
6. **外置配置**：支持application.yml外置配置

---

## 一、系统架构设计

### 1.1 整体架构

系统采用前后端分离架构,后端提供RESTful API和CLI命令行接口,前端提供Web操作界面。

```
┌─────────────────────────────────────────────────────┐
│                   用户层                              │
│  ┌──────────────┐              ┌──────────────────┐  │
│  │   Web UI     │              │   CLI 命令行     │  │
│  │  (Vue + El)  │              │  (Spring Shell)  │  │
│  └──────────────┘              └──────────────────┘  │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│                   API层                               │
│  ┌──────────────────────────────────────────────┐   │
│  │         Spring Boot REST API                 │   │
│  │  /api/export  /api/diff  /api/generate       │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│                   业务层                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │ 导出服务  │ │ 对比服务  │ │ 生成服务  │            │
│  │ExportSvc │ │DiffSvc   │ │GenSvc    │            │
│  └──────────┘ └──────────┘ └──────────┘            │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│                   数据访问层                          │
│  ┌──────────────────────────────────────────────┐   │
│  │        数据库适配器 (Adapter Pattern)         │   │
│  │  MySQL │ Oracle │ OceanBase │ TDSQL │ ...    │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│               外部数据源 (只读)                       │
│  MySQL │ Oracle │ OceanBase │ TDSQL │ GaussDB │ ... │
└─────────────────────────────────────────────────────┘
```

### 1.2 架构特点

1. **分层架构**: 表现层 → API层 → 业务层 → 数据访问层 → 外部数据源
2. **插件化设计**: 数据库适配器采用策略模式,支持动态扩展
3. **无状态服务**: 业务处理无状态,便于水平扩展
4. **轻量化**: 不依赖本地数据库,配置存储在JSON文件

---

## 二、工程目录结构

### 2.1 后端工程结构 (schemasync-backend)

```
schemasync-backend/
├── pom.xml                          # Maven配置
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/schemasync/
│   │   │   ├── SchemaSyncApplication.java          # Spring Boot启动类
│   │   │   │
│   │   │   ├── config/                             # 配置类
│   │   │   │   ├── WebConfig.java                  # Web配置(CORS等)
│   │   │   │   ├── DataSourceConfig.java           # 数据源配置
│   │   │   │   └── SwaggerConfig.java              # API文档配置
│   │   │   │
│   │   │   ├── controller/                         # REST控制器
│   │   │   │   ├── ExportController.java           # 导出接口
│   │   │   │   ├── DiffController.java             # 对比接口
│   │   │   │   ├── GenerateController.java         # 脚本生成接口
│   │   │   │   └── ConfigController.java           # 配置管理接口
│   │   │   │
│   │   │   ├── service/                            # 业务服务层
│   │   │   │   ├── SchemaExportService.java        # 导出服务
│   │   │   │   ├── SchemaDiffService.java          # 对比服务
│   │   │   │   ├── DDLGenerationService.java       # DDL生成服务
│   │   │   │   └── ConfigService.java              # 配置管理服务
│   │   │   │
│   │   │   ├── adapter/                            # 数据库适配器(策略模式)
│   │   │   │   ├── DatabaseAdapter.java            # 适配器接口
│   │   │   │   ├── AbstractDatabaseAdapter.java    # 抽象基类
│   │   │   │   ├── MySQLAdapter.java               # MySQL适配器
│   │   │   │   ├── OracleAdapter.java              # Oracle适配器
│   │   │   │   ├── OceanBaseAdapter.java           # OceanBase适配器
│   │   │   │   ├── TDSQLAdapter.java               # TDSQL适配器
│   │   │   │   ├── GaussDBAdapter.java             # GaussDB适配器
│   │   │   │   ├── GoldenDBAdapter.java            # GoldenDB适配器
│   │   │   │   └── DatabaseAdapterFactory.java     # 适配器工厂
│   │   │   │
│   │   │   ├── model/                              # 数据模型
│   │   │   │   ├── dict/                           # 数据字典模型
│   │   │   │   │   ├── SchemaDictionary.java       # 数据字典
│   │   │   │   │   ├── TableDefinition.java        # 表定义
│   │   │   │   │   ├── ColumnDefinition.java       # 字段定义
│   │   │   │   │   ├── IndexDefinition.java        # 索引定义
│   │   │   │   │   ├── ForeignKeyDefinition.java   # 外键定义
│   │   │   │   │   └── ExportMetadata.java         # 导出元数据
│   │   │   │   │
│   │   │   │   ├── diff/                           # 差异模型
│   │   │   │   │   ├── SchemaDiff.java             # 数据字典差异
│   │   │   │   │   ├── SchemaChange.java           # 变更项
│   │   │   │   │   ├── DiffMetadata.java           # 差异元数据
│   │   │   │   │   ├── DiffSummary.java            # 差异统计
│   │   │   │   │   └── ChangeType.java             # 变更类型枚举
│   │   │   │   │
│   │   │   │   └── config/                         # 配置模型
│   │   │   │       ├── DataSourceConfig.java       # 数据源配置
│   │   │   │       └── AppConfig.java              # 应用配置
│   │   │   │
│   │   │   ├── formatter/                          # 格式化处理
│   │   │   │   ├── SchemaFormatter.java            # 格式化器接口
│   │   │   │   ├── JsonFormatter.java              # JSON格式化
│   │   │   │   └── ExcelFormatter.java             # Excel格式化
│   │   │   │
│   │   │   ├── generator/                          # DDL生成器
│   │   │   │   ├── DDLGenerator.java               # DDL生成器接口
│   │   │   │   ├── AbstractDDLGenerator.java       # 抽象基类
│   │   │   │   ├── MySQLDDLGenerator.java          # MySQL DDL生成
│   │   │   │   ├── OracleDDLGenerator.java         # Oracle DDL生成
│   │   │   │   └── DDLGeneratorFactory.java        # 生成器工厂
│   │   │   │
│   │   │   ├── differ/                             # 差异对比引擎
│   │   │   │   ├── SchemaDiffer.java               # 对比器接口
│   │   │   │   └── DefaultSchemaDiffer.java        # 默认实现
│   │   │   │
│   │   │   ├── cli/                                # 命令行接口
│   │   │   │   ├── SchemaSyncShell.java            # Shell主类
│   │   │   │   ├── ExportCommand.java              # 导出命令
│   │   │   │   ├── DiffCommand.java                # 对比命令
│   │   │   │   ├── GenerateCommand.java            # 生成命令
│   │   │   │   └── TestConnectionCommand.java      # 测试连接命令
│   │   │   │
│   │   │   └── util/                               # 工具类
│   │   │       ├── CryptoUtil.java                 # 加密工具
│   │   │       ├── FileUtil.java                   # 文件工具
│   │   │       ├── SqlUtil.java                    # SQL工具
│   │   │       └── DateUtil.java                   # 日期工具
│   │   │
│   │   └── resources/
│   │       ├── application.yml                     # 应用配置
│   │       ├── application-dev.yml                 # 开发环境配置
│   │       ├── application-prod.yml                # 生产环境配置
│   │       ├── schemasync-config.json              # 数据源配置模板
│   │       └── logback-spring.xml                  # 日志配置
│   │
│   └── test/
│       ├── java/com/schemasync/
│       │   ├── controller/                         # 控制器测试
│       │   ├── service/                            # 服务测试
│       │   ├── adapter/                            # 适配器测试
│       │   ├── formatter/                          # 格式化器测试
│       │   ├── generator/                          # 生成器测试
│       │   └── differ/                             # 对比器测试
│       └── resources/
│           ├── test-config.json                    # 测试配置
│           └── test-data/                          # 测试数据
│               ├── mysql-sample.json
│               ├── oracle-sample.json
│               └── diff-sample.json
├── docs/                                           # 文档
│   ├── api.md                                      # API文档
│   ├── adapter-dev-guide.md                        # 适配器开发指南
│   └── deployment.md                               # 部署指南
└── scripts/                                        # 脚本
    ├── build.sh                                    # 构建脚本
    ├── run.sh                                      # 运行脚本
    └── package.sh                                  # 打包脚本
```

### 2.2 前端工程结构 (schemasync-frontend)

```
schemasync-frontend/
├── package.json
├── vue.config.js / vite.config.js
├── public/
│   └── index.html
├── src/
│   ├── main.js / main.ts
│   ├── App.vue
│   │
│   ├── api/                                        # API接口
│   │   ├── request.js                              # Axios封装
│   │   ├── export.js                               # 导出接口
│   │   ├── diff.js                                 # 对比接口
│   │   └── generate.js                             # 生成接口
│   │
│   ├── views/                                      # 页面
│   │   ├── Home.vue                                # 首页
│   │   ├── Config.vue                              # 数据源配置
│   │   ├── Export.vue                              # 数据字典导出
│   │   ├── Diff.vue                                # 版本对比
│   │   └── Generate.vue                            # 脚本生成
│   │
│   ├── components/                                 # 组件
│   │   ├── common/
│   │   │   ├── Header.vue                          # 头部导航
│   │   │   ├── Footer.vue                          # 底部
│   │   │   └── Loading.vue                         # 加载组件
│   │   │
│   │   ├── config/
│   │   │   ├── DataSourceForm.vue                  # 数据源表单
│   │   │   ├── ConnectionTest.vue                  # 连接测试
│   │   │   └── ConfigList.vue                      # 配置列表
│   │   │
│   │   ├── export/
│   │   │   ├── ExportOptions.vue                   # 导出选项
│   │   │   ├── TableSelector.vue                   # 表选择器
│   │   │   └── ExportProgress.vue                  # 导出进度
│   │   │
│   │   ├── diff/
│   │   │   ├── FileUpload.vue                      # 文件上传
│   │   │   ├── DiffViewer.vue                      # 差异展示
│   │   │   ├── DiffTable.vue                       # 差异表格
│   │   │   └── DiffSummary.vue                     # 差异统计
│   │   │
│   │   └── generate/
│   │       ├── ScriptViewer.vue                    # 脚本查看
│   │       ├── SqlEditor.vue                       # SQL编辑器
│   │       └── DownloadBtn.vue                     # 下载按钮
│   │
│   ├── router/                                     # 路由
│   │   └── index.js
│   │
│   ├── store/                                      # 状态管理
│   │   ├── index.js
│   │   ├── modules/
│   │   │   ├── config.js                           # 配置状态
│   │   │   ├── export.js                           # 导出状态
│   │   │   └── diff.js                             # 差异状态
│   │   └── types.js
│   │
│   ├── utils/                                      # 工具类
│   │   ├── validator.js                            # 表单验证
│   │   ├── formatter.js                            # 数据格式化
│   │   └── storage.js                              # 本地存储
│   │
│   ├── styles/                                     # 样式
│   │   ├── variables.scss                          # 变量
│   │   ├── global.scss                             # 全局样式
│   │   └── themes/                                 # 主题
│   │
│   └── assets/                                     # 静态资源
│       ├── images/
│       └── icons/
│
├── tests/                                          # 测试
│   ├── unit/
│   └── e2e/
│
└── docs/                                           # 文档
    └── README.md
```

---

## 三、核心模块设计

### 3.1 数据库适配器模块

#### 3.1.1 适配器接口定义

```java
/**
 * 数据库适配器接口
 * 所有数据库适配器必须实现此接口
 */
public interface DatabaseAdapter {

    /**
     * 连接数据库
     */
    Connection connect(DataSourceConfig config) throws SQLException;

    /**
     * 测试连接
     */
    boolean testConnection(DataSourceConfig config);

    /**
     * 获取所有数据库名称
     */
    List<String> getDatabases(Connection conn) throws SQLException;

    /**
     * 获取指定数据库的所有表
     */
    List<TableDefinition> getTables(Connection conn, String database) throws SQLException;

    /**
     * 获取表的字段信息
     */
    List<ColumnDefinition> getColumns(Connection conn, String database, String tableName) throws SQLException;

    /**
     * 获取表的索引信息
     */
    List<IndexDefinition> getIndexes(Connection conn, String database, String tableName) throws SQLException;

    /**
     * 获取表的外键信息
     */
    List<ForeignKeyDefinition> getForeignKeys(Connection conn, String database, String tableName) throws SQLException;

    /**
     * 导出完整的数据字典
     */
    SchemaDictionary exportSchema(DataSourceConfig config, ExportOptions options) throws SQLException;

    /**
     * 获取数据库类型
     */
    String getDatabaseType();

    /**
     * 获取数据库版本
     */
    String getDatabaseVersion(Connection conn) throws SQLException;
}
```

#### 3.1.2 适配器继承体系

```
DatabaseAdapter (接口)
    ↓
AbstractDatabaseAdapter (抽象基类 - 实现通用逻辑)
    ├── MySQLAdapter
    │   ├── OceanBaseAdapter (继承MySQL,覆盖部分方法)
    │   ├── TDSQLAdapter (继承MySQL,覆盖部分方法)
    │   └── GoldenDBAdapter (继承MySQL,覆盖部分方法)
    ├── OracleAdapter
    └── GaussDBAdapter
```

#### 3.1.3 适配器工厂

```java
@Component
public class DatabaseAdapterFactory {

    private final Map<String, DatabaseAdapter> adapterMap;

    public DatabaseAdapterFactory(List<DatabaseAdapter> adapters) {
        this.adapterMap = adapters.stream()
            .collect(Collectors.toMap(
                DatabaseAdapter::getDatabaseType,
                adapter -> adapter
            ));
    }

    public DatabaseAdapter getAdapter(String databaseType) {
        DatabaseAdapter adapter = adapterMap.get(databaseType.toUpperCase());
        if (adapter == null) {
            throw new UnsupportedOperationException("不支持的数据库类型: " + databaseType);
        }
        return adapter;
    }
}
```

### 3.2 数据模型设计

#### 3.2.1 核心领域模型

```java
/**
 * 数据字典 - 完整描述一个数据库的结构
 */
@Data
public class SchemaDictionary {
    private ExportMetadata metadata;
    private List<TableDefinition> tables;
}

/**
 * 表定义
 */
@Data
public class TableDefinition {
    private String tableName;
    private String tableComment;
    private String tableType;  // BASE TABLE, VIEW, TEMPORARY
    private String engine;     // MySQL特有
    private String charset;
    private Date createTime;
    private Date updateTime;
    private List<ColumnDefinition> columns;
    private List<IndexDefinition> indexes;
    private List<ForeignKeyDefinition> foreignKeys;
}

/**
 * 字段定义 - 数据类型、长度、精度分离
 */
@Data
public class ColumnDefinition {
    private String columnName;
    private String dataType;        // VARCHAR, DECIMAL, BIGINT等
    private Integer length;         // 长度,如VARCHAR(100)的100
    private Integer precision;      // 精度,如DECIMAL(10,2)的10
    private Integer scale;          // 小数位,如DECIMAL(10,2)的2
    private Boolean nullable;
    private Object defaultValue;
    private Boolean isPrimaryKey;
    private Boolean isAutoIncrement;
    private String comment;
    private String charset;
    private Integer ordinalPosition;
}

/**
 * 索引定义
 */
@Data
public class IndexDefinition {
    private String indexName;
    private String indexType;  // PRIMARY, UNIQUE, INDEX, FULLTEXT
    private List<String> columns;
    private Boolean isUnique;
    private String comment;
}

/**
 * 外键定义
 */
@Data
public class ForeignKeyDefinition {
    private String constraintName;
    private String columnName;
    private String referencedTable;
    private String referencedColumn;
    private String onUpdate;  // CASCADE, RESTRICT, SET NULL
    private String onDelete;
}
```

#### 3.2.2 差异模型

```java
/**
 * 数据字典差异
 */
@Data
public class SchemaDiff {
    private DiffMetadata diffMetadata;
    private DiffSummary summary;
    private List<SchemaChange> changes;
}

/**
 * 变更项
 */
@Data
public class SchemaChange {
    private ChangeType changeType;  // TABLE_ADD, TABLE_DROP, COLUMN_MODIFY等
    private String tableName;
    private String columnName;
    private Severity severity;      // BREAKING, NON_BREAKING
    private Object details;         // 变更详情
}

/**
 * 变更类型枚举
 */
public enum ChangeType {
    // 表级别
    TABLE_ADD,
    TABLE_DROP,
    TABLE_MODIFY,

    // 字段级别
    COLUMN_ADD,
    COLUMN_DROP,
    COLUMN_MODIFY,

    // 索引级别
    INDEX_ADD,
    INDEX_DROP,
    INDEX_MODIFY,

    // 约束级别
    FOREIGN_KEY_ADD,
    FOREIGN_KEY_DROP,
    FOREIGN_KEY_MODIFY
}

/**
 * 严重程度
 */
public enum Severity {
    BREAKING,      // 破坏性变更
    NON_BREAKING   // 非破坏性变更
}
```

### 3.3 核心服务设计

#### 3.3.1 导出服务 (SchemaExportService)

```java
@Service
public class SchemaExportService {

    @Autowired
    private DatabaseAdapterFactory adapterFactory;

    @Autowired
    private JsonFormatter jsonFormatter;

    @Autowired
    private ExcelFormatter excelFormatter;

    /**
     * 导出数据字典
     */
    public byte[] exportSchema(String configName, ExportOptions options) {
        // 1. 获取数据源配置
        DataSourceConfig config = configService.getDataSourceConfig(configName);

        // 2. 获取对应数据库适配器
        DatabaseAdapter adapter = adapterFactory.getAdapter(config.getType());

        // 3. 导出数据字典
        SchemaDictionary dictionary = adapter.exportSchema(config, options);

        // 4. 格式化输出
        if ("json".equalsIgnoreCase(options.getFormat())) {
            return jsonFormatter.format(dictionary);
        } else if ("excel".equalsIgnoreCase(options.getFormat())) {
            return excelFormatter.format(dictionary);
        }

        throw new IllegalArgumentException("不支持的格式: " + options.getFormat());
    }
}
```

#### 3.3.2 对比服务 (SchemaDiffService)

```java
@Service
public class SchemaDiffService {

    @Autowired
    private SchemaDiffer schemaDiffer;

    @Autowired
    private JsonFormatter jsonFormatter;

    @Autowired
    private ExcelFormatter excelFormatter;

    /**
     * 对比两个数据字典
     */
    public SchemaDiff compareSchemas(byte[] oldFile, byte[] newFile, String format) {
        // 1. 解析旧版本数据字典
        SchemaDictionary oldDict = parseDictionary(oldFile, format);

        // 2. 解析新版本数据字典
        SchemaDictionary newDict = parseDictionary(newFile, format);

        // 3. 执行对比
        SchemaDiff diff = schemaDiffer.compare(oldDict, newDict);

        return diff;
    }

    /**
     * 解析数据字典文件
     */
    private SchemaDictionary parseDictionary(byte[] fileData, String format) {
        if ("json".equalsIgnoreCase(format)) {
            return jsonFormatter.parse(fileData);
        } else if ("excel".equalsIgnoreCase(format)) {
            return excelFormatter.parse(fileData);
        }
        throw new IllegalArgumentException("不支持的格式: " + format);
    }
}
```

#### 3.3.3 DDL生成服务 (DDLGenerationService)

```java
@Service
public class DDLGenerationService {

    @Autowired
    private DDLGeneratorFactory generatorFactory;

    /**
     * 生成DDL脚本
     */
    public String generateDDL(SchemaDiff diff, String databaseType, GenerationOptions options) {
        // 1. 获取对应数据库的DDL生成器
        DDLGenerator generator = generatorFactory.getGenerator(databaseType);

        // 2. 生成DDL脚本
        String ddlScript = generator.generate(diff, options);

        return ddlScript;
    }

    /**
     * 生成回滚脚本
     */
    public String generateRollbackDDL(SchemaDiff diff, String databaseType) {
        DDLGenerator generator = generatorFactory.getGenerator(databaseType);
        return generator.generateRollback(diff);
    }
}
```

### 3.4 格式化器设计

#### 3.4.1 格式化器接口

```java
/**
 * 格式化器接口
 */
public interface SchemaFormatter {

    /**
     * 将数据字典格式化为字节数组
     */
    byte[] format(SchemaDictionary dictionary);

    /**
     * 将字节数组解析为数据字典
     */
    SchemaDictionary parse(byte[] data);

    /**
     * 支持的格式类型
     */
    String getFormatType();
}
```

#### 3.4.2 JSON格式化器

```java
@Component
public class JsonFormatter implements SchemaFormatter {

    private final ObjectMapper objectMapper;

    @Override
    public byte[] format(SchemaDictionary dictionary) {
        // 使用Jackson序列化为JSON
        return objectMapper.writeValueAsBytes(dictionary);
    }

    @Override
    public SchemaDictionary parse(byte[] data) {
        // 使用Jackson反序列化
        return objectMapper.readValue(data, SchemaDictionary.class);
    }

    @Override
    public String getFormatType() {
        return "json";
    }
}
```

#### 3.4.3 Excel格式化器

```java
@Component
public class ExcelFormatter implements SchemaFormatter {

    @Override
    public byte[] format(SchemaDictionary dictionary) {
        // 使用Apache POI生成Excel
        // Sheet1: 表列表
        // Sheet2-N: 每个表的详细信息(字段、索引、外键)
        try (Workbook workbook = new XSSFWorkbook()) {
            // 1. 创建表概览Sheet
            Sheet overviewSheet = workbook.createSheet("表概览");
            fillTableOverview(overviewSheet, dictionary.getTables());

            // 2. 为每个表创建详细Sheet
            for (TableDefinition table : dictionary.getTables()) {
                Sheet tableSheet = workbook.createSheet(table.getTableName());
                fillTableDetails(tableSheet, table);
            }

            // 3. 输出为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    @Override
    public SchemaDictionary parse(byte[] data) {
        // 解析Excel文件为数据字典
        // 需要反向处理Excel结构
    }

    @Override
    public String getFormatType() {
        return "excel";
    }
}
```

---

## 四、API接口设计

### 4.1 RESTful API

#### 4.1.1 数据源配置管理

```
GET    /api/config/datasources          # 获取所有数据源配置
POST   /api/config/datasources          # 新增数据源配置
PUT    /api/config/datasources/{id}     # 更新数据源配置
DELETE /api/config/datasources/{id}     # 删除数据源配置
POST   /api/config/datasources/test     # 测试数据源连接
```

#### 4.1.2 数据字典导出

```
POST   /api/export                      # 导出数据字典
       Request:
       {
         "configName": "prod-mysql",
         "format": "json",  // json | excel
         "database": "mydb",
         "tablePattern": "user*",  // 可选,表名过滤
         "excludeTables": ["temp_*"]  // 可选,排除表
       }

       Response: 文件流
```

#### 4.1.3 版本对比

```
POST   /api/diff                        # 对比两个数据字典
       Request: multipart/form-data
       - oldFile: 旧版本文件
       - newFile: 新版本文件
       - format: json | excel

       Response:
       {
         "diffMetadata": {...},
         "summary": {...},
         "changes": [...]
       }
```

#### 4.1.4 差异化DDL脚本生成

```
POST   /api/generate                    # 生成DDL脚本
       Request:
       {
         "diff": {...},  // 差异对象
         "databaseType": "mysql",
         "includeRollback": true,  // 是否包含回滚脚本
         "commentBreakingChanges": true  // 是否注释破坏性变更
       }

       Response:
       {
         "ddlScript": "-- SQL内容",
         "rollbackScript": "-- 回滚SQL"
       }
```

### 4.2 CLI命令设计

```bash
# 导出数据字典
schemasync export --config prod-mysql --format json --output schema.json

# 对比版本
schemasync diff --old v1.json --new v2.json --format json --output diff.json

# 生成DDL
schemasync generate --diff diff.json --db-type mysql --output migration.sql

# 测试连接
schemasync test-connection --config prod-mysql

# 查看帮助
schemasync --help
schemasync export --help
```

---

## 五、数据库设计

### 5.1 配置存储

**不使用数据库**,配置存储在启动目录的JSON文件中:

```json
// schemasync-config.json
{
  "version": "1.0.0",
  "dataSources": [
    {
      "id": "ds-001",
      "name": "prod-mysql",
      "type": "mysql",
      "host": "192.168.1.100",
      "port": 3306,
      "database": "mydb",
      "username": "readonly_user",
      "password": "AES256加密后的密码",
      "charset": "utf8mb4",
      "timeout": 30,
      "createTime": "2026-04-26T10:00:00Z",
      "updateTime": "2026-04-26T10:00:00Z"
    }
  ],
  "settings": {
    "defaultOutputDir": "./output",
    "logLevel": "info",
    "maxConnectionPool": 5
  }
}
```

### 5.2 数据字典文件

导出为JSON或Excel文件,不持久化存储。

### 5.3 差异文件

对比结果输出为JSON或Excel文件,不持久化存储。

---

## 六、关键技术方案

### 6.1 数据库连接管理

```java
/**
 * 数据库连接池管理
 * 使用HikariCP管理连接
 */
@Component
public class ConnectionManager {

    private final Map<String, HikariDataSource> poolMap = new ConcurrentHashMap<>();

    /**
     * 获取或创建连接池
     */
    public HikariDataSource getPool(DataSourceConfig config) {
        String key = config.getId();
        return poolMap.computeIfAbsent(key, k -> createPool(config));
    }

    /**
     * 创建连接池
     */
    private HikariDataSource createPool(DataSourceConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(buildJdbcUrl(config));
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(CryptoUtil.decrypt(config.getPassword()));
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(30000);

        return new HikariDataSource(hikariConfig);
    }

    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(DataSourceConfig config) {
        switch (config.getType().toUpperCase()) {
            case "MYSQL":
            case "OCEANBASE":
            case "TDSQL":
            case "GOLDENDB":
                return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8",
                    config.getHost(), config.getPort(), config.getDatabase());
            case "ORACLE":
                return String.format("jdbc:oracle:thin:@%s:%d:%s",
                    config.getHost(), config.getPort(), config.getDatabase());
            case "GAUSSDB":
                return String.format("jdbc:postgresql://%s:%d/%s",
                    config.getHost(), config.getPort(), config.getDatabase());
            default:
                throw new IllegalArgumentException("不支持的数据库类型");
        }
    }
}
```

### 6.2 元数据查询实现示例

#### 6.2.1 MySQL元数据查询

```java
@Repository
public class MySQLAdapter extends AbstractDatabaseAdapter {

    private static final String QUERY_TABLES =
        "SELECT TABLE_NAME, TABLE_COMMENT, TABLE_TYPE, ENGINE, TABLE_COLLATION, CREATE_TIME, UPDATE_TIME " +
        "FROM INFORMATION_SCHEMA.TABLES " +
        "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE IN ('BASE TABLE', 'VIEW') " +
        "ORDER BY TABLE_NAME";

    private static final String QUERY_COLUMNS =
        "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, " +
        "NUMERIC_PRECISION, NUMERIC_SCALE, IS_NULLABLE, COLUMN_DEFAULT, " +
        "COLUMN_KEY, EXTRA, COLUMN_COMMENT, CHARACTER_SET_NAME, ORDINAL_POSITION " +
        "FROM INFORMATION_SCHEMA.COLUMNS " +
        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
        "ORDER BY ORDINAL_POSITION";

    private static final String QUERY_INDEXES =
        "SELECT INDEX_NAME, NON_UNIQUE, INDEX_TYPE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS " +
        "FROM INFORMATION_SCHEMA.STATISTICS " +
        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
        "GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_TYPE";

    private static final String QUERY_FOREIGN_KEYS =
        "SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
        "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND REFERENCED_TABLE_NAME IS NOT NULL";

    @Override
    public List<TableDefinition> getTables(Connection conn, String database) throws SQLException {
        List<TableDefinition> tables = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_TABLES);
             ResultSet rs = pstmt.executeQuery(database)) {

            while (rs.next()) {
                TableDefinition table = new TableDefinition();
                table.setTableName(rs.getString("TABLE_NAME"));
                table.setTableComment(rs.getString("TABLE_COMMENT"));
                table.setTableType(rs.getString("TABLE_TYPE"));
                table.setEngine(rs.getString("ENGINE"));
                table.setCharset(rs.getString("TABLE_COLLATION"));

                Timestamp createTime = rs.getTimestamp("CREATE_TIME");
                if (createTime != null) {
                    table.setCreateTime(new Date(createTime.getTime()));
                }

                tables.add(table);
            }
        }

        return tables;
    }

    @Override
    public List<ColumnDefinition> getColumns(Connection conn, String database, String tableName) throws SQLException {
        List<ColumnDefinition> columns = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_COLUMNS)) {
            pstmt.setString(1, database);
            pstmt.setString(2, tableName);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ColumnDefinition column = new ColumnDefinition();
                    column.setColumnName(rs.getString("COLUMN_NAME"));
                    column.setDataType(rs.getString("DATA_TYPE").toUpperCase());

                    // 长度
                    int length = rs.getInt("CHARACTER_MAXIMUM_LENGTH");
                    if (!rs.wasNull()) {
                        column.setLength(length);
                    }

                    // 精度
                    int precision = rs.getInt("NUMERIC_PRECISION");
                    if (!rs.wasNull()) {
                        column.setPrecision(precision);
                    }

                    // 小数位
                    int scale = rs.getInt("NUMERIC_SCALE");
                    if (!rs.wasNull()) {
                        column.setScale(scale);
                    }

                    column.setNullable("YES".equals(rs.getString("IS_NULLABLE")));
                    column.setDefaultValue(rs.getObject("COLUMN_DEFAULT"));
                    column.setPrimaryKey("PRI".equals(rs.getString("COLUMN_KEY")));
                    column.setAutoIncrement(rs.getString("EXTRA").contains("auto_increment"));
                    column.setComment(rs.getString("COLUMN_COMMENT"));
                    column.setCharset(rs.getString("CHARACTER_SET_NAME"));
                    column.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));

                    columns.add(column);
                }
            }
        }

        return columns;
    }

    @Override
    public String getDatabaseType() {
        return "MYSQL";
    }
}
```

### 6.3 差异对比算法

```java
@Service
public class DefaultSchemaDiffer implements SchemaDiffer {

    @Override
    public SchemaDiff compare(SchemaDictionary oldDict, SchemaDictionary newDict) {
        SchemaDiff diff = new SchemaDiff();
        List<SchemaChange> changes = new ArrayList<>();

        // 1. 对比表
        compareTables(oldDict.getTables(), newDict.getTables(), changes);

        // 2. 生成统计
        DiffSummary summary = generateSummary(changes);

        // 3. 组装差异对象
        diff.setChanges(changes);
        diff.setSummary(summary);

        return diff;
    }

    /**
     * 对比表列表
     */
    private void compareTables(List<TableDefinition> oldTables,
                               List<TableDefinition> newTables,
                               List<SchemaChange> changes) {
        Map<String, TableDefinition> oldTableMap = oldTables.stream()
            .collect(Collectors.toMap(TableDefinition::getTableName, t -> t));

        Map<String, TableDefinition> newTableMap = newTables.stream()
            .collect(Collectors.toMap(TableDefinition::getTableName, t -> t));

        // 新增的表
        for (String tableName : newTableMap.keySet()) {
            if (!oldTableMap.containsKey(tableName)) {
                changes.add(SchemaChange.builder()
                    .changeType(ChangeType.TABLE_ADD)
                    .tableName(tableName)
                    .severity(Severity.NON_BREAKING)
                    .build());
            }
        }

        // 删除的表
        for (String tableName : oldTableMap.keySet()) {
            if (!newTableMap.containsKey(tableName)) {
                changes.add(SchemaChange.builder()
                    .changeType(ChangeType.TABLE_DROP)
                    .tableName(tableName)
                    .severity(Severity.BREAKING)
                    .build());
            }
        }

        // 修改的表 - 对比字段、索引、外键
        for (String tableName : oldTableMap.keySet()) {
            if (newTableMap.containsKey(tableName)) {
                compareTableDetails(oldTableMap.get(tableName), newTableMap.get(tableName), changes);
            }
        }
    }

    /**
     * 对比表详细信息
     */
    private void compareTableDetails(TableDefinition oldTable,
                                     TableDefinition newTable,
                                     List<SchemaChange> changes) {
        // 对比字段
        compareColumns(oldTable.getColumns(), newTable.getColumns(), changes, oldTable.getTableName());

        // 对比索引
        compareIndexes(oldTable.getIndexes(), newTable.getIndexes(), changes, oldTable.getTableName());

        // 对比外键
        compareForeignKeys(oldTable.getForeignKeys(), newTable.getForeignKeys(), changes, oldTable.getTableName());
    }

    /**
     * 对比字段列表
     */
    private void compareColumns(List<ColumnDefinition> oldColumns,
                                List<ColumnDefinition> newColumns,
                                List<SchemaChange> changes,
                                String tableName) {
        Map<String, ColumnDefinition> oldColumnMap = oldColumns.stream()
            .collect(Collectors.toMap(ColumnDefinition::getColumnName, c -> c));

        Map<String, ColumnDefinition> newColumnMap = newColumns.stream()
            .collect(Collectors.toMap(ColumnDefinition::getColumnName, c -> c));

        // 新增字段
        for (String columnName : newColumnMap.keySet()) {
            if (!oldColumnMap.containsKey(columnName)) {
                changes.add(SchemaChange.builder()
                    .changeType(ChangeType.COLUMN_ADD)
                    .tableName(tableName)
                    .columnName(columnName)
                    .severity(Severity.NON_BREAKING)
                    .build());
            }
        }

        // 删除字段
        for (String columnName : oldColumnMap.keySet()) {
            if (!newColumnMap.containsKey(columnName)) {
                changes.add(SchemaChange.builder()
                    .changeType(ChangeType.COLUMN_DROP)
                    .tableName(tableName)
                    .columnName(columnName)
                    .severity(Severity.BREAKING)
                    .build());
            }
        }

        // 修改字段
        for (String columnName : oldColumnMap.keySet()) {
            if (newColumnMap.containsKey(columnName)) {
                compareColumnDetail(oldColumnMap.get(columnName),
                                   newColumnMap.get(columnName),
                                   changes, tableName, columnName);
            }
        }
    }

    /**
     * 对比单个字段的属性变更
     */
    private void compareColumnDetail(ColumnDefinition oldCol,
                                     ColumnDefinition newCol,
                                     List<SchemaChange> changes,
                                     String tableName,
                                     String columnName) {
        // 对比数据类型
        if (!Objects.equals(oldCol.getDataType(), newCol.getDataType())) {
            changes.add(createColumnModifyChange(tableName, columnName, "dataType",
                oldCol.getDataType(), newCol.getDataType(), Severity.BREAKING));
        }

        // 对比长度
        if (!Objects.equals(oldCol.getLength(), newCol.getLength())) {
            Severity severity = isLengthDecreased(oldCol.getLength(), newCol.getLength())
                ? Severity.BREAKING : Severity.NON_BREAKING;
            changes.add(createColumnModifyChange(tableName, columnName, "length",
                oldCol.getLength(), newCol.getLength(), severity));
        }

        // 对比精度
        if (!Objects.equals(oldCol.getPrecision(), newCol.getPrecision())) {
            changes.add(createColumnModifyChange(tableName, columnName, "precision",
                oldCol.getPrecision(), newCol.getPrecision(), Severity.BREAKING));
        }

        // 对比NULL约束
        if (!Objects.equals(oldCol.getNullable(), newCol.getNullable())) {
            Severity severity = Boolean.FALSE.equals(newCol.getNullable())
                ? Severity.BREAKING : Severity.NON_BREAKING;
            changes.add(createColumnModifyChange(tableName, columnName, "nullable",
                oldCol.getNullable(), newCol.getNullable(), severity));
        }

        // 对比默认值
        if (!Objects.equals(oldCol.getDefaultValue(), newCol.getDefaultValue())) {
            changes.add(createColumnModifyChange(tableName, columnName, "defaultValue",
                oldCol.getDefaultValue(), newCol.getDefaultValue(), Severity.NON_BREAKING));
        }

        // 对比注释
        if (!Objects.equals(oldCol.getComment(), newCol.getComment())) {
            changes.add(createColumnModifyChange(tableName, columnName, "comment",
                oldCol.getComment(), newCol.getComment(), Severity.NON_BREAKING));
        }
    }
}
```

### 6.4 DDL生成策略

```java
@Component
public class MySQLDDLGenerator extends AbstractDDLGenerator {

    @Override
    public String generate(SchemaDiff diff, GenerationOptions options) {
        StringBuilder ddl = new StringBuilder();

        // 添加文件头注释
        ddl.append(generateHeader(diff));
        ddl.append("\nSTART TRANSACTION;\n\n");

        // 1. 新增表
        ddl.append("-- ============================================\n");
        ddl.append("-- 1. 新增表\n");
        ddl.append("-- ============================================\n\n");
        generateCreateTables(diff, ddl);

        // 2. 修改表结构
        ddl.append("\n-- ============================================\n");
        ddl.append("-- 2. 修改表结构\n");
        ddl.append("-- ============================================\n\n");
        generateAlterTables(diff, ddl, options);

        // 3. 索引变更
        ddl.append("\n-- ============================================\n");
        ddl.append("-- 3. 索引变更\n");
        ddl.append("-- ============================================\n\n");
        generateIndexChanges(diff, ddl);

        // 4. 外键约束
        ddl.append("\n-- ============================================\n");
        ddl.append("-- 4. 外键约束\n");
        ddl.append("-- ============================================\n\n");
        generateForeignKeyChanges(diff, ddl);

        // 5. 删除表(最后执行)
        ddl.append("\n-- ============================================\n");
        ddl.append("-- 5. 删除表(最后执行)\n");
        ddl.append("-- ============================================\n\n");
        generateDropTables(diff, ddl, options);

        ddl.append("\nCOMMIT;\n");

        return ddl.toString();
    }

    /**
     * 生成CREATE TABLE语句
     */
    private void generateCreateTables(SchemaDiff diff, StringBuilder ddl) {
        // 从diff中找出所有TABLE_ADD变更
        List<SchemaChange> tableAdds = diff.getChanges().stream()
            .filter(c -> c.getChangeType() == ChangeType.TABLE_ADD)
            .collect(Collectors.toList());

        for (SchemaChange change : tableAdds) {
            // 需要从新版本的完整数据字典中获取表定义
            TableDefinition tableDef = getTableDefinition(change.getTableName());
            ddl.append(generateCreateTableSQL(tableDef));
            ddl.append("\n\n");
        }
    }

    /**
     * 生成CREATE TABLE SQL
     */
    private String generateCreateTableSQL(TableDefinition table) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS `").append(table.getTableName()).append("` (\n");

        List<String> columnDefs = new ArrayList<>();
        List<String> indexDefs = new ArrayList<>();

        // 字段定义
        for (ColumnDefinition col : table.getColumns()) {
            columnDefs.add("    " + generateColumnDefinition(col));
        }

        // 主键
        List<String> pkColumns = table.getColumns().stream()
            .filter(ColumnDefinition::getIsPrimaryKey)
            .map(ColumnDefinition::getColumnName)
            .collect(Collectors.toList());

        if (!pkColumns.isEmpty()) {
            indexDefs.add("    PRIMARY KEY (`" + String.join("`, `", pkColumns) + "`)");
        }

        // 索引
        for (IndexDefinition idx : table.getIndexes()) {
            if (!"PRIMARY".equals(idx.getIndexType())) {
                indexDefs.add("    " + generateIndexSQL(idx));
            }
        }

        sql.append(String.join(",\n", columnDefs));
        if (!indexDefs.isEmpty()) {
            sql.append(",\n");
            sql.append(String.join(",\n", indexDefs));
        }

        sql.append("\n) ENGINE=").append(table.getEngine() != null ? table.getEngine() : "InnoDB");
        sql.append(" DEFAULT CHARSET=").append(table.getCharset() != null ? table.getCharset() : "utf8mb4");

        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
            sql.append(" COMMENT='").append(table.getTableComment()).append("'");
        }

        sql.append(";");

        return sql.toString();
    }

    /**
     * 生成字段定义
     */
    private String generateColumnDefinition(ColumnDefinition col) {
        StringBuilder def = new StringBuilder();
        def.append("`").append(col.getColumnName()).append("` ");
        def.append(col.getDataType());

        // 添加长度/精度
        if (col.getLength() != null) {
            def.append("(").append(col.getLength());
            if (col.getScale() != null) {
                def.append(",").append(col.getScale());
            }
            def.append(")");
        } else if (col.getPrecision() != null) {
            def.append("(").append(col.getPrecision());
            if (col.getScale() != null) {
                def.append(",").append(col.getScale());
            }
            def.append(")");
        }

        // NULL约束
        def.append(Boolean.TRUE.equals(col.getNullable()) ? " NULL" : " NOT NULL");

        // 自增
        if (Boolean.TRUE.equals(col.getIsAutoIncrement())) {
            def.append(" AUTO_INCREMENT");
        }

        // 默认值
        if (col.getDefaultValue() != null) {
            def.append(" DEFAULT ").append(formatDefaultValue(col.getDefaultValue()));
        }

        // 注释
        if (col.getComment() != null && !col.getComment().isEmpty()) {
            def.append(" COMMENT '").append(col.getComment()).append("'");
        }

        return def.toString();
    }
}
```

---

## 七、部署方案

### 7.1 后端部署

#### 7.1.1 打包方式

```xml
<!-- pom.xml -->
<packaging>jar</packaging>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <!-- 打包为可执行jar -->
                <executable>true</executable>
            </configuration>
        </plugin>
    </plugins>
</build>
```

#### 7.1.2 启动命令

```bash
# 前台启动
java -jar schemasync-backend.jar

# 后台启动
nohup java -jar schemasync-backend.jar > schemasync.log 2>&1 &

# 指定配置文件
java -jar schemasync-backend.jar --spring.config.location=./application.yml
```

### 7.2 前端部署

#### 7.2.1 构建

```bash
npm install
npm run build
```

生成dist目录,部署到Nginx或任何静态服务器。

#### 7.2.2 Nginx配置

```nginx
server {
    listen 80;
    server_name schemasync.example.com;

    # 前端静态文件
    location / {
        root /var/www/schemasync-frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 7.3 Docker部署(可选)

```dockerfile
# Dockerfile
FROM openjdk:8-jre-slim

WORKDIR /app

COPY schemasync-backend.jar app.jar
COPY schemasync-config.json ./config/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# 构建镜像
docker build -t schemasync:latest .

# 运行容器
docker run -d -p 8080:8080 -v /data/config:/app/config schemasync:latest
```

---

## 八、开发计划

### 8.1 第一阶段: 核心功能 (3-4周)

**Week 1-2: 基础架构**

- [ ] 项目初始化(Maven + Spring Boot)
- [ ] 数据模型定义
- [ ] 数据库适配器接口设计
- [ ] MySQL适配器实现
- [ ] 配置管理模块

**Week 3-4: 核心功能**

- [ ] 数据字典导出功能
- [ ] JSON/Excel格式化
- [ ] 差异对比引擎
- [ ] 差异化DDL脚本生成
- [ ] 单元测试

**交付物**:

- 支持MySQL的数据字典导出、对比、脚本生成
- RESTful API
- 基础文档

### 8.2 第二阶段: 多数据库支持 (2-3周)

**Week 5-6: 数据库扩展**

- [ ] Oracle适配器
- [ ] OceanBase/TDSQL/GoldenDB适配器(基于MySQL)
- [ ] GaussDB适配器(基于PostgreSQL)
- [ ] 各适配器测试

**Week 7: 功能完善**

- [ ] CLI命令行工具
- [ ] 性能优化
- [ ] 集成测试

**交付物**:

- 支持6种数据库
- CLI工具
- 完整测试用例

### 8.3 第三阶段: 前端界面 (2-3周)

**Week 8-9: 前端开发**

- [ ] Vue项目初始化
- [ ] 数据源配置页面
- [ ] 导出功能页面
- [ ] 对比功能页面
- [ ] 脚本生成页面

**Week 10: 联调优化**

- [ ] 前后端联调
- [ ] UI优化
- [ ] 用户测试

**交付物**:

- 完整的Web UI
- 用户手册

---

## 九、测试策略

### 9.1 单元测试

- 数据模型序列化/反序列化测试
- 适配器各方法测试
- 对比算法测试
- DDL生成逻辑测试
- 格式化器测试

**目标覆盖率**: ≥70%

### 9.2 集成测试

- 使用Testcontainers启动测试数据库
- 验证导出准确性
- 验证DDL可执行性
- 端到端流程测试

### 9.3 兼容性测试

- MySQL 5.7, 8.0
- Oracle 12c, 19c
- OceanBase 3.x, 4.x
- TDSQL最新版本
- GaussDB最新版本
- GoldenDB最新版本

---

## 十、构建与部署

### 10.1 构建方式

#### Maven一键打包

项目使用 `frontend-maven-plugin` 实现前后端一体化构建：

```bash
# Windows
build.bat

# Linux/Mac
chmod +x build.sh
./build.sh
```

**构建流程**：

1. 检查Java和Maven环境
2. 清理旧构建 (`mvn clean`)
3. Maven打包（自动构建前端）
   - 安装Node.js和npm
   - 执行 `npm install`
   - 执行 `npm run build`
   - 将前端产物复制到后端 `resources/static`
4. 生成可执行JAR包
5. 创建 `deploy/` 目录并复制文件

### 10.2 部署包结构

```
deploy/
├── schemasync.jar              # 54.35 MB - 完整应用
├── application.yml             # 外置配置文件
├── start.bat                   # Windows启动脚本
├── start.sh                    # Linux启动脚本
├── DEPLOY.md                   # 部署文档
└── 打包清单.txt                # 打包清单
```

### 10.3 部署步骤

**Windows服务器**：

```batch
# 1. 复制 deploy 目录到服务器
# 2. 双击 start.bat 启动
# 3. 访问 http://localhost:8080
```

**Linux服务器**：

```bash
# 1. 复制 deploy 目录到服务器
# 2. 赋权并启动
chmod +x start.sh
./start.sh

# 3. 后台运行
nohup java -jar schemasync.jar --spring.config.location=application.yml > /dev/null 2>&1 &

# 4. 访问 http://localhost:8080
```

### 10.4 环境要求

**构建环境**：

- JDK: 8 或更高版本
- Maven: 3.6 或更高版本
- Node.js: 16+ (由Maven自动管理)

**运行环境**：

- JDK: 8 或更高版本
- 内存: 至少 512MB
- 磁盘: 至少 200MB

### 10.5 配置说明

**外置配置文件** (`application.yml`)：

```yaml
server:
  port: 8080 # 可自定义端口

schemasync:
  default-output-dir: ./output # 导出文件目录
  max-pool-size: 10 # 连接池大小
  connection-timeout: 30 # 连接超时

logging:
  level:
    com.schemasync: DEBUG # 日志级别
  file:
    name: logs/schemasync.log # 日志文件
```

**数据源配置**：

- 位置: `~/.schemasync/schemasync-config.json`
- 格式: JSON
- 支持动态添加和修改

---

## 十一、版本历史

### v1.0.0-SNAPSHOT (2026-04-29)

**核心功能**：

- ✅ 多数据库适配器（6种数据库）
- ✅ 数据字典导出（JSON/Excel）
- ✅ 版本对比与差异分析
- ✅ DDL脚本生成（差异化/全量）
- ✅ 前后端一体化打包
- ✅ 外置配置支持

**主要改进**：

- 索引变更完整支持（INDEX_ADD/INDEX_DROP/INDEX_MODIFY）
- Excel导出详情格式化
- DDL生成精度优化
- 构建脚本优化（build.bat/build.sh）
- 部署包自动生成

**已知问题**：

- 无

---

## 附录

### A. 关键设计决策

1. **前后端分离 → 一体化打包**
   - 开发时：前后端分离，便于调试
   - 部署时：打包为单JAR，简化部署

2. **无本地数据库**
   - 配置存储在JSON文件
   - 轻量化，降低运维成本

3. **策略模式实现数据库适配**
   - 便于扩展新数据库
   - 代码隔离，降低耦合

### B. 技术栈版本

| 组件         | 版本   |
| ------------ | ------ |
| Spring Boot  | 2.7.18 |
| Vue          | 3.x    |
| Vite         | 5.4.21 |
| Element Plus | 2.x    |
| Apache POI   | 5.2.5  |
| FastJSON2    | 2.0.42 |
| HikariCP     | 5.0.1  |

---

## 十、风险与应对

| 风险                     | 影响 | 概率 | 应对措施                                  |
| ------------------------ | ---- | ---- | ----------------------------------------- |
| 国产数据库元数据查询差异 | 高   | 高   | 提前获取各数据库技术文档,建立测试环境验证 |
| Oracle JDBC驱动授权      | 中   | 中   | 使用Maven中央仓库的ojdbc8,注意许可协议    |
| Excel大数据量性能        | 中   | 低   | 使用SXSSFWorkbook流式写入,分批处理        |
| DDL语法差异              | 高   | 高   | 每种数据库独立测试,建立SQL模板库          |
| 前端大文件上传           | 中   | 低   | 分片上传,进度提示,超时处理                |

---

**文档结束**

_本设计文档为开发团队提供技术实现指导,开发过程中如遇技术调整请及时更新文档。_
