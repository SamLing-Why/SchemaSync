package com.schemasync.controller;

import com.schemasync.service.DdlGeneratorService;
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
    
    @PostMapping("/generate")
    @Operation(summary = "生成全量DDL脚本", description = "基于数据字典文件生成全量DDL")
    public ResponseEntity<byte[]> generateDdl(@RequestParam MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
        
        String fileType = filename.endsWith(".xlsx") || filename.endsWith(".xls") 
            ? "excel" : "json";
        
        try (InputStream inputStream = file.getInputStream()) {
            String ddl = ddlGeneratorService.generateDdl(inputStream, fileType);
            
            byte[] ddlBytes = ddl.getBytes(StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String downloadName = filename.replaceAll("\\.(json|xlsx|xls)$", "") + ".sql";
            headers.setContentDispositionFormData("attachment", downloadName);
            headers.setContentLength(ddlBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(ddlBytes);
        } catch (Exception e) {
            throw new RuntimeException("生成DDL失败: " + e.getMessage(), e);
        }
    }
}
