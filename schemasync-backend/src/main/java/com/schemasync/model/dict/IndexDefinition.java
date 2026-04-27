package com.schemasync.model.dict;

import lombok.Data;

import java.util.List;

/**
 * 索引定义
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
public class IndexDefinition {
    
    /**
     * 索引名称
     */
    private String indexName;
    
    /**
     * 索引类型 (PRIMARY, UNIQUE, INDEX, FULLTEXT)
     */
    private String indexType;
    
    /**
     * 索引字段列表
     */
    private List<String> columns;
    
    /**
     * 是否唯一索引
     */
    private Boolean isUnique;
    
    /**
     * 索引备注
     */
    private String comment;
}
