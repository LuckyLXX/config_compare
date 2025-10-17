<template>
  <div class="compare-results">
    <div class="page-header">
      <h2 class="page-title">比对结果</h2>
      <div class="page-actions">
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出结果
        </el-button>
        <el-button type="info" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="app-card">
      <el-form :model="searchForm" label-width="80px" :inline="true">
        <el-form-item label="任务名称">
          <el-input
            v-model="searchForm.taskName"
            placeholder="请输入任务名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="系统">
          <el-select
            v-model="searchForm.systemId"
            placeholder="请选择系统"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="system in systemList"
              :key="system.id"
              :label="system.systemName"
              :value="system.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="比对状态">
          <el-select
            v-model="searchForm.compareStatus"
            placeholder="请选择比对状态"
            clearable
            style="width: 150px"
          >
            <el-option label="一致" :value="1" />
            <el-option label="不一致" :value="0" />
            <el-option label="比对失败" :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行时间">
          <el-date-picker
            v-model="searchForm.executeTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 350px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计信息 -->
    <div class="app-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalCount || 0 }}</div>
            <div class="stat-label">总数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item success">
            <div class="stat-value">{{ statistics.consistentCount || 0 }}</div>
            <div class="stat-label">一致</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item warning">
            <div class="stat-value">{{ statistics.inconsistentCount || 0 }}</div>
            <div class="stat-label">不一致</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item danger">
            <div class="stat-value">{{ statistics.failedCount || 0 }}</div>
            <div class="stat-label">比对失败</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 结果列表 -->
    <div class="app-card">
      <el-table
        v-loading="loading"
        :data="resultList"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="systemName" label="系统" width="120" />
        <el-table-column prop="serverInstance.hostname" label="服务器" width="120" />
        <el-table-column prop="compareStatus" label="比对状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getCompareStatusColor(row.compareStatus)">
              {{ getCompareStatusText(row.compareStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="consistencyScore" label="一致性评分" width="100">
          <template #default="{ row }">
            <span :class="getConsistencyScoreClass(row.consistencyScore)">
              {{ Math.round(row.consistencyScore || 0) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="差异统计" width="150">
          <template #default="{ row }">
            <div class="diff-count">
              <span class="total">总计: {{ row.diffCount || 0 }}</span>
              <span class="high">高: {{ row.highDiffCount || 0 }}</span>
              <span class="medium">中: {{ row.mediumDiffCount || 0 }}</span>
              <span class="low">低: {{ row.lowDiffCount || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="executeTime" label="执行时间" width="180" />
        <el-table-column prop="durationMs" label="耗时" width="100">
          <template #default="{ row }">
            {{ formatDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetails(row)">
              查看详情
            </el-button>
            <el-button type="warning" size="small" @click="handleViewDiff(row)">
              差异分析
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="比对结果详情"
      width="800px"
      top="5vh"
    >
      <div v-if="currentResult">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务名称">{{ currentResult.taskName }}</el-descriptions-item>
          <el-descriptions-item label="系统">{{ currentResult.systemName }}</el-descriptions-item>
          <el-descriptions-item label="服务器">{{ currentResult.serverInstance?.hostname }}</el-descriptions-item>
          <el-descriptions-item label="比对状态">
            <el-tag :type="getCompareStatusColor(currentResult.compareStatus)">
              {{ getCompareStatusText(currentResult.compareStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="一致性评分">
            {{ Math.round(currentResult.consistencyScore || 0) }}%
          </el-descriptions-item>
          <el-descriptions-item label="执行时间">{{ currentResult.executeTime }}</el-descriptions-item>
          <el-descriptions-item label="执行耗时">{{ formatDuration(currentResult.durationMs) }}</el-descriptions-item>
        </el-descriptions>
        
        <div style="margin-top: 20px" v-if="currentResult.diffSummary">
          <h4>差异摘要</h4>
          <el-card>
            <pre>{{ formatJson(currentResult.diffSummary) }}</pre>
          </el-card>
        </div>
      </div>
    </el-dialog>

    <!-- 差异分析对话框 -->
    <el-dialog
      v-model="diffDialogVisible"
      title="差异分析 - 左右对比"
      width="1400px"
      top="5vh"
    >
      <!-- 差异统计信息 -->
      <div class="diff-summary" style="margin-bottom: 20px;">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="总差异数" :value="diffList.length" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="新增行" :value="diffList.filter(d => d.diffType === 'ADD').length" value-style="color: #67c23a" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="删除行" :value="diffList.filter(d => d.diffType === 'DELETE').length" value-style="color: #f56c6c" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="修改行" :value="diffList.filter(d => d.diffType === 'MODIFY').length" value-style="color: #e6a23c" />
          </el-col>
        </el-row>
      </div>

      <!-- 左右对比显示 -->
      <div class="side-by-side-diff">
        <div class="diff-header">
          <div class="baseline-side">
            <h4>基线配置</h4>
            <span class="file-info">{{ currentResult?.baselineName || '基线配置' }}</span>
          </div>
          <div class="current-side">
            <h4>当前配置</h4>
            <span class="file-info">{{ currentResult?.serverInstance?.hostname || '当前配置' }}</span>
          </div>
        </div>
        
        <div class="diff-content" v-loading="diffLoading">
          <div class="baseline-content">
            <div class="line-numbers">
              <div v-for="(line, index) in baselineLines" :key="`baseline-${index}`" 
                   :class="['line-number', getLineClass(index, 'baseline')]">
                {{ index + 1 }}
              </div>
            </div>
            <div class="content-lines">
              <div v-for="(line, index) in baselineLines" :key="`baseline-line-${index}`"
                   :class="['content-line', getLineClass(index, 'baseline')]">
                <span v-html="highlightLine(line, index, 'baseline')"></span>
              </div>
            </div>
          </div>
          
          <div class="current-content">
            <div class="line-numbers">
              <div v-for="(line, index) in currentLines" :key="`current-${index}`"
                   :class="['line-number', getLineClass(index, 'current')]">
                {{ index + 1 }}
              </div>
            </div>
            <div class="content-lines">
              <div v-for="(line, index) in currentLines" :key="`current-line-${index}`"
                   :class="['content-line', getLineClass(index, 'current')]">
                <span v-html="highlightLine(line, index, 'current')"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 差异详情表格 -->
      <el-divider content-position="left">差异详情</el-divider>
      <el-table
        :data="diffList"
        stripe
        max-height="300"
      >
        <el-table-column prop="diffPath" label="行号" width="80" />
        <el-table-column prop="diffType" label="差异类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getDiffTypeColor(row.diffType)">
              {{ getDiffTypeText(row.diffType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diffLevel" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="getSeverityColor(row.diffLevel)" size="small">
              {{ getSeverityText(row.diffLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baselineValue" label="基线值" min-width="200" show-overflow-tooltip />
        <el-table-column prop="currentValue" label="当前值" min-width="200" show-overflow-tooltip />
        <el-table-column prop="suggestAction" label="建议操作" min-width="150" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { compareResultApi } from '@/api/compare'
import { systemApi } from '@/api/system'

export default {
  name: 'CompareResults',
  setup() {
    const route = useRoute()
    
    // 响应式数据
    const loading = ref(false)
    const diffLoading = ref(false)
    const resultList = ref([])
    const systemList = ref([])
    const diffList = ref([])
    const currentResult = ref(null)
    const statistics = ref({})
    
    // 左右对比显示相关数据
    const baselineLines = ref([])
    const currentLines = ref([])
    const diffLineMap = ref(new Map()) // 存储行号与差异的映射
    
    // 对话框状态
    const detailDialogVisible = ref(false)
    const diffDialogVisible = ref(false)
    
    // 搜索表单
    const searchForm = reactive({
      taskName: '',
      systemId: null,
      compareStatus: null,
      executeTimeRange: null,
      taskId: null // 从路由参数获取
    })
    
    // 分页
    const pagination = reactive({
      current: 1,
      size: 20,
      total: 0
    })
    
    // 获取结果列表
    const getResultList = async () => {
      loading.value = true
      try {
        const params = {
          ...searchForm,
          current: pagination.current,
          size: pagination.size
        }
        
        // 处理时间范围
        if (searchForm.executeTimeRange && searchForm.executeTimeRange.length === 2) {
          params.startTime = searchForm.executeTimeRange[0]
          params.endTime = searchForm.executeTimeRange[1]
        }
        delete params.executeTimeRange
        
        // 确保taskId参数正确传递
        if (searchForm.taskId) {
          params.taskId = searchForm.taskId
        }
        
        console.log('查询参数:', params)
        
        const response = await compareResultApi.getResultList(params)
        resultList.value = response.data?.records || []
        pagination.total = response.data?.total || 0
        pagination.current = response.data?.current || 1
        pagination.size = response.data?.size || 20
        
        console.log('查询结果:', resultList.value.length, '条记录')
      } catch (error) {
        console.error('获取结果列表失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 获取统计信息
    const getStatistics = async () => {
      try {
        const params = { ...searchForm }
        if (searchForm.executeTimeRange && searchForm.executeTimeRange.length === 2) {
          params.startTime = searchForm.executeTimeRange[0]
          params.endTime = searchForm.executeTimeRange[1]
        }
        delete params.executeTimeRange
        
        // 确保taskId参数正确传递
        if (searchForm.taskId) {
          params.taskId = searchForm.taskId
        }
        
        const response = await compareResultApi.getResultStatistics(params)
        statistics.value = response.data || {}
      } catch (error) {
        console.error('获取统计信息失败:', error)
      }
    }
    
    // 获取系统列表
    const getSystemList = async () => {
      try {
        const response = await systemApi.getAllSystemList()
        // 后端返回的是直接的列表，不是分页格式
        systemList.value = response.data || []
        console.log('系统列表获取成功:', systemList.value.length, '个系统')
      } catch (error) {
        console.error('获取系统列表失败:', error)
        systemList.value = []
      }
    }
    
    // 事件处理函数
    const handleSearch = () => {
      pagination.current = 1
      getResultList()
      getStatistics()
    }
    
    const handleReset = () => {
      Object.assign(searchForm, {
        taskName: '',
        systemId: null,
        compareStatus: null,
        executeTimeRange: null
      })
      handleSearch()
    }
    
    const handleRefresh = () => {
      getResultList()
      getStatistics()
    }
    
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      getResultList()
    }
    
    const handleCurrentChange = (current) => {
      pagination.current = current
      getResultList()
    }
    
    const handleViewDetails = (row) => {
      currentResult.value = row
      detailDialogVisible.value = true
    }
    
    const handleViewDiff = async (row) => {
      try {
        diffLoading.value = true
        currentResult.value = row
        
        // 获取差异详情
        const response = await compareResultApi.getDiffDetails(row.id)
        diffList.value = response.data?.records || []
        
        // 构建左右对比数据
        await buildSideBySideData(row)
        
        diffDialogVisible.value = true
      } catch (error) {
        console.error('获取差异详情失败:', error)
      } finally {
        diffLoading.value = false
      }
    }
    
    // 构建左右对比数据
    const buildSideBySideData = async (result) => {
      try {
        // 从差异详情API获取基线内容和当前内容
        const response = await compareResultApi.getDiffDetails(result.id)
        if (response.data) {
          const baselineContent = response.data.baselineContent || ''
          const currentContent = response.data.currentContent || ''
          
          // 按行分割
          baselineLines.value = baselineContent.split('\n')
          currentLines.value = currentContent.split('\n')
          
          // 构建差异行映射
          diffLineMap.value.clear()
          
          // 处理文本比对的差异（line_格式）
          diffList.value.forEach(diff => {
            if (diff.diffPath && diff.diffPath.startsWith('line_')) {
              const lineNum = parseInt(diff.diffPath.replace('line_', '')) - 1
              diffLineMap.value.set(lineNum, diff)
            }
          })
          
          // 处理JSON比对的差异：通过diffKey找到对应的行
          if (diffLineMap.value.size === 0) {
            const baselineLines = baselineContent.split('\n')
            const currentLines = currentContent.split('\n')
            
            // 遍历差异列表，通过diffKey找到对应的行
            diffList.value.forEach(diff => {
              if (diff.diffKey) {
                // 在基线内容和当前内容中查找包含diffKey的行
                for (let i = 0; i < Math.max(baselineLines.length, currentLines.length); i++) {
                  const baselineLine = baselineLines[i] || ''
                  const currentLine = currentLines[i] || ''
                  
                  // 如果行中包含diffKey，标记为差异行
                  if (baselineLine.includes(diff.diffKey) || currentLine.includes(diff.diffKey)) {
                    diffLineMap.value.set(i, diff)
                    break // 找到第一个匹配的行就停止
                  }
                }
              }
            })
          }
        }
      } catch (error) {
        console.error('构建左右对比数据失败:', error)
      }
    }
    
    // 获取行样式类
    const getLineClass = (index, side) => {
      const diff = diffLineMap.value.get(index)
      if (!diff) return side
      
      switch (diff.diffType) {
        case 'ADD':
          return side === 'current' ? 'diff-added' : side
        case 'DELETE':
          return side === 'baseline' ? 'diff-removed' : side
        case 'MODIFY':
          return 'diff-modified'
        default:
          return side
      }
    }
    
    // 高亮行内容
    const highlightLine = (line, index, side) => {
      const diff = diffLineMap.value.get(index)
      if (!diff) return line
      
      // 简单的HTML转义
      const escapedLine = line
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
      
      return escapedLine
    }
    
    const handleExport = async () => {
      try {
        const params = { ...searchForm }
        if (searchForm.executeTimeRange && searchForm.executeTimeRange.length === 2) {
          params.startTime = searchForm.executeTimeRange[0]
          params.endTime = searchForm.executeTimeRange[1]
        }
        delete params.executeTimeRange
        
        const response = await compareResultApi.exportResults(params)
        // 处理文件下载
        const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `比对结果_${new Date().getTime()}.xlsx`
        link.click()
        window.URL.revokeObjectURL(url)
        
        ElMessage.success('导出成功')
      } catch (error) {
        console.error('导出失败:', error)
      }
    }
    
    // 工具函数
    const getCompareStatusColor = (status) => {
      const colorMap = {
        1: 'success',
        0: 'warning',
        '-1': 'danger'
      }
      return colorMap[status] || 'info'
    }
    
    const getCompareStatusText = (status) => {
      const textMap = {
        1: '一致',
        0: '不一致',
        '-1': '比对失败'
      }
      return textMap[status] || '未知'
    }
    
    const getConsistencyScoreClass = (score) => {
      if (score >= 90) return 'text-success'
      if (score >= 70) return 'text-warning'
      return 'text-danger'
    }
    
    const getDiffTypeColor = (type) => {
      const colorMap = {
        'ADD': 'success',
        'DELETE': 'danger',
        'MODIFY': 'warning'
      }
      return colorMap[type] || 'info'
    }
    
    const getDiffTypeText = (type) => {
      const textMap = {
        'ADD': '新增',
        'DELETE': '缺失',
        'MODIFY': '修改'
      }
      return textMap[type] || '未知'
    }
    
    const getSeverityColor = (severity) => {
      const colorMap = {
        'HIGH': 'danger',
        'MEDIUM': 'warning',
        'LOW': 'info'
      }
      return colorMap[severity] || 'info'
    }
    
    const getSeverityText = (severity) => {
      const textMap = {
        'HIGH': '高',
        'MEDIUM': '中',
        'LOW': '低'
      }
      return textMap[severity] || '未知'
    }
    
    const formatDuration = (ms) => {
      if (!ms) return '-'
      if (ms < 1000) return `${ms}ms`
      if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
      return `${(ms / 60000).toFixed(1)}min`
    }
    
    const formatJson = (jsonStr) => {
      try {
        return JSON.stringify(JSON.parse(jsonStr), null, 2)
      } catch (error) {
        return jsonStr
      }
    }
    
    // 初始化
    onMounted(async () => {
      // 从路由参数获取taskId
      if (route.query.taskId) {
        searchForm.taskId = route.query.taskId
      }
      
      await Promise.all([
        getResultList(),
        getStatistics(),
        getSystemList()
      ])
    })
    
    return {
      loading,
      diffLoading,
      resultList,
      systemList,
      diffList,
      currentResult,
      statistics,
      baselineLines,
      currentLines,
      detailDialogVisible,
      diffDialogVisible,
      searchForm,
      pagination,
      handleSearch,
      handleReset,
      handleRefresh,
      handleSizeChange,
      handleCurrentChange,
      handleViewDetails,
      handleViewDiff,
      handleExport,
      getCompareStatusColor,
      getCompareStatusText,
      getConsistencyScoreClass,
      getDiffTypeColor,
      getDiffTypeText,
      getSeverityColor,
      getSeverityText,
      getLineClass,
      highlightLine,
      formatDuration,
      formatJson
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
      font-size: 20px;
      font-weight: 500;
    }
    
    .page-actions {
      display: flex;
      gap: 10px;
    }
  }
  
  .app-card {
    background: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
    padding: 20px;
    margin-bottom: 20px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .stat-item {
    text-align: center;
    padding: 20px;
    border-radius: 4px;
    background: #f5f7fa;
    
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #606266;
      margin-bottom: 5px;
    }
    
    .stat-label {
      font-size: 14px;
      color: #909399;
    }
    
    &.success {
      background: #f0f9ff;
      .stat-value { color: #67c23a; }
    }
    
    &.warning {
      background: #fdf6ec;
      .stat-value { color: #e6a23c; }
    }
    
    &.danger {
      background: #fef0f0;
      .stat-value { color: #f56c6c; }
    }
  }
  
  .diff-count {
    display: flex;
    flex-direction: column;
    gap: 2px;
    
    span {
      font-size: 12px;
      
      &.total { color: #909399; }
      &.high { color: #f56c6c; }
      &.medium { color: #e6a23c; }
      &.low { color: #409eff; }
    }
  }
  
  .pagination-container {
    margin-top: 20px;
    text-align: right;
  }
  
  .text-success { color: #67c23a; }
  .text-warning { color: #e6a23c; }
  .text-danger { color: #f56c6c; }
  
  .side-by-side-diff {
    border: 1px solid #ebeef5;
    border-radius: 4px;
    overflow: hidden;
    margin-bottom: 20px;
    
    .diff-header {
      display: flex;
      background-color: #f5f7fa;
      border-bottom: 1px solid #ebeef5;
      
      .baseline-side, .current-side {
        flex: 1;
        padding: 15px 20px;
        text-align: center;
        border-right: 1px solid #ebeef5;
        
        &:last-child {
          border-right: none;
        }
        
        h4 {
          margin: 0 0 8px 0;
          font-size: 16px;
          color: #303133;
        }
        
        .file-info {
          font-size: 14px;
          color: #909399;
        }
      }
    }
    
    .diff-content {
      display: flex;
      height: 500px;
      overflow: hidden;
      
      .baseline-content, .current-content {
        flex: 1;
        position: relative;
        border-right: 1px solid #ebeef5;
        overflow-y: auto;
        
        &:last-child {
          border-right: none;
        }
        
        .line-numbers {
          position: absolute;
          top: 0;
          left: 0;
          width: 50px;
          background-color: #fafafa;
          border-right: 1px solid #ebeef5;
          z-index: 1;
          
          .line-number {
            height: 20px;
            line-height: 20px;
            padding: 0 10px;
            text-align: right;
            color: #909399;
            font-size: 12px;
            font-family: monospace;
            user-select: none;
            border-bottom: 1px solid #f0f0f0;
          }
        }
        
        .content-lines {
          padding-left: 50px;
          
          .content-line {
            height: 20px;
            line-height: 20px;
            padding: 0 10px;
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            font-size: 13px;
            white-space: pre;
            border-bottom: 1px solid #f0f0f0;
            
            &.diff-added {
              background-color: #f0f9eb;
              color: #67c23a;
            }
            
            &.diff-removed {
              background-color: #fef0f0;
              color: #f56c6c;
            }
            
            &.diff-modified {
              background-color: #fffbe6;
              color: #e6a23c;
            }
          }
        }
      }
    }
  }
}
</style>