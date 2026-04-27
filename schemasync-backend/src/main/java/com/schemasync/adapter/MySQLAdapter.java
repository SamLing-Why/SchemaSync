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
 * MySQL数据库适配器
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Component
public class MySQLAdapter implements DatabaseAdapter {

    private static final Logger log = LoggerFactory.getLogger(MySQLAdapter.class);

    /** 查询表列表 */
    private static final String QUERY_TABLES = 
        "SELECT TABLE_NAME, TABLE_COMMENT, TABLE_TYPE, ENGINE, TABLE_COLLATION, CREATE_TIME, UPDATE_TIME " +
        "FROM INFORMATION_SCHEMA.TABLES " +
        "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE IN ('BASE TABLE', 'VIEW') " +
        "ORDER BY TABLE_NAME";

    /** 查询字段列表 */
    private static final String QUERY_COLUMNS = 
        "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, " +
        "NUMERIC_PRECISION, NUMERIC_SCALE, IS_NULLABLE, COLUMN_DEFAULT, " +
        "COLUMN_KEY, EXTRA, COLUMN_COMMENT, CHARACTER_SET_NAME, ORDINAL_POSITION " +
        "FROM INFORMATION_SCHEMA.COLUMNS " +
        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
        "ORDER BY ORDINAL_POSITION";

    /** 查询索引列表 */
    private static final String QUERY_INDEXES = 
        "SELECT INDEX_NAME, NON_UNIQUE, INDEX_TYPE, " +
        "GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS " +
        "FROM INFORMATION_SCHEMA.STATISTICS " +
        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
        "GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_TYPE";

    /** 查询外键列表 */
    private static final String QUERY_FOREIGN_KEYS = 
        "SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
        "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND REFERENCED_TABLE_NAME IS NOT NULL";

    @Override
    public Connection connect(DataSourceConfig config) throws SQLException {
        return ConnectionPoolManager.getConnection(config);
    }

