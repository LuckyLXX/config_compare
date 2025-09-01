<template>
  <div class="compare-tasks">
    <div class="page-header">
      <h2 class="page-title">比对任务管理</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增比对任务
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
            <el-option label="触发执行" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择任务状态"
            clearable
            style="width: 120px"
          >
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
        <el-table-column prop="categoryName" label="配置分类" width="120" />
        <el-table-column prop="executeType" label="执行类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getExecuteTypeColor(row.executeType)">
              {{ getExecuteTypeText(row.executeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="autoExecute" label="自动执行" width="80">
          <template #default="{ row }">
            <el-tag :type="row.autoExecute ? 'success' : 'info'">
              {{ row.autoExecute ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastExecuteTime" label="最后执行时间" width="180">
          <template #default="{ row }">
            {{ row.lastExecuteTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleExecute(row)">
              执行
            </el-button>
            <el-button type="info" size="small" @click="handleViewResults(row)">
              结果
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
      :title="isEdit ? '编辑任务' : '新增任务'"
      width="800px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务名称" prop="taskName">
              <el-input v-model="form.taskName" placeholder="请输入任务名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属系统" prop="systemId">
              <el-select
                v-model="form.systemId"
                placeholder="请选择所属系统"
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
        
        <!-- 移除服务器类型选择，因为采集任务已经指定了服务器范围 -->
        <!-- <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="服务器类型" prop="serverTypeId">
              <el-select
                v-model="form.serverTypeId"
                placeholder="请选择服务器类型"
                style="width: 100%"
                @change="handleServerTypeChange"
              >
                <el-option
                  v-for="type in serverTypeList"
                  :key="type.id"
                  :label="type.typeName"
                  :value="type.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row> -->

        <el-form-item label="配置分类" prop="categoryId">
          <el-select
            v-model="form.categoryId"
            placeholder="请选择配置分类"
            style="width: 100%"
            @change="handleCategoryChange"
          >
            <el-option
              v-for="category in categoryList"
              :key="category.id"
              :label="category.categoryName"
              :value="category.id"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="指定基线">
              <el-select
                v-model="form.baselineId"
                placeholder="请选择基线（为空则使用默认基线）"
                style="width: 100%"
                clearable
              >
                <el-option
                  v-for="baseline in baselineList"
                  :key="baseline.id"
                  :label="baseline.baselineName"
                  :value="baseline.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联采集任务">
              <el-select
                v-model="form.collectTaskId"
                placeholder="请选择采集任务"
                style="width: 100%"
                clearable
              >
                <el-option
                  v-for="task in collectTaskList"
                  :key="task.id"
                  :label="task.taskName"
                  :value="task.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="执行类型" prop="executeType">
          <el-radio-group v-model="form.executeType">
            <el-radio :label="1">立即执行</el-radio>
            <el-radio :label="2">定时执行</el-radio>
            <el-radio :label="3">触发执行</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item 
          v-if="form.executeType === 2" 
          label="Cron表达式" 
          prop="cronExpression"
        >
          <el-input
            v-model="form.cronExpression"
            placeholder="请输入Cron表达式，如：0 0 2 * * ?"
          />
        </el-form-item>

        <el-form-item label="自动执行">
          <el-switch
            v-model="form.autoExecute"
            :active-value="1"
            :inactive-value="0"
          />
          <span style="margin-left: 10px; color: #999; font-size: 12px">
            采集任务完成后自动执行比对
          </span>
        </el-form-item>

        <!-- 目标服务器信息说明 -->
        <el-form-item label="目标服务器">
          <div style="color: #666; font-size: 14px; line-height: 1.5;">
            <p>✅ 目标服务器范围已由关联的采集任务确定，无需重复选择</p>
            <p>📋 系统将自动使用采集任务中指定的服务器进行配置比对</p>
            <p>💡 如需调整服务器范围，请修改对应的采集任务配置</p>
          </div>
        </el-form-item>

        <el-form-item label="比对规则">
          <div class="compare-rules-container">
            <!-- 通用文本比对规则 -->
            <div class="rule-config">
              <el-form-item label="比对方式">
                <el-radio-group v-model="form.compareMode">
                  <el-radio label="line_by_line">逐行比对</el-radio>
                  <el-radio label="smart_diff">智能比对</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-form-item label="忽略选项">
                <el-checkbox-group v-model="form.ignoreOptions">
                  <el-checkbox label="whitespace">忽略空白字符</el-checkbox>
                  <el-checkbox label="case">忽略大小写</el-checkbox>
                  <el-checkbox label="comments">忽略注释行</el-checkbox>
                  <el-checkbox label="timestamps">忽略时间戳</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              
              <el-form-item label="忽略行关键词">
                <el-input
                  v-model="form.ignoreLines"
                  placeholder="输入要忽略的行关键词，用逗号分隔（如：timestamp,version）"
                />
                <div style="margin-top: 5px; color: #999; font-size: 12px">
                  支持关键词匹配，将忽略包含这些关键词的行
                </div>
              </el-form-item>
            </div>

            <!-- 通用比对选项 -->
            <div class="common-options">
              <el-divider content-position="left">通用比对选项</el-divider>
              <el-form-item label="差异阈值">
                <el-input-number
                  v-model="form.diffThreshold"
                  :min="0"
                  :max="100"
                  :precision="2"
                  placeholder="差异百分比阈值"
                />
                <span style="margin-left: 10px; color: #999;">%</span>
                <div style="margin-top: 5px; color: #999; font-size: 12px">
                  当差异超过此阈值时，任务状态将标记为不一致
                </div>
              </el-form-item>
              
              <el-form-item label="比对说明">
                <div style="color: #666; font-size: 13px; line-height: 1.5;">
                  <p>• 系统将自动识别文本内容的差异，支持左右对比显示</p>
                  <p>• 差异项将按严重程度分类：高（配置关键项）、中（一般配置）、低（注释等）</p>
                  <p>• 比对结果将提供详细的差异分析，包括新增、删除、修改的行</p>
                </div>
              </el-form-item>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="任务描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
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
import { useRouter } from 'vue-router'
import { compareTaskApi } from '@/api/compare'
import { systemApi, serverTypeApi, serverInstanceApi } from '@/api/system'
import { collectTaskApi } from '@/api/collect'
import { baselineApi, categoryApi } from '@/api/baseline'

export default {
  name: 'CompareTasks',
  setup() {
    const router = useRouter()
    
    // 响应式数据
    const loading = ref(false)
    const submitLoading = ref(false)
    const taskList = ref([])
    const systemList = ref([])
    const serverTypeList = ref([])
    const categoryList = ref([])
    const baselineList = ref([])
    const collectTaskList = ref([])
    const targetServerList = ref([])
    const selectedRows = ref([])
    
    // 对话框状态
    const dialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    
    // 搜索表单
    const searchForm = reactive({
      taskName: '',
      systemId: null,
      executeType: null,
      status: null
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
      systemId: null,
      // 移除serverTypeId，因为采集任务已经指定了服务器范围
      categoryId: null,
      baselineId: null,
      // 移除targetServerIds，因为采集任务已经指定了服务器范围
      collectTaskId: null,
      // 比对规则相关字段
      compareMode: 'line_by_line',
      ignoreOptions: ['whitespace', 'case', 'comments', 'timestamps'],
      ignoreLines: '',
      diffThreshold: 5.0,
      executeType: 1,
      cronExpression: '',
      autoExecute: 0,
      description: '',
      status: 1
    })
    
    // 表单验证规则
    const rules = {
      taskName: [
        { required: true, message: '请输入任务名称', trigger: 'blur' }
      ],
      systemId: [
        { required: true, message: '请选择系统', trigger: 'change' }
      ],
      // 移除serverTypeId验证，因为采集任务已经指定了服务器范围
      categoryId: [
        { required: true, message: '请选择配置分类', trigger: 'change' }
      ],
      executeType: [
        { required: true, message: '请选择执行类型', trigger: 'change' }
      ],
      cronExpression: [
        { 
          required: true, 
          message: '请输入Cron表达式', 
          trigger: 'blur',
          validator: (rule, value, callback) => {
            if (form.executeType === 2 && !value) {
              callback(new Error('定时执行必须输入Cron表达式'))
            } else {
              callback()
            }
          }
        }
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
        const response = await compareTaskApi.getTaskList(params)
        taskList.value = response.records || []
        pagination.total = response.total || 0
      } catch (error) {
        console.error('获取任务列表失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 获取系统列表
    const getSystemList = async () => {
      try {
        const response = await systemApi.getSystemList({ current: 1, size: 1000 })
        systemList.value = response.data || []
      } catch (error) {
        console.error('获取系统列表失败:', error)
      }
    }
    
    // 移除getServerTypeList函数，因为不再需要服务器类型选择
    // const getServerTypeList = async () => {
    //   try {
    //     const response = await serverTypeApi.getServerTypeList()
    //     serverTypeList.value = response.data || []
    //   } catch (error) {
    //     console.error('获取服务器类型列表失败:', error)
    //   }
    // }
    
    // 获取采集任务列表
    const getCollectTaskList = async () => {
      try {
        const response = await collectTaskApi.getTaskList({ current: 1, size: 200 })
        collectTaskList.value = response.data?.records || response.records || []
      } catch (error) {
        console.error('获取采集任务列表失败:', error)
      }
    }

    // 获取配置分类列表（按系统）
    const getCategoryList = async (systemId) => {
      try {
        if (!systemId) { 
          categoryList.value = []; 
          console.log('清空配置分类列表'); 
          return 
        }
        console.log('开始获取配置分类，systemId:', systemId);
        
        // 临时解决方案：先获取所有启用的配置分类
        // 后续可以根据实际业务需求调整API
        const res = await categoryApi.getCategoryList()
        console.log('配置分类API响应:', res);
        
        // 确保数据结构正确
        const categories = res.data || res || [];
        console.log('解析后的配置分类数据:', categories);
        
        // 强制更新响应式数据
        categoryList.value = [...categories];
        console.log('更新后的categoryList:', categoryList.value);
      } catch (e) {
        console.error('获取配置分类失败:', e)
        categoryList.value = []
      }
    }

    // 获取基线列表（按系统/分类）
    const getBaselineList = async (systemId, categoryId) => {
      try {
        if (!systemId || !categoryId) { baselineList.value = []; return }
        
        // 临时解决方案：使用现有的API获取基线
        // 后续可以根据实际业务需求调整API
        const res = await baselineApi.getBaselineList({
          systemId: systemId,
          categoryId: categoryId,
          current: 1,
          size: 100
        })
        baselineList.value = res.data?.records || res.records || []
      } catch (e) {
        console.error('获取基线列表失败:', e)
        baselineList.value = []
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
        executeType: null,
        status: null
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
    
    const handleEdit = (row) => {
      isEdit.value = true
      Object.assign(form, {
        ...row
        // 移除targetServerIds处理，因为采集任务已经指定了服务器范围
      })
      dialogVisible.value = true
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
        
        await compareTaskApi.deleteTask(row.id)
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
        await compareTaskApi.executeTask(row.id)
        ElMessage.success('任务已开始执行')
        getTaskList()
      } catch (error) {
        console.error('执行任务失败:', error)
      }
    }
    
    const handleBatchExecute = async () => {
      try {
        const taskIds = selectedRows.value.map(row => row.id)
        await compareTaskApi.batchExecute(taskIds)
        ElMessage.success('批量执行已开始')
        getTaskList()
      } catch (error) {
        console.error('批量执行失败:', error)
      }
    }
    
    const handleStatusChange = async (row) => {
      try {
        await compareTaskApi.toggleTaskStatus(row.id, row.status)
        ElMessage.success('状态更新成功')
      } catch (error) {
        console.error('状态更新失败:', error)
        // 恢复原状态
        row.status = row.status === 1 ? 0 : 1
      }
    }
    
    const handleViewResults = (row) => {
      // 跳转到比对结果页面
      router.push(`/compare/results?taskId=${row.id}`)
    }
    
    const handleSystemChange = async (systemId) => {
      console.log('系统变化，systemId:', systemId);
      // 移除serverTypeId，因为采集任务已经指定了服务器范围
      form.categoryId = null
      // 移除targetServerIds，因为采集任务已经指定了服务器范围
      baselineList.value = []
      categoryList.value = []
      
      // 直接加载配置分类列表（根据系统过滤）
      if (systemId) {
        console.log('开始加载配置分类列表...');
        await getCategoryListBySystem(systemId)
        console.log('配置分类列表加载完成');
      }
    }
    
    // 移除handleServerTypeChange函数，因为不再需要服务器类型选择
    // const handleServerTypeChange = async (typeId) => {
    //   console.log('服务器类型变化，typeId:', typeId);
    //   form.targetServerIds = []
    //   if (form.systemId && typeId) {
    //     console.log('开始加载相关数据...');
    //     await getTargetServerList(form.systemId, typeId)
    //     await getCategoryList(typeId)
    //     console.log('数据加载完成');
    //   } else {
    //     console.log('清空相关数据');
    //     targetServerList.value = []
    //     categoryList.value = []
    //   }
    //   // 清空并尝试加载基线（需要分类）
    //     baselineList.value = []
    // }

    // 分类选择变化时加载基线
    const handleCategoryChange = async (categoryId) => {
      await getBaselineList(form.systemId, categoryId)
    }

    // 比对类型变化处理
    const handleCompareTypeChange = (compareType) => {
      console.log('比对类型变化:', compareType)
      // 根据比对类型设置默认值
      switch (compareType) {
        case 'ssh_text':
          form.sshCompareMode = 'line_by_line'
          form.ignoreLines = ''
          break
        case 'config_file':
          form.fileType = 'text'
          form.fileCompareOptions = ['ignore_whitespace']
          break
        case 'apollo_config':
          form.apolloCompareMode = 'key_value'
          form.ignoreFields = ''
          form.sensitiveFields = ''
          break
        case 'yaml_config':
          form.yamlCompareOptions = ['ignore_whitespace']
          break
        case 'env_var':
          form.envCompareMode = 'exact_match'
          form.envTemplate = ''
          break
        case 'custom':
          form.customRules = ''
          break
      }
    }
    
    const getTargetServerList = async (systemId, typeId) => {
      try {
        const response = await serverInstanceApi.getServersBySystemAndType(systemId, typeId)
        targetServerList.value = response.records || []
      } catch (error) {
        console.error('获取目标服务器列表失败:', error)
      }
    }
    
    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        
        submitLoading.value = true
        
        // 构建比对规则配置
        const compareRuleConfig = buildCompareRuleConfig()
        
                  const data = {
            ...form,
            // 移除targetServerIds，因为采集任务已经指定了服务器范围
            compareRules: JSON.stringify(compareRuleConfig)
          }
        
        if (isEdit.value) {
          await compareTaskApi.updateTask(data)
          ElMessage.success('更新成功')
        } else {
          await compareTaskApi.createTask(data)
          ElMessage.success('创建成功')
        }
        
        dialogVisible.value = false
        getTaskList()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('提交失败: ' + (error.message || '未知错误'))
      } finally {
        submitLoading.value = false
      }
    }

    // 构建比对规则配置
    const buildCompareRuleConfig = () => {
      const config = {
        compareMode: form.compareMode,
        diffThreshold: form.diffThreshold,
        ignoreOptions: form.ignoreOptions,
        ignoreLines: form.ignoreLines
      }
      
      return config
    }
    
    const handleDialogClose = () => {
      resetForm()
      formRef.value?.clearValidate()
    }
    
    const resetForm = () => {
      Object.assign(form, {
        id: null,
        taskName: '',
        systemId: null,
        // 移除serverTypeId，因为采集任务已经指定了服务器范围
        categoryId: null,
        baselineId: null,
        // 移除targetServerIds，因为采集任务已经指定了服务器范围
        collectTaskId: null,
        // 比对规则相关字段
        compareMode: 'line_by_line',
        ignoreOptions: ['whitespace', 'case', 'comments', 'timestamps'],
        ignoreLines: '',
        diffThreshold: 5.0,
        executeType: 1,
        cronExpression: '',
        autoExecute: 0,
        description: '',
        status: 1
      })
    }
    
    const getExecuteTypeColor = (type) => {
      const colorMap = {
        1: 'warning',
        2: 'info',
        3: 'success'
      }
      return colorMap[type] || 'info'
    }
    
    const getExecuteTypeText = (type) => {
      const textMap = {
        1: '立即执行',
        2: '定时执行',
        3: '触发执行'
      }
      return textMap[type] || '未知'
    }
    
    // 初始化
    onMounted(async () => {
      await Promise.all([
        getTaskList(),
        getSystemList(),
        // 移除getServerTypeList，因为不再需要服务器类型选择
        getCollectTaskList()
      ])
    })
    
    return {
      loading,
      submitLoading,
      taskList,
      systemList,
      // 移除serverTypeList，因为不再需要服务器类型选择
      categoryList,
      baselineList,
      collectTaskList,
      // 移除targetServerList，因为不再需要用户选择目标服务器
      selectedRows,
      dialogVisible,
      isEdit,
      formRef,
      searchForm,
      pagination,
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
      handleViewResults,
      handleSystemChange,
      // 移除handleServerTypeChange，因为不再需要服务器类型选择
      handleCategoryChange,

      handleSubmit,
      handleDialogClose,
      getExecuteTypeColor,
      getExecuteTypeText
    }
  }
}
</script>

<style lang="scss" scoped>
.compare-tasks {
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
  
  /* 比对规则样式 */
  .compare-rules-container {
    border: 1px solid #e4e7ed;
    border-radius: 6px;
    padding: 20px;
    background: #fafafa;
  }
  
  .rule-config {
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 15px;
    margin: 15px 0;
    
    .el-form-item {
      margin-bottom: 15px;
    }
  }
  
  .common-options {
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 15px;
    margin-top: 20px;
    
    .el-divider {
      margin: 0 0 15px 0;
    }
    
    .el-form-item {
      margin-bottom: 15px;
    }
  }
}
</style>