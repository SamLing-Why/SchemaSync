package com.schemasync.formatter;

import com.schemasync.model.dict.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

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

    /**
     * 将数据字典转换为Excel字节数组
     * 
     * @param dictionary 数据字典
     * @return Excel文件字节数组
     */
    public byte[] format(SchemaDictionary dictionary) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 创建概览Sheet
            createOverviewSheet(workbook, dictionary, titleStyle, headerStyle, dataStyle);

            // 创建表详情Sheet
            if (dictionary.getTables() != null) {
                for (TableDefinition table : dictionary.getTables()) {
                    createTableDetailSheet(workbook, table, titleStyle, headerStyle, dataStyle);
                }
            }

            // 输出为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] result = outputStream.toByteArray();
            
            log.info("Excel导出成功, 大小: {} bytes, 表数量: {}", 
                    result.length, 
                    dictionary.getTables() != null ? dictionary.getTables().size() : 0);
            
            return result;
            
        } catch (IOException e) {
            log.error("Excel生成失败", e);
            throw new RuntimeException("Excel生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建概览Sheet
     */
    private void createOverviewSheet(Workbook workbook, SchemaDictionary dictionary,
                                     CellStyle titleStyle, CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("数据字典概览");
        
        int rowNum = 0;

        // 标题行
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("数据字典 - " + 
                (dictionary.getMetadata() != null ? dictionary.getMetadata().getDatabaseName() : ""));
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // 元数据信息
        if (dictionary.getMetadata() != null) {
            ExportMetadata metadata = dictionary.getMetadata();
            
            Row metaRow1 = sheet.createRow(rowNum++);
            setCell(metaRow1, 0, "数据库类型:", headerStyle);
            setCell(metaRow1, 1, metadata.getDatabaseType(), dataStyle);
            setCell(metaRow1, 3, "数据库名称:", headerStyle);
            setCell(metaRow1, 4, metadata.getDatabaseName(), dataStyle);

            Row metaRow2 = sheet.createRow(rowNum++);
            setCell(metaRow2, 0, "导出时间:", headerStyle);
            setCell(metaRow2, 1, metadata.getExportTime() != null ? 
                    metadata.getExportTime().toString() : "", dataStyle);
            setCell(metaRow2, 3, "工具版本:", headerStyle);
            setCell(metaRow2, 4, metadata.getToolVersion() != null ? 
                    metadata.getToolVersion() : "1.0.0", dataStyle);
        }

        // 空行
        rowNum++;

        // 表列表标题
        Row tableTitleRow = sheet.createRow(rowNum++);
        setCell(tableTitleRow, 0, "表列表", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

        // 表列表表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"序号", "表名", "表注释", "字段数", "索引数", "外键数"};
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], headerStyle);
        }

        // 表数据
        if (dictionary.getTables() != null) {
            int index = 1;
            for (TableDefinition table : dictionary.getTables()) {
                Row dataRow = sheet.createRow(rowNum++);
                setCell(dataRow, 0, index++, dataStyle);
                setCell(dataRow, 1, table.getTableName(), dataStyle);
                setCell(dataRow, 2, table.getTableComment() != null ? 
                        table.getTableComment() : "", dataStyle);
                setCell(dataRow, 3, table.getColumns() != null ? 
                        table.getColumns().size() : 0, dataStyle);
                setCell(dataRow, 4, table.getIndexes() != null ? 
                        table.getIndexes().size() : 0, dataStyle);
                setCell(dataRow, 5, table.getForeignKeys() != null ? 
                        table.getForeignKeys().size() : 0, dataStyle);
            }
        }

        // 自动调整列宽
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建表详情Sheet
     */
    private void createTableDetailSheet(Workbook workbook, TableDefinition table,
                                        CellStyle titleStyle, CellStyle headerStyle, CellStyle dataStyle) {
        // Sheet名称限制31个字符
        String sheetName = table.getTableName().length() > 31 ? 
                table.getTableName().substring(0, 31) : table.getTableName();
        Sheet sheet = workbook.createSheet(sheetName);

        int rowNum = 0;

        // 标题
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("表: " + table.getTableName() + 
                (table.getTableComment() != null ? " - " + table.getTableComment() : ""));
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        // 基本信息
        Row infoRow = sheet.createRow(rowNum++);
        setCell(infoRow, 0, "表名:", headerStyle);
        setCell(infoRow, 1, table.getTableName(), dataStyle);
        setCell(infoRow, 3, "注释:", headerStyle);
        setCell(infoRow, 4, table.getTableComment() != null ? 
                table.getTableComment() : "", dataStyle);

        // 空行
        rowNum++;

        // 字段信息标题
        Row fieldTitleRow = sheet.createRow(rowNum++);
        setCell(fieldTitleRow, 0, "字段信息", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 9));

        // 字段表头
        Row fieldHeaderRow = sheet.createRow(rowNum++);
        String[] fieldHeaders = {"序号", "字段名", "数据类型", "长度", "精度", "小数位", "允许NULL", "主键", "自增", "注释"};
        for (int i = 0; i < fieldHeaders.length; i++) {
            setCell(fieldHeaderRow, i, fieldHeaders[i], headerStyle);
        }

        // 字段数据
        if (table.getColumns() != null) {
            int index = 1;
            for (ColumnDefinition column : table.getColumns()) {
                Row dataRow = sheet.createRow(rowNum++);
                setCell(dataRow, 0, index++, dataStyle);
                setCell(dataRow, 1, column.getColumnName(), dataStyle);
                setCell(dataRow, 2, column.getDataType(), dataStyle);
                setCell(dataRow, 3, column.getLength() != null ? 
                        column.getLength() : "-", dataStyle);
                setCell(dataRow, 4, column.getPrecision() != null ? 
                        column.getPrecision() : "-", dataStyle);
                setCell(dataRow, 5, column.getScale() != null ? 
                        column.getScale() : "-", dataStyle);
                setCell(dataRow, 6, column.getNullable() != null && column.getNullable() ? 
                        "是" : "否", dataStyle);
                setCell(dataRow, 7, column.getIsPrimaryKey() != null && column.getIsPrimaryKey() ? 
                        "是" : "否", dataStyle);
                setCell(dataRow, 8, column.getIsAutoIncrement() != null && column.getIsAutoIncrement() ? 
                        "是" : "否", dataStyle);
                setCell(dataRow, 9, column.getComment() != null ? 
                        column.getComment() : "", dataStyle);
            }
        }

        // 索引信息(如果有)
        if (table.getIndexes() != null && !table.getIndexes().isEmpty()) {
            rowNum++;
            Row indexTitleRow = sheet.createRow(rowNum++);
            setCell(indexTitleRow, 0, "索引信息", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

            Row indexHeaderRow = sheet.createRow(rowNum++);
            String[] indexHeaders = {"序号", "索引名", "索引类型", "字段列表", "是否唯一", "注释"};
            for (int i = 0; i < indexHeaders.length; i++) {
                setCell(indexHeaderRow, i, indexHeaders[i], headerStyle);
            }

            int index = 1;
            for (IndexDefinition idx : table.getIndexes()) {
                Row dataRow = sheet.createRow(rowNum++);
                setCell(dataRow, 0, index++, dataStyle);
                setCell(dataRow, 1, idx.getIndexName(), dataStyle);
                setCell(dataRow, 2, idx.getIndexType(), dataStyle);
                setCell(dataRow, 3, idx.getColumns() != null ? 
                        String.join(", ", idx.getColumns()) : "", dataStyle);
                setCell(dataRow, 4, idx.getIsUnique() != null && idx.getIsUnique() ? 
                        "是" : "否", dataStyle);
                setCell(dataRow, 5, idx.getComment() != null ? 
                        idx.getComment() : "", dataStyle);
            }
        }

        // 自动调整列宽
        for (int i = 0; i < 10; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建标题样式
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
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
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        }
        cell.setCellStyle(style);
    }
}
