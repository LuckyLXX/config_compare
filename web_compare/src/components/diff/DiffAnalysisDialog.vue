<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="1400px"
    top="5vh"
  >
    <div class="diff-analysis" v-loading="loading">
      <div class="diff-summary">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="总差异数" :value="diffList.length" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="新增行" :value="diffSummary.add" value-style="color: #67c23a" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="删除行" :value="diffSummary.delete" value-style="color: #f56c6c" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="修改行" :value="diffSummary.modify" value-style="color: #e6a23c" />
          </el-col>
        </el-row>
      </div>

      <div class="side-by-side-diff">
        <div class="diff-header">
          <div class="baseline-side">
            <h4>{{ baselineTitle }}</h4>
            <span class="file-info">{{ baselineSubTitle }}</span>
          </div>
          <div class="current-side">
            <h4>{{ currentTitle }}</h4>
            <span class="file-info">{{ currentSubTitle }}</span>
          </div>
        </div>

        <div class="diff-content">
          <div class="baseline-content" ref="baselineContentRef">
            <div class="line-numbers">
              <div
                v-for="(line, index) in baselineLines"
                :key="`baseline-line-number-${index}`"
                :class="['line-number', getLineClass(index, 'baseline')]"
              >
                {{ baselineOriginalLineNumbers[index] > 0 ? baselineOriginalLineNumbers[index] : '' }}
              </div>
            </div>
            <div class="content-lines">
              <div
                v-for="(line, index) in baselineLines"
                :key="`baseline-line-${index}`"
                :class="['content-line', getLineClass(index, 'baseline')]"
                v-html="highlightLine(line, index, 'baseline')"
              />
            </div>
          </div>

          <div class="current-content" ref="currentContentRef">
            <div class="line-numbers">
              <div
                v-for="(line, index) in currentLines"
                :key="`current-line-number-${index}`"
                :class="['line-number', getLineClass(index, 'current')]"
              >
                {{ currentOriginalLineNumbers[index] > 0 ? currentOriginalLineNumbers[index] : '' }}
              </div>
            </div>
            <div class="content-lines">
              <div
                v-for="(line, index) in currentLines"
                :key="`current-line-${index}`"
                :class="['content-line', getLineClass(index, 'current')]"
                v-html="highlightLine(line, index, 'current')"
              />
            </div>
          </div>
        </div>
      </div>

      <el-divider content-position="left">差异详情</el-divider>
      <el-table :data="diffList" stripe max-height="300">
        <el-table-column prop="diffPath" label="行号" width="80" />
        <el-table-column prop="diffType" label="差异类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getDiffTypeColor(row.diffType)">
              {{ getDiffTypeText(row.diffType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diffLevel" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="getSeverityColor(row.diffLevel)" size="small">
              {{ getSeverityText(row.diffLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baselineValue" label="基线值" min-width="200" show-overflow-tooltip />
        <el-table-column prop="currentValue" label="当前值" min-width="200" show-overflow-tooltip />
        <el-table-column prop="suggestAction" label="建议操作" min-width="200" show-overflow-tooltip />
      </el-table>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  title: { type: String, default: '差异分析 - 左右对比' },
  baselineTitle: { type: String, default: '基线配置' },
  baselineSubTitle: { type: String, default: '' },
  currentTitle: { type: String, default: '当前配置' },
  currentSubTitle: { type: String, default: '' },
  baselineContent: { type: String, default: '' },
  currentContent: { type: String, default: '' },
  diffList: { type: Array, default: () => [] },
  alignedLines: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const baselineLines = ref([])
const currentLines = ref([])
const diffLineMap = ref(new Map())
const baselineOriginalLineNumbers = ref([])
const currentOriginalLineNumbers = ref([])

// 【新增】滚动容器的 ref
const baselineContentRef = ref(null)
const currentContentRef = ref(null)
let isSyncing = false // 防止滚动循环

const diffSummary = computed(() => {
  // 基于实际显示的差异计算统计信息
  let add = 0, del = 0, modify = 0

  diffLineMap.value.forEach(diff => {
    if (diff.diffType === 'ADD') add++
    else if (diff.diffType === 'DELETE') del++
    else if (diff.diffType === 'MODIFY') modify++
  })

  return { add, delete: del, modify }
})

/**
 * 检测是否为JSON内容
 */
const isJsonContent = (content) => {
  if (!content || typeof content !== 'string') {
    return false
  }
  
  const trimmed = content.trim()
  return (trimmed.startsWith('{') && trimmed.endsWith('}')) || 
         (trimmed.startsWith('[') && trimmed.endsWith(']'))
}

/**
 * 规范化JSON格式
 * 统一缩进、冒号后空格等格式问题
 */
const normalizeJsonFormat = (jsonContent) => {
  if (!jsonContent || typeof jsonContent !== 'string') {
    return jsonContent
  }
  
  try {
    // 解析JSON并重新格式化（2空格缩进）
    const jsonObject = JSON.parse(jsonContent)
    return JSON.stringify(jsonObject, null, 2)
  } catch (e) {
    console.warn('JSON格式规范化失败，使用原始内容:', e.message)
    return jsonContent
  }
}

const sanitizeLines = content => {
  if (!content) return []
  return content.split(/\r?\n/)
}

const computeLcsMatrix = (a, b) => {
  const m = a.length
  const n = b.length
  const lcs = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0))

  for (let i = m - 1; i >= 0; i -= 1) {
    for (let j = n - 1; j >= 0; j -= 1) {
      if (a[i] === b[j]) {
        lcs[i][j] = lcs[i + 1][j + 1] + 1
      } else {
        lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1])
      }
    }
  }
  return lcs
}

const alignSequences = (baseline, current) => {
  const lcs = computeLcsMatrix(baseline, current)
  const alignedBaseline = []
  const alignedCurrent = []

  let i = 0
  let j = 0

  while (i < baseline.length && j < current.length) {
    if (baseline[i] === current[j]) {
      alignedBaseline.push(baseline[i])
      alignedCurrent.push(current[j])
      i += 1
      j += 1
    } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
      alignedBaseline.push(baseline[i])
      alignedCurrent.push('')
      i += 1
    } else {
      alignedBaseline.push('')
      alignedCurrent.push(current[j])
      j += 1
    }
  }

  while (i < baseline.length) {
    alignedBaseline.push(baseline[i])
    alignedCurrent.push('')
    i += 1
  }

  while (j < current.length) {
    alignedBaseline.push('')
    alignedCurrent.push(current[j])
    j += 1
  }

  return { alignedBaseline, alignedCurrent }
}

// Beyond Compare风格差异对齐算法 - 精确差异分析
const beyondCompareAlignSequences = (baseline, current) => {
  const alignedLines = []
  const baselineLineNumbers = []
  const currentLineNumbers = []

  let i = 0, j = 0
  const m = baseline.length, n = current.length
  let currentLineNumber = 1
  let baselineLineNumber = 1

  // 使用LCS算法找到匹配的行
  const lcs = computeLcsMatrix(baseline, current)

  // 回溯构建对齐结果
  while (i < m && j < n) {
    // 【修复】增强匹配判断，忽略前后空格差异
    if (baseline[i] === current[j] || baseline[i].trim() === current[j].trim()) {
      // 完全匹配的行
      alignedLines.push({
        baseline: baseline[i],
        current: current[j],
        type: 'match',
        diffInfo: null
      })
      baselineLineNumbers.push(baselineLineNumber++)
      currentLineNumbers.push(currentLineNumber++)
      i++
      j++
    } else if (isSimilarLine(baseline[i], current[j])) {
      // 相似但不完全相同的行 - 视为修改行
      alignedLines.push({
        baseline: baseline[i],
        current: current[j],
        type: 'modify',
        diffInfo: null
      })
      baselineLineNumbers.push(baselineLineNumber++)
      currentLineNumbers.push(currentLineNumber++)
      i++
      j++
    } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
      // 基线中的行被删除 - 需要在右侧添加空行对齐
      alignedLines.push({
        baseline: baseline[i],
        current: '', // 空字符串表示占位行，不是斜纹填充
        type: 'delete',
        diffInfo: null
      })
      baselineLineNumbers.push(baselineLineNumber++)
      currentLineNumbers.push(-1) // 【修复】右侧是空行，没有实际行号
      i++
    } else {
      // 当前的行是新增的 - 需要在左侧添加空行对齐
      alignedLines.push({
        baseline: '', // 空字符串表示占位行，不是斜纹填充
        current: current[j],
        type: 'add',
        diffInfo: null
      })
      baselineLineNumbers.push(-1) // 【修复】左侧是空行，没有实际行号
      currentLineNumbers.push(currentLineNumber++)
      j++
    }
  }

  // 处理剩余的基线行（删除）
  while (i < m) {
    alignedLines.push({
      baseline: baseline[i],
      current: '', // 空字符串占位
      type: 'delete',
      diffInfo: null
    })
    baselineLineNumbers.push(baselineLineNumber++)
    currentLineNumbers.push(-1) // 【修复】右侧是空行，没有实际行号
    i++
  }

  // 处理剩余的当前行（新增）
  while (j < n) {
    alignedLines.push({
      baseline: '', // 空字符串占位
      current: current[j],
      type: 'add',
      diffInfo: null
    })
    baselineLineNumbers.push(-1) // 【修复】左侧是空行，没有实际行号
    currentLineNumbers.push(currentLineNumber++)
    j++
  }

  return { alignedLines, baselineLineNumbers, currentLineNumbers }
}

