package com.schemasync.model.dict;

import java.util.List;

/**
 * 数据字典 - 完整描述一个数据库的结构
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public class SchemaDictionary {
    
    /**
     * 导出元数据
     */
    private ExportMetadata metadata;
    
    /**
     * 表定义列表
     */
    private List<TableDefinition> tables;

    public ExportMetadata getMetadata() { return metadata; }
    public void setMetadata(ExportMetadata metadata) { this.metadata = metadata; }
    public List<TableDefinition> getTables() { return tables; }
    public void setTables(List<TableDefinition> tables) { this.tables = tables; }
}
