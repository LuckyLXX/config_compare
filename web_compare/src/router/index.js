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
        meta: { title: '仪表板', icon: 'DataBoard' }
      },
      {
        path: 'schedule',
        name: 'TaskSchedule',
        component: () => import('@/views/report/schedule/index.vue'),
        meta: { title: '任务调度', icon: 'Timer' }
      },
      {
        path: 'execution',
        name: 'TaskExecution',
        component: () => import('@/views/report/execution/index.vue'),
        meta: { title: '执行报告', icon: 'Document' }
      },
      {
        path: 'diff-analysis',
        name: 'DiffAnalysis',
        component: () => import('@/views/report/diff-analysis/index.vue'),
        meta: { title: '差异分析', icon: 'TrendCharts' }
      },
      {
        path: 'system-health',
        name: 'SystemHealth',
        component: () => import('@/views/report/system-health/index.vue'),
        meta: { title: '系统健康', icon: 'CircleCheck' }
      },
      {
        path: 'collect-stats',
        name: 'CollectStats',
        component: () => import('@/views/report/collect-stats/index.vue'),
        meta: { title: '采集统计', icon: 'PieChart' }
      },
      {
        path: 'compare-stats',
        name: 'CompareStats',
        component: () => import('@/views/report/compare-stats/index.vue'),
        meta: { title: '比对统计', icon: 'Histogram' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router