// 判断两行是否相似（用于检测修改行）
const isSimilarLine = (baselineLine, currentLine) => {
  if (!baselineLine || !currentLine) return false

  // 【修复】如果两行内容完全相同（忽略前后空格），不应视为修改行
  if (baselineLine.trim() === currentLine.trim()) {
    return false
  }

  // 优先检查JSON/YAML格式的key-value对
  // 如果两行都包含冒号，提取key部分进行比较
  const colonIndex1 = baselineLine.indexOf(':')
  const colonIndex2 = currentLine.indexOf(':')
  
  if (colonIndex1 > 0 && colonIndex2 > 0) {
    // 提取key部分（冒号前的内容）
    const key1 = baselineLine.substring(0, colonIndex1).trim()
    const key2 = currentLine.substring(0, colonIndex2).trim()
    
    // 如果key相同（忽略引号差异），认为是修改行
    if (key1 === key2 || key1.replace(/['"]/g, '') === key2.replace(/['"]/g, '')) {
      return true
    }
  }

  // 计算相似度（基于编辑距离或最长公共子序列）
  const similarity = calculateLineSimilarity(baselineLine, currentLine)

  // 如果相似度超过阈值，认为是修改行而不是新增/删除
  return similarity > 0.7 // 70%相似度阈值
}

// 计算两行文本的相似度
const calculateLineSimilarity = (line1, line2) => {
  // 使用最长公共子序列长度计算相似度
  const lcsLength = computeLineLCS(line1, line2)
  const maxLength = Math.max(line1.length, line2.length)

  return maxLength > 0 ? lcsLength / maxLength : 0
}

// 计算两行的最长公共子序列
const computeLineLCS = (s1, s2) => {
  const m = s1.length
  const n = s2.length
  const dp = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0))

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (s1[i - 1] === s2[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
      }
    }
  }

  return dp[m][n]
}

