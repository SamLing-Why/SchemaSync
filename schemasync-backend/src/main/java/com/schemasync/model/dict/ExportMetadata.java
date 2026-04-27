package com.schemasync.model.dict;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 导出元数据
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
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
}
