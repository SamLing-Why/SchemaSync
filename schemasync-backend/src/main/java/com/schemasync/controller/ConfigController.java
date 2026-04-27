package com.schemasync.controller;

import com.schemasync.model.config.DataSourceConfig;
import com.schemasync.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 配置管理控制器
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "配置管理", description = "数据源配置管理接口")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @GetMapping("/datasources")
    @Operation(summary = "获取所有数据源配置")
    public ResponseEntity<List<DataSourceConfig>> getAllConfigs() {
        return ResponseEntity.ok(configService.getAllConfigs());
    }

    @GetMapping("/datasources/{id}")
    @Operation(summary = "根据ID获取数据源配置")
    public ResponseEntity<DataSourceConfig> getConfigById(@PathVariable String id) {
        DataSourceConfig config = configService.getConfigById(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    @PostMapping("/datasources")
    @Operation(summary = "新增数据源配置")
    public ResponseEntity<DataSourceConfig> addConfig(@RequestBody DataSourceConfig config) {
        DataSourceConfig created = configService.addConfig(config);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/datasources/{id}")
    @Operation(summary = "更新数据源配置")
    public ResponseEntity<DataSourceConfig> updateConfig(
            @PathVariable String id,
            @RequestBody DataSourceConfig config) {
        DataSourceConfig updated = configService.updateConfig(id, config);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/datasources/{id}")
    @Operation(summary = "删除数据源配置")
    public ResponseEntity<Void> deleteConfig(@PathVariable String id) {
        configService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/datasources/test")
    @Operation(summary = "测试数据源连接")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        String configId = request.get("configId");
        boolean success = configService.testConnection(configId);
        
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "连接成功" : "连接失败"
        ));
    }
}
