<template>
  <div class="collect-templates">
    <div class="page-header">
      <h2 class="page-title">采集模板管理</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增模板
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="app-card">
      <el-form :model="searchForm" label-width="80px" :inline="true">
        <el-form-item label="模板名称">
          <el-input
            v-model="searchForm.templateName"
            placeholder="请输入模板名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="模板类型">
          <el-select
            v-model="searchForm.templateType"
            placeholder="请选择模板类型"
            clearable
            style="width: 200px"
          >
            <el-option label="SSH命令" value="COMMAND" />
            <el-option label="文件下载" value="FILE" />
            <el-option label="HTTP接口" value="API" />
            <el-option label="Apollo配置" value="APOLLO" />
            <el-option label="混合类型" value="MULTI_TYPE" />
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

    <!-- 数据表格 -->
    <div class="app-card">
      <el-table
        v-loading="loading"
        :data="templateList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="templateName" label="模板名称" min-width="150" />
        <el-table-column prop="templateType" label="模板类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTemplateTypeColor(row.templateType)">
              {{ getTemplateTypeText(row.templateType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="适用服务器类型" min-width="200">
          <template #default="{ row }">
            <template v-if="getServerTypeNames(row.applicableServerTypes).length">
              <el-tag
                v-for="(name, idx) in getServerTypeNames(row.applicableServerTypes)"
                :key="idx"
                size="small"
                style="margin-right: 6px"
              >
                {{ name }}
              </el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
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
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleTest(row)">
              测试
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
      :title="isEdit ? '编辑模板' : '新增模板'"
      width="1200px"
      @close="handleDialogClose"
      class="template-dialog"
    >
      <div class="template-form-container">
        <el-row :gutter="20">
          <!-- 左侧配置表单 -->
          <el-col :span="14">
            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-width="120px"
            >
              <!-- 基本信息 -->
              <div class="form-section">
                <div class="section-title">
                  <el-icon><InfoFilled /></el-icon>
                  基本信息
                </div>
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="模板名称" prop="templateName">
                      <el-input 
                        v-model="form.templateName" 
                        placeholder="请输入模板名称"
                        clearable
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="模板类型" prop="templateType">
                      <el-radio-group 
                        v-model="form.templateType" 
                        @change="handleTypeChange"
                        class="template-type-group"
                      >
                        <el-radio-button value="COMMAND">
                          <el-icon><Cpu /></el-icon>
                          SSH命令
                        </el-radio-button>
                        <el-radio-button value="FILE">
                          <el-icon><Document /></el-icon>
                          文件下载
                        </el-radio-button>
                        <el-radio-button value="API">
                          <el-icon><Connection /></el-icon>
                          HTTP接口
                        </el-radio-button>
                        <el-radio-button value="APOLLO">
                          <el-icon><Setting /></el-icon>
                          Apollo配置
                        </el-radio-button>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item label="适用服务器类型">
                  <el-select
                    v-model="form.applicableServerTypes"
                    multiple
                    placeholder="请选择适用的服务器类型"
                    style="width: 100%"
                    clearable
                  >
                    <el-option
                      v-for="type in serverTypeList"
                      :key="type.id"
                      :label="type.typeName"
                      :value="type.id"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="描述">
                  <el-input
                    v-model="form.description"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入模板描述"
                    maxlength="200"
                    show-word-limit
                  />
                </el-form-item>

                <!-- 类型说明 -->
                <div v-if="form.templateType" class="type-description">
                  <el-alert 
                    :title="typeDescriptions[form.templateType].title"
                    :description="typeDescriptions[form.templateType].description"
                    type="info"
                    show-icon
                    :closable="false"
                  />
                </div>
              </div>

              <!-- 动态配置表单 -->
              <div v-if="form.templateType" class="form-section">
                <div class="section-title">
                  <el-icon><Setting /></el-icon>
                  配置参数
                </div>

                <!-- SSH命令配置 -->
                <div v-if="form.templateType === 'COMMAND'" class="config-section">
                  <el-form-item label="执行命令" required>
                    <el-input 
                      v-model="commandConfig.command" 
                      type="textarea"
                      placeholder="例如: cat /etc/nginx/nginx.conf"
                      :rows="3"
                      maxlength="1000"
                      show-word-limit
                    />
                    <div class="field-tip">
                      <el-icon><InfoFilled /></el-icon>
                      输入要在服务器上执行的Shell命令，支持管道、重定向等操作
                    </div>
                  </el-form-item>
                  
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="工作目录">
                        <el-input 
                          v-model="commandConfig.workingDir" 
                          placeholder="例如: /opt/app"
                          clearable
                        />
                        <div class="field-tip">可选，命令执行的工作目录</div>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="超时时间(秒)">
                        <el-input-number 
                          v-model="commandConfig.timeout" 
                          :min="10" 
                          :max="300" 
                          :step="10"
                          style="width: 100%"
                        />
                        <div class="field-tip">命令执行超时时间，默认60秒</div>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </div>

                <!-- SFTP文件配置 -->
                <div v-if="form.templateType === 'FILE'" class="config-section">
                  <el-form-item label="文件路径" required>
                    <el-input 
                      v-model="fileConfig.filePath" 
                      placeholder="例如: /opt/app/config/application.properties"
                      clearable
                    />
                    <div class="field-tip">
                      <el-icon><InfoFilled /></el-icon>
                      服务器上配置文件的完整路径，支持绝对路径和相对路径
                    </div>
                  </el-form-item>
                  
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="文件编码">
                        <el-select v-model="fileConfig.encoding" style="width: 100%">
                          <el-option label="UTF-8" value="UTF-8" />
                          <el-option label="GBK" value="GBK" />
                          <el-option label="ISO-8859-1" value="ISO-8859-1" />
                          <el-option label="UTF-16" value="UTF-16" />
                        </el-select>
                        <div class="field-tip">文件内容的字符编码格式</div>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="最大文件大小(MB)">
                        <el-input-number 
                          v-model="fileConfig.maxSizeMB" 
                          :min="1" 
                          :max="100" 
                          :step="1"
                          style="width: 100%"
                        />
                        <div class="field-tip">限制文件大小，防止内存溢出</div>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </div>

                <!-- HTTP接口配置 -->
                <div v-if="form.templateType === 'API'" class="config-section">
                  <el-form-item label="接口地址" required>
                    <el-input 
                      v-model="apiConfig.url" 
                      placeholder="例如: http://config-service.example.com/api/config"
                      clearable
                    />
                    <div class="field-tip">
                      <el-icon><InfoFilled /></el-icon>
                      HTTP接口的完整URL地址，支持HTTP和HTTPS
                    </div>
                  </el-form-item>
                  
                  <el-row :gutter="20">
                    <el-col :span="8">
                      <el-form-item label="请求方法">
                        <el-select v-model="apiConfig.method" style="width: 100%">
                          <el-option label="GET" value="GET" />
                          <el-option label="POST" value="POST" />
                          <el-option label="PUT" value="PUT" />
                          <el-option label="DELETE" value="DELETE" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="超时时间(秒)">
                        <el-input-number 
                          v-model="apiConfig.timeout" 
                          :min="5" 
                          :max="120" 
                          :step="5"
                          style="width: 100%"
                        />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="重试次数">
                        <el-input-number 
                          v-model="apiConfig.retryCount" 
                          :min="0" 
                          :max="5" 
                          :step="1"
                          style="width: 100%"
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-form-item label="请求头">
                    <div class="headers-config">
                      <div 
                        v-for="(header, index) in apiConfig.headers" 
                        :key="index" 
                        class="header-item"
                      >
                        <el-input 
                          v-model="header.key" 
                          placeholder="Header名称"
                          style="width: 200px; margin-right: 10px"
                        />
                        <el-input 
                          v-model="header.value" 
                          placeholder="Header值"
                          style="width: 300px; margin-right: 10px"
                        />
                        <el-button 
                          type="danger" 
                          size="small" 
                          @click="removeHeader(index)"
                          :icon="Delete"
                        />
                      </div>
                      <el-button 
                        type="primary" 
                        size="small" 
                        @click="addHeader"
                        :icon="Plus"
                      >
                        添加请求头
                      </el-button>
                    </div>
                  </el-form-item>

                  <el-form-item v-if="['POST', 'PUT'].includes(apiConfig.method)" label="请求体">
                    <el-input 
                      v-model="apiConfig.body" 
                      type="textarea"
                      :rows="4"
                      placeholder="JSON格式的请求体内容"
                    />
                  </el-form-item>
                </div>

                <!-- Apollo配置 -->
                <div v-if="form.templateType === 'APOLLO'" class="config-section">
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="服务器地址" required>
                        <el-input 
                          v-model="apolloConfig.serverUrl" 
                          placeholder="例如: http://apollo.example.com:8080"
                          clearable
                        />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="应用标识" required>
                        <el-input 
                          v-model="apolloConfig.appId" 
                          placeholder="例如: my-app"
                          clearable
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-row :gutter="20">
                    <el-col :span="8">
                      <el-form-item label="环境" required>
                        <el-input 
                          v-model="apolloConfig.env" 
                          placeholder="例如: DEV, TEST, UAT, PROD"
                          clearable
                        />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="集群">
                        <el-input 
                          v-model="apolloConfig.cluster" 
                          placeholder="默认: default"
                        />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="访问令牌">
                        <el-input 
                          v-model="apolloConfig.token" 
                          placeholder="可选"
                          show-password
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-form-item label="命名空间" required>
                    <el-input 
                      v-model="apolloConfig.namespaces" 
                      placeholder="例如: application,database,redis"
                    />
                    <div class="field-tip">
                      <el-icon><InfoFilled /></el-icon>
                      多个命名空间用逗号分隔，至少需要一个命名空间
                    </div>
                  </el-form-item>
                </div>
              </div>
            </el-form>
          </el-col>

          <!-- 右侧预览和帮助 -->
          <el-col :span="10">
            <div class="preview-panel">
              <!-- 配置示例 -->
              <el-card v-if="form.templateType" class="example-card" shadow="never">
                <template #header>
                  <div class="card-header">
                    <span>配置示例</span>
                    <el-button type="text" size="small" @click="useExample">
                      使用此示例
                    </el-button>
                  </div>
                </template>
                <div class="example-content">
                  <pre>{{ getConfigExample() }}</pre>
                </div>
              </el-card>

              <!-- 实时预览 -->
              <el-card v-if="form.templateType" class="preview-card" shadow="never">
                <template #header>
                  <div class="card-header">
                    <span>配置预览</span>
                    <div class="test-actions">
                      <el-select 
                        v-model="testServerId" 
                        placeholder="选择测试服务器" 
                        size="small"
                        style="width: 150px; margin-right: 8px"
                      >
                        <el-option 
                          v-for="server in testServerList" 
                          :key="server.id"
                          :label="server.instanceName"
                          :value="server.id"
                        />
                      </el-select>
                      <el-button 
                        type="primary" 
                        size="small" 
                        @click="testConnection"
                        :loading="testing"
                        :disabled="!testServerId"
                      >
                        测试连接
                      </el-button>
                    </div>
                  </div>
                </template>
                <div class="json-preview">
                  <pre>{{ formatJson(generateConfig()) }}</pre>
                </div>
              </el-card>

              <!-- 帮助文档 -->
              <el-card v-if="form.templateType" class="help-card" shadow="never">
                <template #header>
                  <span>使用说明</span>
                </template>
                <div class="help-content">
                  <ul>
                    <li v-for="tip in getHelpTips()" :key="tip">{{ tip }}</li>
                  </ul>
                </div>
              </el-card>
            </div>
          </el-col>
        </el-row>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleSubmit" 
            :loading="submitLoading"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 测试结果展示窗口 -->
    <el-dialog
      v-model="testResultVisible"
      title="测试连接结果"
      width="800px"
      @close="closeTestResult"
      class="test-result-dialog"
    >
      <div v-if="testResult" class="test-result-content">
        <!-- 测试状态 -->
        <div class="result-status">
          <el-alert
            :title="testResult.success ? '测试成功' : '测试失败'"
            :type="testResult.success ? 'success' : 'error'"
            :description="testResult.message"
            show-icon
            :closable="false"
          />
        </div>

        <!-- 详细结果 -->
        <div class="result-details">
          <el-tabs v-model="activeResultTab" class="result-tabs">
            <!-- 基本信息 -->
            <el-tab-pane label="基本信息" name="basic">
              <div class="info-grid">
                <div class="info-item">
                  <label>测试状态:</label>
                  <span :class="testResult.success ? 'success-text' : 'error-text'">
                    {{ testResult.success ? '成功' : '失败' }}
                  </span>
                </div>
                <div class="info-item">
                  <label>响应消息:</label>
                  <span>{{ testResult.message }}</span>
                </div>
                <div v-if="testResult.error" class="info-item">
                  <label>错误类型:</label>
                  <span class="error-text">{{ testResult.error }}</span>
                </div>
                <div v-if="testResult.executionTime" class="info-item">
                  <label>执行时间:</label>
                  <span>{{ testResult.executionTime }}ms</span>
                </div>
              </div>
            </el-tab-pane>

            <!-- 测试结果 -->
            <el-tab-pane label="测试结果" name="result">
              <div class="result-content">
                <pre class="result-text">{{ testResult.testResult || '无详细结果' }}</pre>
              </div>
            </el-tab-pane>

            <!-- 服务器信息 -->
            <el-tab-pane v-if="testResult.serverInfo" label="服务器信息" name="server">
              <div class="info-grid">
                <div v-for="(value, key) in testResult.serverInfo" :key="key" class="info-item">
                  <label>{{ key }}:</label>
                  <span>{{ value }}</span>
                </div>
              </div>
            </el-tab-pane>

            <!-- 响应信息 -->
            <el-tab-pane v-if="testResult.responseInfo" label="响应信息" name="response">
              <div class="info-grid">
                <div v-for="(value, key) in testResult.responseInfo" :key="key" class="info-item">
                  <label>{{ key }}:</label>
                  <span>{{ value }}</span>
                </div>
              </div>
            </el-tab-pane>

            <!-- 文件信息 -->
            <el-tab-pane v-if="testResult.fileInfo" label="文件信息" name="file">
              <div class="info-grid">
                <div v-for="(value, key) in testResult.fileInfo" :key="key" class="info-item">
                  <label>{{ key }}:</label>
                  <span>{{ value }}</span>
                </div>
              </div>
            </el-tab-pane>

            <!-- 配置信息 -->
            <el-tab-pane v-if="testResult.configInfo" label="配置信息" name="config">
              <div class="info-grid">
                <div v-for="(value, key) in testResult.configInfo" :key="key" class="info-item">
                  <label>{{ key }}:</label>
                  <span v-if="Array.isArray(value)">{{ value.join(', ') }}</span>
                  <span v-else>{{ value }}</span>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeTestResult">关闭</el-button>
          <el-button v-if="testResult && testResult.success" type="primary" @click="closeTestResult">
            确认使用此配置
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, 
  Delete, 
  Search, 
  Refresh, 
  InfoFilled, 
  Cpu, 
  Document, 
  Connection, 
  Setting 
} from '@element-plus/icons-vue'
import { collectTemplateApi } from '@/api/collect'
import { serverTypeApi } from '@/api/system'

export default {
  name: 'CollectTemplates',
  setup() {
    // 响应式数据
    const loading = ref(false)
    const submitLoading = ref(false)
    const templateList = ref([])
    const serverTypeList = ref([])
    const serverTypeMap = ref({})
    
    // 对话框状态
    const dialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    
    // 搜索表单
    const searchForm = reactive({
      templateName: '',
      templateType: '',
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
      templateName: '',
      templateType: '',
      templateContent: '',
      applicableServerTypes: [],
      configParams: '',
      description: '',
      status: 1
    })

    // 动态配置数据
    const commandConfig = reactive({
      command: '',
      workingDir: '',
      timeout: 60
    })

    const fileConfig = reactive({
      filePath: '',
      encoding: 'UTF-8',
      maxSizeMB: 10
    })

    const apiConfig = reactive({
      url: '',
      method: 'GET',
      headers: [],
      body: '',
      timeout: 30,
      retryCount: 3
    })

    const apolloConfig = reactive({
      serverUrl: '',
      appId: '',
      env: '',
      cluster: 'default',
      namespaces: '',
      token: ''
    })

    // 其他状态
    const testing = ref(false)
    const testServerId = ref(null)
    const testServerList = ref([])

    // 类型描述
    const typeDescriptions = {
      COMMAND: {
        title: 'SSH命令执行',
        description: '通过SSH连接到服务器执行Shell命令，适用于获取系统配置、运行状态等信息'
      },
      FILE: {
        title: 'SFTP文件下载',
        description: '通过SFTP协议下载服务器上的配置文件，适用于应用配置文件、系统配置文件等'
      },
      API: {
        title: 'HTTP接口调用',
        description: '通过HTTP/HTTPS协议调用远程接口获取配置信息，适用于微服务配置、第三方系统接口等'
      },
      APOLLO: {
        title: 'Apollo配置中心',
        description: '从Apollo配置中心获取应用配置，支持多环境、多命名空间的配置管理'
      }
    }

    // 配置示例
    const configExamples = {
      COMMAND: {
        command: 'cat /etc/nginx/nginx.conf',
        workingDir: '/etc/nginx',
        timeout: 60
      },
      FILE: {
        filePath: '/opt/app/config/application.properties',
        encoding: 'UTF-8',
        maxSize: 10485760
      },
      API: {
        url: 'http://config-service.example.com/api/config',
        method: 'GET',
        headers: {
          'Authorization': 'Bearer your-token',
          'Content-Type': 'application/json'
        },
        timeout: 30
      },
      APOLLO: {
        serverUrl: 'http://apollo.example.com:8080',
        appId: 'my-app',
        env: 'DEV',
        cluster: 'default',
        namespaces: 'application,database,redis',
        token: 'your-access-token'
      }
    }

    // 帮助提示
    const helpTips = {
      COMMAND: [
        '确保服务器已配置SSH访问权限',
        '命令执行结果会作为配置内容保存',
        '支持复杂的Shell命令和管道操作',
        '建议设置合理的超时时间避免长时间等待'
      ],
      FILE: [
        '确保文件路径存在且有读取权限',
        '支持各种文本格式的配置文件',
        '大文件下载可能耗时较长，注意超时设置',
        '建议限制文件大小避免内存问题'
      ],
      API: [
        '确保接口地址可访问且返回配置数据',
        '支持各种HTTP方法和自定义请求头',
        '返回的JSON数据会作为配置内容',
        '建议配置重试机制提高成功率'
      ],
      APOLLO: [
        '确保Apollo服务可访问且应用已注册',
        '支持多环境和多命名空间配置',
        '访问令牌用于权限验证（可选）',
        '配置会实时从Apollo服务获取'
      ]
    }
    
    // 表单验证规则
    const rules = {
      templateName: [
        { required: true, message: '请输入模板名称', trigger: 'blur' }
      ],
      templateType: [
        { required: true, message: '请选择模板类型', trigger: 'change' }
      ],
      templateContent: [
        { required: true, message: '请输入模板内容', trigger: 'blur' },
        { 
          validator: (rule, value, callback) => {
            try {
              if (value) {
                JSON.parse(value)
              }
              callback()
            } catch (error) {
              callback(new Error('模板内容必须为有效的JSON格式'))
            }
          },
          trigger: 'blur'
        }
      ]
    }
    
    // 获取模板列表
    const getTemplateList = async () => {
      loading.value = true
      try {
        const params = {
          ...searchForm,
          current: pagination.current,
          size: pagination.size
        }
        const response = await collectTemplateApi.getTemplateList(params)
        console.log('API响应:', response) // 添加调试日志
        
        // 处理不同的响应格式
        if (response && response.data) {
          // 如果响应有data字段
          const data = response.data
          templateList.value = data.records || data || []
          pagination.total = data.total || 0
        } else if (response && response.records) {
          // 如果响应直接包含records
          templateList.value = response.records || []
          pagination.total = response.total || 0
        } else {
          // 其他格式
          templateList.value = response || []
          pagination.total = 0
        }
        
        console.log('模板列表数据:', templateList.value) // 添加调试日志
      } catch (error) {
        console.error('获取模板列表失败:', error)
        ElMessage.error('获取模板列表失败：' + (error.message || '未知错误'))
      } finally {
        loading.value = false
      }
    }
    
    // 获取服务器类型列表
    const getServerTypeList = async () => {
      try {
        const response = await serverTypeApi.getServerTypeList()
        serverTypeList.value = response.data || response.records || []
        // 构建ID->名称映射，便于列表展示
        const map = {}
        serverTypeList.value.forEach(t => { map[t.id] = t.typeName })
        serverTypeMap.value = map
        console.log('服务器类型列表:', serverTypeList.value)
      } catch (error) {
        console.error('获取服务器类型列表失败:', error)
        ElMessage.error('获取服务器类型列表失败')
      }
    }

    // 将逗号分隔的ID字符串转换为名称数组
    const getServerTypeNames = (ids) => {
      if (!ids) return []
      const arr = Array.isArray(ids) ? ids : String(ids).split(',')
      return arr
        .map(id => Number(id))
        .filter(id => !!serverTypeMap.value[id])
        .map(id => serverTypeMap.value[id])
    }

    // 获取测试服务器列表
    const getTestServerList = async () => {
      try {
        const { serverInstanceApi } = await import('@/api/system')
        const response = await serverInstanceApi.getServerInstanceList({ current: 1, size: 50 })
        testServerList.value = response.data?.records || []
        console.log('测试服务器列表:', testServerList.value)
      } catch (error) {
        console.error('获取测试服务器列表失败:', error)
      }
    }
    
    // 事件处理函数
    const handleSearch = () => {
      pagination.current = 1
      getTemplateList()
    }
    
    const handleReset = () => {
      Object.assign(searchForm, {
        templateName: '',
        templateType: '',
        status: null
      })
      handleSearch()
    }
    
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      getTemplateList()
    }
    
    const handleCurrentChange = (current) => {
      pagination.current = current
      getTemplateList()
    }
    
    const handleCreate = () => {
      isEdit.value = false
      resetForm()
      resetDynamicConfigs()
      dialogVisible.value = true
    }
    
    const handleEdit = async (row) => {
      try {
        isEdit.value = true
        
        // 获取完整的模板详情
        const response = await collectTemplateApi.getTemplateById(row.id)
        const templateData = response.data || response
        
        console.log('获取到的模板详情:', templateData) // 调试日志
        
        Object.assign(form, {
          ...templateData,
          applicableServerTypes: templateData.applicableServerTypes ? 
            templateData.applicableServerTypes.split(',').map(Number) : []
        })
        
        // 解析templateContent中的配置数据
        if (templateData.templateContent) {
          try {
            const config = JSON.parse(templateData.templateContent)
            console.log('解析到的配置:', config) // 调试日志
            
            // 根据模板类型填充对应的配置对象
            switch (templateData.templateType) {
              case 'COMMAND':
                Object.assign(commandConfig, {
                  command: config.command || '',
                  workingDir: config.workingDir || '',
                  timeout: config.timeout || 60
                })
                break
              case 'FILE':
                Object.assign(fileConfig, {
                  filePath: config.filePath || '',
                  encoding: config.encoding || 'UTF-8',
                  backupEnabled: config.backupEnabled || false
                })
                break
              case 'API':
                Object.assign(apiConfig, {
                  url: config.url || '',
                  method: config.method || 'GET',
                  headers: config.headers || [],
                  timeout: config.timeout || 30
                })
                break
              case 'APOLLO':
                Object.assign(apolloConfig, {
                  serverUrl: config.serverUrl || '',
                  appId: config.appId || '',
                  env: config.env || '',
                  cluster: config.cluster || 'default',
                  namespaces: Array.isArray(config.namespaces) ? config.namespaces.join(',') : (config.namespaces || ''),
                  token: config.token || ''
                })
                break
            }
          } catch (parseError) {
            console.error('解析模板内容失败:', parseError)
            ElMessage.warning('模板内容格式不正确，请检查配置')
          }
        }
        
        dialogVisible.value = true
      } catch (error) {
        console.error('获取模板详情失败:', error)
        ElMessage.error('获取模板详情失败：' + (error.message || '未知错误'))
      }
    }
    
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除模板"${row.templateName}"吗？`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        await collectTemplateApi.deleteTemplate(row.id)
        ElMessage.success('删除成功')
        getTemplateList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
        }
      }
    }
    
    const handleTest = async (row) => {
      try {
        await collectTemplateApi.testTemplate(row.id)
        ElMessage.success('模板测试成功')
      } catch (error) {
        console.error('模板测试失败:', error)
      }
    }
    
    const handleStatusChange = async (row) => {
      try {
        await collectTemplateApi.updateTemplate({
          id: row.id,
          status: row.status
        })
        ElMessage.success('状态更新成功')
      } catch (error) {
        console.error('状态更新失败:', error)
        // 恢复原状态
        row.status = row.status === 1 ? 0 : 1
      }
    }
    
    const handleSubmit = async () => {
      try {
        // 基础表单验证
        await formRef.value.validate()
        
        // 验证模板配置
        if (!validateTemplateConfig()) {
          return
        }
        
        // 生成模板内容JSON
        const templateContent = generateConfig()
        console.log('生成的模板配置:', templateContent)
        
        submitLoading.value = true
        const data = {
          ...form,
          templateContent: JSON.stringify(templateContent),
          applicableServerTypes: Array.isArray(form.applicableServerTypes) 
            ? form.applicableServerTypes.join(',') 
            : (form.applicableServerTypes || '')
        }
        
        console.log('提交数据:', data)
        
        if (isEdit.value) {
          await collectTemplateApi.updateTemplate(data)
          ElMessage.success('更新成功')
        } else {
          await collectTemplateApi.createTemplate(data)
          ElMessage.success('创建成功')
        }
        
        dialogVisible.value = false
        getTemplateList()
      } catch (error) {
        console.error('提交失败:', error)
        if (error.message) {
          ElMessage.error('提交失败: ' + error.message)
        } else {
          ElMessage.error('提交失败，请检查网络连接')
        }
      } finally {
        submitLoading.value = false
      }
    }

    // 验证模板配置
    const validateTemplateConfig = () => {
      switch (form.templateType) {
        case 'COMMAND':
          if (!commandConfig.command) {
            ElMessage.error('请输入执行命令')
            return false
          }
          break
        case 'FILE':
          if (!fileConfig.filePath) {
            ElMessage.error('请输入文件路径')
            return false
          }
          break
        case 'API':
          if (!apiConfig.url) {
            ElMessage.error('请输入接口地址')
            return false
          }
          break
        case 'APOLLO':
          if (!apolloConfig.serverUrl || !apolloConfig.appId || !apolloConfig.env || !apolloConfig.namespaces) {
            ElMessage.error('请完整填写Apollo配置信息')
            return false
          }
          break
      }
      return true
    }
    
    const handleDialogClose = () => {
      resetForm()
      formRef.value?.clearValidate()
    }
    
    const resetForm = () => {
      Object.assign(form, {
        id: null,
        templateName: '',
        templateType: '',
        templateContent: '',
        applicableServerTypes: [],
        configParams: '',
        description: '',
        status: 1
      })
      // 重置动态配置
      resetDynamicConfigs()
    }

    // 重置动态配置数据
    const resetDynamicConfigs = () => {
      Object.assign(commandConfig, {
        command: '',
        workingDir: '',
        timeout: 60
      })
      Object.assign(fileConfig, {
        filePath: '',
        encoding: 'UTF-8',
        maxSizeMB: 10
      })
      Object.assign(apiConfig, {
        url: '',
        method: 'GET',
        headers: [],
        body: '',
        timeout: 30,
        retryCount: 3
      })
      Object.assign(apolloConfig, {
        serverUrl: '',
        appId: '',
        env: '',
        cluster: 'default',
        namespaces: '',
        token: ''
      })
    }

    // 处理类型变化
    const handleTypeChange = (type) => {
      resetDynamicConfigs()
      if (type === 'API' && apiConfig.headers.length === 0) {
        addHeader()
      }
    }

    // 添加请求头
    const addHeader = () => {
      apiConfig.headers.push({ key: '', value: '' })
    }

    // 删除请求头
    const removeHeader = (index) => {
      apiConfig.headers.splice(index, 1)
    }

    // 生成配置JSON
    const generateConfig = () => {
      switch (form.templateType) {
        case 'COMMAND':
          return {
            command: commandConfig.command,
            workingDir: commandConfig.workingDir || undefined,
            timeout: commandConfig.timeout
          }
        case 'FILE':
          return {
            filePath: fileConfig.filePath,
            encoding: fileConfig.encoding,
            maxSize: fileConfig.maxSizeMB * 1024 * 1024
          }
        case 'API':
          const config = {
            url: apiConfig.url,
            method: apiConfig.method,
            timeout: apiConfig.timeout
          }
          if (apiConfig.headers.length > 0) {
            const headers = {}
            apiConfig.headers.forEach(h => {
              if (h.key && h.value) {
                headers[h.key] = h.value
              }
            })
            if (Object.keys(headers).length > 0) {
              config.headers = headers
            }
          }
          if (['POST', 'PUT'].includes(apiConfig.method) && apiConfig.body) {
            config.body = apiConfig.body
          }
          return config
        case 'APOLLO':
          return {
            serverUrl: apolloConfig.serverUrl,
            appId: apolloConfig.appId,
            env: apolloConfig.env,
            cluster: apolloConfig.cluster || 'default',
            namespaces: apolloConfig.namespaces,
            token: apolloConfig.token || undefined
          }
        default:
          return {}
      }
    }

    // 格式化JSON
    const formatJson = (obj) => {
      try {
        return JSON.stringify(obj, null, 2)
      } catch (error) {
        return '{}'
      }
    }

    // 获取配置示例
    const getConfigExample = () => {
      return formatJson(configExamples[form.templateType] || {})
    }

    // 使用示例配置
    const useExample = () => {
      const example = configExamples[form.templateType]
      if (!example) return

      switch (form.templateType) {
        case 'COMMAND':
          Object.assign(commandConfig, example)
          break
        case 'FILE':
          Object.assign(fileConfig, {
            ...example,
            maxSizeMB: Math.round(example.maxSize / 1024 / 1024)
          })
          break
        case 'API':
          Object.assign(apiConfig, {
            ...example,
            headers: Object.entries(example.headers || {}).map(([key, value]) => ({ key, value }))
          })
          break
        case 'APOLLO':
          Object.assign(apolloConfig, example)
          break
      }
    }

    // 获取帮助提示
    const getHelpTips = () => {
      return helpTips[form.templateType] || []
    }

    // 测试连接状态
    const testResultVisible = ref(false)
    const testResult = ref(null)

    // 测试连接
    const testConnection = async () => {
      if (!form.templateType) {
        ElMessage.warning('请先选择模板类型')
        return
      }
      
      if (!testServerId.value) {
        ElMessage.warning('请选择测试服务器')
        return
      }

      testing.value = true
      testResult.value = null
      
      try {
        const config = generateConfig()
        console.log('测试连接配置:', config)
        console.log('测试服务器ID:', testServerId.value)
        
        // 调用后端测试连接API
        const requestData = {
          serverId: testServerId.value
        }
        
        // 始终传递动态配置参数，以便使用实时输入的配置进行测试
        requestData.templateType = form.templateType
        requestData.config = JSON.stringify(config)
        
        const response = await collectTemplateApi.testTemplateConnection({
          templateId: form.id || 0, // 新建模板时ID为空，传0表示测试配置
          ...requestData
        })
        
        testResult.value = response.data
        testResultVisible.value = true
        
        if (response.data.success) {
          ElMessage.success('连接测试成功')
        } else {
          ElMessage.warning('连接测试有问题，请查看详细结果')
        }
      } catch (error) {
        console.error('连接测试失败:', error)
        testResult.value = {
          success: false,
          message: error.message || '连接测试失败',
          error: 'NetworkError',
          testResult: '网络请求失败，请检查后端服务是否正常运行'
        }
        testResultVisible.value = true
        ElMessage.error('连接测试失败：' + (error.message || '未知错误'))
      } finally {
        testing.value = false
      }
    }

    // 关闭测试结果窗口
    const closeTestResult = () => {
      testResultVisible.value = false
      testResult.value = null
    }
    
    const getTemplateTypeColor = (type) => {
      const colorMap = {
        'COMMAND': 'primary',
        'FILE': 'success',
        'API': 'warning',
        'APOLLO': 'info',
        'MULTI_TYPE': 'danger',
        // 兼容旧的类型名称
        'SERVER_CONFIG': 'primary',
        'FILE_CONFIG': 'success',
        'API_CONFIG': 'warning'
      }
      return colorMap[type] || 'info'
    }
    
    const getTemplateTypeText = (type) => {
      const textMap = {
        'COMMAND': 'SSH命令',
        'FILE': '文件下载',
        'API': 'HTTP接口',
        'APOLLO': 'Apollo配置',
        'MULTI_TYPE': '混合类型',
        // 兼容旧的类型名称
        'SERVER_CONFIG': '服务器配置',
        'FILE_CONFIG': '文件配置',
        'API_CONFIG': 'API配置'
      }
      return textMap[type] || '未知'
    }
    
    // 初始化
    onMounted(async () => {
      await Promise.all([
        getTemplateList(),
        getServerTypeList(),
        getTestServerList()
      ])
    })
    
    // 测试结果tab状态
    const activeResultTab = ref('basic')

    return {
      loading,
      submitLoading,
      templateList,
      serverTypeList,
      dialogVisible,
      isEdit,
      formRef,
      searchForm,
      pagination,
      form,
      rules,
      // 动态配置数据
      commandConfig,
      fileConfig,
      apiConfig,
      apolloConfig,
      // 其他状态
      testing,
      testServerId,
      testServerList,
      typeDescriptions,
      // 测试结果相关
      testResultVisible,
      testResult,
      activeResultTab,
      // 事件处理
      handleSearch,
      handleReset,
      handleSizeChange,
      handleCurrentChange,
      handleCreate,
      handleEdit,
      handleDelete,
      handleTest,
      handleStatusChange,
      handleSubmit,
      handleDialogClose,
      handleTypeChange,
      addHeader,
      removeHeader,
      // 工具方法
      generateConfig,
      formatJson,
      getConfigExample,
      useExample,
      getHelpTips,
      getServerTypeNames,
      testConnection,
      closeTestResult,
      getTemplateTypeColor,
      getTemplateTypeText,
      // 图标
      Plus,
      Delete,
      Search,
      Refresh,
      InfoFilled,
      Cpu,
      Document,
      Connection,
      Setting
    }
  }
}
</script>

<style lang="scss" scoped>
.collect-templates {
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

// 新增模板对话框样式
.template-dialog {
  .template-form-container {
    max-height: 70vh;
    overflow-y: auto;
  }

  .form-section {
    margin-bottom: 24px;
    
    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 500;
      color: #303133;
      margin-bottom: 16px;
      padding-bottom: 8px;
      border-bottom: 1px solid #ebeef5;
    }
  }

  .template-type-group {
    width: 100%;
    
    :deep(.el-radio-button) {
      margin-right: 8px;
      margin-bottom: 8px;
    }
    
    :deep(.el-radio-button__inner) {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 8px 16px;
    }
  }

  .type-description {
    margin-top: 12px;
  }

  .config-section {
    background: #f8f9fa;
    padding: 16px;
    border-radius: 6px;
    margin-top: 12px;
  }

  .field-tip {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }

  .headers-config {
    .header-item {
      display: flex;
      align-items: center;
      margin-bottom: 8px;
    }
  }

  .preview-panel {
    .example-card,
    .preview-card,
    .help-card {
      margin-bottom: 16px;
      
          .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .test-actions {
        display: flex;
        align-items: center;
      }
    }
    }

    .example-content,
    .json-preview {
      background: #f5f7fa;
      border: 1px solid #e4e7ed;
      border-radius: 4px;
      padding: 12px;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 12px;
      line-height: 1.5;
      color: #2c3e50;
      white-space: pre-wrap;
      word-break: break-all;
      max-height: 200px;
      overflow-y: auto;
    }

    .help-content {
      ul {
        margin: 0;
        padding-left: 20px;
        
        li {
          margin-bottom: 8px;
          font-size: 14px;
          color: #606266;
          line-height: 1.4;
        }
      }
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
  }
}

// 测试结果窗口样式
.test-result-dialog {
  .test-result-content {
    .result-status {
      margin-bottom: 20px;
    }

    .result-details {
      .result-tabs {
        .info-grid {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 16px;
          
          .info-item {
            display: flex;
            align-items: center;
            
            label {
              font-weight: 500;
              color: #606266;
              margin-right: 8px;
              min-width: 80px;
            }
            
            span {
              color: #303133;
              word-break: break-all;
              
              &.success-text {
                color: #67c23a;
                font-weight: 500;
              }
              
              &.error-text {
                color: #f56c6c;
                font-weight: 500;
              }
            }
          }
        }

        .result-content {
          .result-text {
            background: #f5f7fa;
            border: 1px solid #e4e7ed;
            border-radius: 4px;
            padding: 16px;
            font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
            font-size: 14px;
            line-height: 1.6;
            color: #2c3e50;
            white-space: pre-wrap;
            word-break: break-all;
            max-height: 300px;
            overflow-y: auto;
            margin: 0;
          }
        }
      }
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
  }
}

// 响应式设计
@media (max-width: 1400px) {
  .template-dialog {
    :deep(.el-dialog) {
      width: 90% !important;
      max-width: 1000px;
    }
  }
  
  .test-result-dialog {
    :deep(.el-dialog) {
      width: 90% !important;
      max-width: 700px;
    }
    
    .info-grid {
      grid-template-columns: 1fr !important;
    }
  }
}
</style>