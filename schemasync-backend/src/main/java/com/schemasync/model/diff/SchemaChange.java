package com.schemasync.model.diff;

import lombok.Builder;
import lombok.Data;

/**
 * 变更项
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
@Builder
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
}
