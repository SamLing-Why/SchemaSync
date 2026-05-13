package com.schemasync.adapter;

import com.schemasync.model.config.DataSourceConfig;
import com.schemasync.model.dict.*;
import com.schemasync.util.ConnectionPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * GaussDB数据库适配器 (华为云数据库)
 * GaussDB兼容PostgreSQL协议,使用PostgreSQL驱动连接
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Component
public class GaussDBAdapter implements DatabaseAdapter {

    private static final Logger log = LoggerFactory.getLogger(GaussDBAdapter.class);

    // 使用information_schema.tables，兼容性更好
    // GaussDB 9.2.4支持表注释，使用obj_description获取
    private static final String QUERY_TABLES = 
        "SELECT " +
        "    t.table_name AS TABLE_NAME, " +
        "    obj_description((t.table_schema || '.' || t.table_name)::regclass, 'pg_class') AS TABLE_COMMENT, " +
        "    t.table_type AS TABLE_TYPE, " +
        "    NULL AS CREATE_TIME " +
        "FROM information_schema.tables t " +
        "WHERE t.table_schema = ? " +
        "  AND t.table_type IN ('BASE TABLE', 'VIEW') " +
        "ORDER BY t.table_name";

    private static final String QUERY_COLUMNS =
        "SELECT " +
        "    a.attname AS COLUMN_NAME, " +
        "    t.typname AS TYPE_NAME, " +
        "    pg_catalog.format_type(a.atttypid, a.atttypmod) AS FORMAT_TYPE, " +
        "    a.atttypmod AS ATT_TYPE_MOD, " +
        "    CASE WHEN a.attnotnull THEN 'NO' ELSE 'YES' END AS IS_NULLABLE, " +
        "    pg_get_expr(ad.adbin, ad.adrelid) AS COLUMN_DEFAULT, " +
        "    CASE WHEN pk.contype = 'p' THEN 'PRI' ELSE '' END AS COLUMN_KEY, " +
        "    col_description(a.attrelid, a.attnum) AS COLUMN_COMMENT, " +
        "    a.attnum AS ORDINAL_POSITION " +
        "FROM pg_attribute a " +
        "JOIN pg_class c ON a.attrelid = c.oid " +
        "JOIN pg_type t ON a.atttypid = t.oid " +
        "JOIN pg_namespace n ON c.relnamespace = n.oid " +
        "LEFT JOIN pg_attrdef ad ON a.attrelid = ad.adrelid AND a.attnum = ad.adnum " +
        "LEFT JOIN ( " +
        "    SELECT conrelid, conkey[1] AS attnum, contype " +
        "    FROM pg_constraint " +
        "    WHERE contype = 'p' " +
        ") pk ON c.oid = pk.conrelid AND a.attnum = pk.attnum " +
        "WHERE n.nspname = ? AND c.relname = ? AND a.attnum > 0 AND NOT a.attisdropped " +
        "ORDER BY a.attnum";

    private static final String QUERY_INDEXES = 
        "SELECT " +
        "    i.relname AS INDEX_NAME, " +
        "    CASE WHEN ix.indisunique THEN 0 ELSE 1 END AS NON_UNIQUE, " +
        "    CASE WHEN ix.indisprimary THEN 'PRIMARY' ELSE 'INDEX' END AS INDEX_TYPE, " +
        "    array_to_string(array_agg(a.attname ORDER BY a.attnum), ',') AS COLUMNS " +
        "FROM pg_index ix " +
        "JOIN pg_class c ON c.oid = ix.indrelid " +
        "JOIN pg_class i ON i.oid = ix.indexrelid " +
        "JOIN pg_namespace n ON c.relnamespace = n.oid " +
        "JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(ix.indkey) " +
        "WHERE n.nspname = ? AND c.relname = ? " +
        "GROUP BY i.relname, ix.indisunique, ix.indisprimary";

    private static final String QUERY_FOREIGN_KEYS = 
        "SELECT " +
        "    tc.constraint_name AS CONSTRAINT_NAME, " +
        "    kcu.column_name AS COLUMN_NAME, " +
        "    ccu.table_name AS REFERENCED_TABLE_NAME, " +
        "    ccu.column_name AS REFERENCED_COLUMN_NAME " +
        "FROM information_schema.table_constraints tc " +
        "JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name " +
        "JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name " +
        "WHERE tc.constraint_type = 'FOREIGN KEY' " +
        "    AND tc.table_schema = ? " +
        "    AND tc.table_name = ?";

    @Override
    public String getDatabaseType() {
        return "GAUSSDB";
    }

    @Override
    public boolean supportsSchema() {
        return true; // GaussDB支持SCHEMA层级结构
    }

