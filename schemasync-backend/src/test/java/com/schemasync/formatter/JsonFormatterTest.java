package com.schemasync.formatter;

import com.schemasync.model.dict.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonFormatter单元测试
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
class JsonFormatterTest {

    private JsonFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new JsonFormatter();
    }

    @Test
    void testFormat_SchemaDictionary() {
        // 准备
        SchemaDictionary dictionary = createTestDictionary();

        // 执行
        byte[] result = formatter.format(dictionary);

        // 验证
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        String json = new String(result);
        assertTrue(json.contains("metadata"));
        assertTrue(json.contains("tables"));
        assertTrue(json.contains("user"));
    }

    @Test
    void testFormatToString() {
        // 准备
        SchemaDictionary dictionary = createTestDictionary();

        // 执行
        String result = formatter.formatToString(dictionary);

        // 验证
        assertNotNull(result);
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("\"metadata\""));
    }

    @Test
    void testParse_ValidJson() {
        // 准备
        String json = "{" +
                "\"metadata\":{" +
                "\"exportTime\":\"2026-04-26 20:00:00\"," +
                "\"databaseType\":\"MySQL\"," +
                "\"databaseName\":\"test_db\"" +
                "}," +
                "\"tables\":[" +
                "{" +
                "\"tableName\":\"user\"," +
                "\"tableComment\":\"用户表\"," +
                "\"columns\":[]" +
                "}" +
                "]" +
                "}";

        // 执行
        SchemaDictionary result = formatter.parse(json.getBytes(StandardCharsets.UTF_8));

        // 验证
        assertNotNull(result);
        assertNotNull(result.getMetadata());
        assertEquals("MySQL", result.getMetadata().getDatabaseType());
        assertEquals("test_db", result.getMetadata().getDatabaseName());
        assertNotNull(result.getTables());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).getTableName());
    }

    @Test
    void testParse_InvalidJson() {
        // 准备
        String invalidJson = "not a valid json";

        // 执行并验证
        assertThrows(Exception.class, () -> {
            formatter.parse(invalidJson.getBytes(StandardCharsets.UTF_8));
        });
    }

    @Test
    void testParse_EmptyJson() {
        // 准备
        String emptyJson = "{}";

        // 执行
        SchemaDictionary result = formatter.parse(emptyJson.getBytes(StandardCharsets.UTF_8));

        // 验证
        assertNotNull(result);
    }

    @Test
    void testRoundTrip() {
        // 准备
        SchemaDictionary original = createTestDictionary();

        // 执行: 序列化 -> 反序列化
        byte[] serialized = formatter.format(original);
        SchemaDictionary deserialized = formatter.parse(serialized);

        // 验证
        assertNotNull(deserialized);
        assertNotNull(deserialized.getMetadata());
        assertEquals(original.getMetadata().getDatabaseType(), deserialized.getMetadata().getDatabaseType());
        assertNotNull(deserialized.getTables());
        assertEquals(original.getTables().size(), deserialized.getTables().size());
    }

    @Test
    void testFormat_PrettyPrint() {
        // 准备
        SchemaDictionary dictionary = createTestDictionary();

        // 执行
        String json = formatter.formatToString(dictionary);

        // 验证: 应该是格式化的JSON(包含换行和缩进)
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("  "));
    }

    @Test
    void testFormat_NullObject() {
        // 执行
        byte[] result = formatter.format(null);

        // 验证
        assertNotNull(result);
    }

    @Test
    void testParse_NullBytes() {
        // 执行并验证
        assertThrows(Exception.class, () -> {
            formatter.parse((byte[]) null);
        });
    }

    /**
     * 创建测试用的数据字典
     */
    private SchemaDictionary createTestDictionary() {
        SchemaDictionary dictionary = new SchemaDictionary();

        ExportMetadata metadata = new ExportMetadata();
        metadata.setExportTime(new java.util.Date());
        metadata.setDatabaseType("MySQL");
        metadata.setDatabaseName("test_db");
        dictionary.setMetadata(metadata);

        TableDefinition table = new TableDefinition();
        table.setTableName("user");
        table.setTableComment("用户表");

        ColumnDefinition idColumn = new ColumnDefinition();
        idColumn.setColumnName("id");
        idColumn.setDataType("BIGINT");
        idColumn.setNullable(false);
        idColumn.setIsPrimaryKey(true);

        ColumnDefinition nameColumn = new ColumnDefinition();
        nameColumn.setColumnName("username");
        nameColumn.setDataType("VARCHAR");
        nameColumn.setLength(50L);
        nameColumn.setNullable(false);

        table.setColumns(Arrays.asList(idColumn, nameColumn));
        dictionary.setTables(Arrays.asList(table));

        return dictionary;
    }
}
