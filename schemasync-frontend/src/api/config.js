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
export function testConnection(configId) {
  return request.post('/config/datasources/test', { configId })
}
