package com.schemasync.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置静态资源映射和SPA路由支持
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置跨域请求(开发环境)
     * 允许前端Vite开发服务器(5173)访问后端API(8080)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源处理器
     * 将前端构建产物映射到根路径
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600) // 缓存1小时
                .resourceChain(true);
    }

    /**
     * 配置视图控制器
     * 支持Vue Router的history模式
     * 所有非API请求都转发到index.html
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 根路径转发到index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
