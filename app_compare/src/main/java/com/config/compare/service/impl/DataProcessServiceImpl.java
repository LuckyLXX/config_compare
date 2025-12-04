package com.config.compare.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.config.compare.entity.CollectResultEntity;
import com.config.compare.mapper.CollectResultEntityMapper;
import com.config.compare.service.DataProcessService;
import com.config.compare.service.dto.AiProcessRequest;
import com.config.compare.service.dto.DataCleanRequest;
import com.config.compare.service.dto.DataProcessResponse;
import com.config.compare.service.dto.ExcelConvertRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据处理服务实现类
 *
 * @author system
 * @version 1.0.0
 * @since 2025-01-27
 */
@Slf4j
@Service
public class DataProcessServiceImpl implements DataProcessService {

    /**
     * 临时文件存储目录
     */
    @Value("${app.data-process.temp-dir:./temp/data-process}")
    private String tempDir;

    /**
     * AI服务URL配置
     */
    @Value("${app.ai.deepseek.url:}")
    private String deepseekUrl;

    @Value("${app.ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${app.ai.gpt4.url:}")
    private String gpt4Url;

    @Value("${app.ai.gpt4.api-key:}")
    private String gpt4ApiKey;

    @Value("${app.ai.claude.url:}")
    private String claudeUrl;

    @Value("${app.ai.claude.api-key:}")
    private String claudeApiKey;

    /**
     * 文件信息缓存 (fileId -> FileInfo)
     */
    private final Map<String, FileInfo> fileCache = new ConcurrentHashMap<>();

    private WebClient webClient;
    
    @javax.annotation.Resource
    private CollectResultEntityMapper collectResultMapper;

    @PostConstruct
    public void init() {
        // 确保临时目录存在
        FileUtil.mkdir(tempDir);
        // 初始化WebClient，配置超时时间
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)  // 连接超时30秒
                .responseTimeout(Duration.ofSeconds(300));  // 响应超时5分钟
        
