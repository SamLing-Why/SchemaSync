## 1. 系统概述
后端采用 Spring Boot 默认的 **SLF4J + Logback** 日志体系，通过 `spring-boot-starter-logging` 引入，配合 `application.yml` 及多环境 profile 文件进行集中配置。前端（Vue3）未集成专用日志框架，仅依赖浏览器控制台输出。

## 2. 核心依赖与初始化
- 依赖：`spring-boot-starter-logging`（间接引入 SLF4J API 与 Logback 实现）
- 代码层统一使用 `org.slf4j.Logger` / `LoggerFactory.getLogger(...)` 获取实例，以 `private static final Logger log = ...` 形式声明，所有业务类遵循此模式。

## 3. 配置文件与分级策略
| 文件 | 作用 |
|---|---|
| `schemasync-backend/src/main/resources/application.yml` | 基础日志配置：根级别 INFO、包级别 DEBUG、控制台 pattern、滚动文件路径/大小/保留天数 |
| `application-dev.yml` | 开发环境覆盖：额外开启 `org.springframework.web: DEBUG` |
| `application-prod.yml` | 生产环境覆盖：根 WARN、业务包 INFO，并指定独立日志目录 `/var/log/schemasync/schemasync.log` |

默认 pattern：`%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n`
滚动策略：单文件 `logs/schemasync.log`，最大 100MB，最多保留 30 份历史。

## 4. 架构与约定
- **无自定义 Appender/Filter**：完全依赖 Spring Boot 对 Logback 的自动装配，未编写 `logback-spring.xml` 或自定义 RollingFileAppender。
- **按包分层控制**：通过 `com.schemasync.*` 包名粒度调整级别，便于在开发时放大调试信息而不影响全局。
- **结构化程度低**：日志均为纯文本拼接，未见 MDC/TraceId/请求上下文等结构化字段注入；错误日志直接附带异常堆栈（如 `log.error("...", e)`）。
- **部署产物位置**：打包后 jar 运行时的日志目录由 `logging.file.name` 决定，生产部署脚本中会挂载到宿主机 `/var/log/schemasync/`。

## 5. 开发者应遵守的规则
1. **统一入口**：通过 `LoggerFactory.getLogger(当前类.class)` 获取 logger，禁止直接使用 `System.out` / `printStackTrace`。
2. **级别选择**：关键流程节点用 `info`，可恢复异常用 `warn`，不可恢复异常用 `error` 并附带异常对象；调试信息仅在 dev profile 下可见。
3. **避免敏感信息**：密码、密钥、完整 SQL 语句不应写入 info/error 日志。
4. **保持简洁**：消息体尽量短且可读，复杂对象序列化前先做裁剪，避免产生超大日志行。
5. **跨进程追踪**：当前未引入链路 ID，如需增强可在后续引入 Micrometer Tracing 并在日志 pattern 中加入 `%X{traceId}` 等占位符。