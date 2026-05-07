package com.schemasync.service;

import com.schemasync.model.dict.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchemaFlattener单元测试
 */
class SchemaFlattenerTest {
    
    private SchemaFlattener flattener;
    
    @BeforeEach
    void setUp() {
        flattener = new SchemaFlattener();
    }
    
    @Test
    void testFlatten() {
        // 准备测试数据
        SchemaDictionary dictionary = createTestDictionary();
        
        // 执行扁平化
        FlatSchemaDictionary flat = flattener.flatten(dictionary);
        
        // 验证概述信息
        assertNotNull(flat.getOverview());
        assertEquals(6, flat.getOverview().size());
        
        // 验证表信息
        assertNotNull(flat.getTables());
        assertEquals(2, flat.getTables().size());
        assertEquals("user", flat.getTables().get(0).getTableName());
        
        // 验证字段信息(展开的)
        assertNotNull(flat.getColumns());
        assertTrue(flat.getColumns().size() >= 5); // user表3个字段 + order表2个字段
        
        // 验证字段包含表名
        List<ColumnInfoRow> userColumns = flat.getColumns().stream()
                .filter(col -> "user".equals(col.getTableName()))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(3, userColumns.size());
        
        // 验证索引信息
        assertNotNull(flat.getIndexes());
        assertEquals(2, flat.getIndexes().size());
        
        // 验证约束信息
        assertNotNull(flat.getConstraints());
        assertEquals(1, flat.getConstraints().size());
    }
    
    @Test
    void testFlattenWithEmptyDictionary() {
        SchemaDictionary dictionary = new SchemaDictionary();
        dictionary.setMetadata(new ExportMetadata());
        dictionary.setTables(new ArrayList<>());
        
        FlatSchemaDictionary flat = flattener.flatten(dictionary);
        
        assertNotNull(flat.getOverview());
        assertNotNull(flat.getTables());
        assertTrue(flat.getTables().isEmpty());
        assertNotNull(flat.getColumns());
        assertTrue(flat.getColumns().isEmpty());
    }
    
    /**
     * 创建测试用的数据字典
     */
    private SchemaDictionary createTestDictionary() {
        SchemaDictionary dictionary = new SchemaDictionary();
        
        // 元数据
        ExportMetadata metadata = new ExportMetadata();
        metadata.setDatabaseType("MySQL");
        metadata.setDatabaseVersion("8.0.33");
        metadata.setDatabaseName("test_db");
        metadata.setExportTime(new Date());
        metadata.setToolVersion("1.0.0");
        dictionary.setMetadata(metadata);
        
        // 表列表
        List<TableDefinition> tables = new ArrayList<>();
        
        // user表
        TableDefinition userTable = new TableDefinition();
        userTable.setTableName("user");
        userTable.setTableComment("用户表");
        userTable.setTableType("BASE TABLE");
        userTable.setEngine("InnoDB");
        userTable.setCharset("utf8mb4");
        
        List<ColumnDefinition> userColumns = new ArrayList<>();
        userColumns.add(createColumn("id", "BIGINT", null, 20L, null, false, null, true, true, "主键"));
        userColumns.add(createColumn("username", "VARCHAR", 100L, null, null, false, null, false, false, "用户名"));
        userColumns.add(createColumn("email", "VARCHAR", 200L, null, null, true, null, false, false, "邮箱"));
        userTable.setColumns(userColumns);
        
        List<IndexDefinition> userIndexes = new ArrayList<>();
        userIndexes.add(createIndex("PRIMARY", "PRIMARY", Arrays.asList("id"), true));
        userIndexes.add(createIndex("idx_username", "INDEX", Arrays.asList("username"), false));
        userTable.setIndexes(userIndexes);
        
        tables.add(userTable);
        
        // order表
        TableDefinition orderTable = new TableDefinition();
        orderTable.setTableName("order");
        orderTable.setTableComment("订单表");
        orderTable.setTableType("BASE TABLE");
        orderTable.setEngine("InnoDB");
        
        List<ColumnDefinition> orderColumns = new ArrayList<>();
        orderColumns.add(createColumn("id", "BIGINT", null, 20L, null, false, null, true, true, "主键"));
        orderColumns.add(createColumn("user_id", "BIGINT", null, 20L, null, false, null, false, false, "用户ID"));
        orderTable.setColumns(orderColumns);
        
        List<ForeignKeyDefinition> orderFks = new ArrayList<>();
        ForeignKeyDefinition fk = new ForeignKeyDefinition();
        fk.setConstraintName("fk_order_user");
        fk.setColumnName("user_id");
        fk.setReferencedTable("user");
        fk.setReferencedColumn("id");
        fk.setOnUpdate("CASCADE");
        fk.setOnDelete("CASCADE");
        orderFks.add(fk);
        orderTable.setForeignKeys(orderFks);
        
        tables.add(orderTable);
        
        dictionary.setTables(tables);
        
        return dictionary;
    }
    
    private ColumnDefinition createColumn(String name, String type, Long length, 
                                          Long precision, Long scale, Boolean nullable,
                                          Object defaultValue, Boolean isPrimaryKey, 
                                          Boolean isAutoIncrement, String comment) {
        ColumnDefinition column = new ColumnDefinition();
        column.setColumnName(name);
        column.setDataType(type);
        column.setLength(length);
        column.setPrecision(precision);
        column.setScale(scale);
        column.setNullable(nullable);
        column.setDefaultValue(defaultValue);
        column.setIsPrimaryKey(isPrimaryKey);
        column.setIsAutoIncrement(isAutoIncrement);
        column.setComment(comment);
        return column;
    }
    
    private IndexDefinition createIndex(String name, String type, List<String> columns, boolean isUnique) {
        IndexDefinition index = new IndexDefinition();
        index.setIndexName(name);
        index.setIndexType(type);
        index.setColumns(columns);
        index.setIsUnique(isUnique);
        return index;
    }
}
