package com.schemasync.model.diff;

/**
 * 变更类型枚举
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public enum ChangeType {
    
    // ========== 表级别变更 ==========
    /** 新增表 */
    TABLE_ADD,
    /** 删除表 */
    TABLE_DROP,
    /** 修改表 */
    TABLE_MODIFY,
    
    // ========== 字段级别变更 ==========
    /** 新增字段 */
    COLUMN_ADD,
    /** 删除字段 */
    COLUMN_DROP,
    /** 修改字段 */
    COLUMN_MODIFY,
    
    // ========== 索引级别变更 ==========
    /** 新增索引 */
    INDEX_ADD,
    /** 删除索引 */
    INDEX_DROP,
    /** 修改索引 */
    INDEX_MODIFY,
    
    // ========== 约束级别变更 ==========
    /** 新增外键 */
    FOREIGN_KEY_ADD,
    /** 删除外键 */
    FOREIGN_KEY_DROP,
    /** 修改外键 */
    FOREIGN_KEY_MODIFY
}
