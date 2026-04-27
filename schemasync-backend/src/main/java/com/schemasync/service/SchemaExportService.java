package com.schemasync.service;

import com.schemasync.adapter.DatabaseAdapter;
import com.schemasync.adapter.DatabaseAdapterFactory;
import com.schemasync.adapter.ExportOptions;
import com.schemasync.formatter.ExcelFormatter;
import com.schemasync.formatter.JsonFormatter;
import com.schemasync.model.config.DataSourceConfig;
import com.schemasync.model.dict.SchemaDictionary;
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
        if (configName == null || configName.trim().isEmpty()) {
            throw new IllegalArgumentException("数据源配置名称不能为空");
        }
        if (options == null) {
            throw new IllegalArgumentException("导出选项不能为空");
        }
        if (options.getDatabase() == null || options.getDatabase().trim().isEmpty()) {
            throw new IllegalArgumentException("数据库名称不能为空");
        }
        
        // 设置默认值
        if (options.getFormat() == null || options.getFormat().trim().isEmpty()) {
            options.setFormat("excel");  // 默认导出Excel
        }
        
        log.info("开始导出数据字典, 配置: {}, 数据库: {}", configName, options.getDatabase());

        // 1. 获取数据源配置
        DataSourceConfig config = configService.getConfigByName(configName);
        if (config == null) {
            throw new RuntimeException("数据源配置不存在: " + configName);
        }

        // 2. 获取对应数据库适配器
        DatabaseAdapter adapter = adapterFactory.getAdapter(config.getType());
        log.debug("使用适配器: {}", adapter.getDatabaseType());

        try {
            // 3. 导出数据字典
            SchemaDictionary dictionary = adapter.exportSchema(config, options);

            // 4. 根据格式输出
            byte[] result;
            if ("excel".equalsIgnoreCase(options.getFormat())) {
                result = excelFormatter.format(dictionary);
            } else {
                result = jsonFormatter.format(dictionary);
            }
            
            log.info("数据字典导出成功, 格式: {}, 大小: {} bytes", options.getFormat(), result.length);
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
}
