package com.schemasync.service;

import com.schemasync.model.dict.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DDL生成服务
 * 基于数据字典生成CREATE TABLE语句
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
@Service
public class DdlGeneratorService {
    
    private static final Logger log = LoggerFactory.getLogger(DdlGeneratorService.class);
    
    @Autowired
    private SchemaDictionaryParser parser;
    
    /**
     * 从数据字典文件生成DDL
     * 
     * @param inputStream 文件输入流
     * @param fileType 文件类型(json/excel)
     * @return DDL SQL字符串
     */
    public String generateDdl(InputStream inputStream, String fileType) {
        try {
            SchemaDictionary dictionary;
            if ("excel".equals(fileType)) {
                dictionary = parser.parseExcel(inputStream);
            } else {
                dictionary = parser.parseJson(inputStream.readAllBytes());
            }
            
            return generateDdlFromDictionary(dictionary);
        } catch (Exception e) {
            log.error("生成DDL失败", e);
            throw new RuntimeException("生成DDL失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从数据字典生成DDL SQL
     */
    private String generateDdlFromDictionary(SchemaDictionary dictionary) {
        StringBuilder sql = new StringBuilder();
        
        // 1. 添加注释头
        ExportMetadata metadata = dictionary.getMetadata();
        sql.append("-- ============================================\n");
        sql.append("-- SchemaSync DDL Generation Script\n");
        if (metadata != null) {
            sql.append("-- 数据库: ").append(metadata.getDatabaseName()).append("\n");
            sql.append("-- 类型: ").append(metadata.getDatabaseType()).append("\n");
            sql.append("-- 版本: ").append(metadata.getDatabaseVersion()).append("\n");
            sql.append("-- 生成时间: ").append(new Date()).append("\n");
        }
        sql.append("-- ============================================\n\n");
        
        // 2. 遍历每个表生成DDL
        if (dictionary.getTables() != null) {
            for (TableDefinition table : dictionary.getTables()) {
                // 区分表和视图
                if ("VIEW".equalsIgnoreCase(table.getTableType())) {
                    sql.append(generateCreateView(table)).append("\n\n");
                } else {
                    sql.append(generateCreateTable(table)).append("\n\n");
                }
            }
        }
        
        log.info("全量DDL脚本生成完成, 共{}个表", 
                dictionary.getTables() != null ? dictionary.getTables().size() : 0);
        
        return sql.toString();
    }
    
    /**
     * 生成单个表的CREATE TABLE语句
     */
    private String generateCreateTable(TableDefinition table) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `").append(table.getTableName()).append("` (\n");
        
        // 字段定义
        List<String> columnDefs = new ArrayList<>();
        if (table.getColumns() != null) {
            for (ColumnDefinition column : table.getColumns()) {
                columnDefs.add("  " + generateColumnDefinition(column));
            }
        }
        
        // 主键约束
        if (table.getColumns() != null) {
            String primaryKey = generatePrimaryKey(table.getColumns());
            if (primaryKey != null) {
                columnDefs.add("  " + primaryKey);
            }
        }
        
        // 外键约束
        if (table.getForeignKeys() != null) {
            for (ForeignKeyDefinition fk : table.getForeignKeys()) {
                columnDefs.add("  " + generateForeignKey(fk));
            }
        }
        
        sql.append(String.join(",\n", columnDefs));
        sql.append("\n)");
        
        // 表选项
        if (table.getEngine() != null) {
            sql.append(" ENGINE=").append(table.getEngine());
        }
        if (table.getCharset() != null) {
            sql.append(" DEFAULT CHARSET=").append(table.getCharset());
        }
        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
            sql.append(" COMMENT='").append(table.getTableComment().replace("'", "\\'")).append("'");
        }
        sql.append(";");
        
        return sql.toString();
    }
    
    /**
     * 生成字段定义
     */
    private String generateColumnDefinition(ColumnDefinition column) {
        StringBuilder def = new StringBuilder();
        def.append("`").append(column.getColumnName()).append("` ");
        
        // 数据类型
        String dataType = column.getDataType();
        if (dataType == null) {
            dataType = "VARCHAR";
        }
        def.append(dataType.toUpperCase());
        
        // 精度和小数位优先(如 DECIMAL(10,2))
        if (column.getPrecision() != null && column.getScale() != null) {
            def.append("(").append(column.getPrecision()).append(",")
               .append(column.getScale()).append(")");
        }
        // 长度(如 VARCHAR(100)),仅在没有精度时使用
        else if (column.getLength() != null && column.getLength() > 0) {
            def.append("(").append(column.getLength()).append(")");
        }
        
        // NULL约束
        if (column.getNullable() != null && !column.getNullable()) {
            def.append(" NOT NULL");
        }
        
        // 默认值
        if (column.getDefaultValue() != null) {
            String defaultValue = column.getDefaultValue().toString();
            // 字符串类型需要加引号
            if (defaultValue.toLowerCase().equals("null")) {
                def.append(" DEFAULT NULL");
            } else if (isNumericType(dataType)) {
                def.append(" DEFAULT ").append(defaultValue);
            } else {
                def.append(" DEFAULT '").append(defaultValue.replace("'", "\\'")).append("'");
            }
        }
        
        // 自增
        if (column.getIsAutoIncrement() != null && column.getIsAutoIncrement()) {
            def.append(" AUTO_INCREMENT");
        }
        
        // 注释
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            def.append(" COMMENT '").append(column.getComment().replace("'", "\\'")).append("'");
        }
        
        return def.toString();
    }
    
    /**
     * 生成视图的CREATE VIEW语句
     */
    private String generateCreateView(TableDefinition view) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE OR REPLACE VIEW `").append(view.getTableName()).append("` AS\n");
        
        // 视图定义(从注释或其他地方获取)
        String viewDef = view.getTableComment();
        if (viewDef != null && !viewDef.isEmpty()) {
            sql.append(viewDef);
        } else {
            sql.append("-- TODO: 请补充视图定义");
        }
        sql.append(";");
        
        return sql.toString();
    }
    
