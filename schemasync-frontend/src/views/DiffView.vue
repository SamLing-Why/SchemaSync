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
            accept=".json"
            @change="handleOldFile"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">请选择旧版本数据字典JSON文件</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="新版本文件">
          <el-upload
            ref="newUpload"
            :auto-upload="false"
            :limit="1"
            accept=".json"
            @change="handleNewFile"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">请选择新版本数据字典JSON文件</div>
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
          <h3>差异统计</h3>
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

        <el-button type="success" @click="downloadDiff" style="margin-top: 20px;">
          <el-icon><Download /></el-icon>
          下载差异报告
        </el-button>
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
const comparing = ref(false)
const diffResult = ref(null)

const handleOldFile = (file) => {
  oldFile.value = file.raw
}

const handleNewFile = (file) => {
  newFile.value = file.raw
}

const handleCompare = async () => {
  if (!oldFile.value || !newFile.value) {
    ElMessage.warning('请选择两个文件')
    return
  }

  comparing.value = true
  try {
    const formData = new FormData()
    formData.append('oldFile', oldFile.value)
    formData.append('newFile', newFile.value)

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
  try {
    const formData = new FormData()
    formData.append('oldFile', oldFile.value)
    formData.append('newFile', newFile.value)

    const response = await fetch('/api/diff', {
      method: 'POST',
      body: formData
    })

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `diff_${Date.now()}.json`
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
.diff-view {
  max-width: 800px;
}
</style>
