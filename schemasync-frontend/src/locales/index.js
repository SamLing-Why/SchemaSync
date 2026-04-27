import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

// 获取浏览器语言或本地存储的语言设置
const getLanguage = () => {
  const stored = localStorage.getItem('language')
  if (stored && ['zh-CN', 'en-US'].includes(stored)) {
    return stored
  }
  // 默认使用中文
  return 'zh-CN'
}

const i18n = createI18n({
  legacy: false, // 使用Composition API模式
  locale: getLanguage(), // 默认语言
  fallbackLocale: 'zh-CN', // 回退语言
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export default i18n
