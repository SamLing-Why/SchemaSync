package com.schemasync.model.diff;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 差异元数据
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
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

    public Date getGeneratedTime() { return generatedTime; }
    public void setGeneratedTime(Date generatedTime) { this.generatedTime = generatedTime; }
    public String getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
    public String getTargetVersion() { return targetVersion; }
    public void setTargetVersion(String targetVersion) { this.targetVersion = targetVersion; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    public String getTargetFile() { return targetFile; }
    public void setTargetFile(String targetFile) { this.targetFile = targetFile; }
    public String getToolVersion() { return toolVersion; }
    public void setToolVersion(String toolVersion) { this.toolVersion = toolVersion; }
}
