<template>
  <div class="diff-analysis">
    <div class="page-header">
      <h2 class="page-title">差异分析报告</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleGenerateReport">
          <el-icon><Document /></el-icon>
          生成分析报告
        </el-button>
        <el-button type="success" @click="handleExportReport">
          <el-icon><Download /></el-icon>
          导出报告
        </el-button>
        <el-button type="info" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 查询条件 -->
    <div class="app-card">
      <el-form :model="searchForm" label-width="100px" :inline="true">
        <el-form-item label="时间范围:">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 350px"
          />
        </el-form-item>
        <el-form-item label="系统:">
          <el-select v-model="searchForm.systemId" placeholder="请选择系统" clearable style="width: 200px">
            <el-option
              v-for="item in systemOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="环境:">
          <el-select v-model="searchForm.environment" placeholder="请选择环境" clearable style="width: 150px">
            <el-option label="开发环境" value="dev" />
            <el-option label="测试环境" value="test" />
            <el-option label="预生产" value="pre" />
            <el-option label="生产环境" value="prod" />
          </el-select>
        </el-form-item>
        <el-form-item label="差异类型:">
          <el-select v-model="searchForm.diffType" placeholder="请选择差异类型" clearable style="width: 150px">
            <el-option label="新增" value="added" />
            <el-option label="删除" value="deleted" />
            <el-option label="修改" value="modified" />
            <el-option label="不一致" value="inconsistent" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计概览 -->
    <div class="app-card">
      <h3 class="card-title">差异分析概览</h3>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card stat-total">
            <div class="stat-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.totalDiffs }}</div>
              <div class="stat-label">总差异数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-added">
            <div class="stat-icon">
              <el-icon><Plus /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.addedCount }}</div>
              <div class="stat-label">新增配置</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-modified">
            <div class="stat-icon">
              <el-icon><Edit /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.modifiedCount }}</div>
              <div class="stat-label">修改配置</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-deleted">
            <div class="stat-icon">
              <el-icon><Delete /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.deletedCount }}</div>
              <div class="stat-label">删除配置</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 差异趋势分析 -->
    <div class="app-card">
      <h3 class="card-title">差异趋势分析</h3>
      <div class="chart-container">
        <div ref="trendChart" style="width: 100%; height: 400px;"></div>
      </div>
    </div>

    <!-- 差异分类统计 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">差异类型分布</h3>
          <div class="chart-container">
            <div ref="diffTypeChart" style="width: 100%; height: 300px;"></div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">系统差异分布</h3>
          <div class="chart-container">
            <div ref="systemChart" style="width: 100%; height: 300px;"></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 差异详情列表 -->
    <div class="app-card">
      <h3 class="card-title">差异详情</h3>
      <el-table
        :data="diffList"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="compareTaskName" label="比对任务" width="200" />
        <el-table-column prop="systemName" label="系统" width="120" />
        <el-table-column prop="environment" label="环境" width="100">
          <template #default="{ row }">
            <el-tag :type="getEnvironmentType(row.environment)">{{ getEnvironmentText(row.environment) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="configPath" label="配置路径" width="300" show-overflow-tooltip />
        <el-table-column prop="diffType" label="差异类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getDiffTypeTag(row.diffType)">{{ getDiffTypeText(row.diffType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diffCount" label="差异项数" width="100" />
        <el-table-column prop="severity" label="严重程度" width="120">
          <template #default="{ row }">
            <el-tag :type="getSeverityType(row.severity)">{{ getSeverityText(row.severity) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发现时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
            <el-button link type="success" @click="handleAnalyze(row)">
              <el-icon><DataAnalysis /></el-icon>
              分析
            </el-button>
            <el-button link type="warning" @click="handleIgnore(row)">
              <el-icon><CloseBold /></el-icon>
              忽略
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Download,
  Refresh,
  Search,
  Plus,
  Edit,
  Delete,
  View,
  DataAnalysis,
  CloseBold
} from '@element-plus/icons-vue'
import { diffAnalysisApi } from '@/api/report'
import { systemApi } from '@/api/system'
import * as echarts from 'echarts'

export default {
  name: 'DiffAnalysis',
  components: {
    Document,
    Download,
    Refresh,
    Search,
    Plus,
    Edit,
    Delete,
    View,
    DataAnalysis,
    CloseBold
  },
  setup() {
    // 响应式数据
    const loading = ref(false)
    const systemOptions = ref([])
    
    // 搜索表单
    const searchForm = reactive({
      dateRange: [],
      systemId: '',
      environment: '',
      diffType: ''
    })

    // 概览数据
    const overview = reactive({
      totalDiffs: 0,
      addedCount: 0,
      modifiedCount: 0,
      deletedCount: 0
    })

    // 差异列表
    const diffList = ref([])
    
    // 分页
    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    // 图表
    const trendChart = ref(null)
    const diffTypeChart = ref(null)
    const systemChart = ref(null)
    let trendChartInstance = null
    let diffTypeChartInstance = null
    let systemChartInstance = null

    // 获取系统列表
    const loadSystemOptions = async () => {
      try {
        const response = await systemApi.getAllSystemList()
        systemOptions.value = response.data.records || []
      } catch (error) {
        console.error('获取系统列表失败:', error)
      }
    }

    // 加载差异分析数据
    const loadDiffAnalysisData = async () => {
      loading.value = true
      try {
        const params = {
          ...searchForm,
          page: pagination.page,
          size: pagination.size,
          startTime: searchForm.dateRange?.[0],
          endTime: searchForm.dateRange?.[1]
        }
        delete params.dateRange

        const response = await diffAnalysisApi.getDiffAnalysisReports(params)
        if (response.data) {
          diffList.value = response.data.records || []
          pagination.total = response.data.total || 0
          
          // 更新概览数据
          Object.assign(overview, response.data.overview || {})
        }
      } catch (error) {
        console.error('获取差异分析数据失败:', error)
        ElMessage.error('获取数据失败')
      } finally {
        loading.value = false
      }
    }

    // 加载趋势数据
    const loadTrendData = async () => {
      try {
        const params = {
          startTime: searchForm.dateRange?.[0],
          endTime: searchForm.dateRange?.[1]
        }
        const response = await diffAnalysisApi.getDiffTrends(params)
        if (response.data && trendChartInstance) {
          const option = {
            title: {
              text: '差异趋势分析',
              left: 'center'
            },
            tooltip: {
              trigger: 'axis'
            },
            legend: {
              top: 30,
              data: ['新增', '修改', '删除']
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: response.data.dates || []
            },
            yAxis: {
              type: 'value'
            },
            series: [
              {
                name: '新增',
                type: 'line',
                data: response.data.addedData || [],
                itemStyle: { color: '#67C23A' }
              },
              {
                name: '修改',
                type: 'line',
                data: response.data.modifiedData || [],
                itemStyle: { color: '#E6A23C' }
              },
              {
                name: '删除',
                type: 'line',
                data: response.data.deletedData || [],
                itemStyle: { color: '#F56C6C' }
              }
            ]
          }
          trendChartInstance.setOption(option)
        }
      } catch (error) {
        console.error('获取趋势数据失败:', error)
      }
    }

    // 初始化图表
    const initCharts = () => {
      nextTick(() => {
        // 趋势图表
        if (trendChart.value) {
          trendChartInstance = echarts.init(trendChart.value)
        }
        
        // 差异类型分布
        if (diffTypeChart.value) {
          diffTypeChartInstance = echarts.init(diffTypeChart.value)
          const diffTypeOption = {
            title: {
              text: '差异类型分布',
              left: 'center'
            },
            tooltip: {
              trigger: 'item'
            },
            series: [
              {
                type: 'pie',
                radius: '60%',
                data: [
                  { value: overview.addedCount, name: '新增', itemStyle: { color: '#67C23A' } },
                  { value: overview.modifiedCount, name: '修改', itemStyle: { color: '#E6A23C' } },
                  { value: overview.deletedCount, name: '删除', itemStyle: { color: '#F56C6C' } }
                ],
                emphasis: {
                  itemStyle: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                  }
                }
              }
            ]
          }
          diffTypeChartInstance.setOption(diffTypeOption)
        }
        
        // 系统分布
        if (systemChart.value) {
          systemChartInstance = echarts.init(systemChart.value)
          const systemOption = {
            title: {
              text: '系统差异分布',
              left: 'center'
            },
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              }
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: ['系统A', '系统B', '系统C', '系统D', '系统E']
            },
            yAxis: {
              type: 'value'
            },
            series: [
              {
                type: 'bar',
                data: [120, 200, 150, 80, 70],
                itemStyle: {
                  color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: '#83bff6' },
                    { offset: 0.5, color: '#188df0' },
                    { offset: 1, color: '#188df0' }
                  ])
                }
              }
            ]
          }
          systemChartInstance.setOption(systemOption)
        }
      })
    }

    // 事件处理
    const handleSearch = () => {
      pagination.page = 1
      loadDiffAnalysisData()
      loadTrendData()
    }

    const handleReset = () => {
      Object.assign(searchForm, {
        dateRange: [],
        systemId: '',
        environment: '',
        diffType: ''
      })
      handleSearch()
    }

    const handleRefresh = () => {
      loadDiffAnalysisData()
      loadTrendData()
    }

    const handleSizeChange = (size) => {
      pagination.size = size
      loadDiffAnalysisData()
    }

    const handleCurrentChange = (page) => {
      pagination.page = page
      loadDiffAnalysisData()
    }

    const handleGenerateReport = async () => {
      try {
        const params = {
          ...searchForm,
          startTime: searchForm.dateRange?.[0],
          endTime: searchForm.dateRange?.[1]
        }
        delete params.dateRange
        
        await diffAnalysisApi.generateDiffAnalysisReport(params)
        ElMessage.success('差异分析报告生成成功')
        handleRefresh()
      } catch (error) {
        console.error('生成报告失败:', error)
        ElMessage.error('生成报告失败')
      }
    }

    const handleExportReport = () => {
      ElMessage.info('导出功能开发中...')
    }

    const handleViewDetail = (row) => {
      ElMessage.info('查看详情功能开发中...')
    }

    const handleAnalyze = (row) => {
      ElMessage.info('分析功能开发中...')
    }

    const handleIgnore = async (row) => {
      try {
        await ElMessageBox.confirm('确定要忽略这个差异吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        ElMessage.success('已忽略该差异')
        handleRefresh()
      } catch {
        // 用户取消
      }
    }

    // 辅助函数
    const getEnvironmentType = (env) => {
      const types = { dev: 'info', test: 'warning', pre: 'danger', prod: 'success' }
      return types[env] || 'info'
    }

    const getEnvironmentText = (env) => {
      const texts = { dev: '开发环境', test: '测试环境', pre: '预生产', prod: '生产环境' }
      return texts[env] || env
    }

    const getDiffTypeTag = (type) => {
      const tags = { added: 'success', modified: 'warning', deleted: 'danger', inconsistent: 'info' }
      return tags[type] || 'info'
    }

    const getDiffTypeText = (type) => {
      const texts = { added: '新增', modified: '修改', deleted: '删除', inconsistent: '不一致' }
      return texts[type] || type
    }

    const getSeverityType = (severity) => {
      const types = { low: 'info', medium: 'warning', high: 'danger', critical: 'danger' }
      return types[severity] || 'info'
    }

    const getSeverityText = (severity) => {
      const texts = { low: '低', medium: '中', high: '高', critical: '紧急' }
      return texts[severity] || severity
    }

    // 初始化
    onMounted(() => {
      loadSystemOptions()
      loadDiffAnalysisData()
      initCharts()
      loadTrendData()
    })

    return {
      loading,
      systemOptions,
      searchForm,
      overview,
      diffList,
      pagination,
      trendChart,
      diffTypeChart,
      systemChart,
      handleSearch,
      handleReset,
      handleRefresh,
      handleSizeChange,
      handleCurrentChange,
      handleGenerateReport,
      handleExportReport,
      handleViewDetail,
      handleAnalyze,
      handleIgnore,
      getEnvironmentType,
      getEnvironmentText,
      getDiffTypeTag,
      getDiffTypeText,
      getSeverityType,
      getSeverityText
    }
  }
}
</script>

<style scoped>
.diff-analysis {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 500;
  color: #303133;
}

.page-actions {
  display: flex;
  gap: 10px;
}

.app-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
}

.card-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  font-size: 40px;
  margin-right: 15px;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-added {
  background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
  color: white;
}

.stat-modified {
  background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%);
  color: white;
}

.stat-deleted {
  background: linear-gradient(135deg, #F56C6C 0%, #f78989 100%);
  color: white;
}

.chart-container {
  margin-top: 20px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>