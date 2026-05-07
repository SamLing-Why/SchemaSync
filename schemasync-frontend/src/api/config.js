import request from './request'

// 获取所有数据源配置
export function getDataSources() {
  return request.get('/config/datasources')
}

// 获取单个数据源配置
export function getDataSource(id) {
  return request.get(`/config/datasources/${id}`)
}

// 新增数据源配置
export function addDataSource(data) {
  return request.post('/config/datasources', data)
}

// 更新数据源配置
export function updateDataSource(id, data) {
  return request.put(`/config/datasources/${id}`, data)
}

// 删除数据源配置
export function deleteDataSource(id) {
  return request.delete(`/config/datasources/${id}`)
}

// 测试数据源连接
// 支持两种模式:
// 1. 测试已保存的配置: testConnection(configId)
// 2. 测试临时配置: testConnection(configObject)
export function testConnection(config) {
  // 如果传入的是字符串(配置ID),则测试已保存的配置
  if (typeof config === 'string') {
    return request.post('/config/datasources/test', { configId: config })
  }
  // 否则测试临时配置(新增/编辑时)
  return request.post('/config/datasources/test', config)
}

// 获取数据库列表(根据配置名称)
export function getDatabases(configName) {
  return request.get('/export/databases', { params: { configName } })
}
