<template>
  <div class="dashboard">
    <div class="page-header">
      <h2 class="page-title">系统概览</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon systems">
            <el-icon size="32"><Monitor /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">系统总数</div>
            <div class="stat-value">{{ stats.systemCount }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon servers">
            <el-icon size="32"><Cpu /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">服务器总数</div>
            <div class="stat-value">{{ stats.serverCount }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon tasks">
            <el-icon size="32"><List /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">执行任务</div>
            <div class="stat-value">{{ stats.taskCount }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon reports">
            <el-icon size="32"><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">比对结果</div>
            <div class="stat-value">{{ stats.reportCount }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快速入口 -->
    <div class="app-card">
      <h3>快速入口</h3>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="quick-card" @click="$router.push('/system/info')">
            <div class="quick-content">
              <el-icon size="24" color="#409EFF"><Monitor /></el-icon>
              <span>系统管理</span>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="quick-card" @click="$router.push('/baseline/manage')">
            <div class="quick-content">
              <el-icon size="24" color="#67C23A"><Document /></el-icon>
              <span>基线管理</span>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="quick-card" @click="$router.push('/collect/tasks')">
            <div class="quick-content">
              <el-icon size="24" color="#E6A23C"><Download /></el-icon>
              <span>采集中心</span>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="quick-card" @click="$router.push('/compare/tasks')">
            <div class="quick-content">
              <el-icon size="24" color="#F56C6C"><Operation /></el-icon>
              <span>比对中心</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 最近活动 -->
    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <div class="app-card">
          <h3>最近执行任务</h3>
          <el-table :data="recentTasks" style="width: 100%">
            <el-table-column prop="taskName" label="任务名称" />
            <el-table-column prop="type" label="类型" width="80">
              <template #default="{ row }">
                <el-tag :type="row.type === '采集' ? 'success' : 'warning'" size="small">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executeTime" label="执行时间" width="140" />
          </el-table>
          <div class="table-footer">
            <el-link type="primary" @click="$router.push('/report/execution')">查看更多</el-link>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="app-card">
          <h3>系统状态</h3>
          <div class="system-status">
            <div class="status-item">
              <div class="status-label">后端服务</div>
              <el-tag type="success" size="small">运行中</el-tag>
            </div>
            <div class="status-item">
              <div class="status-label">数据库连接</div>
              <el-tag type="success" size="small">正常</el-tag>
            </div>
            <div class="status-item">
              <div class="status-label">任务调度器</div>
              <el-tag type="success" size="small">运行中</el-tag>
            </div>
            <div class="status-item">
              <div class="status-label">磁盘空间</div>
              <el-tag type="warning" size="small">75%</el-tag>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { dashboardApi } from '@/api/dashboard'

export default {
  name: 'Dashboard',
  setup() {
    const stats = ref({
      systemCount: 0,
      serverCount: 0,
      taskCount: 0,
      reportCount: 0,
      todayExecutions: 0,
      successRate: 0,
      activeSystemCount: 0,
      weeklyTrend: []
    })

    const recentTasks = ref([
      {
        taskName: '生产环境配置采集',
        type: '采集',
        status: '成功',
        executeTime: '2024-01-25 14:30'
      },
      {
        taskName: 'UAT环境比对',
        type: '比对',
        status: '运行中',
        executeTime: '2024-01-25 14:25'
      },
      {
        taskName: '数据库配置采集',
        type: '采集',
        status: '失败',
        executeTime: '2024-01-25 14:20'
      },
      {
        taskName: 'Apollo配置比对',
        type: '比对',
        status: '成功',
        executeTime: '2024-01-25 14:15'
      }
    ])

    const getStatusType = (status) => {
      const statusMap = {
        '成功': 'success',
        '失败': 'danger',
        '运行中': 'warning',
        '等待中': 'info'
      }
      return statusMap[status] || 'info'
    }

    // 获取统计数据
    const fetchStats = async () => {
      try {
        const response = await dashboardApi.getStats()
        stats.value = {
          ...stats.value,
          ...response.data
        }
      } catch (error) {
        console.error('获取统计数据失败:', error)
        ElMessage.error('获取统计数据失败')
      }
    }

    onMounted(() => {
      fetchStats()
    })

    return {
      stats,
      recentTasks,
      getStatusType
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard {
  .stats-cards {
    margin-bottom: 24px;
  }

  .stat-card {
    background: white;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    display: flex;
    align-items: center;
    margin-bottom: 20px;

    .stat-icon {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;

      &.systems {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
      }

      &.servers {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        color: white;
      }

      &.tasks {
        background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
        color: white;
      }

      &.reports {
        background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
        color: white;
      }
    }

    .stat-content {
      flex: 1;

      .stat-title {
        font-size: 14px;
        color: #666;
        margin-bottom: 8px;
      }

      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #333;
      }
    }
  }

  .quick-card {
    cursor: pointer;
    transition: all 0.3s;
    margin-bottom: 16px;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .quick-content {
      display: flex;
      align-items: center;
      justify-content: center;
      flex-direction: column;
      padding: 20px;

      span {
        margin-top: 8px;
        font-weight: 500;
      }
    }
  }

  .table-footer {
    text-align: center;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
  }

  .system-status {
    .status-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #ebeef5;

      &:last-child {
        border-bottom: none;
      }

      .status-label {
        font-weight: 500;
      }
    }
  }
}
</style>