<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-content">
        <h1 class="logo">
          <el-icon><Database /></el-icon>
          {{ t('common.appName') }}
        </h1>
        <p class="subtitle">{{ t('common.appSubtitle') }}</p>
      </div>
      <LanguageSwitcher />
    </el-header>
    
    <el-container>
      <el-aside width="200px">
        <el-menu
          :default-active="$route.path"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/config">
            <el-icon><Setting /></el-icon>
            <span>{{ t('menu.config') }}</span>
          </el-menu-item>
          <el-menu-item index="/export">
            <el-icon><Download /></el-icon>
            <span>{{ t('menu.export') }}</span>
          </el-menu-item>
          <el-menu-item index="/diff">
            <el-icon><Compare /></el-icon>
            <span>{{ t('menu.diff') }}</span>
          </el-menu-item>
          <el-menu-item index="/generate">
            <el-icon><Document /></el-icon>
            <span>{{ t('menu.generate') }}</span>
          </el-menu-item>
          <el-menu-item index="/settable">
            <el-icon><Grid /></el-icon>
            <span>{{ t('menu.settable') }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
    
    <el-footer class="layout-footer">
      <p>{{ t('footer.copyright') }}</p>
    </el-footer>
  </el-container>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { Connection as Database, Setting, Download, Switch as Compare, Files as Document, Grid } from '@element-plus/icons-vue'
import LanguageSwitcher from './components/LanguageSwitcher.vue'

const { t } = useI18n()
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-content {
  display: flex;
  align-items: baseline;
  gap: 15px;
}

.logo {
  margin: 0;
  font-size: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.subtitle {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.sidebar-menu {
  border-right: none;
  height: 100%;
}

.layout-main {
  background: #f5f7fa;
  padding: 20px;
}

.layout-footer {
  text-align: center;
  color: #909399;
  font-size: 12px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}
</style>
