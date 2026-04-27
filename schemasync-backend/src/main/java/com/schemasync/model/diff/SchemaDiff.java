package com.schemasync.model.diff;

import lombok.Data;

import java.util.List;

/**
 * 数据字典差异
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
public class SchemaDiff {
    
    /**
     * 差异元数据
     */
    private DiffMetadata diffMetadata;
    
    /**
     * 差异统计
     */
    private DiffSummary summary;
    
    /**
     * 变更列表
     */
    private List<SchemaChange> changes;
}
