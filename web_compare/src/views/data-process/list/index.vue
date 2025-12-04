<template>
  <div class="app-container">
    <el-card class="box-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="title">数据处理任务列表</span>
            <el-tag type="info" class="ml-2" effect="plain">待处理任务: {{ total }}</el-tag>
          </div>
          <div class="right-actions">
            <el-select
              v-model="queryParams.systemId"
              placeholder="所属系统"
              style="width: 160px"
              class="filter-item"
              clearable
              @change="handleQuery"
            >
              <el-option 
                v-for="item in systemList" 
                :key="item.id" 
                :label="item.systemName" 
                :value="item.id" 
              />
            </el-select>

            <el-input
              v-model="queryParams.taskName"
              placeholder="搜索任务名称"
              style="width: 240px"
              class="filter-item"
              clearable
              prefix-icon="Search"
              @keyup.enter="handleQuery"
            />
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </div>
        </div>
      </template>

      <el-table 
        :data="tableData" 
        border 
        style="width: 100%" 
        v-loading="loading"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column prop="executeId" label="执行ID" width="180" show-overflow-tooltip>
           <template #default="{ row }">
             <span class="mono-font">{{ row.executeId || '-' }}</span>
           </template>
        </el-table-column>
        <el-table-column prop="taskName" label="采集任务名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="task-name-cell">
              <span class="name">{{ row.taskName }}</span>
              <el-tag size="small" type="info" class="id-tag">#{{ row.taskId }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="systemName" label="所属系统" width="140">
           <template #default="{ row }">
            <el-tag v-if="row.systemName" effect="light">{{ row.systemName }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="collectType" label="采集来源" width="140">
           <template #default="{ row }">
            <el-tag v-if="row.collectType" :type="getSourceTypeTag(row.collectType)" effect="plain">
              <el-icon class="mr-1"><component :is="getSourceTypeIcon(row.collectType)" /></el-icon>
              {{ getSourceTypeLabel(row.collectType) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="dataSize" label="数据量" width="100" align="right">
          <template #default="{ row }">
            {{ formatDataSize(row.dataSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="collectTime" label="采集完成时间" width="180" sortable>
          <template #default="{ row }">
            {{ formatTime(row.collectTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" icon="MagicStick" @click="handleProcess(scope.row)">
              去处理
            </el-button>
            <el-button plain size="small" icon="View" @click="handleView(scope.row)">
              预览
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.current"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 源数据预览弹窗 -->
    <el-dialog v-model="previewVisible" title="源数据预览" width="70%" top="5vh">
      <div class="preview-meta mb-3">
         <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="任务名称">{{ currentPreviewRow?.taskName }}</el-descriptions-item>
            <el-descriptions-item label="数据类型">{{ currentPreviewRow?.sourceType }}</el-descriptions-item>
            <el-descriptions-item label="数据大小">{{ currentPreviewRow?.dataSize }}</el-descriptions-item>
         </el-descriptions>
      </div>
      <div class="json-preview">
        <pre>{{ previewData }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { dataProcessApi } from '@/api/dataProcess'
import { systemApi } from '@/api/system'

// 定义组件名，确保 keep-alive 正常工作
defineOptions({
  name: 'DataProcessList'
})

const router = useRouter()
const loading = ref(false)
const previewVisible = ref(false)
const previewData = ref('')
const currentPreviewRow = ref(null)
const total = ref(0)

const queryParams = reactive({
  current: 1,
  size: 10,
  taskName: '',
  systemId: null
})

const tableData = ref([])
const systemList = ref([])

// 页面加载时获取数据
onMounted(() => {
  fetchSystemList()
  fetchData()
})

// 页面从缓存恢复时刷新数据
onActivated(() => {
  fetchData()
})

// 获取系统列表
const fetchSystemList = async () => {
  try {
    const res = await systemApi.getAllSystemList()
    if (res.code === 200 && res.data) {
      systemList.value = res.data
    }
  } catch (e) {
    console.error('获取系统列表失败', e)
  }
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await dataProcessApi.getTaskList(queryParams)
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取数据失败')
    }
  } catch (e) {
    console.error('获取数据处理任务列表失败', e)
    ElMessage.error('获取数据失败：' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const map = {
    pending: 'primary',
    processed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status) => {
  const map = {
    pending: '待处理',
    processed: '已处理',
    failed: '处理失败'
  }
  return map[status] || status
}

const getSystemLabel = (val) => {
  const map = {
    trade: '核心交易系统',
    user: '用户管理中心',
    loan: '信贷审批系统'
  }
  return map[val] || val
}

const getSourceTypeLabel = (val) => {
  const map = {
    APOLLO: 'Apollo配置',
    DATABASE: '数据库',
    COMMAND: '主机命令',
    FILE: '文件采集',
    SSH: '主机文件/命令',
    API: '接口调用'
  }
  return map[val] || val
}

const getSourceTypeTag = (val) => {
  const map = {
    APOLLO: 'warning',
    DATABASE: 'success',
    COMMAND: 'info',
    FILE: 'info',
    SSH: 'info',
    API: 'primary'
  }
  return map[val] || ''
}

// 执行状态方法
const getExecuteStatusType = (status) => {
  const map = {
    0: 'info',      // 等待中
    1: 'primary',   // 执行中
    2: 'success',   // 成功
    3: 'danger'     // 失败
  }
  return map[status] || 'info'
}

const getExecuteStatusLabel = (status) => {
  const map = {
    0: '等待中',
    1: '执行中',
    2: '执行成功',
    3: '执行失败'
  }
  return map[status] || '未知'
}

// 格式化数据大小
const formatDataSize = (bytes) => {
  if (!bytes) return '-'
  if (typeof bytes === 'string') return bytes
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time === 'string') {
    // 处理 ISO 格式时间
    return time.replace('T', ' ').substring(0, 19)
  }
  return time
}

const getSourceTypeIcon = (val) => {
  const map = {
    APOLLO: 'Coin',
    DATABASE: 'DataBoard',
    COMMAND: 'Monitor',
    FILE: 'Document',
    SSH: 'Platform',
    API: 'Link'
  }
  return map[val] || 'Document'
}

const handleQuery = () => {
  queryParams.current = 1
  fetchData()
}

const resetQuery = () => {
  queryParams.taskName = ''
  queryParams.systemId = null
  queryParams.current = 1
  fetchData()
}

const handleProcess = (row) => {
  router.push({
    name: 'DataProcessWorkbench',
    query: {
      taskId: row.taskId,
      executeId: row.executeId,
      taskName: row.taskName,
      sourceType: row.collectType
    }
  })
}

const handleView = (row) => {
  currentPreviewRow.value = row
  try {
    const json = JSON.parse(row.content)
    previewData.value = JSON.stringify(json, null, 2)
  } catch (e) {
    previewData.value = row.content
  }
  previewVisible.value = true
}

const handleSizeChange = (val) => {
  queryParams.size = val
  fetchData()
}

const handleCurrentChange = (val) => {
  queryParams.current = val
  fetchData()
}
</script>

<style scoped>
.app-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 84px);
}
.box-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
}
.title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}
.right-actions {
  display: flex;
  gap: 12px;
}
.filter-item {
  width: 160px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.json-preview {
  background-color: #282c34;
  color: #abb2bf;
  padding: 15px;
  border-radius: 4px;
  max-height: 500px;
  overflow: auto;
}
pre {
  margin: 0;
  font-family: 'JetBrains Mono', Consolas, Monaco, monospace;
  font-size: 13px;
  line-height: 1.5;
}
.task-name-cell {
  display: flex;
  flex-direction: column;
}
.task-name-cell .name {
  font-weight: 500;
}
.task-name-cell .id-tag {
  width: fit-content;
  margin-top: 4px;
  font-size: 10px;
  transform: scale(0.9);
  transform-origin: left;
}
.ml-2 {
  margin-left: 8px;
}
.mr-1 {
  margin-right: 4px;
}
.mb-3 {
  margin-bottom: 12px;
}
.mono-font {
  font-family: 'JetBrains Mono', Consolas, monospace;
}
</style>
