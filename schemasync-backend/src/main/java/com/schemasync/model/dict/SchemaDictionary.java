package com.schemasync.model.dict;

import lombok.Data;

import java.util.List;

/**
 * 数据字典 - 完整描述一个数据库的结构
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
public class SchemaDictionary {
    
    /**
     * 导出元数据
     */
    private ExportMetadata metadata;
    
    /**
     * 表定义列表
     */
    private List<TableDefinition> tables;
}
