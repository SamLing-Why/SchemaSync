package com.schemasync.model.diff;

/**
 * 变更项
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public class SchemaChange {
    
    /**
     * 变更类型
     */
    private ChangeType changeType;
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 字段名(字段级别变更时)
     */
    private String columnName;
    
    /**
     * 严重程度
     */
    private Severity severity;
    
    /**
     * 变更详情
     */
    private Object details;

    public ChangeType getChangeType() { return changeType; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public Object getDetails() { return details; }
    public void setDetails(Object details) { this.details = details; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private SchemaChange instance = new SchemaChange();
        
        public Builder changeType(ChangeType changeType) {
            instance.setChangeType(changeType);
            return this;
        }
        
        public Builder tableName(String tableName) {
            instance.setTableName(tableName);
            return this;
        }
        
        public Builder columnName(String columnName) {
            instance.setColumnName(columnName);
            return this;
        }
        
        public Builder severity(Severity severity) {
            instance.setSeverity(severity);
            return this;
        }
        
        public Builder details(Object details) {
            instance.setDetails(details);
            return this;
        }
        
        public SchemaChange build() {
            return instance;
        }
    }
}
