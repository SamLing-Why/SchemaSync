package com.schemasync.generator;

import com.schemasync.model.diff.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQLDDLGenerator单元测试
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
class MySQLDDLGeneratorTest {

    private MySQLDDLGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MySQLDDLGenerator();
    }

    @Test
    void testGenerate_EmptyDiff() {
        // 准备: 空的差异对象
        SchemaDiff diff = createEmptyDiff();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertNotNull(ddl);
        assertTrue(ddl.contains("SchemaSync 自动生成的数据库变更脚本"));
        assertTrue(ddl.contains("START TRANSACTION"));
        assertTrue(ddl.contains("COMMIT"));
        assertTrue(ddl.contains("无新增表"));
        assertTrue(ddl.contains("无表结构变更"));
    }

    @Test
    void testGenerate_CreateTable() {
        // 准备
        SchemaDiff diff = createDiffWithTableAdd();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertNotNull(ddl);
        assertTrue(ddl.contains("新增表: user_profile"));
        assertTrue(ddl.contains("CREATE TABLE"));
    }

    @Test
    void testGenerate_DropTable() {
        // 准备
        SchemaDiff diff = createDiffWithTableDrop();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertNotNull(ddl);
        assertTrue(ddl.contains("删除表: temp_table"));
        // 破坏性变更应该被注释
        assertTrue(ddl.contains("-- [警告: 破坏性变更]"));
        assertTrue(ddl.contains("-- DROP TABLE"));
    }

    @Test
    void testGenerate_DropTableWithoutComment() {
        // 准备
        SchemaDiff diff = createDiffWithTableDrop();
        GenerationOptions options = GenerationOptions.builder()
                .databaseType("MYSQL")
                .commentBreakingChanges(false)
                .useTransaction(true)
                .build();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证: 不应该注释
        assertFalse(ddl.contains("-- [警告: 破坏性变更]"));
        assertTrue(ddl.contains("DROP TABLE IF EXISTS"));
    }

    @Test
    void testGenerate_AddColumn() {
        // 准备
        SchemaDiff diff = createDiffWithColumnAdd();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertNotNull(ddl);
        assertTrue(ddl.contains("ALTER TABLE `user` ADD COLUMN `avatar`"));
    }

    @Test
    void testGenerate_DropColumn() {
        // 准备
        SchemaDiff diff = createDiffWithColumnDrop();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertNotNull(ddl);
        assertTrue(ddl.contains("ALTER TABLE `user` DROP COLUMN `old_field`"));
        // 破坏性变更应该被注释
        assertTrue(ddl.contains("-- ALTER TABLE"));
    }

    @Test
    void testGenerate_ModifyColumn() {
        // 准备
        SchemaDiff diff = createDiffWithColumnModify();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertNotNull(ddl);
        assertTrue(ddl.contains("ALTER TABLE `user` MODIFY COLUMN `username`"));
    }

    @Test
    void testGenerate_WithRollback() {
        // 准备
        SchemaDiff diff = createDiffWithTableAdd();
        GenerationOptions options = GenerationOptions.builder()
                .databaseType("MYSQL")
                .includeRollback(true)
                .build();

        // 执行
        String ddl = generator.generate(diff, options);
        String rollback = generator.generateRollback(diff);
        String fullScript = ddl + "\n\n" + rollback;

        // 验证
        assertNotNull(fullScript);
        assertTrue(fullScript.contains("回滚脚本"));
        assertTrue(fullScript.contains("DROP TABLE IF EXISTS `user_profile`"));
    }

    @Test
    void testGenerate_WithoutTransaction() {
        // 准备
        SchemaDiff diff = createEmptyDiff();
        GenerationOptions options = GenerationOptions.builder()
                .databaseType("MYSQL")
                .useTransaction(false)
                .build();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertFalse(ddl.contains("START TRANSACTION"));
        assertFalse(ddl.contains("COMMIT"));
    }

    @Test
    void testGenerate_VersionInfo() {
        // 准备
        SchemaDiff diff = createEmptyDiff();
        GenerationOptions options = GenerationOptions.builder()
                .databaseType("MYSQL")
                .sourceVersion("v1.0.0")
                .targetVersion("v2.0.0")
                .build();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertTrue(ddl.contains("源版本: v1.0.0"));
        assertTrue(ddl.contains("目标版本: v2.0.0"));
    }

    @Test
    void testGenerate_ComplexChanges() {
        // 准备: 复杂变更场景
        SchemaDiff diff = createComplexDiff();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertNotNull(ddl);
        // 包含所有变更类型
        assertTrue(ddl.contains("新增表"));
        assertTrue(ddl.contains("修改表结构"));
        assertTrue(ddl.contains("删除表"));
        // 包含统计信息
        assertTrue(ddl.contains("变更统计"));
        // 破坏性变更数量
        assertTrue(ddl.contains("破坏性变更"));
    }

    @Test
    void testGenerate_MultipleTablesAlter() {
        // 准备: 多个表的结构变更
        SchemaDiff diff = createMultiTableChanges();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertNotNull(ddl);
        // 应该包含两个表的变更
        assertTrue(ddl.contains("表: user"));
        assertTrue(ddl.contains("表: product"));
    }

    @Test
    void testGenerate_HeaderComment() {
        // 准备
        SchemaDiff diff = createDiffWithBreakingChanges();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证头部注释
        assertTrue(ddl.startsWith("-- ============================================"));
        assertTrue(ddl.contains("SchemaSync 自动生成的数据库变更脚本"));
        assertTrue(ddl.contains("数据库类型: MySQL"));
        // 包含执行建议
        assertTrue(ddl.contains("建议在非生产环境先执行测试"));
        assertTrue(ddl.contains("执行前请备份数据库"));
    }

    @Test
    void testGenerate_FooterComment() {
        // 准备
        SchemaDiff diff = createEmptyDiff();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证尾部注释
        assertTrue(ddl.contains("执行完成后请验证"));
        assertTrue(ddl.contains("检查表结构是否正确"));
        assertTrue(ddl.contains("检查数据是否完整"));
        assertTrue(ddl.contains("检查应用功能是否正常"));
    }

    @Test
    void testGenerate_IndexChanges() {
        // 准备
        SchemaDiff diff = createDiffWithIndexChanges();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertNotNull(ddl);
        assertTrue(ddl.contains("索引变更"));
        assertTrue(ddl.contains("CREATE INDEX"));
        assertTrue(ddl.contains("DROP INDEX"));
    }

    @Test
    void testGenerate_ForeignKeyChanges() {
        // 准备
        SchemaDiff diff = createDiffWithForeignKeyChanges();
        GenerationOptions options = getDefaultOptions();

        // 执行
        String ddl = generator.generate(diff, options);

        // 验证
        assertNotNull(ddl);
        assertTrue(ddl.contains("外键约束"));
        assertTrue(ddl.contains("ADD CONSTRAINT"));
        assertTrue(ddl.contains("DROP FOREIGN KEY"));
    }

    @Test
    void testGetDatabaseType() {
        assertEquals("MYSQL", generator.getDatabaseType());
    }

    /**
     * 创建默认选项
     */
    private GenerationOptions getDefaultOptions() {
        return GenerationOptions.builder()
                .databaseType("MYSQL")
                .includeRollback(false)
                .commentBreakingChanges(true)
                .useTransaction(true)
                .build();
    }

    /**
     * 创建空差异
     */
    private SchemaDiff createEmptyDiff() {
        SchemaDiff diff = new SchemaDiff();
        diff.setChanges(Collections.emptyList());
        diff.setSummary(new DiffSummary());
        return diff;
    }

    /**
     * 创建新增表的差异
     */
    private SchemaDiff createDiffWithTableAdd() {
        SchemaDiff diff = new SchemaDiff();
        
        SchemaChange change = SchemaChange.builder()
                .changeType(ChangeType.TABLE_ADD)
                .tableName("user_profile")
                .severity(Severity.NON_BREAKING)
                .build();
        
        diff.setChanges(Arrays.asList(change));
        
        DiffSummary summary = new DiffSummary();
        summary.setTablesAdded(1);
        diff.setSummary(summary);
        
        return diff;
    }

    /**
     * 创建删除表的差异
     */
    private SchemaDiff createDiffWithTableDrop() {
        SchemaDiff diff = new SchemaDiff();
        
        SchemaChange change = SchemaChange.builder()
                .changeType(ChangeType.TABLE_DROP)
                .tableName("temp_table")
                .severity(Severity.BREAKING)
                .build();
        
        diff.setChanges(Arrays.asList(change));
        
        DiffSummary summary = new DiffSummary();
        summary.setTablesDropped(1);
        summary.setBreakingChanges(1);
        diff.setSummary(summary);
        
        return diff;
    }

    /**
     * 创建新增字段的差异
     */
    private SchemaDiff createDiffWithColumnAdd() {
        SchemaDiff diff = new SchemaDiff();
        
        SchemaChange change = SchemaChange.builder()
                .changeType(ChangeType.COLUMN_ADD)
                .tableName("user")
                .columnName("avatar")
                .severity(Severity.NON_BREAKING)
                .build();
        
        diff.setChanges(Arrays.asList(change));
        
        DiffSummary summary = new DiffSummary();
        summary.setColumnsAdded(1);
        diff.setSummary(summary);
        
        return diff;
    }

    /**
     * 创建删除字段的差异
     */
    private SchemaDiff createDiffWithColumnDrop() {
        SchemaDiff diff = new SchemaDiff();
        
        SchemaChange change = SchemaChange.builder()
                .changeType(ChangeType.COLUMN_DROP)
                .tableName("user")
                .columnName("old_field")
                .severity(Severity.BREAKING)
                .build();
        
        diff.setChanges(Arrays.asList(change));
        
        DiffSummary summary = new DiffSummary();
        summary.setColumnsDropped(1);
        summary.setBreakingChanges(1);
        diff.setSummary(summary);
        
        return diff;
    }

    /**
     * 创建修改字段的差异
     */
    private SchemaDiff createDiffWithColumnModify() {
        SchemaDiff diff = new SchemaDiff();
        
        Map<String, Object> details = new HashMap<>();
        details.put("property", "length");
        details.put("oldValue", 50);
        details.put("newValue", 100);
        
        SchemaChange change = SchemaChange.builder()
                .changeType(ChangeType.COLUMN_MODIFY)
                .tableName("user")
                .columnName("username")
                .severity(Severity.NON_BREAKING)
                .details(details)
                .build();
        
        diff.setChanges(Arrays.asList(change));
        
        DiffSummary summary = new DiffSummary();
        summary.setColumnsModified(1);
        diff.setSummary(summary);
        
        return diff;
    }

    /**
     * 创建包含破坏性变更的差异
     */
    private SchemaDiff createDiffWithBreakingChanges() {
        SchemaDiff diff = new SchemaDiff();
        
        SchemaChange change1 = SchemaChange.builder()
                .changeType(ChangeType.TABLE_DROP)
                .tableName("old_table")
                .severity(Severity.BREAKING)
                .build();
        
        SchemaChange change2 = SchemaChange.builder()
                .changeType(ChangeType.COLUMN_DROP)
                .tableName("user")
                .columnName("deprecated_field")
                .severity(Severity.BREAKING)
                .build();
        
        diff.setChanges(Arrays.asList(change1, change2));
        
        DiffSummary summary = new DiffSummary();
        summary.setTablesDropped(1);
        summary.setColumnsDropped(1);
        summary.setBreakingChanges(2);
        diff.setSummary(summary);
        
        return diff;
    }

    /**
     * 创建复杂变更场景
     */
    private SchemaDiff createComplexDiff() {
        SchemaDiff diff = new SchemaDiff();
        
        diff.setChanges(Arrays.asList(
            SchemaChange.builder().changeType(ChangeType.TABLE_ADD).tableName("order").severity(Severity.NON_BREAKING).build(),
            SchemaChange.builder().changeType(ChangeType.TABLE_DROP).tableName("temp").severity(Severity.BREAKING).build(),
            SchemaChange.builder().changeType(ChangeType.COLUMN_ADD).tableName("user").columnName("phone").severity(Severity.NON_BREAKING).build(),
            SchemaChange.builder().changeType(ChangeType.COLUMN_DROP).tableName("user").columnName("old_email").severity(Severity.BREAKING).build(),
            SchemaChange.builder().changeType(ChangeType.COLUMN_MODIFY).tableName("user").columnName("username").severity(Severity.NON_BREAKING).build()
        ));
        
        DiffSummary summary = new DiffSummary();
        summary.setTablesAdded(1);
        summary.setTablesDropped(1);
        summary.setColumnsAdded(1);
        summary.setColumnsDropped(1);
        summary.setColumnsModified(1);
        summary.setBreakingChanges(2);
        diff.setSummary(summary);
        
        return diff;
    }

    /**
     * 创建多表变更
     */
    private SchemaDiff createMultiTableChanges() {
        SchemaDiff diff = new SchemaDiff();
        
        diff.setChanges(Arrays.asList(
            SchemaChange.builder().changeType(ChangeType.COLUMN_ADD).tableName("user").columnName("avatar").severity(Severity.NON_BREAKING).build(),
            SchemaChange.builder().changeType(ChangeType.COLUMN_ADD).tableName("product").columnName("category").severity(Severity.NON_BREAKING).build()
        ));
        
        DiffSummary summary = new DiffSummary();
        summary.setColumnsAdded(2);
        summary.setTablesModified(2);
        diff.setSummary(summary);
        
        return diff;
    }

    /**
     * 创建索引变更
     */
    private SchemaDiff createDiffWithIndexChanges() {
        SchemaDiff diff = new SchemaDiff();
        
        diff.setChanges(Arrays.asList(
            SchemaChange.builder().changeType(ChangeType.INDEX_ADD).tableName("user").severity(Severity.NON_BREAKING).build(),
            SchemaChange.builder().changeType(ChangeType.INDEX_DROP).tableName("user").severity(Severity.NON_BREAKING).build()
        ));
        
        DiffSummary summary = new DiffSummary();
        summary.setIndexesAdded(1);
        summary.setIndexesDropped(1);
        diff.setSummary(summary);
        
        return diff;
    }

    /**
     * 创建外键变更
     */
    private SchemaDiff createDiffWithForeignKeyChanges() {
        SchemaDiff diff = new SchemaDiff();
        
        diff.setChanges(Arrays.asList(
            SchemaChange.builder().changeType(ChangeType.FOREIGN_KEY_ADD).tableName("order").severity(Severity.NON_BREAKING).build(),
            SchemaChange.builder().changeType(ChangeType.FOREIGN_KEY_DROP).tableName("order").severity(Severity.NON_BREAKING).build()
        ));
        
        DiffSummary summary = new DiffSummary();
        summary.setForeignKeysAdded(1);
        summary.setForeignKeysDropped(1);
        diff.setSummary(summary);
        
        return diff;
    }
}
