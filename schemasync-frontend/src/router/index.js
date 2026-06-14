import { createRouter, createWebHistory } from 'vue-router'
import ConfigView from '../views/ConfigView.vue'
import ExportView from '../views/ExportView.vue'
import DiffView from '../views/DiffView.vue'
import GenerateView from '../views/GenerateView.vue'
import SetTableView from '../views/SetTableView.vue'

const routes = [
  {
    path: '/',
    redirect: '/config'
  },
  {
    path: '/config',
    name: 'Config',
    component: ConfigView
  },
  {
    path: '/export',
    name: 'Export',
    component: ExportView
  },
  {
    path: '/diff',
    name: 'Diff',
    component: DiffView
  },
  {
    path: '/generate',
    name: 'Generate',
    component: GenerateView
  },
  {
    path: '/settable',
    name: 'SetTable',
    component: SetTableView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
