package com.schemasync.service;

import com.schemasync.differ.SchemaDiffer;
import com.schemasync.formatter.ExcelFormatter;
import com.schemasync.formatter.JsonFormatter;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            // 将差异转换为SchemaDictionary后再格式化为Excel
            SchemaDictionary diffDict = convertDiffToDictionary(diff);
            return excelFormatter.format(diffDict);
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
     * 为变更的表生成DDL
     */
    private String generateDdlForChangedTables(SchemaDictionary newDict, SchemaDiff diff) {
        StringBuilder sql = new StringBuilder();
        
        // 收集需要生成DDL的表名（新增和修改的表）
        Map<String, Boolean> changedTables = new HashMap<>();
        if (diff.getChanges() != null) {
            for (SchemaChange change : diff.getChanges()) {
                if (change.getChangeType() == com.schemasync.model.diff.ChangeType.TABLE_ADD ||
                    change.getChangeType() == com.schemasync.model.diff.ChangeType.TABLE_MODIFY ||
                    change.getChangeType() == com.schemasync.model.diff.ChangeType.COLUMN_ADD ||
                    change.getChangeType() == com.schemasync.model.diff.ChangeType.COLUMN_DROP ||
                    change.getChangeType() == com.schemasync.model.diff.ChangeType.COLUMN_MODIFY) {
                    changedTables.put(change.getTableName(), true);
                }
            }
        }
        
        // 为新版本中的变更表生成DDL
        if (newDict.getTables() != null) {
            for (TableDefinition table : newDict.getTables()) {
                if (changedTables.containsKey(table.getTableName())) {
                    sql.append("-- 表: ").append(table.getTableName()).append("\n");
                    if ("VIEW".equalsIgnoreCase(table.getTableType())) {
                        sql.append(ddlGeneratorService.generateCreateView(table));
                    } else {
                        sql.append(ddlGeneratorService.generateCreateTable(table));
                    }
                    sql.append("\n\n");
                }
            }
        }
        
        return sql.toString();
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
