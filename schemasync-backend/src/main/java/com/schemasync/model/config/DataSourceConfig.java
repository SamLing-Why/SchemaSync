package com.schemasync.model.config;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 数据源配置
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
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
     * 默认值: 3306 (MySQL)
     */
    private Integer port = 3306;
    
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
     * 默认值: utf8mb4
     */
    private String charset = "utf8mb4";
    
    /**
     * 超时时间(秒)
     * 默认值: 30
     */
    private Integer timeout = 30;
    
    /**
     * 自定义JDBC URL(可选)
     * 如果提供,将覆盖自动生成的URL,支持高级配置
     * 示例: jdbc:mysql://host:3306/db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true
     */
    private String jdbcUrl;
    
    /**
     * 连接池高级配置(JSON格式,可选)
     * 示例: {"maximumPoolSize":20,"minimumIdle":5,"idleTimeout":600000}
     */
    private String poolConfig;
    
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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }
    public String getJdbcUrl() { return jdbcUrl; }
    public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
    public String getPoolConfig() { return poolConfig; }
    public void setPoolConfig(String poolConfig) { this.poolConfig = poolConfig; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
