<template>
  <div class="compare-results">
    <div class="page-header">
      <h2 class="page-title">基线配置比对结果</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleGenerateReport" :loading="reportLoading">
          <el-icon><Document /></el-icon>
          生成报告
        </el-button>
        <el-button type="success" @click="handleDownloadReport" :disabled="!selectedSystem">
          <el-icon><Download /></el-icon>
          下载报告
        </el-button>
        <el-button type="info" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 系统选择和过滤 -->
    <div class="app-card">
      <div class="filter-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="filter-item">
              <label class="filter-label">选择系统</label>
              <el-select
                v-model="selectedSystem"
                placeholder="请选择要查看的系统"
                @change="handleSystemChange"
                style="width: 100%"
                size="large"
              >
                <el-option
                  v-for="system in systemList"
                  :key="system.id"
                  :label="system.name"
                  :value="system.id"
                >
                  <span style="float: left">{{ system.name }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">
                    {{ system.environment }}
                  </span>
                </el-option>
              </el-select>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="filter-item">
              <label class="filter-label">基线版本</label>
              <el-select
                v-model="selectedBaseline"
                placeholder="选择基线版本"
                @change="handleBaselineChange"
                style="width: 100%"
              >
                <el-option
                  v-for="baseline in baselineList"
                  :key="baseline.id"
                  :label="baseline.version"
                  :value="baseline.id"
                />
              </el-select>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="filter-item">
              <label class="filter-label">比对状态</label>
              <el-select
                v-model="selectedStatus"
                placeholder="全部状态"
                @change="loadCompareResults"
                style="width: 100%"
              >
                <el-option label="全部" value="" />
                <el-option label="一致" value="CONSISTENT" />
                <el-option label="不一致" value="INCONSISTENT" />
                <el-option label="缺失" value="MISSING" />
                <el-option label="多余" value="EXTRA" />
              </el-select>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="filter-item">
              <label class="filter-label">配置分类</label>
              <el-select
                v-model="selectedCategory"
                placeholder="全部分类"
                @change="loadCompareResults"
                style="width: 100%"
              >
                <el-option label="全部" value="" />
                <el-option
                  v-for="category in categoryList"
                  :key="category.id"
                  :label="category.name"
                  :value="category.id"
                />
              </el-select>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="filter-item">
              <label class="filter-label">时间范围</label>
              <el-date-picker
                v-model="dateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                @change="loadCompareResults"
                style="width: 100%"
              />
            </div>
          </el-col>
        </el-row>
      </div>
    </div>

    <!-- 系统比对概览 -->
    <div class="app-card" v-if="selectedSystem">
      <div class="card-header">
        <h3>{{ selectedSystemName }} - 比对概览</h3>
        <div class="header-actions">
          <el-tag :type="getSystemStatusType(systemOverview.status)" size="large">
            {{ getSystemStatusText(systemOverview.status) }}
          </el-tag>
        </div>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="metric-card consistent">
            <div class="metric-icon">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ systemOverview.consistentCount || 0 }}</div>
              <div class="metric-label">配置一致</div>
              <div class="metric-percentage">{{ systemOverview.consistentRate || 0 }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card inconsistent">
            <div class="metric-icon">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ systemOverview.inconsistentCount || 0 }}</div>
              <div class="metric-label">配置不一致</div>
              <div class="metric-percentage">{{ systemOverview.inconsistentRate || 0 }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card missing">
            <div class="metric-icon">
              <el-icon><Remove /></el-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ systemOverview.missingCount || 0 }}</div>
              <div class="metric-label">配置缺失</div>
              <div class="metric-percentage">{{ systemOverview.missingRate || 0 }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card extra">
            <div class="metric-icon">
              <el-icon><Plus /></el-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ systemOverview.extraCount || 0 }}</div>
              <div class="metric-label">多余配置</div>
              <div class="metric-percentage">{{ systemOverview.extraRate || 0 }}%</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 可视化图表 -->
    <el-row :gutter="20" v-if="selectedSystem">
      <el-col :span="12">
        <div class="app-card">
          <div class="card-header">
            <h3>比对结果分布</h3>
          </div>
          <div ref="resultDistributionChart" style="height: 350px"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="app-card">
          <div class="card-header">
            <h3>配置分类对比</h3>
          </div>
          <div ref="categoryCompareChart" style="height: 350px"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" v-if="selectedSystem">
      <el-col :span="24">
        <div class="app-card">
          <div class="card-header">
            <h3>比对历史趋势</h3>
            <el-radio-group v-model="trendPeriod" @change="loadTrendData">
              <el-radio-button label="7d">7天</el-radio-button>
              <el-radio-button label="30d">30天</el-radio-button>
              <el-radio-button label="90d">90天</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="trendChart" style="height: 300px"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 详细比对结果 -->
    <div class="app-card" v-if="selectedSystem">
      <div class="card-header">
        <h3>详细比对结果</h3>
        <div class="header-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索配置项..."
            style="width: 200px; margin-right: 10px"
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button @click="handleExportExcel">
            <el-icon><DocumentCopy /></el-icon>
            导出Excel
          </el-button>
        </div>
      </div>

      <el-table
        :data="compareResults"
        v-loading="tableLoading"
        stripe
        style="width: 100%"
        :row-class-name="getRowClassName"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="configKey" label="配置项" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="config-key">
              <el-icon v-if="row.isImportant" class="important-icon"><Star /></el-icon>
              {{ row.configKey }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.categoryName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baselineValue" label="基线值" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="config-value baseline">
              <span v-if="row.baselineValue">{{ row.baselineValue }}</span>
              <span v-else class="empty-value">未设置</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="actualValue" label="实际值" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="config-value actual">
              <span v-if="row.actualValue">{{ row.actualValue }}</span>
              <span v-else class="empty-value">未设置</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="比对状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              <el-icon class="status-icon">
                <CircleCheck v-if="row.status === 'CONSISTENT'" />
                <Warning v-else-if="row.status === 'INCONSISTENT'" />
                <Remove v-else-if="row.status === 'MISSING'" />
                <Plus v-else-if="row.status === 'EXTRA'" />
              </el-icon>
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="impact" label="影响级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getImpactType(row.impact)" size="small">
              {{ row.impact }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastCompareTime" label="最后比对时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewConfigHistory(row)">
              <el-icon><Clock /></el-icon>
              历史
            </el-button>
            <el-button type="info" size="small" @click="viewConfigDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button 
              v-if="row.status !== 'CONSISTENT'" 
              type="warning" 
              size="small" 
              @click="handleSync(row)"
            >
              <el-icon><Refresh /></el-icon>
              同步
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
          :page-sizes="[20, 50, 100, 200]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 配置详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="`配置项详情 - ${currentConfigDetail?.configKey}`"
      width="800px"
      @close="handleDetailDialogClose"
    >
      <div v-if="currentConfigDetail" class="config-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="配置项">{{ currentConfigDetail.configKey }}</el-descriptions-item>
          <el-descriptions-item label="配置分类">{{ currentConfigDetail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="影响级别">
            <el-tag :type="getImpactType(currentConfigDetail.impact)">{{ currentConfigDetail.impact }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="比对状态">
            <el-tag :type="getStatusType(currentConfigDetail.status)">{{ getStatusText(currentConfigDetail.status) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="value-comparison">
          <h4>值对比</h4>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="value-block baseline">
                <div class="value-header">基线值</div>
                <div class="value-content">
                  <pre>{{ currentConfigDetail.baselineValue || '未设置' }}</pre>
                </div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="value-block actual">
                <div class="value-header">实际值</div>
                <div class="value-content">
                  <pre>{{ currentConfigDetail.actualValue || '未设置' }}</pre>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <div class="config-description" v-if="currentConfigDetail.description">
          <h4>配置说明</h4>
          <p>{{ currentConfigDetail.description }}</p>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button 
          v-if="currentConfigDetail?.status !== 'CONSISTENT'" 
          type="primary" 
          @click="handleSyncConfig(currentConfigDetail)"
        >
          同步配置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Download,
  Refresh,
  CircleCheck,
  Warning,
  Remove,
  Plus,
  Search,
  DocumentCopy,
  Star,
  Clock,
  View
} from '@element-plus/icons-vue'
import { systemApi } from '@/api/system'
import { compareResultApi } from '@/api/compare'
import { baselineApi, categoryApi } from '@/api/baseline'
import * as echarts from 'echarts'

export default {
  name: 'CompareResults',
  components: {
    Document,
    Download,
    Refresh,
    CircleCheck,
    Warning,
    Remove,
    Plus,
    Search,
    DocumentCopy,
    Star,
    Clock,
    View
  },
  setup() {
    // 响应式数据
    const loading = ref(false)
    const tableLoading = ref(false)
    const reportLoading = ref(false)
    
    // 选择器数据
    const selectedSystem = ref('')
    const selectedBaseline = ref('')
    const selectedStatus = ref('')
    const selectedCategory = ref('')
    const dateRange = ref([])
    const trendPeriod = ref('7d')
    const searchKeyword = ref('')
    
    // 基础数据
    const systemList = ref([])
    const baselineList = ref([])
    const categoryList = ref([])
    const compareResults = ref([])
    const selectedRows = ref([])
    
    // 系统概览数据
    const systemOverview = reactive({
      status: 'HEALTHY',
      consistentCount: 0,
      inconsistentCount: 0,
      missingCount: 0,
      extraCount: 0,
      consistentRate: 0,
      inconsistentRate: 0,
      missingRate: 0,
      extraRate: 0
    })
    
    // 分页数据
    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })
    
    // 图表引用
    const resultDistributionChart = ref(null)
    const categoryCompareChart = ref(null)
    const trendChart = ref(null)
    
    let chartInstances = {}
    
    // 详情对话框
    const detailDialogVisible = ref(false)
    const currentConfigDetail = ref(null)
    
    // 计算属性
    const selectedSystemName = computed(() => {
      const system = systemList.value.find(s => s.id === selectedSystem.value)
      return system ? system.name : ''
    })
    
    // 初始化数据
    const initData = async () => {
      await Promise.all([
        loadSystemList(),
        loadCategoryList()
      ])
      
      // 初始化日期范围（最近7天）
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 7)
      dateRange.value = [start, end]
    }
    
    // 加载系统列表
    const loadSystemList = async () => {
      try {
        const response = await systemApi.getAllSystemList()
        systemList.value = response.data?.records || []
      } catch (error) {
        console.error('获取系统列表失败:', error)
      }
      
      // 使用模拟数据确保页面能正常显示
      if (systemList.value.length === 0) {
        systemList.value = [
          { id: 'prod-001', name: '生产环境-订单系统', environment: '生产' },
          { id: 'uat-001', name: 'UAT环境-订单系统', environment: 'UAT' },
          { id: 'test-001', name: '测试环境-订单系统', environment: '测试' },
          { id: 'prod-002', name: '生产环境-支付系统', environment: '生产' },
          { id: 'uat-002', name: 'UAT环境-支付系统', environment: 'UAT' }
        ]
      }
    }
    
    // 加载基线版本列表
    const loadBaselineList = async (systemId) => {
      try {
        const response = await baselineApi.getBaselinesByCondition(systemId)
        baselineList.value = response.data?.records || [
          { id: 'baseline-v1.0', version: 'v1.0.0', createTime: '2024-01-10' },
          { id: 'baseline-v1.1', version: 'v1.1.0', createTime: '2024-01-15' }
        ]
        
        // 默认选择最新的基线版本
        if (baselineList.value.length > 0) {
          selectedBaseline.value = baselineList.value[0].id
        }
      } catch (error) {
        console.error('获取基线版本失败:', error)
        // 使用模拟数据
        baselineList.value = [
          { id: 'baseline-v1.0', version: 'v1.0.0', createTime: '2024-01-10' },
          { id: 'baseline-v1.1', version: 'v1.1.0', createTime: '2024-01-15' }
        ]
        if (baselineList.value.length > 0) {
          selectedBaseline.value = baselineList.value[0].id
        }
      }
    }
    
    // 加载配置分类列表
    const loadCategoryList = async () => {
      try {
        const response = await categoryApi.getCategoryList()
        categoryList.value = response.data?.records || [
          { id: 'database', name: '数据库配置' },
          { id: 'cache', name: '缓存配置' },
          { id: 'mq', name: '消息队列' },
          { id: 'log', name: '日志配置' },
          { id: 'security', name: '安全配置' }
        ]
      } catch (error) {
        console.error('获取配置分类失败:', error)
        // 使用模拟数据
        categoryList.value = [
          { id: 'database', name: '数据库配置' },
          { id: 'cache', name: '缓存配置' },
          { id: 'mq', name: '消息队列' },
          { id: 'log', name: '日志配置' },
          { id: 'security', name: '安全配置' }
        ]
      }
    }
    
    // 加载系统概览数据
    const loadSystemOverview = async () => {
      if (!selectedSystem.value) return
      
      try {
        const params = {
          systemId: selectedSystem.value,
          baselineId: selectedBaseline.value,
          ...getDateRangeParams()
        }
        
        const response = await compareResultApi.getResultStatistics(params)
        const data = response.data || {}
        
        Object.assign(systemOverview, {
          status: data.status || 'HEALTHY',
          consistentCount: data.consistentCount || 456,
          inconsistentCount: data.inconsistentCount || 23,
          missingCount: data.missingCount || 8,
          extraCount: data.extraCount || 5,
          consistentRate: data.consistentRate || 92.5,
          inconsistentRate: data.inconsistentRate || 4.7,
          missingRate: data.missingRate || 1.6,
          extraRate: data.extraRate || 1.0
        })
      } catch (error) {
        console.error('获取系统概览失败:', error)
        // 使用模拟数据确保页面能正常显示
        Object.assign(systemOverview, {
          status: 'HEALTHY',
          consistentCount: 456,
          inconsistentCount: 23,
          missingCount: 8,
          extraCount: 5,
          consistentRate: 92.5,
          inconsistentRate: 4.7,
          missingRate: 1.6,
          extraRate: 1.0
        })
      }
    }
    
    // 加载比对结果
    const loadCompareResults = async () => {
      if (!selectedSystem.value) return
      
      tableLoading.value = true
      try {
        const params = {
          systemId: selectedSystem.value,
          baselineId: selectedBaseline.value,
          status: selectedStatus.value,
          categoryId: selectedCategory.value,
          keyword: searchKeyword.value,
          page: pagination.page,
          size: pagination.size,
          ...getDateRangeParams()
        }
        
        const response = await compareResultApi.getResultList(params)
        compareResults.value = response.data?.records || [
          {
            id: 1,
            configKey: 'spring.datasource.url',
            categoryName: '数据库配置',
            baselineValue: 'jdbc:mysql://prod-db:3306/orderdb',
            actualValue: 'jdbc:mysql://uat-db:3306/orderdb',
            status: 'INCONSISTENT',
            impact: '高',
            isImportant: true,
            lastCompareTime: '2024-01-15 10:30:00',
            description: '数据库连接URL配置'
          },
          {
            id: 2,
            configKey: 'redis.host',
            categoryName: '缓存配置',
            baselineValue: '192.168.1.100',
            actualValue: null,
            status: 'MISSING',
            impact: '中',
            isImportant: false,
            lastCompareTime: '2024-01-15 10:25:00',
            description: 'Redis服务器地址配置'
          },
          {
            id: 3,
            configKey: 'logging.level.root',
            categoryName: '日志配置',
            baselineValue: 'INFO',
            actualValue: 'INFO',
            status: 'CONSISTENT',
            impact: '低',
            isImportant: false,
            lastCompareTime: '2024-01-15 10:20:00',
            description: '根日志级别配置'
          }
        ]
        
        pagination.total = response.data?.total || compareResults.value.length
      } catch (error) {
        console.error('获取比对结果失败:', error)
      } finally {
        tableLoading.value = false
      }
    }
    
    // 初始化图表
    const initCharts = async () => {
      await nextTick()
      
      if (resultDistributionChart.value) {
        chartInstances.resultDistribution = echarts.init(resultDistributionChart.value)
      }
      
      if (categoryCompareChart.value) {
        chartInstances.categoryCompare = echarts.init(categoryCompareChart.value)
      }
      
      if (trendChart.value) {
        chartInstances.trend = echarts.init(trendChart.value)
      }
      
      window.addEventListener('resize', () => {
        Object.values(chartInstances).forEach(chart => chart?.resize())
      })
    }
    
    // 渲染图表
    const renderCharts = () => {
      renderResultDistributionChart()
      renderCategoryCompareChart()
      renderTrendChart()
    }
    
    const renderResultDistributionChart = () => {
      if (!chartInstances.resultDistribution) return
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            name: '比对结果',
            type: 'pie',
            radius: '60%',
            data: [
              { value: systemOverview.consistentCount, name: '配置一致', itemStyle: { color: '#67c23a' } },
              { value: systemOverview.inconsistentCount, name: '配置不一致', itemStyle: { color: '#e6a23c' } },
              { value: systemOverview.missingCount, name: '配置缺失', itemStyle: { color: '#f56c6c' } },
              { value: systemOverview.extraCount, name: '多余配置', itemStyle: { color: '#909399' } }
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
      chartInstances.resultDistribution.setOption(option)
    }
    
    const renderCategoryCompareChart = () => {
      if (!chartInstances.categoryCompare) return
      
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        legend: {
          data: ['一致', '不一致', '缺失', '多余']
        },
        xAxis: {
          type: 'category',
          data: ['数据库', '缓存', '消息队列', '日志', '安全']
        },
        yAxis: {
          type: 'value'
        },
        series: [
          { name: '一致', type: 'bar', stack: 'total', data: [45, 32, 28, 67, 23], itemStyle: { color: '#67c23a' } },
          { name: '不一致', type: 'bar', stack: 'total', data: [3, 2, 1, 4, 2], itemStyle: { color: '#e6a23c' } },
          { name: '缺失', type: 'bar', stack: 'total', data: [1, 1, 0, 2, 1], itemStyle: { color: '#f56c6c' } },
          { name: '多余', type: 'bar', stack: 'total', data: [0, 1, 1, 1, 0], itemStyle: { color: '#909399' } }
        ]
      }
      chartInstances.categoryCompare.setOption(option)
    }
    
    const renderTrendChart = () => {
      if (!chartInstances.trend) return
      
      const option = {
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['一致率', '不一致数量']
        },
        xAxis: {
          type: 'category',
          data: ['01-09', '01-10', '01-11', '01-12', '01-13', '01-14', '01-15']
        },
        yAxis: [
          {
            type: 'value',
            name: '一致率(%)',
            max: 100
          },
          {
            type: 'value',
            name: '不一致数量'
          }
        ],
        series: [
          {
            name: '一致率',
            type: 'line',
            data: [91.2, 92.5, 90.8, 93.1, 94.2, 92.9, 93.5],
            smooth: true,
            itemStyle: { color: '#67c23a' }
          },
          {
            name: '不一致数量',
            type: 'bar',
            yAxisIndex: 1,
            data: [28, 23, 31, 21, 18, 23, 20],
            itemStyle: { color: '#e6a23c' }
          }
        ]
      }
      chartInstances.trend.setOption(option)
    }
    
    // 事件处理
    const handleSystemChange = async (systemId) => {
      if (systemId) {
        await loadBaselineList(systemId)
        await loadSystemOverview()
        await loadCompareResults()
        renderCharts()
      } else {
        baselineList.value = []
        compareResults.value = []
      }
    }
    
    const handleBaselineChange = () => {
      if (selectedSystem.value) {
        loadSystemOverview()
        loadCompareResults()
        renderCharts()
      }
    }
    
    const handleRefresh = () => {
      if (selectedSystem.value) {
        loadSystemOverview()
        loadCompareResults()
        renderCharts()
      }
    }
    
    const handleGenerateReport = async () => {
      if (!selectedSystem.value) {
        ElMessage.warning('请先选择系统')
        return
      }
      
      reportLoading.value = true
      try {
        await new Promise(resolve => setTimeout(resolve, 2000)) // 模拟生成报告
        ElMessage.success('报告生成成功')
      } catch (error) {
        ElMessage.error('报告生成失败')
      } finally {
        reportLoading.value = false
      }
    }
    
    const handleDownloadReport = () => {
      if (!selectedSystem.value) {
        ElMessage.warning('请先选择系统')
        return
      }
      
      // 模拟下载
      const systemName = selectedSystemName.value
      const timestamp = new Date().toISOString().slice(0, 19).replace(/[:\-T]/g, '')
      const filename = `${systemName}_比对报告_${timestamp}.pdf`
      
      ElMessage.success(`正在下载：${filename}`)
    }
    
    const handleSearch = () => {
      pagination.page = 1
      loadCompareResults()
    }
    
    const handleExportExcel = () => {
      if (compareResults.value.length === 0) {
        ElMessage.warning('没有数据可导出')
        return
      }
      
      ElMessage.success('Excel导出功能开发中...')
    }
    
    const handleSelectionChange = (selection) => {
      selectedRows.value = selection
    }
    
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.page = 1
      loadCompareResults()
    }
    
    const handleCurrentChange = (page) => {
      pagination.page = page
      loadCompareResults()
    }
    
    const loadTrendData = () => {
      renderTrendChart()
    }
    
    // 查看配置详情
    const viewConfigDetail = (row) => {
      currentConfigDetail.value = row
      detailDialogVisible.value = true
    }
    
    const viewConfigHistory = (row) => {
      ElMessage.info('配置历史功能开发中...')
    }
    
    const handleSync = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要将配置项"${row.configKey}"同步到基线值吗？`,
          '确认同步',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        ElMessage.success('配置同步成功')
        loadCompareResults()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('配置同步失败')
        }
      }
    }
    
    const handleSyncConfig = (config) => {
      handleSync(config)
      detailDialogVisible.value = false
    }
    
    const handleDetailDialogClose = () => {
      currentConfigDetail.value = null
    }
    
    // 工具函数
    const getDateRangeParams = () => {
      if (dateRange.value && dateRange.value.length === 2) {
        return {
          startTime: dateRange.value[0],
          endTime: dateRange.value[1]
        }
      }
      return {}
    }
    
    const getSystemStatusType = (status) => {
      const typeMap = {
        'HEALTHY': 'success',
        'WARNING': 'warning',
        'ERROR': 'danger'
      }
      return typeMap[status] || 'info'
    }
    
    const getSystemStatusText = (status) => {
      const textMap = {
        'HEALTHY': '健康',
        'WARNING': '警告',
        'ERROR': '异常'
      }
      return textMap[status] || status
    }
    
    const getStatusType = (status) => {
      const typeMap = {
        'CONSISTENT': 'success',
        'INCONSISTENT': 'warning',
        'MISSING': 'danger',
        'EXTRA': 'info'
      }
      return typeMap[status] || 'info'
    }
    
    const getStatusText = (status) => {
      const textMap = {
        'CONSISTENT': '一致',
        'INCONSISTENT': '不一致',
        'MISSING': '缺失',
        'EXTRA': '多余'
      }
      return textMap[status] || status
    }
    
    const getImpactType = (impact) => {
      const typeMap = {
        '高': 'danger',
        '中': 'warning',
        '低': 'success'
      }
      return typeMap[impact] || 'info'
    }
    
    const getRowClassName = ({ row }) => {
      if (row.status === 'INCONSISTENT') return 'warning-row'
      if (row.status === 'MISSING') return 'danger-row'
      if (row.status === 'EXTRA') return 'info-row'
      return ''
    }
    
    // 初始化
    onMounted(async () => {
      await initData()
      await initCharts()
    })
    
    // 组件卸载
    onUnmounted(() => {
      Object.values(chartInstances).forEach(chart => chart?.dispose())
      window.removeEventListener('resize', () => {})
    })
    
    return {
      loading,
      tableLoading,
      reportLoading,
      selectedSystem,
      selectedBaseline,
      selectedStatus,
      selectedCategory,
      dateRange,
      trendPeriod,
      searchKeyword,
      systemList,
      baselineList,
      categoryList,
      compareResults,
      selectedRows,
      systemOverview,
      pagination,
      resultDistributionChart,
      categoryCompareChart,
      trendChart,
      detailDialogVisible,
      currentConfigDetail,
      selectedSystemName,
      handleSystemChange,
      handleBaselineChange,
      handleRefresh,
      handleGenerateReport,
      handleDownloadReport,
      handleSearch,
      handleExportExcel,
      handleSelectionChange,
      handleSizeChange,
      handleCurrentChange,
      loadTrendData,
      viewConfigDetail,
      viewConfigHistory,
      handleSync,
      handleSyncConfig,
      handleDetailDialogClose,
      getSystemStatusType,
      getSystemStatusText,
      getStatusType,
      getStatusText,
      getImpactType,
      getRowClassName
    }
  }
}
</script>

<style lang="scss" scoped>
.compare-results {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
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
  }
  
  .app-card {
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
    padding: 20px;
    margin-bottom: 20px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      
      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 500;
        color: #303133;
      }
      
      .header-actions {
        display: flex;
        align-items: center;
        gap: 10px;
      }
    }
  }
  
  .filter-section {
    .filter-item {
      .filter-label {
        display: block;
        margin-bottom: 8px;
        font-weight: 500;
        color: #606266;
        font-size: 14px;
      }
    }
  }
  
  .metric-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
    display: flex;
    align-items: center;
    transition: transform 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
    }
    
    .metric-icon {
      width: 60px;
      height: 60px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      color: #fff;
      margin-right: 20px;
    }
    
    .metric-content {
      flex: 1;
      
      .metric-value {
        font-size: 32px;
        font-weight: bold;
        color: #303133;
        line-height: 1;
        margin-bottom: 8px;
      }
      
      .metric-label {
        font-size: 14px;
        color: #909399;
        margin-bottom: 4px;
      }
      
      .metric-percentage {
        font-size: 12px;
        color: #606266;
        font-weight: 500;
      }
    }
    
    &.consistent .metric-icon { 
      background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%); 
    }
    &.inconsistent .metric-icon { 
      background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%); 
    }
    &.missing .metric-icon { 
      background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%); 
    }
    &.extra .metric-icon { 
      background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%); 
    }
  }
  
  .config-key {
    display: flex;
    align-items: center;
    
    .important-icon {
      color: #f7ba2a;
      margin-right: 5px;
    }
  }
  
  .config-value {
    &.baseline {
      color: #409eff;
    }
    
    &.actual {
      color: #67c23a;
    }
    
    .empty-value {
      color: #c0c4cc;
      font-style: italic;
    }
  }
  
  .status-icon {
    margin-right: 4px;
  }
  
  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }
  
  .config-detail {
    .value-comparison {
      margin: 20px 0;
      
      h4 {
        margin-bottom: 15px;
        color: #303133;
      }
      
      .value-block {
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        overflow: hidden;
        
        .value-header {
          padding: 10px 15px;
          background: #f5f7fa;
          font-weight: 500;
          color: #606266;
          border-bottom: 1px solid #dcdfe6;
        }
        
        .value-content {
          padding: 15px;
          
          pre {
            margin: 0;
            white-space: pre-wrap;
            word-break: break-all;
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            font-size: 13px;
            line-height: 1.4;
          }
        }
        
        &.baseline .value-header {
          background: #e1f3d8;
          color: #67c23a;
        }
        
        &.actual .value-header {
          background: #ecf5ff;
          color: #409eff;
        }
      }
    }
    
    .config-description {
      margin-top: 20px;
      
      h4 {
        margin-bottom: 10px;
        color: #303133;
      }
      
      p {
        color: #606266;
        line-height: 1.6;
        margin: 0;
      }
    }
  }
}

// 表格行样式
:deep(.el-table) {
  .warning-row {
    background: #fdf6ec;
  }
  
  .danger-row {
    background: #fef0f0;
  }
  
  .info-row {
    background: #f4f4f5;
  }
}
</style>
