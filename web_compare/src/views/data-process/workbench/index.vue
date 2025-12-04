<template>
  <div class="workbench-container">
    <div class="header">
      <div class="left">
        <el-button icon="ArrowLeft" circle @click="router.back()" />
        <div class="title-area">
          <span class="title">数据处理工作台</span>
          <span class="subtitle">{{ taskName }}</span>
        </div>
        <el-tag size="small" type="info" effect="plain">{{ executeId }}</el-tag>
        <el-tag size="small" :type="getSourceTypeTag(sourceType)" effect="dark">{{ sourceType }}</el-tag>
      </div>
      <div class="right">
        <el-button type="primary" icon="Download" @click="handleDownloadResult" :disabled="!processedData" plain>
          下载处理结果
        </el-button>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧源数据区域 -->
      <div class="source-panel">
        <div class="panel-header">
          <div class="ph-left">
            <el-icon><Document /></el-icon>
            <span>源数据预览</span>
          </div>
          <div class="tools">
            <el-tooltip content="格式化JSON" placement="top">
              <el-button size="small" circle icon="Operation" @click="formatSource" />
            </el-tooltip>
            <el-tooltip content="压缩JSON" placement="top">
              <el-button size="small" circle icon="SemiSelect" @click="compressSource" />
            </el-tooltip>
          </div>
        </div>
        <div class="editor-wrapper">
          <el-input
            v-model="sourceContent"
            type="textarea"
            placeholder="请输入或导入JSON数据"
            resize="none"
            class="code-editor"
            readonly
          />
        </div>
        <div class="panel-footer">
          <span>数据大小: {{ calculateSize(sourceContent) }}</span>
          <span>行数: {{ sourceContent.split('\n').length }}</span>
        </div>
      </div>

      <!-- 右侧处理器区域 -->
      <div class="processor-panel">
        <el-tabs v-model="activeTab" type="border-card" class="processor-tabs">
          <!-- 处理器1: JSON转Excel -->
          <el-tab-pane name="excel">
            <template #label>
              <span class="custom-tabs-label">
                <el-icon><Grid /></el-icon>
                <span> JSON转Excel</span>
              </span>
            </template>
            
            <div class="processor-content">
              <div class="config-section">
                <div class="section-title">配置选项</div>
                <el-form label-position="top" size="large">
                  <el-form-item label="转换模式">
                    <el-radio-group v-model="excelConfig.mode">
                      <el-radio-button label="auto">自动识别表头</el-radio-button>
                      <el-radio-button label="custom">自定义映射</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  
                  <transition name="el-zoom-in-top">
                    <div v-if="excelConfig.mode === 'custom'" class="custom-mapping-section">
                      <el-form-item>
                        <div class="mapping-header">
                          <span class="label">字段映射配置</span>
                          <el-button type="primary" size="small" @click="parseJsonFields" icon="Refresh">
                            解析字段
                          </el-button>
                        </div>
                      </el-form-item>
                      
                      <!-- 字段映射表格 -->
                      <el-form-item v-if="excelConfig.fieldMappings.length > 0">
                        <el-table :data="excelConfig.fieldMappings" border size="small" max-height="280" class="mapping-table">
                          <el-table-column prop="field" label="原字段名" width="200">
                            <template #default="{ row }">
                              <code class="field-code">{{ row.field }}</code>
                            </template>
                          </el-table-column>
                          <el-table-column prop="header" label="Excel表头（留空使用原字段名）">
                            <template #default="{ row }">
                              <el-input v-model="row.header" :placeholder="row.field" size="small" clearable />
                            </template>
                          </el-table-column>
                        </el-table>
                      </el-form-item>
                      
                      <!-- 无数据提示 -->
                      <el-form-item v-else>
                        <el-empty description="点击【解析字段】按钮从源数据中提取字段" :image-size="60" />
                      </el-form-item>
                      
                      <!-- 高级：手动输入JSON -->
                      <el-collapse class="advanced-collapse">
                        <el-collapse-item title="高级：手动输入映射JSON" name="manual">
                          <el-input 
                            v-model="excelConfig.mapping" 
                            type="textarea" 
                            rows="3" 
                            placeholder='{"field1": "表头1", "field2": "表头2"}'
                            class="mapping-input" 
                          />
                          <div class="tip">提示：手动输入的JSON会与上方表格配置合并，表格配置优先</div>
                        </el-collapse-item>
                      </el-collapse>
                    </div>
                  </transition>

                  <el-form-item>
                    <el-button type="primary" @click="handleToExcel" :loading="processing" class="action-btn">
                      开始转换
                      <el-icon class="el-icon--right"><Right /></el-icon>
                    </el-button>
                  </el-form-item>
                </el-form>
              </div>

              <div class="result-section" v-if="processResult.type === 'excel'">
                <div class="section-title">处理结果</div>
                <div class="success-card">
                  <el-result
                    icon="success"
                    title="转换成功"
                    sub-title="Excel文件已生成，可以直接下载"
                  >
                    <template #extra>
                      <el-button type="success" @click="handleDownloadExcel">下载 Excel 文件</el-button>
                    </template>
                  </el-result>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 处理器2: AI智能处理 -->
          <el-tab-pane name="ai">
             <template #label>
              <span class="custom-tabs-label">
                <el-icon><Cpu /></el-icon>
                <span> AI智能处理</span>
              </span>
            </template>

            <div class="processor-content">
              <div class="config-section">
                 <div class="section-title">AI 配置</div>
                 <el-form label-position="top">
                  <el-row :gutter="20">
                    <el-col :span="16">
                       <el-form-item label="选择模型">
                        <div class="model-select-wrapper">
                          <el-select v-model="aiConfig.model" placeholder="请选择大模型" style="flex: 1">
                            <el-option 
                              v-for="model in aiConfig.models" 
                              :key="model.id" 
                              :label="model.name" 
                              :value="model.id"
                            >
                              <span style="float: left">{{ model.name }}</span>
                              <span v-if="model.tag" style="float: right; color: #8492a6; font-size: 13px">{{ model.tag }}</span>
                            </el-option>
                          </el-select>
                          <el-button type="primary" plain icon="Plus" @click="openAddModelDialog">新增模型</el-button>
                          <el-button icon="Setting" @click="openEditModelDialog(getCurrentModel())">配置</el-button>
                        </div>
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-form-item label="处理指令 (Prompt)">
                    <div class="prompt-templates">
                      <span class="label">快捷指令:</span>
                      <el-tag 
                        v-for="tag in promptTags" 
                        :key="tag" 
                        size="small" 
                        effect="plain" 
                        class="prompt-tag"
                        @click="applyPrompt(tag)"
                      >
                        {{ tag }}
                      </el-tag>
                    </div>
                    <el-input 
                      v-model="aiConfig.prompt" 
                      type="textarea" 
                      rows="4" 
                      placeholder="请输入指令，例如：'分析配置项中的异常值' 或 '总结该日志的核心问题'" 
                    />
                  </el-form-item>

                  <el-form-item>
                    <el-button type="primary" @click="handleAiProcess" :loading="processing" class="action-btn">
                      发送给 AI
                      <el-icon class="el-icon--right"><Promotion /></el-icon>
                    </el-button>
                  </el-form-item>
                </el-form>
              </div>

              <div class="result-section" v-if="processResult.type === 'ai'">
                <div class="section-title">AI 分析结果</div>
                <div class="ai-response-card">
                  <div class="card-header">
                    <div class="model-badge">
                       <el-icon><Cpu /></el-icon> {{ getCurrentModel()?.name || aiConfig.model }}
                    </div>
                    <div class="actions">
                      <el-button link size="small" icon="CopyDocument">复制</el-button>
                    </div>
                  </div>
                  <div class="card-body">
                    {{ processResult.content }}
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 处理器3: 数据清洗 -->
          <el-tab-pane name="clean">
             <template #label>
              <span class="custom-tabs-label">
                <el-icon><Filter /></el-icon>
                <span> 数据清洗</span>
              </span>
            </template>
             <div class="processor-content">
               <div class="config-section">
                 <div class="section-title">清洗规则</div>
                 <el-form label-position="top">
                   <el-form-item>
                     <el-checkbox-group v-model="cleanConfig.rules" class="rules-group">
                       <el-checkbox label="remove_null" border>移除空值 (null/undefined)</el-checkbox>
                       <el-checkbox label="trim_string" border>去除字符串首尾空格</el-checkbox>
                       <el-checkbox label="remove_duplicates" border>数组去重</el-checkbox>
                       <el-checkbox label="format_date" border>标准化日期格式</el-checkbox>
                     </el-checkbox-group>
                   </el-form-item>
                   <el-form-item>
                      <el-button type="primary" @click="handleClean" :loading="processing" class="action-btn">
                        执行清洗
                        <el-icon class="el-icon--right"><VideoPlay /></el-icon>
                      </el-button>
                   </el-form-item>
                 </el-form>
               </div>
               <div class="result-section" v-if="processResult.type === 'clean'">
                 <div class="section-title">清洗结果预览</div>
                 <div class="clean-preview">
                    <el-input type="textarea" :rows="15" :model-value="processResult.content" readonly class="code-editor-light" />
                 </div>
               </div>
             </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>

  <!-- 模型配置对话框 -->
  <el-dialog 
    v-model="modelDialogVisible" 
    :title="editingModel.isEdit ? '编辑模型配置' : '新增AI模型'" 
    width="500px"
    destroy-on-close
  >
    <el-form label-width="100px">
      <el-form-item label="模型名称" required>
        <el-input v-model="editingModel.name" placeholder="如：Kimi、GPT-4 Turbo" />
      </el-form-item>
      <el-form-item label="模型标识" required>
        <el-input v-model="editingModel.modelId" placeholder="API请求使用的model参数，如：moonshot-v1-8k" />
        <div class="form-tip">常见模型标识：moonshot-v1-8k、gpt-4-turbo、deepseek-chat、claude-3-5-sonnet</div>
      </el-form-item>
      <el-form-item label="API 地址" required>
        <el-input v-model="editingModel.url" placeholder="如：https://api.moonshot.cn/v1/chat/completions" />
      </el-form-item>
      <el-form-item label="API Key">
        <el-input v-model="editingModel.apiKey" placeholder="留空则使用后端配置的默认Key" show-password />
      </el-form-item>
      <el-form-item label="超时时间">
        <el-input-number v-model="editingModel.timeout" :min="10" :max="300" :step="10" />
        <span style="margin-left: 8px; color: #909399">秒（建议60-120秒）</span>
      </el-form-item>
      <el-form-item label="标签">
        <el-input v-model="editingModel.tag" placeholder="如：高精度、快速响应（可选）" />
      </el-form-item>
      <el-form-item label="连接测试">
        <el-button type="success" plain icon="Connection" @click="testAiConnection" :loading="testingConnection">
          测试连接
        </el-button>
        <span v-if="testConnectionResult.tested" :style="{ marginLeft: '10px', color: testConnectionResult.success ? '#67C23A' : '#F56C6C' }">
          {{ testConnectionResult.message }}
          <span v-if="testConnectionResult.duration">({{ testConnectionResult.duration }}ms)</span>
        </span>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="editingModel.isEdit && aiConfig.models.length > 1" type="danger" plain @click="deleteModel(editingModel); modelDialogVisible = false">
          删除此模型
        </el-button>
        <span style="flex: 1"></span>
        <el-button @click="modelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveModel">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { dataProcessApi } from '@/api/dataProcess'

