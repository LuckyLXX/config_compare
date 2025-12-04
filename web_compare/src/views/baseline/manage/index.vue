<template>
  <div class="baseline-manage">
    <div class="page-header">
      <h2 class="page-title">基线版本管理</h2>
      <div class="page-actions">
        <el-button @click="handleRefresh" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增基线
        </el-button>
        <el-button type="success" @click="handleImport">
          <el-icon><Upload /></el-icon>
          导入基线
        </el-button>
      </div>
    </div>

    <!-- 基线列表 -->
    <div class="app-card">
      <!-- 筛选条件 -->
      <div class="filter-section">
        <el-form :inline="true" :model="filterForm" class="filter-form">
          <el-form-item label="系统">
            <el-select
              v-model="filterForm.systemId"
              placeholder="请选择系统"
              clearable
              style="width: 200px"
              @change="handleFilterChange"
            >
              <el-option
                v-for="system in systemList"
                :key="system.id"
                :label="system.systemName"
                :value="system.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="服务器类型">
            <el-select
              v-model="filterForm.serverTypeId"
              placeholder="请选择服务器类型"
              clearable
              style="width: 180px"
              @change="handleFilterChange"
            >
              <el-option
                v-for="type in serverTypeList"
                :key="type.id"
                :label="type.typeName"
                :value="type.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="配置分类">
            <el-select
              v-model="filterForm.categoryId"
              placeholder="请选择配置分类"
              clearable
              style="width: 180px"
              @change="handleFilterChange"
            >
              <el-option
                v-for="category in categoryList"
                :key="category.id"
                :label="category.categoryName"
                :value="category.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="基线名称">
            <el-input
              v-model="filterForm.baselineName"
              placeholder="搜索基线名称..."
              clearable
              style="width: 200px"
              @change="handleFilterChange"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleResetFilter">
              <el-icon><RefreshLeft /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="list-header">
        <h3>基线列表</h3>
        <div class="list-actions">
          <el-button v-if="selectedBaseline" type="warning" size="small" @click="handleSetDefault">
            设为默认
          </el-button>
          <el-button v-if="selectedBaseline" type="info" size="small" @click="handleVersionHistory">
            <el-icon><Clock /></el-icon>
            版本历史
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="baselineList"
        style="width: 100%"
        @row-click="handleRowClick"
        highlight-current-row
      >
        <el-table-column prop="baselineName" label="基线名称" min-width="150" />
        <el-table-column prop="baselineVersion" label="版本号" width="120" />
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="isDefault" label="默认版本" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isDefault ? 'success' : 'info'" size="small">
              {{ row.isDefault ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag 
              :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'info'" 
              size="small"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="createBy" label="创建人" width="100" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button link type="success" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link type="warning" size="small" @click="handleCopy(row)">
              <el-icon><DocumentCopy /></el-icon>
              复制
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
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

    <!-- 基线内容查看对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      :title="`查看基线 - ${currentBaseline?.baselineName}`"
      width="80%"
      @close="handleViewDialogClose"
    >
      <div class="baseline-info">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="基线名称">{{ currentBaseline?.baselineName }}</el-descriptions-item>
          <el-descriptions-item label="版本号">{{ currentBaseline?.baselineVersion }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ currentBaseline?.fileName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentBaseline?.status === 1 ? 'success' : 'warning'">
              {{ getStatusText(currentBaseline?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="是否默认">
            <el-tag :type="currentBaseline?.isDefault ? 'success' : 'info'">
              {{ currentBaseline?.isDefault ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentBaseline?.createTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <div class="content-area">
        <h4>配置内容</h4>
        <el-input
          v-model="currentBaseline.configContent"
          type="textarea"
          :rows="20"
          readonly
          class="content-viewer"
        />
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="viewDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleDownload">
            <el-icon><Download /></el-icon>
            下载
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 版本历史对话框 -->
    <VersionHistoryDialog
      v-model="versionHistoryVisible"
      :system-id="selectedBaseline?.systemId"
      :server-type-id="selectedBaseline?.serverTypeId"
      :category-id="selectedBaseline?.categoryId"
      :baseline-name="selectedBaseline?.baselineName"
      @version-switched="handleVersionSwitched"
    />

    <!-- 新增/编辑基线对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="isEdit ? '编辑基线' : '新增基线'"
      width="70%"
      @close="handleEditDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        class="app-form"
      >
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="基线名称" prop="baselineName">
              <el-input v-model="form.baselineName" placeholder="请输入基线名称" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-alert
          v-if="!isEdit"
          title="提示：版本号将自动生成（格式：V + 时间戳）"
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        />
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="所属系统" prop="systemId">
              <el-select v-model="form.systemId" placeholder="请选择系统" style="width: 100%">
                <el-option
                  v-for="system in systemList"
                  :key="system.id"
                  :label="system.systemName"
                  :value="system.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="服务器类型" prop="serverTypeId">
              <el-select v-model="form.serverTypeId" placeholder="请选择服务器类型" style="width: 100%">
                <el-option
                  v-for="type in serverTypeList"
                  :key="type.id"
                  :label="type.typeName"
                  :value="type.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="配置分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择配置分类" style="width: 100%">
                <el-option
                  v-for="category in categoryList"
                  :key="category.id"
                  :label="category.categoryName"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="文件上传">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".txt,.json,.xml,.properties,.yaml,.yml,.conf"
            @change="handleFileChange"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 txt、json、xml、properties、yaml 等格式文件
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="配置内容" prop="configContent">
          <el-input
            v-model="form.configContent"
            type="textarea"
            :rows="15"
            placeholder="请输入配置内容或上传文件"
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onActivated, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { baselineApi, categoryApi } from '@/api/baseline'
import { systemApi, serverTypeApi } from '@/api/system'
import VersionHistoryDialog from '@/components/baseline/VersionHistoryDialog.vue'

export default {
  name: 'BaselineManage',
  components: {
    VersionHistoryDialog
  },
  setup() {
    const loading = ref(false)
    const submitLoading = ref(false)
    const viewDialogVisible = ref(false)
    const editDialogVisible = ref(false)
    const versionHistoryVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref()
    const treeRef = ref()
    const uploadRef = ref()

    // 筛选条件
    const filterForm = reactive({
      systemId: null,
      serverTypeId: null,
      categoryId: null,
      baselineName: ''
    })

    // 分页信息
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })

    // 数据列表
    const systemList = ref([])
    const serverTypeList = ref([])
    const categoryList = ref([])
    const baselineList = ref([])
    const selectedBaseline = ref(null)
    const currentBaseline = ref({})

    // 表单数据
    const form = reactive({
      id: null,
      systemId: '',
      serverTypeId: '',
      categoryId: '',
      baselineName: '',
      fileName: '',
      configContent: '',
      description: ''
    })

    // 表单验证规则
    const rules = {
      baselineName: [
        { required: true, message: '请输入基线名称', trigger: 'blur' }
      ],
      systemId: [
        { required: true, message: '请选择系统', trigger: 'change' }
      ],
      serverTypeId: [
        { required: true, message: '请选择服务器类型', trigger: 'change' }
      ],
      categoryId: [
        { required: true, message: '请选择配置分类', trigger: 'change' }
      ],
      configContent: [
        { required: true, message: '请输入配置内容', trigger: 'blur' }
      ]
    }

    // 获取基础数据
    const fetchBasicData = async () => {
      try {
        // 获取系统列表
        console.log('开始获取系统列表...')
        const systemResponse = await systemApi.getAllSystemList()
        console.log('系统列表响应:', systemResponse)
        // 后端返回格式: Result.success("查询成功", result)，数据在data字段中
        systemList.value = systemResponse.data || []
        console.log('解析后的系统列表:', systemList.value)

        // 获取服务器类型列表
        console.log('开始获取服务器类型列表...')
        const serverTypeResponse = await serverTypeApi.getServerTypeList()
        console.log('服务器类型响应:', serverTypeResponse)
        serverTypeList.value = serverTypeResponse.data || []
        console.log('解析后的服务器类型列表:', serverTypeList.value)

        // 获取配置分类列表
        console.log('开始获取配置分类列表...')
        const categoryResponse = await categoryApi.getCategoryList()
        console.log('配置分类响应:', categoryResponse)
        categoryList.value = categoryResponse.data || []
        console.log('解析后的配置分类列表:', categoryList.value)
        
      } catch (error) {
        console.error('获取基础数据失败:', error)
        ElMessage.error('获取基础数据失败: ' + (error.message || '未知错误'))
      }
    }

    // 获取基线列表
    const fetchBaselineList = async () => {
      loading.value = true
      try {
        const params = {
          current: pagination.current,
          size: pagination.size
        }
        
        // 添加筛选条件
        if (filterForm.systemId) {
          params.systemId = filterForm.systemId
        }
        if (filterForm.serverTypeId) {
          params.serverTypeId = filterForm.serverTypeId
        }
        if (filterForm.categoryId) {
          params.categoryId = filterForm.categoryId
        }
        if (filterForm.baselineName) {
          params.baselineName = filterForm.baselineName
        }
        
        const response = await baselineApi.getBaselineList(params)
        // 过滤掉归档状态（status=2）的基线，只显示有效基线
        const allBaselines = response.data?.records || []
        baselineList.value = allBaselines.filter(baseline => baseline.status !== 2)
        pagination.total = response.data?.total || baselineList.value.length
      } catch (error) {
        console.error('获取基线列表失败:', error)
        ElMessage.error('获取基线列表失败')
      } finally {
        loading.value = false
      }
    }
    
    // 筛选条件变化
    const handleFilterChange = () => {
      pagination.current = 1
      fetchBaselineList()
    }

    // 查询
    const handleSearch = () => {
      pagination.current = 1
      fetchBaselineList()
    }

    // 重置筛选条件
    const handleResetFilter = () => {
      filterForm.systemId = null
      filterForm.serverTypeId = null
      filterForm.categoryId = null
      filterForm.baselineName = ''
      pagination.current = 1
      fetchBaselineList()
    }

    // 状态文本映射
    const getStatusText = (status) => {
      const statusMap = {
        0: '草稿',
        1: '生效',
        2: '归档'
      }
      return statusMap[status] || '未知'
    }

    // 表格行点击
    const handleRowClick = (row) => {
      selectedBaseline.value = row
    }

    // 查看基线
    const handleView = (row) => {
      currentBaseline.value = { ...row }
      viewDialogVisible.value = true
    }

    // 刷新基线列表
    const handleRefresh = async () => {
      await fetchBaselineList()
      ElMessage.success('刷新成功')
    }

    // 新增基线
    const handleAdd = () => {
      isEdit.value = false
      editDialogVisible.value = true
      resetForm()
    }

    // 编辑基线
    const handleEdit = async (row) => {
      isEdit.value = true
      editDialogVisible.value = true
      
      // 确保基础数据已加载
      if (systemList.value.length === 0 || serverTypeList.value.length === 0 || categoryList.value.length === 0) {
        await fetchBasicData()
      }
      
      // 设置表单数据
      Object.assign(form, row)
      
      console.log('编辑基线数据:', form)
      console.log('系统列表:', systemList.value)
      console.log('服务器类型列表:', serverTypeList.value)
      console.log('配置分类列表:', categoryList.value)
    }

    // 复制基线
    const handleCopy = (row) => {
      isEdit.value = false
      editDialogVisible.value = true
      Object.assign(form, {
        ...row,
        id: null,
        baselineName: `${row.baselineName}_copy`,
        baselineVersion: `${row.baselineVersion}_copy`,
        isDefault: 0
      })
    }

    // 删除基线
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要归档基线"${row.baselineName}"吗？归档后该基线将不会被删除，可在版本历史中查看和恢复。`,
          '归档确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        
        await baselineApi.deleteBaseline(row.id)
        ElMessage.success('归档成功')
        await fetchBaselineList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('归档基线失败:', error)
        }
      }
    }

    // 设为默认
    const handleSetDefault = async () => {
      if (!selectedBaseline.value) return
      
      try {
        await ElMessageBox.confirm(
          `确定要将"${selectedBaseline.value.baselineName}"设为默认版本吗？`,
          '设置确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        
        await baselineApi.setDefaultBaseline(selectedBaseline.value.id)
        ElMessage.success('设置成功')
        await fetchBaselineList()
      } catch (error) {
        // 用户取消
      }
    }

    // 版本历史
    const handleVersionHistory = () => {
      if (!selectedBaseline.value) {
        ElMessage.warning('请先选择一个基线')
        return
      }
      versionHistoryVisible.value = true
    }

    // 版本切换后的回调
    const handleVersionSwitched = async () => {
      await fetchBaselineList()
      ElMessage.success('版本已切换,基线列表已刷新')
    }

    // 导入基线
    const handleImport = () => {
      ElMessage.info('导入基线功能')
    }

    // 下载基线
    const handleDownload = () => {
      if (!currentBaseline.value) return
      
      const blob = new Blob([currentBaseline.value.configContent], { type: 'text/plain' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = currentBaseline.value.fileName || 'baseline.txt'
      link.click()
      window.URL.revokeObjectURL(url)
    }

    // 文件上传变化
    const handleFileChange = (file) => {
      if (!file.raw) return
      
      const reader = new FileReader()
      reader.onload = (e) => {
        form.configContent = e.target.result
        form.fileName = file.raw.name
      }
      reader.readAsText(file.raw)
    }

    // 提交表单
    const handleSubmit = async () => {
      if (!formRef.value) return
      
      try {
        await formRef.value.validate()
        submitLoading.value = true
        
        if (isEdit.value) {
          // 更新基线
          await baselineApi.updateBaseline(form)
          ElMessage.success('更新成功')
        } else {
          // 创建基线
          // 如果未填写版本号，前端按后端规则生成一个（格式：V + yyyyMMddHHmmss），避免后端校验失败
          if (!form.baselineVersion || String(form.baselineVersion).trim() === '') {
            const pad = (n) => String(n).padStart(2, '0')
            const d = new Date()
            const yyyy = d.getFullYear()
            const MM = pad(d.getMonth() + 1)
            const dd = pad(d.getDate())
            const hh = pad(d.getHours())
            const mm = pad(d.getMinutes())
            const ss = pad(d.getSeconds())
            form.baselineVersion = `V${yyyy}${MM}${dd}${hh}${mm}${ss}`
          }
          await baselineApi.createBaseline(form)
          ElMessage.success('创建成功')
        }
        
        editDialogVisible.value = false
        await fetchBaselineList()
      } catch (error) {
        console.error('基线操作失败:', error)
        if (error !== false) {
          ElMessage.error('操作失败：' + (error.message || '未知错误'))
        }
      } finally {
        submitLoading.value = false
      }
    }

    // 对话框关闭
    const handleViewDialogClose = () => {
      currentBaseline.value = {}
    }

    const handleEditDialogClose = () => {
      if (formRef.value) {
        formRef.value.resetFields()
      }
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
      resetForm()
    }

    // 重置表单
    const resetForm = () => {
      Object.assign(form, {
        id: null,
        systemId: '',
        serverTypeId: '',
        categoryId: '',
        baselineName: '',
        fileName: '',
        configContent: '',
        description: ''
      })
    }

    // 分页操作
    const handleSizeChange = (size) => {
      pagination.size = size
      fetchBaselineList()
    }

    const handleCurrentChange = (current) => {
      pagination.current = current
      fetchBaselineList()
    }

    onMounted(async () => {
      await fetchBasicData()
      await fetchBaselineList()
    })

    // 页面激活时刷新（当从其他页面切换回来时）
    onActivated(async () => {
      await fetchBaselineList()
    })

    return {
      loading,
      submitLoading,
      viewDialogVisible,
      editDialogVisible,
      versionHistoryVisible,
      isEdit,
      formRef,
      uploadRef,
      filterForm,
      pagination,
      systemList,
      serverTypeList,
      categoryList,
      baselineList,
      selectedBaseline,
      currentBaseline,
      form,
      rules,
      getStatusText,
      handleFilterChange,
      handleSearch,
      handleResetFilter,
      handleRowClick,
      handleView,
      handleRefresh,
      handleAdd,
      handleEdit,
      handleCopy,
      handleDelete,
      handleSetDefault,
      handleVersionHistory,
      handleVersionSwitched,
      handleImport,
      handleDownload,
      handleFileChange,
      handleSubmit,
      handleViewDialogClose,
      handleEditDialogClose,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style lang="scss" scoped>
.baseline-manage {
  .filter-section {
    margin-bottom: 24px;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    
    .filter-form {
      margin-bottom: 0;
    }
  }

  .list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
    }

    .list-actions {
      display: flex;
      gap: 8px;
    }
  }

  .baseline-info {
    margin-bottom: 20px;
  }

  .content-area {
    h4 {
      margin-bottom: 10px;
    }

    .content-viewer {
      font-family: 'Courier New', monospace;
    }
  }

  .pagination-container {
    margin-top: 20px;
    text-align: right;
  }
}
</style>