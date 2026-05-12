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
    
    /**
     * 旧数据类型
     */
    private String oldDataType;
    
    /**
     * 新数据类型
     */
    private String newDataType;
    
    /**
     * 旧长度
     */
    private Long oldLength;
    
    /**
     * 新长度
     */
    private Long newLength;
    
    /**
     * 旧精度
     */
    private Long oldPrecision;
    
    /**
     * 新精度
     */
    private Long newPrecision;
    
    /**
     * 旧字段注释
     */
    private String oldComment;
    
    /**
     * 新字段注释
     */
    private String newComment;

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
    
    public String getOldDataType() { return oldDataType; }
    public void setOldDataType(String oldDataType) { this.oldDataType = oldDataType; }
    public String getNewDataType() { return newDataType; }
    public void setNewDataType(String newDataType) { this.newDataType = newDataType; }
    public Long getOldLength() { return oldLength; }
    public void setOldLength(Long oldLength) { this.oldLength = oldLength; }
    public Long getNewLength() { return newLength; }
    public void setNewLength(Long newLength) { this.newLength = newLength; }
    public Long getOldPrecision() { return oldPrecision; }
    public void setOldPrecision(Long oldPrecision) { this.oldPrecision = oldPrecision; }
    public Long getNewPrecision() { return newPrecision; }
    public void setNewPrecision(Long newPrecision) { this.newPrecision = newPrecision; }
    public String getOldComment() { return oldComment; }
    public void setOldComment(String oldComment) { this.oldComment = oldComment; }
    public String getNewComment() { return newComment; }
    public void setNewComment(String newComment) { this.newComment = newComment; }
    
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
        
        public Builder oldDataType(String oldDataType) {
            instance.setOldDataType(oldDataType);
            return this;
        }
        
        public Builder newDataType(String newDataType) {
            instance.setNewDataType(newDataType);
            return this;
        }
        
        public Builder oldLength(Long oldLength) {
            instance.setOldLength(oldLength);
            return this;
        }
        
        public Builder newLength(Long newLength) {
            instance.setNewLength(newLength);
            return this;
        }
        
        public Builder oldPrecision(Long oldPrecision) {
            instance.setOldPrecision(oldPrecision);
            return this;
        }
        
        public Builder newPrecision(Long newPrecision) {
            instance.setNewPrecision(newPrecision);
            return this;
        }
        
        public Builder oldComment(String oldComment) {
            instance.setOldComment(oldComment);
            return this;
        }
        
        public Builder newComment(String newComment) {
            instance.setNewComment(newComment);
            return this;
        }
        
        public SchemaChange build() {
            return instance;
        }
    }
}