// 在对齐的行中查找内容
const findLineByContentInAlignedLines = (alignedLines, diff) => {
  const searchKey = diff.diffKey || diff.baselineValue || diff.currentValue
  if (!searchKey) return

  for (let i = 0; i < alignedLines.length; i++) {
    const line = alignedLines[i]
    if ((line.baseline && line.baseline.includes(searchKey)) ||
        (line.current && line.current.includes(searchKey))) {
      diffLineMap.value.set(i, diff)
      return
    }
  }
}

const buildSideBySideData = () => {
  let baseline = props.baselineContent
  let current = props.currentContent
  
  // 检测并规范化JSON内容
  if (isJsonContent(baseline) && isJsonContent(current)) {
    console.log('🔍 检测到JSON内容，进行前端格式规范化')
    baseline = normalizeJsonFormat(baseline)
    current = normalizeJsonFormat(current)
  }
  
  baseline = sanitizeLines(baseline)
  current = sanitizeLines(current)

  // 如果有后端差异数据，优先使用后端数据进行对齐
  if (props.diffList && props.diffList.length > 0) {
    buildFromBackendDiffData(baseline, current)
  } else {
    // 没有后端数据时，使用前端算法
    buildFromFrontendAlgorithm(baseline, current)
  }
}

// 使用后端差异数据构建显示
const buildFromBackendDiffData = (baseline, current) => {
  // 【关键修复】如果后端返回了完整的对齐行信息，直接使用
  if (props.alignedLines && props.alignedLines.length > 0) {
    console.log('✅ 使用后端对齐数据，行数:', props.alignedLines.length)
    
    // 直接使用后端的对齐结果
    baselineLines.value = props.alignedLines.map(line => line.baselineContent || '')
    currentLines.value = props.alignedLines.map(line => line.currentContent || '')
    baselineOriginalLineNumbers.value = props.alignedLines.map(line => line.baselineLineNumber)
    currentOriginalLineNumbers.value = props.alignedLines.map(line => line.currentLineNumber)
    
    // 创建差异映射
    diffLineMap.value = new Map()
    
    // 直接根据后端的diffType标记差异
    props.alignedLines.forEach((line, index) => {
      if (line.diffType !== 'EQUAL') {
        diffLineMap.value.set(index, {
          diffType: line.diffType,
          diffKey: '',
          baselineValue: line.baselineContent,
          currentValue: line.currentContent
        })
      }
    })
    
    return
  }
  
  // 如果后端没有返回对齐数据，使用前端算法进行基本对齐
  console.log('⚠️ 后端未返回对齐数据，使用前端算法对齐')
  const { alignedLines, baselineLineNumbers, currentLineNumbers } = beyondCompareAlignSequences(baseline, current)
  
  baselineLines.value = alignedLines.map(line => line.baseline)
  currentLines.value = alignedLines.map(line => line.current)
  
  // 存储原始行号信息
  baselineOriginalLineNumbers.value = baselineLineNumbers
  currentOriginalLineNumbers.value = currentLineNumbers
  
  // 【关键修复】创建差异映射，完全依据后端数据，不使用前端算法判断
  diffLineMap.value = new Map()
  
  // 【修复】移除前端算法的差异标记，只根据后端数据标记
  // 前端算法只负责对齐显示，差异判断完全交给后端
  
  // 创建一个已匹配索引集合，避免重复匹配
  const matchedIndices = new Set()
  
  props.diffList.forEach(diff => {
    let bestMatchIndex = -1
    let bestMatchScore = 0
    
    // 【优化】优先基于行号进行精确匹配
    if (diff.diffPath && typeof diff.diffPath === 'string') {
      const lineNumberMatch = diff.diffPath.match(/line[_\s]?(\d+)/i) || diff.diffPath.match(/^(\d+)$/)
      if (lineNumberMatch) {
        const lineNumber = parseInt(lineNumberMatch[1], 10)
        
        // 在对齐后的行中查找对应行号
        for (let i = 0; i < alignedLines.length; i++) {
          if (matchedIndices.has(i)) continue
          
          // 检查基线或当前行号是否匹配（跳过-1的占位行号）
          const baselineMatches = baselineLineNumbers[i] > 0 && baselineLineNumbers[i] === lineNumber
          const currentMatches = currentLineNumbers[i] > 0 && currentLineNumbers[i] === lineNumber
          
          if (baselineMatches || currentMatches) {
            bestMatchIndex = i
            bestMatchScore = 100
            break
          }
        }
      }
    }
    
    // 如果基于行号没有找到匹配，再基于内容匹配
    if (bestMatchScore < 100) {
      // 在对齐后的行中查找最佳匹配的差异
      for (let i = 0; i < alignedLines.length; i++) {
        if (matchedIndices.has(i)) continue // 跳过已匹配的行
        
        const line = alignedLines[i]
        let matchScore = 0
        
        // 检查是否匹配后端差异数据
        if (diff.diffType === 'MODIFY') {
        // 对于修改类型，优先检查diffKey是否在行中
        const diffKey = diff.diffKey || ''
        const baselineHasKey = diffKey && line.baseline.includes(diffKey)
        const currentHasKey = diffKey && line.current.includes(diffKey)
        
        // 检查值是否匹配
        const baselineHasValue = diff.baselineValue && line.baseline.includes(diff.baselineValue.trim())
        const currentHasValue = diff.currentValue && line.current.includes(diff.currentValue.trim())
        
        // 计算匹配分数
        if (baselineHasKey && baselineHasValue && currentHasKey && currentHasValue) {
          matchScore = 100 // 完美匹配
        } else if (baselineHasKey && baselineHasValue) {
          matchScore = 80 // 基线匹配
        } else if (currentHasKey && currentHasValue) {
          matchScore = 80 // 当前匹配
        } else if (baselineHasKey || currentHasKey) {
          matchScore = 50 // 只有key匹配
        } else if (baselineHasValue || currentHasValue) {
          matchScore = 30 // 只有value匹配
        }
      } else if (diff.diffType === 'DELETE') {
        // 对于删除类型，检查基线内容
        const diffKey = diff.diffKey || ''
        const baselineHasKey = diffKey && line.baseline.includes(diffKey)
        const baselineHasValue = diff.baselineValue && line.baseline.includes(diff.baselineValue.trim())
        
        if (line.current.trim() === '') {
          if (baselineHasKey && baselineHasValue) {
            matchScore = 100
          } else if (baselineHasKey || baselineHasValue) {
            matchScore = 50
          }
        }
      } else if (diff.diffType === 'ADD') {
        // 对于新增类型，检查当前内容
        const diffKey = diff.diffKey || ''
        const currentHasKey = diffKey && line.current.includes(diffKey)
        const currentHasValue = diff.currentValue && line.current.includes(diff.currentValue.trim())
        
        if (line.baseline.trim() === '') {
          if (currentHasKey && currentHasValue) {
            matchScore = 100
          } else if (currentHasKey || currentHasValue) {
            matchScore = 50
          }
        }
      }
      
        // 更新最佳匹配
        if (matchScore > bestMatchScore) {
          bestMatchScore = matchScore
          bestMatchIndex = i
        }
      }
    }
    
    // 如果找到匹配，应用后端差异数据
    if (bestMatchIndex >= 0 && bestMatchScore >= 30) {
      const line = alignedLines[bestMatchIndex]
      
      if (diff.diffType === 'MODIFY') {
        // 使用后端数据增强差异信息
        const enhancedDiff = {
          ...diff,
          extendedProperties: {
            inlineDiff: analyzeInlineDiffs(line.baseline, line.current)
          }
        }
        diffLineMap.value.set(bestMatchIndex, enhancedDiff)
      } else {
        diffLineMap.value.set(bestMatchIndex, diff)
      }
      
      matchedIndices.add(bestMatchIndex)
    }
  })
}

