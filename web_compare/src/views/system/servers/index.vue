<template>
  <div class="server-manage">
    <div class="page-header">
      <h2 class="page-title">服务器管理</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleAddServer">
          <el-icon><Plus /></el-icon>
          新增服务器
        </el-button>
        <el-button type="success" @click="handleAddType">
          <el-icon><FolderAdd /></el-icon>
          新增类型
        </el-button>
      </div>
    </div>

    <!-- 搜索过滤 -->
    <div class="app-card">
      <el-form :model="searchForm" :inline="true" class="search-form">
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
        <el-form-item label="服务器类型">
          <el-select
            v-model="searchForm.serverTypeId"
            placeholder="请选择服务器类型"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="type in serverTypeList"
              :key="type.id"
              :label="type.typeName"
              :value="type.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="实例名称">
          <el-input
            v-model="searchForm.instanceName"
            placeholder="请输入实例名称"
            clearable
            style="width: 200px"
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

    <!-- 服务器类型管理 -->
    <div class="app-card">
      <h3>服务器类型管理</h3>
      <el-table :data="serverTypeList" style="width: 100%">
        <el-table-column prop="typeName" label="类型名称" />
        <el-table-column prop="typeCode" label="类型编码" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEditType(row)">
              编辑
            </el-button>
            <el-button link type="danger" size="small" @click="handleDeleteType(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 服务器实例列表 -->
    <div class="app-card">
      <h3>服务器实例</h3>
      <el-table
        v-loading="loading"
        :data="serverList"
        style="width: 100%"
      >
        <el-table-column prop="systemName" label="所属系统" width="120" />
        <el-table-column prop="serverTypeName" label="服务器类型" width="120" />
        <el-table-column prop="instanceName" label="实例名称" width="150" />
        <el-table-column prop="serverIp" label="服务器IP" width="130" />
        <el-table-column prop="sshPort" label="SSH端口" width="80" />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column prop="serverRole" label="角色" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.serverRole" size="small">
              {{ row.serverRole }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="connectStatus" label="连接状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.connectStatus === 1 ? 'success' : 'danger'" size="small">
              {{ row.connectStatus === 1 ? '正常' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastConnectTime" label="最后连接时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEditServer(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link type="success" size="small" @click="handleTestConnection(row)">
              <el-icon><Connection /></el-icon>
              测试连接
            </el-button>
            <el-button link type="danger" size="small" @click="handleDeleteServer(row)">
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

    <!-- 服务器类型对话框 -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="isEditType ? '编辑服务器类型' : '新增服务器类型'"
      width="500px"
      @close="handleTypeDialogClose"
    >
      <el-form
        ref="typeFormRef"
        :model="typeForm"
        :rules="typeRules"
        label-width="100px"
        class="app-form"
      >
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="typeForm.typeName" placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model="typeForm.typeCode" placeholder="请输入类型编码" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="typeForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="typeForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="typeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleTypeSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 服务器实例对话框 -->
    <el-dialog
      v-model="serverDialogVisible"
      :title="isEditServer ? '编辑服务器实例' : '新增服务器实例'"
      width="600px"
      @close="handleServerDialogClose"
    >
      <el-form
        ref="serverFormRef"
        :model="serverForm"
        :rules="serverRules"
        label-width="120px"
        class="app-form"
      >
        <el-form-item label="所属系统" prop="systemId">
          <el-select v-model="serverForm.systemId" placeholder="请选择系统" style="width: 100%">
            <el-option
              v-for="system in systemList"
              :key="system.id"
              :label="system.systemName"
              :value="system.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="服务器类型" prop="serverTypeId">
          <el-select v-model="serverForm.serverTypeId" placeholder="请选择服务器类型" style="width: 100%">
            <el-option
              v-for="type in serverTypeList"
              :key="type.id"
              :label="type.typeName"
              :value="type.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="实例名称" prop="instanceName">
          <el-input v-model="serverForm.instanceName" placeholder="请输入实例名称" />
        </el-form-item>
        <el-form-item label="服务器IP" prop="serverIp">
          <el-input v-model="serverForm.serverIp" placeholder="请输入服务器IP" />
        </el-form-item>
        <el-form-item label="SSH端口" prop="sshPort">
          <el-input-number v-model="serverForm.sshPort" :min="1" :max="65535" placeholder="22" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="serverForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="serverForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="服务器角色" prop="serverRole">
          <el-select v-model="serverForm.serverRole" placeholder="请选择服务器角色" clearable>
            <el-option label="主服务器" value="MASTER" />
            <el-option label="从服务器" value="SLAVE" />
            <el-option label="备用服务器" value="BACKUP" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="serverForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="serverDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleServerSubmit" :loading="submitLoading">
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
import { systemApi, serverTypeApi, serverInstanceApi } from '@/api/system'

export default {
  name: 'ServerManage',
  setup() {
    const loading = ref(false)
    const submitLoading = ref(false)
    const typeDialogVisible = ref(false)
    const serverDialogVisible = ref(false)
    const isEditType = ref(false)
    const isEditServer = ref(false)
    const typeFormRef = ref()
    const serverFormRef = ref()

    // 搜索表单
    const searchForm = reactive({
      systemId: '',
      serverTypeId: '',
      instanceName: ''
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
    const serverList = ref([])

    // 服务器类型表单
    const typeForm = reactive({
      id: null,
      typeName: '',
      typeCode: '',
      description: '',
      status: 1
    })

    // 服务器实例表单
    const serverForm = reactive({
      id: null,
      systemId: '',
      serverTypeId: '',
      instanceName: '',
      serverIp: '',
      sshPort: 22,
      username: '',
      password: '',
      serverRole: '',
      description: ''
    })

    // 验证规则
    const typeRules = {
      typeName: [
        { required: true, message: '请输入类型名称', trigger: 'blur' }
      ],
      typeCode: [
        { required: true, message: '请输入类型编码', trigger: 'blur' }
      ]
    }

    const serverRules = {
      systemId: [
        { required: true, message: '请选择系统', trigger: 'change' }
      ],
      serverTypeId: [
        { required: true, message: '请选择服务器类型', trigger: 'change' }
      ],
      instanceName: [
        { required: true, message: '请输入实例名称', trigger: 'blur' }
      ],
      serverIp: [
        { required: true, message: '请输入服务器IP', trigger: 'blur' },
        { pattern: /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/, message: '请输入正确的IP地址', trigger: 'blur' }
      ],
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
      ]
    }

    // 获取数据
    const fetchData = async () => {
      await Promise.all([
        fetchSystemList(),
        fetchServerTypeList(),
        fetchServerList()
      ])
    }

    const fetchSystemList = async () => {
      try {
        const response = await systemApi.getSystemList({ current: 1, size: 1000 })
        systemList.value = response.data?.records || []
      } catch (error) {
        console.error('获取系统列表失败:', error)
        systemList.value = []
      }
    }

    const fetchServerTypeList = async () => {
      try {
        const response = await serverTypeApi.getServerTypeList()
        serverTypeList.value = response.data || []
      } catch (error) {
        console.error('获取服务器类型列表失败:', error)
        serverTypeList.value = []
      }
    }

    const fetchServerList = async () => {
      loading.value = true
      try {
        const params = {
          current: pagination.current,
          size: pagination.size,
          ...searchForm
        }
        const response = await serverInstanceApi.getServerInstanceList(params)
        serverList.value = response.data?.records || []
        pagination.total = response.data?.total || 0
      } catch (error) {
        console.error('获取服务器列表失败:', error)
        serverList.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }

    // 搜索重置
    const handleSearch = () => {
      pagination.current = 1
      fetchServerList()
    }

    const handleReset = () => {
      Object.assign(searchForm, {
        systemId: '',
        serverTypeId: '',
        instanceName: ''
      })
      handleSearch()
    }

    // 服务器类型操作
    const handleAddType = () => {
      isEditType.value = false
      typeDialogVisible.value = true
      resetTypeForm()
    }

    const handleEditType = (row) => {
      isEditType.value = true
      typeDialogVisible.value = true
      Object.assign(typeForm, row)
    }

    const handleDeleteType = async (row) => {
      try {
        await ElMessageBox.confirm(`确定要删除类型"${row.typeName}"吗？`, '删除确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        })
        
        await serverTypeApi.deleteServerType(row.id)
        ElMessage.success('删除成功')
        fetchServerTypeList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除服务器类型失败:', error)
          ElMessage.error('删除失败：' + (error.message || '未知错误'))
        }
      }
    }

    const handleTypeSubmit = async () => {
      if (!typeFormRef.value) return
      
      try {
        await typeFormRef.value.validate()
        submitLoading.value = true
        
        if (isEditType.value) {
          // 更新服务器类型
          await serverTypeApi.updateServerType(typeForm)
          ElMessage.success('更新成功')
        } else {
          // 创建服务器类型
          await serverTypeApi.createServerType(typeForm)
          ElMessage.success('创建成功')
        }
        
        typeDialogVisible.value = false
        fetchServerTypeList()
      } catch (error) {
        console.error('服务器类型操作失败:', error)
        if (error !== false) {
          ElMessage.error('操作失败：' + (error.message || '未知错误'))
        }
      } finally {
        submitLoading.value = false
      }
    }

    const handleTypeDialogClose = () => {
      if (typeFormRef.value) {
        typeFormRef.value.resetFields()
      }
      resetTypeForm()
    }

    const resetTypeForm = () => {
      Object.assign(typeForm, {
        id: null,
        typeName: '',
        typeCode: '',
        description: '',
        status: 1
      })
    }

    // 服务器实例操作
    const handleAddServer = () => {
      isEditServer.value = false
      serverDialogVisible.value = true
      resetServerForm()
    }

    const handleEditServer = (row) => {
      isEditServer.value = true
      serverDialogVisible.value = true
      Object.assign(serverForm, row)
    }

    const handleDeleteServer = async (row) => {
      try {
        await ElMessageBox.confirm(`确定要删除服务器"${row.instanceName}"吗？`, '删除确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        })
        
        await serverInstanceApi.deleteServerInstance(row.id)
        ElMessage.success('删除成功')
        fetchServerList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除服务器失败:', error)
          ElMessage.error('删除失败：' + (error.message || '未知错误'))
        }
      }
    }

    const handleTestConnection = async (row) => {
      try {
        ElMessage.info('正在测试连接...')
        await serverInstanceApi.testServerConnection(row.id)
        ElMessage.success('连接测试成功')
      } catch (error) {
        console.error('连接测试失败:', error)
        ElMessage.error('连接测试失败：' + (error.message || '未知错误'))
      }
    }

    const handleServerSubmit = async () => {
      if (!serverFormRef.value) return
      
      try {
        await serverFormRef.value.validate()
        submitLoading.value = true
        
        if (isEditServer.value) {
          // 更新服务器实例
          await serverInstanceApi.updateServerInstance(serverForm)
          ElMessage.success('更新成功')
        } else {
          // 创建服务器实例
          await serverInstanceApi.createServerInstance(serverForm)
          ElMessage.success('创建成功')
        }
        
        serverDialogVisible.value = false
        fetchServerList()
      } catch (error) {
        console.error('服务器操作失败:', error)
        if (error !== false) {
          ElMessage.error('操作失败：' + (error.message || '未知错误'))
        }
      } finally {
        submitLoading.value = false
      }
    }

    const handleServerDialogClose = () => {
      if (serverFormRef.value) {
        serverFormRef.value.resetFields()
      }
      resetServerForm()
    }

    const resetServerForm = () => {
      Object.assign(serverForm, {
        id: null,
        systemId: '',
        serverTypeId: '',
        instanceName: '',
        serverIp: '',
        sshPort: 22,
        username: '',
        password: '',
        serverRole: '',
        description: ''
      })
    }

    // 分页操作
    const handleSizeChange = (size) => {
      pagination.size = size
      fetchServerList()
    }

    const handleCurrentChange = (current) => {
      pagination.current = current
      fetchServerList()
    }

    onMounted(() => {
      fetchData()
    })

    return {
      loading,
      submitLoading,
      typeDialogVisible,
      serverDialogVisible,
      isEditType,
      isEditServer,
      typeFormRef,
      serverFormRef,
      searchForm,
      pagination,
      systemList,
      serverTypeList,
      serverList,
      typeForm,
      serverForm,
      typeRules,
      serverRules,
      handleSearch,
      handleReset,
      handleAddType,
      handleEditType,
      handleDeleteType,
      handleTypeSubmit,
      handleTypeDialogClose,
      handleAddServer,
      handleEditServer,
      handleDeleteServer,
      handleTestConnection,
      handleServerSubmit,
      handleServerDialogClose,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style lang="scss" scoped>
.server-manage {
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