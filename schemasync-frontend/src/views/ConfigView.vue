<template>
  <div class="config-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>数据源配置</h2>
          <el-button type="primary" @click="showAddDialog">
            <el-icon><Plus /></el-icon>
            新增数据源
          </el-button>
        </div>
      </template>

      <el-table :data="dataSources" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="host" label="主机" width="150" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column prop="database" label="数据库" width="150" />
        <el-table-column label="操作" width="250">
          <template #default="scope">
            <el-button size="small" @click="testConn(scope.row)">测试连接</el-button>
            <el-button size="small" type="primary" @click="showEditDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form 
        :model="form" 
        :rules="formRules" 
        ref="formRef"
        label-width="100px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="数据库类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择数据库类型">
            <el-option label="MySQL" value="mysql" />
            <el-option label="Oracle" value="oracle" />
            <el-option label="OceanBase" value="oceanbase" />
            <el-option label="TDSQL" value="tdsql" />
            <el-option label="GaussDB" value="gaussdb" />
            <el-option label="GoldenDB" value="goldendb" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机" prop="host">
          <el-input v-model="form.host" placeholder="localhost" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="数据库名" prop="database">
          <el-input v-model="form.database" placeholder="请输入数据库名" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="可选" />
        </el-form-item>
        
        <!-- 高级配置(可折叠) -->
        <el-divider>高级配置</el-divider>
        <el-form-item label="JDBC URL">
          <el-input 
            v-model="form.jdbcUrl" 
            type="textarea" 
            :rows="3"
            placeholder="自定义JDBC URL(可选,留空则自动生成)"
          />
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            示例: jdbc:mysql://host:3306/db?useUnicode=true&characterEncoding=utf8
          </div>
        </el-form-item>
        <el-form-item label="连接池配置">
          <el-input 
            v-model="form.poolConfig" 
            type="textarea" 
            :rows="2"
            placeholder='JSON格式,如: {"maximumPoolSize":20,"minimumIdle":5}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button 
          type="warning" 
          @click="testConnectionInForm"
          :loading="testingConnection"
          :disabled="!isFormValid"
        >
          <el-icon><Connection /></el-icon>
          测试连接
        </el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Connection } from '@element-plus/icons-vue'
import { getDataSources, addDataSource, updateDataSource, deleteDataSource, testConnection } from '../api/config'

const dataSources = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增数据源')
const formRef = ref(null)
const testingConnection = ref(false)
const isEditMode = ref(false) // 是否为编辑模式

const form = ref({
  name: '',
  type: 'mysql',
  host: 'localhost',
  port: 3306,
  database: '',
  username: '',
  password: '',
  jdbcUrl: '',
  poolConfig: ''
})

// 表单校验规则
const formRules = {
  name: [
    { required: true, message: '请输入数据源名称', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择数据库类型', trigger: 'change' }
  ],
  host: [
    { required: true, message: '请输入主机地址', trigger: 'blur' }
  ],
  port: [
    { required: true, message: '请输入端口', trigger: 'blur' }
  ],
  database: [
    { required: true, message: '请输入数据库名称', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ]
}

// 检查表单是否填写了必要字段(用于控制测试连接按钮)
const isFormValid = computed(() => {
  return form.value.type && form.value.host && form.value.port && 
         form.value.database && form.value.username
})

onMounted(() => {
  loadDataSources()
})

const loadDataSources = async () => {
  loading.value = true
  try {
    dataSources.value = await getDataSources()
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEditMode.value = false
  dialogTitle.value = '新增数据源'
  
  // 先打开对话框
  dialogVisible.value = true
  
  // 等待DOM更新后再重置表单
  setTimeout(() => {
    form.value = {
      id: null,
      name: '',
      type: 'mysql',
      host: 'localhost',
      port: 3306,
      database: '',
      username: '',
      password: '',
      jdbcUrl: '',
      poolConfig: ''
    }
    
    // 重置表单验证
    if (formRef.value) {
      formRef.value.resetFields()
    }
  }, 100)
}

const showEditDialog = (row) => {
  isEditMode.value = true
  dialogTitle.value = '编辑数据源'
  
  // 先打开对话框
  dialogVisible.value = true
  
  // 等待DOM更新后再设置表单值
  setTimeout(() => {
    form.value = {
      id: row.id,
      name: row.name,
      type: row.type,
      host: row.host,
      port: row.port,
      database: row.database,
      username: row.username,
      password: '', // 密码不回填
      jdbcUrl: row.jdbcUrl || '',
      poolConfig: row.poolConfig || ''
    }
    
    // 清除验证状态，但不重置字段值
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  }, 100)
}

// 在表单中测试连接
const testConnectionInForm = async () => {
  // 先校验表单
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch (error) {
    ElMessage.warning('请填写必填项')
    return
  }
  
  // 测试连接
  testingConnection.value = true
  try {
    const result = await testConnection(form.value)
    if (result.success) {
      let message = '✓ 连接成功'
      if (result.databaseVersion) {
        message += ` - ${result.databaseVersion}`
      }
      ElMessage.success(message)
    } else {
      ElMessage.error(result.message || '连接失败')
    }
  } catch (error) {
    ElMessage.error('连接测试失败: ' + error.message)
  } finally {
    testingConnection.value = false
  }
}

// 重置表单(关闭对话框时)
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const handleSave = async () => {
  try {
    // 表单验证
    if (formRef.value) {
      await formRef.value.validate()
    }
    
    if (isEditMode.value) {
      // 编辑模式: 调用更新API
      await updateDataSource(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      // 新增模式: 调用新增API
      await addDataSource(form.value)
      ElMessage.success('新增成功')
    }
    
    dialogVisible.value = false
    loadDataSources()
  } catch (error) {
    if (error !== false) { // 排除表单验证失败
      ElMessage.error(isEditMode.value ? '更新失败' : '新增失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除此数据源吗?', '提示', {
      type: 'warning'
    })
    await deleteDataSource(row.id)
    ElMessage.success('删除成功')
    loadDataSources()
  } catch (error) {
    // 用户取消
  }
}

const testConn = async (row) => {
  try {
    const result = await testConnection(row.id)
    if (result.success) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error('连接失败')
    }
  } catch (error) {
    ElMessage.error('连接测试失败')
  }
}
</script>

<style scoped>
.config-view {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
}
</style>
