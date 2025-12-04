<template>
  <div class="external-link-iframe">
    <div class="iframe-header" v-if="linkInfo">
      <div class="header-left">
        <el-icon class="header-icon"><component :is="linkInfo.icon || 'Link'" /></el-icon>
        <h3 class="header-title">{{ linkInfo.linkName }}</h3>
        <el-tag size="small" type="info" v-if="linkInfo.description">
          {{ linkInfo.description }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-button-group>
          <el-button size="small" @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button size="small" @click="handleOpenNewWindow">
            <el-icon><TopRight /></el-icon>
            新窗口打开
          </el-button>
          <el-button size="small" @click="handleCopyUrl">
            <el-icon><CopyDocument /></el-icon>
            复制链接
          </el-button>
        </el-button-group>
      </div>
    </div>

    <div class="iframe-container" v-loading="loading" element-loading-text="页面加载中...">
      <iframe
        v-if="linkInfo && linkInfo.linkUrl"
        ref="iframeRef"
        :src="linkInfo.linkUrl"
        :key="iframeKey"
        frameborder="0"
        allowfullscreen
        @load="handleIframeLoad"
        @error="handleIframeError"
      ></iframe>
      <el-empty v-else-if="!loading && !linkInfo" description="未找到链接信息" />
    </div>

    <!-- 加载失败提示 -->
    <el-dialog
      v-model="errorDialogVisible"
      title="页面加载失败"
      width="400px"
    >
      <div class="error-content">
        <el-icon class="error-icon" :size="48"><WarningFilled /></el-icon>
        <p>无法在iframe中加载此页面，可能是由于目标网站的安全策略限制。</p>
        <p class="error-tip">建议使用"新窗口打开"方式访问此链接。</p>
      </div>
      <template #footer>
        <el-button @click="errorDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleOpenNewWindow">
          <el-icon><TopRight /></el-icon>
          新窗口打开
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { externalLinkApi } from '@/api/externalLink'

export default {
  name: 'ExternalLinkIframe',
  setup() {
    const route = useRoute()
    const loading = ref(true)
    const linkInfo = ref(null)
    const iframeRef = ref(null)
    const iframeKey = ref(0)
    const errorDialogVisible = ref(false)

    // 获取链接信息
    const fetchLinkInfo = async (id) => {
      loading.value = true
      try {
        const res = await externalLinkApi.getExternalLinkById(id)
        if (res.code === 200) {
          linkInfo.value = res.data
        } else {
          ElMessage.error('获取链接信息失败')
        }
      } catch (error) {
        console.error('获取链接信息失败:', error)
        ElMessage.error('获取链接信息失败')
      }
    }

    // iframe加载完成
    const handleIframeLoad = () => {
      loading.value = false
    }

    // iframe加载失败
    const handleIframeError = () => {
      loading.value = false
      errorDialogVisible.value = true
    }

    // 刷新iframe
    const handleRefresh = () => {
      loading.value = true
      iframeKey.value++
    }

    // 新窗口打开
    const handleOpenNewWindow = () => {
      if (linkInfo.value && linkInfo.value.linkUrl) {
        window.open(linkInfo.value.linkUrl, '_blank')
      }
      errorDialogVisible.value = false
    }

    // 复制链接
    const handleCopyUrl = async () => {
      if (linkInfo.value && linkInfo.value.linkUrl) {
        try {
          await navigator.clipboard.writeText(linkInfo.value.linkUrl)
          ElMessage.success('链接已复制到剪贴板')
        } catch (error) {
          // 降级方案
          const textarea = document.createElement('textarea')
          textarea.value = linkInfo.value.linkUrl
          document.body.appendChild(textarea)
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
          ElMessage.success('链接已复制到剪贴板')
        }
      }
    }

    // 监听路由变化
    watch(
      () => route.params.id,
      (newId) => {
        if (newId) {
          fetchLinkInfo(newId)
        }
      },
      { immediate: true }
    )

    onMounted(() => {
      const id = route.params.id
      if (id) {
        fetchLinkInfo(id)
      }
    })

    return {
      loading,
      linkInfo,
      iframeRef,
      iframeKey,
      errorDialogVisible,
      handleIframeLoad,
      handleIframeError,
      handleRefresh,
      handleOpenNewWindow,
      handleCopyUrl
    }
  }
}
</script>

<style lang="scss" scoped>
.external-link-iframe {
  height: calc(100vh - 130px);
  display: flex;
  flex-direction: column;
  margin: -20px;
  background: #fff;

  .iframe-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 20px;
    border-bottom: 1px solid #ebeef5;
    background: #fafafa;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .header-icon {
        font-size: 20px;
        color: #409eff;
      }

      .header-title {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
      }
    }
  }

  .iframe-container {
    flex: 1;
    position: relative;
    overflow: hidden;

    iframe {
      width: 100%;
      height: 100%;
      border: none;
    }
  }

  .error-content {
    text-align: center;
    padding: 20px;

    .error-icon {
      color: #e6a23c;
      margin-bottom: 16px;
    }

    p {
      margin: 8px 0;
      color: #606266;
    }

    .error-tip {
      color: #909399;
      font-size: 13px;
    }
  }
}
</style>