// 定义组件名，确保 keep-alive 正常工作
defineOptions({
  name: 'DataProcessWorkbench'
})

const route = useRoute()
const router = useRouter()

const taskId = ref('')
const executeId = ref('')
const taskName = ref('')
const sourceType = ref('')
const sourceContent = ref('')
const processedData = ref(null)
const processing = ref(false)
const activeTab = ref('excel')

// 配置项
const excelConfig = reactive({
  mode: 'auto',
  mapping: '',
  fieldMappings: []  // 字段映射列表 [{field: 'xxx', header: 'xxx'}]
})

const aiConfig = reactive({
  model: 'deepseek',
  prompt: '',
  // 模型列表（从localStorage加载或使用默认值）
  models: JSON.parse(localStorage.getItem('ai_models') || 'null') || [
    { id: 'deepseek', name: 'DeepSeek-V2', modelId: 'deepseek-chat', url: 'https://api.deepseek.com/v1/chat/completions', apiKey: '', tag: '默认' }
  ]
})

// 模型配置对话框
const modelDialogVisible = ref(false)
const editingModel = reactive({
  id: '',
  name: '',
  modelId: '',  // API请求使用的模型标识
  url: '',
  apiKey: '',
  tag: '',
  timeout: 60,  // 超时时间（秒）
  isEdit: false
})

