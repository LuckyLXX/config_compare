package com.config.compare.collect.handler;

import com.config.compare.collect.model.CollectContext;
import com.config.compare.collect.model.CollectResult;
import com.config.compare.entity.ServerInstance;
import com.fasterxml.jackson.databind.JsonNode;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * SFTP文件采集处理器
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Component
public class FileCollectHandler extends AbstractCollectHandler {

    @Override
    public String getTypeCode() {
        return "FILE";
    }

    @Override
    public String getTypeName() {
        return "SFTP文件下载";
    }

    @Override
    public boolean testConnection(CollectContext context) {
        ServerInstance server = context.getServerInstance();
        
        if (!StringUtils.hasText(server.getServerIp()) || server.getSshPort() == null) {
            return false;
        }
        
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;
        
        try {
            session = jsch.getSession(server.getUsername(), server.getServerIp(), server.getSshPort());
            session.setPassword(server.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(30000);
            session.connect();
            
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            
            return sftpChannel.isConnected();
            
        } catch (Exception e) {
            log.error("SFTP连接测试失败：{}", e.getMessage());
            return false;
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    @Override
    protected CollectResult doCollect(CollectContext context) {
        return executeWithRetry(context, () -> {
            Map<String, Object> params = context.getConfigParams();
            String filePath = (String) params.get("filePath");
            
            if (!StringUtils.hasText(filePath)) {
                return CollectResult.fail("文件路径参数为空");
            }
            
            return downloadFile(context, filePath);
        });
    }

    @Override
    protected boolean validateSpecificContext(CollectContext context) {
        ServerInstance server = context.getServerInstance();
        
        if (!StringUtils.hasText(server.getServerIp())) {
            log.error("服务器IP为空");
            return false;
        }
        
        if (server.getSshPort() == null) {
            log.error("SSH端口为空");
            return false;
        }
        
        if (!StringUtils.hasText(server.getUsername())) {
            log.error("SSH用户名为空");
            return false;
        }
        
        if (!StringUtils.hasText(server.getPassword())) {
            log.error("SSH密码为空");
            return false;
        }
        
        return true;
    }

    @Override
    protected boolean validateConfigJson(JsonNode jsonNode) {
        return jsonNode.has("filePath") && StringUtils.hasText(jsonNode.get("filePath").asText());
    }

    @Override
    public String getConfigSchema() {
        return "{\n" +
               "  \"type\": \"object\",\n" +
               "  \"properties\": {\n" +
               "    \"filePath\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"要下载的文件路径\"\n" +
               "    },\n" +
               "    \"encoding\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"文件编码\",\n" +
               "      \"default\": \"UTF-8\"\n" +
               "    },\n" +
               "    \"maxSize\": {\n" +
               "      \"type\": \"integer\",\n" +
               "      \"description\": \"最大文件大小（字节）\",\n" +
               "      \"default\": 10485760\n" +
               "    }\n" +
               "  },\n" +
               "  \"required\": [\"filePath\"]\n" +
               "}";
    }

    @Override
    public String getDescription() {
        return "通过SFTP协议下载远程服务器上的文件内容";
    }

    /**
     * 下载文件
     */
    private CollectResult downloadFile(CollectContext context, String filePath) {
        ServerInstance server = context.getServerInstance();
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;
        
        try {
            // 建立SSH连接
            session = jsch.getSession(server.getUsername(), server.getServerIp(), server.getSshPort());
            session.setPassword(server.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(context.getTimeoutSeconds() * 1000);
            session.connect();
            
            // 创建SFTP通道
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            
            // 获取文件配置参数
            Map<String, Object> params = context.getConfigParams();
            String encoding = (String) params.getOrDefault("encoding", "UTF-8");
            int maxSize = (Integer) params.getOrDefault("maxSize", 10 * 1024 * 1024); // 10MB
            
            // 检查文件是否存在
            try {
                sftpChannel.stat(filePath);
            } catch (Exception e) {
                return CollectResult.fail("文件不存在：" + filePath);
            }
            
            // 检查文件大小
            long fileSize = sftpChannel.stat(filePath).getSize();
            if (fileSize > maxSize) {
                return CollectResult.fail("文件过大：" + fileSize + " 字节，最大允许：" + maxSize + " 字节");
            }
            
            // 下载文件内容
            InputStream inputStream = sftpChannel.get(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding));
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            CollectResult result = CollectResult.success(content.toString());
            result.setFilePath(filePath);
            
            return result;
            
        } catch (Exception e) {
            log.error("SFTP文件下载失败", e);
            return CollectResult.fail("SFTP文件下载失败：" + e.getMessage());
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}