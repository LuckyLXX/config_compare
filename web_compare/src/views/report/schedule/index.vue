<template>
  <div class="task-schedule">
    <div class="page-header">
      <h2 class="page-title">任务调度管理</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增调度任务
        </el-button>
        <el-button type="info" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon total">
              <el-icon><Collection /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">总任务数</div>
              <div class="stat-value">{{ statistics.totalTasks || 0 }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon active">
              <el-icon><VideoPlay /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">活跃任务</div>
              <div class="stat-value">{{ statistics.activeTasks || 0 }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon success">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">今日成功</div>
              <div class="stat-value">{{ statistics.todaySuccess || 0 }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon failed">
              <el-icon><CircleClose /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">今日失败</div>
              <div class="stat-value">{{ statistics.todayFailed || 0 }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
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
        <el-form-item label="任务类型">
          <el-select
            v-model="searchForm.taskType"
            placeholder="请选择任务类型"
            clearable
            style="width: 150px"
          >
            <el-option label="采集任务" value="COLLECT" />
            <el-option label="比对任务" value="COMPARE" />
            <el-option label="清理任务" value="CLEANUP" />
            <el-option label="报告任务" value="REPORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
            style="width: 120px"
          >
            <el-option label="运行中" value="RUNNING" />
            <el-option label="已暂停" value="PAUSED" />
            <el-option label="已停止" value="STOPPED" />
            <el-option label="错误" value="ERROR" />
          </el-select>
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

    <!-- 数据表格 -->
    <div class="app-card">
      <el-table
        v-loading="loading"
        :data="taskList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="taskName" label="任务名称" min-width="150" />
        <el-table-column prop="taskType" label="任务类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTaskTypeColor(row.taskType)" size="small">
              {{ getTaskTypeText(row.taskType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpression" label="Cron表达式" width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastExecuteTime" label="上次执行" width="180">
          <template #default="{ row }">
            {{ row.lastExecuteTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="nextExecuteTime" label="下次执行" width="180">
          <template #default="{ row }">
            {{ row.nextExecuteTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="executionCount" label="执行次数" width="100" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleExecute(row)"
              :disabled="row.status === 'RUNNING'"
            >
              执行
            </el-button>
            <el-button type="info" size="small" @click="handleViewHistory(row)">
              历史
            </el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; text-align: right"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑调度任务' : '新增调度任务'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="form.taskType" placeholder="请选择任务类型" style="width: 100%">
            <el-option label="采集任务" value="COLLECT" />
            <el-option label="比对任务" value="COMPARE" />
            <el-option label="清理任务" value="CLEANUP" />
            <el-option label="报告任务" value="REPORT" />
          </el-select>
        </el-form-item>

        <el-form-item label="Cron表达式" prop="cronExpression">
          <el-input
            v-model="form.cronExpression"
            placeholder="请输入Cron表达式，如：0 0 2 * * ?"
          />
        </el-form-item>

        <el-form-item label="任务参数">
          <el-input
            v-model="form.taskParams"
            type="textarea"
            :rows="3"
            placeholder="请输入JSON格式的任务参数"
          />
        </el-form-item>

        <el-form-item label="任务描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="请输入任务描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scheduleApi } from '@/api/schedule'

export default {
  name: 'TaskSchedule',
  setup() {
    // 响应式数据
    const loading = ref(false)
    const submitLoading = ref(false)
    const taskList = ref([])
    const statistics = ref({})
    
    // 对话框状态
    const dialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    
    // 搜索表单
    const searchForm = reactive({
      taskName: '',
      taskType: '',
      status: ''
    })
    
    // 分页
    const pagination = reactive({
      current: 1,
      size: 20,
      total: 0
    })
    
    // 表单数据
    const form = reactive({
      id: null,
      taskName: '',
      taskType: '',
      cronExpression: '',
      taskParams: '',
      description: ''
    })
    
    // 表单验证规则
    const rules = {
      taskName: [
        { required: true, message: '请输入任务名称', trigger: 'blur' }
      ],
      taskType: [
        { required: true, message: '请选择任务类型', trigger: 'change' }
      ],
      cronExpression: [
        { required: true, message: '请输入Cron表达式', trigger: 'blur' }
      ]
    }
    
    // 获取任务列表
    const getTaskList = async () => {
      loading.value = true
      try {
        const params = {
          ...searchForm,
          current: pagination.current,
          size: pagination.size
        }
        const response = await scheduleApi.getScheduleList(params)
        taskList.value = response.records || []
        pagination.total = response.total || 0
      } catch (error) {
        console.error('获取任务列表失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 获取统计信息
    const getStatistics = async () => {
      try {
        const response = await scheduleApi.getScheduleStatistics()
        statistics.value = response || {}
      } catch (error) {
        console.error('获取统计信息失败:', error)
      }
    }
    
    // 事件处理函数
    const handleSearch = () => {
      pagination.current = 1
      getTaskList()
    }
    
    const handleReset = () => {
      Object.assign(searchForm, {
        taskName: '',
        taskType: '',
        status: ''
      })
      handleSearch()
    }
    
    const handleRefresh = () => {
      getTaskList()
      getStatistics()
    }
    
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      getTaskList()
    }
    
    const handleCurrentChange = (current) => {
      pagination.current = current
      getTaskList()
    }
    
    const handleCreate = () => {
      isEdit.value = false
      resetForm()
      dialogVisible.value = true
    }
    
    const handleEdit = (row) => {
      isEdit.value = true
      Object.assign(form, row)
      dialogVisible.value = true
    }
    
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除调度任务"${row.taskName}"吗？`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        await scheduleApi.deleteScheduleTask(row.id)
        ElMessage.success('删除成功')
        getTaskList()
        getStatistics()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
        }
      }
    }
    
    const handleExecute = async (row) => {
      try {
        await scheduleApi.executeScheduleTask(row.id)
        ElMessage.success('任务已开始执行')
        getTaskList()
      } catch (error) {
        console.error('执行任务失败:', error)
      }
    }
    
    const handleViewHistory = (row) => {
      ElMessage.info('查看历史功能开发中...')
    }
    
    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        
        submitLoading.value = true
        
        if (isEdit.value) {
          await scheduleApi.updateScheduleTask(form)
          ElMessage.success('更新成功')
        } else {
          await scheduleApi.createScheduleTask(form)
          ElMessage.success('创建成功')
        }
        
        dialogVisible.value = false
        getTaskList()
        getStatistics()
      } catch (error) {
        console.error('提交失败:', error)
      } finally {
        submitLoading.value = false
      }
    }
    
    const handleDialogClose = () => {
      resetForm()
      formRef.value?.clearValidate()
    }
    
    const resetForm = () => {
      Object.assign(form, {
        id: null,
        taskName: '',
        taskType: '',
        cronExpression: '',
        taskParams: '',
        description: ''
      })
    }
    
    // 工具函数
    const getTaskTypeColor = (type) => {
      const colorMap = {
        'COLLECT': 'primary',
        'COMPARE': 'success',
        'CLEANUP': 'warning',
        'REPORT': 'info'
      }
      return colorMap[type] || 'info'
    }
    
    const getTaskTypeText = (type) => {
      const textMap = {
        'COLLECT': '采集',
        'COMPARE': '比对',
        'CLEANUP': '清理',
        'REPORT': '报告'
      }
      return textMap[type] || '未知'
    }
    
    const getStatusColor = (status) => {
      const colorMap = {
        'RUNNING': 'success',
        'PAUSED': 'warning',
        'STOPPED': 'info',
        'ERROR': 'danger'
      }
      return colorMap[status] || 'info'
    }
    
    const getStatusText = (status) => {
      const textMap = {
        'RUNNING': '运行中',
        'PAUSED': '已暂停',
        'STOPPED': '已停止',
        'ERROR': '错误'
      }
      return textMap[status] || '未知'
    }
    
    // 初始化
    onMounted(async () => {
      await Promise.all([
        getTaskList(),
        getStatistics()
      ])
    })
    
    return {
      loading,
      submitLoading,
      taskList,
      statistics,
      dialogVisible,
      isEdit,
      formRef,
      searchForm,
      pagination,
      form,
      rules,
      handleSearch,
      handleReset,
      handleRefresh,
      handleSizeChange,
      handleCurrentChange,
      handleCreate,
      handleEdit,
      handleDelete,
      handleExecute,
      handleViewHistory,
      handleSubmit,
      handleDialogClose,
      getTaskTypeColor,
      getTaskTypeText,
      getStatusColor,
      getStatusText
    }
  }
}
</script>

<style lang="scss" scoped>
.task-schedule {
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
  
  .stats-section {
    margin-bottom: 20px;
    
    .stat-card {
      background: #fff;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
      display: flex;
      align-items: center;
      
      .stat-icon {
        width: 50px;
        height: 50px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        color: #fff;
        margin-right: 15px;
        
        &.total { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
        &.active { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
        &.success { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
        &.failed { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
      }
      
      .stat-content {
        flex: 1;
        
        .stat-title {
          font-size: 14px;
          color: #909399;
          margin-bottom: 5px;
        }
        
        .stat-value {
          font-size: 24px;
          font-weight: bold;
          color: #303133;
        }
      }
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
}
</style>