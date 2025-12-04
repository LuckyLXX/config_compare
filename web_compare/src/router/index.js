import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '概览', icon: 'House' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/info',
    name: 'System',
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      {
        path: 'info',
        name: 'SystemInfo',
        component: () => import('@/views/system/info/index.vue'),
        meta: { title: '系统信息', icon: 'Monitor' }
      },
      {
        path: 'servers',
        name: 'ServerManage',
        component: () => import('@/views/system/servers/index.vue'),
        meta: { title: '服务器管理', icon: 'Cpu' }
      }
    ]
  },
  {
    path: '/baseline',
    component: Layout,
    redirect: '/baseline/manage',
    name: 'Baseline',
    meta: { title: '基线管理', icon: 'Document' },
    children: [
      {
        path: 'manage',
        name: 'BaselineManage',
        component: () => import('@/views/baseline/manage/index.vue'),
        meta: { title: '基线版本', icon: 'FolderOpened' }
      },
      {
        path: 'category',
        name: 'BaselineCategory',
        component: () => import('@/views/baseline/category/index.vue'),
        meta: { title: '配置分类', icon: 'Menu' }
      }
    ]
  },
  {
    path: '/collect',
    component: Layout,
    redirect: '/collect/tasks',
    name: 'Collect',
    meta: { title: '采集中心', icon: 'Download' },
    children: [
      {
        path: 'tasks',
        name: 'CollectTasks',
        component: () => import('@/views/collect/tasks/index.vue'),
        meta: { title: '采集任务', icon: 'List' }
      },
      {
        path: 'templates',
        name: 'CollectTemplates',
        component: () => import('@/views/collect/templates/index.vue'),
        meta: { title: '采集模板', icon: 'DocumentCopy' }
      },
      {
        path: 'executions',
        name: 'CollectExecutions',
        component: () => import('@/views/collect/executions/index.vue'),
        meta: { title: '执行历史', icon: 'Clock', hidden: true }
      },
      {
        path: 'results',
        name: 'CollectResults',
        component: () => import('@/views/collect/results/index.vue'),
        meta: { title: '采集结果', icon: 'DataAnalysis', hidden: true }
      }
    ]
  },
  {
    path: '/compare',
    component: Layout,
    redirect: '/compare/tasks',
    name: 'Compare',
    meta: { title: '比对中心', icon: 'Operation' },
    children: [
      {
        path: 'tasks',
        name: 'CompareTasks',
        component: () => import('@/views/compare/tasks/index.vue'),
        meta: { title: '比对任务', icon: 'Switch' }
      },
      {
        path: 'results',
        name: 'CompareResults',
        component: () => import('@/views/compare/results/index.vue'),
        meta: { title: '比对结果', icon: 'DataAnalysis' }
      }
    ]
  },
  {
    path: '/report',
    component: Layout,
    redirect: '/report/dashboard',
    name: 'Report',
    meta: { title: '报告中心', icon: 'TrendCharts' },
    children: [
      {
        path: 'dashboard',
        name: 'ReportDashboard',
        component: () => import('@/views/report/dashboard/index.vue'),
        meta: { title: '总览仪表板', icon: 'DataBoard' }
      },
      {
        path: 'compare-report',
        name: 'CompareReport',
        component: () => import('@/views/report/compare-report/index.vue'),
        meta: { title: '比对报告', icon: 'Document' }
      },
    ]
  },
  {
    path: '/data-process',
    component: Layout,
    redirect: '/data-process/list',
    name: 'DataProcess',
    meta: { title: '数据处理中心', icon: 'MagicStick' },
    children: [
      {
        path: 'list',
        name: 'DataProcessList',
        component: () => import('@/views/data-process/list/index.vue'),
        meta: { title: '任务列表', icon: 'List' }
      },
      {
        path: 'workbench',
        name: 'DataProcessWorkbench',
        component: () => import('@/views/data-process/workbench/index.vue'),
        meta: { title: '处理工作台', icon: 'Cpu', hidden: true }
      }
    ]
  },
  {
    path: '/external-link',
    component: Layout,
    redirect: '/external-link/manage',
    name: 'ExternalLink',
    meta: { title: '外部链接', icon: 'Link' },
    children: [
      {
        path: 'manage',
        name: 'ExternalLinkManage',
        component: () => import('@/views/external-link/manage/index.vue'),
        meta: { title: '链接管理', icon: 'Setting' }
      },
      {
        path: 'view/:id',
        name: 'ExternalLinkView',
        component: () => import('@/views/external-link/iframe/index.vue'),
        meta: { title: '外部页面', icon: 'Monitor', hidden: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router