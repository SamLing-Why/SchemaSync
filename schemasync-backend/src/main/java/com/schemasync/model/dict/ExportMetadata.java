package com.schemasync.model.dict;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 导出元数据
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public class ExportMetadata {
    
    /**
     * 导出时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date exportTime;
    
    /**
     * 数据库类型 (MySQL/Oracle/OceanBase/TDSQL/GaussDB/GoldenDB)
     */
    private String databaseType;
    
    /**
     * 数据库版本
     */
    private String databaseVersion;
    
    /**
     * 数据库名称
     */
    private String databaseName;
    
    /**
     * Schema名称(可选)
     */
    private String schemaName;
    
    /**
     * 工具版本
     */
    private String toolVersion;

    public Date getExportTime() { return exportTime; }
    public void setExportTime(Date exportTime) { this.exportTime = exportTime; }
    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }
    public String getDatabaseVersion() { return databaseVersion; }
    public void setDatabaseVersion(String databaseVersion) { this.databaseVersion = databaseVersion; }
    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public String getToolVersion() { return toolVersion; }
    public void setToolVersion(String toolVersion) { this.toolVersion = toolVersion; }
}
