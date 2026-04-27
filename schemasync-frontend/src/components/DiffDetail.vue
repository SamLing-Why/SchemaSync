<template>
  <div class="diff-detail">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <h3>差异详情</h3>
          <el-button @click="$emit('back')">返回</el-button>
        </div>
      </template>

      <!-- 差异列表 -->
      <el-collapse v-model="activeNames" v-if="diffData && diffData.changes">
        <el-collapse-item 
          v-for="(changes, table) in groupedChanges" 
          :key="table" 
          :name="table"
        >
          <template #title>
            <div style="display: flex; align-items: center; gap: 10px;">
              <strong>{{ table }}</strong>
              <el-tag size="small" :type="getChangeTypeColor(changes[0].changeType)">
                {{ getChangeTypeLabel(changes[0].changeType) }}
              </el-tag>
            </div>
          </template>

          <el-table :data="changes" size="small">
            <el-table-column prop="changeType" label="变更类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getChangeTypeColor(row.changeType)" size="small">
                  {{ getChangeTypeLabel(row.changeType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="columnName" label="字段名" width="150" />
            <el-table-column prop="severity" label="严重级别" width="100">
              <template #default="{ row }">
                <el-tag :type="row.severity === 'BREAKING' ? 'danger' : 'success'" size="small">
                  {{ row.severity === 'BREAKING' ? '破坏性' : '非破坏性' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="详情">
              <template #default="{ row }">
                <span v-if="row.details">{{ formatDetails(row.details) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>

      <el-empty v-else description="暂无差异详情" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  diffData: {
    type: Object,
    required: true
  }
})

defineEmits(['back'])

const activeNames = ref([])

// 按表分组
const groupedChanges = computed(() => {
  if (!props.diffData || !props.diffData.changes) return {}
  
  const groups = {}
  props.diffData.changes.forEach(change => {
    const table = change.tableName || '未知表'
    if (!groups[table]) {
      groups[table] = []
    }
    groups[table].push(change)
  })
  
  return groups
})

const getChangeTypeLabel = (type) => {
  const labels = {
    'TABLE_ADD': '新增表',
    'TABLE_DROP': '删除表',
    'TABLE_MODIFY': '修改表',
    'COLUMN_ADD': '新增字段',
    'COLUMN_DROP': '删除字段',
    'COLUMN_MODIFY': '修改字段',
    'INDEX_ADD': '新增索引',
    'INDEX_DROP': '删除索引',
    'FOREIGN_KEY_ADD': '新增外键',
    'FOREIGN_KEY_DROP': '删除外键'
  }
  return labels[type] || type
}

const getChangeTypeColor = (type) => {
  if (type.includes('ADD')) return 'success'
  if (type.includes('DROP')) return 'danger'
  return 'warning'
}

const formatDetails = (details) => {
  if (typeof details === 'object') {
    return Object.entries(details)
      .filter(([key]) => key !== 'oldDefinition')
      .map(([key, value]) => `${key}: ${value}`)
      .join(', ')
  }
  return String(details)
}
</script>

<style scoped>
.diff-detail {
  max-width: 1200px;
}
</style>
