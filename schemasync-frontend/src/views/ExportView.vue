<template>
  <div class="export-view">
    <el-card>
      <template #header>
        <h2>数据字典导出</h2>
      </template>

      <el-form :model="form" label-width="120px">
        <el-form-item label="数据源">
          <el-select v-model="form.configName" placeholder="请选择数据源" @change="loadDatabases">
            <el-option
              v-for="ds in dataSources"
              :key="ds.id"
              :label="ds.name"
              :value="ds.name"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="数据库">
          <el-input v-model="form.database" placeholder="请输入数据库名" />
        </el-form-item>

        <el-form-item label="导出格式">
          <el-radio-group v-model="form.format">
            <el-radio label="json">JSON</el-radio>
            <el-radio label="excel">Excel</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleExport" :loading="exporting">
            <el-icon><Download /></el-icon>
            导出数据字典
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getDataSources } from '../api/config'

const dataSources = ref([])
const exporting = ref(false)

const form = ref({
  configName: '',
  database: '',
  format: 'excel'  // 默认导出Excel
})

onMounted(() => {
  loadDataSources()
})

const loadDataSources = async () => {
  try {
    dataSources.value = await getDataSources()
  } catch (error) {
    ElMessage.error('加载数据源失败')
  }
}

const loadDatabases = () => {
  // 可以在这里根据数据源加载数据库列表
}

const handleExport = async () => {
  if (!form.value.configName || !form.value.database) {
    ElMessage.warning('请填写完整信息')
    return
  }

  exporting.value = true
  try {
    // 使用原生方式下载文件
    const params = new URLSearchParams({
      configName: form.value.configName,
      database: form.value.database,
      format: form.value.format
    })

    const response = await fetch(`/api/export?${params}`, {
      method: 'POST'
    })

    if (!response.ok) {
      throw new Error('导出失败')
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const extension = form.value.format === 'excel' ? 'xlsx' : 'json'
    a.download = `${form.value.database}_schema_${Date.now()}.${extension}`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)

    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败: ' + error.message)
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.export-view {
  max-width: 800px;
}
</style>
