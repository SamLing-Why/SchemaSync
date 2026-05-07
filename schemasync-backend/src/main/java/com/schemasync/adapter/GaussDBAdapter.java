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

    // GaussDB使用PostgreSQL兼容模式，补充表类型和创建时间
    private static final String QUERY_TABLES = 
        "SELECT " +
        "    t.tablename AS TABLE_NAME, " +
        "    obj_description((t.schemaname || '.' || t.tablename)::regclass) AS TABLE_COMMENT, " +
        "    CASE c.relkind " +
        "        WHEN 'r' THEN 'BASE TABLE' " +
        "        WHEN 'v' THEN 'VIEW' " +
        "        WHEN 'm' THEN 'MATERIALIZED VIEW' " +
        "        ELSE 'UNKNOWN' " +
        "    END AS TABLE_TYPE, " +
        "    NULL AS CREATE_TIME " +  // GaussDB不直接提供表创建时间
        "FROM pg_tables t " +
        "JOIN pg_class c ON c.relname = t.tablename " +
        "JOIN pg_namespace n ON n.oid = c.relnamespace AND n.nspname = t.schemaname " +
        "WHERE t.schemaname = ? " +
        "ORDER BY t.tablename";

    private static final String QUERY_COLUMNS = 
        "SELECT " +
        "    a.attname AS COLUMN_NAME, " +
        "    pg_catalog.format_type(a.atttypid, a.atttypmod) AS DATA_TYPE, " +
        "    CASE " +
        "        WHEN t.typname IN ('varchar', 'char', 'bpchar') THEN information_schema._pg_char_max_length(a.atttypid, a.atttypmod) " +
        "        ELSE NULL " +
        "    END AS CHARACTER_MAXIMUM_LENGTH, " +
        "    CASE " +
        "        WHEN t.typname IN ('numeric', 'decimal') THEN information_schema._pg_numeric_precision(a.atttypid, a.atttypmod) " +
        "        ELSE NULL " +
        "    END AS NUMERIC_PRECISION, " +
        "    CASE " +
        "        WHEN t.typname IN ('numeric', 'decimal') THEN information_schema._pg_numeric_scale(a.atttypid, a.atttypmod) " +
        "        ELSE NULL " +
        "    END AS NUMERIC_SCALE, " +
        "    CASE WHEN a.attnotnull THEN 'NO' ELSE 'YES' END AS IS_NULLABLE, " +
        "    pg_get_expr(ad.adbin, ad.adrelid) AS COLUMN_DEFAULT, " +
        "    CASE WHEN pk.contype = 'p' THEN 'PRI' ELSE '' END AS COLUMN_KEY, " +
        "    CASE WHEN a.atthasdef THEN 'default' ELSE '' END AS EXTRA, " +
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
        SchemaDictionary dictionary = new SchemaDictionary();
        
        ExportMetadata metadata = new ExportMetadata();
        metadata.setExportTime(new Date());
        metadata.setDatabaseType("GaussDB");
        metadata.setDatabaseName(options.getDatabase());
        dictionary.setMetadata(metadata);
        
        try (Connection conn = connect(config)) {
            // 自动检测schema，不局限于public
            String schema = detectSchema(conn, options.getDatabase());
            log.info("GaussDB检测到schema: {}", schema);
            
            List<TableDefinition> tables = getTables(conn, schema);
            log.info("GaussDB获取到{}张表", tables.size());
            
            if (options.getTablePattern() != null) {
                String pattern = options.getTablePattern().replace("%", ".*");
                tables.removeIf(t -> !t.getTableName().matches(pattern));
            }
            
            if (options.getExcludeTables() != null && !options.getExcludeTables().isEmpty()) {
                tables.removeIf(t -> options.getExcludeTables().contains(t.getTableName()));
            }
            
            for (TableDefinition table : tables) {
                table.setColumns(getColumns(conn, schema, table.getTableName()));
                
                if (options.getIncludeIndexes()) {
                    table.setIndexes(getIndexes(conn, schema, table.getTableName()));
                }
                
                if (options.getIncludeForeignKeys()) {
                    table.setForeignKeys(getForeignKeys(conn, schema, table.getTableName()));
                }
            }
            
            dictionary.setTables(tables);
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
                    
                    // 解析数据类型(去掉长度信息)
                    String fullType = rs.getString("DATA_TYPE");
                    String dataType = fullType.split("\\(")[0];
                    column.setDataType(dataType);
                    
                    // 长度 - 使用Long支持超大值
                    long length = rs.getLong("CHARACTER_MAXIMUM_LENGTH");
                    if (!rs.wasNull()) {
                        column.setLength(length);
                    }
                    
                    // 精度
                    long precision = rs.getLong("NUMERIC_PRECISION");
                    if (!rs.wasNull()) {
                        column.setPrecision(precision);
                    }
                    
                    // 小数位
                    long scale = rs.getLong("NUMERIC_SCALE");
                    if (!rs.wasNull()) {
                        column.setScale(scale);
                    }
                    
                    column.setNullable("YES".equals(rs.getString("IS_NULLABLE")));
                    
                    String defaultValue = rs.getString("COLUMN_DEFAULT");
                    if (defaultValue != null) {
                        column.setDefaultValue(defaultValue);
                    }
                    
                    String columnKey = rs.getString("COLUMN_KEY");
                    column.setIsPrimaryKey("PRI".equals(columnKey));
                    
                    String extra = rs.getString("EXTRA");
                    column.setIsAutoIncrement(extra != null && extra.contains("default"));
                    
                    column.setComment(rs.getString("COLUMN_COMMENT"));
                    column.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                    
                    columns.add(column);
                }
            }
        }
        
        return columns;
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
