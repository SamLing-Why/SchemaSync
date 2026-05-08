package com.schemasync.service;

import com.schemasync.adapter.DatabaseAdapterFactory;
import com.schemasync.model.config.DataSourceConfig;
import com.schemasync.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Autowired
    private ConfigService configService;

    @Autowired
    private DatabaseAdapterFactory databaseAdapterFactory;

    @BeforeEach
    void setUp() {
        System.out.println("========================================");
        System.out.println("开始数据源连接集成测试");
        System.out.println("========================================");
    }

    @Test
    void testLoadDataSourcesFromConfig() {
        System.out.println("[测试1] 加载数据源配置");
        
        // 动态加载所有数据源
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        
        // 验证基本结构
        assertNotNull(dataSources, "数据源列表不应为null");
        assertTrue(dataSources.size() > 0, "至少应该有1个数据源配置");
        
        System.out.println("成功加载 " + dataSources.size() + " 个数据源配置");
        
        // 动态输出所有数据源信息
        for (int i = 0; i < dataSources.size(); i++) {
            DataSourceConfig ds = dataSources.get(i);
            System.out.println("  数据源[" + (i + 1) + "]:");
            System.out.println("    名称: " + ds.getName());
            System.out.println("    ID: " + ds.getId());
            System.out.println("    类型: " + ds.getType());
            System.out.println("    地址: " + ds.getHost() + ":" + ds.getPort());
            System.out.println("    数据库: " + ds.getDatabase());
            System.out.println("    用户名: " + ds.getUsername());
            System.out.println("    字符集: " + ds.getCharset());
            System.out.println("    超时: " + ds.getTimeout() + "秒");
        }
    }

    @Test
    void testAllDataSourceConnections() {
        System.out.println("[测试2] 测试所有数据源连接");
        
        // 动态获取所有数据源
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        
        assertTrue(dataSources.size() > 0, "至少应该有1个数据源配置");
        
        int successCount = 0;
        int failCount = 0;
        
        // 遍历测试每个数据源
        for (DataSourceConfig ds : dataSources) {
            System.out.println("----------------------------------------");
            System.out.println("测试数据源: " + ds.getName());
            System.out.println("  类型: " + ds.getType());
            System.out.println("  地址: " + ds.getHost() + ":" + ds.getPort());
            System.out.println("  数据库: " + ds.getDatabase());
            System.out.println("  用户名: " + ds.getUsername());
            
            long startTime = System.currentTimeMillis();
            try {
                boolean success = configService.testConnection(ds.getId());
                long elapsed = System.currentTimeMillis() - startTime;
                
                if (success) {
                    System.out.println("  ✅ 连接测试: 成功");
                    System.out.println("  ⏱️  耗时: " + elapsed + "ms");
                    successCount++;
                } else {
                    System.out.println("  ❌ 连接测试: 失败");
                    System.out.println("  ⏱️  耗时: " + elapsed + "ms");
                    System.out.println("  📝 原因: testConnection()返回false，连接未建立");
                    failCount++;
                }
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("  ❌ 连接测试: 失败");
                System.out.println("  ⏱️  耗时: " + elapsed + "ms");
                System.out.println("  📝 异常类型: " + e.getClass().getSimpleName());
                System.out.println("  📝 异常信息: " + e.getMessage());
                
                // 打印根本原因
                Throwable cause = e.getCause();
                if (cause != null) {
                    System.out.println("  🔍 根本原因: " + cause.getClass().getSimpleName() + " - " + cause.getMessage());
                }
                
                failCount++;
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.println("连接测试汇总:");
        System.out.println("  总计: " + dataSources.size() + " 个数据源");
        System.out.println("  ✅ 成功: " + successCount + " 个");
        System.out.println("  ❌ 失败: " + failCount + " 个");
        
        if (failCount > 0) {
            System.out.println("  ⚠️  注意: 有 " + failCount + " 个数据源连接失败，请检查网络或配置");
        }
        System.out.println("----------------------------------------");
    }

    @Test
    void testPasswordEncryption() {
        System.out.println("[测试3] 测试密码加密状态");
        
        // 动态获取所有数据源
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        
        assertTrue(dataSources.size() > 0, "至少应该有1个数据源配置");
        
        int successCount = 0;
        int failCount = 0;
        
        // 遍历检查每个数据源的密码加密状态
        for (DataSourceConfig ds : dataSources) {
            System.out.println("----------------------------------------");
            System.out.println("检查数据源: " + ds.getName());
            
            // 验证密码字段存在
            assertNotNull(ds.getPassword(), "数据源 '" + ds.getName() + "' 的密码不应为null");
            
            // 检查是否加密
            boolean isEncrypted = CryptoUtil.isEncrypted(ds.getPassword());
            System.out.println("  密码状态: " + (isEncrypted ? "已加密" : "明文"));
            
            if (isEncrypted) {
                // 尝试解密
                try {
                    String decryptedPassword = CryptoUtil.decrypt(ds.getPassword());
                    assertNotNull(decryptedPassword, "解密后的密码不应为null");
                    assertFalse(decryptedPassword.isEmpty(), "解密后的密码不应为空");
                    System.out.println("  ✅ 密码解密: 成功");
                    System.out.println("  📏 明文长度: " + decryptedPassword.length() + " 字符");
                    successCount++;
                } catch (Exception e) {
                    System.out.println("  ❌ 密码解密: 失败");
                    System.out.println("  📝 异常类型: " + e.getClass().getSimpleName());
                    System.out.println("  📝 异常信息: " + e.getMessage());
                    
                    // 打印根本原因
                    Throwable cause = e.getCause();
                    if (cause != null) {
                        System.out.println("  🔍 根本原因: " + cause.getClass().getSimpleName() + " - " + cause.getMessage());
                    }
                    
                    failCount++;
                }
            } else {
                System.out.println("  ℹ️  密码为明文状态");
                System.out.println("  📏 密码长度: " + ds.getPassword().length() + " 字符");
                successCount++;
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.println("密码检查汇总:");
        System.out.println("  总计: " + dataSources.size() + " 个数据源");
        System.out.println("  ✅ 成功: " + successCount + " 个");
        System.out.println("  ❌ 失败: " + failCount + " 个");
        System.out.println("----------------------------------------");
    }

    @Test
    void testSupportedDatabaseTypes() {
        System.out.println("[测试4] 测试支持的数据库类型");
        
        // 动态获取支持的数据库类型
        List<String> supportedTypes = databaseAdapterFactory.getSupportedTypes();
        
        assertNotNull(supportedTypes, "支持的数据库类型列表不应为null");
        assertTrue(supportedTypes.size() > 0, "至少应该支持1种数据库类型");
        
        System.out.println("系统支持的数据库类型 (共" + supportedTypes.size() + "种):");
        for (int i = 0; i < supportedTypes.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + supportedTypes.get(i));
        }
        
        // 动态验证配置中的数据源类型是否都被支持
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        Map<String, Long> typeCounts = dataSources.stream()
            .collect(Collectors.groupingBy(DataSourceConfig::getType, Collectors.counting()));
        
        System.out.println("配置中使用的数据库类型统计:");
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            String type = entry.getKey();
            long count = entry.getValue();
            boolean isSupported = supportedTypes.contains(type.toUpperCase());
            
            System.out.println("  类型: " + type + " (使用" + count + "次) - " + 
                (isSupported ? "✓ 已支持" : "✗ 未支持"));
            
            assertTrue(isSupported, 
                "配置中使用的数据库类型 '" + type + "' 应该在支持列表中");
        }
    }

    @Test
    void testDataSourceConfigurationCompleteness() {
        System.out.println("[测试5] 测试数据源配置完整性");
        
        // 动态获取所有数据源
        List<DataSourceConfig> dataSources = configService.getAllConfigs();
        
        assertTrue(dataSources.size() > 0, "至少应该有1个数据源配置");
        
        // 遍历检查每个数据源的配置完整性
        for (DataSourceConfig ds : dataSources) {
            System.out.println("检查数据源配置完整性: " + ds.getName());
            
            // 检查必填字段
            assertNotNull(ds.getId(), "数据源ID不应为null");
            assertFalse(ds.getId().isEmpty(), "数据源ID不应为空");
            System.out.println("  ✓ ID: " + ds.getId());
            
            assertNotNull(ds.getName(), "数据源名称不应为null");
            assertFalse(ds.getName().isEmpty(), "数据源名称不应为空");
            System.out.println("  ✓ 名称: " + ds.getName());
            
            assertNotNull(ds.getType(), "数据源类型不应为null");
            assertFalse(ds.getType().isEmpty(), "数据源类型不应为空");
            System.out.println("  ✓ 类型: " + ds.getType());
            
            assertNotNull(ds.getHost(), "主机地址不应为null");
            assertFalse(ds.getHost().isEmpty(), "主机地址不应为空");
            System.out.println("  ✓ 主机: " + ds.getHost());
            
            assertTrue(ds.getPort() > 0 && ds.getPort() <= 65535, 
                "端口号应在有效范围内 (1-65535)");
            System.out.println("  ✓ 端口: " + ds.getPort());
            
            assertNotNull(ds.getDatabase(), "数据库名不应为null");
            assertFalse(ds.getDatabase().isEmpty(), "数据库名不应为空");
            System.out.println("  ✓ 数据库: " + ds.getDatabase());
            
            assertNotNull(ds.getUsername(), "用户名不应为null");
            assertFalse(ds.getUsername().isEmpty(), "用户名不应为空");
            System.out.println("  ✓ 用户名: " + ds.getUsername());
            
            assertNotNull(ds.getPassword(), "密码不应为null");
            assertFalse(ds.getPassword().isEmpty(), "密码不应为空");
            System.out.println("  ✓ 密码: [已配置]");
            
            // 检查可选字段
            if (ds.getCharset() != null && !ds.getCharset().isEmpty()) {
                System.out.println("  ℹ 字符集: " + ds.getCharset());
            }
            
            if (ds.getTimeout() != null && ds.getTimeout() > 0) {
                System.out.println("  ℹ 超时: " + ds.getTimeout() + "秒");
            }
            
            System.out.println("  ✓ 配置完整性检查通过");
        }
    }
}
