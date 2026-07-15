package com.schemasync.controller;

import com.schemasync.service.DdlGeneratorService;
import com.schemasync.service.SchemaValidatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * DDL生成控制器
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
@RestController
@RequestMapping("/api/ddl")
@Tag(name = "全量DDL脚本生成", description = "基于数据字典生成全量DDL脚本")
public class DdlController {
    
    @Autowired
    private DdlGeneratorService ddlGeneratorService;
    
    @Autowired
    private SchemaValidatorService schemaValidatorService;
    
    @PostMapping("/generate")
    @Operation(summary = "生成全量DDL脚本", description = "基于数据字典文件生成全量DDL")
    public ResponseEntity<byte[]> generateDdl(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "excel") String fileType,
            @RequestParam(defaultValue = "mysql") String databaseType) {
        try (InputStream inputStream = file.getInputStream()) {
            String ddl = ddlGeneratorService.generateDdl(inputStream, fileType, databaseType);
            
            byte[] ddlBytes = ddl.getBytes(StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            // 生成文件名: ddl_yyyyMMddHHmmss_时间戳.sql
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String dateTime = sdf.format(new Date());
            String downloadName = "ddl_" + dateTime + "_" + System.currentTimeMillis() + ".sql";
            
            headers.setContentDispositionFormData("attachment", downloadName);
            headers.setContentLength(ddlBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(ddlBytes);
        } catch (Exception e) {
            throw new RuntimeException("生成DDL失败: " + e.getMessage(), e);
        }
    }
    
    @PostMapping("/preview")
    @Operation(summary = "预览DDL脚本", description = "预览生成的DDL脚本")
    public ResponseEntity<String> previewDdl(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "excel") String fileType,
            @RequestParam(defaultValue = "mysql") String databaseType) {
        try (InputStream inputStream = file.getInputStream()) {
            String ddl = ddlGeneratorService.generateDdl(inputStream, fileType, databaseType);
            return ResponseEntity.ok(ddl);
        } catch (Exception e) {
            throw new RuntimeException("生成DDL失败: " + e.getMessage(), e);
        }
    }
    
    @PostMapping("/download")
    @Operation(summary = "下载DDL脚本", description = "下载生成的DDL脚本文件")
    public ResponseEntity<byte[]> downloadDdl(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "excel") String fileType,
            @RequestParam(defaultValue = "mysql") String databaseType) {
        try (InputStream inputStream = file.getInputStream()) {
            String ddl = ddlGeneratorService.generateDdl(inputStream, fileType, databaseType);
            
            byte[] ddlBytes = ddl.getBytes(StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            // 生成文件名: ddl_yyyyMMddHHmmss_时间戳.sql
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String dateTime = sdf.format(new Date());
            String downloadName = "ddl_" + dateTime + "_" + System.currentTimeMillis() + ".sql";
            
            headers.setContentDispositionFormData("attachment", downloadName);
            headers.setContentLength(ddlBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(ddlBytes);
        } catch (Exception e) {
            throw new RuntimeException("生成DDL失败: " + e.getMessage(), e);
        }
    }
    
    @PostMapping("/validate")
    @Operation(summary = "校验数据字典文件", description = "校验上传的数据字典文件质量，返回校验结果")
    public ResponseEntity<byte[]> validateDictionary(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "excel") String fileType) {
        try (InputStream inputStream = file.getInputStream()) {
            List<SchemaValidatorService.ValidationResult> results = 
                    schemaValidatorService.validate(inputStream, fileType);
            
            // 导出为Excel
            byte[] excelBytes = schemaValidatorService.exportResultsToExcel(results);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            // 生成文件名: validation_yyyyMMddHHmmss_时间戳.xlsx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String dateTime = sdf.format(new Date());
            String downloadName = "validation_" + dateTime + "_" + System.currentTimeMillis() + ".xlsx";
            
            headers.setContentDispositionFormData("attachment", downloadName);
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            throw new RuntimeException("校验失败: " + e.getMessage(), e);
        }
    }
}