// 使用前端算法构建显示
const buildFromFrontendAlgorithm = (baseline, current) => {
  // 使用新的Beyond Compare风格对齐算法
  const { alignedLines, baselineLineNumbers, currentLineNumbers } = beyondCompareAlignSequences(baseline, current)

  baselineLines.value = alignedLines.map(line => line.baseline)
  currentLines.value = alignedLines.map(line => line.current)

  // 存储原始行号信息
  baselineOriginalLineNumbers.value = baselineLineNumbers
  currentOriginalLineNumbers.value = currentLineNumbers

  // 创建差异映射
  diffLineMap.value = new Map()

  alignedLines.forEach((line, index) => {
    let diffItem = null

    if (line.type === 'delete') {
      diffItem = {
        diffType: 'DELETE',
        baselineValue: line.baseline,
        currentValue: '',
        diffKey: line.baseline
      }
    } else if (line.type === 'add') {
      diffItem = {
        diffType: 'ADD',
        baselineValue: '',
        currentValue: line.current,
        diffKey: line.current
      }
    } else if (line.type === 'modify') {
      diffItem = {
        diffType: 'MODIFY',
        baselineValue: line.baseline,
        currentValue: line.current,
        diffKey: line.baseline,
        extendedProperties: {
          inlineDiff: analyzeInlineDiffs(line.baseline, line.current)
        }
      }
    }

    if (diffItem) {
      diffLineMap.value.set(index, diffItem)
    }
  })
}

