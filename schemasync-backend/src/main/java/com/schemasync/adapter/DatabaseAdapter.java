package com.schemasync.adapter;

import com.schemasync.model.config.DataSourceConfig;
import com.schemasync.model.dict.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库适配器接口
 * 所有数据库适配器必须实现此接口
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public interface DatabaseAdapter {
    
    /**
     * 连接数据库
     * 
     * @param config 数据源配置
     * @return 数据库连接
     * @throws SQLException SQL异常
     */
    Connection connect(DataSourceConfig config) throws SQLException;
    
    /**
     * 测试连接
     * 
     * @param config 数据源配置
     * @return 连接是否成功
     */
    boolean testConnection(DataSourceConfig config);
    
    /**
     * 获取所有数据库名称
     * 
     * @param conn 数据库连接
     * @return 数据库名称列表
     * @throws SQLException SQL异常
     */
    List<String> getDatabases(Connection conn) throws SQLException;
    
    /**
     * 获取指定数据库的所有表
     * 
     * @param conn 数据库连接
     * @param database 数据库名称
     * @return 表定义列表
     * @throws SQLException SQL异常
     */
    List<TableDefinition> getTables(Connection conn, String database) throws SQLException;
    
    /**
     * 获取表的字段信息
     * 
     * @param conn 数据库连接
     * @param database 数据库名称
     * @param tableName 表名
     * @return 字段定义列表
     * @throws SQLException SQL异常
     */
    List<ColumnDefinition> getColumns(Connection conn, String database, String tableName) throws SQLException;
    
    /**
     * 获取表的索引信息
     * 
     * @param conn 数据库连接
     * @param database 数据库名称
     * @param tableName 表名
     * @return 索引定义列表
     * @throws SQLException SQL异常
     */
    List<IndexDefinition> getIndexes(Connection conn, String database, String tableName) throws SQLException;
    
    /**
     * 获取表的外键信息
     * 
     * @param conn 数据库连接
     * @param database 数据库名称
     * @param tableName 表名
     * @return 外键定义列表
     * @throws SQLException SQL异常
     */
    List<ForeignKeyDefinition> getForeignKeys(Connection conn, String database, String tableName) throws SQLException;
    
    /**
     * 导出完整的数据字典
     * 
     * @param config 数据源配置
     * @param options 导出选项
     * @return 数据字典
     * @throws SQLException SQL异常
     */
    SchemaDictionary exportSchema(DataSourceConfig config, ExportOptions options) throws SQLException;
    
    /**
     * 获取数据库类型
     * 
     * @return 数据库类型
     */
    String getDatabaseType();
    
    /**
     * 获取数据库版本
     * 
     * @param conn 数据库连接
     * @return 数据库版本
     * @throws SQLException SQL异常
     */
    String getDatabaseVersion(Connection conn) throws SQLException;
}
