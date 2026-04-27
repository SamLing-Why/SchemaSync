package com.schemasync.model.diff;

import lombok.Data;

/**
 * 差异统计
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
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
}
