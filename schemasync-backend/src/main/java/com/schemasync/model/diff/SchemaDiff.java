package com.schemasync.model.diff;

import java.util.List;

/**
 * 数据字典差异
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
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

    public DiffMetadata getDiffMetadata() { return diffMetadata; }
    public void setDiffMetadata(DiffMetadata diffMetadata) { this.diffMetadata = diffMetadata; }
    public DiffSummary getSummary() { return summary; }
    public void setSummary(DiffSummary summary) { this.summary = summary; }
    public List<SchemaChange> getChanges() { return changes; }
    public void setChanges(List<SchemaChange> changes) { this.changes = changes; }
}
