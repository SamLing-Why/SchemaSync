package com.schemasync.formatter;

import com.schemasync.model.dict.*;
import com.schemasync.service.SchemaFlattener;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Excel格式化工具
 * 使用Apache POI生成Excel格式的数据字典
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Component
public class ExcelFormatter {

    private static final Logger log = LoggerFactory.getLogger(ExcelFormatter.class);

    @Autowired
    private SchemaFlattener schemaFlattener;

    /**
     * 将数据字典转换为Excel字节数组
     * 
     * @param dictionary 数据字典
     * @return Excel文件字节数组
     */
    public byte[] format(SchemaDictionary dictionary) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 先扁平化数据
            FlatSchemaDictionary flat = schemaFlattener.flatten(dictionary);
            
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 创建6个Sheet
            createOverviewSheet(workbook, flat.getOverview(), headerStyle, dataStyle);
            createTablesSheet(workbook, flat.getTables(), headerStyle, dataStyle);
            createColumnsSheet(workbook, flat.getColumns(), headerStyle, dataStyle);
            createIndexesSheet(workbook, flat.getIndexes(), headerStyle, dataStyle);
            createConstraintsSheet(workbook, flat.getConstraints(), headerStyle, dataStyle);
            createViewsSheet(workbook, flat.getViews(), headerStyle, dataStyle);

            // 输出为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] result = outputStream.toByteArray();
            
            log.info("Excel导出成功, 大小: {} bytes, 表数量: {}", 
                    result.length, 
                    flat.getTables() != null ? flat.getTables().size() : 0);
            
