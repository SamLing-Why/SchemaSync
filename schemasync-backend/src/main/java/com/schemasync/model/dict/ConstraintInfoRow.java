package com.schemasync.model.dict;

/**
 * 约束信息行
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
public class ConstraintInfoRow {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 约束名称
     */
    private String constraintName;
    
    /**
     * 约束类型(FK/UNIQUE/CHECK)
     */
    private String constraintType;
    
    /**
     * 引用表
     */
    private String referencedTable;
    
    /**
     * 引用字段
     */
    private String referencedColumn;
    
    /**
     * 级联规则
     */
    private String cascadeRule;
    
    /**
     * 备注
     */
    private String comment;

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getConstraintName() { return constraintName; }
    public void setConstraintName(String constraintName) { this.constraintName = constraintName; }
    public String getConstraintType() { return constraintType; }
    public void setConstraintType(String constraintType) { this.constraintType = constraintType; }
    public String getReferencedTable() { return referencedTable; }
    public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }
    public String getReferencedColumn() { return referencedColumn; }
    public void setReferencedColumn(String referencedColumn) { this.referencedColumn = referencedColumn; }
    public String getCascadeRule() { return cascadeRule; }
    public void setCascadeRule(String cascadeRule) { this.cascadeRule = cascadeRule; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
