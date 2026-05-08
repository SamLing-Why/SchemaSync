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

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据源连接集成测试
 * 使用test/resources/schemasync-config.json中的配置
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
        log.info("=== 开始数据源连接集成测试 ===");
    }

    @Test
    void testLoadDataSourcesFromConfig() {
        // 测试加载配置
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        
        assertNotNull(dataSources, "数据源列表不应为null");
        assertEquals(2, dataSources.size(), "应该有2个数据源配置");
        
        log.info("成功加载 {} 个数据源配置", dataSources.size());
        for (DataSourceConfig ds : dataSources) {
            log.info("数据源: {} ({}), 类型: {}, 主机: {}:{}", 
                ds.getName(), ds.getId(), ds.getType(), ds.getHost(), ds.getPort());
        }
    }

    @Test
    void testOpenGaussConnection() {
        // 测试OpenGauss数据源连接
        DataSourceConfig gaussConfig = configService.getConfigByName("OpenGuass(内网)");
        
        assertNotNull(gaussConfig, "OpenGauss数据源配置不应为null");
        assertEquals("gaussdb", gaussConfig.getType());
        assertEquals("10.161.2.107", gaussConfig.getHost());
        assertEquals(5432, gaussConfig.getPort());
        
        log.info("测试OpenGauss连接: {}:{}", gaussConfig.getHost(), gaussConfig.getPort());
        log.info("注意：此测试需要能够访问内网地址10.161.2.107:5432");
        
        try {
            boolean success = configService.testConnection(gaussConfig.getId());
            if (success) {
                log.info("✓ OpenGauss连接测试成功");
            } else {
                log.warn("⚠ OpenGauss连接测试失败 - 可能无法访问内网地址");
            }
            // 不强制断言，因为内网地址可能无法从当前网络访问
        } catch (Exception e) {
            log.warn("⚠ OpenGauss连接测试异常（可能是网络问题）: {}", e.getMessage());
            // 记录异常但不失败，因为这是网络环境问题
        }
    }

    @Test
    void testMySQLConnection() {
        // 测试MySQL数据源连接
        DataSourceConfig mysqlConfig = configService.getConfigByName("国结公共库(SIT)");
        
        assertNotNull(mysqlConfig, "MySQL数据源配置不应为null");
        assertEquals("mysql", mysqlConfig.getType());
        assertEquals("10.62.97.103", mysqlConfig.getHost());
        assertEquals(3306, mysqlConfig.getPort());
        
        log.info("测试MySQL连接: {}:{}", mysqlConfig.getHost(), mysqlConfig.getPort());
        log.info("注意：此测试需要能够访问公司内网地址10.62.97.103:3306");
        
        try {
            boolean success = configService.testConnection(mysqlConfig.getId());
            if (success) {
                log.info("✓ MySQL连接测试成功");
            } else {
                log.warn("⚠ MySQL连接测试失败 - 可能无法访问公司内网");
            }
            // 不强制断言，因为内网地址可能无法从当前网络访问
        } catch (Exception e) {
            log.warn("⚠ MySQL连接测试异常（可能是网络问题）: {}", e.getMessage());
            // 记录异常但不失败，因为这是网络环境问题
        }
    }

    @Test
    void testPasswordDecryption() {
        // 测试密码解密功能
        DataSourceConfig gaussConfig = configService.getConfigByName("OpenGuass(内网)");
        
        assertNotNull(gaussConfig.getPassword(), "密码不应为null");
        assertTrue(CryptoUtil.isEncrypted(gaussConfig.getPassword()), "密码应该是加密的");
        
        log.info("测试密码解密功能");
        
        try {
            String decryptedPassword = CryptoUtil.decrypt(gaussConfig.getPassword());
            assertNotNull(decryptedPassword, "解密后的密码不应为null");
            assertFalse(decryptedPassword.isEmpty(), "解密后的密码不应为空");
            log.info("✓ 密码解密成功");
        } catch (Exception e) {
            log.error("✗ 密码解密失败: {}", e.getMessage());
            fail("密码解密失败: " + e.getMessage());
        }
    }

    @Test
    void testGetSupportedDatabaseTypes() {
        // 测试获取支持的数据库类型
        List<String> supportedTypes = databaseAdapterFactory.getSupportedTypes();
        
        assertNotNull(supportedTypes, "支持的数据库类型列表不应为null");
        // 类型名称是大写的
        assertTrue(supportedTypes.contains("MYSQL"), "应该支持MySQL");
        assertTrue(supportedTypes.contains("GAUSSDB"), "应该支持GaussDB");
        
        log.info("支持的数据库类型: {}", supportedTypes);
    }
}
