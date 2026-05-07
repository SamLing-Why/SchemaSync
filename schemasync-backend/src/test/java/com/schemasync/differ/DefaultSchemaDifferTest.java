package com.schemasync.differ;

import com.schemasync.model.diff.*;
import com.schemasync.model.dict.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultSchemaDiffer单元测试
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
class DefaultSchemaDifferTest {

    private DefaultSchemaDiffer differ;

    @BeforeEach
    void setUp() {
        differ = new DefaultSchemaDiffer();
    }

    @Test
    void testCompare_NoChanges() {
        // 准备: 两个相同的数据字典
        SchemaDictionary dict1 = createTestDictionary();
        SchemaDictionary dict2 = createTestDictionary();

        // 执行
        SchemaDiff diff = differ.compare(dict1, dict2);

        // 验证
        assertNotNull(diff);
        assertNotNull(diff.getChanges());
        assertEquals(0, diff.getChanges().size());
        assertNotNull(diff.getSummary());
        assertEquals(0, diff.getSummary().getTablesAdded());
        assertEquals(0, diff.getSummary().getTablesDropped());
        assertEquals(0, diff.getSummary().getColumnsAdded());
        assertEquals(0, diff.getSummary().getBreakingChanges());
    }

    @Test
    void testCompare_AddTable() {
        // 准备
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionary();
        
        // 新增一个表
        TableDefinition newTable = new TableDefinition();
        newTable.setTableName("user_profile");
        newTable.setTableComment("用户资料表");
        newTable.setColumns(Collections.emptyList());
        newDict.getTables().add(newTable);

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertEquals(1, diff.getChanges().size());
        assertEquals(1, diff.getSummary().getTablesAdded());
        
        SchemaChange change = diff.getChanges().get(0);
        assertEquals(ChangeType.TABLE_ADD, change.getChangeType());
        assertEquals("user_profile", change.getTableName());
        assertEquals(Severity.NON_BREAKING, change.getSeverity());
    }

    @Test
    void testCompare_DropTable() {
        // 准备
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionary();
        
        // 删除user表
        newDict.getTables().removeIf(t -> "user".equals(t.getTableName()));

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertEquals(1, diff.getChanges().size());
        assertEquals(1, diff.getSummary().getTablesDropped());
        
        SchemaChange change = diff.getChanges().get(0);
        assertEquals(ChangeType.TABLE_DROP, change.getChangeType());
        assertEquals("user", change.getTableName());
        assertEquals(Severity.BREAKING, change.getSeverity());
    }

    @Test
    void testCompare_AddColumn() {
        // 准备
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionary();
        
        // 给user表添加字段
        TableDefinition userTable = newDict.getTables().stream()
                .filter(t -> "user".equals(t.getTableName()))
                .findFirst()
                .orElseThrow();
        
        ColumnDefinition newColumn = new ColumnDefinition();
        newColumn.setColumnName("avatar");
        newColumn.setDataType("VARCHAR");
        newColumn.setLength(255L);
        newColumn.setNullable(true);
        newColumn.setComment("头像URL");
        userTable.getColumns().add(newColumn);

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertEquals(1, diff.getChanges().size());
        assertEquals(1, diff.getSummary().getColumnsAdded());
        
        SchemaChange change = diff.getChanges().get(0);
        assertEquals(ChangeType.COLUMN_ADD, change.getChangeType());
        assertEquals("user", change.getTableName());
        assertEquals("avatar", change.getColumnName());
        assertEquals(Severity.NON_BREAKING, change.getSeverity());
    }

    @Test
    void testCompare_DropColumn() {
        // 准备
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionary();
        
        // 删除user表的email字段
        TableDefinition newUserTable = newDict.getTables().stream()
                .filter(t -> "user".equals(t.getTableName()))
                .findFirst()
                .orElseThrow();
        newUserTable.getColumns().removeIf(c -> "email".equals(c.getColumnName()));

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertEquals(1, diff.getChanges().size());
        assertEquals(1, diff.getSummary().getColumnsDropped());
        
        SchemaChange change = diff.getChanges().get(0);
        assertEquals(ChangeType.COLUMN_DROP, change.getChangeType());
        assertEquals("user", change.getTableName());
        assertEquals("email", change.getColumnName());
        assertEquals(Severity.BREAKING, change.getSeverity());
    }

