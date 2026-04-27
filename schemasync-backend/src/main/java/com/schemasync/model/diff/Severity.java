package com.schemasync.model.diff;

/**
 * 严重程度枚举
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public enum Severity {
    
    /** 破坏性变更(可能导致数据丢失或应用故障) */
    BREAKING,
    
    /** 非破坏性变更 */
    NON_BREAKING
}