    /**
     * 生成主键约束
     */
    private String generatePrimaryKey(List<ColumnDefinition> columns) {
        List<String> pkColumns = columns.stream()
                .filter(col -> col.getIsPrimaryKey() != null && col.getIsPrimaryKey())
                .map(ColumnDefinition::getColumnName)
                .collect(Collectors.toList());
        
        if (pkColumns.isEmpty()) {
            return null;
        }
        
        return "PRIMARY KEY (" + pkColumns.stream()
                .map(col -> "`" + col + "`")
                .collect(Collectors.joining(", ")) + ")";
    }
    
    /**
     * 生成外键约束
     */
    private String generateForeignKey(ForeignKeyDefinition fk) {
        StringBuilder fkDef = new StringBuilder();
        fkDef.append("CONSTRAINT `").append(fk.getConstraintName()).append("` ");
        fkDef.append("FOREIGN KEY (`").append(fk.getColumnName()).append("`) ");
        fkDef.append("REFERENCES `").append(fk.getReferencedTable()).append("` ");
        fkDef.append("(`").append(fk.getReferencedColumn()).append("`)");
        
        // 级联规则
        if (fk.getOnUpdate() != null) {
            fkDef.append(" ON UPDATE ").append(fk.getOnUpdate());
        }
        if (fk.getOnDelete() != null) {
            fkDef.append(" ON DELETE ").append(fk.getOnDelete());
        }
        
        return fkDef.toString();
    }
    
    /**
     * 判断是否为数值类型
     */
    private boolean isNumericType(String dataType) {
        if (dataType == null) return false;
        String upper = dataType.toUpperCase();
        return upper.equals("INT") || upper.equals("INTEGER") || 
               upper.equals("BIGINT") || upper.equals("SMALLINT") ||
               upper.equals("TINYINT") || upper.equals("DECIMAL") ||
               upper.equals("NUMERIC") || upper.equals("FLOAT") ||
               upper.equals("DOUBLE") || upper.startsWith("NUMBER");
    }
}
