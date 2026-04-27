package com.schemasync.service;

import com.schemasync.differ.SchemaDiffer;
import com.schemasync.formatter.JsonFormatter;
import com.schemasync.model.dict.SchemaDictionary;
import com.schemasync.model.diff.SchemaDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 数据字典对比服务
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Service
public class SchemaDiffService {

    private static final Logger log = LoggerFactory.getLogger(SchemaDiffService.class);

    @Autowired
    private SchemaDiffer schemaDiffer;

    @Autowired
    private JsonFormatter jsonFormatter;
    
    @Autowired
    private SchemaFlattener schemaFlattener;
    
    @Autowired
    private SchemaDictionaryParser schemaDictionaryParser;
    
    /**
     * 解析文件(支持JSON和Excel)
     */
    private SchemaDictionary parseFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            return schemaDictionaryParser.parseExcel(file.getInputStream());
        } else {
            return jsonFormatter.parse(file.getBytes());
        }
    }

    /**
     * 对比两个数据字典文件
     * 
     * @param oldFile 旧版本文件
     * @param newFile 新版本文件
     * @return 差异结果
     */
    public SchemaDiff compareFiles(MultipartFile oldFile, MultipartFile newFile) {
        try {
            log.info("开始对比文件: {} vs {}", oldFile.getOriginalFilename(), newFile.getOriginalFilename());

            // 1. 解析旧版本(支持JSON和Excel)
            SchemaDictionary oldDict = parseFile(oldFile);
            log.debug("解析旧版本成功, 表数量: {}", 
                    oldDict.getTables() != null ? oldDict.getTables().size() : 0);

            // 2. 解析新版本(支持JSON和Excel)
            SchemaDictionary newDict = parseFile(newFile);
            log.debug("解析新版本成功, 表数量: {}", 
                    newDict.getTables() != null ? newDict.getTables().size() : 0);

            // 3. 执行对比
            SchemaDiff diff = schemaDiffer.compare(oldDict, newDict);

            log.info("对比完成, 发现{}处变更", diff.getChanges().size());
            return diff;

        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("对比失败", e);
            throw new RuntimeException("对比失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对比两个数据字典对象
     */
    public SchemaDiff compareDictionaries(SchemaDictionary oldDict, SchemaDictionary newDict) {
        log.info("开始对比数据字典对象");
        SchemaDiff diff = schemaDiffer.compare(oldDict, newDict);
        log.info("对比完成, 发现{}处变更", diff.getChanges().size());
        return diff;
    }

    /**
     * 将差异结果转为JSON字节数组
     */
    public byte[] diffToJsonBytes(SchemaDiff diff) {
        return jsonFormatter.formatDiff(diff);
    }

    /**
     * 将差异结果转为JSON字符串
     */
    public String diffToJsonString(SchemaDiff diff) {
        return jsonFormatter.formatDiffToString(diff);
    }
}
