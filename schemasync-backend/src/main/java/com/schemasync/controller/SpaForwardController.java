package com.schemasync.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA转发控制器
 * 将所有非API请求转发到index.html,支持Vue Router的history模式
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
@Controller
public class SpaForwardController {

    /**
     * 将所有非API、非静态资源的请求转发到index.html
     * 这样Vue Router可以处理前端路由
     */
    @RequestMapping(value = {
        "/config",
        "/export",
        "/diff",
        "/generate"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
