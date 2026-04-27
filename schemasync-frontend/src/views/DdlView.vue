<template>
  <div class="ddl-view">
    <el-card>
      <template #header>
        <h2>全量DDL脚本生成</h2>
      </template>

      <el-alert
        title="功能说明"
        type="info"
        description="基于数据字典文件(JSON或Excel)生成完整的CREATE TABLE语句。上传导出的数据字典文件，即可生成对应的DDL SQL脚本。"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <el-form label-width="120px">
        <el-form-item label="数据字典文件">
          <el-upload
            ref="upload"
            :auto-upload="false"
            :limit="1"
            accept=".json,.xlsx,.xls"
            @change="handleFile"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                请选择数据字典文件(JSON或Excel，默认Excel)
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleGenerate" :loading="generating">
            <el-icon><Download /></el-icon>
            生成DDL
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'

const selectedFile = ref(null)
const generating = ref(false)

const handleFile = (file) => {
  selectedFile.value = file.raw
}

const handleGenerate = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择数据字典文件')
    return
  }

  generating.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    const response = await fetch('/api/ddl/generate', {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || '生成失败')
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = selectedFile.value.name.replace(/\.(json|xlsx|xls)$/, '.sql')
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)

    ElMessage.success('DDL生成成功')
  } catch (error) {
    ElMessage.error('生成失败: ' + error.message)
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.ddl-view {
  max-width: 800px;
}
</style>
