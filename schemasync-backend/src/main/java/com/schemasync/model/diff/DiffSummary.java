package com.schemasync.model.diff;

/**
 * 差异统计
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public class DiffSummary {
    
    /** 新增表数量 */
    private Integer tablesAdded;
    
    /** 删除表数量 */
    private Integer tablesDropped;
    
    /** 修改表数量 */
    private Integer tablesModified;
    
    /** 新增字段数量 */
    private Integer columnsAdded;
    
    /** 删除字段数量 */
    private Integer columnsDropped;
    
    /** 修改字段数量 */
    private Integer columnsModified;
    
    /** 新增索引数量 */
    private Integer indexesAdded;
    
    /** 删除索引数量 */
    private Integer indexesDropped;
    
    /** 新增外键数量 */
    private Integer foreignKeysAdded;
    
    /** 删除外键数量 */
    private Integer foreignKeysDropped;
    
    /** 破坏性变更数量 */
    private Integer breakingChanges;

    public Integer getTablesAdded() { return tablesAdded; }
    public void setTablesAdded(Integer tablesAdded) { this.tablesAdded = tablesAdded; }
    public Integer getTablesDropped() { return tablesDropped; }
    public void setTablesDropped(Integer tablesDropped) { this.tablesDropped = tablesDropped; }
    public Integer getTablesModified() { return tablesModified; }
    public void setTablesModified(Integer tablesModified) { this.tablesModified = tablesModified; }
    public Integer getColumnsAdded() { return columnsAdded; }
    public void setColumnsAdded(Integer columnsAdded) { this.columnsAdded = columnsAdded; }
    public Integer getColumnsDropped() { return columnsDropped; }
    public void setColumnsDropped(Integer columnsDropped) { this.columnsDropped = columnsDropped; }
    public Integer getColumnsModified() { return columnsModified; }
    public void setColumnsModified(Integer columnsModified) { this.columnsModified = columnsModified; }
    public Integer getIndexesAdded() { return indexesAdded; }
    public void setIndexesAdded(Integer indexesAdded) { this.indexesAdded = indexesAdded; }
    public Integer getIndexesDropped() { return indexesDropped; }
    public void setIndexesDropped(Integer indexesDropped) { this.indexesDropped = indexesDropped; }
    public Integer getForeignKeysAdded() { return foreignKeysAdded; }
    public void setForeignKeysAdded(Integer foreignKeysAdded) { this.foreignKeysAdded = foreignKeysAdded; }
    public Integer getForeignKeysDropped() { return foreignKeysDropped; }
    public void setForeignKeysDropped(Integer foreignKeysDropped) { this.foreignKeysDropped = foreignKeysDropped; }
    public Integer getBreakingChanges() { return breakingChanges; }
    public void setBreakingChanges(Integer breakingChanges) { this.breakingChanges = breakingChanges; }
}
