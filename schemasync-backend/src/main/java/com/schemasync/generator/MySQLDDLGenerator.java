package com.schemasync.generator;

import com.schemasync.model.diff.*;
import com.schemasync.model.dict.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MySQL DDL生成器
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Component
public class MySQLDDLGenerator implements DDLGenerator {

    private static final Logger log = LoggerFactory.getLogger(MySQLDDLGenerator.class);

    @Override
    public String generate(SchemaDiff diff, GenerationOptions options) {
        log.info("开始生成MySQL DDL脚本");

        StringBuilder ddl = new StringBuilder();

        // 文件头注释
        ddl.append(generateHeader(diff, options));

        // 事务开始
        if (options.getUseTransaction()) {
            ddl.append("START TRANSACTION;\n\n");
        }

        // 1. 新增表
        ddl.append(generateCreateTables(diff));

        // 2. 修改表结构
        ddl.append(generateAlterTables(diff, options));

        // 3. 索引变更
        ddl.append(generateIndexChanges(diff));

        // 4. 外键约束
        ddl.append(generateForeignKeyChanges(diff));

        // 5. 删除表(最后执行)
        ddl.append(generateDropTables(diff, options));

        // 事务提交
        if (options.getUseTransaction()) {
            ddl.append("\nCOMMIT;\n");
        }

        ddl.append("\n-- ============================================\n");
        ddl.append("-- 执行完成后请验证:\n");
        ddl.append("-- 1. 检查表结构是否正确\n");
        ddl.append("-- 2. 检查数据是否完整\n");
        ddl.append("-- 3. 检查应用功能是否正常\n");
        ddl.append("-- ============================================\n");

        log.info("差异化DDL脚本生成完成, 长度: {} 字符", ddl.length());
        return ddl.toString();
    }

    @Override
    public String generateRollback(SchemaDiff diff) {
        log.info("开始生成回滚脚本");

        StringBuilder ddl = new StringBuilder();
        ddl.append("-- ============================================\n");
        ddl.append("-- 回滚脚本\n");
        ddl.append("-- 生成时间: " + new Date() + "\n");
        ddl.append("-- 警告: 请仔细检查后再执行\n");
        ddl.append("-- ============================================\n\n");

        ddl.append("START TRANSACTION;\n\n");

        // 回滚顺序与正相反: 先删除新增的,再恢复删除的

        // 1. 删除新增的表
        for (SchemaChange change : diff.getChanges()) {
            if (change.getChangeType() == ChangeType.TABLE_ADD) {
                ddl.append("-- 回滚: 删除新增的表 ").append(change.getTableName()).append("\n");
                ddl.append("DROP TABLE IF EXISTS `").append(change.getTableName()).append("`;\n\n");
            }
        }

        // 2. 恢复删除的表(需要原始表定义,这里只能注释提示)
        for (SchemaChange change : diff.getChanges()) {
            if (change.getChangeType() == ChangeType.TABLE_DROP) {
                ddl.append("-- [手动操作] 恢复删除的表: ").append(change.getTableName()).append("\n");
                ddl.append("-- 需要从备份中恢复此表\n\n");
            }
        }

        ddl.append("\nCOMMIT;\n");

        return ddl.toString();
    }

    @Override
    public String getDatabaseType() {
        return "MYSQL";
    }

