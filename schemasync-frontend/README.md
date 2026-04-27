# SchemaSync 前端项目

## 📦 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Element Plus** - Vue 3 UI组件库
- **Vue Router** - 官方路由管理器
- **Axios** - HTTP客户端
- **Vite** - 下一代前端构建工具

## 🚀 快速启动

### 1. 安装依赖

```bash
cd schemasync-frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

构建产物在 `dist/` 目录

### 4. 预览生产构建

```bash
npm run preview
```

## 📁 项目结构

```
schemasync-frontend/
├── package.json              # 项目配置
├── vite.config.js            # Vite配置
├── index.html                # 入口HTML
├── src/
│   ├── main.js               # 应用入口
│   ├── App.vue               # 根组件
│   ├── router/
│   │   └── index.js          # 路由配置
│   ├── api/
│   │   ├── request.js        # Axios封装
│   │   └── config.js         # 配置API
│   └── views/
│       ├── ConfigView.vue    # 数据源配置页面
│       ├── ExportView.vue    # 数据字典导出页面
│       ├── DiffView.vue      # 版本对比页面
│       └── GenerateView.vue  # 差异化DDL脚本生成页面
└── dist/                     # 构建产物
```

## 🎯 功能页面

### 1. 数据源配置 (`/config`)

- 查看数据源列表
- 新增数据源配置
- 删除数据源
- 测试数据库连接

### 2. 数据字典导出 (`/export`)

- 选择数据源
- 输入数据库名
- 选择导出格式(JSON)
- 下载数据字典文件

### 3. 版本对比 (`/diff`)

- 上传旧版本JSON文件
- 上传新版本JSON文件
- 查看差异统计
- 下载差异报告

### 4. 差异化DDL脚本生成 (`/generate`)

- 上传两个版本的JSON文件
- 配置生成选项
- 预览DDL脚本
- 下载SQL文件

## 🔧 开发说明

### API代理配置

开发模式下,`/api`请求会自动代理到后端服务器:

```javascript
// vite.config.js
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

**注意**: 确保后端服务在8080端口运行

### 组件库使用

本项目使用Element Plus组件库,已全局注册:

```vue
<template>
  <el-button type="primary">按钮</el-button>
  <el-table :data="tableData">
    <!-- ... -->
  </el-table>
</template>
```

所有Element Plus图标也已全局注册:

```vue
<template>
  <el-icon><Download /></el-icon>
  <el-icon><Setting /></el-icon>
</template>
```

## 📝 环境要求

- Node.js >= 16.0.0
- npm >= 7.0.0

## 🎨 UI设计

### 布局

- 顶部: 渐变色导航栏(紫色系)
- 左侧: 功能菜单
- 主区域: 内容展示
- 底部: 版本信息

### 主题色

- 主色: #667eea (紫色)
- 渐变色: linear-gradient(135deg, #667eea 0%, #764ba2 100%)
- 背景色: #f5f7fa

## 🔗 后端API

确保后端服务运行在 `http://localhost:8080`

API文档: http://localhost:8080/swagger-ui.html

## 📊 项目统计

- 页面数量: 4个
- 组件数量: 1个根组件
- 代码行数: ~800行
- 依赖包: 6个核心依赖

---

**SchemaSync Team** © 2026