// 分析行内差异 - 前端实现
const analyzeInlineDiffs = (baselineLine, currentLine) => {
  const diffSegments = []
  let baselinePos = 0
  let currentPos = 0

  // 使用字符级别的diff算法
  const result = computeCharacterDiff(baselineLine, currentLine)

  // 构建差异段
  result.forEach(segment => {
    if (segment.type === 'EQUAL') {
      diffSegments.push({
        type: 'EQUAL',
        baselineText: segment.text,
        currentText: segment.text
      })
      baselinePos += segment.text.length
      currentPos += segment.text.length
    } else if (segment.type === 'DELETE') {
      diffSegments.push({
        type: 'DELETE',
        baselineText: segment.text,
        currentText: ''
      })
      baselinePos += segment.text.length
    } else if (segment.type === 'INSERT') {
      diffSegments.push({
        type: 'ADD',
        baselineText: '',
        currentText: segment.text
      })
      currentPos += segment.text.length
    }
  })

  return {
    diffSegments: diffSegments
  }
}

// 字符级别的差异计算
const computeCharacterDiff = (s1, s2) => {
  const m = s1.length
  const n = s2.length
  const dp = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0))

  // 构建LCS矩阵
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (s1[i - 1] === s2[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
      }
    }
  }

  // 回溯构建差异结果
  const result = []
  let i = m, j = n

  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && s1[i - 1] === s2[j - 1]) {
      // 字符匹配
      result.unshift({ type: 'EQUAL', text: s1[i - 1] })
      i--
      j--
    } else if (i > 0 && (j === 0 || dp[i - 1][j] >= dp[i][j - 1])) {
      // 删除字符
      result.unshift({ type: 'DELETE', text: s1[i - 1] })
      i--
    } else {
      // 插入字符
      result.unshift({ type: 'INSERT', text: s2[j - 1] })
      j--
    }
  }

  // 合并连续的相同类型字符
  const mergedResult = []
  let currentSegment = null

  result.forEach(segment => {
    if (!currentSegment || currentSegment.type !== segment.type) {
      currentSegment = { type: segment.type, text: segment.text }
      mergedResult.push(currentSegment)
    } else {
      currentSegment.text += segment.text
    }
  })

  return mergedResult
}


