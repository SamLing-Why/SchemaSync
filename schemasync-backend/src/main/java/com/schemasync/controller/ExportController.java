package com.schemasync.controller;

import com.schemasync.adapter.DatabaseAdapter;
import com.schemasync.adapter.DatabaseAdapterFactory;
import com.schemasync.adapter.ExportOptions;
import com.schemasync.model.config.DataSourceConfig;
import com.schemasync.service.ConfigService;
import com.schemasync.service.SchemaExportService;
import com.schemasync.util.CryptoUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据字典导出控制器
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/api/export")
@Tag(name = "数据字典导出", description = "导出数据字典接口")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    @Autowired
    private SchemaExportService exportService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private DatabaseAdapterFactory adapterFactory;

    @PostMapping
    @Operation(summary = "导出数据字典", description = "根据配置导出数据字典为JSON或Excel文件")
    public ResponseEntity<byte[]> exportSchema(
            @RequestParam String configName,
            @RequestParam String database,
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(required = false) String tablePattern,
            @RequestParam(required = false) String excludeTables) {
        
        // 参数校验
        if (configName == null || configName.trim().isEmpty()) {
            throw new IllegalArgumentException("数据源配置名称不能为空");
        }
        if (database == null || database.trim().isEmpty()) {
            throw new IllegalArgumentException("数据库名称不能为空");
        }
        
        // 构建导出选项
        ExportOptions options = ExportOptions.builder()
                .format(format)
                .database(database)
                .tablePattern(tablePattern)
                .includeIndexes(true)
                .includeForeignKeys(true)
                .build();

        // 导出数据字典
        byte[] data = exportService.exportSchema(configName, options);

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        String fileName = database + "_schema_" + System.currentTimeMillis() + 
                         ("json".equals(format) ? ".json" : ".xlsx");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentLength(data.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    @GetMapping("/databases")
    @Operation(summary = "获取数据库列表", description = "根据数据源配置获取数据库列表")
    public ResponseEntity<List<String>> getDatabases(@RequestParam String configName) {
        log.info("获取数据库列表, 配置名: {}", configName);
        
        // 参数校验
        if (configName == null || configName.trim().isEmpty()) {
            throw new IllegalArgumentException("数据源配置名称不能为空");
        }
        
        // 获取配置
        DataSourceConfig config = configService.getConfigByName(configName);
        if (config == null) {
            throw new IllegalArgumentException("数据源配置不存在: " + configName);
        }
        
        try {
            // 解密密码
            DataSourceConfig configCopy = cloneConfig(config);
            if (configCopy.getPassword() != null && CryptoUtil.isEncrypted(configCopy.getPassword())) {
                try {
                    String decryptedPassword = CryptoUtil.decrypt(configCopy.getPassword());
                    configCopy.setPassword(decryptedPassword);
                } catch (Exception e) {
                    log.error("密码解密失败", e);
                }
            }
            
            // 获取适配器并连接
            DatabaseAdapter adapter = adapterFactory.getAdapter(configCopy.getType());
            List<String> databases;
            
            try (Connection conn = adapter.connect(configCopy)) {
                databases = adapter.getDatabases(conn);
            }
            
            log.info("获取到 {} 个数据库", databases.size());
            return ResponseEntity.ok(databases);
            
        } catch (Exception e) {
            log.error("获取数据库列表失败", e);
            throw new RuntimeException("获取数据库列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 复制配置对象
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
