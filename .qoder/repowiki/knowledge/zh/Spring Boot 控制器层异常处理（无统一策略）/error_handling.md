该仓库的 SchemaSync 后端基于 Spring Boot，但**未建立统一的错误处理体系**。当前错误处理呈现以下特征：

1. **异常类型分散且不规范**：Controller 中直接抛出 `RuntimeException`、`IllegalArgumentException`、`UnsupportedOperationException` 等原生异常，没有自定义业务异常类或统一的错误码枚举。例如 `DdlController` 在 DDL 生成失败时抛 `RuntimeException("生成DDL失败: ...")`，`ExportController` 对参数校验抛 `IllegalArgumentException`。

2. **缺少全局异常处理器**：代码库中不存在 `@RestControllerAdvice` + `@ExceptionHandler` 的全局异常拦截器，也没有实现 `HandlerExceptionResolver`。这意味着所有未捕获异常最终由 Spring Boot 默认的 `BasicErrorController` 处理，返回标准的 `/error` 页面或 JSON，前端无法获得一致的错误响应格式。

3. **日志记录不统一**：部分方法使用 `log.error(...)` 记录异常堆栈（如 `ExportController` 中的数据库连接、密码解密失败），但多数 Controller 仅将异常向上抛出而不记录上下文信息，不利于问题排查。

4. **HTTP 状态码使用随意**：成功路径统一返回 `ResponseEntity.ok()`，资源不存在时使用 `ResponseEntity.notFound().build()`，但业务异常全部以 500 状态码返回，缺乏对 400（参数错误）、404（资源不存在）等语义化状态码的区分。

5. **前端错误处理缺失**：前端 `src/api/request.js` 中未发现针对 HTTP 错误码的统一拦截和提示逻辑，依赖浏览器默认行为。

6. **WebConfig 仅配置 CORS/静态资源**：`WebConfig` 实现了 `WebMvcConfigurer`，但只用于跨域、静态资源和 SPA 路由转发，未注册任何异常解析器。

**结论**：该项目处于“无统一错误处理”的初级阶段，异常通过原生 Exception 向上传播，由 Spring Boot 默认机制兜底。建议引入统一的业务异常类、全局异常处理器、标准化错误响应体以及前端错误拦截，以提升可维护性和用户体验。