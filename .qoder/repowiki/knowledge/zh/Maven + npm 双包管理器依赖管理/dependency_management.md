## 1. 使用的系统/方法
- 后端（Java）：使用 Maven 作为构建与依赖管理工具，基于 spring-boot-starter-parent 2.7.18 作为父 POM，通过 properties 集中声明第三方库版本。
- 前端（Vue3）：使用 npm + Vite，通过 package.json 声明运行时与开发时依赖，并通过 package-lock.json 锁定精确版本。
- 聚合构建：后端通过 frontend-maven-plugin 在 Maven 生命周期中自动安装 Node.js、执行 npm install 和 npm run build，并将产物复制到 target/classes/static，实现前后端单体打包。

## 2. 关键文件与包
- schemasync-backend/pom.xml：后端所有依赖声明、版本集中管理、插件配置（Spring Boot、Frontend Maven Plugin、Compiler、Resources）。
- schemasync-frontend/package.json：前端依赖清单与脚本入口。
- schemasync-frontend/package-lock.json：npm 锁文件，记录完整依赖树及每个包的 resolved URL 与 integrity 校验值。
- deploy/schemasync_1.0.3.jar：Maven repackage 生成的可执行 Spring Boot 应用包。
- build.sh / build.bat：顶层便捷脚本，调用 Maven 完成前后端一体化构建。

## 3. 架构与约定
- 版本集中化：后端将 MySQL、Oracle、PostgreSQL、OpenGauss 驱动、POI、Fastjson2、HikariCP、Picocli 等版本统一放在 properties 中，新增依赖时应优先复用已有属性或在此处新增，避免散落在 dependency version 里。
- 可选依赖标记：Lombok、Configuration Processor 使用 optional=true，不随传递性引入下游模块。
- 测试隔离：Junit、spring-boot-starter-test 使用 scope=test，不参与生产包。
- 数据库驱动按需引入：MySQL、Oracle、PostgreSQL、OpenGauss 驱动均声明为直接依赖，由运行期通过 SPI/反射加载，便于按部署环境裁剪。
- 前端锁定策略：使用 npm v3 lockfile，package-lock.json 已提交仓库，保证 CI/CD 与本地安装结果一致；依赖范围采用 ^ 语义化版本，允许小版本升级但受锁文件约束。
- Node 版本固化：frontend-maven-plugin 显式指定 nodeVersion=v18.19.0、npmVersion=10.2.3，避免不同机器 Node 差异导致构建不一致。
- 私有源/镜像：从 package-lock.json 可见 resolved 指向 https://registry.npmmirror.com，说明项目默认使用国内镜像源（可通过 .npmrc 进一步定制）。

## 4. 开发者应遵循的规则
- 添加后端依赖：先在 properties 中定义版本号，再在 dependencies 中引用该属性；若仅用于编译期（如 Lombok），务必加上 optional=true；测试相关依赖加 scope=test。
- 添加前端依赖：仅在 package.json 的 dependencies 或 devDependencies 中声明；修改后运行 npm install 并检查 package-lock.json 变更，确保锁文件同步提交。
- 版本升级：后端统一修改 properties 中的版本变量，然后执行 mvn dependency:tree 确认无冲突；前端使用 npm update <pkg> 或手动调整 ^x.y.z，再提交 package-lock.json。
- 构建一致性：不要绕过 Maven 直接在前端目录执行 npm install 参与 CI；统一通过 ./build.sh 或 mvn package 触发，以确保 Node 版本与依赖树固定。
- 生产包裁剪：Lombok 已在 spring-boot-maven-plugin 的 excludes 中排除，新增编译期注解处理器也应遵循此模式，避免打入最终 JAR。