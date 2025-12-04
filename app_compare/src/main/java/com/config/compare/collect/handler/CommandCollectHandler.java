package com.config.compare.collect.handler;

import com.config.compare.collect.model.CollectContext;
import com.config.compare.collect.model.CollectResult;
import com.config.compare.entity.ServerInstance;
import com.fasterxml.jackson.databind.JsonNode;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

/**
 * SSH命令采集处理器
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Component
public class CommandCollectHandler extends AbstractCollectHandler {

    @Override
    public String getTypeCode() {
        return "COMMAND";
    }

    @Override
    public String getTypeName() {
        return "SSH命令执行";
    }

    @Override
    public boolean testConnection(CollectContext context) {
        ServerInstance server = context.getServerInstance();
        
        if (!StringUtils.hasText(server.getServerIp()) || server.getSshPort() == null) {
            return false;
        }
        
        JSch jsch = new JSch();
        Session session = null;
        
        try {
            session = jsch.getSession(server.getUsername(), server.getServerIp(), server.getSshPort());
            session.setPassword(server.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            
            // 设置认证方式，优先使用密码认证，避免Kerberos认证问题
            Properties config = new Properties();
            config.put("PreferredAuthentications", "password,publickey");
            config.put("PubkeyAuthentication", "no");
            config.put("GSSAPIAuthentication", "no");
            config.put("KerberosAuthentication", "no");
            session.setConfig(config);
            
            session.setTimeout(30000);
            session.connect();
            
            return session.isConnected();
            
        } catch (Exception e) {
            log.error("SSH连接测试失败：{}", e.getMessage());
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    @Override
    protected CollectResult doCollect(CollectContext context) {
        return executeWithRetry(context, () -> {
            Map<String, Object> params = context.getConfigParams();
            String command = (String) params.get("command");
            
            if (!StringUtils.hasText(command)) {
                return CollectResult.fail("命令参数为空");
            }
            
            return executeCommand(context, command);
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
        return jsonNode.has("command") && StringUtils.hasText(jsonNode.get("command").asText());
    }

    @Override
    public String getConfigSchema() {
        return "{\n" +
               "  \"type\": \"object\",\n" +
               "  \"properties\": {\n" +
               "    \"command\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"要执行的命令\"\n" +
               "    },\n" +
               "    \"workingDir\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"工作目录\"\n" +
               "    },\n" +
               "    \"timeout\": {\n" +
               "      \"type\": \"integer\",\n" +
               "      \"description\": \"超时时间（秒）\",\n" +
               "      \"default\": 60\n" +
               "    }\n" +
               "  },\n" +
               "  \"required\": [\"command\"]\n" +
               "}";
    }

    @Override
    public String getDescription() {
        return "通过SSH连接执行命令采集配置信息，支持各种Shell命令";
    }

    /**
     * 执行SSH命令
     */
    private CollectResult executeCommand(CollectContext context, String command) {
        ServerInstance server = context.getServerInstance();
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        
        try {
            // 建立SSH连接
            session = jsch.getSession(server.getUsername(), server.getServerIp(), server.getSshPort());
            session.setPassword(server.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            
            // 设置认证方式，优先使用密码认证，避免Kerberos认证问题
            Properties config = new Properties();
            config.put("PreferredAuthentications", "password,publickey");
            config.put("PubkeyAuthentication", "no");
            config.put("GSSAPIAuthentication", "no");
            config.put("KerberosAuthentication", "no");
            session.setConfig(config);
            
            session.setTimeout(context.getTimeoutSeconds() * 1000);
            session.connect();
            
            // 创建执行通道
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            // 获取输出流
            InputStream inputStream = channel.getInputStream();
            InputStream errorStream = channel.getErrStream();
            
            channel.connect();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
            
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                // 处理BOM字符（UTF-8 BOM为 \uFEFF）
                if (firstLine && line.length() > 0) {
                    if (line.charAt(0) == '\uFEFF') {
                        line = line.substring(1);
                        log.info("检测到并移除了UTF-8 BOM字符，命令: {}", command);
                    }
                    firstLine = false;
                }
                output.append(line).append("\n");
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            // 等待命令执行完成
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }
            
            int exitCode = channel.getExitStatus();
            
            if (exitCode == 0) {
                // 移除最后多余的换行符
                String result = output.toString();
                if (result.endsWith("\n") && result.length() > 0) {
                    result = result.substring(0, result.length() - 1);
                }
                return CollectResult.success(result);
            } else {
                String error = errorOutput.length() > 0 ? errorOutput.toString() : "命令执行失败，退出码：" + exitCode;
                return CollectResult.fail(error);
            }
            
        } catch (Exception e) {
            log.error("SSH命令执行失败", e);
            return CollectResult.fail("SSH命令执行失败：" + e.getMessage());
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}