// 查找匹配的后端差异项
const findMatchingBackendDiff = (line, diffList) => {
  for (const diff of diffList) {
    if ((diff.baselineValue && line.baseline && line.baseline.includes(diff.baselineValue)) ||
        (diff.currentValue && line.current && line.current.includes(diff.currentValue)) ||
        (diff.diffKey && ((line.baseline && line.baseline.includes(diff.diffKey)) ||
                        (line.current && line.current.includes(diff.diffKey))))) {
      return diff
    }
  }
  return null
}

watch(
  () => [props.baselineContent, props.currentContent, props.diffList, props.alignedLines],
  () => buildSideBySideData(),
  { immediate: true }
)

// 【新增】滚动同步功能
const syncScroll = (source, target) => {
  if (isSyncing) return
  isSyncing = true
  
  if (source && target) {
    target.scrollTop = source.scrollTop
    target.scrollLeft = source.scrollLeft
  }
  
  // 使用 requestAnimationFrame 确保滚动完成后再重置标志
  requestAnimationFrame(() => {
    isSyncing = false
  })
}

// 【新增】设置滚动监听器 - 在对话框打开时绑定
watch(visible, (isVisible) => {
  if (isVisible) {
    // 使用 nextTick 确保 DOM 已完全渲染
    setTimeout(() => {
      if (baselineContentRef.value && currentContentRef.value) {
        // 移除旧的监听器（如果存在）
        baselineContentRef.value.onscroll = null
        currentContentRef.value.onscroll = null
        
        // 添加新的监听器
        baselineContentRef.value.onscroll = () => {
          syncScroll(baselineContentRef.value, currentContentRef.value)
        }
        
        currentContentRef.value.onscroll = () => {
          syncScroll(currentContentRef.value, baselineContentRef.value)
        }
        
        console.log('✅ 滚动同步已启用')
      } else {
        console.warn('⚠️ 滚动容器未找到')
      }
    }, 100)
  }
})

