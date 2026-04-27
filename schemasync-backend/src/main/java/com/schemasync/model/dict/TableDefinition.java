package com.schemasync.model.dict;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 表定义
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
public class TableDefinition {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 表注释/说明
     */
    private String tableComment;
    
    /**
     * 表类型 (BASE TABLE, VIEW, TEMPORARY)
     */
    private String tableType;
    
    /**
     * 存储引擎 (MySQL特有,如InnoDB)
     */
    private String engine;
    
    /**
     * 字符集
     */
    private String charset;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    
    /**
     * 字段列表
     */
    private List<ColumnDefinition> columns;
    
    /**
     * 索引列表
     */
    private List<IndexDefinition> indexes;
    
    /**
     * 外键列表
     */
    private List<ForeignKeyDefinition> foreignKeys;
}
