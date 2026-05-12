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
 * GoldenDB数据库适配器 (中兴分布式数据库)
 * GoldenDB兼容MySQL协议,使用MySQL驱动连接
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Component
public class GoldenDBAdapter implements DatabaseAdapter {

    private static final Logger log = LoggerFactory.getLogger(GoldenDBAdapter.class);

    // GoldenDB使用MySQL兼容模式
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
        "SELECT INDEX_NAME, NON_UNIQUE, INDEX_TYPE, " +
        "GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS " +
        "FROM INFORMATION_SCHEMA.STATISTICS " +
        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
        "GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_TYPE";

    private static final String QUERY_FOREIGN_KEYS = 
        "SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
        "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND REFERENCED_TABLE_NAME IS NOT NULL";

    @Override
    public String getDatabaseType() {
        return "GOLDENDB";
    }

    @Override
    public List<String> getDatabases(Connection conn) throws SQLException {
        List<String> databases = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            while (rs.next()) {
                String dbName = rs.getString(1);
                // 排除系统数据库
                if (!dbName.equals("information_schema") && 
                    !dbName.equals("mysql") && 
                    !dbName.equals("performance_schema") &&
                    !dbName.equals("sys") &&
                    !dbName.equals("goldendb")) {
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
                return "GoldenDB " + rs.getString(1);
            }
        }
        return "GoldenDB Unknown";
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
            log.error("GoldenDB连接测试失败", e);
            return false;
        }
    }

    @Override
    public SchemaDictionary exportSchema(DataSourceConfig config, ExportOptions options) throws SQLException {
        long startTime = System.currentTimeMillis();
        log.info("========== 开始导出GoldenDB数据字典 ==========");
        log.info("数据库: {}", options.getDatabase());
        
        SchemaDictionary dictionary = new SchemaDictionary();
        
        ExportMetadata metadata = new ExportMetadata();
        metadata.setExportTime(new Date());
        metadata.setDatabaseType("GoldenDB");
        metadata.setDatabaseName(options.getDatabase());
        dictionary.setMetadata(metadata);
        
        try (Connection conn = connect(config)) {
            long connTime = System.currentTimeMillis();
            log.info("[1/4] 数据库连接成功, 耗时: {}ms", connTime - startTime);
            
            log.info("[2/4] 开始查询表列表...");
            long tableQueryStart = System.currentTimeMillis();
            List<TableDefinition> tables = getTables(conn, options.getDatabase());
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
                
                table.setColumns(getColumns(conn, options.getDatabase(), table.getTableName()));
                
                if (options.getIncludeIndexes()) {
                    table.setIndexes(getIndexes(conn, options.getDatabase(), table.getTableName()));
                }
                
                if (options.getIncludeForeignKeys()) {
                    table.setForeignKeys(getForeignKeys(conn, options.getDatabase(), table.getTableName()));
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
            log.info("========== GoldenDB数据字典导出完成 ==========");
            log.info("总计耗时: {}ms ({}秒), 导出表数: {}", 
                totalTime, totalTime / 1000.0, tables.size());
        }
        
        return dictionary;
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
                    column.setDataType(rs.getString("DATA_TYPE"));
                    
                    // 使用Long支持超大值
                    long length = rs.getLong("CHARACTER_MAXIMUM_LENGTH");
                    if (!rs.wasNull()) {
                        column.setLength(length);
                    }
                    
                    long precision = rs.getLong("NUMERIC_PRECISION");
                    if (!rs.wasNull()) {
                        column.setPrecision(precision);
                    }
                    
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
                    column.setIsAutoIncrement(extra != null && extra.contains("auto_increment"));
                    
                    column.setComment(rs.getString("COLUMN_COMMENT"));
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
                    fk.setReferencedColumn(rs.getString("REFERENCED_COLUMN_NAME"));
                    foreignKeys.add(fk);
                }
            }
        }
        
        return foreignKeys;
    }
}
