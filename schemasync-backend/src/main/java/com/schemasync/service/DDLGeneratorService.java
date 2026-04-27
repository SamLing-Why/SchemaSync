package com.schemasync.service;

import com.schemasync.generator.DDLGenerator;
import com.schemasync.generator.GenerationOptions;
import com.schemasync.model.diff.SchemaDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DDL生成服务
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Service
public class DDLGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DDLGeneratorService.class);

    @Autowired
    private List<DDLGenerator> generators;

    /**
     * 生成DDL脚本
     * 
     * @param diff 差异对象
     * @param options 生成选项
     * @return DDL脚本
     */
    public String generateDDL(SchemaDiff diff, GenerationOptions options) {
        log.info("开始生成DDL脚本");

        // 获取对应数据库的生成器
        DDLGenerator generator = getGenerator(options.getDatabaseType());
        
        // 生成DDL
        String ddl = generator.generate(diff, options);

        // 如果需要回滚脚本,追加到后面
        if (options.getIncludeRollback()) {
            String rollback = generator.generateRollback(diff);
            ddl += "\n\n" + rollback;
        }

        log.info("DDL脚本生成完成, 长度: {} 字符", ddl.length());
        return ddl;
    }

    /**
     * 生成DDL脚本(使用默认选项)
     */
    public String generateDDL(SchemaDiff diff, String databaseType) {
        GenerationOptions options = GenerationOptions.builder()
                .databaseType(databaseType)
                .includeRollback(true)
                .commentBreakingChanges(true)
                .useTransaction(true)
                .build();
        
        return generateDDL(diff, options);
    }

    /**
     * 生成DDL脚本为字节数组
     */
    public byte[] generateDDLBytes(SchemaDiff diff, GenerationOptions options) {
        String ddl = generateDDL(diff, options);
        return ddl.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 根据数据库类型获取生成器
     */
    private DDLGenerator getGenerator(String databaseType) {
        String type = databaseType.toUpperCase();
        
        List<DDLGenerator> matched = generators.stream()
                .filter(g -> g.getDatabaseType().equals(type))
                .collect(Collectors.toList());

        if (matched.isEmpty()) {
            throw new UnsupportedOperationException("不支持的数据库类型: " + databaseType);
        }

        return matched.get(0);
    }

    /**
     * 获取所有支持的数据库类型
     */
    public List<String> getSupportedDatabaseTypes() {
        return generators.stream()
                .map(DDLGenerator::getDatabaseType)
                .collect(Collectors.toList());
    }
}
