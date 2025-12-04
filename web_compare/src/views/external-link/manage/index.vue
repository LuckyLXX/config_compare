<template>
  <div class="external-link-manage">
    <div class="page-header">
      <h2 class="page-title">外部链接管理</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增链接
        </el-button>
      </div>
    </div>

    <!-- 搜索过滤 -->
    <div class="app-card">
      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="链接名称">
          <el-input
            v-model="searchForm.linkName"
            placeholder="请输入链接名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="打开方式">
          <el-select
            v-model="searchForm.openType"
            placeholder="请选择打开方式"
            clearable
            style="width: 150px"
          >
            <el-option label="内嵌iframe" :value="1" />
            <el-option label="新窗口打开" :value="2" />
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

    <!-- 链接列表 -->
    <div class="app-card">
      <el-table
        v-loading="loading"
        :data="linkList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="linkName" label="链接名称" min-width="120">
          <template #default="{ row }">
            <div class="link-name-cell">
              <el-icon class="link-icon"><component :is="row.icon || 'Link'" /></el-icon>
              <span>{{ row.linkName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="linkUrl" label="链接地址" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :href="row.linkUrl" target="_blank" :underline="false">
              {{ row.linkUrl }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="80" align="center">
          <template #default="{ row }">
            <el-icon :size="18"><component :is="row.icon || 'Link'" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="openType" label="打开方式" width="120">
          <template #default="{ row }">
            <el-tag :type="row.openType === 1 ? 'primary' : 'success'">
              {{ row.openType === 1 ? '内嵌iframe' : '新窗口打开' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
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
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handlePreview(row)">
              <el-icon><View /></el-icon>
              预览
            </el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
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
      :title="isEdit ? '编辑外部链接' : '新增外部链接'"
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
        <el-form-item label="链接名称" prop="linkName">
          <el-input v-model="form.linkName" placeholder="请输入链接名称，将显示在菜单中" />
        </el-form-item>
        <el-form-item label="链接地址" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="请输入完整URL，如: http://example.com">
            <template #prepend>
              <el-select v-model="urlProtocol" style="width: 100px">
                <el-option label="http://" value="http://" />
                <el-option label="https://" value="https://" />
              </el-select>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-select v-model="form.icon" placeholder="请选择图标" filterable>
            <el-option
              v-for="icon in iconOptions"
              :key="icon"
              :label="icon"
              :value="icon"
            >
              <div class="icon-option">
                <el-icon><component :is="icon" /></el-icon>
                <span>{{ icon }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="打开方式" prop="openType">
          <el-radio-group v-model="form.openType">
            <el-radio :label="1">
              <el-icon><Monitor /></el-icon>
              内嵌iframe
              <span class="radio-desc">（在系统内部嵌入展示）</span>
            </el-radio>
            <el-radio :label="2">
              <el-icon><TopRight /></el-icon>
              新窗口打开
              <span class="radio-desc">（在浏览器新标签页打开）</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" placeholder="数字越小越靠前" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入链接描述"
          />
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { externalLinkApi } from '@/api/externalLink'

export default {
  name: 'ExternalLinkManage',
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const submitLoading = ref(false)
    const dialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref()
    const urlProtocol = ref('http://')

    // 可选图标列表
    const iconOptions = [
      'Link', 'Connection', 'Setting', 'Monitor', 'Cpu', 'Document',
      'TrendCharts', 'DataBoard', 'DataAnalysis', 'Operation', 'Tools',
      'Platform', 'Grid', 'Menu', 'List', 'Files', 'Folder', 'FolderOpened',
      'Edit', 'Search', 'View', 'Download', 'Upload', 'Share', 'Bell',
      'User', 'Lock', 'Key', 'Warning', 'CircleCheck', 'InfoFilled'
    ]

    // 搜索表单
    const searchForm = reactive({
      linkName: '',
      openType: '',
      status: ''
    })

    // 分页
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })

    // 链接列表
    const linkList = ref([])
    const selectedRows = ref([])

    // 表单
    const form = reactive({
      id: null,
      linkName: '',
      linkUrl: '',
      icon: 'Link',
      openType: 1,
      sortOrder: 0,
      description: '',
      status: 1
    })

    // 表单验证规则
    const rules = {
      linkName: [
        { required: true, message: '请输入链接名称', trigger: 'blur' },
        { max: 100, message: '链接名称不能超过100个字符', trigger: 'blur' }
      ],
      linkUrl: [
        { required: true, message: '请输入链接地址', trigger: 'blur' },
        { max: 500, message: '链接地址不能超过500个字符', trigger: 'blur' }
      ],
      openType: [
        { required: true, message: '请选择打开方式', trigger: 'change' }
      ]
    }

    // 获取链接列表
    const fetchLinkList = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.current,
          size: pagination.size,
          ...searchForm
        }
        const res = await externalLinkApi.getExternalLinkList(params)
        if (res.code === 200) {
          linkList.value = res.data.records || res.data.list || []
          pagination.total = res.data.total || 0
        }
      } catch (error) {
        console.error('获取链接列表失败:', error)
      } finally {
        loading.value = false
      }
    }

    // 搜索
    const handleSearch = () => {
      pagination.current = 1
      fetchLinkList()
    }

    // 重置
    const handleReset = () => {
      searchForm.linkName = ''
      searchForm.openType = ''
      searchForm.status = ''
      pagination.current = 1
      fetchLinkList()
    }

    // 表格选择变化
    const handleSelectionChange = (rows) => {
      selectedRows.value = rows
    }

    // 分页大小变化
    const handleSizeChange = (size) => {
      pagination.size = size
      fetchLinkList()
    }

    // 当前页变化
    const handleCurrentChange = (page) => {
      pagination.current = page
      fetchLinkList()
    }

    // 新增
    const handleAdd = () => {
      isEdit.value = false
      resetForm()
      dialogVisible.value = true
    }

    // 编辑
    const handleEdit = (row) => {
      isEdit.value = true
      Object.assign(form, row)
      // 解析URL协议
      if (row.linkUrl.startsWith('https://')) {
        urlProtocol.value = 'https://'
        form.linkUrl = row.linkUrl.replace('https://', '')
      } else if (row.linkUrl.startsWith('http://')) {
        urlProtocol.value = 'http://'
        form.linkUrl = row.linkUrl.replace('http://', '')
      }
      dialogVisible.value = true
    }

    // 预览
    const handlePreview = (row) => {
      if (row.openType === 2) {
        window.open(row.linkUrl, '_blank')
      } else {
        router.push(`/external-link/view/${row.id}`)
      }
    }

    // 删除
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除链接"${row.linkName}"吗？`,
          '删除确认',
          { type: 'warning' }
        )
        await externalLinkApi.deleteExternalLink(row.id)
        ElMessage.success('删除成功')
        fetchLinkList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
        }
      }
    }

    // 状态变更
    const handleStatusChange = async (row) => {
      try {
        await externalLinkApi.updateExternalLinkStatus(row.id, row.status)
        ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
      } catch (error) {
        // 恢复原状态
        row.status = row.status === 1 ? 0 : 1
        console.error('状态更新失败:', error)
      }
    }

    // 关闭对话框
    const handleDialogClose = () => {
      resetForm()
    }

    // 重置表单
    const resetForm = () => {
      form.id = null
      form.linkName = ''
      form.linkUrl = ''
      form.icon = 'Link'
      form.openType = 1
      form.sortOrder = 0
      form.description = ''
      form.status = 1
      urlProtocol.value = 'http://'
      formRef.value?.resetFields()
    }

    // 提交表单
    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        submitLoading.value = true

        // 拼接完整URL
        const submitData = {
          ...form,
          linkUrl: form.linkUrl.startsWith('http') ? form.linkUrl : urlProtocol.value + form.linkUrl
        }

        if (isEdit.value) {
          await externalLinkApi.updateExternalLink(submitData)
          ElMessage.success('更新成功')
        } else {
          await externalLinkApi.createExternalLink(submitData)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        fetchLinkList()
      } catch (error) {
        console.error('提交失败:', error)
      } finally {
        submitLoading.value = false
      }
    }

    onMounted(() => {
      fetchLinkList()
    })

    return {
      loading,
      submitLoading,
      dialogVisible,
      isEdit,
      formRef,
      urlProtocol,
      iconOptions,
      searchForm,
      pagination,
      linkList,
      selectedRows,
      form,
      rules,
      handleSearch,
      handleReset,
      handleSelectionChange,
      handleSizeChange,
      handleCurrentChange,
      handleAdd,
      handleEdit,
      handlePreview,
      handleDelete,
      handleStatusChange,
      handleDialogClose,
      handleSubmit
    }
  }
}
</script>

<style lang="scss" scoped>
.external-link-manage {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .page-title {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
    }
  }

  .app-card {
    background: #fff;
    padding: 20px;
    border-radius: 4px;
    margin-bottom: 20px;
  }

  .link-name-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .link-icon {
      color: #409eff;
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .icon-option {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .radio-desc {
    color: #909399;
    font-size: 12px;
    margin-left: 4px;
  }
}
</style>
