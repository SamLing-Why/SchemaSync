package com.schemasync.service;

import com.schemasync.model.dict.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DDL生成服务
 * 基于数据字典生成CREATE TABLE语句
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
@Service
public class DdlGeneratorService {
    
    private static final Logger log = LoggerFactory.getLogger(DdlGeneratorService.class);
    
    @Autowired
    private SchemaDictionaryParser parser;
    
    /**
     * 从数据字典文件生成DDL
     * 
     * @param inputStream 文件输入流
     * @param fileType 文件类型(json/excel)
     * @param databaseType 数据库类型(mysql/gaussdb_mysql/gaussdb_oracle)
     * @return DDL SQL字符串
     */
    public String generateDdl(InputStream inputStream, String fileType, String databaseType) {
        try {
            SchemaDictionary dictionary;
            if ("excel".equals(fileType)) {
                dictionary = parser.parseExcel(inputStream);
            } else {
                // Java 8兼容: 使用ByteArrayOutputStream读取所有字节
                java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                dictionary = parser.parseJson(outputStream.toByteArray());
            }
            
            return generateDdlFromDictionary(dictionary, databaseType);
        } catch (Exception e) {
            log.error("生成DDL失败", e);
            throw new RuntimeException("生成DDL失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从数据字典文件生成DDL（兼容旧接口，默认MySQL）
     * 
     * @param inputStream 文件输入流
     * @param fileType 文件类型(json/excel)
     * @return DDL SQL字符串
     */
    public String generateDdl(InputStream inputStream, String fileType) {
        return generateDdl(inputStream, fileType, "mysql");
    }
    
    /**
     * 从数据字典生成DDL SQL
     * 
     * @param dictionary 数据字典
     * @param databaseType 数据库类型(mysql/gaussdb_mysql/gaussdb_oracle)
     * @return DDL SQL字符串
     */
    public String generateDdlFromDictionary(SchemaDictionary dictionary, String databaseType) {
        // 根据数据库类型选择生成策略
        String dbType = databaseType != null ? databaseType.toLowerCase() : "mysql";
        
        switch (dbType) {
            case "mysql":
            case "gaussdb_mysql":
                // MySQL和GaussDB MySQL兼容模式使用相同逻辑
                return generateMySqlStyleDdl(dictionary);
            case "gaussdb_oracle":
                // GaussDB Oracle兼容模式
                return generateGaussDbOracleStyleDdl(dictionary);
            default:
                log.warn("不支持的数据库类型: {}, 使用MySQL默认模式", databaseType);
                return generateMySqlStyleDdl(dictionary);
        }
    }
    
    /**
     * 从数据字典生成DDL SQL（兼容旧接口，默认MySQL）
     */
    public String generateDdlFromDictionary(SchemaDictionary dictionary) {
        return generateDdlFromDictionary(dictionary, "mysql");
    }
    
    /**
     * 生成MySQL风格DDL（包括GaussDB MySQL兼容模式）
     */
    private String generateMySqlStyleDdl(SchemaDictionary dictionary) {
        StringBuilder sql = new StringBuilder();
        
        // 1. 添加注释头
        ExportMetadata metadata = dictionary.getMetadata();
        String dbType = metadata != null ? metadata.getDatabaseType() : "MySQL";
        sql.append("-- ============================================\n");
        sql.append("-- SchemaSync DDL Generation Script\n");
        sql.append("-- 数据库类型: MySQL兼容模式\n");
        if (metadata != null) {
            sql.append("-- 数据库: ").append(metadata.getDatabaseName()).append("\n");
            sql.append("-- 版本: ").append(metadata.getDatabaseVersion()).append("\n");
            sql.append("-- 生成时间: ").append(new Date()).append("\n");
        }
        sql.append("-- ============================================\n\n");
        
        // 2. 遍历每个表生成DDL
        if (dictionary.getTables() != null) {
            for (TableDefinition table : dictionary.getTables()) {
                // 区分表和视图
                if ("VIEW".equalsIgnoreCase(table.getTableType())) {
                    sql.append(generateCreateView(table)).append("\n\n");
                } else {
                    sql.append(generateCreateTable(table)).append("\n\n");
                }
            }
        }
        
        log.info("MySQL风格DDL脚本生成完成, 共{}个表", 
                dictionary.getTables() != null ? dictionary.getTables().size() : 0);
        
        return sql.toString();
    }
    
    /**
     * 生成GaussDB Oracle兼容模式风格DDL
     */
    private String generateGaussDbOracleStyleDdl(SchemaDictionary dictionary) {
        StringBuilder sql = new StringBuilder();
        
        // 1. 添加注释头
        ExportMetadata metadata = dictionary.getMetadata();
        sql.append("-- ============================================\n");
        sql.append("-- SchemaSync DDL Generation Script\n");
        sql.append("-- 数据库类型: GaussDB Oracle兼容模式\n");
        if (metadata != null) {
            sql.append("-- 数据库: ").append(metadata.getDatabaseName()).append("\n");
            sql.append("-- 版本: ").append(metadata.getDatabaseVersion()).append("\n");
            sql.append("-- 生成时间: ").append(new Date()).append("\n");
        }
        sql.append("-- ============================================\n\n");
        
        // 2. 遍历每个表生成DDL
        if (dictionary.getTables() != null) {
            for (TableDefinition table : dictionary.getTables()) {
                // 区分表和视图
                if ("VIEW".equalsIgnoreCase(table.getTableType())) {
                    sql.append(generateGaussDbOracleCreateView(table)).append("\n\n");
                } else {
                    sql.append(generateGaussDbOracleCreateTable(table)).append("\n\n");
                }
            }
        }
        
        log.info("GaussDB Oracle风格DDL脚本生成完成, 共{}个表", 
                dictionary.getTables() != null ? dictionary.getTables().size() : 0);
        
        return sql.toString();
    }
    
    /**
     * 生成单个表的CREATE TABLE语句
     */
    public String generateCreateTable(TableDefinition table) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `").append(table.getTableName()).append("` (\n");
        
        // 字段定义
        List<String> columnDefs = new ArrayList<>();
        if (table.getColumns() != null) {
            for (ColumnDefinition column : table.getColumns()) {
                columnDefs.add("  " + generateColumnDefinition(column));
            }
        }
        
        // 主键约束
        if (table.getColumns() != null) {
            String primaryKey = generatePrimaryKey(table.getColumns());
            if (primaryKey != null) {
                columnDefs.add("  " + primaryKey);
            }
        }
        
        // 索引约束（非主键）
        if (table.getIndexes() != null) {
            // 创建字段名映射：旧名 -> 新名
            Map<String, String> columnNameMap = buildColumnNameMap(table.getColumns());
            
            for (IndexDefinition idx : table.getIndexes()) {
                // 过滤主键索引：indexType或indexName为PRIMARY的都不需要单独生成
                // 因为主键已经在前面通过generatePrimaryKey()生成了
                if (!"PRIMARY".equalsIgnoreCase(idx.getIndexType()) 
                    && !"PRIMARY".equalsIgnoreCase(idx.getIndexName())) {
                    columnDefs.add("  " + generateIndex(idx, columnNameMap));
                }
            }
        }
        
        // 外键约束
        if (table.getForeignKeys() != null) {
            // 创建字段名映射：旧名 -> 新名
            Map<String, String> columnNameMap = buildColumnNameMap(table.getColumns());
            
            for (ForeignKeyDefinition fk : table.getForeignKeys()) {
                columnDefs.add("  " + generateForeignKey(fk, columnNameMap));
            }
        }
        
        sql.append(String.join(",\n", columnDefs));
        sql.append("\n)");
        
        // 表选项
        if (table.getEngine() != null) {
            sql.append(" ENGINE=").append(table.getEngine());
        }
        if (table.getCharset() != null) {
            sql.append(" DEFAULT CHARSET=").append(table.getCharset());
        }
        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
            sql.append(" COMMENT='").append(table.getTableComment().replace("'", "\\'")).append("'");
        }
        sql.append(";");
        
        return sql.toString();
    }
    
    /**
     * 构建字段名映射（旧名 -> 新名）
     */
    private Map<String, String> buildColumnNameMap(List<ColumnDefinition> columns) {
        Map<String, String> map = new java.util.HashMap<>();
        if (columns != null) {
            for (ColumnDefinition col : columns) {
                String effectiveName = col.getEffectiveName();
                if (!col.getColumnName().equals(effectiveName)) {
                    map.put(col.getColumnName(), effectiveName);
                }
            }
        }
        return map;
    }
    
    /**
     * 生成字段定义
     */
    private String generateColumnDefinition(ColumnDefinition column) {
        StringBuilder def = new StringBuilder();
        // 使用getEffectiveName()获取实际字段名（支持重命名）
        def.append("`").append(column.getEffectiveName()).append("` ");
        
        // 数据类型
        String dataType = column.getDataType();
        if (dataType == null) {
            dataType = "VARCHAR";
        }
        def.append(dataType.toUpperCase());
        
        // 添加长度/精度（仅对需要的类型）
        if (!isTypeWithoutLength(dataType)) {
            // 精度和小数位优先(如 DECIMAL(10,2))
            if (column.getPrecision() != null && column.getScale() != null) {
                def.append("(").append(column.getPrecision()).append(",")
                   .append(column.getScale()).append(")");
            }
            // 长度(如 VARCHAR(100)),仅在没有精度时使用
            else if (column.getLength() != null && column.getLength() > 0) {
                def.append("(").append(column.getLength()).append(")");
            }
        }
        
        // NULL约束
        if (column.getNullable() != null && !column.getNullable()) {
            def.append(" NOT NULL");
        }
        
        // 默认值
        if (column.getDefaultValue() != null) {
            String defaultValue = column.getDefaultValue().toString();
            // 字符串类型需要加引号
            if (defaultValue.toLowerCase().equals("null")) {
                def.append(" DEFAULT NULL");
            } else if (isNumericType(dataType)) {
                def.append(" DEFAULT ").append(defaultValue);
            } else {
                def.append(" DEFAULT '").append(defaultValue.replace("'", "\\'")).append("'");
            }
        }
        
        // 自增
        if (column.getIsAutoIncrement() != null && column.getIsAutoIncrement()) {
            def.append(" AUTO_INCREMENT");
        }
        
        // 注释
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            def.append(" COMMENT '").append(column.getComment().replace("'", "\\'")).append("'");
        }
        
        return def.toString();
    }
    
    /**
     * 生成视图的CREATE VIEW语句
     */
    public String generateCreateView(TableDefinition view) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE OR REPLACE VIEW `").append(view.getTableName()).append("` AS\n");
        
        // 视图定义(从注释或其他地方获取)
        String viewDef = view.getTableComment();
        if (viewDef != null && !viewDef.isEmpty()) {
            sql.append(viewDef);
        } else {
            sql.append("-- TODO: 请补充视图定义");
        }
        sql.append(";");
        
        return sql.toString();
    }
    
