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
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="testConn(scope.row)">测试连接</el-button>
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
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="数据库类型">
          <el-select v-model="form.type" placeholder="请选择数据库类型">
            <el-option label="MySQL" value="mysql" />
            <el-option label="Oracle" value="oracle" />
            <el-option label="OceanBase" value="oceanbase" />
            <el-option label="TDSQL" value="tdsql" />
            <el-option label="GaussDB" value="gaussdb" />
            <el-option label="GoldenDB" value="goldendb" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机">
          <el-input v-model="form.host" placeholder="localhost" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="数据库名">
          <el-input v-model="form.database" placeholder="请输入数据库名" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getDataSources, addDataSource, deleteDataSource, testConnection } from '../api/config'

const dataSources = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增数据源')

const form = ref({
  name: '',
  type: 'mysql',
  host: 'localhost',
  port: 3306,
  database: '',
  username: '',
  password: ''
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
  form.value = {
    name: '',
    type: 'mysql',
    host: 'localhost',
    port: 3306,
    database: '',
    username: '',
    password: ''
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    await addDataSource(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadDataSources()
  } catch (error) {
    ElMessage.error('保存失败')
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