const findLineByContent = (baseline, current, diff) => {
  const searchKey = diff.diffKey || diff.baselineValue || diff.currentValue
  if (!searchKey) return

  for (let i = 0; i < baseline.length; i++) {
    if (baseline[i].includes(searchKey)) {
      diffLineMap.value.set(i, diff)
      return
    }
  }

  for (let i = 0; i < current.length; i++) {
    if (current[i].includes(searchKey)) {
      diffLineMap.value.set(i, diff)
      return
    }
  }
}

const getLineClass = (index, side) => {
  const line = side === 'baseline' ? baselineLines.value[index] : currentLines.value[index]
  const diff = diffLineMap.value.get(index)
  
  if (!diff) return side

  if (diff.diffType === 'ADD') {
    // 新增行：右侧绿色背景，左侧斜纹填充
    return side === 'current' ? 'diff-added' : 'baseline-empty'
  }
  if (diff.diffType === 'DELETE') {
    // 删除行：左侧红色背景，右侧斜纹填充
    return side === 'baseline' ? 'diff-removed' : 'current-empty'
  }
  if (diff.diffType === 'MODIFY') {
    // 修改行：两侧黄色背景，差异内容通过行内高亮显示
    return 'diff-modified'
  }
  return side
}

const escapeHtml = text => text
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;')

const highlightLine = (line, index, side) => {
  const diff = diffLineMap.value.get(index)
  if (!diff) {
    return escapeHtml(line || '')
  }

  let prefix = ''
  let content = line || ''

  // 根据差异类型添加前缀符号
  if (diff.diffType === 'ADD' && side === 'current') {
    prefix = '+ '
  } else if (diff.diffType === 'DELETE' && side === 'baseline') {
    prefix = '- '
  } else if (diff.diffType === 'MODIFY') {
    prefix = '~ '
  }

  // 如果有行内差异信息，使用行内高亮
  if (diff.extendedProperties && diff.extendedProperties.inlineDiff) {
    const highlightedContent = highlightInlineDiffs(content, diff.extendedProperties.inlineDiff, side)
    return prefix + highlightedContent
  }

  // 对于ADD和DELETE类型，直接显示整行内容
  return prefix + escapeHtml(content)
}

// 行内差异高亮显示
const highlightInlineDiffs = (text, inlineDiff, side) => {
  if (!inlineDiff || !inlineDiff.diffSegments) {
    return escapeHtml(text)
  }

  let result = ''
  let baselinePos = 0
  let currentPos = 0

  for (const segment of inlineDiff.diffSegments) {
    let segmentText = ''

    switch (segment.type) {
      case 'ADD':
        segmentText = side === 'current' ? segment.currentText : ''
        if (side === 'current' && segmentText) {
          result += `<span class="inline-added">${escapeHtml(segmentText)}</span>`
        }
        currentPos += segment.currentText.length
        break

      case 'DELETE':
        segmentText = side === 'baseline' ? segment.baselineText : ''
        if (side === 'baseline' && segmentText) {
          result += `<span class="inline-removed">${escapeHtml(segmentText)}</span>`
        }
        baselinePos += segment.baselineText.length
        break

      case 'EQUAL':
        segmentText = side === 'baseline' ? segment.baselineText : segment.currentText
        result += escapeHtml(segmentText)
        baselinePos += segment.baselineText.length
        currentPos += segment.currentText.length
        break
    }
  }

  return result
}

const getDiffTypeColor = type => {
  const map = { ADD: 'success', DELETE: 'danger', MODIFY: 'warning' }
  return map[type] || 'info'
}

const getDiffTypeText = type => {
  const map = { ADD: '新增', DELETE: '删除', MODIFY: '修改' }
  return map[type] || '未知'
}