    /**
     * 生成GaussDB Oracle兼容模式的CREATE TABLE语句
     */
    private String generateGaussDbOracleCreateTable(TableDefinition table) {
        StringBuilder sql = new StringBuilder();
        // GaussDB Oracle模式不使用反引号，使用双引号或无引号
        sql.append("CREATE TABLE ").append(table.getTableName()).append(" (\n");
        
        // 字段定义
        List<String> columnDefs = new ArrayList<>();
        if (table.getColumns() != null) {
            for (ColumnDefinition column : table.getColumns()) {
                columnDefs.add("  " + generateGaussDbOracleColumnDefinition(column));
            }
        }
        
        // 主键约束
        if (table.getColumns() != null) {
            String primaryKey = generateGaussDbOraclePrimaryKey(table.getColumns());
            if (primaryKey != null) {
                columnDefs.add("  " + primaryKey);
            }
        }
        
        // 索引约束（非主键）
        if (table.getIndexes() != null) {
            // 创建字段名映射：旧名 -> 新名
            Map<String, String> columnNameMap = buildColumnNameMap(table.getColumns());
            
            for (IndexDefinition idx : table.getIndexes()) {
                if (!"PRIMARY".equalsIgnoreCase(idx.getIndexType())) {
                    // Oracle模式的索引通常在建表后单独创建，这里仅作为注释
                    columnDefs.add("  -- INDEX: " + idx.getIndexName() + " (" + 
                        String.join(", ", idx.getColumns()) + ")");
                }
            }
        }
        
        // 外键约束
        if (table.getForeignKeys() != null) {
            // 创建字段名映射：旧名 -> 新名
            Map<String, String> columnNameMap = buildColumnNameMap(table.getColumns());
            
            for (ForeignKeyDefinition fk : table.getForeignKeys()) {
                columnDefs.add("  " + generateGaussDbOracleForeignKey(fk, columnNameMap));
            }
        }
        
        sql.append(String.join(",\n", columnDefs));
        sql.append("\n)");
        
        // 表注释（Oracle模式使用COMMENT ON TABLE）
        sql.append(";\n");
        
        // 添加表注释
        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
            sql.append("COMMENT ON TABLE ").append(table.getTableName())
               .append(" IS '").append(table.getTableComment().replace("'", "\\'")).append("';\n");
        }
        
