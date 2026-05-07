package com.schemasync.differ;

import com.schemasync.model.diff.*;
import com.schemasync.model.dict.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 默认的数据字典对比器实现
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Component
public class DefaultSchemaDiffer implements SchemaDiffer {

    private static final Logger log = LoggerFactory.getLogger(DefaultSchemaDiffer.class);

    @Override
    public SchemaDiff compare(SchemaDictionary oldDict, SchemaDictionary newDict) {
        log.info("开始对比数据字典");

        SchemaDiff diff = new SchemaDiff();
        List<SchemaChange> changes = new ArrayList<>();

        // 1. 对比表
        compareTables(
                oldDict.getTables(),
                newDict.getTables(),
                changes
        );

        // 2. 生成统计
        DiffSummary summary = generateSummary(changes);

        // 3. 设置元数据
        DiffMetadata metadata = new DiffMetadata();
        metadata.setGeneratedTime(new Date());
        metadata.setToolVersion("1.0.0");

        // 4. 组装差异对象
        diff.setDiffMetadata(metadata);
        diff.setSummary(summary);
        diff.setChanges(changes);

        log.info("对比完成, 共发现{}处变更", changes.size());
        return diff;
    }

    /**
     * 对比表列表
     */
    private void compareTables(List<TableDefinition> oldTables,
                               List<TableDefinition> newTables,
                               List<SchemaChange> changes) {
        if (oldTables == null) oldTables = Collections.emptyList();
        if (newTables == null) newTables = Collections.emptyList();

        Map<String, TableDefinition> oldTableMap = oldTables.stream()
                .collect(Collectors.toMap(TableDefinition::getTableName, t -> t, (a, b) -> a));

        Map<String, TableDefinition> newTableMap = newTables.stream()
                .collect(Collectors.toMap(TableDefinition::getTableName, t -> t, (a, b) -> a));

        // 新增的表
        for (String tableName : newTableMap.keySet()) {
            if (!oldTableMap.containsKey(tableName)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.TABLE_ADD)
                        .tableName(tableName)
                        .severity(Severity.NON_BREAKING)
                        .details(Map.of(
                                "tableComment", newTableMap.get(tableName).getTableComment(),
                                "columns", newTableMap.get(tableName).getColumns().size(),
                                "indexes", newTableMap.get(tableName).getIndexes() != null ? 
                                        newTableMap.get(tableName).getIndexes().size() : 0
                        ))
                        .build());
                log.debug("发现新增表: {}", tableName);
            }
        }

        // 删除的表
        for (String tableName : oldTableMap.keySet()) {
            if (!newTableMap.containsKey(tableName)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.TABLE_DROP)
                        .tableName(tableName)
                        .severity(Severity.BREAKING)
                        .details(Map.of(
                                "tableComment", oldTableMap.get(tableName).getTableComment()
                        ))
                        .build());
                log.debug("发现删除表: {}", tableName);
            }
        }

        // 修改的表 - 对比字段、索引、外键
        for (String tableName : oldTableMap.keySet()) {
            if (newTableMap.containsKey(tableName)) {
                compareTableDetails(
                        oldTableMap.get(tableName),
                        newTableMap.get(tableName),
                        changes
                );
            }
        }
    }

    /**
     * 对比表详细信息
     */
    private void compareTableDetails(TableDefinition oldTable,
                                     TableDefinition newTable,
                                     List<SchemaChange> changes) {
        String tableName = oldTable.getTableName();

        // 对比字段
        compareColumns(
                oldTable.getColumns(),
                newTable.getColumns(),
                changes,
                tableName
        );

        // 对比索引
        compareIndexes(
                oldTable.getIndexes(),
                newTable.getIndexes(),
                changes,
                tableName
        );

        // 对比外键
        compareForeignKeys(
                oldTable.getForeignKeys(),
                newTable.getForeignKeys(),
                changes,
                tableName
        );
    }

    /**
     * 对比字段列表
     */
    private void compareColumns(List<ColumnDefinition> oldColumns,
                                List<ColumnDefinition> newColumns,
                                List<SchemaChange> changes,
                                String tableName) {
        if (oldColumns == null) oldColumns = Collections.emptyList();
        if (newColumns == null) newColumns = Collections.emptyList();

        Map<String, ColumnDefinition> oldColumnMap = oldColumns.stream()
                .collect(Collectors.toMap(ColumnDefinition::getColumnName, c -> c, (a, b) -> a));

        Map<String, ColumnDefinition> newColumnMap = newColumns.stream()
                .collect(Collectors.toMap(ColumnDefinition::getColumnName, c -> c, (a, b) -> a));

        // 新增字段
        for (String columnName : newColumnMap.keySet()) {
            if (!oldColumnMap.containsKey(columnName)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.COLUMN_ADD)
                        .tableName(tableName)
                        .columnName(columnName)
                        .severity(Severity.NON_BREAKING)
                        .build());
                log.debug("表{} 新增字段: {}", tableName, columnName);
            }
        }

        // 删除字段
        for (String columnName : oldColumnMap.keySet()) {
            if (!newColumnMap.containsKey(columnName)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.COLUMN_DROP)
                        .tableName(tableName)
                        .columnName(columnName)
                        .severity(Severity.BREAKING)
                        .details(Map.of(
                                "oldDefinition", oldColumnMap.get(columnName)
                        ))
                        .build());
                log.debug("表{} 删除字段: {}", tableName, columnName);
            }
        }

        // 修改字段
        for (String columnName : oldColumnMap.keySet()) {
            if (newColumnMap.containsKey(columnName)) {
                compareColumnDetail(
                        oldColumnMap.get(columnName),
                        newColumnMap.get(columnName),
                        changes,
                        tableName,
                        columnName
                );
            }
        }
    }

    /**
     * 对比单个字段的属性变更
     */
    private void compareColumnDetail(ColumnDefinition oldCol,
                                     ColumnDefinition newCol,
                                     List<SchemaChange> changes,
                                     String tableName,
                                     String columnName) {
        // 对比数据类型
        if (!Objects.equals(oldCol.getDataType(), newCol.getDataType())) {
            changes.add(SchemaChange.builder()
                    .changeType(ChangeType.COLUMN_MODIFY)
                    .tableName(tableName)
                    .columnName(columnName)
                    .severity(Severity.BREAKING)
                    .details(Map.of(
                            "property", "dataType",
                            "oldValue", oldCol.getDataType(),
                            "newValue", newCol.getDataType(),
                            "impact", "数据类型变更,可能导致数据转换失败"
                    ))
                    .build());
        }

        // 对比长度
        if (!Objects.equals(oldCol.getLength(), newCol.getLength())) {
            Severity severity = isLengthDecreased(oldCol.getLength(), newCol.getLength())
                    ? Severity.BREAKING : Severity.NON_BREAKING;
            changes.add(SchemaChange.builder()
                    .changeType(ChangeType.COLUMN_MODIFY)
                    .tableName(tableName)
                    .columnName(columnName)
                    .severity(severity)
                    .details(Map.of(
                            "property", "length",
                            "oldValue", oldCol.getLength(),
                            "newValue", newCol.getLength(),
                            "impact", severity == Severity.BREAKING ? "长度缩小,可能导致数据截断" : "长度扩展"
                    ))
                    .build());
        }

        // 对比精度
        if (!Objects.equals(oldCol.getPrecision(), newCol.getPrecision())) {
            changes.add(SchemaChange.builder()
                    .changeType(ChangeType.COLUMN_MODIFY)
                    .tableName(tableName)
                    .columnName(columnName)
                    .severity(Severity.BREAKING)
                    .details(Map.of(
                            "property", "precision",
                            "oldValue", oldCol.getPrecision(),
                            "newValue", newCol.getPrecision()
                    ))
                    .build());
        }

        // 对比小数位
        if (!Objects.equals(oldCol.getScale(), newCol.getScale())) {
            changes.add(SchemaChange.builder()
                    .changeType(ChangeType.COLUMN_MODIFY)
                    .tableName(tableName)
                    .columnName(columnName)
                    .severity(Severity.BREAKING)
                    .details(Map.of(
                            "property", "scale",
                            "oldValue", oldCol.getScale(),
                            "newValue", newCol.getScale()
                    ))
                    .build());
        }

        // 对比NULL约束
        if (!Objects.equals(oldCol.getNullable(), newCol.getNullable())) {
            Severity severity = Boolean.FALSE.equals(newCol.getNullable())
                    ? Severity.BREAKING : Severity.NON_BREAKING;
            changes.add(SchemaChange.builder()
                    .changeType(ChangeType.COLUMN_MODIFY)
                    .tableName(tableName)
                    .columnName(columnName)
                    .severity(severity)
                    .details(Map.of(
                            "property", "nullable",
                            "oldValue", oldCol.getNullable(),
                            "newValue", newCol.getNullable(),
                            "impact", severity == Severity.BREAKING ? "添加NOT NULL约束,需要处理现有NULL值" : "移除NOT NULL约束"
                    ))
                    .build());
        }

        // 对比默认值
        if (!Objects.equals(oldCol.getDefaultValue(), newCol.getDefaultValue())) {
            changes.add(SchemaChange.builder()
                    .changeType(ChangeType.COLUMN_MODIFY)
                    .tableName(tableName)
                    .columnName(columnName)
                    .severity(Severity.NON_BREAKING)
                    .details(Map.of(
                            "property", "defaultValue",
                            "oldValue", oldCol.getDefaultValue(),
                            "newValue", newCol.getDefaultValue()
                    ))
                    .build());
        }

        // 对比注释
        if (!Objects.equals(oldCol.getComment(), newCol.getComment())) {
            changes.add(SchemaChange.builder()
                    .changeType(ChangeType.COLUMN_MODIFY)
                    .tableName(tableName)
                    .columnName(columnName)
                    .severity(Severity.NON_BREAKING)
                    .details(Map.of(
                            "property", "comment",
                            "oldValue", oldCol.getComment(),
                            "newValue", newCol.getComment()
                    ))
                    .build());
        }
    }

    /**
     * 对比索引列表
     */
    private void compareIndexes(List<IndexDefinition> oldIndexes,
                                List<IndexDefinition> newIndexes,
                                List<SchemaChange> changes,
                                String tableName) {
        if (oldIndexes == null) oldIndexes = Collections.emptyList();
        if (newIndexes == null) newIndexes = Collections.emptyList();

        Map<String, IndexDefinition> oldIndexMap = oldIndexes.stream()
                .collect(Collectors.toMap(IndexDefinition::getIndexName, i -> i, (a, b) -> a));

        Map<String, IndexDefinition> newIndexMap = newIndexes.stream()
                .collect(Collectors.toMap(IndexDefinition::getIndexName, i -> i, (a, b) -> a));

        // 新增索引
        for (String indexName : newIndexMap.keySet()) {
            if (!oldIndexMap.containsKey(indexName)) {
                IndexDefinition newIndex = newIndexMap.get(indexName);
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.INDEX_ADD)
                        .tableName(tableName)
                        .severity(Severity.NON_BREAKING)
                        .details(newIndex)
                        .build());
                log.debug("表{} 新增索引: {}", tableName, indexName);
            }
        }

        // 删除索引
        for (String indexName : oldIndexMap.keySet()) {
            if (!newIndexMap.containsKey(indexName)) {
                IndexDefinition oldIndex = oldIndexMap.get(indexName);
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.INDEX_DROP)
                        .tableName(tableName)
                        .severity(Severity.NON_BREAKING)
                        .details(oldIndex)
                        .build());
                log.debug("表{} 删除索引: {}", tableName, indexName);
            }
        }
        
        // 修改索引（对比同名索引的定义）
        for (String indexName : newIndexMap.keySet()) {
            if (oldIndexMap.containsKey(indexName)) {
                IndexDefinition oldIndex = oldIndexMap.get(indexName);
                IndexDefinition newIndex = newIndexMap.get(indexName);
                
                // 对比索引定义是否变化
                if (!Objects.equals(oldIndex.getIndexType(), newIndex.getIndexType()) ||
                    !Objects.equals(oldIndex.getIsUnique(), newIndex.getIsUnique()) ||
                    !Objects.equals(oldIndex.getColumns(), newIndex.getColumns())) {
                    changes.add(SchemaChange.builder()
                            .changeType(ChangeType.INDEX_MODIFY)
                            .tableName(tableName)
                            .severity(Severity.NON_BREAKING)
                            .details(Map.of(
                                    "indexName", indexName,
                                    "oldValue", oldIndex,
                                    "newValue", newIndex
                            ))
                            .build());
                    log.debug("表{} 修改索引: {}", tableName, indexName);
                }
            }
        }
    }

    /**
     * 对比外键列表
     */
    private void compareForeignKeys(List<ForeignKeyDefinition> oldForeignKeys,
                                    List<ForeignKeyDefinition> newForeignKeys,
                                    List<SchemaChange> changes,
                                    String tableName) {
        if (oldForeignKeys == null) oldForeignKeys = Collections.emptyList();
        if (newForeignKeys == null) newForeignKeys = Collections.emptyList();

        Map<String, ForeignKeyDefinition> oldFkMap = oldForeignKeys.stream()
                .collect(Collectors.toMap(ForeignKeyDefinition::getConstraintName, f -> f, (a, b) -> a));

        Map<String, ForeignKeyDefinition> newFkMap = newForeignKeys.stream()
                .collect(Collectors.toMap(ForeignKeyDefinition::getConstraintName, f -> f, (a, b) -> a));

        // 新增外键
        for (String fkName : newFkMap.keySet()) {
            if (!oldFkMap.containsKey(fkName)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.FOREIGN_KEY_ADD)
                        .tableName(tableName)
                        .severity(Severity.NON_BREAKING)
                        .build());
            }
        }

        // 删除外键
        for (String fkName : oldFkMap.keySet()) {
            if (!newFkMap.containsKey(fkName)) {
                changes.add(SchemaChange.builder()
                        .changeType(ChangeType.FOREIGN_KEY_DROP)
                        .tableName(tableName)
                        .severity(Severity.NON_BREAKING)
                        .build());
            }
        }
    }

    /**
     * 生成差异统计
     */
    private DiffSummary generateSummary(List<SchemaChange> changes) {
        DiffSummary summary = new DiffSummary();

        summary.setTablesAdded(countChanges(changes, ChangeType.TABLE_ADD));
        summary.setTablesDropped(countChanges(changes, ChangeType.TABLE_DROP));
        summary.setTablesModified(countTableModifications(changes));

        summary.setColumnsAdded(countChanges(changes, ChangeType.COLUMN_ADD));
        summary.setColumnsDropped(countChanges(changes, ChangeType.COLUMN_DROP));
        summary.setColumnsModified(countColumnModifications(changes));

        summary.setIndexesAdded(countChanges(changes, ChangeType.INDEX_ADD));
        summary.setIndexesDropped(countChanges(changes, ChangeType.INDEX_DROP));

        summary.setForeignKeysAdded(countChanges(changes, ChangeType.FOREIGN_KEY_ADD));
        summary.setForeignKeysDropped(countChanges(changes, ChangeType.FOREIGN_KEY_DROP));

        summary.setBreakingChanges((int) changes.stream()
                .filter(c -> c.getSeverity() == Severity.BREAKING)
                .count());

        return summary;
    }

    /**
     * 统计指定类型的变更数量
     */
    private int countChanges(List<SchemaChange> changes, ChangeType type) {
        return (int) changes.stream()
                .filter(c -> c.getChangeType() == type)
                .count();
    }

    /**
     * 统计表修改数量
     */
    private int countTableModifications(List<SchemaChange> changes) {
        return (int) changes.stream()
                .filter(c -> c.getChangeType() == ChangeType.TABLE_MODIFY)
                .map(SchemaChange::getTableName)
                .distinct()
                .count();
    }

    /**
     * 统计字段修改数量
     */
    private int countColumnModifications(List<SchemaChange> changes) {
        return (int) changes.stream()
                .filter(c -> c.getChangeType() == ChangeType.COLUMN_MODIFY)
                .map(c -> c.getTableName() + "." + c.getColumnName())
                .distinct()
                .count();
    }

    /**
     * 判断长度是否缩小
     */
    private boolean isLengthDecreased(Long oldLength, Long newLength) {
        if (oldLength == null || newLength == null) {
            return false;
        }
        return newLength < oldLength;
    }
}