    @Test
    void testCompare_ModifyColumnDataType() {
        // 准备
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionary();
        
        // 修改user表的age字段类型
        TableDefinition newUserTable = newDict.getTables().stream()
                .filter(t -> "user".equals(t.getTableName()))
                .findFirst()
                .orElseThrow();
        ColumnDefinition ageColumn = newUserTable.getColumns().stream()
                .filter(c -> "age".equals(c.getColumnName()))
                .findFirst()
                .orElseThrow();
        ageColumn.setDataType("BIGINT"); // INT -> BIGINT

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertEquals(1, diff.getChanges().size());
        assertEquals(1, diff.getSummary().getColumnsModified());
        
        SchemaChange change = diff.getChanges().get(0);
        assertEquals(ChangeType.COLUMN_MODIFY, change.getChangeType());
        assertEquals("user", change.getTableName());
        assertEquals("age", change.getColumnName());
        assertEquals(Severity.BREAKING, change.getSeverity());
        assertTrue(change.getDetails().toString().contains("dataType"));
    }

    @Test
    void testCompare_ModifyColumnLengthIncrease() {
        // 准备
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionary();
        
        // 扩展username字段长度
        TableDefinition newUserTable = newDict.getTables().stream()
                .filter(t -> "user".equals(t.getTableName()))
                .findFirst()
                .orElseThrow();
        ColumnDefinition usernameColumn = newUserTable.getColumns().stream()
                .filter(c -> "username".equals(c.getColumnName()))
                .findFirst()
                .orElseThrow();
        usernameColumn.setLength(100L); // 50 -> 100

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertEquals(1, diff.getChanges().size());
        
        SchemaChange change = diff.getChanges().get(0);
        assertEquals(ChangeType.COLUMN_MODIFY, change.getChangeType());
        assertEquals(Severity.NON_BREAKING, change.getSeverity());
        assertTrue(change.getDetails().toString().contains("length"));
    }

    @Test
    void testCompare_ModifyColumnLengthDecrease() {
        // 准备
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionary();
        
        // 缩小username字段长度
        TableDefinition newUserTable = newDict.getTables().stream()
                .filter(t -> "user".equals(t.getTableName()))
                .findFirst()
                .orElseThrow();
        ColumnDefinition usernameColumn = newUserTable.getColumns().stream()
                .filter(c -> "username".equals(c.getColumnName()))
                .findFirst()
                .orElseThrow();
        usernameColumn.setLength(30L); // 50 -> 30 (缩小)

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertEquals(1, diff.getChanges().size());
        
        SchemaChange change = diff.getChanges().get(0);
        assertEquals(ChangeType.COLUMN_MODIFY, change.getChangeType());
        assertEquals(Severity.BREAKING, change.getSeverity()); // 缩小应该是破坏性变更
    }

    @Test
    void testCompare_ModifyColumnNullable() {
        // 准备
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionary();
        
        // 修改email为NOT NULL
        TableDefinition newUserTable = newDict.getTables().stream()
                .filter(t -> "user".equals(t.getTableName()))
                .findFirst()
                .orElseThrow();
        ColumnDefinition emailColumn = newUserTable.getColumns().stream()
                .filter(c -> "email".equals(c.getColumnName()))
                .findFirst()
                .orElseThrow();
        emailColumn.setNullable(false); // true -> false

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertEquals(1, diff.getChanges().size());
        
        SchemaChange change = diff.getChanges().get(0);
        assertEquals(ChangeType.COLUMN_MODIFY, change.getChangeType());
        assertEquals(Severity.BREAKING, change.getSeverity());
    }

    @Test
    void testCompare_MultipleChanges() {
        // 准备 - 使用深拷贝避免引用问题
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionaryCopy();
        
        // 多个变更:
        // 1. 新增表
        TableDefinition newTable = new TableDefinition();
        newTable.setTableName("order");
        newTable.setTableComment("订单表");
        newTable.setColumns(new ArrayList<>());
        newTable.setIndexes(new ArrayList<>());
        newTable.setForeignKeys(new ArrayList<>());
        newDict.getTables().add(newTable);
        
        // 2. 删除表
        newDict.getTables().removeIf(t -> "product".equals(t.getTableName()));
        
        // 3. 添加字段
        TableDefinition userTable = newDict.getTables().stream()
                .filter(t -> "user".equals(t.getTableName()))
                .findFirst()
                .orElseThrow();
        ColumnDefinition phoneColumn = new ColumnDefinition();
        phoneColumn.setColumnName("phone");
        phoneColumn.setDataType("VARCHAR");
        phoneColumn.setLength(20L);
        userTable.getColumns().add(phoneColumn);
        
        // 4. 删除字段
        userTable.getColumns().removeIf(c -> "email".equals(c.getColumnName()));
        
        // 5. 修改字段
        ColumnDefinition usernameColumn = userTable.getColumns().stream()
                .filter(c -> "username".equals(c.getColumnName()))
                .findFirst()
                .orElseThrow();
        usernameColumn.setLength(100L);

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证统计
        assertEquals(5, diff.getChanges().size());
        assertEquals(1, diff.getSummary().getTablesAdded());
        assertEquals(1, diff.getSummary().getTablesDropped());
        assertEquals(1, diff.getSummary().getColumnsAdded());
        assertEquals(1, diff.getSummary().getColumnsDropped());
        assertEquals(1, diff.getSummary().getColumnsModified());
        
        // 验证破坏性变更数量
        long breakingCount = diff.getChanges().stream()
                .filter(c -> c.getSeverity() == Severity.BREAKING)
                .count();
        assertEquals(2, breakingCount); // 删除表 + 删除字段
        assertEquals(2, diff.getSummary().getBreakingChanges());
    }

