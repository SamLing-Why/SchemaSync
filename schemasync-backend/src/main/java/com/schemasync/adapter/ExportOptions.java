package com.schemasync.adapter;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 导出选项
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
@Builder
public class ExportOptions {
    
    /**
     * 输出格式 (json/excel)
     */
    private String format;
    
    /**
     * 数据库名称
     */
    private String database;
    
    /**
     * 表名模式过滤(支持通配符)
     */
    private String tablePattern;
    
    /**
     * 排除的表名列表(支持通配符)
     */
    private List<String> excludeTables;
    
    /**
     * 是否包含索引
     */
    @Builder.Default
    private Boolean includeIndexes = true;
    
    /**
     * 是否包含外键
     */
    @Builder.Default
    private Boolean includeForeignKeys = true;
    
    /**
     * 是否包含视图
     */
    @Builder.Default
    private Boolean includeViews = false;
}
