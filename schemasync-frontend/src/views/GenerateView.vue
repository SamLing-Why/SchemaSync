<template>
  <div class="generate-view">
    <el-card>
      <template #header>
        <h2>全量DDL脚本生成</h2>
      </template>

      <el-form label-width="120px">
        <el-form-item label="文件格式">
          <el-radio-group v-model="fileType">
            <el-radio label="excel">Excel</el-radio>
            <el-radio label="json">JSON</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="数据字典文件">
          <el-upload
            :auto-upload="false"
            :limit="1"
            :accept="fileType === 'excel' ? '.xlsx,.xls' : '.json'"
            @change="handleFile"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">请选择数据字典文件({{ fileType === 'excel' ? 'Excel' : 'JSON' }})</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="数据库类型">
          <el-select v-model="form.databaseType">
            <el-option label="MySQL" value="mysql" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleGenerate" :loading="generating">
            <el-icon><Document /></el-icon>
            生成DDL脚本
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 预览 -->
      <el-card v-if="ddlScript" style="margin-top: 20px;">
        <template #header>
          <div style="display: flex; justify-content: space-between;">
            <h3>DDL脚本预览</h3>
            <el-button type="success" @click="downloadDDL">
              <el-icon><Download /></el-icon>
              下载SQL文件
            </el-button>
          </div>
        </template>

        <pre class="ddl-preview">{{ ddlScript }}</pre>
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Download } from '@element-plus/icons-vue'

const selectedFile = ref(null)
const generating = ref(false)
const ddlScript = ref('')
const fileType = ref('excel')

const form = ref({
  databaseType: 'mysql'
})

const handleFile = (file) => {
  selectedFile.value = file.raw
}

const handleGenerate = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  generating.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('fileType', fileType.value)
    formData.append('databaseType', form.value.databaseType)

    const response = await fetch('/api/ddl/preview', {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error('生成失败')
    }

    ddlScript.value = await response.text()
    ElMessage.success('生成成功')
  } catch (error) {
    ElMessage.error('生成失败: ' + error.message)
  } finally {
    generating.value = false
  }
}

const downloadDDL = async () => {
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('fileType', fileType.value)
    formData.append('databaseType', form.value.databaseType)

    const response = await fetch('/api/ddl/download', {
      method: 'POST',
      body: formData
    })

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `ddl_${Date.now()}.sql`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)

    ElMessage.success('下载成功')
  } catch (error) {
    ElMessage.error('下载失败')
  }
}
</script>

<style scoped>
.generate-view {
  max-width: 800px;
}

.ddl-preview {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 20px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  max-height: 600px;
  overflow-y: auto;
}
</style>
