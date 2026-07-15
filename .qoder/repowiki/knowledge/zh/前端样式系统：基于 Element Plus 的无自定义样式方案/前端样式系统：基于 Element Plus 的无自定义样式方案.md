---
kind: frontend_style
name: 前端样式系统：基于 Element Plus 的无自定义样式方案
category: frontend_style
scope:
    - '**'
source_files:
    - schemasync-frontend/package.json
    - schemasync-frontend/vite.config.js
---

该仓库的前端采用 Vue3 + Vite + Element Plus 技术栈，但**未建立任何自定义 CSS/SCSS 样式体系**。经全面扫描未发现任何 `.css`、`.scss`、`.less` 等样式文件，也未在 `main.js`、`App.vue` 或组件中引入外部样式资源。项目完全依赖 Element Plus 组件库提供的默认主题与样式，通过其内置组件（如 el-table、el-form、el-button 等）构建界面，未使用 CSS Modules、CSS-in-JS、Tailwind 等现代样式方案，也未定义设计令牌或全局主题变量。这种“零样式”策略意味着所有视觉呈现均由 Element Plus 默认主题决定，缺乏品牌化定制能力，属于最小化的 UI 实现方式。