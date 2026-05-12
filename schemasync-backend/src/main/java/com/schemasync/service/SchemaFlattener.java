package com.schemasync.service;

import com.schemasync.model.dict.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据字典扁平化转换器
 * 将嵌套的SchemaDictionary转换为扁平化的FlatSchemaDictionary
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
@Service
public class SchemaFlattener {
    
    /**
     * 将嵌套的数据字典转换为扁平化结构
     */
    public FlatSchemaDictionary flatten(SchemaDictionary dictionary) {
        FlatSchemaDictionary flat = new FlatSchemaDictionary();
        
        // 1. 提取概述信息
        flat.setOverview(extractOverview(dictionary));
        
        // 2. 提取表级别信息
        flat.setTables(extractTables(dictionary));
        
        // 3. 展开所有字段
        flat.setColumns(extractColumns(dictionary));
        
        // 4. 展开所有索引
        flat.setIndexes(extractIndexes(dictionary));
        
        // 5. 展开所有约束
        flat.setConstraints(extractConstraints(dictionary));
        
        // 6. 提取视图
        flat.setViews(extractViews(dictionary));
        
        return flat;
    }
    
    /**
     * 提取概述信息
     */
    private List<Map<String, Object>> extractOverview(SchemaDictionary dictionary) {
        List<Map<String, Object>> overview = new ArrayList<>();
        ExportMetadata metadata = dictionary.getMetadata();
        
        if (metadata != null) {
            addOverviewRow(overview, "数据库类型", metadata.getDatabaseType());
            addOverviewRow(overview, "数据库版本", metadata.getDatabaseVersion());
            addOverviewRow(overview, "数据库名称", metadata.getDatabaseName());
            addOverviewRow(overview, "数据库实例名称", metadata.getDatabaseName()); // 默认使用数据库名称
            addOverviewRow(overview, "导出的日期时间", metadata.getExportTime());
            addOverviewRow(overview, "工具版本", metadata.getToolVersion() != null ? metadata.getToolVersion() : "1.0.0");
        }
        
        return overview;
    }
    
    private void addOverviewRow(List<Map<String, Object>> overview, String field, Object value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("field", field);
        row.put("value", value != null ? value : "");
        overview.add(row);
    }
    
    /**
     * 提取表级别信息
     */
    private List<TableInfoRow> extractTables(SchemaDictionary dictionary) {
        if (dictionary.getTables() == null) {
            return Collections.emptyList();
        }
        
        return dictionary.getTables().stream()
                .map(this::convertToTableInfoRow)
                .collect(Collectors.toList());
    }
    
    private TableInfoRow convertToTableInfoRow(TableDefinition table) {
        TableInfoRow row = new TableInfoRow();
        row.setTableName(table.getTableName());
        row.setTableComment(table.getTableComment());
        row.setTableType(table.getTableType());
        row.setCreateTime(table.getCreateTime());
        row.setUpdateTime(table.getUpdateTime());
        row.setEngine(table.getEngine());
        row.setCharset(table.getCharset());
        // collation字段暂时为空,需要后续在TableDefinition中添加此字段
        row.setCollation(null);
        return row;
    }
    
    /**
     * 展开所有字段信息
     */
    private List<ColumnInfoRow> extractColumns(SchemaDictionary dictionary) {
        if (dictionary.getTables() == null) {
            return Collections.emptyList();
        }
        
        List<ColumnInfoRow> columns = new ArrayList<>();
        
        for (TableDefinition table : dictionary.getTables()) {
            if (table.getColumns() == null) {
                continue;
            }
            
            for (ColumnDefinition column : table.getColumns()) {
                ColumnInfoRow row = new ColumnInfoRow();
                row.setTableName(table.getTableName());
                row.setColumnName(column.getColumnName());
                row.setDataType(column.getDataType());
                row.setLength(column.getLength());
                row.setPrecision(column.getPrecision());
                row.setScale(column.getScale());
                row.setNullable(column.getNullable());
                row.setDefaultValue(column.getDefaultValue());
                row.setIsPrimaryKey(column.getIsPrimaryKey());
                row.setIsAutoIncrement(column.getIsAutoIncrement());
                row.setComment(column.getComment());
                row.setCharset(column.getCharset());
                
                columns.add(row);
            }
        }
        
        return columns;
    }
    
    /**
     * 展开所有索引信息
     */
    private List<IndexInfoRow> extractIndexes(SchemaDictionary dictionary) {
        if (dictionary.getTables() == null) {
            return Collections.emptyList();
        }
        
        List<IndexInfoRow> indexes = new ArrayList<>();
        
        for (TableDefinition table : dictionary.getTables()) {
            if (table.getIndexes() == null) {
                continue;
            }
            
            for (IndexDefinition index : table.getIndexes()) {
                IndexInfoRow row = new IndexInfoRow();
                row.setTableName(table.getTableName());
                row.setIndexName(index.getIndexName());
                row.setIndexType(index.getIndexType());
                row.setColumns(index.getColumns() != null ? String.join(", ", index.getColumns()) : "");
                row.setComment(index.getComment());
                
                indexes.add(row);
            }
        }
        
        return indexes;
    }
    
    /**
     * 展开所有约束信息
     */
    private List<ConstraintInfoRow> extractConstraints(SchemaDictionary dictionary) {
        if (dictionary.getTables() == null) {
            return Collections.emptyList();
        }
        
        List<ConstraintInfoRow> constraints = new ArrayList<>();
        
        for (TableDefinition table : dictionary.getTables()) {
            if (table.getForeignKeys() == null) {
                continue;
            }
            
            for (ForeignKeyDefinition fk : table.getForeignKeys()) {
                ConstraintInfoRow row = new ConstraintInfoRow();
                row.setTableName(table.getTableName());
                row.setConstraintName(fk.getConstraintName());
                row.setConstraintType("FK"); // 外键
                row.setReferencedTable(fk.getReferencedTable());
                row.setReferencedColumn(fk.getReferencedColumn());
                
                // 组合级联规则
                String cascadeRule = "";
                if (fk.getOnUpdate() != null) {
                    cascadeRule += "ON UPDATE " + fk.getOnUpdate();
                }
                if (fk.getOnDelete() != null) {
                    if (!cascadeRule.isEmpty()) {
                        cascadeRule += ", ";
                    }
                    cascadeRule += "ON DELETE " + fk.getOnDelete();
                }
                row.setCascadeRule(cascadeRule);
                row.setComment("");
                
                constraints.add(row);
            }
        }
        
        return constraints;
    }
    
    /**
     * 提取视图信息
     */
    private List<ViewInfoRow> extractViews(SchemaDictionary dictionary) {
        if (dictionary.getTables() == null) {
            return Collections.emptyList();
        }
        
        // 过滤出视图类型的表
        return dictionary.getTables().stream()
                .filter(table -> "VIEW".equalsIgnoreCase(table.getTableType()))
                .map(this::convertToViewInfoRow)
                .collect(Collectors.toList());
    }
    
    private ViewInfoRow convertToViewInfoRow(TableDefinition table) {
        ViewInfoRow row = new ViewInfoRow();
        row.setViewName(table.getTableName());
        // viewDefinition需要从数据库导出时获取,这里暂时为空
        row.setViewDefinition("");
        row.setComment(table.getTableComment());
        return row;
    }
}