// 测试连接状态
const testingConnection = ref(false)
const testConnectionResult = reactive({
  tested: false,
  success: false,
  message: '',
  duration: null
})

const cleanConfig = reactive({
  rules: ['remove_null']
})

const promptTags = ['提取关键信息', '生成数据摘要', '转换为CSV格式', '分析异常数据', '翻译字段值']

// 处理结果暂存
const processResult = reactive({
  type: '',
  content: '',
  fileId: '',
  fileName: '',
  model: '',
  appliedRules: ''
})

// 加载数据状态
const loadingData = ref(false)

onMounted(async () => {
  taskId.value = route.query.taskId || ''
  executeId.value = route.query.executeId || ''
  taskName.value = route.query.taskName || '未命名任务'
  sourceType.value = route.query.sourceType || 'UNKNOWN'
  
  // 如果有executeId，通过API获取数据内容
  if (executeId.value) {
    loadingData.value = true
    try {
      const res = await dataProcessApi.getResultByExecuteId(executeId.value)
      if (res.code === 200 && res.data) {
        const content = res.data.content
        if (content) {
          try {
            const obj = JSON.parse(content)
            sourceContent.value = JSON.stringify(obj, null, 2)
          } catch (e) {
            sourceContent.value = content
          }
        }
      }
    } catch (e) {
      console.error('获取数据失败:', e)
      ElMessage.error('获取数据内容失败')
    } finally {
      loadingData.value = false
    }
  } else if (route.query.content) {
    // 兼容旧方式：直接从URL获取content
    try {
      const obj = JSON.parse(route.query.content)
      sourceContent.value = JSON.stringify(obj, null, 2)
    } catch (e) {
      sourceContent.value = route.query.content
    }
  } else {
    // 默认 Mock 数据
    const defaultData = {
      "nodes": [
        {"id": 1, "name": "Server-A", "status": "running", "cpu": "45%"},
        {"id": 2, "name": "Server-B", "status": "stopped", "cpu": "0%"},
        {"id": 3, "name": "Server-C", "status": "running", "cpu": "80%"}
      ],
      "timestamp": "2024-05-22T10:00:00Z"
    }
    sourceContent.value = JSON.stringify(defaultData, null, 2)
  }
})

