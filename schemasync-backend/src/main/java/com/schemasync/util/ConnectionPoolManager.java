package com.schemasync.util;

import com.schemasync.model.config.DataSourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库连接池管理器
 * 负责管理所有数据源的连接池,避免连接池泄漏
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
public class ConnectionPoolManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPoolManager.class);

    /**
     * 连接池缓存: key = 数据源ID, value = HikariDataSource
     */
    private static final ConcurrentHashMap<String, HikariDataSource> POOL_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取数据库连接
     * 
     * @param config 数据源配置
     * @return 数据库连接
     * @throws SQLException SQL异常
     */
    public static Connection getConnection(DataSourceConfig config) throws SQLException {
        String poolKey = buildPoolKey(config);
        
        // 从缓存获取或创建连接池
        HikariDataSource dataSource = POOL_CACHE.computeIfAbsent(poolKey, key -> createDataSource(config));
        
        // 如果连接池已关闭,重新创建
        if (dataSource.isClosed()) {
            dataSource = createDataSource(config);
            POOL_CACHE.put(poolKey, dataSource);
        }
        
        return dataSource.getConnection();
    }

    /**
     * 创建新的连接池
     */
    private static HikariDataSource createDataSource(DataSourceConfig config) {
        log.info("创建数据库连接池: {} - {}:{}", config.getName(), config.getHost(), config.getPort());
        
        HikariConfig hikariConfig = new HikariConfig();
        
        // 使用自定义JDBC URL或自动生成
        if (config.getJdbcUrl() != null && !config.getJdbcUrl().trim().isEmpty()) {
            log.info("使用自定义JDBC URL: {}", config.getJdbcUrl());
            hikariConfig.setJdbcUrl(config.getJdbcUrl());
        } else {
            hikariConfig.setJdbcUrl(buildJdbcUrl(config));
        }
        
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        
        // 连接池基础配置
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(getTimeout(config) * 1000L);
        hikariConfig.setIdleTimeout(600000); // 10分钟
        hikariConfig.setMaxLifetime(1800000); // 30分钟
        
        // 连接测试
        hikariConfig.setConnectionTestQuery("SELECT 1");
        
        // 应用自定义连接池配置(如果提供)
        if (config.getPoolConfig() != null && !config.getPoolConfig().trim().isEmpty()) {
            try {
                applyPoolConfig(hikariConfig, config.getPoolConfig());
            } catch (Exception e) {
                log.error("应用连接池配置失败, 使用默认配置", e);
            }
        }
        
        return new HikariDataSource(hikariConfig);
    }

    /**
     * 构建连接池缓存key
     */
    private static String buildPoolKey(DataSourceConfig config) {
        return config.getType() + ":" + config.getHost() + ":" + 
               config.getPort() + ":" + config.getDatabase() + ":" + config.getUsername();
    }

    /**
     * 构建JDBC URL (根据不同数据库类型)
     */
    private static String buildJdbcUrl(DataSourceConfig config) {
        String type = config.getType().toUpperCase();
        
        switch (type) {
            case "MYSQL":
            case "OCEANBASE":
            case "TDSQL":
            case "GOLDENDB":
                // MySQL兼容型数据库使用utf8字符编码
                // 注意: JDBC层面的utf8等同于MySQL服务器的utf8mb4
                return String.format(
                    "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    config.getHost(), config.getPort(), config.getDatabase()
                );
            case "ORACLE":
                return String.format(
                    "jdbc:oracle:thin:@%s:%d:%s",
                    config.getHost(), config.getPort(), config.getDatabase()
                );
            case "GAUSSDB":
                // GaussDB支持PostgreSQL协议和OpenGauss协议
                // 使用PostgreSQL驱动，关闭SSL要求，增加兼容性
                return String.format(
                    "jdbc:postgresql://%s:%d/%s?sslmode=disable&loggerLevel=OFF",
                    config.getHost(), config.getPort(), config.getDatabase()
                );
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
    }

    /**
     * 获取超时时间
     */
    private static int getTimeout(DataSourceConfig config) {
        return config.getTimeout() != null ? config.getTimeout() : 30;
    }

    /**
     * 应用自定义连接池配置
     * @param hikariConfig Hikari配置对象
     * @param poolConfigJson JSON格式的配置
     */
    private static void applyPoolConfig(HikariConfig hikariConfig, String poolConfigJson) {
        try {
            // 简单的JSON解析(不依赖额外库)
            // 支持配置: maximumPoolSize, minimumIdle, connectionTimeout, idleTimeout, maxLifetime
            
            if (poolConfigJson.contains("maximumPoolSize")) {
                int value = extractIntValue(poolConfigJson, "maximumPoolSize");
                hikariConfig.setMaximumPoolSize(value);
                log.debug("设置 maximumPoolSize = {}", value);
            }
            
            if (poolConfigJson.contains("minimumIdle")) {
                int value = extractIntValue(poolConfigJson, "minimumIdle");
                hikariConfig.setMinimumIdle(value);
                log.debug("设置 minimumIdle = {}", value);
            }
            
            if (poolConfigJson.contains("connectionTimeout")) {
                long value = extractLongValue(poolConfigJson, "connectionTimeout");
                hikariConfig.setConnectionTimeout(value);
                log.debug("设置 connectionTimeout = {}", value);
            }
            
            if (poolConfigJson.contains("idleTimeout")) {
                long value = extractLongValue(poolConfigJson, "idleTimeout");
                hikariConfig.setIdleTimeout(value);
                log.debug("设置 idleTimeout = {}", value);
            }
            
            if (poolConfigJson.contains("maxLifetime")) {
                long value = extractLongValue(poolConfigJson, "maxLifetime");
                hikariConfig.setMaxLifetime(value);
                log.debug("设置 maxLifetime = {}", value);
            }
            
            log.info("自定义连接池配置应用成功");
        } catch (Exception e) {
            log.error("解析连接池配置失败: {}", poolConfigJson, e);
            throw e;
        }
    }
    
    /**
     * 从 JSON中提取int值
     */
    private static int extractIntValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0;
        
        start += pattern.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        
        return Integer.parseInt(json.substring(start, end).trim());
    }
    
    /**
     * 从 JSON中提取long值
     */
    private static long extractLongValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0L;
        
        start += pattern.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        
        return Long.parseLong(json.substring(start, end).trim());
    }

    /**
     * 关闭指定数据源的连接池
     */
    public static void closePool(DataSourceConfig config) {
        String poolKey = buildPoolKey(config);
        HikariDataSource dataSource = POOL_CACHE.remove(poolKey);
        
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("关闭数据库连接池: {}", poolKey);
            dataSource.close();
        }
    }

    /**
     * 关闭所有连接池
     */
    public static void closeAll() {
        log.info("关闭所有数据库连接池, 当前数量: {}", POOL_CACHE.size());
        
        POOL_CACHE.forEach((key, dataSource) -> {
            try {
                if (!dataSource.isClosed()) {
                    dataSource.close();
                    log.debug("已关闭连接池: {}", key);
                }
            } catch (Exception e) {
                log.error("关闭连接池失败: {}", key, e);
            }
        });
        
        POOL_CACHE.clear();
    }

    /**
     * 获取当前连接池数量
     */
    public static int getPoolCount() {
        return POOL_CACHE.size();
    }
}
