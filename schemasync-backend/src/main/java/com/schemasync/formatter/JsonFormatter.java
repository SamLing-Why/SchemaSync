package com.schemasync.formatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schemasync.model.dict.FlatSchemaDictionary;
import com.schemasync.model.dict.SchemaDictionary;
import com.schemasync.model.diff.SchemaDiff;
import com.schemasync.service.SchemaFlattener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JSON格式化工具
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Component
public class JsonFormatter {

    private static final Logger log = LoggerFactory.getLogger(JsonFormatter.class);

    private final ObjectMapper objectMapper;
    
    @Autowired
    private SchemaFlattener schemaFlattener;

    public JsonFormatter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 将数据字典序列化为JSON字节数组(扁平化结构,6个根节点)
     * 
     * @param dictionary 数据字典
     * @return JSON字节数组
     */
    public byte[] format(SchemaDictionary dictionary) {
        try {
            // 先扁平化数据
            FlatSchemaDictionary flat = schemaFlattener.flatten(dictionary);
            return objectMapper.writeValueAsBytes(flat);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /**
     * 将JSON字节数组反序列化为数据字典
     * 
     * @param data JSON字节数组
     * @return 数据字典
     */
    public SchemaDictionary parse(byte[] data) {
        try {
            return objectMapper.readValue(data, SchemaDictionary.class);
        } catch (Exception e) {
            log.error("JSON反序列化失败", e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    /**
     * 将数据字典序列化为JSON字符串(扁平化结构,6个根节点)
     */
    public String formatToString(SchemaDictionary dictionary) {
        try {
            FlatSchemaDictionary flat = schemaFlattener.flatten(dictionary);
            return objectMapper.writeValueAsString(flat);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /**
     * 将JSON字符串反序列化为数据字典
     */
    public SchemaDictionary parseString(String json) {
        try {
            return objectMapper.readValue(json, SchemaDictionary.class);
        } catch (Exception e) {
            log.error("JSON反序列化失败", e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    /**
     * 将差异结果序列化为JSON字节数组
     */
    public byte[] formatDiff(SchemaDiff diff) {
        try {
            return objectMapper.writeValueAsBytes(diff);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /**
     * 将差异结果序列化为JSON字符串
     */
    public String formatDiffToString(SchemaDiff diff) {
        try {
            return objectMapper.writeValueAsString(diff);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            throw new RuntimeException("JSON序列化失败", e);
        }
    }
}
