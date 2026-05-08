package com.schemasync.adapter;

import java.util.List;

/**
 * 导出选项
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public class ExportOptions {
    
    /**
     * 输出格式 (json/excel)
     */
    private String format;
    
    /**
     * 数据库名称
     */
    private String database;
    
    /**
     * SCHEMA名称(仅PostgreSQL/GaussDB等支持SCHEMA的数据库使用)
     */
    private String schema;
    
    /**
     * 表名模式过滤(支持通配符)
     */
    private String tablePattern;
    
    /**
     * 排除的表名列表(支持通配符)
     */
    private List<String> excludeTables;
    
    /**
     * 是否包含索引
     */
    private Boolean includeIndexes = true;
    
    /**
     * 是否包含外键
     */
    private Boolean includeForeignKeys = true;
    
    /**
     * 是否包含视图
     */
    private Boolean includeViews = false;

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
    public String getTablePattern() { return tablePattern; }
    public void setTablePattern(String tablePattern) { this.tablePattern = tablePattern; }
    public List<String> getExcludeTables() { return excludeTables; }
    public void setExcludeTables(List<String> excludeTables) { this.excludeTables = excludeTables; }
    public Boolean getIncludeIndexes() { return includeIndexes; }
    public void setIncludeIndexes(Boolean includeIndexes) { this.includeIndexes = includeIndexes; }
    public Boolean getIncludeForeignKeys() { return includeForeignKeys; }
    public void setIncludeForeignKeys(Boolean includeForeignKeys) { this.includeForeignKeys = includeForeignKeys; }
    public Boolean getIncludeViews() { return includeViews; }
    public void setIncludeViews(Boolean includeViews) { this.includeViews = includeViews; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ExportOptions instance = new ExportOptions();
        
        public Builder format(String format) {
            instance.setFormat(format);
            return this;
        }
        
        public Builder database(String database) {
            instance.setDatabase(database);
            return this;
        }
        
        public Builder schema(String schema) {
            instance.setSchema(schema);
            return this;
        }
        
        public Builder tablePattern(String tablePattern) {
            instance.setTablePattern(tablePattern);
            return this;
        }
        
        public Builder excludeTables(List<String> excludeTables) {
            instance.setExcludeTables(excludeTables);
            return this;
        }
        
        public Builder includeIndexes(Boolean includeIndexes) {
            instance.setIncludeIndexes(includeIndexes);
            return this;
        }
        
        public Builder includeForeignKeys(Boolean includeForeignKeys) {
            instance.setIncludeForeignKeys(includeForeignKeys);
            return this;
        }
        
        public Builder includeViews(Boolean includeViews) {
            instance.setIncludeViews(includeViews);
            return this;
        }
        
        public ExportOptions build() {
            return instance;
        }
    }
}
