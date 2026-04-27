package com.schemasync.model.dict;

/**
 * 外键定义
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public class ForeignKeyDefinition {
    
    /**
     * 约束名称
     */
    private String constraintName;
    
    /**
     * 字段名称
     */
    private String columnName;
    
    /**
     * 引用表名
     */
    private String referencedTable;
    
    /**
     * 引用字段名
     */
    private String referencedColumn;
    
    /**
     * 更新规则 (CASCADE, RESTRICT, SET NULL, NO ACTION)
     */
    private String onUpdate;
    
    /**
     * 删除规则 (CASCADE, RESTRICT, SET NULL, NO ACTION)
     */
    private String onDelete;

    public String getConstraintName() { return constraintName; }
    public void setConstraintName(String constraintName) { this.constraintName = constraintName; }
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public String getReferencedTable() { return referencedTable; }
    public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }
    public String getReferencedColumn() { return referencedColumn; }
    public void setReferencedColumn(String referencedColumn) { this.referencedColumn = referencedColumn; }
    public String getOnUpdate() { return onUpdate; }
    public void setOnUpdate(String onUpdate) { this.onUpdate = onUpdate; }
    public String getOnDelete() { return onDelete; }
    public void setOnDelete(String onDelete) { this.onDelete = onDelete; }
}