    @Override
    public List<String> getDatabases(Connection conn) throws SQLException {
        List<String> databases = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false")) {
            while (rs.next()) {
                String dbName = rs.getString("datname");
                // 排除系统数据库
                if (!dbName.equals("postgres") && 
                    !dbName.equals("template0") && 
                    !dbName.equals("template1")) {
                    databases.add(dbName);
                }
            }
        }
        return databases;
    }

    /**
     * 获取SCHEMA列表(GaussDB/OpenGauss特有)
     * GaussDB层级: 数据库 → SCHEMA → 表
     * OpenGauss: 默认只有public SCHEMA，支持手动创建多SCHEMA
     */
    public List<String> getSchemas(Connection conn) throws SQLException {
        List<String> schemas = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT nspname FROM pg_namespace " +
                 "WHERE nspname NOT IN ('pg_catalog', 'pg_toast', 'information_schema') " +
                 "AND nspname NOT LIKE 'pg_temp_%' " +
                 "AND nspname NOT LIKE 'pg_toast_temp_%' " +
                 "ORDER BY nspname")) {
            while (rs.next()) {
                String schemaName = rs.getString("nspname");
                schemas.add(schemaName);
            }
        }
        
        log.info("OpenGauss/GaussDB查询到 {} 个SCHEMA: {}", schemas.size(), schemas);
        
        // 如果没有找到任何SCHEMA，默认返回public
        if (schemas.isEmpty()) {
            schemas.add("public");
        }
        
        return schemas;
    }

    @Override
    public String getDatabaseVersion(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT VERSION()")) {
            if (rs.next()) {
                return "GaussDB " + rs.getString(1);
            }
        }
        return "GaussDB Unknown";
    }

    @Override
    public Connection connect(DataSourceConfig config) throws SQLException {
        return ConnectionPoolManager.getConnection(config);
    }

    @Override
    public boolean testConnection(DataSourceConfig config) {
        try (Connection conn = connect(config)) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            log.error("GaussDB连接测试失败", e);
            return false;
        }
    }

    @Override
    public SchemaDictionary exportSchema(DataSourceConfig config, ExportOptions options) throws SQLException {
        long startTime = System.currentTimeMillis();
        log.info("========== 开始导出GaussDB数据字典 ==========");
        log.info("数据库: {}, SCHEMA: {}", options.getDatabase(), options.getSchema());
        
        SchemaDictionary dictionary = new SchemaDictionary();
        
        ExportMetadata metadata = new ExportMetadata();
        metadata.setExportTime(new Date());
        metadata.setDatabaseType("GaussDB");
        metadata.setDatabaseName(options.getDatabase());
        metadata.setSchemaName(options.getSchema());
        dictionary.setMetadata(metadata);
        
        try (Connection conn = connect(config)) {
            long connTime = System.currentTimeMillis();
            log.info("[1/4] 数据库连接成功, 耗时: {}ms", connTime - startTime);
            
            String schema = options.getSchema();
            if (schema == null || schema.trim().isEmpty()) {
                schema = detectSchema(conn, options.getDatabase());
                log.info("未指定SCHEMA，自动检测到: {}", schema);
            } else {
                log.info("使用用户指定的SCHEMA: {}", schema);
            }
            
            log.info("[2/4] 开始查询表列表...");
            long tableQueryStart = System.currentTimeMillis();
            List<TableDefinition> tables = getTables(conn, schema);
            long tableQueryTime = System.currentTimeMillis() - tableQueryStart;
            log.info("获取到 {} 张表, 耗时: {}ms", tables.size(), tableQueryTime);
            
            if (options.getTablePattern() != null) {
                int beforeFilter = tables.size();
                String pattern = options.getTablePattern().replace("%", ".*");
                tables.removeIf(t -> !t.getTableName().matches(pattern));
                log.info("按模式过滤后: {} 张表 (原 {} 张)", tables.size(), beforeFilter);
            }
            
            if (options.getExcludeTables() != null && !options.getExcludeTables().isEmpty()) {
                int beforeExclude = tables.size();
                tables.removeIf(t -> options.getExcludeTables().contains(t.getTableName()));
                log.info("排除指定表后: {} 张表 (原 {} 张)", tables.size(), beforeExclude);
            }
            
            log.info("[3/4] 开始导出表详细信息...");
            long detailStart = System.currentTimeMillis();
            int exportedCount = 0;
            
            for (TableDefinition table : tables) {
                exportedCount++;
                long tableStart = System.currentTimeMillis();
                
                table.setColumns(getColumns(conn, schema, table.getTableName()));
                
                if (options.getIncludeIndexes()) {
                    table.setIndexes(getIndexes(conn, schema, table.getTableName()));
                }
                
                if (options.getIncludeForeignKeys()) {
                    table.setForeignKeys(getForeignKeys(conn, schema, table.getTableName()));
                }
                
                long tableTime = System.currentTimeMillis() - tableStart;
                
                if (exportedCount % 10 == 0 || exportedCount == tables.size()) {
                    log.info("进度: {}/{} 张表 ({}%), 当前表: {}, 耗时: {}ms", 
                        exportedCount, tables.size(), 
                        (exportedCount * 100 / tables.size()),
                        table.getTableName(),
                        tableTime);
                }
            }
            
            long detailTime = System.currentTimeMillis() - detailStart;
            log.info("[3/4] 表详细信息导出完成, 共 {} 张表, 耗时: {}ms", tables.size(), detailTime);
            
            dictionary.setTables(tables);
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[4/4] 数据字典组装完成");
            log.info("========== GaussDB数据字典导出完成 ==========");
            log.info("总计耗时: {}ms ({}秒), 导出表数: {}", 
                totalTime, totalTime / 1000.0, tables.size());
        }
        
        return dictionary;
    }

    /**
     * 检测数据库中的schema
     * 优先使用有用户表的schema，排除系统schema
     */
    private String detectSchema(Connection conn, String database) throws SQLException {
        // 先尝试获取非系统schema且有表的
        String sql = "SELECT schemaname FROM pg_tables " +
                     "WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pg_toast') " +
                     "GROUP BY schemaname " +
                     "ORDER BY COUNT(*) DESC " +
                     "LIMIT 1";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString("schemaname");
            }
        }
        
        // 如果没找到，返回public
        return "public";
    }

    @Override
    public List<TableDefinition> getTables(Connection conn, String schema) throws SQLException {
        List<TableDefinition> tables = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_TABLES)) {
            pstmt.setString(1, schema);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TableDefinition table = new TableDefinition();
                    table.setTableName(rs.getString("TABLE_NAME"));
                    table.setTableComment(rs.getString("TABLE_COMMENT"));
                    
                    // 表类型
                    String tableType = rs.getString("TABLE_TYPE");
                    if (tableType != null) {
                        table.setTableType(tableType);
                    }
                    
                    // 创建时间
                    Timestamp createTime = rs.getTimestamp("CREATE_TIME");
                    if (createTime != null) {
                        table.setCreateTime(new Date(createTime.getTime()));
                    }
                    
                    tables.add(table);
                }
            }
        }
        
        return tables;
    }

    @Override
    public List<ColumnDefinition> getColumns(Connection conn, String schema, String tableName) throws SQLException {
        List<ColumnDefinition> columns = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_COLUMNS)) {
            pstmt.setString(1, schema);
            pstmt.setString(2, tableName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ColumnDefinition column = new ColumnDefinition();
                    column.setColumnName(rs.getString("COLUMN_NAME"));
                    
                    // 获取数据类型名称
                    String typeName = rs.getString("TYPE_NAME");
                    String dataType = convertToStandardTypeName(typeName);
                    column.setDataType(dataType);
                    
                    // 获取format_type的完整格式，如 "varchar(64)" 或 "numeric(25,2)"
                    String formatType = rs.getString("FORMAT_TYPE");
                    int attTypMod = rs.getInt("ATT_TYPE_MOD");
                    
                    // 解析长度和精度
                    parseTypeLengthAndPrecision(dataType, formatType, attTypMod, column);
                    
                    column.setNullable("YES".equals(rs.getString("IS_NULLABLE")));
                    
                    String defaultValue = rs.getString("COLUMN_DEFAULT");
                    if (defaultValue != null) {
                        column.setDefaultValue(defaultValue);
                    }
                    
                    String columnKey = rs.getString("COLUMN_KEY");
                    column.setIsPrimaryKey("PRI".equals(columnKey));
                    
                    column.setComment(rs.getString("COLUMN_COMMENT"));
                    column.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                    
                    columns.add(column);
                }
            }
        }
        
        return columns;
    }
    
    /**
     * 解析类型长度和精度
     * 兼容OpenGauss和GaussDB的差异
     */
    private void parseTypeLengthAndPrecision(String dataType, String formatType, int attTypMod, ColumnDefinition column) {
        if (formatType == null || formatType.isEmpty()) {
            return;
        }
            
        // 从formatType中解析括号内的值
        // 例如: varchar(64) -> 64, numeric(25,2) -> 25,2
        int openParen = formatType.indexOf('(');
        int closeParen = formatType.indexOf(')');
            
        if (openParen > 0 && closeParen > openParen) {
            String params = formatType.substring(openParen + 1, closeParen);
            String[] parts = params.split(",");
                
            if ("numeric".equalsIgnoreCase(dataType) || "decimal".equalsIgnoreCase(dataType) || "number".equalsIgnoreCase(dataType)) {
                // numeric(precision, scale) 或 numeric(precision)
                if (parts.length >= 1) {
                    try {
                        long precision = Long.parseLong(parts[0].trim());
                        // GaussDB商业版可能有异常大的精度值，需要限制
                        if (precision > 0 && precision < 10000) {
                            column.setPrecision(precision);
                        } else {
                            log.debug("numeric精度值异常，已忽略: {} (格式: {})", precision, formatType);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("解析numeric精度失败: {}", parts[0]);
                    }
                }
                if (parts.length >= 2) {
                    try {
                        long scale = Long.parseLong(parts[1].trim());
                        if (scale >= 0 && scale < 10000) {
                            column.setScale(scale);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("解析numeric小数位失败: {}", parts[1]);
                    }
                }
            } else if ("varchar".equalsIgnoreCase(dataType) || "char".equalsIgnoreCase(dataType) || 
                       "nvarchar".equalsIgnoreCase(dataType) || "nvarchar2".equalsIgnoreCase(dataType) ||
                       "bpchar".equalsIgnoreCase(dataType)) {
                // varchar(length) 或 char(length) 或 nvarchar2(length)
                if (parts.length >= 1) {
                    try {
                        long length = Long.parseLong(parts[0].trim());
                        if (length > 0 && length < 1000000) {
                            column.setLength(length);
                        } else {
                            log.debug("字符类型长度异常，已忽略: {} (格式: {})", length, formatType);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("解析字符类型长度失败: {}", parts[0]);
                    }
                }
            }
        }
    }
    
    /**
     * 将PostgreSQL内部类型名转换为标准DDL类型名
     */
    private String convertToStandardTypeName(String typeName) {
        if (typeName == null) {
            return "unknown";
        }
        
        switch (typeName.toLowerCase()) {
            case "varchar":
            case "character varying":
                return "varchar";
            case "char":
            case "character":
                return "char";
            case "bpchar":
                return "char";
            case "nvarchar":
            case "nvarchar2":
                return "nvarchar2";
            case "int2":
                return "smallint";
            case "int4":
                return "integer";
            case "int8":
                return "bigint";
            case "float4":
                return "real";
            case "float8":
                return "double precision";
            case "bool":
                return "boolean";
            case "timestamp":
                return "timestamp";
            case "timestamptz":
                return "timestamp with time zone";
            case "timetz":
                return "time with time zone";
            case "interval":
                return "interval";
            case "numeric":
            case "decimal":
                return "numeric";
            case "number":
                return "number";
            case "text":
                return "text";
            case "bytea":
                return "bytea";
            case "json":
                return "json";
            case "jsonb":
                return "jsonb";
            case "uuid":
                return "uuid";
            case "xml":
                return "xml";
            case "inet":
                return "inet";
            case "macaddr":
                return "macaddr";
            case "cidr":
                return "cidr";
            default:
                return typeName.toLowerCase();
        }
    }

    @Override
    public List<IndexDefinition> getIndexes(Connection conn, String schema, String tableName) throws SQLException {
        List<IndexDefinition> indexes = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_INDEXES)) {
            pstmt.setString(1, schema);
            pstmt.setString(2, tableName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    IndexDefinition index = new IndexDefinition();
                    index.setIndexName(rs.getString("INDEX_NAME"));
                    index.setIndexType(rs.getString("INDEX_TYPE"));
                    index.setIsUnique(rs.getInt("NON_UNIQUE") == 0);
                    
                    String columnsStr = rs.getString("COLUMNS");
                    if (columnsStr != null) {
                        index.setColumns(Arrays.asList(columnsStr.split(",")));
                    }
                    
                    indexes.add(index);
                }
            }
        }
        
        return indexes;
    }

    @Override
    public List<ForeignKeyDefinition> getForeignKeys(Connection conn, String schema, String tableName) throws SQLException {
        List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_FOREIGN_KEYS)) {
            pstmt.setString(1, schema);
            pstmt.setString(2, tableName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ForeignKeyDefinition fk = new ForeignKeyDefinition();
                    fk.setConstraintName(rs.getString("CONSTRAINT_NAME"));
                    fk.setColumnName(rs.getString("COLUMN_NAME"));
                    fk.setReferencedTable(rs.getString("REFERENCED_TABLE_NAME"));
                    fk.setReferencedColumn(rs.getString("REFERENCED_COLUMN_NAME"));
                    foreignKeys.add(fk);
                }
            }
        }
        
        return foreignKeys;
    }
}
