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
 * Oracle数据库适配器
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Component
public class OracleAdapter implements DatabaseAdapter {

    private static final Logger log = LoggerFactory.getLogger(OracleAdapter.class);

    // 查询所有表
    private static final String QUERY_TABLES = 
            "SELECT TABLE_NAME, COMMENTS AS TABLE_COMMENT " +
            "FROM ALL_TABLES t " +
            "LEFT JOIN ALL_TAB_COMMENTS c ON t.TABLE_NAME = c.TABLE_NAME AND t.OWNER = c.OWNER " +
            "WHERE t.OWNER = ? " +
            "ORDER BY TABLE_NAME";

    // 查询字段
    private static final String QUERY_COLUMNS = 
            "SELECT " +
            "    COLUMN_NAME, " +
            "    DATA_TYPE, " +
            "    DATA_LENGTH, " +
            "    DATA_PRECISION, " +
            "    DATA_SCALE, " +
            "    NULLABLE, " +
            "    DATA_DEFAULT, " +
            "    COLUMN_ID, " +
            "    COMMENTS AS COLUMN_COMMENT " +
            "FROM ALL_TAB_COLUMNS c " +
            "LEFT JOIN ALL_COL_COMMENTS m ON c.TABLE_NAME = m.TABLE_NAME " +
            "    AND c.COLUMN_NAME = m.COLUMN_NAME " +
            "    AND c.OWNER = m.OWNER " +
            "WHERE c.OWNER = ? AND c.TABLE_NAME = ? " +
            "ORDER BY COLUMN_ID";

    // 查询主键
    private static final String QUERY_PRIMARY_KEYS = 
            "SELECT cols.COLUMN_NAME " +
            "FROM ALL_CONSTRAINTS cons " +
            "JOIN ALL_CONS_COLUMNS cols ON cons.CONSTRAINT_NAME = cols.CONSTRAINT_NAME " +
            "    AND cons.OWNER = cols.OWNER " +
            "WHERE cons.CONSTRAINT_TYPE = 'P' " +
            "    AND cons.OWNER = ? " +
            "    AND cons.TABLE_NAME = ?";

    @Override
    public String getDatabaseType() {
        return "ORACLE";
    }

