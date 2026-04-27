package com.schemasync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemasync.model.config.DataSourceConfig;
import com.schemasync.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 配置管理服务
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    @Value("${schemasync.config-file:schemasync-config.json}")
    private String configFile;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 内存中的配置缓存
     */
    private final Map<String, DataSourceConfig> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadConfigs();
    }

    /**
     * 加载配置文件
     */
    private void loadConfigs() {
        try {
            File file = new File(configFile);
            if (!file.exists()) {
                log.warn("配置文件不存在: {}, 使用空配置", configFile);
                return;
            }

            String content = new String(Files.readAllBytes(Paths.get(configFile)));
            Map<String, Object> configMap = objectMapper.readValue(content, Map.class);

            List<Map<String, Object>> dataSources = (List<Map<String, Object>>) configMap.get("dataSources");
            if (dataSources != null) {
                for (Map<String, Object> dsMap : dataSources) {
                    DataSourceConfig config = objectMapper.convertValue(dsMap, DataSourceConfig.class);
                    configCache.put(config.getId(), config);
                }
            }

            log.info("加载数据源配置成功, 共{}个", configCache.size());
        } catch (Exception e) {
            log.error("加载配置文件失败", e);
        }
    }

    /**
     * 获取所有数据源配置
     */
    public List<DataSourceConfig> getAllConfigs() {
        return new ArrayList<>(configCache.values());
    }

    /**
     * 根据ID获取配置
     */
    public DataSourceConfig getConfigById(String id) {
        return configCache.get(id);
    }

    /**
     * 根据名称获取配置
     */
    public DataSourceConfig getConfigByName(String name) {
        return configCache.values().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 添加数据源配置
     */
    public DataSourceConfig addConfig(DataSourceConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId("ds-" + UUID.randomUUID().toString().substring(0, 8));
        }

        // 加密密码
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            if (!CryptoUtil.isEncrypted(config.getPassword())) {
                config.setPassword(CryptoUtil.encrypt(config.getPassword()));
            }
        }

        config.setCreateTime(new Date());
        config.setUpdateTime(new Date());
        configCache.put(config.getId(), config);

        saveConfigs();
        return config;
    }

    /**
     * 更新数据源配置
     */
    public DataSourceConfig updateConfig(String id, DataSourceConfig config) {
        DataSourceConfig existing = configCache.get(id);
        if (existing == null) {
            throw new RuntimeException("配置不存在: " + id);
        }

        config.setId(id);
        config.setCreateTime(existing.getCreateTime());
        config.setUpdateTime(new Date());

        // 加密密码
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            if (!CryptoUtil.isEncrypted(config.getPassword())) {
                config.setPassword(CryptoUtil.encrypt(config.getPassword()));
            }
        }

        configCache.put(id, config);
        saveConfigs();
        return config;
    }

    /**
     * 删除数据源配置
     */
    public void deleteConfig(String id) {
        configCache.remove(id);
        saveConfigs();
    }

    /**
     * 测试数据库连接
     */
    public boolean testConnection(String configId) {
        DataSourceConfig config = getConfigById(configId);
        if (config == null) {
            throw new RuntimeException("配置不存在: " + configId);
        }

        // TODO: 实际测试连接逻辑,需要注入DatabaseAdapter
        log.info("测试连接: {} - {}:{}", config.getName(), config.getHost(), config.getPort());
        return true;
    }

    /**
     * 保存配置到文件
     */
    private void saveConfigs() {
        try {
            Map<String, Object> configMap = new LinkedHashMap<>();
            configMap.put("version", "1.0.0");
            configMap.put("dataSources", new ArrayList<>(configCache.values()));

            String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(configMap);
            Files.write(Paths.get(configFile), content.getBytes());

            log.info("保存配置成功");
        } catch (Exception e) {
            log.error("保存配置文件失败", e);
            throw new RuntimeException("保存配置文件失败", e);
        }
    }
}