    /**
     * 生成文件头
     */
    private String generateHeader(SchemaDiff diff, GenerationOptions options) {
        StringBuilder header = new StringBuilder();
        header.append("-- ============================================\n");
        header.append("-- SchemaSync 自动生成的数据库变更脚本\n");
        if (options.getSourceVersion() != null) {
            header.append("-- 源版本: ").append(options.getSourceVersion()).append("\n");
        }
        if (options.getTargetVersion() != null) {
            header.append("-- 目标版本: ").append(options.getTargetVersion()).append("\n");
        }
        header.append("-- 生成时间: ").append(new Date()).append("\n");
        header.append("-- 数据库类型: MySQL\n");

        if (diff.getSummary() != null) {
            DiffSummary summary = diff.getSummary();
            header.append("-- 变更统计: ");
            header.append("新增").append(summary.getTablesAdded() != null ? summary.getTablesAdded() : 0).append("表, ");
            header.append("删除").append(summary.getTablesDropped() != null ? summary.getTablesDropped() : 0).append("表, ");
            header.append("修改").append(summary.getTablesModified() != null ? summary.getTablesModified() : 0).append("表\n");
            
            if (summary.getBreakingChanges() != null && summary.getBreakingChanges() > 0) {
                header.append("-- 破坏性变更: ").append(summary.getBreakingChanges()).append("处(请仔细审查)\n");
            }
        }

        header.append("-- ============================================\n\n");
        header.append("-- 建议在非生产环境先执行测试\n");
        header.append("-- 执行前请备份数据库\n\n");

        return header.toString();
    }

    /**
     * 生成CREATE TABLE语句
     */
    private String generateCreateTables(SchemaDiff diff) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("-- ============================================\n");
        ddl.append("-- 1. 新增表\n");
        ddl.append("-- ============================================\n\n");

        List<SchemaChange> tableAdds = diff.getChanges().stream()
                .filter(c -> c.getChangeType() == ChangeType.TABLE_ADD)
                .collect(Collectors.toList());

        if (tableAdds.isEmpty()) {
            ddl.append("-- 无新增表\n\n");
            return ddl.toString();
        }

        for (SchemaChange change : tableAdds) {
            ddl.append("-- 新增表: ").append(change.getTableName()).append("\n");
            // 实际应该从完整的新版本数据字典中获取表定义
            // 这里生成占位符
            ddl.append("-- [TODO] 需要从新版本数据字典中获取完整表定义\n");
            ddl.append("-- CREATE TABLE `").append(change.getTableName()).append("` (...);\n\n");
        }

