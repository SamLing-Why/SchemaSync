package com.schemasync.controller;

import com.schemasync.generator.GenerationOptions;
import com.schemasync.model.diff.SchemaDiff;
import com.schemasync.service.DDLGeneratorService;
import com.schemasync.service.SchemaDiffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * DDL生成控制器
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/api/ddl")
@Tag(name = "DDL生成", description = "数据库变更脚本生成接口")
public class DDLController {

    @Autowired
    private SchemaDiffService diffService;

    @Autowired
    private DDLGeneratorService ddlService;

    @PostMapping
    @Operation(summary = "生成DDL变更脚本", description = "对比两个数据字典文件,自动生成DDL脚本")
    public ResponseEntity<byte[]> generateDDL(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile,
            @RequestParam(defaultValue = "mysql") String databaseType,
            @RequestParam(defaultValue = "true") Boolean includeRollback,
            @RequestParam(defaultValue = "true") Boolean commentBreakingChanges,
            @RequestParam(defaultValue = "true") Boolean useTransaction,
            @RequestParam(required = false) String sourceVersion,
            @RequestParam(required = false) String targetVersion) {
        
        // 验证文件
        if (oldFile.isEmpty() || newFile.isEmpty()) {
            throw new RuntimeException("请上传两个文件");
        }

        // 1. 对比数据字典
        SchemaDiff diff = diffService.compareFiles(oldFile, newFile);

        // 2. 构建生成选项
        GenerationOptions options = GenerationOptions.builder()
                .databaseType(databaseType)
                .includeRollback(includeRollback)
                .commentBreakingChanges(commentBreakingChanges)
                .useTransaction(useTransaction)
                .sourceVersion(sourceVersion)
                .targetVersion(targetVersion)
                .build();

        // 3. 生成DDL
        byte[] ddl = ddlService.generateDDLBytes(diff, options);

        // 4. 设置响应头
        HttpHeaders headers = new HttpHeaders();
        String fileName = "ddl_" + System.currentTimeMillis() + ".sql";
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentLength(ddl.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(ddl);
    }

    @PostMapping("/preview")
    @Operation(summary = "预览DDL脚本", description = "对比并预览生成的DDL脚本(不下载)")
    public ResponseEntity<String> previewDDL(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile,
            @RequestParam(defaultValue = "mysql") String databaseType) {
        
        if (oldFile.isEmpty() || newFile.isEmpty()) {
            throw new RuntimeException("请上传两个文件");
        }

        SchemaDiff diff = diffService.compareFiles(oldFile, newFile);
        String ddl = ddlService.generateDDL(diff, databaseType);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(ddl);
    }
}
