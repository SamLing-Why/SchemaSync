package com.schemasync.service;

import com.schemasync.model.dict.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据字典校验服务
 * 校验上传的数据字典文件质量，发现表结构问题
 * 
 * @author SchemaSync Team
 * @since 2026-07-14
 */
@Service
public class SchemaValidatorService {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaValidatorService.class);
    
    @Autowired
    private SchemaDictionaryParser parser;
    
    /**
     * 校验结果记录
     */
    public static class ValidationResult {
        private String tableName;
        private String tableComment;
        private String checkItem;
        private String issue;
        
        public ValidationResult(String tableName, String tableComment, String checkItem, String issue) {
            this.tableName = tableName;
            this.tableComment = tableComment;
            this.checkItem = checkItem;
            this.issue = issue;
        }
        
        public String getTableName() { return tableName; }
        public String getTableComment() { return tableComment; }
        public String getCheckItem() { return checkItem; }
        public String getIssue() { return issue; }
    }
    
    /**
     * 校验数据字典文件
     * 
     * @param inputStream 文件输入流
     * @param fileType 文件类型(excel/json)
     * @return 校验结果列表
     */
    public List<ValidationResult> validate(InputStream inputStream, String fileType) {
        List<ValidationResult> results = new ArrayList<>();
        
        try {
            SchemaDictionary dictionary;
            if ("excel".equals(fileType)) {
                dictionary = parser.parseExcel(inputStream);
            } else {
                java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                dictionary = parser.parseJson(outputStream.toByteArray());
            }
            
            if (dictionary.getTables() == null || dictionary.getTables().isEmpty()) {
                results.add(new ValidationResult("全局", "", "文件解析", "数据字典中没有找到任何表定义"));
                return results;
            }
            
            // 逐表校验
            for (TableDefinition table : dictionary.getTables()) {
                validateTable(table, results);
            }
            
            log.info("数据字典校验完成, 共{}个表, 发现{}个问题", 
                    dictionary.getTables().size(), results.size());
            
        } catch (Exception e) {
            log.error("校验失败", e);
            results.add(new ValidationResult("全局", "", "文件解析", "解析文件失败: " + e.getMessage()));
        }
        
        return results;
    }
    
    /**
     * 校验单个表
     */
    private void validateTable(TableDefinition table, List<ValidationResult> results) {
        String tableName = table.getTableName();
        String tableComment = table.getTableComment() != null ? table.getTableComment() : "";
        
        if (tableName == null || tableName.trim().isEmpty()) {
            results.add(new ValidationResult("未知表名", tableComment, "基本信息", "表名为空"));
            return;
        }
        
        // 1. 校验重复字段（考虑L列字段名称新）
        checkDuplicateColumns(table, results);
        
        // 2. 校验索引/主键引用的字段是否存在
        checkIndexColumnsExist(table, results);
        
        // 3. 校验主键定义
        checkPrimaryKey(table, results);
        
        // 4. 校验字段数据类型
        checkColumnDataTypes(table, results);
        
        // 5. 校验字段长度/精度合理性
        checkColumnLengthPrecision(table, results);
        
        // 6. 校验索引名称重复
        checkDuplicateIndexNames(table, results);
        
        // 7. 校验外键引用的字段是否存在
        checkForeignKeyColumns(table, results);
    }
    
    /**
     * 校验1：检查重复字段（考虑L列字段名称新）
     */
    private void checkDuplicateColumns(TableDefinition table, List<ValidationResult> results) {
        if (table.getColumns() == null) return;
        
        // 使用effectiveName来检查重复
        Map<String, Long> nameCounts = table.getColumns().stream()
                .map(col -> col.getEffectiveName().toLowerCase())
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
        
        nameCounts.forEach((name, count) -> {
            if (count > 1) {
                results.add(new ValidationResult(
                        table.getTableName(),
                        table.getTableComment() != null ? table.getTableComment() : "",
                        "重复字段",
                        String.format("存在重复字段名: %s (出现%d次)", name, count)
                ));
            }
        });
    }
    
    /**
     * 校验2：检查索引/主键引用的字段是否存在
     */
    private void checkIndexColumnsExist(TableDefinition table, List<ValidationResult> results) {
        if (table.getIndexes() == null || table.getColumns() == null) return;
        
        // 构建有效字段名集合（考虑L列字段名称新）
        Set<String> effectiveNames = table.getColumns().stream()
                .map(col -> col.getEffectiveName().toLowerCase())
                .collect(Collectors.toSet());
        
        // 同时保留原始字段名（索引可能引用原始名称）
        Set<String> originalNames = table.getColumns().stream()
                .map(col -> col.getColumnName().toLowerCase())
                .collect(Collectors.toSet());
        
        for (IndexDefinition idx : table.getIndexes()) {
            if (idx.getColumns() == null) continue;
            
            for (String idxCol : idx.getColumns()) {
                if (idxCol == null || idxCol.trim().isEmpty()) {
                    results.add(new ValidationResult(
                            table.getTableName(),
                            table.getTableComment() != null ? table.getTableComment() : "",
                            "索引字段",
                            String.format("索引 %s 中包含空字段名", idx.getIndexName())
                    ));
                    continue;
                }
                
                String lowerIdxCol = idxCol.toLowerCase();
                if (!effectiveNames.contains(lowerIdxCol) && !originalNames.contains(lowerIdxCol)) {
                    results.add(new ValidationResult(
                            table.getTableName(),
                            table.getTableComment() != null ? table.getTableComment() : "",
                            "索引字段",
                            String.format("索引 %s 引用的字段 %s 在表字段中不存在", 
                                    idx.getIndexName(), idxCol)
                    ));
                }
            }
        }
    }
    
    /**
     * 校验3：检查主键定义
     */
    private void checkPrimaryKey(TableDefinition table, List<ValidationResult> results) {
        if (table.getColumns() == null) return;
        
        // 检查是否有主键字段
        List<ColumnDefinition> pkColumns = table.getColumns().stream()
                .filter(col -> col.getIsPrimaryKey() != null && col.getIsPrimaryKey())
                .collect(Collectors.toList());
        
        // 检查索引中是否有PRIMARY索引
        boolean hasPrimaryIndex = table.getIndexes() != null && table.getIndexes().stream()
                .anyMatch(idx -> "PRIMARY".equalsIgnoreCase(idx.getIndexType()) 
                        || "PRIMARY".equalsIgnoreCase(idx.getIndexName()));
        
        if (pkColumns.isEmpty() && !hasPrimaryIndex) {
            results.add(new ValidationResult(
                    table.getTableName(),
                    table.getTableComment() != null ? table.getTableComment() : "",
                    "主键定义",
                    "表没有定义主键"
            ));
        }
        
        // 检查主键字段是否允许NULL
        for (ColumnDefinition pkCol : pkColumns) {
            if (pkCol.getNullable() != null && pkCol.getNullable()) {
                results.add(new ValidationResult(
                        table.getTableName(),
                        table.getTableComment() != null ? table.getTableComment() : "",
                        "主键定义",
                        String.format("主键字段 %s 允许NULL，这可能导致问题", pkCol.getEffectiveName())
                ));
            }
        }
    }
    
    /**
     * 校验4：检查字段数据类型
     */
    private void checkColumnDataTypes(TableDefinition table, List<ValidationResult> results) {
        if (table.getColumns() == null) return;
        
        for (ColumnDefinition col : table.getColumns()) {
            if (col.getDataType() == null || col.getDataType().trim().isEmpty()) {
                results.add(new ValidationResult(
                        table.getTableName(),
                        table.getTableComment() != null ? table.getTableComment() : "",
                        "数据类型",
                        String.format("字段 %s 没有指定数据类型", col.getEffectiveName())
                ));
            }
        }
    }
    
    /**
     * 校验5：检查字段长度/精度合理性
     */
    private void checkColumnLengthPrecision(TableDefinition table, List<ValidationResult> results) {
        if (table.getColumns() == null) return;
        
        for (ColumnDefinition col : table.getColumns()) {
            String dataType = col.getDataType() != null ? col.getDataType().toUpperCase() : "";
            
            // VARCHAR等字符类型需要有长度
            if (dataType.equals("VARCHAR") || dataType.equals("VARCHAR2") 
                    || dataType.equals("NVARCHAR") || dataType.equals("NVARCHAR2")
                    || dataType.equals("CHAR") || dataType.equals("NCHAR")) {
                if (col.getLength() == null || col.getLength() <= 0) {
                    results.add(new ValidationResult(
                            table.getTableName(),
                            table.getTableComment() != null ? table.getTableComment() : "",
                            "字段长度",
                            String.format("字段 %s 类型为 %s，但未指定长度", col.getEffectiveName(), dataType)
                    ));
                }
            }
            
            // DECIMAL/NUMERIC类型需要有精度和小数位
            if (dataType.equals("DECIMAL") || dataType.equals("NUMERIC") || dataType.equals("NUMBER")) {
                if (col.getPrecision() == null || col.getPrecision() <= 0) {
                    results.add(new ValidationResult(
                            table.getTableName(),
                            table.getTableComment() != null ? table.getTableComment() : "",
                            "字段精度",
                            String.format("字段 %s 类型为 %s，但未指定精度", col.getEffectiveName(), dataType)
                    ));
                }
            }
            
            // 检查长度是否合理（不超过65535）
            if (col.getLength() != null && col.getLength() > 65535) {
                // 排除TEXT/BLOB等类型
                if (!dataType.equals("TEXT") && !dataType.equals("LONGTEXT") 
                        && !dataType.equals("MEDIUMTEXT") && !dataType.equals("TINYTEXT")
                        && !dataType.equals("BLOB") && !dataType.equals("LONGBLOB")
                        && !dataType.equals("MEDIUMBLOB") && !dataType.equals("TINYBLOB")) {
                    results.add(new ValidationResult(
                            table.getTableName(),
                            table.getTableComment() != null ? table.getTableComment() : "",
                            "字段长度",
                            String.format("字段 %s 长度 %d 超过65535，请确认是否合理", 
                                    col.getEffectiveName(), col.getLength())
                    ));
                }
            }
        }
    }
    
    /**
     * 校验6：检查索引名称重复
     */
    private void checkDuplicateIndexNames(TableDefinition table, List<ValidationResult> results) {
        if (table.getIndexes() == null) return;
        
        Map<String, Long> nameCounts = table.getIndexes().stream()
                .filter(idx -> idx.getIndexName() != null)
                .map(idx -> idx.getIndexName().toLowerCase())
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
        
        nameCounts.forEach((name, count) -> {
            if (count > 1) {
                results.add(new ValidationResult(
                        table.getTableName(),
                        table.getTableComment() != null ? table.getTableComment() : "",
                        "索引名称",
                        String.format("存在重复索引名: %s (出现%d次)", name, count)
                ));
            }
        });
    }
    
    /**
     * 校验7：检查外键引用的字段是否存在
     */
    private void checkForeignKeyColumns(TableDefinition table, List<ValidationResult> results) {
        if (table.getForeignKeys() == null || table.getColumns() == null) return;
        
        Set<String> effectiveNames = table.getColumns().stream()
                .map(col -> col.getEffectiveName().toLowerCase())
                .collect(Collectors.toSet());
        
        for (ForeignKeyDefinition fk : table.getForeignKeys()) {
            String fkCol = fk.getColumnName();
            if (fkCol != null && !effectiveNames.contains(fkCol.toLowerCase())) {
                results.add(new ValidationResult(
                        table.getTableName(),
                        table.getTableComment() != null ? table.getTableComment() : "",
                        "外键字段",
                        String.format("外键 %s 引用的字段 %s 在表字段中不存在", 
                                fk.getConstraintName(), fkCol)
                ));
            }
        }
    }
    
    /**
     * 将校验结果导出为Excel
     * 
     * @param results 校验结果列表
     * @return Excel字节数组
     */
    public byte[] exportResultsToExcel(List<ValidationResult> results) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("校验结果");
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"表名", "表注释", "校验项", "校验问题描述"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            if (results != null && !results.isEmpty()) {
                for (int i = 0; i < results.size(); i++) {
                    ValidationResult result = results.get(i);
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(result.getTableName());
                    row.createCell(1).setCellValue(result.getTableComment());
                    row.createCell(2).setCellValue(result.getCheckItem());
                    row.createCell(3).setCellValue(result.getIssue());
                }
            } else {
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("无问题");
                row.createCell(1).setCellValue("");
                row.createCell(2).setCellValue("");
                row.createCell(3).setCellValue("数据字典校验通过，未发现问题");
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("导出校验结果失败", e);
            throw new RuntimeException("导出校验结果失败: " + e.getMessage(), e);
        }
    }
}
