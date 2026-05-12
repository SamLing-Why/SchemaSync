<template>
  <div class="diff-view">
    <el-card>
      <template #header>
        <h2>版本对比</h2>
      </template>

      <el-form label-width="120px">
        <el-form-item label="旧版本文件">
          <el-upload
            ref="oldUpload"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            @change="handleOldFile"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">请选择旧版本数据字典文件(Excel)</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="新版本文件">
          <el-upload
            ref="newUpload"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            @change="handleNewFile"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">请选择新版本数据字典文件(Excel)</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleCompare" :loading="comparing">
            <el-icon><Compare /></el-icon>
            开始对比
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 差异结果 -->
      <el-card v-if="diffResult" style="margin-top: 20px;">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <h3>差异统计</h3>
            <div>
              <el-button type="success" @click="downloadDiff">
                <el-icon><Download /></el-icon>
                下载差异报告
              </el-button>
              <el-button type="warning" @click="generateDdl" :loading="generatingDdl">
                <el-icon><Download /></el-icon>
                生成DDL脚本
              </el-button>
            </div>
          </div>
        </template>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="新增表">{{ diffResult.summary?.tablesAdded || 0 }}</el-descriptions-item>
          <el-descriptions-item label="删除表">{{ diffResult.summary?.tablesDropped || 0 }}</el-descriptions-item>
          <el-descriptions-item label="修改表">{{ diffResult.summary?.tablesModified || 0 }}</el-descriptions-item>
          <el-descriptions-item label="新增字段">{{ diffResult.summary?.columnsAdded || 0 }}</el-descriptions-item>
          <el-descriptions-item label="删除字段">{{ diffResult.summary?.columnsDropped || 0 }}</el-descriptions-item>
          <el-descriptions-item label="修改字段">{{ diffResult.summary?.columnsModified || 0 }}</el-descriptions-item>
          <el-descriptions-item label="破坏性变更" :span="3">
            <el-tag type="danger">{{ diffResult.summary?.breakingChanges || 0 }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          title="提示"
          type="info"
          description="差异明细数据较多，请通过【下载差异报告】查看完整的变更明细。"
          show-icon
          :closable="false"
          style="margin-top: 20px;"
        />
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Switch as Compare, Download } from '@element-plus/icons-vue'

const oldFile = ref(null)
const newFile = ref(null)
const oldFileBuffer = ref(null)  // 保存文件内容
const newFileBuffer = ref(null)  // 保存文件内容
const comparing = ref(false)
const generatingDdl = ref(false)
const diffResult = ref(null)

const handleOldFile = async (file) => {
  oldFile.value = file.raw
  // 保存文件内容
  try {
    oldFileBuffer.value = await file.raw.arrayBuffer()
  } catch (e) {
    console.error('读取旧文件失败:', e)
  }
}

const handleNewFile = async (file) => {
  newFile.value = file.raw
  // 保存文件内容
  try {
    newFileBuffer.value = await file.raw.arrayBuffer()
  } catch (e) {
    console.error('读取新文件失败:', e)
  }
}

const handleCompare = async () => {
  if (!oldFileBuffer.value || !newFileBuffer.value) {
    ElMessage.warning('请选择两个文件')
    return
  }

  comparing.value = true
  try {
    const formData = new FormData()
    formData.append('oldFile', new Blob([oldFileBuffer.value]), oldFile.value.name)
    formData.append('newFile', new Blob([newFileBuffer.value]), newFile.value.name)

    const response = await fetch('/api/diff/summary', {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error('对比失败')
    }

    diffResult.value = await response.json()
    ElMessage.success('对比完成')
  } catch (error) {
    ElMessage.error('对比失败: ' + error.message)
  } finally {
    comparing.value = false
  }
}

const downloadDiff = async () => {
  if (!oldFileBuffer.value || !newFileBuffer.value) {
    ElMessage.warning('请先选择文件并进行对比')
    return
  }

  comparing.value = true
  try {
    const formData = new FormData()
    formData.append('oldFile', new Blob([oldFileBuffer.value]), oldFile.value.name)
    formData.append('newFile', new Blob([newFileBuffer.value]), newFile.value.name)
    formData.append('exportFormat', 'excel')  // 固定为Excel

    const response = await fetch('/api/diff', {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error('下载失败')
    }

    const blob = await response.blob()
    
    // 检查blob是否有效
    if (!blob || blob.size === 0) {
      throw new Error('下载的文件为空')
    }
    
    // 从响应头获取文件名
    let fileName = `diff_${Date.now()}.xlsx`
    const contentDisposition = response.headers.get('Content-Disposition')
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (fileNameMatch && fileNameMatch[1]) {
        fileName = decodeURIComponent(fileNameMatch[1].replace(/['"]/g, ''))
      }
    }
    
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)

    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载差异报告失败:', error)
    ElMessage.error('下载失败: ' + (error.message || '未知错误'))
  } finally {
    comparing.value = false
  }
}

const generateDdl = async () => {
  if (!diffResult.value) {
    ElMessage.warning('请先进行对比')
    return
  }

  generatingDdl.value = true
  try {
    const formData = new FormData()
    formData.append('oldFile', oldFile.value)
    formData.append('newFile', newFile.value)

    const response = await fetch('/api/diff/ddl', {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error('DDL生成失败')
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `ddl_${Date.now()}.sql`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)

    ElMessage.success('DDL脚本生成成功')
  } catch (error) {
    ElMessage.error('DDL生成失败: ' + error.message)
  } finally {
    generatingDdl.value = false
  }
}

const getChangeTypeTag = (type) => {
  const map = {
    'TABLE_ADD': 'success',
    'TABLE_DROP': 'danger',
    'TABLE_MODIFY': 'warning',
    'COLUMN_ADD': 'success',
    'COLUMN_DROP': 'danger',
    'COLUMN_MODIFY': 'warning'
  }
  return map[type] || 'info'
}

const getChangeTypeLabel = (type) => {
  const map = {
    'TABLE_ADD': '新增表',
    'TABLE_DROP': '删除表',
    'TABLE_MODIFY': '修改表',
    'COLUMN_ADD': '新增字段',
    'COLUMN_DROP': '删除字段',
    'COLUMN_MODIFY': '修改字段'
  }
  return map[type] || type
}

const getSeverityTag = (severity) => {
  const map = {
    'HIGH': 'danger',
    'MEDIUM': 'warning',
    'LOW': 'info'
  }
  return map[severity] || 'info'
}

const getSeverityLabel = (severity) => {
  const map = {
    'HIGH': '高',
    'MEDIUM': '中',
    'LOW': '低'
  }
  return map[severity] || severity
}

const formatDetails = (details) => {
  if (!details) return '-'
  if (typeof details === 'string') return details
  return JSON.stringify(details)
}
</script>

<style scoped>
.diff-view {
  max-width: 800px;
}
</style>
