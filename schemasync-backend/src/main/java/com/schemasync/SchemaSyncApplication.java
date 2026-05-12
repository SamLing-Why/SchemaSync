package com.schemasync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

/**
 * SchemaSync 应用启动类
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@SpringBootApplication
public class SchemaSyncApplication {

    private static final Logger log = LoggerFactory.getLogger(SchemaSyncApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SchemaSyncApplication.class);
        Environment env = app.run(args).getEnvironment();
        
        // 输出版本信息
        String version = env.getProperty("spring.application.version", "unknown");
        String appName = env.getProperty("spring.application.name", "SchemaSync");
        log.info("=== {} 启动完成, 版本: {} ===", appName, version);
    }
}
