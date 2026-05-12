package com.schemasync.service;

import com.schemasync.differ.SchemaDiffer;
import com.schemasync.formatter.ExcelFormatter;
import com.schemasync.formatter.JsonFormatter;
import com.schemasync.model.dict.ColumnDefinition;
import com.schemasync.model.dict.SchemaDictionary;
import com.schemasync.model.dict.TableDefinition;
import com.schemasync.model.diff.SchemaChange;
import com.schemasync.model.diff.SchemaDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 数据字典对比服务
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Service
public class SchemaDiffService {

    private static final Logger log = LoggerFactory.getLogger(SchemaDiffService.class);

    @Autowired
    private SchemaDiffer schemaDiffer;

    @Autowired
    private JsonFormatter jsonFormatter;
    
    @Autowired
    private SchemaFlattener schemaFlattener;
    
    @Autowired
    private SchemaDictionaryParser schemaDictionaryParser;
    
    @Autowired
    private ExcelFormatter excelFormatter;
    
    @Autowired
    private DdlGeneratorService ddlGeneratorService;
    
    /**
     * 解析文件(支持JSON和Excel)
     */
    private SchemaDictionary parseFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            return schemaDictionaryParser.parseExcel(file.getInputStream());
        } else {
            return jsonFormatter.parse(file.getBytes());
        }
    }

    /**
     * 对比两个数据字典文件
     * 
     * @param oldFile 旧版本文件
     * @param newFile 新版本文件
     * @return 差异结果
     */
    public SchemaDiff compareFiles(MultipartFile oldFile, MultipartFile newFile) {
        try {
            log.info("开始对比文件: {} vs {}", oldFile.getOriginalFilename(), newFile.getOriginalFilename());

            // 1. 解析旧版本(支持JSON和Excel)
            SchemaDictionary oldDict = parseFile(oldFile);
            log.debug("解析旧版本成功, 表数量: {}", 
                    oldDict.getTables() != null ? oldDict.getTables().size() : 0);

            // 2. 解析新版本(支持JSON和Excel)
            SchemaDictionary newDict = parseFile(newFile);
            log.debug("解析新版本成功, 表数量: {}", 
                    newDict.getTables() != null ? newDict.getTables().size() : 0);

            // 3. 执行对比
            SchemaDiff diff = schemaDiffer.compare(oldDict, newDict);

            log.info("对比完成, 发现{}处变更", diff.getChanges().size());
            return diff;

        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("对比失败", e);
            throw new RuntimeException("对比失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对比两个数据字典对象
     */
    public SchemaDiff compareDictionaries(SchemaDictionary oldDict, SchemaDictionary newDict) {
        log.info("开始对比数据字典对象");
        SchemaDiff diff = schemaDiffer.compare(oldDict, newDict);
        log.info("对比完成, 发现{}处变更", diff.getChanges().size());
        return diff;
    }

    /**
     * 格式化差异结果为指定格式
     */
    public byte[] formatDiff(SchemaDiff diff, String format) {
        if ("excel".equalsIgnoreCase(format)) {
            // 使用简单表格格式导出差异列表
            return exportDiffAsSimpleExcel(diff);
        } else {
            return jsonFormatter.formatDiff(diff);
        }
    }
    
    /**
     * 将差异结果转为JSON字节数组(保留旧方法兼容)
     */
    public byte[] diffToJsonBytes(SchemaDiff diff) {
        return jsonFormatter.formatDiff(diff);
    }

    /**
     * 将差异结果转为JSON字符串(保留旧方法兼容)
     */
    public String diffToJsonString(SchemaDiff diff) {
        return jsonFormatter.formatDiffToString(diff);
    }
    
    /**
     * 从对比结果生成DDL脚本（差异化）
     */
    public byte[] generateDdlFromDiff(MultipartFile oldFile, MultipartFile newFile) {
        try {
            // 1. 解析两个版本
            SchemaDictionary oldDict = parseFile(oldFile);
            SchemaDictionary newDict = parseFile(newFile);
            
            // 2. 执行对比
            SchemaDiff diff = schemaDiffer.compare(oldDict, newDict);
            
            // 3. 只生成新增和修改的表的DDL
            String ddl = generateDdlForChangedTables(newDict, diff);
            
            return ddl.getBytes("UTF-8");
        } catch (Exception e) {
            log.error("生成DDL失败", e);
            throw new RuntimeException("生成DDL失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 为变更的表生成DDL（根据变更类型生成精确DDL）
     */
    private String generateDdlForChangedTables(SchemaDictionary newDict, SchemaDiff diff) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- ============================================\n");
        sql.append("-- SchemaSync 差异化DDL脚本\n");
        sql.append("-- 生成时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("\n");
        sql.append("-- ============================================\n\n");
        
        if (diff.getChanges() == null || diff.getChanges().isEmpty()) {
            sql.append("-- 无变更\n");
            return sql.toString();
        }
        
        // 按表名分组变更
        Map<String, List<SchemaChange>> changesByTable = diff.getChanges().stream()
            .collect(Collectors.groupingBy(SchemaChange::getTableName));
        
        // 为每个变更的表生成DDL
        for (Map.Entry<String, List<SchemaChange>> entry : changesByTable.entrySet()) {
            String tableName = entry.getKey();
            List<SchemaChange> tableChanges = entry.getValue();
            
            // 查找新版本的表定义
            TableDefinition newTable = findTableByName(newDict, tableName);
            
            for (SchemaChange change : tableChanges) {
                com.schemasync.model.diff.ChangeType changeType = change.getChangeType();
                if (changeType == com.schemasync.model.diff.ChangeType.TABLE_ADD) {
                    // 新增表：生成CREATE TABLE
                    sql.append("-- 变更类型: 新增表\n");
                    if (newTable != null) {
                        if ("VIEW".equalsIgnoreCase(newTable.getTableType())) {
                            sql.append(ddlGeneratorService.generateCreateView(newTable));
                        } else {
                            sql.append(ddlGeneratorService.generateCreateTable(newTable));
                        }
                    }
                    sql.append("\n\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.TABLE_DROP) {
                    // 删除表：生成注释掉的DROP TABLE
                    sql.append("-- 变更类型: 删除表 (已注释)\n");
                    sql.append("-- DROP TABLE `").append(tableName).append("`;\n");
                    sql.append("\n\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.TABLE_MODIFY) {
                    // 修改表属性：生成ALTER TABLE
                    sql.append("-- 变更类型: 修改表\n");
                    if (newTable != null && change.getDetails() != null) {
                        sql.append("-- 详情: ").append(change.getDetails().toString()).append("\n");
                        sql.append("-- 请手动修改表属性\n");
                    }
                    sql.append("\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_ADD) {
                    // 新增字段：生成ALTER TABLE ADD COLUMN
                    sql.append("-- 变更类型: 新增字段\n");
                    if (newTable != null && change.getColumnName() != null) {
                        ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                        if (newColumn != null) {
                            sql.append(generateAddColumnSql(tableName, newColumn));
                        }
                    }
                    sql.append("\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_DROP) {
                    // 删除字段：生成注释掉的ALTER TABLE DROP COLUMN
                    sql.append("-- 变更类型: 删除字段 (已注释)\n");
                    if (change.getColumnName() != null) {
                        sql.append("-- ALTER TABLE `").append(tableName)
                           .append("` DROP COLUMN `").append(change.getColumnName()).append("`;\n");
                    }
                    sql.append("\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_MODIFY) {
                    // 修改字段：生成ALTER TABLE MODIFY COLUMN
                    sql.append("-- 变更类型: 修改字段\n");
                    if (newTable != null && change.getColumnName() != null) {
                        ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                        if (newColumn != null) {
                            sql.append(generateModifyColumnSql(tableName, newColumn));
                        }
                    }
                    sql.append("\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_ADD) {
                    // 新增索引：生成CREATE INDEX
                    sql.append("-- 变更类型: 新增索引\n");
                    if (newTable != null && change.getDetails() != null) {
                        com.schemasync.model.dict.IndexDefinition index = extractIndexDefinition(change.getDetails());
                        if (index != null) {
                            sql.append(generateCreateIndexSql(tableName, index));
                        } else {
                            sql.append("-- 警告: 无法提取索引定义\n");
                        }
                    }
                    sql.append("\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_DROP) {
                    // 删除索引：生成注释掉的DROP INDEX
                    sql.append("-- 变更类型: 删除索引 (已注释)\n");
                    if (change.getDetails() != null) {
                        com.schemasync.model.dict.IndexDefinition index = extractIndexDefinition(change.getDetails());
                        if (index != null) {
                            sql.append("-- ALTER TABLE `").append(tableName)
                               .append("` DROP INDEX `").append(index.getIndexName()).append("`;\n");
                        }
                    }
                    sql.append("\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_MODIFY) {
                    // 修改索引：删除旧索引+创建新索引
                    sql.append("-- 变更类型: 修改索引\n");
                    if (change.getDetails() instanceof java.util.Map) {
                        java.util.Map<?, ?> map = (java.util.Map<?, ?>) change.getDetails();
                        if (map.containsKey("oldValue") && map.containsKey("newValue")) {
                            com.schemasync.model.dict.IndexDefinition oldIndex = extractIndexDefinition(map.get("oldValue"));
                            com.schemasync.model.dict.IndexDefinition newIndex = extractIndexDefinition(map.get("newValue"));
                            if (oldIndex != null) {
                                sql.append("-- 删除旧索引\n");
                                sql.append("ALTER TABLE `").append(tableName)
                                   .append("` DROP INDEX `").append(oldIndex.getIndexName()).append("`;\n");
                            }
                            if (newIndex != null) {
                                sql.append("-- 创建新索引\n");
                                sql.append(generateCreateIndexSql(tableName, newIndex));
                            }
                        }
                    }
                    sql.append("\n");
                } else {
                    sql.append("-- 未知变更类型: ").append(changeType).append("\n\n");
                }
            }
        }
        
        return sql.toString();
    }
    
    /**
     * 从details中提取IndexDefinition对象
     */
    private com.schemasync.model.dict.IndexDefinition extractIndexDefinition(Object details) {
        if (details == null) return null;
        
        // 直接是IndexDefinition对象
        if (details instanceof com.schemasync.model.dict.IndexDefinition) {
            return (com.schemasync.model.dict.IndexDefinition) details;
        }
        
        // Map中包含indexDefinition或其他键
        if (details instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) details;
            
            // 尝试直接获取IndexDefinition
            for (Object value : map.values()) {
                if (value instanceof com.schemasync.model.dict.IndexDefinition) {
                    return (com.schemasync.model.dict.IndexDefinition) value;
                }
            }
            
            // 如果Map本身就是索引信息，尝试构建IndexDefinition
            if (map.containsKey("indexName") || map.containsKey("indexType")) {
                com.schemasync.model.dict.IndexDefinition index = new com.schemasync.model.dict.IndexDefinition();
                if (map.containsKey("indexName")) {
                    index.setIndexName((String) map.get("indexName"));
                }
                if (map.containsKey("indexType")) {
                    index.setIndexType((String) map.get("indexType"));
                }
                if (map.containsKey("isUnique")) {
                    index.setIsUnique((Boolean) map.get("isUnique"));
                }
                if (map.containsKey("columns") && map.get("columns") instanceof java.util.List) {
                    index.setColumns((java.util.List<String>) map.get("columns"));
                }
                return index;
            }
        }
        
        return null;
    }
    
    /**
     * 根据表名查找表定义
     */
    private TableDefinition findTableByName(SchemaDictionary dict, String tableName) {
        if (dict.getTables() == null) return null;
        return dict.getTables().stream()
            .filter(t -> tableName.equals(t.getTableName()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 根据字段名查找字段定义
     */
    private ColumnDefinition findColumnByName(TableDefinition table, String columnName) {
        if (table.getColumns() == null) return null;
        return table.getColumns().stream()
            .filter(c -> columnName.equals(c.getColumnName()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 生成新增字段的SQL
     */
    private String generateAddColumnSql(String tableName, ColumnDefinition column) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("` ADD COLUMN `");
        sql.append(column.getColumnName()).append("` ");
        
        // 数据类型
        sql.append(column.getDataType().toUpperCase());
        if (column.getPrecision() != null && column.getScale() != null) {
            sql.append("(").append(column.getPrecision()).append(",").append(column.getScale()).append(")");
        } else if (column.getLength() != null && column.getLength() > 0) {
            sql.append("(").append(column.getLength()).append(")");
        }
        
        // 约束
        if (column.getNullable() != null && !column.getNullable()) {
            sql.append(" NOT NULL");
        }
        if (column.getDefaultValue() != null && !((String)column.getDefaultValue()).isEmpty()) {
            sql.append(" DEFAULT ").append(column.getDefaultValue());
        }
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sql.append(" COMMENT '").append(column.getComment()).append("'");
        }
        
        sql.append(";\n");
        return sql.toString();
    }
    
    /**
     * 生成修改字段的SQL
     */
    private String generateModifyColumnSql(String tableName, ColumnDefinition column) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("` MODIFY COLUMN `");
        sql.append(column.getColumnName()).append("` ");
        
        // 数据类型
        sql.append(column.getDataType().toUpperCase());
        if (column.getPrecision() != null && column.getScale() != null) {
            sql.append("(").append(column.getPrecision()).append(",").append(column.getScale()).append(")");
        } else if (column.getLength() != null && column.getLength() > 0) {
            sql.append("(").append(column.getLength()).append(")");
        }
        
        // 约束
        if (column.getNullable() != null && !column.getNullable()) {
            sql.append(" NOT NULL");
        }
        if (column.getDefaultValue() != null && !((String)column.getDefaultValue()).isEmpty()) {
            sql.append(" DEFAULT ").append(column.getDefaultValue());
        }
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sql.append(" COMMENT '").append(column.getComment()).append("'");
        }
        
        sql.append(";\n");
        return sql.toString();
    }
    
    /**
     * 生成创建索引的SQL
     */
    private String generateCreateIndexSql(String tableName, com.schemasync.model.dict.IndexDefinition index) {
        StringBuilder sql = new StringBuilder();
        
        // 判断是否唯一索引
        if (index.getIsUnique() != null && index.getIsUnique()) {
            sql.append("CREATE UNIQUE INDEX `");
        } else {
            sql.append("CREATE INDEX `");
        }
        
        sql.append(index.getIndexName()).append("` ON `").append(tableName).append("` (");
        
        // 索引字段
        if (index.getColumns() != null && !index.getColumns().isEmpty()) {
            sql.append(String.join(", ", index.getColumns().stream()
                .map(c -> "`" + c + "`")
                .collect(java.util.stream.Collectors.toList())));
        }
        
        sql.append(");\n");
        return sql.toString();
    }
    
    /**
     * 导出差异为简单Excel格式
     */
    private byte[] exportDiffAsSimpleExcel(SchemaDiff diff) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("差异列表");
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 创建表头 - 增加12个新列
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "变更类型", "表名", "字段名", "严重程度", "详情",
                "数据类型(旧)", "数据类型(新)", "数据类型是否发生变化",
                "长度(旧)", "长度(新)", "长度是否发生变化",
                "精度(旧)", "精度(新)", "精度是否发生变化",
                "字段注释(旧)", "字段注释(新)", "字段注释是否发生变化"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            if (diff.getChanges() != null) {
                for (int i = 0; i < diff.getChanges().size(); i++) {
                    SchemaChange change = diff.getChanges().get(i);
                    Row row = sheet.createRow(i + 1);
                    
                    // 基础列
                    row.createCell(0).setCellValue(getChangeTypeLabel(change.getChangeType()));
                    row.createCell(1).setCellValue(change.getTableName() != null ? change.getTableName() : "");
                    row.createCell(2).setCellValue(change.getColumnName() != null ? change.getColumnName() : "");
                    row.createCell(3).setCellValue(getSeverityLabel(change.getSeverity()));
                    row.createCell(4).setCellValue(formatChangeDetails(change.getDetails()));
                    
                    // 新增12个列
                    // 数据类型
                    row.createCell(5).setCellValue(change.getOldDataType() != null ? change.getOldDataType() : "");
                    row.createCell(6).setCellValue(change.getNewDataType() != null ? change.getNewDataType() : "");
                    row.createCell(7).setCellValue(change.getOldDataType() != null && change.getNewDataType() != null 
                            && !change.getOldDataType().equals(change.getNewDataType()) ? "是" : "否");
                    
                    // 长度
                    row.createCell(8).setCellValue(change.getOldLength() != null ? change.getOldLength().toString() : "");
                    row.createCell(9).setCellValue(change.getNewLength() != null ? change.getNewLength().toString() : "");
                    row.createCell(10).setCellValue(change.getOldLength() != null && change.getNewLength() != null 
                            && !change.getOldLength().equals(change.getNewLength()) ? "是" : "否");
                    
                    // 精度
                    row.createCell(11).setCellValue(change.getOldPrecision() != null ? change.getOldPrecision().toString() : "");
                    row.createCell(12).setCellValue(change.getNewPrecision() != null ? change.getNewPrecision().toString() : "");
                    row.createCell(13).setCellValue(change.getOldPrecision() != null && change.getNewPrecision() != null 
                            && !change.getOldPrecision().equals(change.getNewPrecision()) ? "是" : "否");
                    
                    // 字段注释
                    row.createCell(14).setCellValue(change.getOldComment() != null ? change.getOldComment() : "");
                    row.createCell(15).setCellValue(change.getNewComment() != null ? change.getNewComment() : "");
                    row.createCell(16).setCellValue(change.getOldComment() != null && change.getNewComment() != null 
                            && !change.getOldComment().equals(change.getNewComment()) ? "是" : "否");
                }
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
            log.error("导出差异Excel失败", e);
            throw new RuntimeException("导出差异Excel失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取变更类型标签
     */
    private String getChangeTypeLabel(com.schemasync.model.diff.ChangeType type) {
        if (type == null) return "";
        switch (type) {
            case TABLE_ADD: return "新增表";
            case TABLE_DROP: return "删除表";
            case TABLE_MODIFY: return "修改表";
            case COLUMN_ADD: return "新增字段";
            case COLUMN_DROP: return "删除字段";
            case COLUMN_MODIFY: return "修改字段";
            case INDEX_ADD: return "新增索引";
            case INDEX_DROP: return "删除索引";
            case INDEX_MODIFY: return "修改索引";
            case FOREIGN_KEY_ADD: return "新增外键";
            case FOREIGN_KEY_DROP: return "删除外键";
            case FOREIGN_KEY_MODIFY: return "修改外键";
            default: return type.name();
        }
    }
    
    /**
     * 获取严重程度标签
     */
    private String getSeverityLabel(com.schemasync.model.diff.Severity severity) {
        if (severity == null) return "";
        if (severity == com.schemasync.model.diff.Severity.BREAKING) return "高";
        if (severity == com.schemasync.model.diff.Severity.NON_BREAKING) return "低";
        return severity.name();
    }
    
    /**
     * 格式化ColumnDefinition对象
     */
    private String formatColumnDefinition(com.schemasync.model.dict.ColumnDefinition col) {
        StringBuilder sb = new StringBuilder();
        sb.append("字段: ").append(col.getColumnName());
        sb.append(", 类型: ").append(col.getDataType());
        if (col.getLength() != null && col.getLength() > 0) {
            sb.append("(").append(col.getLength()).append(")");
        }
        if (col.getPrecision() != null && col.getScale() != null) {
            sb.append("(").append(col.getPrecision()).append(",").append(col.getScale()).append(")");
        }
        if (col.getNullable() != null && !col.getNullable()) {
            sb.append(", 非空");
        }
        if (col.getDefaultValue() != null) {
            sb.append(", 默认值: ").append(col.getDefaultValue());
        }
        if (col.getComment() != null && !col.getComment().isEmpty()) {
            sb.append(", 注释: ").append(col.getComment());
        }
        return sb.toString();
    }
    
    /**
     * 格式化IndexDefinition对象
     */
    private String formatIndexDefinition(com.schemasync.model.dict.IndexDefinition idx) {
        StringBuilder sb = new StringBuilder();
        sb.append("索引: ").append(idx.getIndexName());
        
        if (idx.getIndexType() != null) {
            sb.append(", 类型: ").append(idx.getIndexType());
        }
        
        if (idx.getIsUnique() != null && idx.getIsUnique()) {
            sb.append(", 唯一");
        }
        
        if (idx.getColumns() != null && !idx.getColumns().isEmpty()) {
            sb.append(", 字段: ").append(String.join(", ", idx.getColumns()));
        }
        
        return sb.toString();
    }
    private String formatChangeDetails(Object details) {
        if (details == null) return "";
        if (details instanceof String) return (String) details;
        
        // 处理ColumnDefinition对象
        if (details instanceof com.schemasync.model.dict.ColumnDefinition) {
            return formatColumnDefinition((com.schemasync.model.dict.ColumnDefinition) details);
        }
        
        // 处理IndexDefinition对象
        if (details instanceof com.schemasync.model.dict.IndexDefinition) {
            return formatIndexDefinition((com.schemasync.model.dict.IndexDefinition) details);
        }
        
        // 处理Map类型
        if (details instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) details;
            StringBuilder sb = new StringBuilder();
            
            // 处理删除字段的情况（包含oldDefinition）
            if (map.containsKey("oldDefinition")) {
                Object oldDef = map.get("oldDefinition");
                if (oldDef instanceof com.schemasync.model.dict.ColumnDefinition) {
                    return formatColumnDefinition((com.schemasync.model.dict.ColumnDefinition) oldDef);
                }
            }
            
            // 处理修改索引的情况（包含oldValue/newValue，且为IndexDefinition）
            if (map.containsKey("oldValue") && map.containsKey("newValue")) {
                Object oldValue = map.get("oldValue");
                Object newValue = map.get("newValue");
                
                // 如果是索引变更
                if (oldValue instanceof com.schemasync.model.dict.IndexDefinition && 
                    newValue instanceof com.schemasync.model.dict.IndexDefinition) {
                    com.schemasync.model.dict.IndexDefinition oldIdx = (com.schemasync.model.dict.IndexDefinition) oldValue;
                    com.schemasync.model.dict.IndexDefinition newIdx = (com.schemasync.model.dict.IndexDefinition) newValue;
                    
                    sb.append("索引: ").append(map.containsKey("indexName") ? map.get("indexName") : oldIdx.getIndexName());
                    
                    // 对比索引类型
                    if (!java.util.Objects.equals(oldIdx.getIndexType(), newIdx.getIndexType())) {
                        sb.append(", 类型: ").append(oldIdx.getIndexType()).append(" -> ").append(newIdx.getIndexType());
                    }
                    
                    // 对比唯一性
                    if (!java.util.Objects.equals(oldIdx.getIsUnique(), newIdx.getIsUnique())) {
                        sb.append(", 唯一性: ").append(oldIdx.getIsUnique()).append(" -> ").append(newIdx.getIsUnique());
                    }
                    
                    // 对比字段列表
                    if (!java.util.Objects.equals(oldIdx.getColumns(), newIdx.getColumns())) {
                        sb.append(", 字段: ");
                        if (oldIdx.getColumns() != null) {
                            sb.append(String.join(", ", oldIdx.getColumns()));
                        }
                        sb.append(" -> ");
                        if (newIdx.getColumns() != null) {
                            sb.append(String.join(", ", newIdx.getColumns()));
                        }
                    }
                    return sb.toString();
                }
                
                // 处理修改字段的情况（包含property/impact）
                sb.append("从: ").append(map.get("oldValue"));
                sb.append(" 改为: ").append(map.get("newValue"));
                if (map.containsKey("property")) {
                    sb.append(", 属性: ").append(map.get("property"));
                }
                if (map.containsKey("impact")) {
                    sb.append(", 影响: ").append(map.get("impact"));
                }
                return sb.toString();
            } else if (map.containsKey("tableComment")) {
                // 表级别信息
                if (map.containsKey("columns")) {
                    sb.append("字段数: ").append(map.get("columns"));
                }
                if (map.containsKey("indexes")) {
                    sb.append(", 索引数: ").append(map.get("indexes"));
                }
                return sb.toString();
            } else {
                return map.toString();
            }
        }
        
        return details.toString();
    }
    
    /**
     * 将差异结果转换为SchemaDictionary(用于Excel导出)
     */
    private SchemaDictionary convertDiffToDictionary(SchemaDiff diff) {
        SchemaDictionary dictionary = new SchemaDictionary();
        
        // 从变更中提取表信息
        List<TableDefinition> tables = new ArrayList<>();
        
        if (diff.getChanges() != null) {
            // 按表名分组变更
            Map<String, List<SchemaChange>> changesByTable = diff.getChanges().stream()
                .collect(Collectors.groupingBy(SchemaChange::getTableName));
            
            // 为每个有变更的表创建表定义
            for (Map.Entry<String, List<SchemaChange>> entry : changesByTable.entrySet()) {
                String tableName = entry.getKey();
                List<SchemaChange> tableChanges = entry.getValue();
                
                TableDefinition tableDef = new TableDefinition();
                tableDef.setTableName(tableName);
                
                // 构建变更信息作为注释
                StringBuilder comment = new StringBuilder();
                comment.append("变更统计: ");
                long tableAdds = tableChanges.stream().filter(c -> c.getChangeType() == com.schemasync.model.diff.ChangeType.TABLE_ADD).count();
                long columnAdds = tableChanges.stream().filter(c -> c.getChangeType() == com.schemasync.model.diff.ChangeType.COLUMN_ADD).count();
                long columnMods = tableChanges.stream().filter(c -> c.getChangeType() == com.schemasync.model.diff.ChangeType.COLUMN_MODIFY).count();
                long columnDrops = tableChanges.stream().filter(c -> c.getChangeType() == com.schemasync.model.diff.ChangeType.COLUMN_DROP).count();
                
                if (tableAdds > 0) comment.append("新增表, ");
                if (columnAdds > 0) comment.append("新增").append(columnAdds).append("字段, ");
                if (columnMods > 0) comment.append("修改").append(columnMods).append("字段, ");
                if (columnDrops > 0) comment.append("删除").append(columnDrops).append("字段, ");
                
                tableDef.setTableComment(comment.toString());
                tableDef.setTableType("DIFF_CHANGE");
                
                tables.add(tableDef);
            }
        }
        
        dictionary.setTables(tables);
        
        return dictionary;
    }
}
