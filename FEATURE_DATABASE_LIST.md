# 数据字典导出功能优化说明

## 功能概述

优化数据字典导出功能，支持选择数据源后自动查询数据库列表，并提供下拉选择。

## 主要改进

### 1. 后端API增强

**新增接口**：`GET /api/export/databases?configName=xxx`

- **功能**：根据数据源配置名称获取数据库列表
- **支持数据库**：
  - MySQL/OceanBase/TDSQL/GoldenDB：使用 `SHOW DATABASES`
  - GaussDB/OpenGauss：使用 `SELECT datname FROM pg_database`
  - Oracle：使用 `SELECT USERNAME FROM ALL_USERS`
- **特性**：
  - 自动过滤系统数据库
  - 支持密码加密/解密
  - 连接池管理

### 2. 前端交互优化

**ExportView.vue 改进**：

#### 数据库选择方式

- **修改前**：手动输入数据库名
- **修改后**：下拉选择 + 手动输入（支持过滤和自定义输入）

#### 自动加载机制

1. 选择数据源后自动加载数据库列表
2. 点击数据库下拉框时懒加载（如果未加载过）
3. 加载过程显示loading状态
4. 加载成功显示数据库数量提示

#### 用户体验

- ✅ 下拉选择：快速选择已知数据库
- ✅ 过滤搜索：输入关键字过滤数据库列表
- ✅ 手动输入：支持输入不在列表中的数据库名
- ✅ 状态提示：加载中、加载成功、加载失败都有明确提示

## 技术实现

### 后端实现

**ExportController.java**

```java
@GetMapping("/databases")
public ResponseEntity<List<String>> getDatabases(@RequestParam String configName)
```

**核心流程**：

1. 参数校验
2. 获取数据源配置
3. 解密密码
4. 获取对应数据库适配器
5. 连接数据库并查询数据库列表
6. 返回数据库名称列表

### 前端实现

**config.js API**

```javascript
export function getDatabases(configName) {
  return request.get("/export/databases", { params: { configName } });
}
```

**ExportView.vue 组件**

```vue
<el-select
  v-model="form.database"
  placeholder="请选择或输入数据库名"
  :loading="loadingDatabases"
  filterable
  allow-create
  default-first-option
  @focus="onDatabaseFocus"
>
  <el-option
    v-for="db in databaseList"
    :key="db"
    :label="db"
    :value="db"
  />
</el-select>
```

**核心逻辑**：

- `onDataSourceChange()`：数据源改变时清空并重新加载数据库列表
- `loadDatabases()`：调用API获取数据库列表
- `onDatabaseFocus()`：下拉框获得焦点时懒加载

## 多数据库支持

### MySQL兼容系列

- **MySQL**
- **OceanBase** (MySQL模式)
- **TDSQL** (MySQL模式)
- **GoldenDB** (MySQL模式)

**查询SQL**：

```sql
SHOW DATABASES
```

**过滤规则**：

- information_schema
- mysql
- performance_schema
- sys
- goldendb (GoldenDB特有)

### PostgreSQL兼容系列

- **GaussDB**
- **OpenGauss**

**查询SQL**：

```sql
SELECT datname FROM pg_database WHERE datistemplate = false
```

**过滤规则**：

- postgres
- template0
- template1

### Oracle

**查询SQL**：

```sql
SELECT USERNAME FROM ALL_USERS ORDER BY USERNAME
```

## 使用示例

### 1. 导出数据字典

1. 进入"数据字典导出"页面
2. 选择数据源（如：OpenGuass(内网)）
3. 系统自动加载数据库列表
4. 从下拉框选择数据库（如：feboia_design_db）
5. 选择导出格式（Excel/JSON）
6. 点击"导出"按钮

### 2. API调用示例

```bash
# 获取数据库列表
curl http://localhost:8999/api/export/databases?configName=OpenGuass(内网)

# 响应示例
[
  "feboia_design_db",
  "feboia_runtime_db",
  "test_db"
]
```

## 优势

1. **减少输入错误**：下拉选择避免手动输入错误
2. **提升效率**：自动加载，无需记忆数据库名称
3. **灵活性强**：支持过滤搜索和手动输入
4. **多数据库兼容**：适配6种数据库类型
5. **用户体验好**：清晰的状态提示和loading效果

## 注意事项

1. **网络依赖**：需要连接数据库才能获取列表
2. **权限要求**：数据源用户需要有查看数据库的权限
3. **性能考虑**：数据库列表会缓存，避免重复查询
4. **手动输入**：仍支持手动输入数据库名（allow-create）

## 后续优化建议

1. **缓存机制**：缓存数据库列表，减少重复查询
2. **刷新按钮**：添加手动刷新数据库列表的功能
3. **批量导出**：支持一次导出多个数据库
4. **Schema支持**：对于PostgreSQL/GaussDB，支持选择Schema
