package com.schemasync.service;

import com.schemasync.formatter.JsonFormatter;
import com.schemasync.model.dict.*;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据字典解析器
 * 从JSON或Excel文件解析回SchemaDictionary
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
@Service
public class SchemaDictionaryParser {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaDictionaryParser.class);
    
    @Autowired
    private JsonFormatter jsonFormatter;
    
    /**
     * 从JSON解析
     */
    public SchemaDictionary parseJson(byte[] data) {
        return jsonFormatter.parse(data);
    }
    
    /**
     * 从Excel解析(反向解析6个Sheet)
     */
    public SchemaDictionary parseExcel(InputStream inputStream) {
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            
            SchemaDictionary dictionary = new SchemaDictionary();
            ExportMetadata metadata = new ExportMetadata();
            dictionary.setMetadata(metadata);
            
            // 1. 从"概述信息"Sheet读取metadata
            parseOverviewSheet(workbook, metadata);
            
            // 2. 从"表级别信息"Sheet读取表列表
            List<TableDefinition> tables = parseTablesSheet(workbook);
            
            // 3. 从"字段级别信息"Sheet按tableName分组读取字段
            Map<String, List<ColumnDefinition>> columnsByTable = parseColumnsSheet(workbook);
            
            // 4. 从"索引信息"Sheet按tableName分组读取索引
            Map<String, List<IndexDefinition>> indexesByTable = parseIndexesSheet(workbook);
            
            // 5. 从"约束信息"Sheet按tableName分组读取约束
            Map<String, List<ForeignKeyDefinition>> constraintsByTable = parseConstraintsSheet(workbook);
            
            // 6. 组装数据
            for (TableDefinition table : tables) {
                table.setColumns(columnsByTable.getOrDefault(table.getTableName(), new ArrayList<>()));
                table.setIndexes(indexesByTable.getOrDefault(table.getTableName(), new ArrayList<>()));
                table.setForeignKeys(constraintsByTable.getOrDefault(table.getTableName(), new ArrayList<>()));
            }
            
            dictionary.setTables(tables);
            
            log.info("Excel解析成功, 表数量: {}", tables.size());
            return dictionary;
            
        } catch (Exception e) {
            log.error("Excel解析失败", e);
            throw new RuntimeException("Excel解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析概述信息Sheet
     */
    private void parseOverviewSheet(Workbook workbook, ExportMetadata metadata) {
        Sheet sheet = workbook.getSheet("概述信息");
        if (sheet == null) return;
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            String field = getCellStringValue(row.getCell(0));
            String value = getCellStringValue(row.getCell(1));
            
            if ("数据库类型".equals(field)) metadata.setDatabaseType(value);
            else if ("数据库版本".equals(field)) metadata.setDatabaseVersion(value);
            else if ("数据库名称".equals(field)) metadata.setDatabaseName(value);
            else if ("工具版本".equals(field)) metadata.setToolVersion(value);
            else if ("导出的日期时间".equals(field)) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    metadata.setExportTime(sdf.parse(value));
                } catch (ParseException e) {
                    log.warn("解析导出时间失败: {}", value);
                }
            }
        }
    }
    
    /**
     * 解析表级别信息Sheet
     */
    private List<TableDefinition> parseTablesSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet("表级别信息");
        if (sheet == null) return new ArrayList<>();
        
        List<TableDefinition> tables = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            TableDefinition table = new TableDefinition();
            table.setTableName(getCellStringValue(row.getCell(0)));
            table.setTableComment(getCellStringValue(row.getCell(1)));
            table.setTableType(getCellStringValue(row.getCell(2)));
            
            String createTimeStr = getCellStringValue(row.getCell(3));
            if (!createTimeStr.isEmpty()) {
                try {
                    table.setCreateTime(sdf.parse(createTimeStr));
                } catch (ParseException e) {
                    log.warn("解析创建时间失败: {}", createTimeStr);
                }
            }
            
            String updateTimeStr = getCellStringValue(row.getCell(4));
            if (!updateTimeStr.isEmpty()) {
                try {
                    table.setUpdateTime(sdf.parse(updateTimeStr));
                } catch (ParseException e) {
                    log.warn("解析更新时间失败: {}", updateTimeStr);
                }
            }
            
            table.setEngine(getCellStringValue(row.getCell(5)));
            table.setCharset(getCellStringValue(row.getCell(6)));
            
            tables.add(table);
        }
        
        return tables;
    }
    
    /**
     * 解析字段级别信息Sheet
     */
    private Map<String, List<ColumnDefinition>> parseColumnsSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet("字段级别信息");
        if (sheet == null) return new HashMap<>();
        
        Map<String, List<ColumnDefinition>> columnsByTable = new HashMap<>();
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            ColumnDefinition column = new ColumnDefinition();
            String tableName = getCellStringValue(row.getCell(0));
            column.setColumnName(getCellStringValue(row.getCell(1)));
            column.setDataType(getCellStringValue(row.getCell(2)));
            
            String lengthStr = getCellStringValue(row.getCell(3));
            if (!lengthStr.isEmpty()) {
                column.setLength(Long.parseLong(lengthStr));
            }
            
            String precisionStr = getCellStringValue(row.getCell(4));
            if (!precisionStr.isEmpty()) {
                column.setPrecision(Long.parseLong(precisionStr));
            }
            
            String nullableStr = getCellStringValue(row.getCell(5));
            column.setNullable("是".equals(nullableStr));
            
            String defaultValue = getCellStringValue(row.getCell(6));
            if (!defaultValue.isEmpty()) {
                column.setDefaultValue(defaultValue);
            }
            
            String pkStr = getCellStringValue(row.getCell(7));
            column.setIsPrimaryKey("是".equals(pkStr));
            
            String autoIncStr = getCellStringValue(row.getCell(8));
            column.setIsAutoIncrement("是".equals(autoIncStr));
            
            column.setComment(getCellStringValue(row.getCell(9)));
            column.setCharset(getCellStringValue(row.getCell(10)));
            
            columnsByTable.computeIfAbsent(tableName, k -> new ArrayList<>()).add(column);
        }
        
        return columnsByTable;
    }
    
    /**
     * 解析索引信息Sheet
     */
    private Map<String, List<IndexDefinition>> parseIndexesSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet("索引信息");
        if (sheet == null) return new HashMap<>();
        
        Map<String, List<IndexDefinition>> indexesByTable = new HashMap<>();
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            IndexDefinition index = new IndexDefinition();
            String tableName = getCellStringValue(row.getCell(0));
            index.setIndexName(getCellStringValue(row.getCell(1)));
            index.setIndexType(getCellStringValue(row.getCell(2)));
            
            String columnsStr = getCellStringValue(row.getCell(3));
            if (!columnsStr.isEmpty()) {
                index.setColumns(Arrays.asList(columnsStr.split(",\\s*")));
            }
            
            index.setComment(getCellStringValue(row.getCell(4)));
            
            indexesByTable.computeIfAbsent(tableName, k -> new ArrayList<>()).add(index);
        }
        
        return indexesByTable;
    }
    
    /**
     * 解析约束信息Sheet
     */
    private Map<String, List<ForeignKeyDefinition>> parseConstraintsSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet("约束信息");
        if (sheet == null) return new HashMap<>();
        
        Map<String, List<ForeignKeyDefinition>> constraintsByTable = new HashMap<>();
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            // 只解析外键约束
            String constraintType = getCellStringValue(row.getCell(2));
            if (!"FK".equals(constraintType)) continue;
            
            ForeignKeyDefinition fk = new ForeignKeyDefinition();
            String tableName = getCellStringValue(row.getCell(0));
            fk.setConstraintName(getCellStringValue(row.getCell(1)));
            // 注意:约束信息Sheet中没有本端字段列,需要从字段信息推断
            // 这里暂时使用约束名称作为占位,实际应该通过其他方式获取
            fk.setColumnName(""); // TODO: 需要从字段信息中推断
            fk.setReferencedTable(getCellStringValue(row.getCell(3)));
            fk.setReferencedColumn(getCellStringValue(row.getCell(4)));
            
            // 解析级联规则
            String cascadeRule = getCellStringValue(row.getCell(5));
            if (!cascadeRule.isEmpty()) {
                try {
                    if (cascadeRule.contains("ON UPDATE")) {
                        int updateStart = cascadeRule.indexOf("ON UPDATE") + "ON UPDATE".length();
                        int deleteIdx = cascadeRule.indexOf("ON DELETE");
                        if (deleteIdx > 0) {
                            String updatePart = cascadeRule.substring(updateStart, deleteIdx).trim();
                            fk.setOnUpdate(updatePart.split(",")[0].trim());
                            fk.setOnDelete(cascadeRule.substring(deleteIdx + "ON DELETE".length()).trim());
                        } else {
                            fk.setOnUpdate(cascadeRule.substring(updateStart).trim().split(",")[0].trim());
                        }
                    } else if (cascadeRule.contains("ON DELETE")) {
                        fk.setOnDelete(cascadeRule.substring(cascadeRule.indexOf("ON DELETE") + "ON DELETE".length()).trim());
                    }
                } catch (Exception e) {
                    log.warn("解析级联规则失败: {}", cascadeRule, e);
                }
            }
            
            constraintsByTable.computeIfAbsent(tableName, k -> new ArrayList<>()).add(fk);
        }
        
        return constraintsByTable;
    }
    
    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 使用与导出时相同的格式
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    return sdf.format(cell.getDateCellValue());
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    }
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
