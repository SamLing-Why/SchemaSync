package com.schemasync.model.dict;

/**
 * 字段定义 - 数据类型、长度、精度分离
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public class ColumnDefinition {
    
    /**
     * 字段名称
     */
    private String columnName;
    
    /**
     * 数据类型(不含长度精度),如VARCHAR,DECIMAL,BIGINT
     */
    private String dataType;
    
    /**
     * 长度,字符串类型使用,如VARCHAR(100)的100
     * 使用Long支持超大值(如TEXT类型的4294967295)
     */
    private Long length;
    
    /**
     * 精度,数值型字段总位数,如DECIMAL(10,2)的10
     */
    private Long precision;
    
    /**
     * 小数位数,如DECIMAL(10,2)的2
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
     * 字段位置
     */
    private Integer ordinalPosition;
    
    /**
     * 字段名称(新) - 用于字段重命名场景
     * 如果不为空，DDL生成时使用此名称替代columnName
     */
    private String newColumnName;

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
    public Integer getOrdinalPosition() { return ordinalPosition; }
    public void setOrdinalPosition(Integer ordinalPosition) { this.ordinalPosition = ordinalPosition; }
    public String getNewColumnName() { return newColumnName; }
    public void setNewColumnName(String newColumnName) { this.newColumnName = newColumnName; }
    
    /**
     * 获取实际使用的字段名称
     * 如果newColumnName不为空，返回newColumnName；否则返回columnName
     */
    public String getEffectiveName() {
        if (newColumnName != null && !newColumnName.trim().isEmpty()) {
            return newColumnName.trim();
        }
        return columnName;
    }
}
