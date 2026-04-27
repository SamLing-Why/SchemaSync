package com.schemasync.model.diff;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 差异元数据
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
public class DiffMetadata {
    
    /**
     * 生成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date generatedTime;
    
    /**
     * 源版本标识
     */
    private String sourceVersion;
    
    /**
     * 目标版本标识
     */
    private String targetVersion;
    
    /**
     * 源文件路径
     */
    private String sourceFile;
    
    /**
     * 目标文件路径
     */
    private String targetFile;
    
    /**
     * 工具版本
     */
    private String toolVersion;
}
