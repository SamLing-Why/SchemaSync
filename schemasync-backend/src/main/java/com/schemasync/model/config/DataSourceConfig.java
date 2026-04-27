package com.schemasync.model.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 数据源配置
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Data
public class DataSourceConfig {
    
    /**
     * 数据源ID
     */
    private String id;
    
    /**
     * 数据源名称(别名)
     */
    private String name;
    
    /**
     * 数据库类型 (mysql/oracle/oceanbase/tdsql/gaussdb/goldendb)
     */
    private String type;
    
    /**
     * 主机地址
     */
    private String host;
    
    /**
     * 端口
     */
    private Integer port;
    
    /**
     * 数据库名称
     */
    private String database;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码(加密存储)
     */
    private String password;
    
    /**
     * 字符集
     */
    private String charset;
    
    /**
     * 超时时间(秒)
     */
    private Integer timeout;
    
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
}
