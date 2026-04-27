package com.schemasync.controller;

import com.schemasync.adapter.ExportOptions;
import com.schemasync.service.SchemaExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private SchemaExportService exportService;

    @PostMapping
    @Operation(summary = "导出数据字典", description = "根据配置导出数据字典为JSON或Excel文件")
    public ResponseEntity<byte[]> exportSchema(
            @RequestParam String configName,
            @RequestParam String database,
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(required = false) String tablePattern,
            @RequestParam(required = false) String excludeTables) {
        
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
}
