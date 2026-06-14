package com.schemasync.controller;

import com.schemasync.model.dict.SchemaDictionary;
import com.schemasync.service.SetTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 套表处理控制器
 */
@RestController
@RequestMapping("/api/settable")
public class SetTableController {

    private static final Logger log = LoggerFactory.getLogger(SetTableController.class);

    @Autowired
    private SetTableService setTableService;

    /**
     * 套表还原导出
     * 上传数据字典Excel，套表只保留一张（优先级：fo > tp > ar）
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportSetTableReduced(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("请上传文件");
            }

            log.info("开始套表还原导出, 文件名: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

            // 解析Excel
            SchemaDictionary dictionary = setTableService.parseExcel(file.getBytes());

            // 套表还原导出
            byte[] data = setTableService.exportSetTableReduced(dictionary);

            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "SchemaSync_SetTable_Reduced_" + timestamp + ".xlsx";

            log.info("套表还原导出完成, 文件大小: {} bytes", data.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);

        } catch (Exception e) {
            log.error("套表还原导出失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("套表还原导出失败: " + e.getMessage()).getBytes());
        }
    }

    /**
     * 套表差异比对
     * 上传数据字典Excel，比对套表内部的字段差异
     */
    @PostMapping("/compare")
    public ResponseEntity<byte[]> compareSetTables(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("请上传文件");
            }

            log.info("开始套表差异比对, 文件名: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

            // 解析Excel
            SchemaDictionary dictionary = setTableService.parseExcel(file.getBytes());

            // 套表差异比对
            byte[] data = setTableService.compareSetTables(dictionary);

            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "SchemaSync_SetTable_Diff_" + timestamp + ".xlsx";

            log.info("套表差异比对完成, 文件大小: {} bytes", data.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);

        } catch (Exception e) {
            log.error("套表差异比对失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("套表差异比对失败: " + e.getMessage()).getBytes());
        }
    }
}
