package com.schemasync.generator;

import lombok.Builder;
import lombok.Data;

/**
 * DDL生成选项
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
@Builder
public class GenerationOptions {
    
    /**
     * 数据库类型
     */
    private String databaseType;
    
    /**
     * 是否包含回滚脚本
     */
    @Builder.Default
    private Boolean includeRollback = false;
    
    /**
     * 是否注释破坏性变更(默认注释,需要手动取消)
     */
    @Builder.Default
    private Boolean commentBreakingChanges = true;
    
    /**
     * 是否添加事务控制
     */
    @Builder.Default
    private Boolean useTransaction = true;
    
    /**
     * 源版本标识
     */
    private String sourceVersion;
    
    /**
     * 目标版本标识
     */
    private String targetVersion;
}
