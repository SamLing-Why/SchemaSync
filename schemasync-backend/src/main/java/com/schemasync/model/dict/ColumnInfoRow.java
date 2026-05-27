package com.schemasync.model.dict;

/**
 * 字段级别信息行
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
public class ColumnInfoRow {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 字段名称
     */
    private String columnName;
    
    /**
     * 数据类型(不含长度、精度)
     */
    private String dataType;
    
    /**
     * 长度 - 使用Long支持超大值
     */
    private Long length;
    
    /**
     * 精度(数值型字段) - 使用Long支持超大值
     */
    private Long precision;
    
    /**
     * 小数位(numeric/decimal字段的小数位数) - 使用Long支持超大值
     */
    private Long scale;
    
    /**
     * 是否允许NULL
     */
    private Boolean nullable;
    
    /**
     * 默认值
     */
    private Object defaultValue;
    
    /**
     * 是否主键
     */
    private Boolean isPrimaryKey;
    
    /**
     * 是否自增
     */
    private Boolean isAutoIncrement;
    
    /**
     * 字段注释/说明
     */
    private String comment;
    
    /**
     * 字符集(字段级别)
     */
    private String charset;
    
    /**
     * 字段名称(新) - 用于字段重命名场景
     */
    private String newColumnName;

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public Long getLength() { return length; }
    public void setLength(Long length) { this.length = length; }
    public Long getPrecision() { return precision; }
    public void setPrecision(Long precision) { this.precision = precision; }
    public Long getScale() { return scale; }
    public void setScale(Long scale) { this.scale = scale; }
    public Boolean getNullable() { return nullable; }
    public void setNullable(Boolean nullable) { this.nullable = nullable; }
    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
    public Boolean getIsPrimaryKey() { return isPrimaryKey; }
    public void setIsPrimaryKey(Boolean isPrimaryKey) { this.isPrimaryKey = isPrimaryKey; }
    public Boolean getIsAutoIncrement() { return isAutoIncrement; }
    public void setIsAutoIncrement(Boolean isAutoIncrement) { this.isAutoIncrement = isAutoIncrement; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
    public String getNewColumnName() { return newColumnName; }
    public void setNewColumnName(String newColumnName) { this.newColumnName = newColumnName; }
}
