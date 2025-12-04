<template>
  <div class="system-info">
    <div class="page-header">
      <h2 class="page-title">系统信息管理</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增系统
        </el-button>
      </div>
    </div>

    <!-- 搜索过滤 -->
    <div class="app-card">
      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="系统名称">
          <el-input
            v-model="searchForm.systemName"
            placeholder="请输入系统名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="环境类型">
          <el-select
            v-model="searchForm.envType"
            placeholder="请选择环境类型"
            clearable
            style="width: 150px"
          >
            <el-option label="UAT" value="UAT" />
            <el-option label="生产" value="PROD" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
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

    <!-- 系统列表 -->
    <div class="app-card">
      <el-table
        v-loading="loading"
        :data="systemList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="systemName" label="系统名称" min-width="120" />
        <el-table-column prop="systemDesc" label="系统描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="envType" label="环境类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.envType === 'PROD' ? 'danger' : 'warning'">
              {{ row.envType === 'PROD' ? '生产' : 'UAT' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="100" />
        <el-table-column prop="contact" label="联系方式" width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link type="success" size="small" @click="handleViewServers(row)">
              <el-icon><Cpu /></el-icon>
              服务器
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑系统' : '新增系统'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="app-form"
      >
        <el-form-item label="系统名称" prop="systemName">
          <el-input v-model="form.systemName" placeholder="请输入系统名称" />
        </el-form-item>
        <el-form-item label="系统描述" prop="systemDesc">
          <el-input
            v-model="form.systemDesc"
            type="textarea"
            :rows="3"
            placeholder="请输入系统描述"
          />
        </el-form-item>
        <el-form-item label="环境类型" prop="envType">
          <el-radio-group v-model="form.envType">
            <el-radio label="UAT">UAT环境</el-radio>
            <el-radio label="PROD">生产环境</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="负责人" prop="owner">
          <el-input v-model="form.owner" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="form.contact" placeholder="请输入联系方式" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemApi } from '@/api/system'

export default {
  name: 'SystemInfo',
  setup() {
    const loading = ref(false)
    const submitLoading = ref(false)
    const dialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref()

    // 搜索表单
    const searchForm = reactive({
      systemName: '',
      envType: '',
      status: ''
    })

    // 分页信息
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })

    // 系统列表
    const systemList = ref([])
    const selectedSystems = ref([])

    // 表单数据
    const form = reactive({
      id: null,
      systemName: '',
      systemDesc: '',
      envType: 'UAT',
      owner: '',
      contact: '',
      status: 1
    })

    // 表单验证规则
    const rules = {
      systemName: [
        { required: true, message: '请输入系统名称', trigger: 'blur' },
        { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
      ],
      envType: [
        { required: true, message: '请选择环境类型', trigger: 'change' }
      ]
    }

    // 获取系统列表
    const fetchSystemList = async () => {
      loading.value = true
      try {
        console.log('[DEBUG] 系统信息页面开始获取系统列表...')
        const params = {
          current: pagination.current,
          size: pagination.size,
          ...searchForm
        }
        const response = await systemApi.getSystemList(params)
        console.log('[DEBUG] 系统信息页面API响应:', response)
        
        if (response.code === 200) {
          systemList.value = response.data.records || []
          pagination.total = response.data.total || 0
          console.log('[DEBUG] 系统信息页面获取到的系统列表:', systemList.value)
        } else {
          ElMessage.error(response.message)
        }
        
        // 强制触发Vue响应式更新
        await new Promise(resolve => setTimeout(resolve, 0))
      } catch (error) {
        ElMessage.error('获取系统列表失败')
        console.error(error)
        systemList.value = []
      } finally {
        loading.value = false
      }
    }

    // 搜索
    const handleSearch = () => {
      pagination.current = 1
      fetchSystemList()
    }

    // 重置
    const handleReset = () => {
      Object.assign(searchForm, {
        systemName: '',
        envType: '',
        status: ''
      })
      handleSearch()
    }

    // 新增
    const handleAdd = () => {
      isEdit.value = false
      dialogVisible.value = true
      resetForm()
    }

    // 编辑
    const handleEdit = (row) => {
      isEdit.value = true
      dialogVisible.value = true
      Object.assign(form, row)
    }

    // 删除
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除系统"${row.systemName}"吗？`,
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        
        const response = await systemApi.deleteSystem(row.id)
        if (response.code === 200) {
          ElMessage.success('删除成功')
          fetchSystemList()
        } else {
          ElMessage.error(response.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
          console.error(error)
        }
      }
    }

    // 查看服务器
    const handleViewServers = (row) => {
      // TODO: 跳转到服务器管理页面
      ElMessage.info('跳转到服务器管理')
    }

    // 表格选择变化
    const handleSelectionChange = (selection) => {
      selectedSystems.value = selection
    }

    // 分页大小变化
    const handleSizeChange = (size) => {
      pagination.size = size
      fetchSystemList()
    }

    // 当前页变化
    const handleCurrentChange = (current) => {
      pagination.current = current
      fetchSystemList()
    }

    // 提交表单
    const handleSubmit = async () => {
      if (!formRef.value) return
      
      try {
        await formRef.value.validate()
        submitLoading.value = true
        
        const response = isEdit.value
          ? await systemApi.updateSystem(form)
          : await systemApi.createSystem(form)
          
        if (response.code === 200) {
          ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
          dialogVisible.value = false
          fetchSystemList()
        } else {
          ElMessage.error(response.message)
        }
      } catch (error) {
        if (error !== false) { // 表单验证失败时error为false
          ElMessage.error('操作失败')
          console.error(error)
        }
      } finally {
        submitLoading.value = false
      }
    }

    // 对话框关闭
    const handleDialogClose = () => {
      if (formRef.value) {
        formRef.value.resetFields()
      }
      resetForm()
    }

    // 重置表单
    const resetForm = () => {
      Object.assign(form, {
        id: null,
        systemName: '',
        systemDesc: '',
        envType: 'UAT',
        owner: '',
        contact: '',
        status: 1
      })
    }

    onMounted(() => {
      console.log('[DEBUG] 系统信息页面组件挂载，开始加载数据...')
      fetchSystemList()
    })

    return {
      loading,
      submitLoading,
      dialogVisible,
      isEdit,
      formRef,
      searchForm,
      pagination,
      systemList,
      selectedSystems,
      form,
      rules,
      handleSearch,
      handleReset,
      handleAdd,
      handleEdit,
      handleDelete,
      handleViewServers,
      handleSelectionChange,
      handleSizeChange,
      handleCurrentChange,
      handleSubmit,
      handleDialogClose
    }
  }
}
</script>

<style lang="scss" scoped>
.system-info {
  .search-form {
    .el-form-item {
      margin-bottom: 0;
    }
  }

  .pagination-container {
    margin-top: 20px;
    text-align: right;
  }
}
</style>