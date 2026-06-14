package com.schemasync.service;

import com.schemasync.formatter.ExcelFormatter;
import com.schemasync.model.dict.ColumnDefinition;
import com.schemasync.model.dict.SchemaDictionary;
import com.schemasync.model.dict.TableDefinition;
import com.schemasync.model.diff.ChangeType;
import com.schemasync.model.diff.SchemaChange;
import com.schemasync.model.diff.SchemaDiff;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 套表处理服务
 * 支持套表识别、还原导出、差异比对
 */
@Service
public class SetTableService {

    private static final Logger log = LoggerFactory.getLogger(SetTableService.class);

    // 套表后缀模式：_tp, _fo, _ar
    private static final Pattern SET_TABLE_PATTERN = Pattern.compile("^(.+)_(tp|fo|ar)$", Pattern.CASE_INSENSITIVE);

    // 套表保留优先级：fo > tp > ar
    private static final List<String> PRIORITY_ORDER = Arrays.asList("fo", "tp", "ar");

    @Autowired
    private ExcelFormatter excelFormatter;

    @Autowired
    private SchemaDictionaryParser parser;

    /**
     * 套表信息
     */
    public static class SetTableInfo {
        private String prefix;              // 套表前缀
        private List<TableDefinition> tables; // 套表中的所有表

        public SetTableInfo(String prefix) {
            this.prefix = prefix;
            this.tables = new ArrayList<>();
        }

        public String getPrefix() {
            return prefix;
        }

        public List<TableDefinition> getTables() {
            return tables;
        }

        public void addTable(TableDefinition table) {
            this.tables.add(table);
        }
    }

    /**
     * 从Excel文件解析数据字典
     */
    public SchemaDictionary parseExcel(byte[] excelData) throws Exception {
        java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(excelData);
        return parser.parseExcel(inputStream);
    }

    /**
     * 识别套表
     * 返回所有套表组（每组至少2张表）
     */
    public List<SetTableInfo> identifySetTables(SchemaDictionary dictionary) {
        if (dictionary == null || dictionary.getTables() == null) {
            return Collections.emptyList();
        }

        // 按前缀分组
        Map<String, SetTableInfo> prefixMap = new LinkedHashMap<>();

        for (TableDefinition table : dictionary.getTables()) {
            String tableName = table.getTableName();
            Matcher matcher = SET_TABLE_PATTERN.matcher(tableName);

            if (matcher.matches()) {
                String prefix = matcher.group(1);
                String suffix = matcher.group(2).toLowerCase();

                prefixMap.computeIfAbsent(prefix, SetTableInfo::new)
                        .addTable(table);
            }
        }

        // 只保留至少2张表的套表组
        return prefixMap.values().stream()
                .filter(info -> info.getTables().size() >= 2)
                .collect(Collectors.toList());
    }

    /**
     * 套表还原导出
     * 套表中按优先级保留一张表（fo > tp > ar）
     */
    public byte[] exportSetTableReduced(SchemaDictionary dictionary) {
        if (dictionary == null || dictionary.getTables() == null) {
            return excelFormatter.format(dictionary);
        }

        // 识别套表
        List<SetTableInfo> setTables = identifySetTables(dictionary);
        Set<String> setTableNames = setTables.stream()
                .flatMap(info -> info.getTables().stream())
                .map(TableDefinition::getTableName)
                .collect(Collectors.toSet());

        // 构建新的数据字典
        SchemaDictionary reducedDict = new SchemaDictionary();
        reducedDict.setMetadata(dictionary.getMetadata());
        reducedDict.setTables(new ArrayList<>());

        // 需要保留的表
        List<TableDefinition> tablesToKeep = new ArrayList<>();

        // 非套表：直接保留
        for (TableDefinition table : dictionary.getTables()) {
            if (!setTableNames.contains(table.getTableName())) {
                tablesToKeep.add(table);
            }
        }

        // 套表：按优先级保留一张
        for (SetTableInfo setTable : setTables) {
            TableDefinition keepTable = selectTableByPriority(setTable.getTables());
            if (keepTable != null) {
                tablesToKeep.add(keepTable);
                log.info("套表 [{}] 保留表: {}", setTable.getPrefix(), keepTable.getTableName());
            }
        }

        // 按表名排序
        tablesToKeep.sort(Comparator.comparing(TableDefinition::getTableName));
        reducedDict.setTables(tablesToKeep);

        log.info("套表还原导出: 原表数={}, 套表组数={}, 还原后表数={}",
                dictionary.getTables().size(), setTables.size(), reducedDict.getTables().size());

        return excelFormatter.format(reducedDict);
    }