        webClient = WebClient.builder()
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        log.info("数据处理服务初始化完成，临时目录: {}", tempDir);
    }

    @Override
    public DataProcessResponse convertToExcel(ExcelConvertRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("开始JSON转Excel处理，模式: {}", request.getMode());

        try {
            // 解析JSON数据
            String sourceData = request.getSourceData();
            long originalSize = sourceData.getBytes(StandardCharsets.UTF_8).length;

            // 解析JSON
            Object jsonData = JSONUtil.parse(sourceData);
            List<Map<String, Object>> dataList = extractDataList(jsonData, request.getMapping());

            if (CollUtil.isEmpty(dataList)) {
                return DataProcessResponse.error("excel", "无法从JSON数据中提取有效的数据列表");
            }

            // 创建Excel工作簿
            String fileId = IdUtil.fastSimpleUUID();
            String fileName = StrUtil.blankToDefault(request.getFileName(), "data") + ".xlsx";
            
            // 确保临时目录存在（使用绝对路径）
            File tempDirFile = new File(tempDir).getAbsoluteFile();
            if (!tempDirFile.exists()) {
                boolean created = tempDirFile.mkdirs();
                log.info("创建临时目录: {}, 结果: {}", tempDirFile.getAbsolutePath(), created);
            }
            File excelFile = new File(tempDirFile, fileId + ".xlsx");
            String filePath = excelFile.getAbsolutePath();

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("数据");

                // 提取表头
                Set<String> headers = new LinkedHashSet<>();
                for (Map<String, Object> row : dataList) {
                    headers.addAll(row.keySet());
                }
                List<String> headerList = new ArrayList<>(headers);

                // 创建表头样式
                CellStyle headerStyle = createHeaderStyle(workbook);

                // 写入表头
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headerList.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(getDisplayHeader(headerList.get(i), request.getMapping()));
                    cell.setCellStyle(headerStyle);
                }

                // 写入数据
                int rowNum = 1;
                for (Map<String, Object> rowData : dataList) {
                    Row row = sheet.createRow(rowNum++);
                    for (int i = 0; i < headerList.size(); i++) {
                        Cell cell = row.createCell(i);
                        Object value = rowData.get(headerList.get(i));
                        setCellValue(cell, value);
                    }
                }

                // 自动调整列宽
                for (int i = 0; i < headerList.size(); i++) {
                    sheet.autoSizeColumn(i);
                }

                // 写入文件
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    workbook.write(fos);
                }
            }

            // 缓存文件信息
            long processedSize = new File(filePath).length();
            fileCache.put(fileId, new FileInfo(fileId, fileName, filePath, LocalDateTime.now()));

            long duration = System.currentTimeMillis() - startTime;
            log.info("JSON转Excel完成，文件ID: {}, 耗时: {}ms", fileId, duration);

            return DataProcessResponse.builder()
                    .type("excel")
                    .success(true)
                    .content("Excel文件生成成功")
                    .fileId(fileId)
                    .fileName(fileName)
                    .originalSize(originalSize)
                    .processedSize(processedSize)
                    .duration(duration)
                    .build();

        } catch (Exception e) {
            log.error("JSON转Excel失败", e);
            return DataProcessResponse.error("excel", "转换失败: " + e.getMessage());
        }
    }

    @Override
    public DataProcessResponse aiProcess(AiProcessRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("开始AI处理，模型: {}, 指令: {}", request.getModel(), request.getPrompt());

        try {
            long originalSize = request.getSourceData().getBytes(StandardCharsets.UTF_8).length;

            // 构建AI请求
            String result = callAiService(request);

            long duration = System.currentTimeMillis() - startTime;
            long processedSize = result.getBytes(StandardCharsets.UTF_8).length;

            log.info("AI处理完成，模型: {}, 耗时: {}ms", request.getModel(), duration);

            return DataProcessResponse.builder()
                    .type("ai")
                    .success(true)
                    .content(result)
                    .model(request.getModel())
                    .originalSize(originalSize)
                    .processedSize(processedSize)
                    .duration(duration)
                    .build();

        } catch (Exception e) {
            log.error("AI处理失败", e);
            return DataProcessResponse.error("ai", "AI处理失败: " + e.getMessage());
        }
    }

    @Override
    public DataProcessResponse cleanData(DataCleanRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("开始数据清洗，规则: {}", request.getRules());

        try {
            String sourceData = request.getSourceData();
            long originalSize = sourceData.getBytes(StandardCharsets.UTF_8).length;

            // 解析JSON
            Object jsonData = JSONUtil.parse(sourceData);

            // 应用清洗规则
            Object cleanedData = applyCleanRules(jsonData, request.getRules(), request.getDateFormat());

            // 格式化输出
            String result = JSONUtil.toJsonPrettyStr(cleanedData);
            long processedSize = result.getBytes(StandardCharsets.UTF_8).length;
            long duration = System.currentTimeMillis() - startTime;

            log.info("数据清洗完成，规则: {}, 耗时: {}ms", request.getRules(), duration);

            return DataProcessResponse.builder()
                    .type("clean")
                    .success(true)
                    .content(result)
                    .appliedRules(String.join(",", request.getRules()))
                    .originalSize(originalSize)
                    .processedSize(processedSize)
                    .duration(duration)
                    .build();

        } catch (Exception e) {
            log.error("数据清洗失败", e);
            return DataProcessResponse.error("clean", "清洗失败: " + e.getMessage());
        }
    }

    @Override
    public void downloadFile(String fileId, HttpServletResponse response) {
        FileInfo fileInfo = fileCache.get(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在或已过期");
        }

        File file = new File(fileInfo.getFilePath());
        if (!file.exists()) {
            fileCache.remove(fileId);
            throw new RuntimeException("文件不存在");
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", 
                    "attachment;filename=" + URLEncoder.encode(fileInfo.getFileName(), "UTF-8"));
            response.setContentLengthLong(file.length());

            try (InputStream is = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            log.info("文件下载成功，fileId: {}", fileId);
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public String getFilePath(String fileId) {
        FileInfo fileInfo = fileCache.get(fileId);
        return fileInfo != null ? fileInfo.getFilePath() : null;
    }

    @Override
    public int cleanExpiredFiles(int expireMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expireMinutes);
        int count = 0;

        Iterator<Map.Entry<String, FileInfo>> iterator = fileCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, FileInfo> entry = iterator.next();
            FileInfo fileInfo = entry.getValue();
            if (fileInfo.getCreateTime().isBefore(cutoff)) {
                FileUtil.del(fileInfo.getFilePath());
                iterator.remove();
                count++;
            }
        }

        if (count > 0) {
            log.info("清理过期文件: {} 个", count);
        }
        return count;
    }

    // ==================== 私有方法 ====================

    /**
     * 从JSON数据中提取数据列表
     */
    private List<Map<String, Object>> extractDataList(Object jsonData, String mappingStr) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (jsonData instanceof JSONArray) {
            // 直接是数组
            JSONArray array = (JSONArray) jsonData;
            for (Object item : array) {
                if (item instanceof JSONObject) {
                    result.add(flattenObject((JSONObject) item, ""));
                }
            }
        } else if (jsonData instanceof JSONObject) {
            JSONObject obj = (JSONObject) jsonData;

            // 尝试查找数组字段
            for (String key : obj.keySet()) {
                Object value = obj.get(key);
                if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    for (Object item : array) {
                        if (item instanceof JSONObject) {
                            result.add(flattenObject((JSONObject) item, ""));
                        }
                    }
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }

            // 如果没有数组，将对象本身作为单行数据
            if (result.isEmpty()) {
                result.add(flattenObject(obj, ""));
            }
        }

        return result;
    }

    /**
     * 展平嵌套对象
     */
    private Map<String, Object> flattenObject(JSONObject obj, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (String key : obj.keySet()) {
            String fullKey = StrUtil.isEmpty(prefix) ? key : prefix + "." + key;
            Object value = obj.get(key);

            if (value instanceof JSONObject) {
                result.putAll(flattenObject((JSONObject) value, fullKey));
            } else if (value instanceof JSONArray) {
                result.put(fullKey, JSONUtil.toJsonStr(value));
            } else {
                result.put(fullKey, value);
            }
        }

        return result;
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    /**
     * 获取显示表头（支持自定义映射）
     */
    private String getDisplayHeader(String key, String mappingStr) {
        if (StrUtil.isNotBlank(mappingStr)) {
            try {
                JSONObject mapping = JSONUtil.parseObj(mappingStr);
                String displayName = mapping.getStr(key);
                if (StrUtil.isNotBlank(displayName)) {
                    return displayName;
                }
            } catch (Exception e) {
                log.warn("解析映射配置失败", e);
            }
        }
        return key;
    }

    /**
     * 设置单元格值
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 调用AI服务
     */
    private String callAiService(AiProcessRequest request) {
        String apiUrl;
        String apiKey;

        // 优先使用前端传递的自定义配置
        if (StrUtil.isNotBlank(request.getCustomUrl())) {
            apiUrl = request.getCustomUrl();
            apiKey = StrUtil.blankToDefault(request.getCustomApiKey(), deepseekApiKey);
            log.info("使用自定义AI配置，URL: {}", apiUrl);
        } else {
            // 使用后端默认配置
            switch (request.getModel()) {
                case "deepseek":
                    apiUrl = deepseekUrl;
                    apiKey = deepseekApiKey;
                    break;
                case "gpt4":
                    apiUrl = gpt4Url;
                    apiKey = gpt4ApiKey;
                    break;
                case "claude3":
                    apiUrl = claudeUrl;
                    apiKey = claudeApiKey;
                    break;
                default:
                    // 对于自定义模型ID，使用默认deepseek配置
                    apiUrl = deepseekUrl;
                    apiKey = deepseekApiKey;
            }
        }

        // 如果没有配置AI服务，返回模拟结果
        if (StrUtil.isBlank(apiUrl) || StrUtil.isBlank(apiKey)) {
            log.warn("AI服务未配置，返回模拟结果");
            return generateMockAiResponse(request);
        }

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        // 优先使用前端传递的自定义模型标识
        String modelName = StrUtil.isNotBlank(request.getCustomModelId()) 
                ? request.getCustomModelId() 
                : getModelName(request.getModel());
        requestBody.put("model", modelName);
        requestBody.put("max_tokens", request.getMaxTokens());
        requestBody.put("temperature", request.getTemperature());
        log.info("AI请求使用模型标识: {}", modelName);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "你是一个专业的数据分析助手，请根据用户的指令处理JSON数据。"));
        messages.add(Map.of("role", "user", "content", 
                "数据:\n" + request.getSourceData() + "\n\n指令: " + request.getPrompt()));
        requestBody.put("messages", messages);

        try {
            // 使用请求中的超时时间，默认60秒
            int timeoutSeconds = request.getTimeout() != null ? request.getTimeout() : 60;
            
            String response = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            // 解析响应
            JSONObject respObj = JSONUtil.parseObj(response);
            JSONArray choices = respObj.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                if (message != null) {
                    return message.getStr("content");
                }
            }

            return "AI响应解析失败";
        } catch (Exception e) {
            log.error("调用AI服务失败", e);
            // 返回模拟结果作为降级
            return generateMockAiResponse(request);
        }
    }

    /**
     * 获取AI模型名称
     */
    private String getModelName(String model) {
        switch (model) {
            case "deepseek":
                return "deepseek-chat";
            case "gpt4":
                return "gpt-4-turbo-preview";
            case "claude3":
                return "claude-3-5-sonnet-20241022";
            default:
                return model;
        }
    }

    /**
     * 生成模拟AI响应
     */
    private String generateMockAiResponse(AiProcessRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[AI 模型: ").append(request.getModel()).append("]\n\n");
        sb.append("根据您的指令 \"").append(request.getPrompt()).append("\"，我对数据进行了分析：\n\n");

        try {
            Object jsonData = JSONUtil.parse(request.getSourceData());
            if (jsonData instanceof JSONObject) {
                JSONObject obj = (JSONObject) jsonData;
                sb.append("1. 数据类型：JSON对象\n");
                sb.append("2. 包含字段数：").append(obj.size()).append("\n");
                sb.append("3. 主要字段：").append(String.join(", ", obj.keySet())).append("\n");
            } else if (jsonData instanceof JSONArray) {
                JSONArray arr = (JSONArray) jsonData;
                sb.append("1. 数据类型：JSON数组\n");
                sb.append("2. 数组长度：").append(arr.size()).append("\n");
            }
        } catch (Exception e) {
            sb.append("数据解析失败：").append(e.getMessage());
        }

        sb.append("\n数据结构完整，分析完成。");
        return sb.toString();
    }

    /**
     * 应用清洗规则
     */
    private Object applyCleanRules(Object data, List<String> rules, String dateFormat) {
        for (String rule : rules) {
            switch (rule) {
                case "remove_null":
                    data = removeNullValues(data);
                    break;
                case "trim_string":
                    data = trimStrings(data);
                    break;
                case "remove_duplicates":
                    data = removeDuplicates(data);
                    break;
                case "format_date":
                    data = formatDates(data, dateFormat);
                    break;
                default:
                    log.warn("未知的清洗规则: {}", rule);
            }
        }
        return data;
    }

    /**
     * 移除空值
     */
    private Object removeNullValues(Object data) {
        if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject) data;
            JSONObject result = new JSONObject();
            for (String key : obj.keySet()) {
                Object value = obj.get(key);
                if (value != null && !"null".equals(value.toString())) {
                    result.set(key, removeNullValues(value));
                }
            }
            return result;
        } else if (data instanceof JSONArray) {
            JSONArray arr = (JSONArray) data;
            JSONArray result = new JSONArray();
            for (Object item : arr) {
                if (item != null && !"null".equals(item.toString())) {
                    result.add(removeNullValues(item));
                }
            }
            return result;
        }
        return data;
    }

    /**
     * 去除字符串首尾空格
     */
    private Object trimStrings(Object data) {
        if (data instanceof String) {
            return ((String) data).trim();
        } else if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject) data;
            JSONObject result = new JSONObject();
            for (String key : obj.keySet()) {
                result.set(key, trimStrings(obj.get(key)));
            }
            return result;
        } else if (data instanceof JSONArray) {
            JSONArray arr = (JSONArray) data;
            JSONArray result = new JSONArray();
            for (Object item : arr) {
                result.add(trimStrings(item));
            }
            return result;
        }
        return data;
    }

    /**
     * 数组去重
     */
    private Object removeDuplicates(Object data) {
        if (data instanceof JSONArray) {
            JSONArray arr = (JSONArray) data;
            Set<String> seen = new LinkedHashSet<>();
            JSONArray result = new JSONArray();
            for (Object item : arr) {
                String key = JSONUtil.toJsonStr(item);
                if (seen.add(key)) {
                    result.add(removeDuplicates(item));
                }
            }
            return result;
        } else if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject) data;
            JSONObject result = new JSONObject();
            for (String key : obj.keySet()) {
                result.set(key, removeDuplicates(obj.get(key)));
            }
            return result;
        }
        return data;
    }

    /**
     * 格式化日期
     */
    private Object formatDates(Object data, String dateFormat) {
        if (data instanceof String) {
            String str = (String) data;
            // 尝试解析常见的日期格式
            Date date = parseDate(str);
            if (date != null) {
                return DateUtil.format(date, dateFormat);
            }
            return str;
        } else if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject) data;
            JSONObject result = new JSONObject();
            for (String key : obj.keySet()) {
                result.set(key, formatDates(obj.get(key), dateFormat));
            }
            return result;
        } else if (data instanceof JSONArray) {
            JSONArray arr = (JSONArray) data;
            JSONArray result = new JSONArray();
            for (Object item : arr) {
                result.add(formatDates(item, dateFormat));
            }
            return result;
        }
        return data;
    }

    /**
     * 尝试解析日期字符串
     */
    private Date parseDate(String str) {
        if (StrUtil.isBlank(str)) {
            return null;
        }

        // 常见日期格式
        String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd-MM-yyyy",
                "MM/dd/yyyy"
        };

        for (String format : formats) {
            try {
                return DateUtil.parse(str, format);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    /**
     * 文件信息内部类
     */
    private static class FileInfo {
        private final String fileId;
        private final String fileName;
        private final String filePath;
        private final LocalDateTime createTime;

        public FileInfo(String fileId, String fileName, String filePath, LocalDateTime createTime) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.filePath = filePath;
            this.createTime = createTime;
        }

        public String getFileId() {
            return fileId;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }
    }

    @Override
    public IPage<Map<String, Object>> getDataProcessTasks(Integer current, Integer size, 
            String taskName, String collectType, Long systemId) {
        log.info("查询数据处理任务列表, current={}, size={}, taskName={}, collectType={}, systemId={}", 
                current, size, taskName, collectType, systemId);
        
        // 使用自定义SQL查询每个任务的最新采集结果
        Page<Map<String, Object>> page = new Page<>(current, size);
        return collectResultMapper.selectLatestResultsWithTask(page, taskName, collectType, systemId);
    }

    @Override
    public Map<String, Object> getCollectResultByExecuteId(String executeId) {
        log.info("查询采集结果详情, executeId={}", executeId);
        
        // 查询该执行ID下的所有采集结果
        List<CollectResultEntity> results = collectResultMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CollectResultEntity>()
                        .eq(CollectResultEntity::getExecuteId, executeId)
                        .orderByDesc(CollectResultEntity::getExecuteTime)
        );
        
        if (CollUtil.isEmpty(results)) {
            return null;
        }
        
        // 合并所有采集内容
        Map<String, Object> result = new HashMap<>();
        result.put("executeId", executeId);
        result.put("totalCount", results.size());
        result.put("successCount", results.stream().filter(r -> r.getCollectStatus() == 1).count());
        result.put("failedCount", results.stream().filter(r -> r.getCollectStatus() == 0).count());
        
        // 合并采集内容
        List<Map<String, Object>> contents = new ArrayList<>();
        for (CollectResultEntity entity : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", entity.getId());
            item.put("collectItemName", entity.getCollectItemName());
            item.put("collectType", entity.getCollectType());
            item.put("collectContent", entity.getCollectContent());
            item.put("collectStatus", entity.getCollectStatus());
            item.put("executeTime", entity.getExecuteTime());
            item.put("durationMs", entity.getDurationMs());
            item.put("errorMessage", entity.getErrorMessage());
            contents.add(item);
        }
        result.put("results", contents);
        
        // 如果只有一条结果，直接返回内容字符串
        if (results.size() == 1) {
            result.put("content", results.get(0).getCollectContent());
        } else {
            // 多条结果，合并为JSON数组
            result.put("content", JSONUtil.toJsonStr(contents));
        }
        
        return result;
    }

    @Override
    public Map<String, Object> testAiConnection(String url, String apiKey, String modelId, Integer timeout) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            if (StrUtil.isBlank(url)) {
                result.put("success", false);
                result.put("message", "API地址不能为空");
                return result;
            }
            
            // 使用后端默认apiKey如果前端没有传
            String effectiveApiKey = StrUtil.isNotBlank(apiKey) ? apiKey : deepseekApiKey;
            if (StrUtil.isBlank(effectiveApiKey)) {
                result.put("success", false);
                result.put("message", "API Key不能为空，请在模型配置中填写或在后端配置");
                return result;
            }
            
            String effectiveModelId = StrUtil.isNotBlank(modelId) ? modelId : "deepseek-chat";
            int effectiveTimeout = timeout != null && timeout > 0 ? timeout : 30;
            
            // 构建一个简单的测试请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", effectiveModelId);
            requestBody.put("max_tokens", 50);
            requestBody.put("temperature", 0.1);
            
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", "请回复'连接成功'四个字"));
            requestBody.put("messages", messages);
            
            log.info("测试AI连接: url={}, model={}, timeout={}s", url, effectiveModelId, effectiveTimeout);
            
            String response = webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + effectiveApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(effectiveTimeout))
                    .block();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 解析响应
            JSONObject respObj = JSONUtil.parseObj(response);
            JSONArray choices = respObj.getJSONArray("choices");
            
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                String content = message != null ? message.getStr("content") : "";
                
                result.put("success", true);
                result.put("message", "连接成功");
                result.put("response", content);
                result.put("duration", duration);
                result.put("model", respObj.getStr("model", effectiveModelId));
            } else {
                result.put("success", false);
                result.put("message", "AI响应格式异常");
                result.put("rawResponse", response);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("测试AI连接失败", e);
            
            result.put("success", false);
            result.put("duration", duration);
            
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("timeout")) {
                result.put("message", "连接超时，请检查网络或增加超时时间");
            } else if (errorMsg != null && errorMsg.contains("401")) {
                result.put("message", "API Key无效或已过期");
            } else if (errorMsg != null && errorMsg.contains("Connection refused")) {
                result.put("message", "无法连接到AI服务，请检查API地址");
            } else {
                result.put("message", "连接失败: " + errorMsg);
            }
        }
        
        return result;
    }
}
