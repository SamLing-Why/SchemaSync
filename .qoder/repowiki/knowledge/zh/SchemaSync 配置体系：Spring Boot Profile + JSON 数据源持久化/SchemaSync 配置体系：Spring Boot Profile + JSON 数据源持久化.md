---
kind: configuration_system
name: SchemaSync 配置体系：Spring Boot Profile + JSON 数据源持久化
category: configuration_system
scope:
    - '**'
source_files:
    - schemasync-backend/src/main/resources/application.yml
    - schemasync-backend/src/main/resources/application-dev.yml
    - schemasync-backend/src/main/resources/application-prod.yml
    - schemasync-backend/src/main/resources/schemasync-config.json
    - schemasync-backend/src/main/java/com/schemasync/service/ConfigService.java
    - schemasync-backend/src/main/java/com/schemasync/controller/ConfigController.java
    - schemasync-backend/src/main/java/com/schemasync/model/config/DataSourceConfig.java
    - schemasync-backend/src/main/java/com/schemasync/util/CryptoUtil.java
    - deploy/start.sh
---

## 1. 系统概览

SchemaSync 采用**分层配置策略**，将“应用运行期配置”与“业务运行时配置”解耦：

- **应用层配置**：基于 Spring Boot `application.yml` + `application-{profile}.yml`，通过 `--spring.profiles.active` 切换环境（dev/prod）。
- **业务层配置**：数据源连接信息以 JSON 文件形式持久化在用户主目录 `~/.schemasync/schemasync-config.json`，由 `ConfigService` 在启动时加载到内存 `ConcurrentHashMap`，并通过 REST API 动态增删改查。

## 2. 关键文件与包

| 层次 | 文件/类 | 职责 |
|---|---|---|
| 应用配置 | `schemasync-backend/src/main/resources/application.yml` | 端口、日志、Jackson、Actuator、Swagger、`schemasync.*` 全局开关 |
| 环境覆盖 | `application-dev.yml` / `application-prod.yml` | 按 profile 覆盖日志级别、端口等 |
| 部署入口 | `deploy/start.sh` | 通过 `--spring.config.location=application.yml` 指定外部配置文件位置 |
| 业务配置模型 | `model/config/DataSourceConfig.java` | 数据源实体（id/name/type/host/port/database/username/password/jdbcUrl/poolConfig） |
| 配置服务 | `service/ConfigService.java` | JSON 文件解析、内存缓存、CRUD、连接测试、密码加解密 |
| 配置控制器 | `controller/ConfigController.java` | `/api/config/datasources/*` REST 接口 |
| 加密工具 | `util/CryptoUtil.java` | AES 对称加密（密钥硬编码于代码中） |
| 示例数据源 | `resources/schemasync-config.json` | 默认空数据源示例 |

## 3. 架构与约定

### 3.1 应用级配置加载顺序
```
application.yml (基础) → application-{profile}.yml (覆盖) → --spring.config.location (外部覆盖)
```
- `schemasync.config-file` 通过 `@Value("${schemasync.config-file:schemasync-config.json}")` 注入，默认值为 `schemasync-config.json`。
- `ConfigService.init()` 在 `@PostConstruct` 阶段解析路径：绝对路径直接使用；相对路径拼接 `user.home/.schemasync/`。

### 3.2 数据源配置持久化格式
JSON 结构：
```json
{
  "version": "1.0.0",
  "dataSources": [ { ... DataSourceConfig 字段 ... } ],
  "settings": { "defaultOutputDir": "...", "logLevel": "...", "maxConnectionPool": 5 }
}
```
- 新增/更新/删除后自动写回文件，父目录不存在则自动创建。
- 内存使用 `ConcurrentHashMap<String, DataSourceConfig>` 缓存，按 `id` 索引。

### 3.3 敏感信息保护
- 写入前调用 `CryptoUtil.encrypt()` 对 password 进行 AES 加密，并 Base64 编码。
- 读取/连接测试前调用 `CryptoUtil.decrypt()` 还原明文。
- 判断是否已加密：`isEncrypted()` 尝试 Base64 解码。

### 3.4 多数据库适配集成
- `ConfigController` 在返回数据源列表时，通过 `DatabaseAdapterFactory.getAdapter(type)` 动态注入 `supportsSchema` 标识，供前端渲染差异化 UI。
- 连接测试走适配器抽象，支持 mysql/oracle/oceanbase/tdsql/gaussdb/goldendb 等多种类型。

## 4. 开发者应遵循的规则

1. **新增应用级配置项**：统一写在 `application.yml` 的 `schemasync.*` 命名空间下，并通过 `@Value("${schemasync.xxx:默认值}")` 注入，避免直接读环境变量。
2. **新增数据源字段**：同步修改 `DataSourceConfig.java`、`schemasync-config.json` 示例以及 `ConfigService.saveConfigs()` 序列化逻辑，保持 JSON 结构一致。
3. **敏感字段一律走 CryptoUtil**：新增任何需要落盘的敏感字段，必须在 `addConfig/updateConfig` 中先 `encrypt`，在连接前 `decrypt`。
4. **不要绕过 ConfigService**：所有数据源读写必须经过 `ConfigService`，禁止 Controller 直接操作文件系统，确保内存缓存与磁盘一致性。
5. **Profile 覆盖原则**：仅在 `application-{profile}.yml` 中覆盖差异项（如端口、日志级别），不要复制完整 `application.yml`。
6. **部署时通过 `--spring.config.location` 指定外部配置**：参考 `deploy/start.sh`，生产环境应将 `application.yml` 和 `schemasync-config.json` 放在容器卷或宿主机可持久化路径。

## 5. 已知局限

- 加密密钥 `SECRET_KEY = "SchemaSync2026!!"` 硬编码在 `CryptoUtil` 中，未从配置或环境变量注入，跨节点共享同一密钥存在安全风险。
- 配置变更仅内存生效，重启后从 JSON 重新加载；当前无热重载机制。
- 未引入 Spring `@ConfigurationProperties` 绑定，自定义 `schemasync.*` 配置未做类型安全校验。