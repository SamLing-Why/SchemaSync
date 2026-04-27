package com.schemasync.model.dict;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 表级别信息行
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
public class TableInfoRow {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 表注释/说明
     */
    private String tableComment;
    
    /**
     * 表类型(普通表/视图/临时表)
     */
    private String tableType;
    
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
     * 存储引擎(MySQL)
     */
    private String engine;
    
    /**
     * 字符集
     */
    private String charset;
    
    /**
     * 排序规则
     */
    private String collation;

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getTableComment() { return tableComment; }
    public void setTableComment(String tableComment) { this.tableComment = tableComment; }
    public String getTableType() { return tableType; }
    public void setTableType(String tableType) { this.tableType = tableType; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getEngine() { return engine; }
    public void setEngine(String engine) { this.engine = engine; }
    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
    public String getCollation() { return collation; }
    public void setCollation(String collation) { this.collation = collation; }
}
