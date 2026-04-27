package com.schemasync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemasync.model.config.DataSourceConfig;
import com.schemasync.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private com.schemasync.adapter.DatabaseAdapterFactory adapterFactory;

    /**
     * 内存中的配置缓存
     */
    private final Map<String, DataSourceConfig> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 解析配置文件路径,支持相对路径和绝对路径
        configFile = resolveConfigPath(configFile);
        log.info("数据源配置文件路径: {}", configFile);
        loadConfigs();
    }

    /**
     * 解析配置文件路径
     * - 如果是绝对路径,直接使用
     * - 如果是相对路径,相对于用户主目录 ~/.schemasync/
     */
    private String resolveConfigPath(String path) {
        File file = new File(path);
        
        // 如果是绝对路径,直接返回
        if (file.isAbsolute()) {
            log.debug("使用绝对路径: {}", path);
            return path;
        }
        
        // 相对路径,相对于用户主目录的 .schemasync 目录
        String userHome = System.getProperty("user.home");
        String resolvedPath = Paths.get(userHome, ".schemasync", path).toString();
        log.debug("解析相对路径: {} -> {}", path, resolvedPath);
        return resolvedPath;
    }

    /**
     * 加载配置文件
     */
    private void loadConfigs() {
        try {
            File file = new File(configFile);
            log.info("尝试加载配置文件: {}, 存在: {}", configFile, file.exists());
            
            if (!file.exists()) {
                log.warn("配置文件不存在, 使用空配置, 路径: {}", configFile);
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

            log.info("加载数据源配置成功, 文件: {}, 共{}个", configFile, configCache.size());
        } catch (Exception e) {
            log.error("加载配置文件失败, 文件: {}", configFile, e);
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
        if (name == null || name.isEmpty()) {
            return null;
        }
        return configCache.values().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 添加数据源配置
     */
    public DataSourceConfig addConfig(DataSourceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("配置不能为空");
        }
        
        // 校验必填字段
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("数据源名称不能为空");
        }
        if (config.getType() == null || config.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("数据库类型不能为空");
        }
        if (config.getHost() == null || config.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("主机地址不能为空");
        }
        if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        
        // 设置默认值
        if (config.getPort() == null) {
            config.setPort(3306);
        }
        if (config.getTimeout() == null) {
            config.setTimeout(30);
        }
        if (config.getCharset() == null) {
            config.setCharset("utf8mb4");
        }
        
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
    /**
     * 测试连接(已保存的配置)
     */
    public boolean testConnection(String configId) {
        DataSourceConfig config = getConfigById(configId);
        if (config == null) {
            throw new RuntimeException("配置不存在: " + configId);
        }

        return testConnectionInternal(config);
    }

    /**
     * 测试连接(临时配置,用于新增/编辑时)
     * 包含完整的参数校验
     */
    public Map<String, Object> testConnectionWithConfig(DataSourceConfig config) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            // 参数校验
            List<String> missingFields = validateConfig(config);
            if (!missingFields.isEmpty()) {
                result.put("success", false);
                result.put("message", "请填写必填项: " + String.join(", ", missingFields));
                result.put("missingFields", missingFields);
                return result;
            }
            
            // 测试连接
            boolean success = testConnectionInternal(config);
            result.put("success", success);
            result.put("message", success ? "连接成功" : "连接失败,请检查配置");
            
            // 如果连接成功,返回数据库版本信息
            if (success) {
                try {
                    com.schemasync.adapter.DatabaseAdapter adapter = adapterFactory.getAdapter(config.getType());
                    java.sql.Connection conn = adapter.connect(config);
                    String version = adapter.getDatabaseVersion(conn);
                    conn.close();
                    result.put("databaseVersion", version);
                } catch (Exception e) {
                    log.debug("获取数据库版本失败", e);
                }
            }
        } catch (Exception e) {
            log.error("测试连接异常", e);
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 校验配置参数
     */
    private List<String> validateConfig(DataSourceConfig config) {
        List<String> missingFields = new ArrayList<>();
        
        if (config.getType() == null || config.getType().trim().isEmpty()) {
            missingFields.add("数据库类型");
        }
        if (config.getHost() == null || config.getHost().trim().isEmpty()) {
            missingFields.add("主机地址");
        }
        if (config.getPort() == null) {
            missingFields.add("端口");
        }
        if (config.getDatabase() == null || config.getDatabase().trim().isEmpty()) {
            missingFields.add("数据库名称");
        }
        if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
            missingFields.add("用户名");
        }
        // 密码不强制要求,有些数据库允许无密码登录
        
        return missingFields;
    }

    /**
     * 内部测试连接方法
     */
    private boolean testConnectionInternal(DataSourceConfig config) {
        try {
            log.info("测试连接: {} - {}:{}/{}", 
                config.getName() != null ? config.getName() : "临时配置",
                config.getHost(), 
                config.getPort(),
                config.getDatabase());
            
            com.schemasync.adapter.DatabaseAdapter adapter = adapterFactory.getAdapter(config.getType());
            
            java.sql.Connection conn = adapter.connect(config);
            boolean isValid = conn.isValid(5); // 5秒超时
            conn.close();
            
            return isValid;
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return false;
        }
    }

    /**
     * 保存配置到文件
     */
    private void saveConfigs() {
        try {
            Map<String, Object> configMap = new LinkedHashMap<>();
            configMap.put("version", "1.0.0");
            configMap.put("updateTime", new Date());
            configMap.put("dataSources", new ArrayList<>(configCache.values()));

            String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(configMap);
            
            // 确保父目录存在
            File file = new File(configFile);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            
            Files.write(Paths.get(configFile), content.getBytes());

            log.info("保存配置成功, 文件: {}, 数据源数量: {}", configFile, configCache.size());
        } catch (Exception e) {
            log.error("保存配置文件失败, 文件: {}", configFile, e);
            throw new RuntimeException("保存配置文件失败: " + e.getMessage(), e);
        }
    }
}
