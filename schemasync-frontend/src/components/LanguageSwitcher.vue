<template>
  <el-dropdown @command="handleLanguageChange" trigger="click">
    <span class="language-switcher">
      <el-icon><Switch /></el-icon>
      {{ currentLanguage === 'zh-CN' ? '中文' : 'English' }}
      <el-icon class="el-icon--right"><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN" :disabled="currentLanguage === 'zh-CN'">
          🇨🇳 中文
        </el-dropdown-item>
        <el-dropdown-item command="en-US" :disabled="currentLanguage === 'en-US'">
          🇺🇸 English
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Switch, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const { locale } = useI18n()
const currentLanguage = ref('zh-CN')

onMounted(() => {
  currentLanguage.value = locale.value
})

const handleLanguageChange = (lang) => {
  locale.value = lang
  currentLanguage.value = lang
  localStorage.setItem('language', lang)
  
  // 更新Element Plus语言
  updateElementPlusLocale(lang)
  
  ElMessage.success(lang === 'zh-CN' ? '语言已切换为中文' : 'Language switched to English')
}

const updateElementPlusLocale = (lang) => {
  // 动态导入Element Plus语言包
  if (lang === 'en-US') {
    import('element-plus/dist/locale/en.mjs').then((module) => {
      // Element Plus会自动检测并更新
      window.location.reload() // 简单刷新页面应用新语言
    })
  } else {
    import('element-plus/dist/locale/zh-cn.mjs').then((module) => {
      window.location.reload()
    })
  }
}
</script>

<style scoped>
.language-switcher {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: white;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.language-switcher:hover {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>