    /**
     * 按优先级选择表：fo > tp > ar
     */
    private TableDefinition selectTableByPriority(List<TableDefinition> tables) {
        if (tables == null || tables.isEmpty()) {
            return null;
        }

        // 按后缀排序
        Map<String, TableDefinition> suffixMap = new HashMap<>();
        for (TableDefinition table : tables) {
            Matcher matcher = SET_TABLE_PATTERN.matcher(table.getTableName());
            if (matcher.matches()) {
                String suffix = matcher.group(2).toLowerCase();
                suffixMap.put(suffix, table);
            }
        }

        // 按优先级选择
        for (String suffix : PRIORITY_ORDER) {
            if (suffixMap.containsKey(suffix)) {
                return suffixMap.get(suffix);
            }
        }

        return tables.get(0);
    }

    /**
     * 套表差异比对
     * 生成套表内部的差异报告
     */
    public byte[] compareSetTables(SchemaDictionary dictionary) throws IOException {
        List<SetTableInfo> setTables = identifySetTables(dictionary);

        if (setTables.isEmpty()) {
            log.warn("未找到套表");
            return createEmptyDiffExcel();
        }

        // 收集所有差异
        List<DiffRecord> allDiffs = new ArrayList<>();

        for (SetTableInfo setTable : setTables) {
            List<TableDefinition> tables = setTable.getTables();

            // 选择基准表
            TableDefinition baseTable = selectTableByPriority(tables);
            if (baseTable == null) {
                continue;
            }

            log.info("套表 [{}] 差异比对，基准表: {}", setTable.getPrefix(), baseTable.getTableName());

            // 其他表与基准表比对
            for (TableDefinition table : tables) {
                if (table.getTableName().equals(baseTable.getTableName())) {
                    continue; // 跳过基准表自己
                }

                compareTwoTables(setTable.getPrefix(), baseTable, table, allDiffs);
            }
        }

        // 生成Excel差异报告
        return generateDiffExcel(allDiffs);
    }

    /**
     * 比对两张表的字段差异
     */
    private void compareTwoTables(String setTablePrefix,
                                   TableDefinition baseTable,
                                   TableDefinition compareTable,
                                   List<DiffRecord> allDiffs) {
        String baseName = baseTable.getTableName();
        String compareName = compareTable.getTableName();

        // 构建字段Map
        Map<String, ColumnDefinition> baseColumns = baseTable.getColumns() != null
                ? baseTable.getColumns().stream().collect(Collectors.toMap(ColumnDefinition::getColumnName, c -> c))
                : Collections.emptyMap();

        Map<String, ColumnDefinition> compareColumns = compareTable.getColumns() != null
                ? compareTable.getColumns().stream().collect(Collectors.toMap(ColumnDefinition::getColumnName, c -> c))
                : Collections.emptyMap();

        // 检查缺失字段（基准表有，对比表没有）
        for (Map.Entry<String, ColumnDefinition> entry : baseColumns.entrySet()) {
            if (!compareColumns.containsKey(entry.getKey())) {
                allDiffs.add(new DiffRecord(
                        setTablePrefix,
                        compareName,
                        entry.getKey(),
                        "字段缺失",
                        "HIGH",
                        entry.getValue().getDataType(),
                        "",
                        entry.getValue().getLength() != null ? entry.getValue().getLength().toString() : "",
                        "",
                        entry.getValue().getPrecision() != null ? entry.getValue().getPrecision().toString() : "",
                        "",
                        "字段在基准表 " + baseName + " 中存在，但在 " + compareName + " 中缺失"
                ));
            }
        }

        // 检查多余字段（对比表有，基准表没有）
        for (Map.Entry<String, ColumnDefinition> entry : compareColumns.entrySet()) {
            if (!baseColumns.containsKey(entry.getKey())) {
                allDiffs.add(new DiffRecord(
                        setTablePrefix,
                        compareName,
                        entry.getKey(),
                        "字段多余",
                        "MEDIUM",
                        "",
                        entry.getValue().getDataType(),
                        "",
                        entry.getValue().getLength() != null ? entry.getValue().getLength().toString() : "",
                        "",
                        entry.getValue().getPrecision() != null ? entry.getValue().getPrecision().toString() : "",
                        "字段在 " + compareName + " 中存在，但在基准表 " + baseName + " 中不存在"
                ));
            }
        }

        // 检查共有字段的差异
        for (String columnName : baseColumns.keySet()) {
            if (compareColumns.containsKey(columnName)) {
                ColumnDefinition baseCol = baseColumns.get(columnName);
                ColumnDefinition compareCol = compareColumns.get(columnName);

                // 类型对比
                if (!Objects.equals(baseCol.getDataType(), compareCol.getDataType())) {
                    allDiffs.add(new DiffRecord(
                            setTablePrefix,
                            compareName,
                            columnName,
                            "字段类型不一致",
                            "HIGH",
                            baseCol.getDataType(),
                            compareCol.getDataType(),
                            "",
                            "",
                            "",
                            "",
                            String.format("基准表: %s, 对比表: %s",
                                    baseCol.getDataType(), compareCol.getDataType())
                    ));
                }

                // 长度对比
                if (!Objects.equals(baseCol.getLength(), compareCol.getLength())) {
                    allDiffs.add(new DiffRecord(
                            setTablePrefix,
                            compareName,
                            columnName,
                            "字段长度不一致",
                            "MEDIUM",
                            "",
                            "",
                            baseCol.getLength() != null ? baseCol.getLength().toString() : "",
                            compareCol.getLength() != null ? compareCol.getLength().toString() : "",
                            "",
                            "",
                            String.format("基准表: %s, 对比表: %s",
                                    baseCol.getLength(), compareCol.getLength())
                    ));
                }

                // 精度对比
                if (!Objects.equals(baseCol.getPrecision(), compareCol.getPrecision())) {
                    allDiffs.add(new DiffRecord(
                            setTablePrefix,
                            compareName,
                            columnName,
                            "字段精度不一致",
                            "MEDIUM",
                            "",
                            "",
                            "",
                            "",
                            baseCol.getPrecision() != null ? baseCol.getPrecision().toString() : "",
                            compareCol.getPrecision() != null ? compareCol.getPrecision().toString() : "",
                            String.format("基准表: %s, 对比表: %s",
                                    baseCol.getPrecision(), compareCol.getPrecision())
                    ));
                }
            }
        }
    }