const getSeverityColor = level => {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return map[level] || 'info'
}

const getSeverityText = level => {
  const map = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[level] || '未知'
}
</script>

<style scoped lang="scss">
.diff-analysis {
  .diff-summary {
    margin-bottom: 20px;
  }

  .side-by-side-diff {
    margin-bottom: 20px;

    .diff-header {
      display: flex;
      background: #f8fafc;
      border: 1px solid #e5e7eb;

      .baseline-side,
      .current-side {
        flex: 1;
        padding: 12px 20px;
        border-right: 1px solid #e5e7eb;
        text-align: center;

        &:last-child {
          border-right: none;
        }

        h4 {
          margin: 0 0 4px;
          font-size: 16px;
          color: #1f2937;
        }

        .file-info {
          font-size: 13px;
          color: #6b7280;
        }
      }
    }

    .diff-content {
      display: flex;
      border: 1px solid #e5e7eb;
      border-top: none;
      height: 400px;

      .baseline-content,
      .current-content {
        flex: 1;
        position: relative;
        border-right: 1px solid #ebeef5;
        overflow-y: auto;

        &:last-child {
          border-right: none;
        }

        .line-numbers {
          position: absolute;
          top: 0;
          left: 0;
          width: 50px;
          background-color: #fafafa;
          border-right: 1px solid #ebeef5;
          z-index: 1;

          .line-number {
            height: 20px;
            line-height: 20px;
            padding: 0 10px;
            text-align: right;
            color: #909399;
            font-size: 12px;
            font-family: monospace;
            user-select: none;
            border-bottom: 1px solid #f0f0f0;
            display: flex;
            align-items: center;
            justify-content: flex-end;
          }
        }

        .content-lines {
          padding-left: 50px;

          .content-line {
            height: 20px;
            line-height: 20px;
            padding: 0 10px;
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            font-size: 13px;
            white-space: pre;
            border-bottom: 1px solid #f0f0f0;
            display: flex;
            align-items: center;

            &.diff-added {
              background-color: #d1fae5;
              color: #065f46;
              border-left: 3px solid #10b981;
              font-weight: 500;
            }

            &.diff-removed {
              background-color: #fee2e2;
              color: #991b1b;
              border-left: 3px solid #ef4444;
              font-weight: 500;
            }

            &.diff-modified {
              background-color: #fed7aa;
              color: #92400e;
              border-left: 3px solid #f97316;
              font-weight: 500;
            }

            // 斜纹填充行样式
            &.baseline-empty,
            &.current-empty {
              background-image: repeating-linear-gradient(
                45deg,
                #f8fafc,
                #f8fafc 2px,
                #e5e7eb 2px,
                #e5e7eb 4px
              );
              background-color: #f8fafc;
              position: relative;
            }

            &.baseline-empty::after {
              content: '';
              position: absolute;
              left: 0;
              top: 0;
              right: 0;
              bottom: 0;
              background: rgba(148, 163, 184, 0.1);
            }

            &.current-empty::after {
              content: '';
              position: absolute;
              left: 0;
              top: 0;
              right: 0;
              bottom: 0;
              background: rgba(148, 163, 184, 0.1);
            }

            // 空占位符样式
            :deep(.empty-placeholder) {
              display: inline-block;
              width: 100%;
              height: 100%;
            }

            // 行内差异高亮样式 - 使用红色标注差异内容
            :deep(.inline-added) {
              background-color: #fef2f2;
              color: #dc2626;
              padding: 1px 2px;
              border-radius: 2px;
              border: 1px solid #fca5a5;
            }

            :deep(.inline-removed) {
              background-color: #fef2f2;
              color: #dc2626;
              padding: 1px 2px;
              border-radius: 2px;
              border: 1px solid #fca5a5;
              text-decoration: line-through;
            }

            :deep(.inline-modified) {
              background-color: #fef2f2;
              color: #dc2626;
              padding: 1px 2px;
              border-radius: 2px;
              border: 1px solid #fca5a5;
            }
          }
        }
      }
    }
  }
}
</style>