    @Override
    public List<String> getDatabases(Connection conn) throws SQLException {
        // Oracle使用Schema,返回当前用户的所有Schema
        List<String> schemas = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT USERNAME FROM ALL_USERS ORDER BY USERNAME")) {
            while (rs.next()) {
                schemas.add(rs.getString("USERNAME"));
            }
        }
        return schemas;
    }

    @Override
    public String getDatabaseVersion(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT VERSION FROM V$INSTANCE")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "Unknown";
    }

    @Override
    public Connection connect(DataSourceConfig config) throws SQLException {
        return ConnectionPoolManager.getConnection(config);
    }

    @Override
    public SchemaDictionary exportSchema(DataSourceConfig config, ExportOptions options) throws SQLException {
        long startTime = System.currentTimeMillis();
        log.info("========== 开始导出Oracle数据字典 ==========");
        log.info("数据库: {}, SCHEMA: {}", options.getDatabase(), config.getUsername());
        
        SchemaDictionary dictionary = new SchemaDictionary();
        
        ExportMetadata metadata = new ExportMetadata();
        metadata.setExportTime(new Date());
        metadata.setDatabaseType("Oracle");
        metadata.setDatabaseName(options.getDatabase());
        dictionary.setMetadata(metadata);
        
        try (Connection conn = connect(config)) {
            long connTime = System.currentTimeMillis();
            log.info("[1/4] 数据库连接成功, 耗时: {}ms", connTime - startTime);
            
            log.info("[2/4] 开始查询表列表...");
            long tableQueryStart = System.currentTimeMillis();
            List<TableDefinition> tables = getTables(conn, config.getUsername());
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
                
                table.setColumns(getColumns(conn, config.getUsername(), table.getTableName()));
                
                if (options.getIncludeIndexes()) {
                    table.setIndexes(getIndexes(conn, config.getUsername(), table.getTableName()));
                }
                
                if (options.getIncludeForeignKeys()) {
                    table.setForeignKeys(getForeignKeys(conn, config.getUsername(), table.getTableName()));
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
            log.info("========== Oracle数据字典导出完成 ==========");
            log.info("总计耗时: {}ms ({}秒), 导出表数: {}", 
                totalTime, totalTime / 1000.0, tables.size());
        }
        
        return dictionary;
    }

    @Override
    public List<TableDefinition> getTables(Connection conn, String schema) throws SQLException {
        List<TableDefinition> tables = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_TABLES)) {
            pstmt.setString(1, schema.toUpperCase());
            
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
    public List<ColumnDefinition> getColumns(Connection conn, String schema, String tableName) throws SQLException {
        List<ColumnDefinition> columns = new ArrayList<>();
        
        // 先获取主键字段
        List<String> primaryKeys = getPrimaryKeys(conn, schema, tableName);
        
        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_COLUMNS)) {
            pstmt.setString(1, schema.toUpperCase());
            pstmt.setString(2, tableName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ColumnDefinition column = new ColumnDefinition();
                    column.setColumnName(rs.getString("COLUMN_NAME"));
                    
                    String dataType = rs.getString("DATA_TYPE");
                    column.setDataType(dataType);
                    
                    // 长度(字符类型) - 使用Long支持超大值
                    long dataLength = rs.getLong("DATA_LENGTH");
                    if (!rs.wasNull() && isCharType(dataType)) {
                        column.setLength(dataLength);
                    }
                    
                    // 精度
                    long precision = rs.getLong("DATA_PRECISION");
                    if (!rs.wasNull()) {
                        column.setPrecision(precision);
                    }
                    
                    // 小数位
                    long scale = rs.getLong("DATA_SCALE");
                    if (!rs.wasNull()) {
                        column.setScale(scale);
                    }
                    
                    column.setNullable("Y".equals(rs.getString("NULLABLE")));
                    
                    // 默认值
                    String defaultValue = rs.getString("DATA_DEFAULT");
                    if (defaultValue != null) {
                        column.setDefaultValue(defaultValue);
                    }
                    
                    // 主键
                    column.setIsPrimaryKey(primaryKeys.contains(column.getColumnName()));
                    
                    column.setComment(rs.getString("COLUMN_COMMENT"));
                    column.setOrdinalPosition(rs.getInt("COLUMN_ID"));
                    
                    columns.add(column);
                }
            }
        }
        
        return columns;
    }

    @Override
    public List<IndexDefinition> getIndexes(Connection conn, String schema, String tableName) throws SQLException {
        List<IndexDefinition> indexes = new ArrayList<>();
        
        String queryIndexes = 
                "SELECT " +
                "    i.INDEX_NAME, " +
                "    i.INDEX_TYPE, " +
                "    i.UNIQUENESS, " +
                "    LISTAGG(ic.COLUMN_NAME, ', ') WITHIN GROUP (ORDER BY ic.COLUMN_POSITION) AS COLUMNS " +
                "FROM ALL_INDEXES i " +
                "LEFT JOIN ALL_IND_COLUMNS ic ON i.INDEX_NAME = ic.INDEX_NAME AND i.OWNER = ic.INDEX_OWNER " +
                "WHERE i.TABLE_OWNER = ? AND i.TABLE_NAME = ? " +
                "GROUP BY i.INDEX_NAME, i.INDEX_TYPE, i.UNIQUENESS " +
                "ORDER BY i.INDEX_NAME";
        
        try (PreparedStatement pstmt = conn.prepareStatement(queryIndexes)) {
            pstmt.setString(1, schema.toUpperCase());
            pstmt.setString(2, tableName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    IndexDefinition index = new IndexDefinition();
                    index.setIndexName(rs.getString("INDEX_NAME"));
                    index.setIndexType(rs.getString("INDEX_TYPE"));
                    index.setIsUnique("UNIQUE".equals(rs.getString("UNIQUENESS")));
                    
                    String columns = rs.getString("COLUMNS");
                    if (columns != null) {
                        index.setColumns(Arrays.asList(columns.split(", ")));
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
        
        String queryFK = 
                "SELECT " +
                "    cons.CONSTRAINT_NAME, " +
                "    cols.COLUMN_NAME, " +
                "    ref_cons.TABLE_NAME AS REF_TABLE, " +
                "    ref_cols.COLUMN_NAME AS REF_COLUMN " +
                "FROM ALL_CONSTRAINTS cons " +
                "JOIN ALL_CONS_COLUMNS cols ON cons.CONSTRAINT_NAME = cols.CONSTRAINT_NAME " +
                "    AND cons.OWNER = cols.OWNER " +
                "JOIN ALL_CONSTRAINTS ref_cons ON cons.R_CONSTRAINT_NAME = ref_cons.CONSTRAINT_NAME " +
                "    AND cons.OWNER = ref_cons.OWNER " +
                "JOIN ALL_CONS_COLUMNS ref_cols ON ref_cons.CONSTRAINT_NAME = ref_cols.CONSTRAINT_NAME " +
                "    AND ref_cons.OWNER = ref_cols.OWNER " +
                "WHERE cons.CONSTRAINT_TYPE = 'R' " +
                "    AND cons.OWNER = ? " +
                "    AND cons.TABLE_NAME = ? " +
                "ORDER BY cons.CONSTRAINT_NAME, cols.POSITION";
        
        try (PreparedStatement pstmt = conn.prepareStatement(queryFK)) {
            pstmt.setString(1, schema.toUpperCase());
            pstmt.setString(2, tableName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ForeignKeyDefinition fk = new ForeignKeyDefinition();
                    fk.setConstraintName(rs.getString("CONSTRAINT_NAME"));
                    fk.setColumnName(rs.getString("COLUMN_NAME"));
                    fk.setReferencedTable(rs.getString("REF_TABLE"));
                    fk.setReferencedColumn(rs.getString("REF_COLUMN"));
                    foreignKeys.add(fk);
                }
            }
        }
        
        return foreignKeys;
    }

    @Override
    public boolean testConnection(DataSourceConfig config) {
        try (Connection conn = connect(config)) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            log.error("Oracle连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取主键字段列表
     */
    private List<String> getPrimaryKeys(Connection conn, String schema, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(QUERY_PRIMARY_KEYS)) {
            pstmt.setString(1, schema.toUpperCase());
            pstmt.setString(2, tableName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        
        return primaryKeys;
    }

    /**
     * 判断是否为字符类型
     */
    private boolean isCharType(String dataType) {
        return dataType != null && (
                dataType.contains("CHAR") || 
                dataType.contains("VARCHAR") || 
                dataType.contains("CLOB") ||
                dataType.contains("NCHAR") ||
                dataType.contains("NVARCHAR"));
    }
}
