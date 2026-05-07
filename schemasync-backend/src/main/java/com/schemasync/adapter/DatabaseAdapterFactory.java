package com.schemasync.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库适配器工厂
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Component
public class DatabaseAdapterFactory {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAdapterFactory.class);

    @Autowired
    private List<DatabaseAdapter> adapters;

    private final Map<String, DatabaseAdapter> adapterMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 注册所有适配器
        for (DatabaseAdapter adapter : adapters) {
            String type = adapter.getDatabaseType().toUpperCase();
            adapterMap.put(type, adapter);
            log.info("注册数据库适配器: {}", type);
        }
    }

    /**
     * 根据数据库类型获取适配器
     * 
     * @param databaseType 数据库类型 (mysql/oracle/oceanbase等)
     * @return 数据库适配器
     */
    public DatabaseAdapter getAdapter(String databaseType) {
        String type = databaseType.toUpperCase();
        DatabaseAdapter adapter = adapterMap.get(type);
        
        if (adapter == null) {
            throw new UnsupportedOperationException("不支持的数据库类型: " + databaseType + 
                    ", 支持的类型: " + adapterMap.keySet());
        }
        
        return adapter;
    }

    /**
     * 获取所有支持的数据库类型
     */
    public List<String> getSupportedTypes() {
        return new java.util.ArrayList<>(adapterMap.keySet());
    }
}