    @Test
    void testCompare_NullTables() {
        // 准备: 空表列表
        SchemaDictionary oldDict = new SchemaDictionary();
        oldDict.setTables(null);
        
        SchemaDictionary newDict = new SchemaDictionary();
        newDict.setTables(null);

        // 执行: 不应该抛异常
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertNotNull(diff);
        assertEquals(0, diff.getChanges().size());
    }

    @Test
    void testCompare_AddIndex() {
        // 准备
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionary();
        
        // 添加索引
        TableDefinition userTable = newDict.getTables().stream()
                .filter(t -> "user".equals(t.getTableName()))
                .findFirst()
                .orElseThrow();
        IndexDefinition newIndex = new IndexDefinition();
        newIndex.setIndexName("idx_username");
        newIndex.setIndexType("NORMAL");
        userTable.setIndexes(Arrays.asList(newIndex));

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertEquals(1, diff.getChanges().size());
        assertEquals(1, diff.getSummary().getIndexesAdded());
        
        SchemaChange change = diff.getChanges().get(0);
        assertEquals(ChangeType.INDEX_ADD, change.getChangeType());
        assertEquals(Severity.NON_BREAKING, change.getSeverity());
    }

    @Test
    void testCompare_AddForeignKey() {
        // 准备
        SchemaDictionary oldDict = createTestDictionary();
        SchemaDictionary newDict = createTestDictionary();
        
        // 添加外键
        TableDefinition userTable = newDict.getTables().stream()
                .filter(t -> "user".equals(t.getTableName()))
                .findFirst()
                .orElseThrow();
        ForeignKeyDefinition newFk = new ForeignKeyDefinition();
        newFk.setConstraintName("fk_user_dept");
        userTable.setForeignKeys(Arrays.asList(newFk));

        // 执行
        SchemaDiff diff = differ.compare(oldDict, newDict);

        // 验证
        assertEquals(1, diff.getChanges().size());
        assertEquals(1, diff.getSummary().getForeignKeysAdded());
        
        SchemaChange change = diff.getChanges().get(0);
        assertEquals(ChangeType.FOREIGN_KEY_ADD, change.getChangeType());
    }

    /**
     * 创建测试用的数据字典(深拷贝版本)
     */
    private SchemaDictionary createTestDictionaryCopy() {
        SchemaDictionary dictionary = new SchemaDictionary();

        // user表
        TableDefinition userTable = new TableDefinition();
        userTable.setTableName("user");
        userTable.setTableComment("用户表");
        
        ColumnDefinition idColumn = new ColumnDefinition();
        idColumn.setColumnName("id");
        idColumn.setDataType("BIGINT");
        idColumn.setNullable(false);
        idColumn.setIsPrimaryKey(true);
        idColumn.setIsAutoIncrement(true);

        ColumnDefinition usernameColumn = new ColumnDefinition();
        usernameColumn.setColumnName("username");
        usernameColumn.setDataType("VARCHAR");
        usernameColumn.setLength(50L);
        usernameColumn.setNullable(false);

        ColumnDefinition emailColumn = new ColumnDefinition();
        emailColumn.setColumnName("email");
        emailColumn.setDataType("VARCHAR");
        emailColumn.setLength(100L);
        emailColumn.setNullable(true);

        ColumnDefinition ageColumn = new ColumnDefinition();
        ageColumn.setColumnName("age");
        ageColumn.setDataType("INT");
        ageColumn.setNullable(true);

        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(idColumn);
        columns.add(usernameColumn);
        columns.add(emailColumn);
        columns.add(ageColumn);
        userTable.setColumns(columns);

        // product表
        TableDefinition productTable = new TableDefinition();
        productTable.setTableName("product");
        productTable.setTableComment("产品表");
        productTable.setColumns(new ArrayList<>());

        List<TableDefinition> tables = new ArrayList<>();
        tables.add(userTable);
        tables.add(productTable);
        dictionary.setTables(tables);
        
        return dictionary;
    }

    /**
     * 创建测试用的数据字典
     */
    private SchemaDictionary createTestDictionary() {
        return createTestDictionaryCopy();
    }
}
