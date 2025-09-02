<template>
  <div class="collect-tasks">
    <div class="page-header">
      <h2 class="page-title">采集任务管理</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增任务
        </el-button>
        <el-button 
          type="success" 
          :disabled="selectedRows.length === 0"
          @click="handleBatchExecute"
        >
          <el-icon><VideoPlay /></el-icon>
          批量执行
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
        <el-form-item label="执行类型">
          <el-select
            v-model="searchForm.executeType"
            placeholder="请选择执行类型"
            clearable
            style="width: 200px"
          >
            <el-option label="立即执行" :value="1" />
            <el-option label="定时执行" :value="2" />
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
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" />
        <el-table-column prop="systemName" label="所属系统" width="120" />
        <el-table-column prop="templateName" label="采集模板" width="120" />
        <el-table-column prop="executeType" label="执行类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.executeType === 1 ? 'warning' : 'info'">
              {{ row.executeType === 1 ? '立即执行' : '定时执行' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="任务状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="executeStatus" label="执行状态" width="120">
          <template #default="{ row }">
            <el-tag 
              :type="getExecuteStatusType(row.executeStatus)"
              :icon="getExecuteStatusIcon(row.executeStatus)"
            >
              {{ getExecuteStatusText(row.executeStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastExecuteTime" label="最后执行" width="160">
          <template #default="{ row }">
            <div v-if="row.lastExecuteTime">
              <div>{{ formatDateTime(row.lastExecuteTime) }}</div>
              <div class="text-sm text-gray-500" v-if="row.executeStatus === 1">
                {{ row.successServers || 0 }}/{{ row.totalServers || 0 }} 成功
              </div>
            </div>
            <span v-else class="text-gray-400">未执行</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleExecute(row)">
              执行
            </el-button>
            <el-button 
              type="success" 
              size="small" 
              @click="handleViewResults(row)"
              :disabled="!row.lastExecuteTime"
            >
              查看结果
            </el-button>
            <el-button type="info" size="small" @click="handleViewHistory(row)">
              执行历史
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

    <!-- 新增/编辑任务对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑任务' : '新增任务'"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务名称" prop="taskName">
              <el-input
                v-model="form.taskName"
                placeholder="请输入任务名称"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属系统" prop="systemId">
              <el-select
                v-model="form.systemId"
                placeholder="请选择系统"
                style="width: 100%"
                @change="handleSystemChange"
              >
                <el-option
                  v-for="system in systemList"
                  :key="system.id"
                  :label="system.systemName"
                  :value="system.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采集模板" prop="templateId">
              <el-select
                v-model="form.templateId"
                placeholder="请选择采集模板"
                style="width: 100%"
              >
                <el-option
                  v-for="template in templateList"
                  :key="template.id"
                  :label="template.templateName"
                  :value="template.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行类型" prop="executeType">
              <el-radio-group v-model="form.executeType">
                <el-radio :label="1">立即执行</el-radio>
                <el-radio :label="2">定时执行</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item
          v-if="form.executeType === 2"
          label="Cron表达式"
          prop="cronExpression"
        >
          <div class="cron-input-group">
            <el-input
              v-model="form.cronExpression"
              placeholder="请输入Cron表达式，如：0 0 2 * * ?"
              maxlength="100"
              @blur="validateCronExpression"
            />
            <el-button 
              type="primary" 
              size="small" 
              @click="validateCronExpression"
              :loading="validatingCron"
            >
              验证
            </el-button>
            <el-button 
              type="info" 
              size="small" 
              @click="showCronHelper = true"
            >
              帮助
            </el-button>
          </div>
          <div class="field-tip">
            示例：0 0 2 * * ? (每天凌晨2点执行)
          </div>
          <div v-if="cronValidationResult" class="validation-result">
            <el-tag :type="cronValidationResult.valid ? 'success' : 'danger'">
              {{ cronValidationResult.message }}
            </el-tag>
            <div v-if="cronValidationResult.nextExecution" class="next-execution">
              下次执行时间：{{ cronValidationResult.nextExecution }}
            </div>
          </div>
        </el-form-item>

        <!-- 目标服务器选择 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="服务器类型" prop="serverTypeIds">
              <el-select
                v-model="form.serverTypeIds"
                placeholder="请选择服务器类型"
                multiple
                style="width: 100%"
                @change="handleServerTypeChange"
              >
                <el-option
                  v-for="serverType in serverTypeList"
                  :key="serverType.id"
                  :label="serverType.typeName"
                  :value="serverType.id"
                />
              </el-select>
              <div class="field-tip">
                选择服务器类型后，系统会自动获取该类型下的所有服务器实例
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务器实例" prop="serverInstanceIds">
              <el-select
                v-model="form.serverInstanceIds"
                placeholder="请选择服务器实例"
                multiple
                style="width: 100%"
                :disabled="serverInstanceList.length === 0"
              >
                <el-option
                  v-for="server in serverInstanceList"
                  :key="server.id"
                  :label="`${server.instanceName} (${server.serverIp})`"
                  :value="server.id"
                />
              </el-select>
              <div class="field-tip">
                可以直接选择具体的服务器实例，优先级高于服务器类型
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="最大并发数">
              <el-input-number
                v-model="form.maxConcurrency"
                :min="1"
                :max="20"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时时间(秒)">
              <el-input-number
                v-model="form.timeoutSeconds"
                :min="60"
                :max="3600"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重试次数">
              <el-input-number
                v-model="form.retryCount"
                :min="0"
                :max="5"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="任务描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" @click="handleSave">
            {{ isEdit ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Cron表达式帮助对话框 -->
    <el-dialog
      v-model="showCronHelper"
      title="Cron表达式帮助"
      width="600px"
    >
      <div class="cron-helper">
        <h4>Cron表达式格式说明</h4>
        <p>Cron表达式由6个字段组成：<code>秒 分 时 日 月 周</code></p>
        
        <el-table :data="cronExamples" border style="width: 100%">
          <el-table-column prop="expression" label="表达式" width="150" />
          <el-table-column prop="description" label="说明" />
        </el-table>
        
        <h4 style="margin-top: 20px;">字段说明</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="秒">0-59</el-descriptions-item>
          <el-descriptions-item label="分">0-59</el-descriptions-item>
          <el-descriptions-item label="时">0-23</el-descriptions-item>
          <el-descriptions-item label="日">1-31</el-descriptions-item>
          <el-descriptions-item label="月">1-12</el-descriptions-item>
          <el-descriptions-item label="周">0-7 (0和7都表示周日)</el-descriptions-item>
        </el-descriptions>
        
        <h4 style="margin-top: 20px;">特殊字符</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="*">表示任意值</el-descriptions-item>
          <el-descriptions-item label="?">表示不指定值（仅用于日和周）</el-descriptions-item>
          <el-descriptions-item label="-">表示范围，如：1-5</el-descriptions-item>
          <el-descriptions-item label="/">表示步长，如：*/5</el-descriptions-item>
          <el-descriptions-item label=",">表示列表，如：1,3,5</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCronHelper = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { collectTaskApi, collectTemplateApi } from '@/api/collect'
import { systemApi } from '@/api/system'

export default {
  name: 'CollectTasks',
  setup() {
    // 响应式数据
    const loading = ref(false)
    const taskList = ref([])
    const systemList = ref([])
    const selectedRows = ref([])
    const templateList = ref([])
    const serverTypeList = ref([])
    const serverInstanceList = ref([])
    
    // 对话框相关
    const dialogVisible = ref(false)
    const isEdit = ref(false)
    const showCronHelper = ref(false)
    const validatingCron = ref(false)
    const cronValidationResult = ref(null)
    
    // 任务表单
    const form = reactive({
      id: null,
      taskName: '',
      systemId: null,
      templateId: null,
      executeType: 1,
      cronExpression: '',
      serverTypeIds: [],
      serverInstanceIds: [],
      maxConcurrency: 5,
      timeoutSeconds: 300,
      retryCount: 2,
      status: 1,
      description: ''
    })
    
    // 搜索表单
    const searchForm = reactive({
      taskName: '',
      systemId: null,
      executeType: null
    })
    
    // 分页
    const pagination = reactive({
      current: 1,
      size: 20,
      total: 0
    })
    
    // Cron表达式示例
    const cronExamples = ref([
      { expression: '0 0 2 * * ?', description: '每天凌晨2点执行' },
      { expression: '0 30 9 * * ?', description: '每天上午9点30分执行' },
      { expression: '0 0 12 * * ?', description: '每天中午12点执行' },
      { expression: '0 0 18 * * ?', description: '每天下午6点执行' },
      { expression: '0 0 0 1 * ?', description: '每月1号0点执行' },
      { expression: '0 0 9 ? * MON-FRI', description: '工作日（周一到周五）上午9点执行' },
      { expression: '0 0 10 ? * SUN', description: '每周日上午10点执行' },
      { expression: '0 */30 * * * ?', description: '每30分钟执行一次' },
      { expression: '0 0 */2 * * ?', description: '每2小时执行一次' },
      { expression: '0 0 0 1 1 ?', description: '每年1月1日0点执行' }
    ])

    // 表单验证规则
    const rules = reactive({
      taskName: [
        { required: true, message: '请输入任务名称', trigger: 'blur' },
        { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
      ],
      systemId: [
        { required: true, message: '请选择所属系统', trigger: 'change' }
      ],
      templateId: [
        { required: true, message: '请选择采集模板', trigger: 'change' }
      ],
      executeType: [
        { required: true, message: '请选择执行类型', trigger: 'change' }
      ],
      cronExpression: [
        { 
          validator: (rule, value, callback) => {
            if (form.executeType === 2 && !value) {
              callback(new Error('定时执行需要输入Cron表达式'))
            } else {
              callback()
            }
          }, 
          trigger: 'blur' 
        }
      ],
      serverTypeIds: [
        {
          validator: (rule, value, callback) => {
            if ((!value || value.length === 0) && (!form.serverInstanceIds || form.serverInstanceIds.length === 0)) {
              callback(new Error('请选择服务器类型或服务器实例'))
            } else {
              callback()
            }
          },
          trigger: 'change'
        }
      ],
      serverInstanceIds: [
        {
          validator: (rule, value, callback) => {
            if ((!value || value.length === 0) && (!form.serverTypeIds || form.serverTypeIds.length === 0)) {
              callback(new Error('请选择服务器类型或服务器实例'))
            } else {
              callback()
            }
          },
          trigger: 'change'
        }
      ]
    })
    
    // 获取任务列表
    const getTaskList = async () => {
      loading.value = true
      try {
        const params = {
          ...searchForm,
          current: pagination.current,
          size: pagination.size
        }
        const response = await collectTaskApi.getTaskList(params)
        const data = response.data || response
        taskList.value = data.records || []
        pagination.total = data.total || 0
      } catch (error) {
        console.error('获取任务列表失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 获取系统列表
    const getSystemList = async () => {
      try {
        const response = await systemApi.getAllSystemList()
        // 系统列表API返回的是直接的List，不是分页格式
        systemList.value = response.data || []
        console.log('获取到的系统列表:', systemList.value) // 调试日志
      } catch (error) {
        console.error('获取系统列表失败:', error)
      }
    }
    
    // 获取模板列表
    const getTemplateList = async () => {
      try {
        const response = await collectTemplateApi.getTemplateList()
        // 模板列表API返回的是分页格式
        const data = response.data || response
        templateList.value = data.records || []
        console.log('获取到的模板列表:', templateList.value) // 调试日志
      } catch (error) {
        console.error('获取模板列表失败:', error)
      }
    }
    
    // 获取服务器类型列表
    const getServerTypeList = async (systemId) => {
      if (!systemId) {
        serverTypeList.value = []
        return
      }
      try {
        const response = await systemApi.getServerTypesBySystem(systemId)
        // 服务器类型API返回的是直接的List
        serverTypeList.value = response.data || []
        console.log('获取到的服务器类型列表:', serverTypeList.value) // 调试日志
      } catch (error) {
        console.error('获取服务器类型列表失败:', error)
        serverTypeList.value = []
      }
    }
    
    // 获取服务器实例列表
    const getServerInstanceList = async (systemId, serverTypeIds) => {
      if (!systemId) {
        serverInstanceList.value = []
        return
      }
      try {
        const params = {
          systemId,
          serverTypeIds: serverTypeIds && serverTypeIds.length > 0 ? serverTypeIds.join(',') : undefined
        }
        const response = await systemApi.getServerInstancesBySystem(params)
        // 服务器实例API返回的是直接的List
        serverInstanceList.value = response.data || []
        console.log('获取到的服务器实例列表:', serverInstanceList.value) // 调试日志
      } catch (error) {
        console.error('获取服务器实例列表失败:', error)
        serverInstanceList.value = []
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
        systemId: null,
        executeType: null
      })
      handleSearch()
    }
    
    const handleSelectionChange = (selection) => {
      selectedRows.value = selection
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
    
    const handleEdit = async (row) => {
      try {
        isEdit.value = true
        
        // 确保基础数据已加载
        if (systemList.value.length === 0) {
          await getSystemList()
        }
        if (templateList.value.length === 0) {
          await getTemplateList()
        }
        
        // 获取完整的任务详情
        const response = await collectTaskApi.getTaskById(row.id)
        const taskData = response.data || response
        
        // 设置表单数据
        Object.assign(form, {
          ...taskData,
          serverTypeIds: taskData.serverTypeIds ? taskData.serverTypeIds.split(',').map(Number) : [],
          serverInstanceIds: taskData.serverInstanceIds ? taskData.serverInstanceIds.split(',').map(Number) : []
        })
        
        // 加载相关的服务器类型和实例数据
        if (taskData.systemId) {
          await getServerTypeList(taskData.systemId)
          await getServerInstanceList(taskData.systemId, form.serverTypeIds)
        }
        
        console.log('编辑采集任务数据:', form)
        console.log('系统列表:', systemList.value)
        console.log('模板列表:', templateList.value)
        console.log('服务器类型列表:', serverTypeList.value)
        console.log('服务器实例列表:', serverInstanceList.value)
        
        dialogVisible.value = true
      } catch (error) {
        console.error('获取任务详情失败:', error)
        ElMessage.error('获取任务详情失败：' + (error.message || '未知错误'))
      }
    }
    
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除任务"${row.taskName}"吗？`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        await collectTaskApi.deleteTask(row.id)
        ElMessage.success('删除成功')
        getTaskList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
        }
      }
    }
    
    const handleExecute = async (row) => {
      try {
        await collectTaskApi.executeTask(row.id)
        ElMessage.success('任务已开始执行')
        getTaskList()
      } catch (error) {
        console.error('执行任务失败:', error)
      }
    }
    
    const handleBatchExecute = async () => {
      try {
        const taskIds = selectedRows.value.map(row => row.id)
        await collectTaskApi.batchExecute(taskIds)
        ElMessage.success('批量执行已开始')
        getTaskList()
      } catch (error) {
        console.error('批量执行失败:', error)
      }
    }
    
    const handleStatusChange = async (row) => {
      try {
        await collectTaskApi.toggleTaskStatus(row.id, row.status)
        ElMessage.success('状态更新成功')
      } catch (error) {
        console.error('状态更新失败:', error)
        // 恢复原状态
        row.status = row.status === 1 ? 0 : 1
      }
    }
    
    const handleViewHistory = (row) => {
      // 跳转到执行历史页面
      window.open(`/collect/executions?taskId=${row.id}`, '_blank')
    }
    
    const handleViewResults = (row) => {
      // 跳转到最新执行结果页面
      if (row.lastExecuteId) {
        window.open(`/collect/results?taskId=${row.id}&executeId=${row.lastExecuteId}`, '_blank')
      } else {
        ElMessage.warning('该任务尚未有执行记录')
      }
    }
    
    // 处理系统选择变化
    const handleSystemChange = async (systemId) => {
      form.serverTypeIds = []
      form.serverInstanceIds = []
      serverTypeList.value = []
      serverInstanceList.value = []
      
      if (systemId) {
        await getServerTypeList(systemId)
        await getServerInstanceList(systemId, [])
      }
    }
    
    // 处理服务器类型选择变化
    const handleServerTypeChange = async (serverTypeIds) => {
      form.serverInstanceIds = []
      if (form.systemId) {
        await getServerInstanceList(form.systemId, serverTypeIds)
      }
    }
    
    // 执行状态相关函数
    const getExecuteStatusType = (status) => {
      if (status === null || status === undefined) return 'info'
      const statusMap = {
        1: 'success',    // 成功
        2: 'warning',    // 部分成功
        3: 'danger',     // 失败
        4: 'info'        // 运行中
      }
      return statusMap[status] || 'info'
    }
    
    // Cron表达式验证
    const validateCronExpression = async () => {
      if (!form.cronExpression || form.executeType !== 2) {
        cronValidationResult.value = null
        return
      }
      
      validatingCron.value = true
      try {
        const response = await fetch('/api/schedule/validate-cron', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ cronExpression: form.cronExpression })
        })
        
        const result = await response.json()
        
        if (result.success) {
          cronValidationResult.value = {
            valid: result.data,
            message: result.data ? 'Cron表达式有效' : 'Cron表达式无效',
            nextExecution: null
          }
          
          // 如果有效，获取下次执行时间
          if (result.data) {
            try {
              const nextResponse = await fetch('/api/schedule/next-execution', {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/json'
                },
                body: JSON.stringify({ cronExpression: form.cronExpression })
              })
              
              const nextResult = await nextResponse.json()
              if (nextResult.success) {
                cronValidationResult.value.nextExecution = nextResult.data
              }
            } catch (error) {
              console.error('获取下次执行时间失败:', error)
            }
          }
        } else {
          cronValidationResult.value = {
            valid: false,
            message: result.message || '验证失败',
            nextExecution: null
          }
        }
      } catch (error) {
        console.error('验证Cron表达式失败:', error)
        cronValidationResult.value = {
          valid: false,
          message: '验证失败：' + (error.message || '未知错误'),
          nextExecution: null
        }
      } finally {
        validatingCron.value = false
      }
    }

    const getExecuteStatusIcon = (status) => {
      const iconMap = {
        1: 'Check',      // 成功
        2: 'Warning',    // 部分成功
        3: 'Close',      // 失败
        4: 'Loading'     // 运行中
      }
      return iconMap[status] || ''
    }
    
    const getExecuteStatusText = (status) => {
      if (status === null || status === undefined) return '未执行'
      const textMap = {
        1: '成功',
        2: '部分成功',
        3: '失败',
        4: '运行中'
      }
      return textMap[status] || '未知'
    }
    
    // 时间格式化函数
    const formatDateTime = (dateTime) => {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      const now = new Date()
      const diff = now - date
      
      // 如果是今天
      if (diff < 24 * 60 * 60 * 1000 && date.getDate() === now.getDate()) {
        return date.toLocaleTimeString('zh-CN', { 
          hour: '2-digit', 
          minute: '2-digit',
          second: '2-digit'
        })
      }
      
      // 如果是近7天
      if (diff < 7 * 24 * 60 * 60 * 1000) {
        const days = Math.floor(diff / (24 * 60 * 60 * 1000))
        return `${days}天前`
      }
      
      // 其他情况显示完整日期
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    // 重置表单
    const resetForm = () => {
      Object.assign(form, {
        id: null,
        taskName: '',
        systemId: null,
        templateId: null,
        executeType: 1,
        cronExpression: '',
        serverTypeIds: [],
        serverInstanceIds: [],
        maxConcurrency: 5,
        timeoutSeconds: 300,
        retryCount: 2,
        status: 1,
        description: ''
      })
    }
    
    // 保存任务
    const handleSave = async () => {
      try {
        const taskData = {
          ...form,
          serverTypeIds: form.serverTypeIds.length > 0 ? form.serverTypeIds.join(',') : null,
          serverInstanceIds: form.serverInstanceIds.length > 0 ? form.serverInstanceIds.join(',') : null
        }
        
        if (isEdit.value) {
          await collectTaskApi.updateTask(taskData)
          ElMessage.success('更新成功')
        } else {
          await collectTaskApi.createTask(taskData)
          ElMessage.success('创建成功')
        }
        
        dialogVisible.value = false
        getTaskList()
      } catch (error) {
        console.error('保存任务失败:', error)
        ElMessage.error('保存失败：' + (error.message || '未知错误'))
      }
    }
    
    // 取消操作
    const handleCancel = () => {
      dialogVisible.value = false
      resetForm()
    }
    
    // 初始化
    onMounted(async () => {
      await Promise.all([
        getTaskList(),
        getSystemList(),
        getTemplateList()
      ])
    })
    
    return {
      loading,
      taskList,
      systemList,
      selectedRows,
      templateList,
      serverTypeList,
      serverInstanceList,
      searchForm,
      pagination,
      dialogVisible,
      isEdit,
      showCronHelper,
      validatingCron,
      cronValidationResult,
      cronExamples,
      form,
      rules,
      handleSearch,
      handleReset,
      handleSelectionChange,
      handleSizeChange,
      handleCurrentChange,
      handleCreate,
      handleEdit,
      handleDelete,
      handleExecute,
      handleBatchExecute,
      handleStatusChange,
      handleViewHistory,
      handleViewResults,
      handleSystemChange,
      handleServerTypeChange,
      validateCronExpression,
      getExecuteStatusType,
      getExecuteStatusIcon,
      getExecuteStatusText,
      formatDateTime,
      resetForm,
      handleSave,
      handleCancel
    }
  }
}
</script>

<style lang="scss" scoped>
.collect-tasks {
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
}

.field-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.4;
}

.dialog-footer {
  text-align: right;
}

.cron-input-group {
  display: flex;
  gap: 8px;
  align-items: center;
  
  .el-input {
    flex: 1;
  }
}

.validation-result {
  margin-top: 8px;
  
  .next-execution {
    font-size: 12px;
    color: #666;
    margin-top: 4px;
  }
}

.cron-helper {
  h4 {
    margin: 16px 0 8px 0;
    color: #333;
  }
  
  p {
    margin: 8px 0;
    color: #666;
  }
  
  code {
    background: #f5f5f5;
    padding: 2px 4px;
    border-radius: 3px;
    font-family: 'Courier New', monospace;
  }
}
</style>