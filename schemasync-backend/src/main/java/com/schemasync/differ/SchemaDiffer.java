package com.schemasync.differ;

import com.schemasync.model.diff.SchemaChange;
import com.schemasync.model.diff.SchemaDiff;
import com.schemasync.model.dict.SchemaDictionary;

/**
 * 数据字典对比器接口
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public interface SchemaDiffer {
    
    /**
     * 对比两个数据字典
     * 
     * @param oldDict 旧版本数据字典
     * @param newDict 新版本数据字典
     * @return 差异结果
     */
    SchemaDiff compare(SchemaDictionary oldDict, SchemaDictionary newDict);
}
