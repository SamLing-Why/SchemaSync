<template>
  <div class="generate-view">
    <el-card>
      <template #header>
        <h2>全量DDL脚本生成</h2>
      </template>

      <el-form label-width="120px">
        <el-form-item label="数据字典文件">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            @change="handleFile"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">请选择数据字典文件({{ fileType === 'excel' ? 'Excel' : 'JSON' }})</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="数据库类型">
          <el-select v-model="form.databaseType" placeholder="请选择数据库类型">
            <el-option label="MySQL" value="mysql" />
            <el-option label="GaussDB (MySQL兼容模式)" value="gaussdb_mysql" />
            <el-option label="GaussDB (Oracle兼容模式)" value="gaussdb_oracle" />
            <el-option label="GaussDB (PG模式)" value="gaussdb_pg" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleGenerate" :loading="generating">
            <el-icon><Document /></el-icon>
            生成DDL脚本
          </el-button>
          <el-button type="warning" @click="handleValidate" :loading="validating">
            <el-icon><Warning /></el-icon>
            校验数据字典
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 校验结果 -->
      <el-card v-if="validationResult" style="margin-top: 20px;">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <h3>校验结果</h3>
            <el-button type="success" @click="downloadValidation">
              <el-icon><Download /></el-icon>
              下载校验报告
            </el-button>
          </div>
        </template>

        <el-alert
          v-if="validationResult.issueCount === 0"
          title="校验通过"
          type="success"
          description="数据字典校验通过，未发现问题"
          show-icon
          :closable="false"
        />
        <div v-else>
          <el-alert
            :title="`发现 ${validationResult.issueCount} 个问题`"
            type="warning"
            show-icon
            :closable="false"
            style="margin-bottom: 15px;"
          />
          <el-table :data="validationResult.issues" stripe style="width: 100%;">
            <el-table-column prop="tableName" label="表名" width="200" />
            <el-table-column prop="tableComment" label="表注释" width="200" />
            <el-table-column prop="checkItem" label="校验项" width="150" />
            <el-table-column prop="issue" label="校验问题描述" />
          </el-table>
        </div>
      </el-card>

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
import { Document, Download, Warning } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'

const selectedFile = ref(null)
const generating = ref(false)
const validating = ref(false)
const ddlScript = ref('')
const fileType = ref('excel')
const validationResult = ref(null)

const form = ref({
  databaseType: 'mysql'
})

const handleFile = (file) => {
  selectedFile.value = file.raw
  validationResult.value = null  // 清空校验结果
}

const handleValidate = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  validating.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('fileType', 'excel')

    const response = await fetch('/api/ddl/validate', {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error('校验失败')
    }

    // 解析Excel文件
    const blob = await response.blob()
    const arrayBuffer = await blob.arrayBuffer()
    
    // 使用xlsx库解析Excel
    const workbook = XLSX.read(arrayBuffer, { type: 'array' })
    const sheetName = workbook.SheetNames[0]
    const worksheet = workbook.Sheets[sheetName]
    const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 })
    
    // 跳过表头，解析数据行
    const issues = []
    if (jsonData.length > 1) {
      for (let i = 1; i < jsonData.length; i++) {
        const row = jsonData[i]
        if (row && row.length >= 4) {
          // 跳过"无问题"行
          if (row[0] === '无问题') {
            continue
          }
          issues.push({
            tableName: row[0] || '',
            tableComment: row[1] || '',
            checkItem: row[2] || '',
            issue: row[3] || ''
          })
        }
      }
    }
    
    // 设置校验结果
    validationResult.value = {
      issueCount: issues.length,
      issues: issues
    }
    
    // 同时下载Excel文件
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `validation_${Date.now()}.xlsx`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
    
    if (issues.length > 0) {
      ElMessage.warning(`校验完成，发现 ${issues.length} 个问题`)
    } else {
      ElMessage.success('校验完成，未发现问题')
    }
  } catch (error) {
    ElMessage.error('校验失败: ' + error.message)
  } finally {
    validating.value = false
  }
}

const downloadValidation = async () => {
  // 校验报告已经在handleValidate中下载了
  ElMessage.info('校验报告已在上方自动下载，如未下载请重新点击校验')
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
    formData.append('fileType', 'excel')  // 固定为Excel
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
