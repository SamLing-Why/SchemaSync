package com.schemasync.model.dict;

import lombok.Data;

/**
 * 外键定义
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
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
}
