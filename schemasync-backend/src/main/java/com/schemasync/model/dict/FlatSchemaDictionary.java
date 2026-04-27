package com.schemasync.model.dict;

import java.util.List;
import java.util.Map;

/**
 * 扁平化的数据字典
 * 将嵌套结构转换为6个独立的二维数据列表,方便Excel加工
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
public class FlatSchemaDictionary {
    
    /**
     * 1. 概述信息 - 键值对列表
     * 包含: 数据库类型、数据库版本、数据库名称、数据库实例名称、导出的日期时间、工具版本
     */
    private List<Map<String, Object>> overview;
    
    /**
     * 2. 表级别信息 - 每个表一行
     */
    private List<TableInfoRow> tables;
    
    /**
     * 3. 字段级别信息 - 每个字段一行(包含表名)
     */
    private List<ColumnInfoRow> columns;
    
    /**
     * 4. 索引信息 - 每个索引一行(包含表名)
     */
    private List<IndexInfoRow> indexes;
    
    /**
     * 5. 约束信息 - 每个约束一行(包含表名)
     */
    private List<ConstraintInfoRow> constraints;
    
    /**
     * 6. 视图定义 - 每个视图一行
     */
    private List<ViewInfoRow> views;

    public List<Map<String, Object>> getOverview() { return overview; }
    public void setOverview(List<Map<String, Object>> overview) { this.overview = overview; }
    public List<TableInfoRow> getTables() { return tables; }
    public void setTables(List<TableInfoRow> tables) { this.tables = tables; }
    public List<ColumnInfoRow> getColumns() { return columns; }
    public void setColumns(List<ColumnInfoRow> columns) { this.columns = columns; }
    public List<IndexInfoRow> getIndexes() { return indexes; }
    public void setIndexes(List<IndexInfoRow> indexes) { this.indexes = indexes; }
    public List<ConstraintInfoRow> getConstraints() { return constraints; }
    public void setConstraints(List<ConstraintInfoRow> constraints) { this.constraints = constraints; }
    public List<ViewInfoRow> getViews() { return views; }
    public void setViews(List<ViewInfoRow> views) { this.views = views; }
}