// 工具函数
const formatSource = () => {
  try {
    const obj = JSON.parse(sourceContent.value)
    sourceContent.value = JSON.stringify(obj, null, 2)
  } catch (e) {
    ElMessage.error('JSON格式错误，无法格式化')
  }
}

const compressSource = () => {
  try {
    const obj = JSON.parse(sourceContent.value)
    sourceContent.value = JSON.stringify(obj)
  } catch (e) {
    ElMessage.error('JSON格式错误，无法压缩')
  }
}

const applyPrompt = (text) => {
  aiConfig.prompt = text
}

const getSourceTypeTag = (val) => {
   const map = {
    APOLLO: 'warning',
    DATABASE: 'success',
    SSH: 'info',
    API: 'primary'
  }
  return map[val] || 'info'
}

const calculateSize = (str) => {
  const bytes = new Blob([str]).size
  if (bytes < 1024) return bytes + ' B'
  else if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// ========== AI模型管理 ==========
// 保存模型列表到localStorage
const saveModels = () => {
  localStorage.setItem('ai_models', JSON.stringify(aiConfig.models))
}

// 打开新增模型对话框
const openAddModelDialog = () => {
  editingModel.id = ''
  editingModel.name = ''
  editingModel.modelId = ''
  editingModel.url = ''
  editingModel.apiKey = ''
  editingModel.tag = ''
  editingModel.timeout = 60
  editingModel.isEdit = false
  // 重置测试状态
  testConnectionResult.tested = false
  testConnectionResult.success = false
  testConnectionResult.message = ''
  testConnectionResult.duration = null
  modelDialogVisible.value = true
}

// 打开编辑模型对话框
const openEditModelDialog = (model) => {
  editingModel.id = model.id
  editingModel.name = model.name
  editingModel.modelId = model.modelId || ''
  editingModel.url = model.url
  editingModel.apiKey = model.apiKey
  editingModel.tag = model.tag || ''
  editingModel.timeout = model.timeout || 60
  editingModel.isEdit = true
  // 重置测试状态
  testConnectionResult.tested = false
  testConnectionResult.success = false
  testConnectionResult.message = ''
  testConnectionResult.duration = null
  modelDialogVisible.value = true
}

// 测试AI连接
const testAiConnection = async () => {
  if (!editingModel.url || !editingModel.modelId) {
    ElMessage.warning('请先填写API地址和模型标识')
    return
  }
  
  testingConnection.value = true
  testConnectionResult.tested = false
  
  try {
    const res = await dataProcessApi.testAiConnection({
      url: editingModel.url,
      apiKey: editingModel.apiKey,
      modelId: editingModel.modelId,
      timeout: editingModel.timeout || 30
    })
    
    testConnectionResult.tested = true
    if (res.code === 200 && res.data) {
      testConnectionResult.success = res.data.success
      testConnectionResult.message = res.data.message
      testConnectionResult.duration = res.data.duration
      
      if (res.data.success) {
        ElMessage.success('AI连接测试成功')
      } else {
        ElMessage.error(res.data.message || '连接失败')
      }
    } else {
      testConnectionResult.success = false
      testConnectionResult.message = res.message || '测试失败'
    }
  } catch (e) {
    testConnectionResult.tested = true
    testConnectionResult.success = false
    testConnectionResult.message = e.message || '测试出错'
    ElMessage.error('测试连接失败：' + (e.message || '未知错误'))
  } finally {
    testingConnection.value = false
  }
}

// 保存模型（新增或编辑）
const saveModel = () => {
  if (!editingModel.name || !editingModel.url || !editingModel.modelId) {
    ElMessage.warning('请填写模型名称、模型标识和API地址')
    return
  }
  
  if (editingModel.isEdit) {
    // 编辑模式
    const index = aiConfig.models.findIndex(m => m.id === editingModel.id)
    if (index !== -1) {
      aiConfig.models[index] = {
        id: editingModel.id,
        name: editingModel.name,
        modelId: editingModel.modelId,
        url: editingModel.url,
        apiKey: editingModel.apiKey,
        tag: editingModel.tag,
        timeout: editingModel.timeout || 60
      }
    }
    ElMessage.success('模型配置已更新')
  } else {
    // 新增模式
    const newId = 'model_' + Date.now()
    aiConfig.models.push({
      id: newId,
      name: editingModel.name,
      modelId: editingModel.modelId,
      url: editingModel.url,
      apiKey: editingModel.apiKey,
      tag: editingModel.tag,
      timeout: editingModel.timeout || 60
    })
    ElMessage.success('模型添加成功')
  }
  
  saveModels()
  modelDialogVisible.value = false
}

// 删除模型
const deleteModel = (model) => {
  if (aiConfig.models.length <= 1) {
    ElMessage.warning('至少保留一个模型')
    return
  }
  const index = aiConfig.models.findIndex(m => m.id === model.id)
  if (index !== -1) {
    aiConfig.models.splice(index, 1)
    // 如果删除的是当前选中的模型，切换到第一个
    if (aiConfig.model === model.id) {
      aiConfig.model = aiConfig.models[0].id
    }
    saveModels()
    ElMessage.success('模型已删除')
  }
}

// 获取当前选中的模型配置
const getCurrentModel = () => {
  return aiConfig.models.find(m => m.id === aiConfig.model) || aiConfig.models[0]
}

// 解析JSON字段，提取所有可映射的字段
const parseJsonFields = () => {
  try {
    const data = JSON.parse(sourceContent.value)
    const fields = new Set()
    
    // 递归提取字段
    const extractFields = (obj, prefix = '') => {
      if (Array.isArray(obj)) {
        if (obj.length > 0) {
          extractFields(obj[0], prefix)
        }
      } else if (obj && typeof obj === 'object') {
        for (const key in obj) {
          const fullKey = prefix ? `${prefix}.${key}` : key
          const value = obj[key]
          if (Array.isArray(value)) {
            if (value.length > 0 && typeof value[0] === 'object') {
              extractFields(value[0], fullKey)
            } else {
              fields.add(fullKey)
            }
          } else if (value && typeof value === 'object') {
            extractFields(value, fullKey)
          } else {
            fields.add(fullKey)
          }
        }
      }
    }
    
    extractFields(data)
    
    // 转换为映射列表
    excelConfig.fieldMappings = Array.from(fields).map(field => ({
      field,
      header: ''  // 默认为空，使用原字段名
    }))
    
    ElMessage.success(`解析成功，共发现 ${fields.size} 个字段`)
  } catch (e) {
    ElMessage.error('JSON解析失败：' + e.message)
  }
}

// 生成mapping JSON字符串
const generateMappingJson = () => {
  const mapping = {}
  excelConfig.fieldMappings.forEach(item => {
    if (item.header && item.header.trim()) {
      mapping[item.field] = item.header.trim()
    }
  })
  return Object.keys(mapping).length > 0 ? JSON.stringify(mapping) : ''
}

// 处理器逻辑
const handleToExcel = async () => {
  processing.value = true
  try {
    // 如果是自定义模式，优先使用可视化配置的映射
    let mappingStr = excelConfig.mapping
    if (excelConfig.mode === 'custom' && excelConfig.fieldMappings.length > 0) {
      mappingStr = generateMappingJson() || mappingStr
    }
    
    const res = await dataProcessApi.convertToExcel({
      sourceData: sourceContent.value,
      mode: excelConfig.mode,
      mapping: mappingStr || undefined,
      taskId: taskId.value ? Number(taskId.value) : undefined,
      executeId: executeId.value || undefined,
      fileName: taskName.value || 'data'
    })
    if (res.code === 200 && res.data) {
      processResult.type = 'excel'
      processResult.content = res.data.content
      processResult.fileId = res.data.fileId
      processResult.fileName = res.data.fileName
      processedData.value = true
      ElMessage.success('转换成功')
    } else {
      ElMessage.error(res.message || '转换失败')
    }
  } catch (e) {
    ElMessage.error('转换失败：' + (e.message || '未知错误'))
  } finally {
    processing.value = false
  }
}

const handleAiProcess = async () => {
  if (!aiConfig.prompt) {
    ElMessage.warning('请输入处理指令')
    return
  }
  processing.value = true
  try {
    // 获取当前选中的模型配置
    const currentModel = getCurrentModel()
    const res = await dataProcessApi.aiProcess({
      sourceData: sourceContent.value,
      model: aiConfig.model,
      prompt: aiConfig.prompt,
      taskId: taskId.value ? Number(taskId.value) : undefined,
      executeId: executeId.value || undefined,
      // 传递自定义模型配置
      customUrl: currentModel?.url || undefined,
      customApiKey: currentModel?.apiKey || undefined,
      customModelId: currentModel?.modelId || undefined,
      timeout: currentModel?.timeout || 60
    })
    if (res.code === 200 && res.data) {
      processResult.type = 'ai'
      processResult.content = res.data.content
      processResult.model = res.data.model
      processedData.value = true
      ElMessage.success('AI 处理完成')
    } else {
      ElMessage.error(res.message || 'AI处理失败')
    }
  } catch (e) {
    ElMessage.error('AI处理失败：' + (e.message || '未知错误'))
  } finally {
    processing.value = false
  }
}

const handleClean = async () => {
  if (cleanConfig.rules.length === 0) {
    ElMessage.warning('请至少选择一个清洗规则')
    return
  }
  processing.value = true
  try {
    const res = await dataProcessApi.cleanData({
      sourceData: sourceContent.value,
      rules: cleanConfig.rules,
      taskId: taskId.value ? Number(taskId.value) : undefined,
      executeId: executeId.value || undefined
    })
    if (res.code === 200 && res.data) {
      processResult.type = 'clean'
      processResult.content = res.data.content
      processResult.appliedRules = res.data.appliedRules
      processedData.value = true
      ElMessage.success('数据清洗完成')
    } else {
      ElMessage.error(res.message || '清洗失败')
    }
  } catch (e) {
    ElMessage.error('清洗失败：' + (e.message || '未知错误'))
  } finally {
    processing.value = false
  }
}

const handleDownloadExcel = () => {
  if (processResult.fileId) {
    dataProcessApi.downloadFile(processResult.fileId, processResult.fileName)
    ElMessage.success('开始下载...')
  } else {
    ElMessage.warning('文件不存在')
  }
}

const handleDownloadResult = () => {
  if (processResult.type === 'excel') {
    handleDownloadExcel()
  } else if (processResult.type === 'ai') {
    // 下载AI分析结果文本
    const blob = new Blob([processResult.content], { type: 'text/plain;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'ai_analysis.txt'
    link.click()
    window.URL.revokeObjectURL(url)
  } else if (processResult.type === 'clean') {
    // 下载清洗后的JSON
    const blob = new Blob([processResult.content], { type: 'application/json;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'cleaned_data.json'
    link.click()
    window.URL.revokeObjectURL(url)
  } else {
    ElMessage.info('当前结果暂不支持直接下载')
  }
}
</script>

<style scoped>
.workbench-container {
  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.header {
  background-color: #fff;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  height: 60px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  z-index: 10;
}

.header .left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-area {
  display: flex;
  flex-direction: column;
  margin: 0 8px;
}

.header .title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.header .subtitle {
  font-size: 12px;
  color: #909399;
}

.main-content {
  flex: 1;
  display: flex;
  padding: 16px;
  gap: 16px;
  overflow: hidden;
}

.source-panel, .processor-panel {
  background-color: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.source-panel {
  width: 40%;
  border-right: 1px solid #ebeef5;
}

.processor-panel {
  width: 60%;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fdfdfd;
}

.ph-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
}

.panel-footer {
  padding: 8px 16px;
  background-color: #f5f7fa;
  border-top: 1px solid #ebeef5;
  font-size: 12px;
  color: #909399;
  display: flex;
  justify-content: space-between;
}

.editor-wrapper {
  flex: 1;
  padding: 0;
  overflow: hidden;
  position: relative;
}

.code-editor {
  height: 100%;
}

.code-editor :deep(.el-textarea__inner) {
  height: 100% !important;
  border: none;
  border-radius: 0;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  background-color: #282c34;
  color: #abb2bf;
  padding: 16px;
  line-height: 1.5;
  box-sizing: border-box;
}

.processor-tabs {
  height: 100%;
  border: none;
  box-shadow: none;
}

.processor-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
  background-color: #f9fafc;
  border-bottom: 1px solid #ebeef5;
}

.processor-tabs :deep(.el-tabs__content) {
  padding: 0;
  height: calc(100% - 39px);
  overflow-y: auto;
  background-color: #fff;
}

.processor-content {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.config-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 4px solid #409eff;
  line-height: 1;
}

.custom-tabs-label .el-icon {
  vertical-align: middle;
  margin-right: 4px;
}

.prompt-templates {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.prompt-templates .label {
  font-size: 12px;
  color: #909399;
  margin-right: 4px;
}

.prompt-tag {
  cursor: pointer;
  transition: all 0.2s;
}
.prompt-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.action-btn {
  width: 100%;
  height: 40px;
  font-size: 14px;
  letter-spacing: 1px;
}

/* Result Styles */
.result-section {
  animation: fadeInUp 0.4s ease;
}

.success-card {
  background-color: #f0f9eb;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.ai-response-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.ai-response-card .card-header {
  background-color: #f5f7fa;
  padding: 10px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
}

.model-badge {
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.ai-response-card .card-body {
  padding: 20px;
  background-color: #fff;
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
}

.code-editor-light :deep(.el-textarea__inner) {
  font-family: 'JetBrains Mono', Consolas, monospace;
  background-color: #f5f7fa;
  color: #606266;
  font-size: 13px;
}

.rules-group .el-checkbox {
  margin-right: 12px;
  margin-bottom: 12px;
  width: calc(50% - 12px);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 字段映射配置样式 */
.custom-mapping-section {
  margin-top: 8px;
}

.mapping-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.mapping-header .label {
  font-weight: 500;
  color: #606266;
}

.mapping-table {
  width: 100%;
}

.mapping-table .field-code {
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: #409eff;
}

.advanced-collapse {
  margin-top: 16px;
  border: none;
}

.advanced-collapse :deep(.el-collapse-item__header) {
  font-size: 13px;
  color: #909399;
  background-color: transparent;
  border-bottom: none;
  height: 32px;
  line-height: 32px;
}

.advanced-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.advanced-collapse :deep(.el-collapse-item__content) {
  padding-bottom: 0;
}

.advanced-collapse .tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

/* 模型选择器样式 */
.model-select-wrapper {
  display: flex;
  gap: 8px;
  width: 100%;
}

.dialog-footer {
  display: flex;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
