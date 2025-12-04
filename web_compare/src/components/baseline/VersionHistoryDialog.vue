<template>
  <el-dialog
    v-model="visible"
    title="版本历史"
    width="80%"
    @close="handleClose"
  >
    <div class="version-history-dialog">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索版本号或描述..."
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="loadVersionHistory" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 版本历史表格 -->
      <el-table
        v-loading="loading"
        :data="filteredVersionList"
        style="width: 100%; margin-top: 20px"
        @row-click="handleRowClick"
        highlight-current-row
      >
        <el-table-column prop="baselineVersion" label="版本号" width="180">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success" size="small" style="margin-right: 8px">
              当前默认
            </el-tag>
            <span>{{ row.baselineVersion }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="baselineName" label="基线名称" min-width="150" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'info'" 
              size="small"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getSourceTypeTag(row.sourceType)">
              {{ getSourceTypeText(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createBy" label="创建人" width="100" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click.stop="handleViewContent(row)"
            >
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button
              v-if="row.isDefault !== 1"
              link
              type="success"
              size="small"
              @click.stop="handleSetDefault(row)"
            >
              <el-icon><Check /></el-icon>
              设为默认
            </el-button>
            <el-button
              link
              type="warning"
              size="small"
              @click.stop="handleCompare(row)"
            >
              <el-icon><Document /></el-icon>
              对比
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 查看内容对话框 -->
    <el-dialog
      v-model="contentDialogVisible"
      :title="`查看版本 - ${currentVersion?.baselineVersion}`"
      width="70%"
      append-to-body
    >
      <div class="content-viewer">
        <el-input
          v-model="currentVersion.configContent"
          type="textarea"
          :rows="20"
          readonly
        />
      </div>
      <template #footer>
        <el-button @click="contentDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleDownload">
          <el-icon><Download /></el-icon>
          下载
        </el-button>
      </template>
    </el-dialog>

    <!-- 切换版本确认对话框 -->
    <el-dialog
      v-model="switchDialogVisible"
      title="切换版本确认"
      width="500px"
      append-to-body
    >
      <el-form :model="switchForm" label-width="100px">
        <el-form-item label="目标版本">
          <span style="font-weight: bold">{{ switchForm.targetVersion }}</span>
        </el-form-item>
        <el-form-item label="切换原因">
          <el-input
            v-model="switchForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入切换原因（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="switchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSwitch" :loading="switching">
          确认切换
        </el-button>
      </template>
    </el-dialog>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { baselineApi } from '@/api/baseline'

export default {
  name: 'VersionHistoryDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    systemId: {
      type: Number,
      required: true
    },
    serverTypeId: {
      type: Number,
      required: true
    },
    categoryId: {
      type: Number,
      required: true
    },
    baselineName: {
      type: String,
      required: true
    }
  },
  emits: ['update:modelValue', 'version-switched'],
  setup(props, { emit }) {
    const visible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const loading = ref(false)
    const switching = ref(false)
    const versionList = ref([])
    const searchKeyword = ref('')
    const contentDialogVisible = ref(false)
    const switchDialogVisible = ref(false)
    const currentVersion = ref({})
    const switchForm = ref({
      baselineId: null,
      targetVersion: '',
      reason: ''
    })

    // 过滤版本列表
    const filteredVersionList = computed(() => {
      if (!searchKeyword.value) {
        return versionList.value
      }
      const keyword = searchKeyword.value.toLowerCase()
      return versionList.value.filter(item => 
        item.baselineVersion?.toLowerCase().includes(keyword) ||
        item.baselineName?.toLowerCase().includes(keyword) ||
        item.description?.toLowerCase().includes(keyword)
      )
    })

    // 加载版本历史
    const loadVersionHistory = async () => {
      loading.value = true
      try {
        console.log('🔍 查询版本历史:', {
          systemId: props.systemId,
          serverTypeId: props.serverTypeId,
          categoryId: props.categoryId,
          baselineName: props.baselineName
        })
        const response = await baselineApi.getVersionHistory(
          props.systemId,
          props.serverTypeId,
          props.categoryId,
          props.baselineName
        )
        versionList.value = response.data || []
        console.log('✅ 版本历史加载成功:', versionList.value.length, '个版本')
      } catch (error) {
        console.error('加载版本历史失败:', error)
        ElMessage.error('加载版本历史失败: ' + (error.message || '未知错误'))
      } finally {
        loading.value = false
      }
    }

    // 状态文本
    const getStatusText = (status) => {
      const statusMap = {
        0: '草稿',
        1: '生效',
        2: '归档'
      }
      return statusMap[status] || '未知'
    }

    // 来源类型文本
    const getSourceTypeText = (sourceType) => {
      const typeMap = {
        'MANUAL': '手动创建',
        'IMPORT': '导入',
        'COPY': '复制',
        'PROMOTE': '晋级'
      }
      return typeMap[sourceType] || sourceType || '未知'
    }

    // 来源类型标签
    const getSourceTypeTag = (sourceType) => {
      const tagMap = {
        'MANUAL': '',
        'IMPORT': 'success',
        'COPY': 'warning',
        'PROMOTE': 'danger'
      }
      return tagMap[sourceType] || ''
    }

    // 表格行点击
    const handleRowClick = (row) => {
      currentVersion.value = row
    }

    // 查看内容
    const handleViewContent = (row) => {
      currentVersion.value = { ...row }
      contentDialogVisible.value = true
    }

    // 设为默认
    const handleSetDefault = (row) => {
      switchForm.value = {
        baselineId: row.id,
        targetVersion: row.baselineVersion,
        reason: ''
      }
      switchDialogVisible.value = true
    }

    // 确认切换
    const confirmSwitch = async () => {
      switching.value = true
      try {
        await baselineApi.switchToVersion(
          switchForm.value.baselineId,
          switchForm.value.reason
        )
        ElMessage.success('切换版本成功')
        switchDialogVisible.value = false
        await loadVersionHistory()
        emit('version-switched')
      } catch (error) {
        console.error('切换版本失败:', error)
        ElMessage.error('切换版本失败: ' + (error.message || '未知错误'))
      } finally {
        switching.value = false
      }
    }

    // 对比
    const handleCompare = (row) => {
      ElMessage.info('对比功能正在开发中...')
    }

    // 下载
    const handleDownload = () => {
      if (!currentVersion.value) return
      
      const blob = new Blob([currentVersion.value.configContent], { type: 'text/plain;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = currentVersion.value.fileName || `baseline_${currentVersion.value.baselineVersion}.txt`
      link.click()
      window.URL.revokeObjectURL(url)
    }

    // 关闭对话框
    const handleClose = () => {
      visible.value = false
    }

    // 监听对话框显示,自动加载数据
    watch(visible, (newVal) => {
      if (newVal) {
        loadVersionHistory()
      }
    })

    return {
      visible,
      loading,
      switching,
      versionList,
      searchKeyword,
      filteredVersionList,
      contentDialogVisible,
      switchDialogVisible,
      currentVersion,
      switchForm,
      loadVersionHistory,
      getStatusText,
      getSourceTypeText,
      getSourceTypeTag,
      handleRowClick,
      handleViewContent,
      handleSetDefault,
      confirmSwitch,
      handleCompare,
      handleDownload,
      handleClose
    }
  }
}
</script>

<style lang="scss" scoped>
.version-history-dialog {
  .search-bar {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .content-viewer {
    :deep(.el-textarea__inner) {
      font-family: 'Courier New', Consolas, monospace;
      font-size: 13px;
    }
  }
}
</style>

