<template>
  <div class="baseline-category">
    <div class="page-header">
      <h2 class="page-title">配置分类管理</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增分类
        </el-button>
      </div>
    </div>

    <!-- 搜索过滤 -->
    <div class="app-card">
      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="分类名称">
          <el-input
            v-model="searchForm.categoryName"
            placeholder="请输入分类名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="分类编码">
          <el-input
            v-model="searchForm.categoryCode"
            placeholder="请输入分类编码"
            clearable
            style="width: 200px"
          />
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

    <!-- 分类表格 -->
    <div class="app-card">
      <el-table
        v-loading="loading"
        :data="categoryList"
        style="width: 100%"
      >
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column prop="categoryCode" label="分类编码" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="applicableTypes" label="适用服务器类型" min-width="200">
          <template #default="{ row }">
            <div v-if="row.applicableTypeNames && row.applicableTypeNames.length">
              <el-tag
                v-for="typeName in row.applicableTypeNames"
                :key="typeName"
                size="small"
                style="margin-right: 5px"
              >
                {{ typeName }}
              </el-tag>
            </div>
            <span v-else class="text-placeholder">全部类型</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="app-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑配置分类' : '新增配置分类'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        class="app-form"
      >
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="请输入分类编码" />
        </el-form-item>
        <el-form-item label="适用服务器类型" prop="applicableTypes">
          <el-select
            v-model="form.applicableTypes"
            multiple
            placeholder="请选择适用的服务器类型（不选择表示适用所有类型）"
            style="width: 100%"
          >
            <el-option
              v-for="type in serverTypeList"
              :key="type.id"
              :label="type.typeName"
              :value="type.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述"
          />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            :max="999"
            placeholder="排序号"
            style="width: 100%"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/baseline'
import { serverTypeApi } from '@/api/system'

export default {
  name: 'BaselineCategory',
  setup() {
    const loading = ref(false)
    const submitLoading = ref(false)
    const dialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref()

    // 搜索表单
    const searchForm = reactive({
      categoryName: '',
      categoryCode: '',
      status: ''
    })

    // 数据列表
    const categoryList = ref([])
    const serverTypeList = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)

    // 表单数据
    const form = reactive({
      id: null,
      categoryName: '',
      categoryCode: '',
      applicableTypes: [],
      description: '',
      sortOrder: 0,
      status: 1
    })

    // 表单验证规则
    const rules = {
      categoryName: [
        { required: true, message: '请输入分类名称', trigger: 'blur' },
        { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
      ],
      categoryCode: [
        { required: true, message: '请输入分类编码', trigger: 'blur' },
        { pattern: /^[A-Z_][A-Z0-9_]*$/, message: '编码只能包含大写字母、数字和下划线，且以字母或下划线开头', trigger: 'blur' }
      ]
    }

    // 获取服务器类型列表
    const fetchServerTypeList = async () => {
      try {
        const response = await serverTypeApi.getServerTypeList()
        serverTypeList.value = response.data || []
      } catch (error) {
        console.error('获取服务器类型失败:', error)
        ElMessage.error('获取服务器类型失败')
        serverTypeList.value = []
      }
    }

    // 获取配置分类列表
    const fetchCategoryList = async () => {
      loading.value = true
      try {
        const params = {
          current: currentPage.value,
          size: pageSize.value,
          categoryName: searchForm.categoryName || undefined,
          categoryCode: searchForm.categoryCode || undefined,
          status: searchForm.status !== '' ? searchForm.status : undefined
        }
        
        console.log('查询参数:', params)
        
        const response = await categoryApi.getCategoryPage(params)
        const pageData = response.data || {}
        const categoryData = pageData.records || []
        
        // 处理适用类型显示
        categoryData.forEach(category => {
          if (category.applicableTypes) {
            const typeIds = category.applicableTypes.split(',').map(id => parseInt(id.trim()))
            category.applicableTypeNames = serverTypeList.value
              .filter(type => typeIds.includes(type.id))
              .map(type => type.typeName)
          }
        })
        
        categoryList.value = categoryData
        total.value = pageData.total || 0
      } catch (error) {
        console.error('获取配置分类失败:', error)
        ElMessage.error('获取配置分类失败')
        categoryList.value = []
        total.value = 0
      } finally {
        loading.value = false
      }
    }

    // 搜索
    const handleSearch = () => {
      fetchCategoryList()
    }

    // 重置
    const handleReset = () => {
      Object.assign(searchForm, {
        categoryName: '',
        categoryCode: '',
        status: ''
      })
      currentPage.value = 1
      handleSearch()
    }

    // 页码变化
    const handleCurrentChange = (page) => {
      currentPage.value = page
      fetchCategoryList()
    }

    // 页面大小变化
    const handleSizeChange = (size) => {
      pageSize.value = size
      currentPage.value = 1
      fetchCategoryList()
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
      Object.assign(form, {
        ...row,
        applicableTypes: row.applicableTypes || []
      })
    }

    // 删除
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除分类"${row.categoryName}"吗？删除后将无法恢复。`,
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        
        // 调用后端API删除
        await categoryApi.deleteCategory(row.id)
        
        ElMessage.success('删除成功')
        fetchCategoryList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除分类失败:', error)
          ElMessage.error(error.response?.data?.message || '删除失败')
        }
      }
    }

    // 提交表单
    const handleSubmit = async () => {
      if (!formRef.value) return
      
      try {
        await formRef.value.validate()
        submitLoading.value = true
        
        if (isEdit.value) {
          // 更新配置分类
          await categoryApi.updateCategory(form)
          ElMessage.success('更新成功')
        } else {
          // 创建配置分类
          await categoryApi.createCategory(form)
          ElMessage.success('创建成功')
        }
        
        dialogVisible.value = false
        fetchCategoryList()
      } catch (error) {
        if (error !== false) {
          ElMessage.error('操作失败')
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
        categoryName: '',
        categoryCode: '',
        applicableTypes: [],
        description: '',
        sortOrder: 0,
        status: 1
      })
    }

    onMounted(async () => {
      await fetchServerTypeList()
      fetchCategoryList()
    })

    return {
      loading,
      submitLoading,
      dialogVisible,
      isEdit,
      formRef,
      searchForm,
      categoryList,
      serverTypeList,
      total,
      currentPage,
      pageSize,
      form,
      rules,
      handleSearch,
      handleReset,
      handleCurrentChange,
      handleSizeChange,
      handleAdd,
      handleEdit,
      handleDelete,
      handleSubmit,
      handleDialogClose
    }
  }
}
</script>

<style lang="scss" scoped>
.baseline-category {
  .search-form {
    .el-form-item {
      margin-bottom: 0;
    }
  }

  .text-placeholder {
    color: #c0c4cc;
    font-style: italic;
  }
}
</style>