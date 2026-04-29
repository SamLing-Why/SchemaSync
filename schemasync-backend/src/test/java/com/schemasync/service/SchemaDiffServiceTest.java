package com.schemasync.service;

import com.schemasync.model.dict.SchemaDictionary;
import com.schemasync.model.diff.SchemaDiff;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchemaDiffService单元测试
 * 测试版本对比和DDL生成功能
 */
@SpringBootTest
public class SchemaDiffServiceTest {

    @Autowired
    private SchemaDiffService schemaDiffService;

    @Test
    public void testCompareJsonFiles() throws Exception {
        // 创建测试JSON文件
        String oldJson = "{\"metadata\":{\"databaseType\":\"MySQL\",\"databaseVersion\":\"8.0\",\"databaseName\":\"test_db\"},\"tables\":[]}";
        String newJson = "{\"metadata\":{\"databaseType\":\"MySQL\",\"databaseVersion\":\"8.0\",\"databaseName\":\"test_db\"},\"tables\":[]}";

        MockMultipartFile oldFile = new MockMultipartFile(
                "oldFile", "old.json", "application/json", oldJson.getBytes()
        );
        MockMultipartFile newFile = new MockMultipartFile(
                "newFile", "new.json", "application/json", newJson.getBytes()
        );

        // 执行对比
        SchemaDiff diff = schemaDiffService.compareFiles(oldFile, newFile);

        // 验证结果
        assertNotNull(diff);
        assertNotNull(diff.getChanges());
        assertEquals(0, diff.getChanges().size());
    }

    @Test
    public void testFormatDiffAsJson() throws Exception {
        // 创建测试JSON文件
        String json = "{\"metadata\":{\"databaseType\":\"MySQL\"},\"tables\":[]}";

        MockMultipartFile oldFile = new MockMultipartFile(
                "oldFile", "old.json", "application/json", json.getBytes()
        );
        MockMultipartFile newFile = new MockMultipartFile(
                "newFile", "new.json", "application/json", json.getBytes()
        );

        // 执行对比
        SchemaDiff diff = schemaDiffService.compareFiles(oldFile, newFile);

        // 测试JSON格式导出
        byte[] jsonBytes = schemaDiffService.formatDiff(diff, "json");
        assertNotNull(jsonBytes);
        assertTrue(jsonBytes.length > 0);
    }

    @Test
    public void testFormatDiffAsExcel() throws Exception {
        // 创建测试JSON文件
        String json = "{\"metadata\":{\"databaseType\":\"MySQL\"},\"tables\":[]}";

        MockMultipartFile oldFile = new MockMultipartFile(
                "oldFile", "old.json", "application/json", json.getBytes()
        );
        MockMultipartFile newFile = new MockMultipartFile(
                "newFile", "new.json", "application/json", json.getBytes()
        );

        // 执行对比
        SchemaDiff diff = schemaDiffService.compareFiles(oldFile, newFile);

        // 测试Excel格式导出
        byte[] excelBytes = schemaDiffService.formatDiff(diff, "excel");
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
        // Excel文件应该以PK开头（ZIP格式）
        assertEquals('P', (char) excelBytes[0]);
        assertEquals('K', (char) excelBytes[1]);
    }

    @Test
    public void testGenerateDdlFromDiff() throws Exception {
        // 创建测试JSON文件（包含表定义）
        String json = "{\"metadata\":{\"databaseType\":\"MySQL\",\"databaseVersion\":\"8.0\",\"databaseName\":\"test_db\"},\"tables\":[{\"tableName\":\"user\",\"tableComment\":\"User Table\",\"tableType\":\"BASE TABLE\",\"columns\":[{\"columnName\":\"id\",\"dataType\":\"BIGINT\",\"isPrimaryKey\":true,\"nullable\":false},{\"columnName\":\"name\",\"dataType\":\"VARCHAR\",\"length\":100,\"nullable\":false}]}]}";

        MockMultipartFile oldFile = new MockMultipartFile(
                "oldFile", "old.json", "application/json", "{}".getBytes("UTF-8")
        );
        MockMultipartFile newFile = new MockMultipartFile(
                "newFile", "new.json", "application/json", json.getBytes("UTF-8")
        );

        // 生成DDL
        byte[] ddlBytes = schemaDiffService.generateDdlFromDiff(oldFile, newFile);
        assertNotNull(ddlBytes);
        assertTrue(ddlBytes.length > 0);

        String ddl = new String(ddlBytes, "UTF-8");
        assertTrue(ddl.contains("CREATE TABLE"));
        assertTrue(ddl.contains("user"));
    }
}
