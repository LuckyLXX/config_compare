<template>
  <div class="compare-report">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon class="title-icon"><Document /></el-icon>
        比对报告
      </h2>
      <div class="page-actions">
        <el-button type="primary" @click="openOverviewDialog" :disabled="!selectedSystem || compareResults.length === 0">
          <el-icon><Document /></el-icon>
          报告总览
        </el-button>
        <el-button type="success" @click="handleExportExcel" :disabled="!selectedSystem || compareResults.length === 0">
          <el-icon><Document /></el-icon>
          导出Excel
        </el-button>
        <el-button type="info" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-card">
      <div class="filter-header">
        <h3>
          <el-icon><Filter /></el-icon>
          筛选条件
        </h3>
      </div>
      <el-row :gutter="20" class="filter-row">
        <el-col :span="8">
          <div class="filter-item required">
            <label class="filter-label">
              <span class="required-mark">*</span>
              选择系统
            </label>
            <el-select
              v-model="selectedSystem"
              placeholder="请选择要查看的系统"
              @change="handleSystemChange"
              size="large"
              style="width: 100%"
              :loading="systemLoading"
            >
              <el-option
                v-for="system in systemList"
                :key="system.id"
                :label="system.name"
                :value="system.id"
              >
                <div class="system-option">
                  <span class="system-name">{{ system.name }}</span>
                  <el-tag :type="getSystemTagType(system.envType)" size="small">
                    {{ system.envType }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="filter-item">
            <label class="filter-label">配置分类</label>
            <el-select
              v-model="selectedCategory"
              placeholder="全部分类"
              @change="loadCompareResults"
              size="large"
              style="width: 100%"
              clearable
            >
              <el-option label="全部分类" value="" />
              <el-option
                v-for="category in categoryList"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="filter-item">
            <label class="filter-label">报告时间</label>
            <div class="report-time">
              <el-icon><Clock /></el-icon>
              <span v-if="reportTime">{{ reportTime }}</span>
              <span v-else class="no-data">请先选择系统</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 系统概览卡片 -->
    <div class="system-overview" v-if="selectedSystem">
      <div class="overview-header">
        <h3>
          <el-icon><Monitor /></el-icon>
          {{ selectedSystemName }} - 比对概览
        </h3>
        <div class="overview-actions">
          <el-tag :type="getSystemHealthType(systemHealth)" size="large">
            <el-icon>
              <CircleCheck v-if="systemHealth === 'HEALTHY'" />
              <Warning v-else-if="systemHealth === 'WARNING'" />
              <CircleClose v-else />
            </el-icon>
            {{ getSystemHealthText(systemHealth) }}
          </el-tag>
        </div>
      </div>
      
      <el-row :gutter="20" class="overview-metrics">
        <el-col :span="6">
          <div class="metric-card consistent">
            <div class="metric-icon">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ overview.consistentCount || 0 }}</div>
              <div class="metric-label">配置一致</div>
              <div class="metric-percentage">{{ overview.consistentRate || 0 }}%</div>
            </div>
            <div class="metric-trend" :class="getTrendClass(overview.consistentTrend)">
              <el-icon><TrendCharts /></el-icon>
              {{ overview.consistentTrend || 0 }}%
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card inconsistent">
            <div class="metric-icon">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ overview.inconsistentCount || 0 }}</div>
              <div class="metric-label">配置不一致</div>
              <div class="metric-percentage">{{ overview.inconsistentRate || 0 }}%</div>
            </div>
            <div class="metric-trend" :class="getTrendClass(overview.inconsistentTrend)">
              <el-icon><TrendCharts /></el-icon>
              {{ overview.inconsistentTrend || 0 }}%
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card missing">
            <div class="metric-icon">
              <el-icon><Remove /></el-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ overview.missingCount || 0 }}</div>
              <div class="metric-label">配置缺失</div>
              <div class="metric-percentage">{{ overview.missingRate || 0 }}%</div>
            </div>
            <div class="metric-trend" :class="getTrendClass(overview.missingTrend)">
              <el-icon><TrendCharts /></el-icon>
              {{ overview.missingTrend || 0 }}%
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card extra">
            <div class="metric-icon">
              <el-icon><Plus /></el-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ overview.extraCount || 0 }}</div>
              <div class="metric-label">多余配置</div>
              <div class="metric-percentage">{{ overview.extraRate || 0 }}%</div>
            </div>
            <div class="metric-trend" :class="getTrendClass(overview.extraTrend)">
              <el-icon><TrendCharts /></el-icon>
              {{ overview.extraTrend || 0 }}%
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 最新比对结果详情 -->
    <div class="results-section" v-if="selectedSystem">
      <div class="results-header">
        <h3>
          <el-icon><DataAnalysis /></el-icon>
          最新比对结果详情
        </h3>
        <div class="results-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索任务名称..."
            style="width: 300px"
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

      <!-- 结果列表表格 - 参考比对中心的展示方式 -->
      <div class="app-card">
        <el-table
          v-loading="loading"
          :data="compareResults"
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="task-name-cell">
                <span class="task-name">{{ row.taskName }}</span>
                <el-tag type="success" size="small" class="latest-tag">最新</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="systemName" label="系统" width="120">
            <template #default="{ row }">
              {{ selectedSystemName }}
            </template>
          </el-table-column>
          <el-table-column prop="serverInstance" label="服务器" width="120">
            <template #default="{ row }">
              {{ row.serverInstance || row.hostname || '-' }}
            </template>
          </el-table-column>
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
          <el-table-column prop="executeTime" label="执行时间" width="180">
            <template #default="{ row }">
              {{ row.executeTime || row.compareTime }}
            </template>
          </el-table-column>
          <el-table-column prop="durationMs" label="耗时" width="100">
            <template #default="{ row }">
              {{ formatDuration(row.durationMs || row.duration) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="warning" size="small" @click="viewDiffAnalysis(row)">
                <el-icon><DataAnalysis /></el-icon>
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
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty 
        v-if="!loading && compareResults.length === 0 && selectedSystem"
        description="暂无比对结果数据"
        :image-size="120"
      >
        <el-button type="primary" @click="handleRefresh">刷新数据</el-button>
      </el-empty>
    </div>

    <!-- 未选择系统的提示 -->
    <div class="no-system-selected" v-if="!selectedSystem">
      <el-empty
        description="请先选择要查看的系统"
        :image-size="200"
      >
        <div class="empty-actions">
          <el-text type="info">选择系统后，将显示该系统所有比对任务的最新结果</el-text>
        </div>
      </el-empty>
    </div>

  <!-- 报告总览对话框 -->
    <el-dialog
      v-model="overviewDialogVisible"
      title=""
      width="1200px"
      top="20px"
      :show-close="false"
      class="formal-report-dialog"
    >
      <div class="formal-report">
        <!-- 报告头部信息 -->
        <div class="report-header-section">
          <div class="report-title-area">
            <h1 class="main-title">{{ selectedSystemName }} 配置比对分析报告</h1>
            <div class="report-id">报告编号：{{ generateReportId() }}</div>
          </div>
          <div class="report-meta-info">
            <div class="meta-row">
              <div class="meta-item">
                <span class="meta-label">生成时间：</span>
                <span class="meta-value">{{ reportTime || new Date().toLocaleString() }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">系统环境：</span>
                <span class="meta-value">{{ getSystemEnvType(selectedSystem) }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">比对范围：</span>
                <span class="meta-value">{{ compareResults.length }} 个任务</span>
              </div>
            </div>
            <div class="meta-row">
              <div class="meta-item">
                <span class="meta-label">报告状态：</span>
                <span class="meta-value status-complete">已完成</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">总体评分：</span>
                <span class="meta-value">{{ calculateOverallScore() }}分 ({{ getHealthScoreText() }})</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 执行摘要 -->
        <div class="executive-summary-section">
          <h2 class="section-title">一、执行摘要</h2>
          <div class="summary-grid">
            <div class="summary-item consistent">
              <div class="item-number">{{ overview.consistentCount || 0 }}</div>
              <div class="item-label">配置一致项</div>
              <div class="item-percent">{{ overview.consistentRate || 0 }}%</div>
            </div>
            <div class="summary-item inconsistent">
              <div class="item-number">{{ overview.inconsistentCount || 0 }}</div>
              <div class="item-label">配置不一致项</div>
              <div class="item-percent">{{ overview.inconsistentRate || 0 }}%</div>
            </div>
            <div class="summary-item missing">
              <div class="item-number">{{ overview.missingCount || 0 }}</div>
              <div class="item-label">配置缺失项</div>
              <div class="item-percent">{{ overview.missingRate || 0 }}%</div>
            </div>
            <div class="summary-item extra">
              <div class="item-number">{{ overview.extraCount || 0 }}</div>
              <div class="item-label">多余配置项</div>
              <div class="item-percent">{{ overview.extraRate || 0 }}%</div>
            </div>
          </div>
          <div class="summary-conclusion">
            <p>{{ generateSummaryConclusion() }}</p>
          </div>
        </div>

        <!-- 差异分析详细内容 -->
        <div class="diff-analysis-section" v-if="hasSignificantDifferences()">
          <h2 class="section-title">二、差异分析详情</h2>

          <div class="diff-content-wrapper">
            <!-- 按任务分组展示差异 -->
            <div v-for="result in compareResults.filter(r => r.diffCount > 0)" :key="result.id" class="task-diff-section">
              <div class="task-diff-header">
                <h3 class="task-name">{{ result.taskName }}</h3>
                <div class="task-meta">
                  <span class="server-info">{{ result.serverInstance || result.hostname }}</span>
                  <span class="diff-count">{{ result.diffCount }} 个差异</span>
                </div>
              </div>

              <!-- 差异详情展示 -->
              <div class="diff-details">
                <!-- 加载状态 -->
                <div v-if="reportDiffData.has(result.id) && reportDiffData.get(result.id).loading"
                     class="loading-container">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>正在加载差异数据...</span>
                </div>

                <!-- 差异表格 -->
                <el-table v-else-if="reportDiffData.has(result.id) && reportDiffData.get(result.id) && reportDiffData.get(result.id).diffs && reportDiffData.get(result.id).diffs.length > 0"
                         :data="reportDiffData.get(result.id).diffs"
                         stripe
                         class="diff-table"
                         v-loading="reportDiffData.get(result.id).loading">
                  <el-table-column type="index" label="序号" width="60" align="center" />
                  <el-table-column prop="diffPath" label="配置路径" min-width="200" show-overflow-tooltip />
                  <el-table-column prop="diffType" label="差异类型" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag :type="getDiffTypeColor(row.diffType)" size="small">
                        {{ getDiffTypeText(row.diffType) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="diffLevel" label="严重程度" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag :type="getSeverityColor(row.diffLevel)" size="small">
                        {{ getSeverityText(row.diffLevel) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="baselineValue" label="基线值" min-width="200" show-overflow-tooltip>
                    <template #default="{ row }">
                      <code class="value-code">{{ row.baselineValue || '-' }}</code>
                    </template>
                  </el-table-column>
                  <el-table-column prop="currentValue" label="当前值" min-width="200" show-overflow-tooltip>
                    <template #default="{ row }">
                      <code class="current-value-code">{{ row.currentValue || '-' }}</code>
                    </template>
                  </el-table-column>
                  <el-table-column prop="suggestAction" label="建议操作" min-width="180" show-overflow-tooltip />
                </el-table>

                <!-- 无差异数据 -->
                <div v-else-if="reportDiffData.has(result.id) && reportDiffData.get(result.id) && !reportDiffData.get(result.id).loading"
                     class="empty-diff">
                  <el-icon><DocumentChecked /></el-icon>
                  <span>该任务配置一致，无差异项</span>
                </div>

                <!-- 数据加载失败或未加载 -->
                <div v-else class="empty-diff">
                  <el-icon><Warning /></el-icon>
                  <span>暂无差异数据或加载失败</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 比对结果汇总 -->
        <div class="results-summary-section">
          <h2 class="section-title">三、比对结果汇总</h2>
          <el-table :data="compareResults" stripe class="summary-table">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="taskName" label="任务名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="serverInstance" label="服务器实例" width="180" show-overflow-tooltip />
            <el-table-column prop="compareStatus" label="比对状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="getCompareStatusColor(row.compareStatus)">
                  {{ getCompareStatusText(row.compareStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="差异统计" width="200">
              <template #default="{ row }">
                <span class="diff-stat">
                  新增: {{ row.addCount || 0 }} |
                  缺失: {{ row.deleteCount || 0 }} |
                  修改: {{ row.modifyCount || 0 }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="一致性评分" width="120" align="center">
              <template #default="{ row }">
                <span :class="getScoreClass(row.consistencyScore)">
                  {{ Math.round(row.consistencyScore || 0) }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="executeTime" label="执行时间" width="180" show-overflow-tooltip />
          </el-table>
        </div>

        <!-- 结论与建议 -->
        <div class="conclusion-section">
          <h2 class="section-title">四、结论与建议</h2>
          <div class="conclusion-content">
            <div class="conclusion-item" v-for="(item, index) in generateConclusions()" :key="index">
              <div class="conclusion-number">{{ index + 1 }}.</div>
              <div class="conclusion-text">{{ item }}</div>
            </div>
          </div>
        </div>

        <!-- 报告尾部 -->
        <div class="report-footer">
          <div class="footer-info">
            <div class="report-signature">
              <span>配置比对系统自动生成</span>
            </div>
            <div class="report-note">
              本报告由配置比对系统自动生成，如有疑问请联系系统管理员
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="overviewDialogVisible = false">关闭</el-button>
          <el-button type="primary" size="large" @click="handleExportExcel">导出Excel</el-button>
        </div>
      </template>
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
            <span class="file-info">{{ currentResult?.serverInstance || '当前配置' }}</span>
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
        <el-table-column prop="baselineValue" label="基线值" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="value-code baseline-value">{{ row.baselineValue || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="currentValue" label="当前值" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="value-code current-value">{{ row.currentValue || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="suggestAction" label="建议操作" min-width="150" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Printer,
  Refresh,
  Filter,
  Clock,
  Monitor,
  CircleCheck,
  Warning,
  CircleClose,
  Remove,
  Plus,
  TrendCharts,
  DataAnalysis,
  Search,
  DocumentCopy,
  Folder,
  Star,
  View,
  DataBoard,
  QuestionFilled,
  Close,
  Download,
  List
} from '@element-plus/icons-vue'
import { systemApi } from '@/api/system'
import { compareResultApi } from '@/api/compare'
import { categoryApi } from '@/api/baseline'
import { compareReportApi } from '@/api/report'

export default {
  name: 'CompareReport',
  components: {
    Document,
    Printer,
    Refresh,
    Filter,
    Clock,
    Monitor,
    CircleCheck,
    Warning,
    CircleClose,
    Remove,
    Plus,
    TrendCharts,
    DataAnalysis,
    Search,
    DocumentCopy,
    Folder,
    Star,
    View,
    DataBoard,
    QuestionFilled,
    Close,
    Download,
    List
  },
  setup() {
    // 响应式数据
    const loading = ref(false)
    const systemLoading = ref(false)
    
    const diffLoading = ref(false)
    const overviewDialogVisible = ref(false)
    
    // 筛选条件
    const selectedSystem = ref('')
    const selectedCategory = ref('')
    const searchKeyword = ref('')
    
    // 基础数据
    const systemList = ref([])
    const categoryList = ref([])
    const compareResults = ref([])
    const reportTime = ref('')
    const systemHealth = ref('HEALTHY')
    
    // 系统概览数据
    const overview = reactive({
      consistentCount: 0,
      inconsistentCount: 0,
      missingCount: 0,
      extraCount: 0,
      consistentRate: 0,
      inconsistentRate: 0,
      missingRate: 0,
      extraRate: 0,
      consistentTrend: 0,
      inconsistentTrend: 0,
      missingTrend: 0,
      extraTrend: 0
    })
    
    // UI状态
    const activeCategories = ref([])
    const diffDialogVisible = ref(false)
    const currentResult = ref(null)
    const selectedRows = ref([])
    
    // 差异分析相关数据
    const diffList = ref([])
    const baselineLines = ref([])
    const currentLines = ref([])
    const diffLineMap = ref(new Map())

    // 差异统计数据
    const criticalDiffs = ref([])
    const warningDiffs = ref([])

    // 报告用差异数据缓存
    const reportDiffData = ref(new Map())
    
    // 分页数据
    const pagination = reactive({
      current: 1,
      size: 20,
      total: 0
    })
    
    // 计算属性
    const selectedSystemName = computed(() => {
      const system = systemList.value.find(s => s.id === selectedSystem.value)
      return system ? system.name : ''
    })
    
    // 按分类分组的结果
    const categorizedResults = computed(() => {
      if (!compareResults.value.length) return []
      
      const categoryMap = new Map()
      
      compareResults.value.forEach(result => {
        const categoryId = result.categoryId || 'uncategorized'
        const categoryName = result.categoryName || '未分类'
        
        if (!categoryMap.has(categoryId)) {
          categoryMap.set(categoryId, {
            categoryId,
            categoryName,
            totalCount: 0,
            consistentCount: 0,
            inconsistentCount: 0,
            missingCount: 0,
            extraCount: 0,
            configs: []
          })
        }
        
        const category = categoryMap.get(categoryId)
        category.totalCount++
        category.configs.push(result)
        
        // 统计各种状态的数量
        switch (result.status) {
          case 'CONSISTENT':
            category.consistentCount++
            break
          case 'INCONSISTENT':
            category.inconsistentCount++
            break
          case 'MISSING':
            category.missingCount++
            break
          case 'EXTRA':
            category.extraCount++
            break
        }
      })
      
      return Array.from(categoryMap.values()).sort((a, b) => a.categoryName.localeCompare(b.categoryName))
    })
    
    // 初始化数据
    const initData = async () => {
      await loadSystemList()
      await loadCategoryList()
    }
    
    // 加载系统列表
    const loadSystemList = async () => {
      systemLoading.value = true
      try {
        console.log('🔍 开始加载系统列表...')
        const response = await systemApi.getAllSystemList()
        console.log('🔍 系统列表API响应:', response)
        
        // 处理不同的响应格式
        let systemData = []
        if (response.data) {
          systemData = Array.isArray(response.data) ? response.data : (response.data.records || [])
        } else if (Array.isArray(response)) {
          systemData = response
        }
        
        // 转换数据格式，确保字段名称一致
        systemList.value = systemData.map(system => ({
          id: system.id,
          name: system.systemName || system.name,
          envType: system.envType
        }))
        
        console.log('🔍 处理后的系统列表:', systemList.value)
      } catch (error) {
        console.error('❌ 获取系统列表失败:', error)
        ElMessage.error('获取系统列表失败，请稍后重试')
        systemList.value = []
      } finally {
        systemLoading.value = false
      }
    }
    
    // 加载配置分类列表
    const loadCategoryList = async () => {
      try {
        console.log('🔍 开始加载配置分类列表...')
        const response = await categoryApi.getCategoryList()
        console.log('🔍 配置分类API响应:', response)
        
        // 处理响应数据
        let categoryData = []
        if (response.data) {
          categoryData = Array.isArray(response.data) ? response.data : (response.data.records || [])
        } else if (Array.isArray(response)) {
          categoryData = response
        }
        
        // 转换数据格式，确保字段名称一致
        categoryList.value = categoryData.map(category => ({
          id: category.id,
          name: category.categoryName || category.name
        }))
        
        console.log('🔍 处理后的配置分类列表:', categoryList.value)
      } catch (error) {
        console.error('❌ 获取配置分类失败:', error)
        ElMessage.error('获取配置分类失败，请稍后重试')
        categoryList.value = []
      }
    }
    
    // 基于最新比对结果计算系统概览数据
    const loadSystemOverview = async () => {
      if (!selectedSystem.value || compareResults.value.length === 0) return
      
      console.log('🔍 基于最新比对结果计算概览统计...', compareResults.value)
      
      // 基于去重后的最新比对结果进行统计
      let consistentCount = 0, inconsistentCount = 0, missingCount = 0, extraCount = 0
      
      compareResults.value.forEach(result => {
        if (result.compareStatus === 1) {
          // 一致
          consistentCount++
        } else if (result.compareStatus === 0) {
          // 不一致，需要进一步分类
          const deleteCount = result.deleteCount || 0
          const addCount = result.addCount || 0
          const modifyCount = result.modifyCount || 0
          
          // 统计各类差异
          if (deleteCount > 0) missingCount++  // 有DELETE类型的差异 = 配置缺失
          if (addCount > 0) extraCount++       // 有ADD类型的差异 = 多余配置
          if (modifyCount > 0) inconsistentCount++ // 有MODIFY类型的差异 = 配置不一致
          
          // 如果一个结果包含多种差异类型，它会被计入多个分类中
        } else if (result.compareStatus === -1) {
          // 比对失败，归类为缺失
          missingCount++
        }
      })
      
      const totalCount = compareResults.value.length
      
      // 计算百分比
      const consistentRate = totalCount > 0 ? ((consistentCount / totalCount) * 100).toFixed(1) : 0
      const inconsistentRate = totalCount > 0 ? ((inconsistentCount / totalCount) * 100).toFixed(1) : 0
      const missingRate = totalCount > 0 ? ((missingCount / totalCount) * 100).toFixed(1) : 0
      const extraRate = totalCount > 0 ? ((extraCount / totalCount) * 100).toFixed(1) : 0
      
      Object.assign(overview, {
        consistentCount,
        inconsistentCount,
        missingCount,
        extraCount,
        consistentRate: parseFloat(consistentRate),
        inconsistentRate: parseFloat(inconsistentRate),
        missingRate: parseFloat(missingRate),
        extraRate: parseFloat(extraRate),
        consistentTrend: 0, // 趋势数据需要历史对比，暂时设为0
        inconsistentTrend: 0,
        missingTrend: 0,
        extraTrend: 0
      })
      
      // 根据不一致率判断系统健康状态
      if (inconsistentRate == 0 && missingRate == 0 && extraRate == 0) {
        systemHealth.value = 'HEALTHY'
      } else if (inconsistentRate < 20 && missingRate < 10 && extraRate < 10) {
        systemHealth.value = 'WARNING'
      } else {
        systemHealth.value = 'ERROR'
      }
      
      // 使用最新比对结果的时间作为报告时间
      if (compareResults.value.length > 0) {
        const latestResult = compareResults.value[0] // 已按时间倒序排列
        reportTime.value = latestResult.executeTime || new Date().toLocaleString()
      }
      
      console.log('🔍 基于最新数据计算的概览统计:', overview)
      console.log(`🔍 统计: 一致=${consistentCount}, 不一致=${inconsistentCount}, 缺失=${missingCount}, 多余=${extraCount}`)
    }
    
    // 基于去重后的最新比对结果更新概览统计
    const updateOverviewFromResults = () => {
      if (compareResults.value.length === 0) return
      
      console.log('🔍 基于最新比对结果更新概览统计...', compareResults.value)
      
      // 统计各种状态的任务数量
      let consistentCount = 0, inconsistentCount = 0, missingCount = 0, extraCount = 0
      
      compareResults.value.forEach(result => {
        if (result.compareStatus === 1) {
          // 一致
          consistentCount++
        } else if (result.compareStatus === 0) {
          // 不一致，需要进一步分类
          const deleteCount = result.deleteCount || 0
          const addCount = result.addCount || 0
          const modifyCount = result.modifyCount || 0
          
          // 统计各类差异
          if (deleteCount > 0) missingCount++  // 有DELETE类型的差异 = 配置缺失
          if (addCount > 0) extraCount++       // 有ADD类型的差异 = 多余配置
          if (modifyCount > 0) inconsistentCount++ // 有MODIFY类型的差异 = 配置不一致
          
          // 如果一个结果包含多种差异类型，它会被计入多个分类中
        } else if (result.compareStatus === -1) {
          // 比对失败，归类为缺失
          missingCount++
        }
      })
      
      const totalCount = compareResults.value.length
      
      // 计算百分比
      const consistentRate = totalCount > 0 ? ((consistentCount / totalCount) * 100).toFixed(1) : 0
      const inconsistentRate = totalCount > 0 ? ((inconsistentCount / totalCount) * 100).toFixed(1) : 0
      const missingRate = totalCount > 0 ? ((missingCount / totalCount) * 100).toFixed(1) : 0
      const extraRate = totalCount > 0 ? ((extraCount / totalCount) * 100).toFixed(1) : 0
      
      Object.assign(overview, {
        consistentCount,
        inconsistentCount,
        missingCount,
        extraCount,
        consistentRate: parseFloat(consistentRate),
        inconsistentRate: parseFloat(inconsistentRate),
        missingRate: parseFloat(missingRate),
        extraRate: parseFloat(extraRate),
        consistentTrend: 0,
        inconsistentTrend: 0,
        missingTrend: 0,
        extraTrend: 0
      })
      
      // 根据不一致率判断系统健康状态
      if (inconsistentRate == 0 && missingRate == 0 && extraRate == 0) {
        systemHealth.value = 'HEALTHY'
      } else if (inconsistentRate < 20 && missingRate < 10 && extraRate < 10) {
        systemHealth.value = 'WARNING'
      } else {
        systemHealth.value = 'ERROR'
      }
      
      // 使用最新比对结果的时间作为报告时间
      if (compareResults.value.length > 0) {
        const latestResult = compareResults.value[0] // 已按时间倒序排列
        reportTime.value = latestResult.executeTime || new Date().toLocaleString()
      }
      
      console.log('🔍 更新后的概览统计:', overview)
      console.log(`🔍 统计说明: 基于${totalCount}个最新比对任务 - 一致:${consistentCount}, 不一致:${inconsistentCount}, 缺失:${missingCount}, 多余:${extraCount}`)
    }
    
    // 加载比对结果（只获取最新结果）
    const loadCompareResults = async () => {
      if (!selectedSystem.value) return
      
      loading.value = true
      try {
        console.log('🔍 开始加载比对结果...', { 
          systemId: selectedSystem.value, 
          categoryId: selectedCategory.value,
          keyword: searchKeyword.value 
        })
        
        // 构建查询参数，确保只获取最新结果
        const params = {
          systemId: selectedSystem.value,
          current: 1, // 暂时获取所有数据，在前端进行去重
          size: 1000, // 获取足够多的数据进行去重处理
          taskName: searchKeyword.value || '', // 使用搜索关键词作为任务名称筛选
          compareStatus: null, // 不筛选状态，获取所有状态的结果
          latest: true, // 关键参数：只获取最新结果
          onlyLatest: true, // 额外参数：明确要求只返回最新数据
          groupByTask: true // 按任务分组，每组只返回最新的一条
        }
        
        // 如果选择了分类，添加分类筛选（如果API支持）
        if (selectedCategory.value) {
          params.categoryId = selectedCategory.value
        }
        
        const response = await compareResultApi.getResultList(params)
        console.log('🔍 比对结果API响应:', response)
        
        const data = response.data || response || {}
        let resultData = []
        
        if (data.records && Array.isArray(data.records)) {
          resultData = data.records
        } else if (Array.isArray(data)) {
          resultData = data
        }
        
        // 转换数据格式，确保字段名称一致，参考比对中心的数据结构
        let transformedResults = resultData.map(result => ({
          id: result.id,
          taskName: result.taskName,
          systemName: selectedSystemName.value,
          serverInstance: result.serverInstance?.hostname || result.hostname,
          compareStatus: result.compareStatus,
          consistencyScore: result.consistencyScore || calculateConsistencyScore(result),
          diffCount: result.diffCount || 0,
          highDiffCount: result.highDiffCount || 0,
          mediumDiffCount: result.mediumDiffCount || 0,
          lowDiffCount: result.lowDiffCount || 0,
          addCount: result.addCount || 0,      // 新增配置（多余配置）
          deleteCount: result.deleteCount || 0, // 删除配置（配置缺失）
          modifyCount: result.modifyCount || 0, // 修改配置（配置不一致）
          executeTime: result.executeTime || result.compareTime || result.createTime,
          durationMs: result.durationMs || result.duration,
          taskId: result.taskId,
          taskName: result.taskName,
          isLatest: true // 标记为最新结果
        }))
        
        // 🔑 关键逻辑：每个任务只保留最新的一条记录
        // 按任务名称+服务器实例分组，每组只保留最新的一条
        const latestResultsMap = new Map()
        
        transformedResults.forEach(result => {
          // 使用任务名称+服务器实例作为唯一键
          const taskKey = `${result.taskName}-${result.serverInstance || 'default'}`
          const existingResult = latestResultsMap.get(taskKey)
          
          // 比较执行时间，保留最新的记录
          if (!existingResult || new Date(result.executeTime) > new Date(existingResult.executeTime)) {
            latestResultsMap.set(taskKey, result)
            console.log(`🔍 更新最新记录: ${taskKey} -> ${result.executeTime}`)
          } else {
            console.log(`🔍 跳过旧记录: ${taskKey} -> ${result.executeTime} (已有更新的: ${existingResult.executeTime})`)
          }
        })
        
        // 转换为数组并按执行时间倒序排列
        compareResults.value = Array.from(latestResultsMap.values())
          .sort((a, b) => new Date(b.executeTime) - new Date(a.executeTime))
        
        console.log('🔍 去重后的最新比对结果:', compareResults.value)
        console.log(`🔍 原始数据${transformedResults.length}条，去重后${compareResults.value.length}条`)
        
        // 更新分页信息
        pagination.total = compareResults.value.length
        pagination.current = 1 // 重置到第一页
        
        // 🔑 立即基于最新结果更新概览统计
        updateOverviewFromResults()
        
        console.log('🔍 处理后的比对结果:', compareResults.value)
      } catch (error) {
        console.error('❌ 获取比对结果失败:', error)
        ElMessage.error('获取比对结果失败，请稍后重试')
        compareResults.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }
    
    // 事件处理
    const handleSystemChange = async (systemId) => {
      if (systemId) {
        // 先加载比对结果，然后基于结果计算概览统计
        await loadCompareResults()
        await loadSystemOverview()
        
        // 默认展开第一个分类
        if (categorizedResults.value.length > 0) {
          activeCategories.value = [categorizedResults.value[0].categoryId]
        }
      } else {
        compareResults.value = []
        reportTime.value = ''
        // 清空概览数据
        Object.assign(overview, {
          consistentCount: 0,
          inconsistentCount: 0,
          missingCount: 0,
          extraCount: 0,
          consistentRate: 0,
          inconsistentRate: 0,
          missingRate: 0,
          extraRate: 0
        })
      }
    }
    
    const handleRefresh = () => {
      if (selectedSystem.value) {
        // 先加载比对结果，再基于结果更新概览统计
        loadCompareResults()
      }
    }
    
    const handleSearch = () => {
      pagination.current = 1
      loadCompareResults()
    }
    
    const handleSelectionChange = (selection) => {
      selectedRows.value = selection
    }
    
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      loadCompareResults()
    }
    
    const handleCurrentChange = (current) => {
      pagination.current = current
      loadCompareResults()
    }
    
    // 查看差异分析 - 复用比对中心的实现
    const viewDiffAnalysis = async (row) => {
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
        ElMessage.error('获取差异详情失败，请稍后重试')
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
    
    // 计算总体评分
    const calculateOverallScore = () => {
      if (!compareResults.value.length) return 100

      const totalScore = compareResults.value.reduce((sum, result) => {
        return sum + (result.consistencyScore || 0)
      }, 0)

      return Math.round(totalScore / compareResults.value.length)
    }

    // 获取健康评分文字
    const getHealthScoreText = () => {
      const score = calculateOverallScore()
      if (score >= 95) return '优秀'
      if (score >= 85) return '良好'
      if (score >= 70) return '一般'
      if (score >= 60) return '较差'
      return '异常'
    }

    // 获取系统环境类型
    const getSystemEnvType = (systemId) => {
      const system = systemList.value.find(s => s.id === systemId)
      return system ? (system.envType || '未知') : '未知'
    }

    // 检查是否有显著差异
    const hasSignificantDifferences = () => {
      // 检查概览统计数据
      if (overview.inconsistentCount > 0 || overview.missingCount > 0 || overview.extraCount > 0) {
        return true
      }

      // 检查已加载的真实差异数据
      for (const [resultId, data] of reportDiffData.value) {
        if (data.diffs && data.diffs.length > 0) {
          return true
        }
      }

      return false
    }

    // 获取严重差异列表
    const getCriticalDiffs = () => {
      // 模拟数据，实际应该从diffList中筛选
      return [
        { id: 1, diffPath: 'application.properties', diffKey: 'server.port', suggestAction: '建议统一端口配置' },
        { id: 2, diffPath: 'logback.xml', diffKey: 'log.level', suggestAction: '建议调整日志级别' }
      ]
    }

    // 获取警告差异列表
    const getWarningDiffs = () => {
      // 模拟数据，实际应该从diffList中筛选
      return [
        { id: 3, diffPath: 'application.yml', diffKey: 'spring.datasource.url', suggestAction: '建议检查数据库连接' }
      ]
    }

    // 生成报告编号
    const generateReportId = () => {
      const now = new Date()
      const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '')
      const timeStr = now.toTimeString().slice(0, 8).replace(/:/g, '')
      return `RPT-${dateStr}-${timeStr}-${Math.random().toString(36).substr(2, 6).toUpperCase()}`
    }

    // 生成摘要结论
    const generateSummaryConclusion = () => {
      const total = compareResults.value.length
      const consistent = overview.consistentCount || 0
      const inconsistent = overview.inconsistentCount || 0
      const missing = overview.missingCount || 0
      const extra = overview.extraCount || 0

      if (inconsistent === 0 && missing === 0 && extra === 0) {
        return `本次比对共检查了 ${total} 个配置任务，所有配置项均与基线保持一致，系统配置状态良好，未发现任何配置差异。`
      } else {
        let conclusion = `本次比对共检查了 ${total} 个配置任务，发现 `
        const issues = []
        if (inconsistent > 0) issues.push(`${inconsistent} 个配置不一致项`)
        if (missing > 0) issues.push(`${missing} 个配置缺失项`)
        if (extra > 0) issues.push(`${extra} 个多余配置项`)

        conclusion += issues.join('、') + `。建议及时处理发现的配置差异，确保系统配置的一致性和稳定性。`
        return conclusion
      }
    }

    // 获取任务的差异详情（从API获取真实数据）
    const getTaskDiffs = async (result) => {
      // 检查缓存中是否已有数据
      if (reportDiffData.value.has(result.id)) {
        return reportDiffData.value.get(result.id)
      }

      try {
        console.log(`🔍 正在获取任务 ${result.taskName} 的差异详情...`)

        // 调用API获取差异详情
        const response = await compareResultApi.getDiffDetails(result.id)
        let diffs = []

        // 处理API响应数据
        if (response.data) {
          // 如果返回的是records格式
          if (response.data.records && Array.isArray(response.data.records)) {
            diffs = response.data.records
          }
          // 如果直接返回数组
          else if (Array.isArray(response.data)) {
            diffs = response.data
          }
          // 如果是单个对象，尝试解析diffList字段
          else if (response.data.diffList && Array.isArray(response.data.diffList)) {
            diffs = response.data.diffList
          }
        }

        console.log(`🔍 任务 ${result.taskName} 的差异数据:`, diffs)

        // 处理差异数据格式，确保必要的字段存在
        const processedDiffs = diffs.map((diff, index) => ({
          id: diff.id || `${result.id}-diff-${index}`,
          diffPath: diff.diffPath || diff.configKey || diff.diffKey || `未知路径-${index + 1}`,
          diffType: diff.diffType || 'MODIFY',
          diffLevel: diff.diffLevel || 'MEDIUM',
          baselineValue: diff.baselineValue || diff.oldValue || '',
          currentValue: diff.currentValue || diff.newValue || '',
          suggestAction: diff.suggestAction || getDefaultSuggestion(diff.diffType),
          lineNumber: diff.lineNumber || diff.diffPath?.match(/line_(\d+)/)?.[1] || null
        }))

        // 缓存数据（包装成对象格式，包含diffs数组）
        reportDiffData.value.set(result.id, {
          diffs: processedDiffs,
          loading: false
        })

        return processedDiffs
      } catch (error) {
        console.error(`❌ 获取任务 ${result.taskName} 的差异详情失败:`, error)

        // 如果API调用失败，生成模拟数据作为备用
        console.warn(`⚠️ 使用模拟数据作为备用方案`)
        return generateMockDiffs(result)
      }
    }

    // 生成模拟差异数据（备用方案）
    const generateMockDiffs = (result) => {
      const mockDiffs = []

      if (result.addCount > 0) {
        for (let i = 0; i < Math.min(result.addCount, 2); i++) {
          mockDiffs.push({
            id: `mock-add-${result.id}-${i}`,
            diffPath: `application.properties`,
            diffType: 'ADD',
            diffLevel: i === 0 ? 'HIGH' : 'MEDIUM',
            baselineValue: '',
            currentValue: `new.config.${i + 1}=value${i + 1}`,
            suggestAction: '评估新增配置的必要性，如不需要可删除'
          })
        }
      }

      if (result.deleteCount > 0) {
        for (let i = 0; i < Math.min(result.deleteCount, 2); i++) {
          mockDiffs.push({
            id: `mock-delete-${result.id}-${i}`,
            diffPath: `application.properties`,
            diffType: 'DELETE',
            diffLevel: i === 0 ? 'HIGH' : 'MEDIUM',
            baselineValue: `removed.config.${i + 1}=value${i + 1}`,
            currentValue: '',
            suggestAction: '检查配置是否被误删除，如需要请重新添加'
          })
        }
      }

      if (result.modifyCount > 0) {
        for (let i = 0; i < Math.min(result.modifyCount, 2); i++) {
          mockDiffs.push({
            id: `mock-modify-${result.id}-${i}`,
            diffPath: `application.properties`,
            diffType: 'MODIFY',
            diffLevel: i === 0 ? 'HIGH' : 'MEDIUM',
            baselineValue: `config.${i + 1}=old_value${i + 1}`,
            currentValue: `config.${i + 1}=new_value${i + 1}`,
            suggestAction: '核实配置变更的合理性和影响范围'
          })
        }
      }

      return mockDiffs
    }

    // 获取默认建议
    const getDefaultSuggestion = (diffType) => {
      const suggestions = {
        'ADD': '评估新增配置的必要性，如不需要可删除',
        'DELETE': '检查配置是否被误删除，如需要请重新添加',
        'MODIFY': '核实配置变更的合理性和影响范围'
      }
      return suggestions[diffType] || '请核实此配置项的差异'
    }

    // 生成结论与建议
    const generateConclusions = () => {
      const conclusions = []
      const score = calculateOverallScore()

      if (score >= 95) {
        conclusions.push('系统配置状态优秀，所有配置项均符合基线标准')
      } else if (score >= 85) {
        conclusions.push('系统配置状态良好，存在少量配置差异但影响较小')
      } else if (score >= 70) {
        conclusions.push('系统配置状态一般，存在一定数量的配置差异需要关注')
      } else {
        conclusions.push('系统配置状态较差，存在较多配置差异需要及时处理')
      }

      if (overview.inconsistentCount > 0) {
        conclusions.push(`发现 ${overview.inconsistentCount} 个配置不一致项，建议核实配置变更的合理性`)
      }

      if (overview.missingCount > 0) {
        conclusions.push(`发现 ${overview.missingCount} 个配置缺失项，建议检查配置是否被误删除并及时补充`)
      }

      if (overview.extraCount > 0) {
        conclusions.push(`发现 ${overview.extraCount} 个多余配置项，建议评估新增配置的必要性`)
      }

      conclusions.push('建议定期执行配置比对，建立配置变更管理流程，确保系统配置的一致性和可追溯性')

      return conclusions
    }

    // 获取评分样式类
    const getScoreClass = (score) => {
      const value = score || 0
      if (value >= 95) return 'score-excellent'
      if (value >= 85) return 'score-good'
      if (value >= 70) return 'score-warning'
      return 'score-danger'
    }

    // 获取表格行样式类
    const getTableRowClassName = ({ row }) => {
      const score = row.consistencyScore || 0
      if (score < 60) return 'danger-row'
      if (score < 80) return 'warning-row'
      if (score < 95) return 'info-row'
      return ''
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
    
    // 差异类型相关工具函数
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
    
    // 打开报告总览对话框
    const openOverviewDialog = async () => {
      if (!selectedSystem.value) {
        ElMessage.warning('请先选择系统')
        return
      }
      if (compareResults.value.length === 0) {
        ElMessage.warning('暂无比对数据')
        return
      }

      // 清除之前的数据缓存，重新加载
      reportDiffData.value.clear()

      // 预加载所有任务的差异数据
      await preloadAllDiffData()

      overviewDialogVisible.value = true
    }

    // 预加载所有任务的差异数据
    const preloadAllDiffData = async () => {
      const tasksWithDiffs = compareResults.value.filter(r => r.diffCount > 0)

      if (tasksWithDiffs.length === 0) {
        console.log('🔍 没有需要预加载差异的任务')
        return
      }

      console.log(`🔍 开始预加载 ${tasksWithDiffs.length} 个任务的差异数据...`)

      try {
        // 并行加载所有任务的差异数据
        const promises = tasksWithDiffs.map(async (task) => {
          await getTaskDiffs(task)
        })

        await Promise.all(promises)
        console.log('✅ 所有任务差异数据预加载完成')
      } catch (error) {
        console.error('❌ 预加载差异数据时出错:', error)
        ElMessage.warning('部分差异数据加载失败，将显示备用数据')
      }
    }
    
    // 导出Excel
    const handleExportExcel = async () => {
      if (!selectedSystem.value) {
        ElMessage.warning('请先选择系统')
        return
      }
      
      if (compareResults.value.length === 0) {
        ElMessage.warning('暂无比对数据，无法导出')
        return
      }
      
      try {
        console.log('🔍 开始导出Excel...')
        
        // 准备导出数据
        const exportData = {
          systemId: selectedSystem.value,
          systemName: selectedSystemName.value,
          categoryId: selectedCategory.value,
          overview: {
            consistentCount: overview.consistentCount,
            inconsistentCount: overview.inconsistentCount,
            missingCount: overview.missingCount,
            extraCount: overview.extraCount
          },
          compareResults: compareResults.value
        }
        
        // 调用后端API导出Excel
        const response = await compareReportApi.exportCompareExcel(exportData)
        
        // 创建下载链接
        const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        
        const timestamp = new Date().toISOString().slice(0, 19).replace(/[:\-T]/g, '')
        link.download = `${selectedSystemName.value}_比对数据_${timestamp}.xlsx`
        
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        
        ElMessage.success('Excel导出成功')
      } catch (error) {
        console.error('❌ 导出Excel失败:', error)
        ElMessage.error('Excel导出失败，请稍后重试')
      }
    }
    
    
    // 辅助工具函数
    const getCategoryNameById = (categoryId) => {
      const category = categoryList.value.find(c => c.id === categoryId)
      return category ? category.name : '未分类'
    }
    
    const convertCompareStatus = (status) => {
      // 转换数字状态码为字符串状态
      const statusMap = {
        1: 'CONSISTENT',
        2: 'INCONSISTENT', 
        3: 'MISSING',
        4: 'EXTRA'
      }
      return statusMap[status] || status
    }
    
    // 计算一致性评分 - 如果API没有返回的话
    const calculateConsistencyScore = (result) => {
      if (result.consistencyScore !== undefined) return result.consistencyScore
      
      // 根据差异统计计算一致性评分
      const total = (result.diffCount || 0) + (result.consistentCount || 100)
      const consistent = result.consistentCount || (100 - (result.diffCount || 0))
      return total > 0 ? Math.round((consistent / total) * 100) : 100
    }
    
    // 参考比对中心的工具函数
    const getCompareStatusColor = (status) => {
      const colorMap = {
        1: 'success',    // 一致
        0: 'warning',    // 不一致  
        '-1': 'danger'   // 比对失败
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
      if (score >= 95) return 'score-excellent'
      if (score >= 85) return 'score-good'
      if (score >= 70) return 'score-warning'
      return 'score-danger'
    }
    
    // 格式化耗时
    const formatDuration = (ms) => {
      if (!ms) return '-'
      if (ms < 1000) return `${ms}ms`
      if (ms < 60000) return `${Math.round(ms / 1000)}s`
      return `${Math.round(ms / 60000)}min`
    }
    
    // 工具函数
    const getSystemTagType = (envType) => {
      const typeMap = {
        'PROD': 'danger',
        'UAT': 'warning',
        '测试': 'success'
      }
      return typeMap[envType] || 'info'
    }
    
    const getSystemHealthType = (health) => {
      const typeMap = {
        'HEALTHY': 'success',
        'WARNING': 'warning',
        'ERROR': 'danger'
      }
      return typeMap[health] || 'info'
    }
    
    const getSystemHealthText = (health) => {
      const textMap = {
        'HEALTHY': '健康',
        'WARNING': '警告',
        'ERROR': '异常'
      }
      return textMap[health] || health
    }
    
    const getTrendClass = (trend) => {
      const value = parseFloat(trend)
      if (value > 0) return 'trend-up'
      if (value < 0) return 'trend-down'
      return 'trend-stable'
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
    })
    
    return {
      loading,
      systemLoading,
      diffLoading,
      overviewDialogVisible,
      selectedSystem,
      selectedCategory,
      searchKeyword,
      systemList,
      categoryList,
      compareResults,
      reportTime,
      systemHealth,
      overview,
      activeCategories,
      diffDialogVisible,
      currentResult,
      selectedRows,
      pagination,
      diffList,
      baselineLines,
      currentLines,
      criticalDiffs,
      warningDiffs,
      selectedSystemName,
      categorizedResults,
      reportDiffData,
      handleSystemChange,
      handleRefresh,
      handleSearch,
      handleSelectionChange,
      handleSizeChange,
      handleCurrentChange,
      openOverviewDialog,
      handleExportExcel,
      viewDiffAnalysis,
      calculateOverallScore,
      getHealthScoreText,
      getSystemEnvType,
      hasSignificantDifferences,
      getCriticalDiffs,
      getWarningDiffs,
      generateReportId,
      generateSummaryConclusion,
      getTaskDiffs,
      generateConclusions,
      getScoreClass,
      getTableRowClassName,
      getLineClass,
      highlightLine,
      getSystemTagType,
      getSystemHealthType,
      getSystemHealthText,
      getTrendClass,
      getCompareStatusColor,
      getCompareStatusText,
      getConsistencyScoreClass,
      formatDuration,
      getDiffTypeColor,
      getDiffTypeText,
      getSeverityColor,
      getSeverityText,
      getRowClassName
    }
  }
}
</script>

<style lang="scss" scoped>
.compare-report {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding: 20px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    
    .page-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #1f2937;
      display: flex;
      align-items: center;
      
      .title-icon {
        margin-right: 12px;
        font-size: 28px;
        color: #3b82f6;
      }
    }
    
    .page-actions {
      display: flex;
      gap: 12px;
    }
  }
  
  .filter-card {
    background: white;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    
    .filter-header {
      margin-bottom: 20px;
      
      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: #1f2937;
        display: flex;
        align-items: center;
        
        .el-icon {
          margin-right: 8px;
          color: #6366f1;
        }
      }
    }
    
    .filter-row {
      .filter-item {
        &.required .filter-label {
          .required-mark {
            color: #ef4444;
            margin-right: 4px;
          }
        }
        
        .filter-label {
          display: block;
          margin-bottom: 8px;
          font-weight: 500;
          color: #374151;
          font-size: 14px;
        }
        
        .report-time {
          display: flex;
          align-items: center;
          height: 40px;
          padding: 0 12px;
          background: #f9fafb;
          border-radius: 6px;
          border: 1px solid #d1d5db;
          
          .el-icon {
            margin-right: 8px;
            color: #6b7280;
          }
          
          .no-data {
            color: #9ca3af;
            font-style: italic;
          }
        }
      }
    }
  }
  
  .system-option {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    
    .system-name {
      flex: 1;
    }
  }
  
  .system-overview {
    background: white;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    
    .overview-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      
      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: #1f2937;
        display: flex;
        align-items: center;
        
        .el-icon {
          margin-right: 8px;
          color: #10b981;
        }
      }
    }
    
    .overview-metrics {
      .metric-card {
        background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
        border-radius: 12px;
        padding: 24px;
        border: 1px solid #e5e7eb;
        position: relative;
        overflow: hidden;
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
        }
        
        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          height: 4px;
        }
        
        &.consistent::before { background: linear-gradient(90deg, #10b981, #34d399); }
        &.inconsistent::before { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
        &.missing::before { background: linear-gradient(90deg, #ef4444, #f87171); }
        &.extra::before { background: linear-gradient(90deg, #6b7280, #9ca3af); }
        
        .metric-icon {
          width: 48px;
          height: 48px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 24px;
          color: white;
          margin-bottom: 16px;
        }
        
        &.consistent .metric-icon { background: linear-gradient(135deg, #10b981, #34d399); }
        &.inconsistent .metric-icon { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
        &.missing .metric-icon { background: linear-gradient(135deg, #ef4444, #f87171); }
        &.extra .metric-icon { background: linear-gradient(135deg, #6b7280, #9ca3af); }
        
        .metric-content {
          .metric-value {
            font-size: 32px;
            font-weight: 700;
            color: #1f2937;
            line-height: 1;
            margin-bottom: 8px;
          }
          
          .metric-label {
            font-size: 14px;
            color: #6b7280;
            margin-bottom: 4px;
          }
          
          .metric-percentage {
            font-size: 16px;
            font-weight: 600;
            color: #374151;
          }
        }
        
        .metric-trend {
          position: absolute;
          top: 20px;
          right: 20px;
          display: flex;
          align-items: center;
          font-size: 12px;
          font-weight: 600;
          
          .el-icon {
            margin-right: 4px;
          }
          
          &.trend-up {
            color: #ef4444;
          }
          
          &.trend-down {
            color: #10b981;
          }
          
          &.trend-stable {
            color: #6b7280;
          }
        }
      }
    }
  }
  
  .results-section {
    background: white;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    
    .results-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      
      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: #1f2937;
        display: flex;
        align-items: center;
        
        .el-icon {
          margin-right: 8px;
          color: #8b5cf6;
        }
      }
      
      .results-actions {
        display: flex;
        align-items: center;
        gap: 12px;
      }
    }
    
    .results-by-category {
      .category-collapse {
        margin-bottom: 16px;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        overflow: hidden;
        
        :deep(.el-collapse-item__header) {
          background: #f9fafb;
          padding: 16px 20px;
          border: none;
        }
        
        :deep(.el-collapse-item__content) {
          padding: 0;
        }
        
        .category-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          width: 100%;
          
          .category-info {
            display: flex;
            align-items: center;
            gap: 12px;
            
            .category-icon {
              color: #3b82f6;
              font-size: 18px;
            }
            
            .category-name {
              font-weight: 600;
              color: #1f2937;
            }
          }
          
          .category-stats {
            display: flex;
            gap: 8px;
          }
        }
      }
    }
  }
  
  .task-name-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .task-name {
      flex: 1;
      font-weight: 500;
    }
    
    .latest-tag {
      margin-left: 8px;
    }
  }
  
  .diff-count {
    display: flex;
    flex-direction: column;
    gap: 2px;
    font-size: 12px;
    
    .total {
      font-weight: 600;
      color: #374151;
    }
    
    .high {
      color: #ef4444;
    }
    
    .medium {
      color: #f59e0b;
    }
    
    .low {
      color: #10b981;
    }
  }
  
  .score-excellent {
    color: #10b981;
    font-weight: 600;
  }
  
  .score-good {
    color: #84cc16;
    font-weight: 600;
  }
  
  .score-warning {
    color: #f59e0b;
    font-weight: 600;
  }
  
  .score-danger {
    color: #ef4444;
    font-weight: 600;
  }
  
  .value-cell {
    .value-text {
      font-family: 'Consolas', 'Monaco', monospace;
      font-size: 13px;
    }
    
    .empty-value {
      color: #9ca3af;
      font-style: italic;
    }
    
    &.baseline .value-text {
      color: #3b82f6;
    }
    
    &.actual .value-text {
      color: #10b981;
    }
  }
  
  .status-tag {
    .status-icon {
      margin-right: 4px;
    }
  }
  
  .no-system-selected {
    background: white;
    border-radius: 12px;
    padding: 60px;
    text-align: center;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    
    .empty-actions {
      margin-top: 20px;
    }
  }
  
  // 差异分析对话框样式
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

// 正式报告对话框样式 - A4报告尺寸优化
.formal-report-dialog {
  :deep(.el-dialog) {
    border-radius: 16px;
    overflow: hidden;
    background: #f8fafc;
    // A4比例: 210mm × 297mm，转换为像素比例约为 1:1.414
    // 设置最大高度以适应A4比例
    max-height: calc(1200px * 1.414);
  }

  :deep(.el-dialog__body) {
    padding: 0;
    // 适应A4高度，留出底部按钮空间
    max-height: calc(100vh - 120px);
    overflow-y: auto;
    // 设置A4报告的宽高比
    aspect-ratio: 210/297;
  }

  :deep(.el-dialog__footer) {
    padding: 20px 30px;
    background: #ffffff;
    border-top: 1px solid #e5e7eb;
  }
}

.formal-report {
  // A4报告容器设置
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  
  .report-header-section {
    background: linear-gradient(135deg, #1e40af 0%, #3730a3 100%);
    color: white;
    padding: 30px 40px;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="80" cy="80" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="1.5" fill="rgba(255,255,255,0.1)"/></svg>');
      opacity: 0.3;
    }

    .report-title-area {
      text-align: center;
      margin-bottom: 30px;
      position: relative;
      z-index: 1;

      .main-title {
        margin: 0 0 12px 0;
        font-size: 32px;
        font-weight: 700;
        letter-spacing: 1px;
      }

      .report-id {
        font-size: 14px;
        opacity: 0.8;
        font-family: 'Consolas', 'Monaco', monospace;
      }
    }

    .report-meta-info {
      background: rgba(255, 255, 255, 0.1);
      border-radius: 12px;
      padding: 20px;
      backdrop-filter: blur(10px);
      position: relative;
      z-index: 1;

      .meta-row {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12px;

        &:last-child {
          margin-bottom: 0;
        }

        .meta-item {
          .meta-label {
            font-size: 14px;
            opacity: 0.8;
            margin-right: 8px;
          }

          .meta-value {
            font-size: 14px;
            font-weight: 600;

            &.status-complete {
              color: #34d399;
            }
          }
        }
      }
    }
  }

  .section-title {
    font-size: 20px;
    font-weight: 600;
    color: #1f2937;
    margin: 0 0 24px 0;
    padding-bottom: 8px;
    border-bottom: 3px solid #3b82f6;
    display: inline-block;
  }

  .executive-summary-section {
    background: white;
    padding: 30px 40px;
    border-bottom: 1px solid #e5e7eb;

    .summary-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 20px;
      margin-bottom: 30px;

      .summary-item {
        background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
        border-radius: 12px;
        padding: 24px;
        text-align: center;
        border: 1px solid #e2e8f0;
        position: relative;

        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          height: 4px;
        }

        &.consistent::before { background: linear-gradient(90deg, #10b981, #34d399); }
        &.inconsistent::before { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
        &.missing::before { background: linear-gradient(90deg, #ef4444, #f87171); }
        &.extra::before { background: linear-gradient(90deg, #6b7280, #9ca3af); }

        .item-number {
          font-size: 36px;
          font-weight: 700;
          color: #1f2937;
          margin-bottom: 8px;
        }

        .item-label {
          font-size: 14px;
          color: #6b7280;
          margin-bottom: 4px;
        }

        .item-percent {
          font-size: 16px;
          font-weight: 600;
          color: #374151;
        }
      }
    }

    .summary-conclusion {
      background: #f0f9ff;
      border-left: 4px solid #0ea5e9;
      padding: 20px;
      border-radius: 0 8px 8px 0;

      p {
        margin: 0;
        line-height: 1.6;
        color: #0c4a6e;
        font-size: 14px;
      }
    }
  }

  .diff-analysis-section {
    background: white;
    padding: 30px 40px;
    border-bottom: 1px solid #e5e7eb;

    .diff-content-wrapper {
      .task-diff-section {
        margin-bottom: 30px;
        border: 1px solid #e5e7eb;
        border-radius: 12px;
        overflow: hidden;

        &:last-child {
          margin-bottom: 0;
        }

        .task-diff-header {
          background: #f8fafc;
          padding: 16px 20px;
          border-bottom: 1px solid #e5e7eb;

          .task-name {
            margin: 0 0 8px 0;
            font-size: 16px;
            font-weight: 600;
            color: #1f2937;
          }

          .task-meta {
            display: flex;
            gap: 16px;
            font-size: 12px;

            .server-info {
              color: #6b7280;
            }

            .diff-count {
              color: #ef4444;
              font-weight: 600;
            }
          }
        }

        .diff-details {
          .diff-table {
            // 确保表格单元格能够换行显示长文本
            :deep(.el-table__cell) {
              .cell {
                word-break: break-all;
                white-space: pre-wrap;
                line-height: 1.4;
              }
            }
            
            .value-code {
              // 基线值使用蓝色系背景
              background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
              padding: 6px 12px;
              border-radius: 6px;
              border-left: 3px solid #3b82f6;
              font-family: 'Consolas', 'Monaco', monospace;
              font-size: 12px;
              color: #1e40af;
              display: block;
              width: 100%;
              text-align: left;
              word-break: break-all;
              white-space: pre-wrap;
              line-height: 1.5;
              box-shadow: 0 1px 3px rgba(59, 130, 246, 0.1);
              transition: all 0.2s ease;
              box-sizing: border-box;
              
              &:hover {
                transform: translateY(-1px);
                box-shadow: 0 2px 6px rgba(59, 130, 246, 0.15);
              }
            }
            
            // 当前值使用绿色系背景
            .current-value-code {
              background: linear-gradient(135deg, #d1fae5 0%, #ecfdf5 100%);
              padding: 6px 12px;
              border-radius: 6px;
              border-left: 3px solid #10b981;
              font-family: 'Consolas', 'Monaco', monospace;
              font-size: 12px;
              color: #065f46;
              display: block;
              width: 100%;
              text-align: left;
              word-break: break-all;
              white-space: pre-wrap;
              line-height: 1.5;
              box-shadow: 0 1px 3px rgba(16, 185, 129, 0.1);
              transition: all 0.2s ease;
              box-sizing: border-box;
              
              &:hover {
                transform: translateY(-1px);
                box-shadow: 0 2px 6px rgba(16, 185, 129, 0.15);
              }
            }
          }
        }
      }
    }
  }

  .results-summary-section {
    background: white;
    padding: 30px 40px;
    border-bottom: 1px solid #e5e7eb;

    .summary-table {
      // 确保汇总表格也能正确处理长文本
      :deep(.el-table__cell) {
        .cell {
          word-break: break-all;
          white-space: pre-wrap;
          line-height: 1.4;
        }
      }
      
      .diff-stat {
        font-size: 12px;
        color: #6b7280;
      }

      .score-excellent, .score-good, .score-warning, .score-danger {
        font-weight: 600;
      }
    }
  }

  .conclusion-section {
    background: white;
    padding: 30px 40px;
    border-bottom: 1px solid #e5e7eb;

    .conclusion-content {
      .conclusion-item {
        display: flex;
        margin-bottom: 16px;
        align-items: flex-start;

        &:last-child {
          margin-bottom: 0;
        }

        .conclusion-number {
          width: 24px;
          height: 24px;
          background: #3b82f6;
          color: white;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          font-weight: 600;
          margin-right: 12px;
          flex-shrink: 0;
          margin-top: 2px;
        }

        .conclusion-text {
          flex: 1;
          line-height: 1.6;
          color: #374151;
          font-size: 14px;
        }
      }
    }
  }

  .report-footer {
    background: #f8fafc;
    padding: 20px 40px;
    text-align: center;

    .footer-info {
      .report-signature {
        font-size: 14px;
        color: #6b7280;
        margin-bottom: 8px;
      }

      .report-note {
        font-size: 12px;
        color: #9ca3af;
        font-style: italic;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .el-button {
    border-radius: 8px;
    font-weight: 600;
    padding: 12px 24px;
  }
}

// 表格行样式
:deep(.el-table) {
  // 全局表格单元格换行设置，确保所有表格都能正确处理长文本
  .el-table__cell {
    .cell {
      word-break: break-all;
      white-space: pre-wrap;
      line-height: 1.4;
      // 防止内容溢出
      max-width: 100%;
      // 确保内容左对齐
      text-align: left;
      justify-content: flex-start;
    }
  }
  
  .warning-row {
    background: #fef3cd;
  }

  .danger-row {
    background: #fee2e2;
  }

  .info-row {
    background: #f3f4f6;
  }
}

// 加载状态和空数据样式
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #6b7280;
  font-size: 14px;

  .el-icon {
    margin-right: 8px;
    font-size: 16px;
    color: #3b82f6;
  }
}

.empty-diff {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px;
  background: #f8fafc;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  color: #6b7280;
  font-size: 14px;

  .el-icon {
    margin-right: 8px;
    font-size: 18px;
    color: #10b981;
  }
}

.diff-details {
  .loading-container,
  .empty-diff {
    margin: 16px 0;
  }
  
  // 基线值样式
  .baseline-value {
    background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
    border-left: 3px solid #3b82f6;
    color: #1e40af;
    display: block;
    width: 100%;
    text-align: left;
    box-sizing: border-box;
  }
  
  // 当前值样式
  .current-value {
    background: linear-gradient(135deg, #d1fae5 0%, #ecfdf5 100%);
    border-left: 3px solid #10b981;
    color: #065f46;
    display: block;
    width: 100%;
    text-align: left;
    box-sizing: border-box;
  }
}
</style>