    /**
     * 差异记录
     */
    private static class DiffRecord {
        String setTablePrefix;      // 套表前缀
        String tableName;           // 表名
        String columnName;          // 字段名
        String diffType;            // 差异类型
        String severity;            // 严重程度
        String baseDataType;        // 基准表数据类型
        String compareDataType;     // 对比表数据类型
        String baseLength;          // 基准表长度
        String compareLength;       // 对比表长度
        String basePrecision;       // 基准表精度
        String comparePrecision;    // 对比表精度
        String details;             // 详情

        public DiffRecord(String setTablePrefix, String tableName, String columnName,
                         String diffType, String severity,
                         String baseDataType, String compareDataType,
                         String baseLength, String compareLength,
                         String basePrecision, String comparePrecision,
                         String details) {
            this.setTablePrefix = setTablePrefix;
            this.tableName = tableName;
            this.columnName = columnName;
            this.diffType = diffType;
            this.severity = severity;
            this.baseDataType = baseDataType;
            this.compareDataType = compareDataType;
            this.baseLength = baseLength;
            this.compareLength = compareLength;
            this.basePrecision = basePrecision;
            this.comparePrecision = comparePrecision;
            this.details = details;
        }
    }

    /**
     * 生成差异Excel报告
     */
    private byte[] generateDiffExcel(List<DiffRecord> diffs) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("套表差异分析");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"套表前缀", "表名", "字段名", "差异类型", "严重程度",
                    "基准值(数据类型)", "对比值(数据类型)",
                    "基准值(长度)", "对比值(长度)",
                    "基准值(精度)", "对比值(精度)",
                    "详情"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            CellStyle dataStyle = workbook.createCellStyle();
            int rowNum = 1;
            for (DiffRecord diff : diffs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(diff.setTablePrefix);
                row.createCell(1).setCellValue(diff.tableName);
                row.createCell(2).setCellValue(diff.columnName);
                row.createCell(3).setCellValue(diff.diffType);
                row.createCell(4).setCellValue(diff.severity);
                row.createCell(5).setCellValue(diff.baseDataType != null ? diff.baseDataType : "");
                row.createCell(6).setCellValue(diff.compareDataType != null ? diff.compareDataType : "");
                row.createCell(7).setCellValue(diff.baseLength != null ? diff.baseLength : "");
                row.createCell(8).setCellValue(diff.compareLength != null ? diff.compareLength : "");
                row.createCell(9).setCellValue(diff.basePrecision != null ? diff.basePrecision : "");
                row.createCell(10).setCellValue(diff.comparePrecision != null ? diff.comparePrecision : "");
                row.createCell(11).setCellValue(diff.details != null ? diff.details : "");
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                if (width > 50 * 256) {
                    sheet.setColumnWidth(i, 50 * 256);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 创建空的差异Excel（未找到套表时）
     */
    private byte[] createEmptyDiffExcel() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("套表差异分析");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("套表差异分析结果");
            headerRow.getCell(0).setCellStyle(headerStyle);

            Row messageRow = sheet.createRow(1);
            messageRow.createCell(0).setCellValue("未找到套表（套表需要至少2张具有相同前缀且后缀为_tp/_fo/_ar的表）");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
