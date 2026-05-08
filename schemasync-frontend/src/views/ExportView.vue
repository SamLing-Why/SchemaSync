<template>
  <div class="export-view">
    <el-card>
      <template #header>
        <h2>数据字典导出</h2>
      </template>

      <el-form :model="form" label-width="120px">
        <el-form-item label="数据源">
          <el-select v-model="form.configName" placeholder="请选择数据源" @change="onDataSourceChange">
            <el-option
              v-for="ds in dataSources"
              :key="ds.id"
              :label="ds.name"
              :value="ds.name"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="数据库">
          <el-select 
            v-model="form.database" 
            placeholder="请选择或输入数据库名" 
            :loading="loadingDatabases"
            filterable
            allow-create
            default-first-option
            @focus="onDatabaseFocus"
            @change="onDatabaseChange"
          >
            <el-option
              v-for="db in databaseList"
              :key="db"
              :label="db"
              :value="db"
            />
          </el-select>
          <div v-if="databaseList.length === 0 && !loadingDatabases" style="color: #909399; font-size: 12px; margin-top: 5px;">
            选择数据源后将自动加载数据库列表
          </div>
        </el-form-item>

        <el-form-item v-if="showSchemaSelect" label="SCHEMA">
          <el-select 
            v-model="form.schema" 
            placeholder="请选择SCHEMA" 
            :loading="loadingSchemas"
            filterable
          >
            <el-option
              v-for="schema in schemaList"
              :key="schema"
              :label="schema"
              :value="schema"
            />
          </el-select>
          <div v-if="schemaList.length === 0 && !loadingSchemas" style="color: #909399; font-size: 12px; margin-top: 5px;">
            选择数据库后将自动加载SCHEMA列表
          </div>
        </el-form-item>

        <el-form-item label="导出格式">
          <el-radio-group v-model="form.format">
            <el-radio label="excel">Excel</el-radio>
            <el-radio label="json">JSON</el-radio>
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
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getDataSources, getDatabases, getSchemas } from '../api/config'

const dataSources = ref([])
const databaseList = ref([])
const schemaList = ref([])
const loadingDatabases = ref(false)
const loadingSchemas = ref(false)
const exporting = ref(false)
const databasesLoaded = ref(false) // 标记是否已加载过数据库列表
const currentDataSource = ref(null) // 当前选中的数据源

const form = ref({
  configName: '',
  database: '',
  schema: '',  // SCHEMA字段
  format: 'excel'  // 默认导出Excel
})

// 计算是否显示SCHEMA选择框
const showSchemaSelect = computed(() => {
  return currentDataSource.value && 
         (currentDataSource.value.type === 'GAUSSDB' || 
          currentDataSource.value.type === 'OPENGAUSS' ||
          currentDataSource.value.type === 'POSTGRESQL')
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

// 数据源改变时加载数据库列表
const onDataSourceChange = async () => {
  // 清空数据库和SCHEMA选择
  form.value.database = ''
  form.value.schema = ''
  databaseList.value = []
  schemaList.value = []
  databasesLoaded.value = false
  
  // 记录当前数据源
  currentDataSource.value = dataSources.value.find(ds => ds.name === form.value.configName)
  
  // 自动加载数据库列表
  await loadDatabases()
}

// 加载数据库列表
const loadDatabases = async () => {
  if (!form.value.configName) {
    ElMessage.warning('请先选择数据源')
    return
  }
  
  loadingDatabases.value = true
  try {
    const databases = await getDatabases(form.value.configName)
    databaseList.value = databases
    databasesLoaded.value = true
    ElMessage.success(`加载了 ${databases.length} 个数据库`)
  } catch (error) {
    ElMessage.error('加载数据库列表失败: ' + (error.message || '未知错误'))
    databaseList.value = []
  } finally {
    loadingDatabases.value = false
  }
}

// 数据库下拉框获得焦点时加载
const onDatabaseFocus = () => {
  if (!databasesLoaded.value && form.value.configName) {
    loadDatabases()
  }
}

// 数据库改变时加载SCHEMA列表
const onDatabaseChange = async () => {
  // 清空SCHEMA选择
  form.value.schema = ''
  schemaList.value = []
  
  // 如果当前数据源支持SCHEMA，自动加载
  if (showSchemaSelect.value && form.value.database) {
    await loadSchemas()
  }
}

// 加载SCHEMA列表
const loadSchemas = async () => {
  if (!form.value.configName || !form.value.database) {
    return
  }
  
  loadingSchemas.value = true
  try {
    const schemas = await getSchemas(form.value.configName, form.value.database)
    schemaList.value = schemas
    if (schemas.length > 0) {
      ElMessage.success(`加载了 ${schemas.length} 个SCHEMA`)
    }
  } catch (error) {
    ElMessage.warning('加载SCHEMA列表失败: ' + (error.message || '未知错误'))
    schemaList.value = []
  } finally {
    loadingSchemas.value = false
  }
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
    
    // 如果有SCHEMA，添加到参数中
    if (form.value.schema) {
      params.append('schema', form.value.schema)
    }

    const response = await fetch(`/api/export?${params}`, {
      method: 'POST'
    })

    if (!response.ok) {
      // 尝试读取错误信息
      let errorMsg = '导出失败'
      try {
        const errorData = await response.json()
        if (errorData.message) {
          errorMsg = errorData.message
        } else if (errorData.error) {
          errorMsg = errorData.error
        }
      } catch (e) {
        // 如果无法解析JSON，使用HTTP状态码
        errorMsg = `导出失败 (HTTP ${response.status})`
      }
      throw new Error(errorMsg)
    }

    const blob = await response.blob()
    
    // 检查是否是错误响应（可能返回的是JSON而不是文件）
    if (blob.type && blob.type.includes('application/json')) {
      const errorText = await blob.text()
      try {
        const errorData = JSON.parse(errorText)
        throw new Error(errorData.message || errorData.error || '导出失败')
      } catch (e) {
        throw new Error('导出失败')
      }
    }
    
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
    ElMessage.error('导出失败: ' + (error.message || '未知错误'))
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