        return ddl.toString();
    }

    /**
     * 生成ALTER TABLE语句
     */
    private String generateAlterTables(SchemaDiff diff, GenerationOptions options) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("-- ============================================\n");
        ddl.append("-- 2. 修改表结构\n");
        ddl.append("-- ============================================\n\n");

        // 字段相关变更
        List<SchemaChange> columnChanges = diff.getChanges().stream()
                .filter(c -> c.getChangeType().name().startsWith("COLUMN_"))
                .collect(Collectors.toList());

        if (columnChanges.isEmpty()) {
            ddl.append("-- 无表结构变更\n\n");
            return ddl.toString();
        }

        // 按表分组
        Map<String, List<SchemaChange>> changesByTable = columnChanges.stream()
                .collect(Collectors.groupingBy(SchemaChange::getTableName));

        for (Map.Entry<String, List<SchemaChange>> entry : changesByTable.entrySet()) {
            String tableName = entry.getKey();
            List<SchemaChange> changes = entry.getValue();

            ddl.append("-- 表: ").append(tableName).append("\n");

            for (SchemaChange change : changes) {
                boolean isBreaking = change.getSeverity() == Severity.BREAKING;

                if (isBreaking && options.getCommentBreakingChanges()) {
                    ddl.append("-- [警告: 破坏性变更] ");
                }

                switch (change.getChangeType()) {
                    case COLUMN_ADD:
                        ddl.append("ALTER TABLE `").append(tableName)
                           .append("` ADD COLUMN `").append(change.getColumnName())
                           .append("` ...;  -- [TODO] 补充字段定义\n");
                        break;

                    case COLUMN_DROP:
                        String dropStmt = "ALTER TABLE `" + tableName + "` DROP COLUMN `" + 
                                         change.getColumnName() + "`;";
                        if (isBreaking && options.getCommentBreakingChanges()) {
                            ddl.append("-- ").append(dropStmt)
                               .append("  -- 请手动取消注释确认\n");
                        } else {
                            ddl.append(dropStmt).append("\n");
                        }
                        break;

                    case COLUMN_MODIFY:
                        ddl.append("ALTER TABLE `").append(tableName)
                           .append("` MODIFY COLUMN `").append(change.getColumnName())
                           .append("` ...;  -- [TODO] 补充字段定义\n");
                        break;
                }

                if (isBreaking && change.getDetails() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> details = (Map<String, Object>) change.getDetails();
                    if (details.containsKey("impact")) {
                        ddl.append("-- 影响: ").append(details.get("impact")).append("\n");
                    }
                }
                ddl.append("\n");
            }
        }

        return ddl.toString();
    }

    /**
     * 生成索引变更
     */
    private String generateIndexChanges(SchemaDiff diff) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("-- ============================================\n");
        ddl.append("-- 3. 索引变更\n");
        ddl.append("-- ============================================\n\n");

        List<SchemaChange> indexChanges = diff.getChanges().stream()
                .filter(c -> c.getChangeType().name().startsWith("INDEX_"))
                .collect(Collectors.toList());

        if (indexChanges.isEmpty()) {
            ddl.append("-- 无索引变更\n\n");
            return ddl.toString();
        }

        for (SchemaChange change : indexChanges) {
            switch (change.getChangeType()) {
                case INDEX_ADD:
                    ddl.append("-- 新增索引\n");
                    ddl.append("-- [TODO] CREATE INDEX ... \n\n");
                    break;

                case INDEX_DROP:
                    ddl.append("-- 删除索引\n");
                    ddl.append("-- [TODO] DROP INDEX ... \n\n");
                    break;
            }
        }

        return ddl.toString();
    }

    /**
     * 生成外键变更
     */
    private String generateForeignKeyChanges(SchemaDiff diff) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("-- ============================================\n");
        ddl.append("-- 4. 外键约束\n");
        ddl.append("-- ============================================\n\n");

        List<SchemaChange> fkChanges = diff.getChanges().stream()
                .filter(c -> c.getChangeType().name().startsWith("FOREIGN_KEY_"))
                .collect(Collectors.toList());

        if (fkChanges.isEmpty()) {
            ddl.append("-- 无外键变更\n\n");
            return ddl.toString();
        }

        for (SchemaChange change : fkChanges) {
            switch (change.getChangeType()) {
                case FOREIGN_KEY_ADD:
                    ddl.append("-- 新增外键\n");
                    ddl.append("-- [TODO] ALTER TABLE ... ADD CONSTRAINT ... \n\n");
                    break;

                case FOREIGN_KEY_DROP:
                    ddl.append("-- 删除外键\n");
                    ddl.append("-- [TODO] ALTER TABLE ... DROP FOREIGN KEY ... \n\n");
                    break;
            }
        }

        return ddl.toString();
    }

    /**
     * 生成DROP TABLE语句
     */
    private String generateDropTables(SchemaDiff diff, GenerationOptions options) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("-- ============================================\n");
        ddl.append("-- 5. 删除表(最后执行)\n");
        ddl.append("-- ============================================\n\n");

        List<SchemaChange> tableDrops = diff.getChanges().stream()
                .filter(c -> c.getChangeType() == ChangeType.TABLE_DROP)
                .collect(Collectors.toList());

        if (tableDrops.isEmpty()) {
            ddl.append("-- 无删除表\n\n");
            return ddl.toString();
        }

        for (SchemaChange change : tableDrops) {
            String dropStmt = "DROP TABLE IF EXISTS `" + change.getTableName() + "`;";
            
            if (options.getCommentBreakingChanges()) {
                ddl.append("-- [警告: 破坏性变更] 删除表: ").append(change.getTableName()).append("\n");
                ddl.append("-- 影响: 表及其所有数据将被永久删除\n");
                ddl.append("-- ").append(dropStmt).append("  -- 请手动取消注释确认\n\n");
            } else {
                ddl.append("-- 删除表: ").append(change.getTableName()).append("\n");
                ddl.append(dropStmt).append("\n\n");
            }
        }

        return ddl.toString();
    }
}
