package com.schemasync.model.dict;

/**
 * 索引信息行
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
public class IndexInfoRow {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 索引名称
     */
    private String indexName;
    
    /**
     * 索引类型(主键/唯一/普通/全文)
     */
    private String indexType;
    
    /**
     * 索引字段及顺序(逗号分隔)
     */
    private String columns;
    
    /**
     * 索引备注
     */
    private String comment;

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    public String getIndexType() { return indexType; }
    public void setIndexType(String indexType) { this.indexType = indexType; }
    public String getColumns() { return columns; }
    public void setColumns(String columns) { this.columns = columns; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
