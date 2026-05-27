package com.schemasync.controller;

import com.schemasync.model.diff.SchemaDiff;
import com.schemasync.service.SchemaDiffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 数据字典对比控制器
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/api/diff")
@Tag(name = "版本对比", description = "数据字典版本对比接口")
public class DiffController {

    @Autowired
    private SchemaDiffService diffService;

    @PostMapping
    @Operation(summary = "对比两个数据字典文件", description = "上传两个版本的文件,生成差异报告(支持JSON和Excel)")
    public ResponseEntity<byte[]> compareFiles(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile,
            @RequestParam(defaultValue = "excel") String exportFormat) {
        
        // 验证文件
        if (oldFile.isEmpty() || newFile.isEmpty()) {
            throw new RuntimeException("请上传两个文件");
        }

        // 执行对比并格式化（内部会传递newDict用于生成DDL）
        byte[] data = diffService.compareAndFormat(oldFile, newFile, exportFormat);

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        
        // 生成文件名: diff_yyyyMMddHHmmss_时间戳.扩展名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateTime = sdf.format(new Date());
        String extension = "excel".equals(exportFormat) ? ".xlsx" : ".json";
        String fileName = "diff_" + dateTime + "_" + System.currentTimeMillis() + extension;
        
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentLength(data.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    @PostMapping("/summary")
    @Operation(summary = "获取差异统计", description = "上传两个版本的文件,返回差异统计信息")
    public ResponseEntity<SchemaDiff> compareAndGetSummary(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile) {
        
        if (oldFile.isEmpty() || newFile.isEmpty()) {
            throw new RuntimeException("请上传两个文件");
        }

        SchemaDiff diff = diffService.compareFiles(oldFile, newFile);
        return ResponseEntity.ok(diff);
    }
    
    @PostMapping("/ddl")
    @Operation(summary = "生成差异化DDL脚本", description = "基于两个版本的对比结果生成DDL脚本")
    public ResponseEntity<byte[]> generateDdl(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile,
            @RequestParam(defaultValue = "mysql") String databaseType) {
        
        if (oldFile.isEmpty() || newFile.isEmpty()) {
            throw new RuntimeException("请上传两个文件");
        }

        byte[] data = diffService.generateDdlFromDiff(oldFile, newFile, databaseType);

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        
        // 生成文件名: ddl_yyyyMMddHHmmss_时间戳.sql
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateTime = sdf.format(new Date());
        String fileName = "ddl_" + dateTime + "_" + System.currentTimeMillis() + ".sql";
        
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentLength(data.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}
