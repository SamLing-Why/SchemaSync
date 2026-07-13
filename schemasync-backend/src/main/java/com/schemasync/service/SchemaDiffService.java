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
     * 对比两个数据字典文件并格式化结果（用于导出）
     * 
     * @param oldFile 旧版本文件
     * @param newFile 新版本文件
     * @param format 输出格式
     * @return 格式化后的字节数组
     */
    public byte[] compareAndFormat(MultipartFile oldFile, MultipartFile newFile, String format, String databaseType) {
        try {
            log.info("开始对比文件并格式化: {} vs {}", oldFile.getOriginalFilename(), newFile.getOriginalFilename());

            // 1. 解析旧版本
            SchemaDictionary oldDict = parseFile(oldFile);
            log.debug("解析旧版本成功, 表数量: {}", 
                    oldDict.getTables() != null ? oldDict.getTables().size() : 0);

            // 2. 解析新版本
            SchemaDictionary newDict = parseFile(newFile);
            log.debug("解析新版本成功, 表数量: {}", 
                    newDict.getTables() != null ? newDict.getTables().size() : 0);

            // 3. 执行对比
            SchemaDiff diff = schemaDiffer.compare(oldDict, newDict);
            log.info("对比完成, 发现{}处变更", diff.getChanges().size());
            
            // 4. 格式化结果（传递newDict和databaseType用于生成DDL）
            byte[] data = formatDiff(diff, format, newDict, databaseType);
            
            log.info("格式化完成, 输出格式: {}, 大小: {} bytes", format, data.length);
            return data;

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
        return formatDiff(diff, format, null, "mysql");
    }
    
    /**
     * 格式化差异结果为指定格式（带新字典和数据库类型）
     */
    public byte[] formatDiff(SchemaDiff diff, String format, SchemaDictionary newDict, String databaseType) {
        if ("excel".equalsIgnoreCase(format)) {
            // 使用简单表格格式导出差异列表
            return exportDiffAsSimpleExcel(diff, newDict, databaseType);
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
     * 
     * @param oldFile 旧版本文件
     * @param newFile 新版本文件
     * @param databaseType 数据库类型(mysql/gaussdb_mysql/gaussdb_oracle)
     * @return DDL SQL字节数组
     */
    public byte[] generateDdlFromDiff(MultipartFile oldFile, MultipartFile newFile, String databaseType) {
        try {
            // 1. 解析两个版本
            SchemaDictionary oldDict = parseFile(oldFile);
            SchemaDictionary newDict = parseFile(newFile);
            
            // 2. 执行对比
            SchemaDiff diff = schemaDiffer.compare(oldDict, newDict);
            
            // 3. 根据数据库类型生成DDL
            String ddl = generateDdlForChangedTables(newDict, oldDict, diff, databaseType);
            
            return ddl.getBytes("UTF-8");
        } catch (Exception e) {
            log.error("生成DDL失败", e);
            throw new RuntimeException("生成DDL失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从对比结果生成DDL脚本（差异化，兼容旧接口，默认MySQL）
     */
    public byte[] generateDdlFromDiff(MultipartFile oldFile, MultipartFile newFile) {
        return generateDdlFromDiff(oldFile, newFile, "mysql");
    }
    
    /**
     * 为变更的表生成DDL（根据变更类型生成精确DDL）- 旧版本兼容
     */
    private String generateDdlForChangedTables(SchemaDictionary newDict, SchemaDiff diff) {
        return generateDdlForChangedTables(newDict, null, diff, "mysql");
    }
    
    /**
     * 为变更的表生成DDL（根据变更类型生成精确DDL）- 支持数据库类型
     */
    private String generateDdlForChangedTables(SchemaDictionary newDict, SchemaDictionary oldDict, 
                                                SchemaDiff diff, String databaseType) {
        // 根据数据库类型选择生成策略
        String dbType = databaseType != null ? databaseType.toLowerCase() : "mysql";
        
        StringBuilder sql = new StringBuilder();
        String dbTypeLabel;
        switch (dbType) {
            case "gaussdb_mysql":
                dbTypeLabel = "GaussDB (MySQL兼容模式)";
                break;
            case "gaussdb_oracle":
                dbTypeLabel = "GaussDB (Oracle兼容模式)";
                break;
            case "gaussdb_pg":
                dbTypeLabel = "GaussDB (PG模式)";
                break;
            default:
                dbTypeLabel = "MySQL";
                break;
        }
        
        sql.append("-- ============================================\n");
        sql.append("-- SchemaSync 差异化DDL脚本\n");
        sql.append("-- 数据库类型: ").append(dbTypeLabel).append("\n");
        sql.append("-- 生成时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("\n");
        sql.append("-- ============================================\n\n");
        
        if (diff.getChanges() == null || diff.getChanges().isEmpty()) {
            sql.append("-- 无变更\n");
            return sql.toString();
        }
        
        switch (dbType) {
            case "mysql":
            case "gaussdb_mysql":
                return generateMySqlStyleDiffDdl(sql, newDict, diff);
            case "gaussdb_oracle":
                return generateGaussDbOracleStyleDiffDdl(sql, newDict, diff);
            case "gaussdb_pg":
                return generateGaussDbPgStyleDiffDdl(sql, newDict, diff);
            default:
                return generateMySqlStyleDiffDdl(sql, newDict, diff);
        }
    }
    
    /**
     * 为单个变更生成DDL（统一方法，供Excel和文件使用）
     * 
     * @param change 变更信息
     * @param newTable 新版本的表定义
     * @param databaseType 数据库类型
     * @return DDL语句
     */
    private String generateDdlForSingleChange(SchemaChange change, TableDefinition newTable, String databaseType) {
        String dbType = databaseType != null ? databaseType.toLowerCase() : "mysql";
        
        switch (dbType) {
            case "mysql":
            case "gaussdb_mysql":
                return generateMySqlStyleDdlForChange(change, newTable);
            case "gaussdb_oracle":
                return generateGaussDbOracleDdlForChange(change, newTable);
            case "gaussdb_pg":
                return generateGaussDbPgDdlForChange(change, newTable);
            default:
                return generateMySqlStyleDdlForChange(change, newTable);
        }
    }
    
    /**
     * 生成MySQL风格的单条DDL
     */
    private String generateMySqlStyleDdlForChange(SchemaChange change, TableDefinition newTable) {
        String tableName = change.getTableName();
        com.schemasync.model.diff.ChangeType changeType = change.getChangeType();
        
        if (changeType == com.schemasync.model.diff.ChangeType.TABLE_ADD) {
            if (newTable != null) {
                if ("VIEW".equalsIgnoreCase(newTable.getTableType())) {
                    return ddlGeneratorService.generateCreateView(newTable).trim();
                } else {
                    return ddlGeneratorService.generateCreateTable(newTable).trim();
                }
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.TABLE_DROP) {
            return "-- DROP TABLE `" + tableName + "`; -- 已注释，请确认后手动执行";
        } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_ADD) {
            if (newTable != null && change.getColumnName() != null) {
                ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                if (newColumn != null) {
                    return generateAddColumnSql(tableName, newColumn).trim();
                }
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_DROP) {
            if (change.getColumnName() != null) {
                return "-- ALTER TABLE `" + tableName + "` DROP COLUMN `" + change.getColumnName() + "`; -- 已注释，请确认后手动执行";
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_MODIFY) {
            if (newTable != null && change.getColumnName() != null) {
                ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                if (newColumn != null) {
                    return generateModifyColumnSql(tableName, newColumn).trim();
                }
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_ADD) {
            // 新增索引：从details中获取IndexDefinition
            if (change.getDetails() instanceof com.schemasync.model.dict.IndexDefinition) {
                com.schemasync.model.dict.IndexDefinition index = (com.schemasync.model.dict.IndexDefinition) change.getDetails();
                return generateCreateIndexSql(tableName, index).trim();
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_DROP) {
            // 删除索引：从 details中获取IndexDefinition
            if (change.getDetails() instanceof com.schemasync.model.dict.IndexDefinition) {
                com.schemasync.model.dict.IndexDefinition index = (com.schemasync.model.dict.IndexDefinition) change.getDetails();
                return ("ALTER TABLE `" + tableName + "` DROP INDEX `" + index.getIndexName() + "`;").trim();
            }
            return "-- ALTER TABLE `" + tableName + "` DROP INDEX ...; -- 已注释，请确认后手动执行";
        } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_MODIFY) {
            // 修改索引：先删除旧索引，再创建新索引
            if (change.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) change.getDetails();
                String indexName = (String) details.get("indexName");
                Object newValue = details.get("newValue");
                
                StringBuilder ddl = new StringBuilder();
                ddl.append("-- 修改索引: 先删除旧索引，再创建新索引\n");
                ddl.append("ALTER TABLE `" + tableName + "` DROP INDEX `" + indexName + "`;");
                
                if (newValue instanceof com.schemasync.model.dict.IndexDefinition) {
                    com.schemasync.model.dict.IndexDefinition newIndex = (com.schemasync.model.dict.IndexDefinition) newValue;
                    ddl.append("\n").append(generateCreateIndexSql(tableName, newIndex).trim());
                }
                return ddl.toString().trim();
            }
            return "-- 修改索引: " + tableName + " -- 请手动处理";
        } else if (changeType == com.schemasync.model.diff.ChangeType.TABLE_MODIFY) {
            return "-- 表属性变更，请手动处理: " + (change.getDetails() != null ? change.getDetails().toString() : "");
        }
        
        return "";
    }
    
    /**
     * 生成GaussDB Oracle风格的单条DDL
     */
    private String generateGaussDbOracleDdlForChange(SchemaChange change, TableDefinition newTable) {
        String tableName = change.getTableName();
        com.schemasync.model.diff.ChangeType changeType = change.getChangeType();
        
        if (changeType == com.schemasync.model.diff.ChangeType.TABLE_ADD) {
            if (newTable != null && !"VIEW".equalsIgnoreCase(newTable.getTableType())) {
                return generateGaussDbOracleCreateTableForDiff(newTable).trim();
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.TABLE_DROP) {
            return "-- DROP TABLE " + tableName + "; -- 已注释，请确认后手动执行";
        } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_ADD) {
            if (newTable != null && change.getColumnName() != null) {
                ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                if (newColumn != null) {
                    return generateGaussDbOracleAddColumnSql(tableName, newColumn).trim();
                }
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_DROP) {
            if (change.getColumnName() != null) {
                return "-- ALTER TABLE " + tableName + " DROP COLUMN " + change.getColumnName() + "; -- 已注释，请确认后手动执行";
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_MODIFY) {
            if (newTable != null && change.getColumnName() != null) {
                ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                if (newColumn != null) {
                    return generateGaussDbOracleModifyColumnSql(tableName, newColumn).trim();
                }
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_ADD) {
            // 新增索引（Oracle风格）
            if (change.getDetails() instanceof com.schemasync.model.dict.IndexDefinition) {
                com.schemasync.model.dict.IndexDefinition index = (com.schemasync.model.dict.IndexDefinition) change.getDetails();
                return generateGaussDbOracleCreateIndexSql(tableName, index).trim();
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_DROP) {
            // 删除索引（Oracle风格）
            if (change.getDetails() instanceof com.schemasync.model.dict.IndexDefinition) {
                com.schemasync.model.dict.IndexDefinition index = (com.schemasync.model.dict.IndexDefinition) change.getDetails();
                return ("DROP INDEX " + index.getIndexName() + ";").trim();
            }
            return "-- DROP INDEX ...; -- 已注释，请确认后手动执行";
        } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_MODIFY) {
            // 修改索引：先删除旧索引，再创建新索引（Oracle风格）
            if (change.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) change.getDetails();
                String indexName = (String) details.get("indexName");
                Object newValue = details.get("newValue");
                
                StringBuilder ddl = new StringBuilder();
                ddl.append("-- 修改索引: 先删除旧索引，再创建新索引\n");
                ddl.append("DROP INDEX " + indexName + ";");
                
                if (newValue instanceof com.schemasync.model.dict.IndexDefinition) {
                    com.schemasync.model.dict.IndexDefinition newIndex = (com.schemasync.model.dict.IndexDefinition) newValue;
                    ddl.append("\n").append(generateGaussDbOracleCreateIndexSql(tableName, newIndex).trim());
                }
                return ddl.toString().trim();
            }
            return "-- 修改索引: " + tableName + " -- 请手动处理";
        }
        
        return "";
    }
    
    /**
     * 生成MySQL风格的差异化DDL
     */
    private String generateMySqlStyleDiffDdl(StringBuilder sql, SchemaDictionary newDict, SchemaDiff diff) {
        Map<String, List<SchemaChange>> changesByTable = diff.getChanges().stream()
            .collect(Collectors.groupingBy(SchemaChange::getTableName));
        
        for (Map.Entry<String, List<SchemaChange>> entry : changesByTable.entrySet()) {
            String tableName = entry.getKey();
            List<SchemaChange> tableChanges = entry.getValue();
            TableDefinition newTable = findTableByName(newDict, tableName);
            
            for (SchemaChange change : tableChanges) {
                com.schemasync.model.diff.ChangeType changeType = change.getChangeType();
                if (changeType == com.schemasync.model.diff.ChangeType.TABLE_ADD) {
                    sql.append("-- 变更类型: 新增表\n");
                    if (newTable != null) {
                        if ("VIEW".equalsIgnoreCase(newTable.getTableType())) {
                            sql.append(ddlGeneratorService.generateCreateView(newTable));
                        } else {
                            sql.append(ddlGeneratorService.generateCreateTable(newTable));
                        }
                    }
                    sql.append("\n\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_ADD) {
                    sql.append("-- 变更类型: 新增字段\n");
                    if (newTable != null && change.getColumnName() != null) {
                        ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                        if (newColumn != null) {
                            sql.append(generateAddColumnSql(tableName, newColumn));
                        }
                    }
                    sql.append("\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_MODIFY) {
                    sql.append("-- 变更类型: 修改字段\n");
                    if (newTable != null && change.getColumnName() != null) {
                        ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                        if (newColumn != null) {
                            sql.append(generateModifyColumnSql(tableName, newColumn));
                        }
                    }
                    sql.append("\n");
                }
            }
        }
        
        return sql.toString();
    }
    
    /**
     * 生成GaussDB Oracle风格的差异化DDL
     */
    private String generateGaussDbOracleStyleDiffDdl(StringBuilder sql, SchemaDictionary newDict, SchemaDiff diff) {
        Map<String, List<SchemaChange>> changesByTable = diff.getChanges().stream()
            .collect(Collectors.groupingBy(SchemaChange::getTableName));
        
        for (Map.Entry<String, List<SchemaChange>> entry : changesByTable.entrySet()) {
            String tableName = entry.getKey();
            List<SchemaChange> tableChanges = entry.getValue();
            TableDefinition newTable = findTableByName(newDict, tableName);
            
            for (SchemaChange change : tableChanges) {
                com.schemasync.model.diff.ChangeType changeType = change.getChangeType();
                if (changeType == com.schemasync.model.diff.ChangeType.TABLE_ADD) {
                    sql.append("-- 变更类型: 新增表\n");
                    if (newTable != null && !"VIEW".equalsIgnoreCase(newTable.getTableType())) {
                        sql.append(generateGaussDbOracleCreateTableForDiff(newTable));
                    }
                    sql.append("\n\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_ADD) {
                    sql.append("-- 变更类型: 新增字段\n");
                    if (newTable != null && change.getColumnName() != null) {
                        ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                        if (newColumn != null) {
                            sql.append(generateGaussDbOracleAddColumnSql(tableName, newColumn));
                        }
                    }
                    sql.append("\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_MODIFY) {
                    sql.append("-- 变更类型: 修改字段\n");
                    if (newTable != null && change.getColumnName() != null) {
                        ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                        if (newColumn != null) {
                            sql.append(generateGaussDbOracleModifyColumnSql(tableName, newColumn));
                        }
                    }
                    sql.append("\n");
                }
            }
        }
        
        return sql.toString();
    }
    
    /**
     * 生成GaussDB Oracle风格的CREATE TABLE（用于差异化DDL）
     */
    private String generateGaussDbOracleCreateTableForDiff(TableDefinition table) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(table.getTableName()).append(" (\n");
        
        List<String> columnDefs = new ArrayList<>();
        if (table.getColumns() != null) {
            for (ColumnDefinition column : table.getColumns()) {
                columnDefs.add("  " + generateGaussDbOracleColumnDefForDiff(column));
            }
        }
        
        if (table.getColumns() != null) {
            List<String> pkColumns = table.getColumns().stream()
                .filter(c -> c.getIsPrimaryKey() != null && c.getIsPrimaryKey())
                .map(col -> col.getEffectiveName().toUpperCase())
                .collect(Collectors.toList());
            if (!pkColumns.isEmpty()) {
                columnDefs.add("  PRIMARY KEY (" + String.join(", ", pkColumns) + ")");
            }
        }
        
        sql.append(String.join(",\n", columnDefs));
        sql.append("\n);\n");
        
        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
            sql.append("COMMENT ON TABLE ").append(table.getTableName())
               .append(" IS '").append(table.getTableComment().replace("'", "\\'")).append("';\n");
        }
        
        return sql.toString();
    }
    
    /**
     * 生成GaussDB Oracle风格的字段定义（用于差异化DDL）
     */
    private String generateGaussDbOracleColumnDefForDiff(ColumnDefinition column) {
        StringBuilder def = new StringBuilder();
        def.append(column.getEffectiveName().toUpperCase()).append(" ");
        
        String dataType = column.getDataType();
        if (dataType != null) {
            def.append(convertToOracleTypeForDiff(dataType));
        } else {
            def.append("VARCHAR2");
        }
        
        if (column.getPrecision() != null && column.getScale() != null) {
            def.append("(").append(column.getPrecision()).append(",").append(column.getScale()).append(")");
        } else if (column.getLength() != null && column.getLength() > 0) {
            def.append("(").append(column.getLength()).append(")");
        }
        
        if (column.getNullable() != null && !column.getNullable()) {
            def.append(" NOT NULL");
        }
        
        if (column.getDefaultValue() != null) {
            String defaultValue = column.getDefaultValue().toString();
            if (!defaultValue.toLowerCase().equals("null")) {
                if (isNumericTypeForDiff(dataType)) {
                    def.append(" DEFAULT ").append(defaultValue);
                } else {
                    def.append(" DEFAULT '").append(defaultValue.replace("'", "\\'")).append("'");
                }
            }
        }
        
        return def.toString();
    }
    
    /**
     * GaussDB Oracle风格：新增字段
     */
    private String generateGaussDbOracleAddColumnSql(String tableName, ColumnDefinition column) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(tableName).append(" ADD (");
        sql.append(generateGaussDbOracleColumnDefForDiff(column));
        sql.append(");\n");
        return sql.toString();
    }
    
    /**
     * GaussDB Oracle风格：修改字段
     */
    private String generateGaussDbOracleModifyColumnSql(String tableName, ColumnDefinition column) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(tableName).append(" MODIFY (");
        sql.append(generateGaussDbOracleColumnDefForDiff(column));
        sql.append(");\n");
        return sql.toString();
    }
    
    /**
     * 将MySQL类型转换为Oracle类型（用于差异化DDL）
     */
    private String convertToOracleTypeForDiff(String mysqlType) {
        if (mysqlType == null) return "VARCHAR2";
        String upper = mysqlType.toUpperCase();
        switch (upper) {
            case "VARCHAR": case "VARCHAR2": case "NVARCHAR": case "NVARCHAR2":
                return "VARCHAR2";
            case "TEXT": case "LONGTEXT": case "MEDIUMTEXT":
                return "CLOB";
            case "INT": case "INTEGER": case "TINYINT": case "SMALLINT": case "MEDIUMINT":
                return "NUMBER";
            case "BIGINT": return "NUMBER(19)";
            case "FLOAT": return "FLOAT";
            case "DOUBLE": return "DOUBLE PRECISION";
            case "DECIMAL": case "NUMERIC": case "NUMBER":
                return "NUMBER";
            case "DATETIME": case "TIMESTAMP":
                return "TIMESTAMP";
            case "DATE": return "DATE";
            case "BLOB": case "LONGBLOB": case "MEDIUMBLOB": case "TINYBLOB":
                return "BLOB";
            case "BOOLEAN": case "BOOL": case "BIT":
                return "NUMBER(1)";
            default:
                return upper;
        }
    }
    
    /**
     * 判断是否为数值类型（用于差异化DDL）
     */
    private boolean isNumericTypeForDiff(String dataType) {
        if (dataType == null) return false;
        String upper = dataType.toUpperCase();
        return upper.equals("INT") || upper.equals("INTEGER") || 
               upper.equals("BIGINT") || upper.equals("SMALLINT") ||
               upper.equals("TINYINT") || upper.equals("DECIMAL") ||
               upper.equals("NUMERIC") || upper.equals("FLOAT") ||
               upper.equals("DOUBLE") || upper.startsWith("NUMBER");
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
        // 使用getEffectiveName()获取实际字段名（支持重命名）
        sql.append(column.getEffectiveName()).append("` ");
        
        // 数据类型
        String dataType = column.getDataType().toUpperCase();
        sql.append(dataType);
        
        // 添加长度/精度（仅对需要的类型）
        if (!isTypeWithoutLengthForDiff(dataType)) {
            if (column.getPrecision() != null && column.getScale() != null) {
                sql.append("(").append(column.getPrecision()).append(",").append(column.getScale()).append(")");
            } else if (column.getLength() != null && column.getLength() > 0) {
                sql.append("(").append(column.getLength()).append(")");
            }
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
        // 使用getEffectiveName()获取实际字段名（支持重命名）
        sql.append(column.getEffectiveName()).append("` ");
        
        // 数据类型
        String dataType = column.getDataType().toUpperCase();
        sql.append(dataType);
        
        // 添加长度/精度（仅对需要的类型）
        if (!isTypeWithoutLengthForDiff(dataType)) {
            if (column.getPrecision() != null && column.getScale() != null) {
                sql.append("(").append(column.getPrecision()).append(",").append(column.getScale()).append(")");
            } else if (column.getLength() != null && column.getLength() > 0) {
                sql.append("(").append(column.getLength()).append(")");
            }
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
     * 生成GaussDB Oracle风格的创建索引SQL
     */
    private String generateGaussDbOracleCreateIndexSql(String tableName, com.schemasync.model.dict.IndexDefinition index) {
        StringBuilder sql = new StringBuilder();
        
        // 判断是否唯一索引
        if (index.getIsUnique() != null && index.getIsUnique()) {
            sql.append("CREATE UNIQUE INDEX ");
        } else {
            sql.append("CREATE INDEX ");
        }
        
        // Oracle风格：索引名大写，不使用反引号
        sql.append(index.getIndexName().toUpperCase()).append(" ON ").append(tableName.toUpperCase()).append(" (");
        
        // 索引字段（大写）
        if (index.getColumns() != null && !index.getColumns().isEmpty()) {
            sql.append(String.join(", ", index.getColumns().stream()
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toList())));
        }
        
        sql.append(");\n");
        return sql.toString();
    }
    
    /**
     * 导出差异为简单Excel格式
     */
    private byte[] exportDiffAsSimpleExcel(SchemaDiff diff, SchemaDictionary newDict, String databaseType) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("差异列表");
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 创建表头 - DDL列放在详情列后面
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "变更类型", "表名", "字段名", "严重程度",
                "数据类型(旧)", "数据类型(新)", "数据类型是否发生变化",
                "长度(旧)", "长度(新)", "长度是否发生变化",
                "精度(旧)", "精度(新)", "精度是否发生变化",
                "字段注释(旧)", "字段注释(新)", "字段注释是否发生变化",
                "详情", "DDL语句"
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
                    
                    // 基础列（不含详情和DDL）
                    row.createCell(0).setCellValue(getChangeTypeLabel(change.getChangeType()));
                    row.createCell(1).setCellValue(change.getTableName() != null ? change.getTableName() : "");
                    row.createCell(2).setCellValue(change.getColumnName() != null ? change.getColumnName() : "");
                    row.createCell(3).setCellValue(getSeverityLabel(change.getSeverity()));
                    
                    // 数据类型（列4-6）
                    row.createCell(4).setCellValue(change.getOldDataType() != null ? change.getOldDataType() : "");
                    row.createCell(5).setCellValue(change.getNewDataType() != null ? change.getNewDataType() : "");
                    row.createCell(6).setCellValue(change.getOldDataType() != null && change.getNewDataType() != null 
                            && !change.getOldDataType().equals(change.getNewDataType()) ? "是" : "否");
                    
                    // 长度（列7-9）
                    row.createCell(7).setCellValue(change.getOldLength() != null ? change.getOldLength().toString() : "");
                    row.createCell(8).setCellValue(change.getNewLength() != null ? change.getNewLength().toString() : "");
                    row.createCell(9).setCellValue(change.getOldLength() != null && change.getNewLength() != null 
                            && !change.getOldLength().equals(change.getNewLength()) ? "是" : "否");
                    
                    // 精度（列10-12）
                    row.createCell(10).setCellValue(change.getOldPrecision() != null ? change.getOldPrecision().toString() : "");
                    row.createCell(11).setCellValue(change.getNewPrecision() != null ? change.getNewPrecision().toString() : "");
                    row.createCell(12).setCellValue(change.getOldPrecision() != null && change.getNewPrecision() != null 
                            && !change.getOldPrecision().equals(change.getNewPrecision()) ? "是" : "否");
                    
                    // 字段注释（列13-15）
                    row.createCell(13).setCellValue(change.getOldComment() != null ? change.getOldComment() : "");
                    row.createCell(14).setCellValue(change.getNewComment() != null ? change.getNewComment() : "");
                    row.createCell(15).setCellValue(change.getOldComment() != null && change.getNewComment() != null 
                            && !change.getOldComment().equals(change.getNewComment()) ? "是" : "否");
                    
                    // 详情列（列16）
                    row.createCell(16).setCellValue(formatChangeDetails(change.getDetails()));
                    
                    // DDL列（列17）- 使用统一的DDL生成方法，根据数据库类型生成对应风格的DDL
                    TableDefinition tableDef = newDict != null ? findTableByName(newDict, change.getTableName()) : null;
                    String ddl = generateDdlForSingleChange(change, tableDef, databaseType);
                    row.createCell(17).setCellValue(ddl);
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
    
    /**
     * 判断是否为不需要指定长度的类型（用于差异化DDL）
     */
    private boolean isTypeWithoutLengthForDiff(String dataType) {
        if (dataType == null) return false;
        String upper = dataType.toUpperCase();
        
        // TEXT系列
        if (upper.equals("TEXT") || upper.equals("TINYTEXT") || 
            upper.equals("MEDIUMTEXT") || upper.equals("LONGTEXT")) {
            return true;
        }
        
        // BLOB系列
        if (upper.equals("BLOB") || upper.equals("TINYBLOB") || 
            upper.equals("MEDIUMBLOB") || upper.equals("LONGBLOB")) {
            return true;
        }
        
        // JSON类型
        if (upper.equals("JSON")) {
            return true;
        }
        
        // 空间数据类型
        if (upper.equals("GEOMETRY") || upper.equals("POINT") || 
            upper.equals("LINESTRING") || upper.equals("POLYGON") ||
            upper.equals("MULTIPOINT") || upper.equals("MULTILINESTRING") ||
            upper.equals("MULTIPOLYGON") || upper.equals("GEOMETRYCOLLECTION")) {
            return true;
        }
        
        // ENUM和SET
        if (upper.equals("ENUM") || upper.equals("SET")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 生成GaussDB PG风格的差异化DDL
     */
    private String generateGaussDbPgStyleDiffDdl(StringBuilder sql, SchemaDictionary newDict, SchemaDiff diff) {
        Map<String, List<SchemaChange>> changesByTable = diff.getChanges().stream()
            .collect(Collectors.groupingBy(SchemaChange::getTableName));
        
        for (Map.Entry<String, List<SchemaChange>> entry : changesByTable.entrySet()) {
            String tableName = entry.getKey();
            List<SchemaChange> tableChanges = entry.getValue();
            TableDefinition newTable = findTableByName(newDict, tableName);
            
            for (SchemaChange change : tableChanges) {
                com.schemasync.model.diff.ChangeType changeType = change.getChangeType();
                if (changeType == com.schemasync.model.diff.ChangeType.TABLE_ADD) {
                    sql.append("-- 变更类型: 新增表\n");
                    if (newTable != null && !"VIEW".equalsIgnoreCase(newTable.getTableType())) {
                        sql.append(generateGaussDbPgCreateTableForDiff(newTable));
                    }
                    sql.append("\n\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_ADD) {
                    sql.append("-- 变更类型: 新增字段\n");
                    if (newTable != null && change.getColumnName() != null) {
                        ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                        if (newColumn != null) {
                            sql.append(generateGaussDbPgAddColumnSql(tableName, newColumn));
                        }
                    }
                    sql.append("\n");
                } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_MODIFY) {
                    sql.append("-- 变更类型: 修改字段\n");
                    if (newTable != null && change.getColumnName() != null) {
                        ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                        if (newColumn != null) {
                            sql.append(generateGaussDbPgModifyColumnSql(tableName, newColumn));
                        }
                    }
                    sql.append("\n");
                }
            }
        }
        
        return sql.toString();
    }
    
    /**
     * 生成GaussDB PG风格的单条DDL
     */
    private String generateGaussDbPgDdlForChange(SchemaChange change, TableDefinition newTable) {
        String tableName = change.getTableName();
        com.schemasync.model.diff.ChangeType changeType = change.getChangeType();
        
        if (changeType == com.schemasync.model.diff.ChangeType.TABLE_ADD) {
            if (newTable != null && !"VIEW".equalsIgnoreCase(newTable.getTableType())) {
                return generateGaussDbPgCreateTableForDiff(newTable).trim();
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.TABLE_DROP) {
            return "-- DROP TABLE " + tableName + "; -- 已注释，请确认后手动执行";
        } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_ADD) {
            if (newTable != null && change.getColumnName() != null) {
                ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                if (newColumn != null) {
                    return generateGaussDbPgAddColumnSql(tableName, newColumn).trim();
                }
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_DROP) {
            if (change.getColumnName() != null) {
                return "-- ALTER TABLE " + tableName + " DROP COLUMN " + change.getColumnName() + "; -- 已注释，请确认后手动执行";
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.COLUMN_MODIFY) {
            if (newTable != null && change.getColumnName() != null) {
                ColumnDefinition newColumn = findColumnByName(newTable, change.getColumnName());
                if (newColumn != null) {
                    return generateGaussDbPgModifyColumnSql(tableName, newColumn).trim();
                }
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_ADD) {
            // 新增索引（PG风格）
            if (change.getDetails() instanceof com.schemasync.model.dict.IndexDefinition) {
                com.schemasync.model.dict.IndexDefinition index = (com.schemasync.model.dict.IndexDefinition) change.getDetails();
                return generateGaussDbPgCreateIndexSql(tableName, index).trim();
            }
            return "";
        } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_DROP) {
            // 删除索引（PG风格）
            if (change.getDetails() instanceof com.schemasync.model.dict.IndexDefinition) {
                com.schemasync.model.dict.IndexDefinition index = (com.schemasync.model.dict.IndexDefinition) change.getDetails();
                return ("DROP INDEX " + index.getIndexName() + ";").trim();
            }
            return "-- DROP INDEX ...; -- 已注释，请确认后手动执行";
        } else if (changeType == com.schemasync.model.diff.ChangeType.INDEX_MODIFY) {
            // 修改索引：先删除旧索引，再创建新索引（PG风格）
            if (change.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) change.getDetails();
                String indexName = (String) details.get("indexName");
                Object newValue = details.get("newValue");
                
                StringBuilder ddl = new StringBuilder();
                ddl.append("-- 修改索引: 先删除旧索引，再创建新索引\n");
                ddl.append("DROP INDEX " + indexName + ";");
                
                if (newValue instanceof com.schemasync.model.dict.IndexDefinition) {
                    com.schemasync.model.dict.IndexDefinition newIndex = (com.schemasync.model.dict.IndexDefinition) newValue;
                    ddl.append("\n").append(generateGaussDbPgCreateIndexSql(tableName, newIndex).trim());
                }
                return ddl.toString().trim();
            }
            return "-- 修改索引: " + tableName + " -- 请手动处理";
        }
        
        return "";
    }
    
    /**
     * 生成GaussDB PG风格的CREATE TABLE（用于差异化DDL）
     */
    private String generateGaussDbPgCreateTableForDiff(TableDefinition table) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(table.getTableName()).append(" (\n");
        
        List<String> columnDefs = new ArrayList<>();
        if (table.getColumns() != null) {
            for (ColumnDefinition column : table.getColumns()) {
                columnDefs.add("  " + generateGaussDbPgColumnDefForDiff(column));
            }
        }
        
        if (table.getColumns() != null) {
            List<String> pkColumns = table.getColumns().stream()
                .filter(c -> c.getIsPrimaryKey() != null && c.getIsPrimaryKey())
                .map(ColumnDefinition::getEffectiveName)
                .collect(Collectors.toList());
            if (!pkColumns.isEmpty()) {
                columnDefs.add("  PRIMARY KEY (" + String.join(", ", pkColumns) + ")");
            }
        }
        
        sql.append(String.join(",\n", columnDefs));
        sql.append("\n);\n");
        
        // 表注释
        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
            sql.append("COMMENT ON TABLE ").append(table.getTableName())
               .append(" IS '").append(table.getTableComment().replace("'", "''")).append("';\n");
        }
        
        // 字段注释
        if (table.getColumns() != null) {
            for (ColumnDefinition column : table.getColumns()) {
                if (column.getComment() != null && !column.getComment().isEmpty()) {
                    sql.append("COMMENT ON COLUMN ").append(table.getTableName())
                       .append(".").append(column.getEffectiveName())
                       .append(" IS '").append(column.getComment().replace("'", "''")).append("';\n");
                }
            }
        }
        
        return sql.toString();
    }
    
    /**
     * 生成GaussDB PG风格的字段定义（用于差异化DDL）
     */
    private String generateGaussDbPgColumnDefForDiff(ColumnDefinition column) {
        StringBuilder def = new StringBuilder();
        def.append(column.getEffectiveName()).append(" ");
        
        String dataType = column.getDataType();
        if (dataType != null) {
            def.append(convertToPgTypeForDiff(dataType));
        } else {
            def.append("VARCHAR");
        }
        
        if (column.getPrecision() != null && column.getScale() != null) {
            def.append("(").append(column.getPrecision()).append(",").append(column.getScale()).append(")");
        } else if (column.getLength() != null && column.getLength() > 0) {
            def.append("(").append(column.getLength()).append(")");
        }
        
        if (column.getNullable() != null && !column.getNullable()) {
            def.append(" NOT NULL");
        }
        
        if (column.getDefaultValue() != null) {
            String defaultValue = column.getDefaultValue().toString();
            if (!defaultValue.toLowerCase().equals("null")) {
                if (isNumericTypeForDiff(dataType)) {
                    def.append(" DEFAULT ").append(defaultValue);
                } else {
                    def.append(" DEFAULT '").append(defaultValue.replace("'", "''")).append("'");
                }
            }
        }
        
        return def.toString();
    }
    
    /**
     * GaussDB PG风格：新增字段
     */
    private String generateGaussDbPgAddColumnSql(String tableName, ColumnDefinition column) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(tableName).append(" ADD COLUMN ");
        sql.append(generateGaussDbPgColumnDefForDiff(column));
        sql.append(";\n");
        
        // 字段注释
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sql.append("COMMENT ON COLUMN ").append(tableName)
               .append(".").append(column.getEffectiveName())
               .append(" IS '").append(column.getComment().replace("'", "''")).append("';\n");
        }
        
        return sql.toString();
    }
    
    /**
     * GaussDB PG风格：修改字段
     * PG模式下修改字段需要分开处理类型、约束和注释
     */
    private String generateGaussDbPgModifyColumnSql(String tableName, ColumnDefinition column) {
        StringBuilder sql = new StringBuilder();
        
        // 修改数据类型
        sql.append("ALTER TABLE ").append(tableName).append(" ALTER COLUMN ")
           .append(column.getEffectiveName()).append(" TYPE ");
        
        String dataType = column.getDataType();
        if (dataType != null) {
            sql.append(convertToPgTypeForDiff(dataType));
        } else {
            sql.append("VARCHAR");
        }
        
        if (column.getPrecision() != null && column.getScale() != null) {
            sql.append("(").append(column.getPrecision()).append(",").append(column.getScale()).append(")");
        } else if (column.getLength() != null && column.getLength() > 0) {
            sql.append("(").append(column.getLength()).append(")");
        }
        sql.append(";\n");
        
        // 修改NOT NULL约束
        if (column.getNullable() != null) {
            if (!column.getNullable()) {
                sql.append("ALTER TABLE ").append(tableName).append(" ALTER COLUMN ")
                   .append(column.getEffectiveName()).append(" SET NOT NULL;\n");
            } else {
                sql.append("ALTER TABLE ").append(tableName).append(" ALTER COLUMN ")
                   .append(column.getEffectiveName()).append(" DROP NOT NULL;\n");
            }
        }
        
        // 修改注释
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sql.append("COMMENT ON COLUMN ").append(tableName)
               .append(".").append(column.getEffectiveName())
               .append(" IS '").append(column.getComment().replace("'", "''")).append("';\n");
        }
        
        return sql.toString();
    }
    
    /**
     * 将数据类型转换为PG标准类型（用于差异化DDL）
     */
    private String convertToPgTypeForDiff(String dataType) {
        if (dataType == null) return "VARCHAR";
        String upper = dataType.toUpperCase();
        switch (upper) {
            case "VARCHAR": case "VARCHAR2": case "NVARCHAR": case "NVARCHAR2":
                return "VARCHAR";
            case "TEXT": case "LONGTEXT": case "MEDIUMTEXT": case "TINYTEXT":
                return "TEXT";
            case "INT": case "INTEGER": case "TINYINT": case "SMALLINT": case "MEDIUMINT":
                return "INTEGER";
            case "BIGINT": return "BIGINT";
            case "FLOAT": return "REAL";
            case "DOUBLE": return "DOUBLE PRECISION";
            case "DECIMAL": case "NUMERIC": case "NUMBER":
                return "NUMERIC";
            case "DATETIME": case "TIMESTAMP":
                return "TIMESTAMP";
            case "DATE": return "DATE";
            case "BLOB": case "LONGBLOB": case "MEDIUMBLOB": case "TINYBLOB":
                return "BYTEA";
            case "BOOLEAN": case "BOOL": case "BIT":
                return "BOOLEAN";
            case "JSON": case "JSONB":
                return "JSONB";
            default:
                return upper;
        }
    }
    
    /**
     * GaussDB PG风格：创建索引
     */
    private String generateGaussDbPgCreateIndexSql(String tableName, com.schemasync.model.dict.IndexDefinition index) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE ");
        if (index.getIsUnique() != null && index.getIsUnique()) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ").append(index.getIndexName());
        sql.append(" ON ").append(tableName);
        sql.append(" (").append(index.getColumns()).append(");");
        return sql.toString();
    }
}