            return result;
            
        } catch (IOException e) {
            log.error("Excel生成失败", e);
            throw new RuntimeException("Excel生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * Sheet 1: 概述信息
     */
    private void createOverviewSheet(Workbook workbook, List<Map<String, Object>> overview,
                                     CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("概述信息");
        
        // 表头
        Row headerRow = sheet.createRow(0);
        setCell(headerRow, 0, "字段名", headerStyle);
        setCell(headerRow, 1, "值", headerStyle);
        
        // 数据
        if (overview != null) {
            int rowNum = 1;
            for (Map<String, Object> row : overview) {
                Row dataRow = sheet.createRow(rowNum++);
                setCell(dataRow, 0, row.get("field") != null ? row.get("field").toString() : "", dataStyle);
                Object value = row.get("value");
                if (value instanceof java.util.Date) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    setCell(dataRow, 1, sdf.format((java.util.Date) value), dataStyle);
                } else {
                    setCell(dataRow, 1, value != null ? value.toString() : "", dataStyle);
                }
            }
        }
        
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 40 * 256);
    }
    
    /**
     * Sheet 2: 表级别信息
     */
    private void createTablesSheet(Workbook workbook, List<TableInfoRow> tables,
                                   CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("表级别信息");
        
        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"表名", "表注释", "表类型", "创建时间", "更新时间", "存储引擎", "字符集", "排序规则"};
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], headerStyle);
        }
        
        // 数据
        if (tables != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            int rowNum = 1;
            for (TableInfoRow table : tables) {
                Row dataRow = sheet.createRow(rowNum++);
                setCell(dataRow, 0, table.getTableName(), dataStyle);
                setCell(dataRow, 1, table.getTableComment(), dataStyle);
                setCell(dataRow, 2, table.getTableType(), dataStyle);
                setCell(dataRow, 3, table.getCreateTime() != null ? sdf.format(table.getCreateTime()) : "", dataStyle);
                setCell(dataRow, 4, table.getUpdateTime() != null ? sdf.format(table.getUpdateTime()) : "", dataStyle);
                setCell(dataRow, 5, table.getEngine(), dataStyle);
                setCell(dataRow, 6, table.getCharset(), dataStyle);
                setCell(dataRow, 7, table.getCollation(), dataStyle);
            }
        }
        
        autoSizeColumns(sheet, headers.length);
    }
    
    /**
     * Sheet 3: 字段级别信息
     */
    private void createColumnsSheet(Workbook workbook, List<ColumnInfoRow> columns,
                                    CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("字段级别信息");
        
        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"表名", "字段名称", "数据类型", "长度", "精度", "是否允许NULL", "默认值", "是否主键", "是否自增", "字段注释", "字符集"};
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], headerStyle);
        }
        
        // 数据
        if (columns != null) {
            int rowNum = 1;
            for (ColumnInfoRow column : columns) {
                Row dataRow = sheet.createRow(rowNum++);
                setCell(dataRow, 0, column.getTableName(), dataStyle);
                setCell(dataRow, 1, column.getColumnName(), dataStyle);
                setCell(dataRow, 2, column.getDataType(), dataStyle);
                setCell(dataRow, 3, column.getLength() != null ? column.getLength().toString() : "", dataStyle);
                setCell(dataRow, 4, column.getPrecision() != null ? column.getPrecision().toString() : "", dataStyle);
                setCell(dataRow, 5, column.getNullable() != null && column.getNullable() ? "是" : "否", dataStyle);
                setCell(dataRow, 6, column.getDefaultValue() != null ? column.getDefaultValue().toString() : "", dataStyle);
                setCell(dataRow, 7, column.getIsPrimaryKey() != null && column.getIsPrimaryKey() ? "是" : "否", dataStyle);
                setCell(dataRow, 8, column.getIsAutoIncrement() != null && column.getIsAutoIncrement() ? "是" : "否", dataStyle);
                setCell(dataRow, 9, column.getComment(), dataStyle);
                setCell(dataRow, 10, column.getCharset(), dataStyle);
            }
        }
        
        autoSizeColumns(sheet, headers.length);
    }
    
    /**
     * Sheet 4: 索引信息
     */
    private void createIndexesSheet(Workbook workbook, List<IndexInfoRow> indexes,
                                    CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("索引信息");
        
        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"表名", "索引名称", "索引类型", "索引字段及顺序", "索引备注"};
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], headerStyle);
        }
        
        // 数据
        if (indexes != null) {
            int rowNum = 1;
            for (IndexInfoRow index : indexes) {
                Row dataRow = sheet.createRow(rowNum++);
                setCell(dataRow, 0, index.getTableName(), dataStyle);
                setCell(dataRow, 1, index.getIndexName(), dataStyle);
                setCell(dataRow, 2, index.getIndexType(), dataStyle);
                setCell(dataRow, 3, index.getColumns(), dataStyle);
                setCell(dataRow, 4, index.getComment(), dataStyle);
            }
        }
        
        autoSizeColumns(sheet, headers.length);
    }
    
    /**
     * Sheet 5: 约束信息
     */
    private void createConstraintsSheet(Workbook workbook, List<ConstraintInfoRow> constraints,
                                       CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("约束信息");
        
        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"表名", "约束名称", "约束类型", "引用表", "引用字段", "级联规则", "备注"};
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], headerStyle);
        }
        
        // 数据
        if (constraints != null) {
            int rowNum = 1;
            for (ConstraintInfoRow constraint : constraints) {
                Row dataRow = sheet.createRow(rowNum++);
                setCell(dataRow, 0, constraint.getTableName(), dataStyle);
                setCell(dataRow, 1, constraint.getConstraintName(), dataStyle);
                setCell(dataRow, 2, constraint.getConstraintType(), dataStyle);
                setCell(dataRow, 3, constraint.getReferencedTable(), dataStyle);
                setCell(dataRow, 4, constraint.getReferencedColumn(), dataStyle);
                setCell(dataRow, 5, constraint.getCascadeRule(), dataStyle);
                setCell(dataRow, 6, constraint.getComment(), dataStyle);
            }
        }
        
        autoSizeColumns(sheet, headers.length);
    }
    
    /**
     * Sheet 6: 视图定义
     */
    private void createViewsSheet(Workbook workbook, List<ViewInfoRow> views,
                                  CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("视图定义");
        
        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"视图名称", "视图SQL定义", "备注"};
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], headerStyle);
        }
        
        // 数据
        if (views != null) {
            int rowNum = 1;
            for (ViewInfoRow view : views) {
                Row dataRow = sheet.createRow(rowNum++);
                setCell(dataRow, 0, view.getViewName(), dataStyle);
                setCell(dataRow, 1, view.getViewDefinition(), dataStyle);
                setCell(dataRow, 2, view.getComment(), dataStyle);
            }
        }
        
        autoSizeColumns(sheet, headers.length);
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 设置单元格值
     */
    private void setCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }
    
    /**
     * 自动调整列宽
     */
    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // 设置最小列宽和最大列宽
            int width = sheet.getColumnWidth(i);
            if (width < 10 * 256) {
                sheet.setColumnWidth(i, 10 * 256);
            } else if (width > 50 * 256) {
                sheet.setColumnWidth(i, 50 * 256);
            }
        }
    }
}
