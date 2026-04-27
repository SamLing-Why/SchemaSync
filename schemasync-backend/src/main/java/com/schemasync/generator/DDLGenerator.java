package com.schemasync.generator;

import com.schemasync.model.diff.SchemaDiff;

/**
 * DDL生成器接口
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public interface DDLGenerator {
    
    /**
     * 生成DDL脚本
     * 
     * @param diff 差异对象
     * @param options 生成选项
     * @return DDL脚本
     */
    String generate(SchemaDiff diff, GenerationOptions options);
    
    /**
     * 生成回滚脚本
     * 
     * @param diff 差异对象
     * @return 回滚DDL脚本
     */
    String generateRollback(SchemaDiff diff);
    
    /**
     * 获取数据库类型
     */
    String getDatabaseType();
}
