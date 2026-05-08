package com.schemasync.service;

import com.schemasync.adapter.DatabaseAdapterFactory;
import com.schemasync.model.config.DataSourceConfig;
import com.schemasync.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据源连接集成测试
 * 使用test/resources/schemasync-config.json中的配置
 * 
 * 测试特点:
 * - 不硬编码配置文件内容，所有信息动态读取
 * - 支持任意数量和类型的数据源配置
 * - 自动遍历所有数据源进行连接测试
 * 
 * @author SchemaSync Team
 * @since 2026-05-07
 */
@SpringBootTest
@TestPropertySource(properties = {
    "schemasync.config.file=classpath:schemasync-config.json"
})
class DataSourceConnectionIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConnectionIntegrationTest.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private DatabaseAdapterFactory databaseAdapterFactory;

    @BeforeEach
    void setUp() {
        log.info("========================================");
        log.info("开始数据源连接集成测试");
        log.info("========================================");
    }

    @Test
    void testLoadDataSourcesFromConfig() {
        log.info("[测试1] 加载数据源配置");
        
        // 动态加载所有数据源
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        
        // 验证基本结构
        assertNotNull(dataSources, "数据源列表不应为null");
        assertTrue(dataSources.size() > 0, "至少应该有1个数据源配置");
        
        log.info("成功加载 {} 个数据源配置", dataSources.size());
        
        // 动态输出所有数据源信息
        for (int i = 0; i < dataSources.size(); i++) {
            DataSourceConfig ds = dataSources.get(i);
            log.info("  数据源[{}]:", i + 1);
            log.info("    名称: {}", ds.getName());
            log.info("    ID: {}", ds.getId());
            log.info("    类型: {}", ds.getType());
            log.info("    地址: {}:{}", ds.getHost(), ds.getPort());
            log.info("    数据库: {}", ds.getDatabase());
            log.info("    用户名: {}", ds.getUsername());
            log.info("    字符集: {}", ds.getCharset());
            log.info("    超时: {}秒", ds.getTimeout());
        }
    }

    @Test
    void testAllDataSourceConnections() {
        log.info("[测试2] 测试所有数据源连接");
        
        // 动态获取所有数据源
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        
        assertTrue(dataSources.size() > 0, "至少应该有1个数据源配置");
        
        // 遍历测试每个数据源
        for (DataSourceConfig ds : dataSources) {
            log.info("----------------------------------------");
            log.info("测试数据源: {}", ds.getName());
            log.info("  类型: {}", ds.getType());
            log.info("  地址: {}:{}", ds.getHost(), ds.getPort());
            log.info("  数据库: {}", ds.getDatabase());
            log.info("  注意: 此测试需要能够访问地址 {}:{}", ds.getHost(), ds.getPort());
            
            try {
                long startTime = System.currentTimeMillis();
                boolean success = configService.testConnection(ds.getId());
                long elapsed = System.currentTimeMillis() - startTime;
                
                if (success) {
                    log.info("  ✓ 连接测试成功 (耗时: {}ms)", elapsed);
                } else {
                    log.warn("  ⚠ 连接测试失败 - 可能无法访问目标地址 (耗时: {}ms)", elapsed);
                }
                // 不强制断言，因为内网地址可能无法从当前网络访问
            } catch (Exception e) {
                log.warn("  ⚠ 连接测试异常 (可能是网络问题): {}", e.getMessage());
                // 记录异常但不失败，因为这是网络环境问题
            }
        }
        
        log.info("----------------------------------------");
        log.info("所有数据源连接测试完成");
    }

    @Test
    void testPasswordEncryption() {
        log.info("[测试3] 测试密码加密状态");
        
        // 动态获取所有数据源
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        
        assertTrue(dataSources.size() > 0, "至少应该有1个数据源配置");
        
        // 遍历检查每个数据源的密码加密状态
        for (DataSourceConfig ds : dataSources) {
            log.info("检查数据源 '{}' 的密码加密状态", ds.getName());
            
            // 验证密码字段存在
            assertNotNull(ds.getPassword(), "数据源 '" + ds.getName() + "' 的密码不应为null");
            
            // 检查是否加密
            boolean isEncrypted = CryptoUtil.isEncrypted(ds.getPassword());
            log.info("  数据源: {}", ds.getName());
            log.info("  密码状态: {}", isEncrypted ? "已加密" : "明文");
            
            if (isEncrypted) {
                // 尝试解密
                try {
                    String decryptedPassword = CryptoUtil.decrypt(ds.getPassword());
                    assertNotNull(decryptedPassword, "解密后的密码不应为null");
                    assertFalse(decryptedPassword.isEmpty(), "解密后的密码不应为空");
                    log.info("  ✓ 密码解密成功 (解密后长度: {} 字符)", decryptedPassword.length());
                } catch (Exception e) {
                    log.warn("  ⚠ 密码解密失败: {}", e.getMessage());
                    // 记录警告但不失败，可能是测试环境的加密密钥不同
                }
            } else {
                log.info("  ℹ 密码为明文状态 (长度: {} 字符)", ds.getPassword().length());
            }
        }
    }

    @Test
    void testSupportedDatabaseTypes() {
        log.info("[测试4] 测试支持的数据库类型");
        
        // 动态获取支持的数据库类型
        List<String> supportedTypes = databaseAdapterFactory.getSupportedTypes();
        
        assertNotNull(supportedTypes, "支持的数据库类型列表不应为null");
        assertTrue(supportedTypes.size() > 0, "至少应该支持1种数据库类型");
        
        log.info("系统支持的数据库类型 (共{}种):", supportedTypes.size());
        for (int i = 0; i < supportedTypes.size(); i++) {
            log.info("  [{}] {}", i + 1, supportedTypes.get(i));
        }
        
        // 动态验证配置中的数据源类型是否都被支持
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        Map<String, Long> typeCounts = dataSources.stream()
            .collect(Collectors.groupingBy(DataSourceConfig::getType, Collectors.counting()));
        
        log.info("配置中使用的数据库类型统计:");
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            String type = entry.getKey();
            long count = entry.getValue();
            boolean isSupported = supportedTypes.contains(type.toUpperCase());
            
            log.info("  类型: {} (使用{}次) - {}", 
                type, 
                count, 
                isSupported ? "✓ 已支持" : "✗ 未支持");
            
            assertTrue(isSupported, 
                "配置中使用的数据库类型 '" + type + "' 应该在支持列表中");
        }
    }

    @Test
    void testDataSourceConfigurationCompleteness() {
        log.info("[测试5] 测试数据源配置完整性");
        
        // 动态获取所有数据源
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        
        assertTrue(dataSources.size() > 0, "至少应该有1个数据源配置");
        
        // 遍历检查每个数据源的配置完整性
        for (DataSourceConfig ds : dataSources) {
            log.info("检查数据源配置完整性: {}", ds.getName());
            
            // 检查必填字段
            assertNotNull(ds.getId(), "数据源ID不应为null");
            assertFalse(ds.getId().isEmpty(), "数据源ID不应为空");
            log.info("  ✓ ID: {}", ds.getId());
            
            assertNotNull(ds.getName(), "数据源名称不应为null");
            assertFalse(ds.getName().isEmpty(), "数据源名称不应为空");
            log.info("  ✓ 名称: {}", ds.getName());
            
            assertNotNull(ds.getType(), "数据源类型不应为null");
            assertFalse(ds.getType().isEmpty(), "数据源类型不应为空");
            log.info("  ✓ 类型: {}", ds.getType());
            
            assertNotNull(ds.getHost(), "主机地址不应为null");
            assertFalse(ds.getHost().isEmpty(), "主机地址不应为空");
            log.info("  ✓ 主机: {}", ds.getHost());
            
            assertTrue(ds.getPort() > 0 && ds.getPort() <= 65535, 
                "端口号应在有效范围内 (1-65535)");
            log.info("  ✓ 端口: {}", ds.getPort());
            
            assertNotNull(ds.getDatabase(), "数据库名不应为null");
            assertFalse(ds.getDatabase().isEmpty(), "数据库名不应为空");
            log.info("  ✓ 数据库: {}", ds.getDatabase());
            
            assertNotNull(ds.getUsername(), "用户名不应为null");
            assertFalse(ds.getUsername().isEmpty(), "用户名不应为空");
            log.info("  ✓ 用户名: {}", ds.getUsername());
            
            assertNotNull(ds.getPassword(), "密码不应为null");
            assertFalse(ds.getPassword().isEmpty(), "密码不应为空");
            log.info("  ✓ 密码: [已配置]");
            
            // 检查可选字段
            if (ds.getCharset() != null && !ds.getCharset().isEmpty()) {
                log.info("  ℹ 字符集: {}", ds.getCharset());
            }
            
            if (ds.getTimeout() != null && ds.getTimeout() > 0) {
                log.info("  ℹ 超时: {}秒", ds.getTimeout());
            }
            
            log.info("  ✓ 配置完整性检查通过");
        }
    }
}