        return sql.toString();
    }
    
    /**
     * 生成GaussDB Oracle模式的字段定义
     */
    private String generateGaussDbOracleColumnDefinition(ColumnDefinition column) {
        StringBuilder def = new StringBuilder();
        // Oracle模式字段名通常大写，不使用反引号
        def.append(column.getEffectiveName().toUpperCase()).append(" ");
        
        // 数据类型（Oracle风格）
        String dataType = column.getDataType();
        if (dataType == null) {
            dataType = "VARCHAR2";
        } else {
            // 转换MySQL类型到Oracle类型
            dataType = convertToOracleType(dataType);
        }
        def.append(dataType);
        
        // 长度/精度
        if (column.getPrecision() != null && column.getScale() != null) {
            def.append("(").append(column.getPrecision()).append(",")
               .append(column.getScale()).append(")");
        } else if (column.getLength() != null && column.getLength() > 0) {
            def.append("(").append(column.getLength()).append(")");
        }
        
        // NULL约束
        if (column.getNullable() != null && !column.getNullable()) {
            def.append(" NOT NULL");
        }
        
        // 默认值
        if (column.getDefaultValue() != null) {
            String defaultValue = column.getDefaultValue().toString();
            if (defaultValue.toLowerCase().equals("null")) {
                def.append(" DEFAULT NULL");
            } else if (isNumericType(dataType)) {
                def.append(" DEFAULT ").append(defaultValue);
            } else {
                def.append(" DEFAULT '").append(defaultValue.replace("'", "\\'")).append("'");
            }
        }
        
        return def.toString();
    }
    
    /**
     * 生成GaussDB Oracle模式的主键约束
     */
    private String generateGaussDbOraclePrimaryKey(List<ColumnDefinition> columns) {
        // 使用getEffectiveName()获取实际字段名（支持重命名）
        List<String> pkColumns = columns.stream()
                .filter(col -> col.getIsPrimaryKey() != null && col.getIsPrimaryKey())
                .map(col -> col.getEffectiveName().toUpperCase())
                .collect(Collectors.toList());
        
        if (pkColumns.isEmpty()) {
            return null;
        }
        
        return "PRIMARY KEY (" + String.join(", ", pkColumns) + ")";
    }
    
    /**
     * 生成GaussDB Oracle模式的外键约束
     */
    private String generateGaussDbOracleForeignKey(ForeignKeyDefinition fk, Map<String, String> columnNameMap) {
        StringBuilder fkDef = new StringBuilder();
        fkDef.append("CONSTRAINT ").append(fk.getConstraintName().toUpperCase()).append(" ");
        fkDef.append("FOREIGN KEY (");
        
        // 本端字段名（应用映射）
        String localColumn = columnNameMap.getOrDefault(fk.getColumnName(), fk.getColumnName());
        fkDef.append(localColumn.toUpperCase()).append(") ");
        
        fkDef.append("REFERENCES ").append(fk.getReferencedTable().toUpperCase()).append(" ");
        fkDef.append("(").append(fk.getReferencedColumn().toUpperCase()).append(")");
        
        // 级联规则
        if (fk.getOnUpdate() != null) {
            // Oracle不支持ON UPDATE CASCADE
            fkDef.append(" -- ON UPDATE ").append(fk.getOnUpdate()).append(" (需要手动处理)");
        }
        if (fk.getOnDelete() != null) {
            fkDef.append(" ON DELETE ").append(fk.getOnDelete());
        }
        
        return fkDef.toString();
    }
    
    /**
     * 生成GaussDB Oracle模式的视图
     */
    private String generateGaussDbOracleCreateView(TableDefinition view) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE OR REPLACE VIEW ").append(view.getTableName()).append(" AS\n");
        
        // 视图定义
        String viewDef = view.getTableComment();
        if (viewDef != null && !viewDef.isEmpty()) {
            sql.append(viewDef);
        } else {
            sql.append("-- TODO: 请补充视图定义");
        }
        sql.append(";");
        
        return sql.toString();
    }
    
    /**
     * 将MySQL类型转换为Oracle类型
     */
    private String convertToOracleType(String mysqlType) {
        if (mysqlType == null) return "VARCHAR2";
        
        String upper = mysqlType.toUpperCase();
        switch (upper) {
            case "VARCHAR":
            case "VARCHAR2":
            case "NVARCHAR":
            case "NVARCHAR2":
                return "VARCHAR2";
            case "TEXT":
            case "LONGTEXT":
            case "MEDIUMTEXT":
                return "CLOB";
            case "TINYTEXT":
                return "VARCHAR2(255)";
            case "INT":
            case "INTEGER":
            case "TINYINT":
            case "SMALLINT":
            case "MEDIUMINT":
                return "NUMBER";
            case "BIGINT":
                return "NUMBER(19)";
            case "FLOAT":
                return "FLOAT";
            case "DOUBLE":
                return "DOUBLE PRECISION";
            case "DECIMAL":
            case "NUMERIC":
                return "NUMBER";
            case "NUMBER":
                return "NUMBER";
            case "DATETIME":
            case "TIMESTAMP":
                return "TIMESTAMP";
            case "DATE":
                return "DATE";
            case "TIME":
                return "TIMESTAMP";
            case "BLOB":
            case "LONGBLOB":
            case "MEDIUMBLOB":
            case "TINYBLOB":
                return "BLOB";
            case "BOOLEAN":
            case "BOOL":
            case "BIT":
                return "NUMBER(1)";
            case "CHAR":
                return "CHAR";
            case "BINARY":
            case "VARBINARY":
                return "RAW";
            default:
                // 未知类型，返回原类型
                return upper;
        }
    }
    
    /**
     * 生成主键约束
     */
    private String generatePrimaryKey(List<ColumnDefinition> columns) {
        // 使用getEffectiveName()获取实际字段名（支持重命名）
        List<String> pkColumns = columns.stream()
                .filter(col -> col.getIsPrimaryKey() != null && col.getIsPrimaryKey())
                .map(ColumnDefinition::getEffectiveName)
                .collect(Collectors.toList());
        
        if (pkColumns.isEmpty()) {
            return null;
        }
        
        return "PRIMARY KEY (" + pkColumns.stream()
                .map(col -> "`" + col + "`")
                .collect(Collectors.joining(", ")) + ")";
    }
    
    /**
     * 生成索引定义
     */
    private String generateIndex(IndexDefinition index, Map<String, String> columnNameMap) {
        StringBuilder idxDef = new StringBuilder();
        
        // 索引类型
        if ("UNIQUE".equalsIgnoreCase(index.getIndexType())) {
            idxDef.append("UNIQUE KEY ");
        } else {
            idxDef.append("KEY ");
        }
        
        idxDef.append("`").append(index.getIndexName()).append("` ");
        
        // 索引字段（应用字段名映射）
        List<String> mappedColumns = new ArrayList<>();
        if (index.getColumns() != null) {
            for (String colName : index.getColumns()) {
                // 如果有新字段名，使用新名称
                String effectiveName = columnNameMap.getOrDefault(colName, colName);
                mappedColumns.add("`" + effectiveName + "`");
            }
        }
        
        idxDef.append("(").append(String.join(", ", mappedColumns)).append(")");
        
        // 注释
        if (index.getComment() != null && !index.getComment().isEmpty()) {
            idxDef.append(" COMMENT '").append(index.getComment().replace("'", "\\'")).append("'");
        }
        
        return idxDef.toString();
    }
    
    /**
     * 生成外键约束
     */
    private String generateForeignKey(ForeignKeyDefinition fk) {
        return generateForeignKey(fk, new java.util.HashMap<>());
    }
    
    /**
     * 生成外键约束（支持字段名映射）
     */
    private String generateForeignKey(ForeignKeyDefinition fk, Map<String, String> columnNameMap) {
        StringBuilder fkDef = new StringBuilder();
        fkDef.append("CONSTRAINT `").append(fk.getConstraintName()).append("` ");
        fkDef.append("FOREIGN KEY (");
        
        // 本端字段名（应用映射）
        String localColumn = columnNameMap.getOrDefault(fk.getColumnName(), fk.getColumnName());
        fkDef.append("`").append(localColumn).append("`) ");
        
        fkDef.append("REFERENCES `").append(fk.getReferencedTable()).append("` ");
        fkDef.append("(`").append(fk.getReferencedColumn()).append("`)");
        
        // 级联规则
        if (fk.getOnUpdate() != null) {
            fkDef.append(" ON UPDATE ").append(fk.getOnUpdate());
        }
        if (fk.getOnDelete() != null) {
            fkDef.append(" ON DELETE ").append(fk.getOnDelete());
        }
        
        return fkDef.toString();
    }
    
    /**
     * 判断是否为数值类型
     */
    private boolean isNumericType(String dataType) {
        if (dataType == null) return false;
        String upper = dataType.toUpperCase();
        return upper.equals("INT") || upper.equals("INTEGER") || 
               upper.equals("BIGINT") || upper.equals("SMALLINT") ||
               upper.equals("TINYINT") || upper.equals("DECIMAL") ||
               upper.equals("NUMERIC") || upper.equals("FLOAT") ||
               upper.equals("DOUBLE") || upper.startsWith("NUMBER");
    }
    
    /**
     * 判断是否为不需要指定长度的类型
     * MySQL中以下类型不需要（也不能）指定长度：
     * - TEXT系列: TINYTEXT, TEXT, MEDIUMTEXT, LONGTEXT
     * - BLOB系列: TINYBLOB, BLOB, MEDIUMBLOB, LONGBLOB
     * - 其他: JSON, GEOMETRY, POINT, LINESTRING, POLYGON等
     */
    private boolean isTypeWithoutLength(String dataType) {
        if (dataType == null) return false;
        String upper = dataType.toUpperCase();
        
        // TEXT系列
        if (upper.equals("TEXT") || upper.equals("TINYTEXT") || 
            upper.equals("MEDIUMTEXT") || upper.equals("LONGTEXT")) {
            return true;
        }
        
        // BLOB系列
        if (upper.equals("BLOB") || upper.equals("TINYBLOB") || 
            upper.equals("MEDIUMBLOB") || upper.equals("LONGBLOB")) {
            return true;
        }
        
        // JSON类型
        if (upper.equals("JSON")) {
            return true;
        }
        
        // 空间数据类型
        if (upper.equals("GEOMETRY") || upper.equals("POINT") || 
            upper.equals("LINESTRING") || upper.equals("POLYGON") ||
            upper.equals("MULTIPOINT") || upper.equals("MULTILINESTRING") ||
            upper.equals("MULTIPOLYGON") || upper.equals("GEOMETRYCOLLECTION")) {
            return true;
        }
        
        // ENUM和SET也不需要长度（它们的定义在括号内是值列表，不是长度）
        if (upper.equals("ENUM") || upper.equals("SET")) {
            return true;
        }
        
        return false;
    }
}