    @Override
    public boolean testConnection(DataSourceConfig config) {
        try (Connection conn = connect(config)) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return false;
        }
    }

    @Override
    public List<String> getDatabases(Connection conn) throws SQLException {
        List<String> databases = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            while (rs.next()) {
                String dbName = rs.getString(1);
                // 过滤系统数据库
                if (!isSystemDatabase(dbName)) {
                    databases.add(dbName);
                }
            }
        }
        return databases;
    }

    @Override
    public List<TableDefinition> getTables(Connection conn, String database) throws SQLException {
        List<TableDefinition> tables = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_TABLES)) {
            pstmt.setString(1, database);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TableDefinition table = new TableDefinition();
                    table.setTableName(rs.getString("TABLE_NAME"));
                    table.setTableComment(rs.getString("TABLE_COMMENT"));
                    table.setTableType(rs.getString("TABLE_TYPE"));
                    table.setEngine(rs.getString("ENGINE"));
                    table.setCharset(rs.getString("TABLE_COLLATION"));
                    
                    Timestamp createTime = rs.getTimestamp("CREATE_TIME");
                    if (createTime != null) {
                        table.setCreateTime(new java.util.Date(createTime.getTime()));
                    }
                    
                    Timestamp updateTime = rs.getTimestamp("UPDATE_TIME");
                    if (updateTime != null) {
                        table.setUpdateTime(new java.util.Date(updateTime.getTime()));
                    }
                    
                    tables.add(table);
                }
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
                    column.setIsPrimaryKey("PRI".equals(rs.getString("COLUMN_KEY")));
                    column.setIsAutoIncrement(rs.getString("EXTRA").contains("auto_increment"));
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
    public List<IndexDefinition> getIndexes(Connection conn, String database, String tableName) throws SQLException {
        List<IndexDefinition> indexes = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_INDEXES)) {
            pstmt.setString(1, database);
            pstmt.setString(2, tableName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    IndexDefinition index = new IndexDefinition();
                    index.setIndexName(rs.getString("INDEX_NAME"));
                    index.setIndexType(rs.getString("INDEX_TYPE"));
                    index.setIsUnique(!rs.getBoolean("NON_UNIQUE"));
                    
                    // 解析字段列表
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
    public List<ForeignKeyDefinition> getForeignKeys(Connection conn, String database, String tableName) throws SQLException {
        List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_FOREIGN_KEYS)) {
            pstmt.setString(1, database);
            pstmt.setString(2, tableName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ForeignKeyDefinition fk = new ForeignKeyDefinition();
                    fk.setConstraintName(rs.getString("CONSTRAINT_NAME"));
                    fk.setColumnName(rs.getString("COLUMN_NAME"));
                    fk.setReferencedTable(rs.getString("REFERENCED_TABLE_NAME"));
                    fk.setReferencedColumn(rs.getString("REFERENCED_COLUMN"));
                    
                    foreignKeys.add(fk);
                }
            }
        }
        
        return foreignKeys;
    }

    @Override
    public SchemaDictionary exportSchema(DataSourceConfig config, ExportOptions options) throws SQLException {
        log.info("开始导出数据字典: {} - {}", config.getName(), options.getDatabase());
        
        SchemaDictionary dictionary = new SchemaDictionary();
        
        // 设置元数据
        ExportMetadata metadata = new ExportMetadata();
        metadata.setExportTime(new Date());
        metadata.setDatabaseType("MySQL");
        metadata.setDatabaseName(options.getDatabase());
        metadata.setToolVersion("1.0.0");
        dictionary.setMetadata(metadata);
        
        // 连接数据库并导出数据
        try (Connection conn = connect(config)) {
            metadata.setDatabaseVersion(getDatabaseVersion(conn));
            
            // 获取所有表
            List<TableDefinition> tables = getTables(conn, options.getDatabase());
            
            // 过滤表
            if (options.getTablePattern() != null && !options.getTablePattern().isEmpty()) {
                tables = filterTablesByPattern(tables, options.getTablePattern());
            }
            if (options.getExcludeTables() != null && !options.getExcludeTables().isEmpty()) {
                tables = excludeTables(tables, options.getExcludeTables());
            }
            
            // 导出每个表的详细信息
            for (TableDefinition table : tables) {
                log.debug("导出表: {}", table.getTableName());
                
                // 字段
                table.setColumns(getColumns(conn, options.getDatabase(), table.getTableName()));
                
                // 索引
                if (options.getIncludeIndexes()) {
                    table.setIndexes(getIndexes(conn, options.getDatabase(), table.getTableName()));
                }
                
                // 外键
                if (options.getIncludeForeignKeys()) {
                    table.setForeignKeys(getForeignKeys(conn, options.getDatabase(), table.getTableName()));
                }
            }
            
            dictionary.setTables(tables);
            log.info("数据字典导出完成, 共{}个表", tables.size());
        }
        
        return dictionary;
    }

    @Override
    public String getDatabaseType() {
        return "MYSQL";
    }

    @Override
    public String getDatabaseVersion(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT VERSION()")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "unknown";
    }

    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(DataSourceConfig config) {
        return String.format(
            "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=%s&useSSL=false&serverTimezone=Asia/Shanghai",
            config.getHost(),
            config.getPort(),
            config.getDatabase(),
            config.getCharset() != null ? config.getCharset() : "utf8mb4"
        );
    }

    /**
     * 判断是否为系统数据库
     */
    private boolean isSystemDatabase(String dbName) {
        return dbName.equals("information_schema") ||
               dbName.equals("mysql") ||
               dbName.equals("performance_schema") ||
               dbName.equals("sys");
    }

    /**
     * 根据模式过滤表
     */
    private List<TableDefinition> filterTablesByPattern(List<TableDefinition> tables, String pattern) {
        String regex = pattern.replace("*", ".*").replace("?", ".");
        return tables.stream()
                .filter(t -> t.getTableName().matches(regex))
                .toList();
    }

    /**
     * 排除指定的表
     */
    private List<TableDefinition> excludeTables(List<TableDefinition> tables, List<String> excludePatterns) {
        return tables.stream()
                .filter(t -> excludePatterns.stream()
                        .noneMatch(pattern -> {
                            String regex = pattern.replace("*", ".*").replace("?", ".");
                            return t.getTableName().matches(regex);
                        }))
                .toList();
    }
}
