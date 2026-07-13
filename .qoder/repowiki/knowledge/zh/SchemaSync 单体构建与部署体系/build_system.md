## 构建系统概览

SchemaSync 采用 Maven 单仓聚合的构建方式，通过 Maven 插件在 Java 构建流程中自动完成前端 Vue3 应用的打包，最终输出一个包含前后端资源的 Spring Boot JAR 包。

### 核心构建工具链
- 后端: Maven + Spring Boot 2.7.18 (Java 8)
- 前端: Vite 5 + Vue3，由 frontend-maven-plugin 在 Maven 生命周期中管理 Node.js 环境
- 打包产物: 单一可执行 JAR (schemasync.jar)，内嵌静态资源

### 关键构建文件
- schemasync-backend/pom.xml: 核心构建配置，集成前后端构建、多环境 Profile、Spring Boot 重打包
- build.sh / build.bat: 跨平台一键打包脚本，封装完整构建流程
- deploy/start.sh / deploy/start.bat: 生产启动脚本，支持后台运行和日志输出
- deploy/application.yml: 外置配置文件，支持运行时覆盖
- schemasync-frontend/package.json: 前端依赖与构建脚本定义

### 构建流水线
开发或 CI 触发 -> build.sh 或 build.bat 执行 -> 检查 JDK 8+ 和 Maven 3.6+ 环境 -> 进入 schemasync-backend 目录 -> mvn clean package -DskipTests -> 复制 JAR 和资源到 deploy/ 目录 -> 生成 start.sh/bat 启动脚本

其中 mvn package 阶段会依次执行:
- frontend-maven-plugin: 安装 Node v18.19.0 + npm 10.2.3
- npm install: 安装前端依赖
- npm run build: Vite 构建 dist/
- maven-resources-plugin: 复制 dist/ 到 resources/static/
- spring-boot-maven-plugin: repackage 生成可执行 JAR

### 多环境配置策略
通过 Maven Profile 切换环境:
- dev (默认): 使用 application-dev.yml
- prod: 使用 application-prod.yml
构建时可通过 -Pprod 激活生产环境，Spring Boot 会加载对应配置文件。

### 版本管理
- 后端版本: 1.0.3 (pom.xml)
- 前端版本: 1.0.3 (package.json)
- 构建快照: 1.0.0-SNAPSHOT (build.sh 中硬编码)

### 部署架构
应用以 fat JAR 形式部署，无需外部 Web 服务器:
- 端口: 8080 (开发) / 8999 (生产脚本)
- 内存: -Xms256m -Xmx512m
- 配置: 通过 --spring.config.location=application.yml 指定外置配置
- 日志: Linux 使用 nohup 后台运行，输出到 app.log

### 开发者约定
1. 构建入口: 统一使用根目录的 build.sh 或 build.bat，避免手动执行 Maven 命令
2. 前端修改: 修改 schemasync-frontend/ 后需重新执行构建脚本
3. 环境切换: 通过 Maven Profile 控制，不直接修改配置文件
4. 版本同步: 前后端版本号应保持一致，便于发布追踪
5. 测试跳过: 构建脚本默认 -DskipTests，CI 环境建议单独配置测试阶段