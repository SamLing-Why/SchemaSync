package com.schemasync.model.dict;

import lombok.Data;

/**
 * 字段定义 - 数据类型、长度、精度分离
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
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
     */
    private Integer length;
    
    /**
     * 精度,数值型字段总位数,如DECIMAL(10,2)的10
     */
    private Integer precision;
    
    /**
     * 小数位数,如DECIMAL(10,2)的2
     */
    private Integer scale;
    
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
}
