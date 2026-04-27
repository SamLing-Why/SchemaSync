package com.schemasync.generator;

/**
 * DDL生成选项
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public class GenerationOptions {
    
    /**
     * 数据库类型
     */
    private String databaseType;
    
    /**
     * 是否包含回滚脚本
     */
    private Boolean includeRollback = false;
    
    /**
     * 是否注释破坏性变更(默认注释,需要手动取消)
     */
    private Boolean commentBreakingChanges = true;
    
    /**
     * 是否添加事务控制
     */
    private Boolean useTransaction = true;
    
    /**
     * 源版本标识
     */
    private String sourceVersion;
    
    /**
     * 目标版本标识
     */
    private String targetVersion;

    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }
    public Boolean getIncludeRollback() { return includeRollback; }
    public void setIncludeRollback(Boolean includeRollback) { this.includeRollback = includeRollback; }
    public Boolean getCommentBreakingChanges() { return commentBreakingChanges; }
    public void setCommentBreakingChanges(Boolean commentBreakingChanges) { this.commentBreakingChanges = commentBreakingChanges; }
    public Boolean getUseTransaction() { return useTransaction; }
    public void setUseTransaction(Boolean useTransaction) { this.useTransaction = useTransaction; }
    public String getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
    public String getTargetVersion() { return targetVersion; }
    public void setTargetVersion(String targetVersion) { this.targetVersion = targetVersion; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private GenerationOptions instance = new GenerationOptions();
        
        public Builder databaseType(String databaseType) {
            instance.setDatabaseType(databaseType);
            return this;
        }
        
        public Builder includeRollback(Boolean includeRollback) {
            instance.setIncludeRollback(includeRollback);
            return this;
        }
        
        public Builder commentBreakingChanges(Boolean commentBreakingChanges) {
            instance.setCommentBreakingChanges(commentBreakingChanges);
            return this;
        }
        
        public Builder useTransaction(Boolean useTransaction) {
            instance.setUseTransaction(useTransaction);
            return this;
        }
        
        public Builder sourceVersion(String sourceVersion) {
            instance.setSourceVersion(sourceVersion);
            return this;
        }
        
        public Builder targetVersion(String targetVersion) {
            instance.setTargetVersion(targetVersion);
            return this;
        }
        
        public GenerationOptions build() {
            return instance;
        }
    }
}
