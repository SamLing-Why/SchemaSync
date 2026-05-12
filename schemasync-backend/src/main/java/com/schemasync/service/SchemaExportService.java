package com.schemasync.service;

import com.schemasync.adapter.DatabaseAdapter;
import com.schemasync.adapter.DatabaseAdapterFactory;
import com.schemasync.adapter.ExportOptions;
import com.schemasync.formatter.ExcelFormatter;
import com.schemasync.formatter.JsonFormatter;
import com.schemasync.model.config.DataSourceConfig;
import com.schemasync.model.dict.SchemaDictionary;
import com.schemasync.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 数据字典导出服务
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Service
public class SchemaExportService {

    private static final Logger log = LoggerFactory.getLogger(SchemaExportService.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private DatabaseAdapterFactory adapterFactory;

    @Autowired
    private JsonFormatter jsonFormatter;

    @Autowired
    private ExcelFormatter excelFormatter;

    /**
     * 导出数据字典
     * 
     * @param configName 数据源配置名称
     * @param options 导出选项
     * @return 数据字典文件的字节数组
     */
    public byte[] exportSchema(String configName, ExportOptions options) {
        long totalStartTime = System.currentTimeMillis();
        
        if (configName == null || configName.trim().isEmpty()) {
            throw new IllegalArgumentException("数据源配置名称不能为空");
        }
        if (options == null) {
            throw new IllegalArgumentException("导出选项不能为空");
        }
        if (options.getDatabase() == null || options.getDatabase().trim().isEmpty()) {
            throw new IllegalArgumentException("数据库名称不能为空");
        }
        
        if (options.getFormat() == null || options.getFormat().trim().isEmpty()) {
            options.setFormat("excel");
        }
        
        log.info("========== 开始导出数据字典 ==========");
        log.info("配置: {}, 数据库: {}, 格式: {}", configName, options.getDatabase(), options.getFormat());

        DataSourceConfig config = configService.getConfigByName(configName);
        if (config == null) {
            throw new RuntimeException("数据源配置不存在: " + configName);
        }

        DatabaseAdapter adapter = adapterFactory.getAdapter(config.getType());
        log.debug("使用适配器: {}", adapter.getDatabaseType());

        DataSourceConfig configCopy = cloneConfig(config);
        if (configCopy.getPassword() != null && CryptoUtil.isEncrypted(configCopy.getPassword())) {
            try {
                String decryptedPassword = CryptoUtil.decrypt(configCopy.getPassword());
                configCopy.setPassword(decryptedPassword);
                log.debug("密码已解密");
            } catch (Exception e) {
                log.error("密码解密失败", e);
            }
        }

        try {
            long exportStart = System.currentTimeMillis();
            SchemaDictionary dictionary = adapter.exportSchema(configCopy, options);
            long exportTime = System.currentTimeMillis() - exportStart;
            log.info("数据字典导出完成, 耗时: {}ms", exportTime);

            byte[] result;
            long formatStart = System.currentTimeMillis();
            if ("excel".equalsIgnoreCase(options.getFormat())) {
                result = excelFormatter.format(dictionary);
            } else {
                result = jsonFormatter.format(dictionary);
            }
            long formatTime = System.currentTimeMillis() - formatStart;
            
            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("========== 数据字典导出完成 ==========");
            log.info("导出格式: {}, 文件大小: {} bytes, 格式化耗时: {}ms", 
                options.getFormat(), result.length, formatTime);
            log.info("总耗时: {}ms ({}秒)", totalTime, totalTime / 1000.0);
            return result;
            
        } catch (Exception e) {
            log.error("导出数据字典失败", e);
            throw new RuntimeException("导出数据字典失败: " + e.getMessage(), e);
        }
    }

    /**
     * 导出数据字典为JSON字符串
     */
    public String exportSchemaAsString(String configName, ExportOptions options) {
        byte[] data = exportSchema(configName, options);
        return new String(data);
    }

    /**
     * 克隆配置对象
     */
    private DataSourceConfig cloneConfig(DataSourceConfig config) {
        DataSourceConfig copy = new DataSourceConfig();
        copy.setId(config.getId());
        copy.setName(config.getName());
        copy.setType(config.getType());
        copy.setHost(config.getHost());
        copy.setPort(config.getPort());
        copy.setDatabase(config.getDatabase());
        copy.setUsername(config.getUsername());
        copy.setPassword(config.getPassword());
        copy.setCharset(config.getCharset());
        copy.setTimeout(config.getTimeout());
        copy.setJdbcUrl(config.getJdbcUrl());
        copy.setPoolConfig(config.getPoolConfig());
        return copy;
    }
}
