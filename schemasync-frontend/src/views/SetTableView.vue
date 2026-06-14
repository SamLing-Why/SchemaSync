<template>
  <div class="settable-view">
    <el-card>
      <template #header>
        <h2>套表处理</h2>
      </template>

      <el-alert
        title="套表说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <p><strong>套表定义</strong>：表名前缀相同，后缀为 _tp、_fo、_ar 的表组（至少2张）</p>
        <p><strong>示例</strong>：order_tp、order_fo、order_ar 是一组套表（前缀：order）</p>
        <p><strong>功能1 - 套表还原导出</strong>：套表按优先级保留一张（fo > tp > ar），导出完整数据字典</p>
        <p><strong>功能2 - 套表差异比对</strong>：比对套表内部的字段差异（类型、长度、精度等）</p>
      </el-alert>

      <el-form label-width="120px">
        <el-form-item label="数据字典文件">
          <el-upload
            ref="fileUpload"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            @change="handleFile"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">请上传数据字典Excel文件</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="success" @click="handleExport" :loading="exporting" :disabled="!selectedFile">
            <el-icon><Download /></el-icon>
            套表还原导出
          </el-button>
          <el-button type="warning" @click="handleCompare" :loading="comparing" :disabled="!selectedFile">
            <el-icon><Compare /></el-icon>
            套表差异比对
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 统计信息 -->
      <el-card v-if="stats" style="margin-top: 20px;">
        <template #header>
          <h3>处理结果</h3>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="总表数">{{ stats.totalTables }}</el-descriptions-item>
          <el-descriptions-item label="套表组数">{{ stats.setTableGroups }}</el-descriptions-item>
          <el-descriptions-item label="还原后表数">{{ stats.reducedTables }}</el-descriptions-item>
          <el-descriptions-item label="差异数量">{{ stats.diffCount }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Switch as Compare } from '@element-plus/icons-vue'
import axios from 'axios'

const selectedFile = ref(null)
const fileUpload = ref(null)
const exporting = ref(false)
const comparing = ref(false)
const stats = ref(null)

// 处理文件选择
const handleFile = (file) => {
  selectedFile.value = file.raw
  stats.value = null
}

// 套表还原导出
const handleExport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  exporting.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    const response = await axios.post('/api/settable/export', formData, {
      responseType: 'blob'
    })

    // 下载文件
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    
    // 从响应头获取文件名
    const disposition = response.headers['content-disposition']
    let filename = 'SchemaSync_SetTable_Reduced.xlsx'
    if (disposition) {
      const matches = /filename="(.+)"/.exec(disposition)
      if (matches && matches[1]) {
        filename = matches[1]
      }
    }
    
    link.download = filename
    link.click()
    window.URL.revokeObjectURL(url)

    ElMessage.success('套表还原导出成功')
  } catch (error) {
    console.error('套表还原导出失败:', error)
    ElMessage.error('套表还原导出失败: ' + (error.response?.data || error.message))
  } finally {
    exporting.value = false
  }
}

// 套表差异比对
const handleCompare = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  comparing.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    const response = await axios.post('/api/settable/compare', formData, {
      responseType: 'blob'
    })

    // 下载文件
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    
    // 从响应头获取文件名
    const disposition = response.headers['content-disposition']
    let filename = 'SchemaSync_SetTable_Diff.xlsx'
    if (disposition) {
      const matches = /filename="(.+)"/.exec(disposition)
      if (matches && matches[1]) {
        filename = matches[1]
      }
    }
    
    link.download = filename
    link.click()
    window.URL.revokeObjectURL(url)

    ElMessage.success('套表差异比对成功')
  } catch (error) {
    console.error('套表差异比对失败:', error)
    ElMessage.error('套表差异比对失败: ' + (error.response?.data || error.message))
  } finally {
    comparing.value = false
  }
}
</script>

<style scoped>
.settable-view {
  padding: 20px;
}

.el-alert p {
  margin: 5px 0;
  line-height: 1.6;
}
</style>
