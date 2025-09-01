package com.config.compare.util;

import com.config.compare.entity.ServerInstance;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * SSH连接工具类
 */
@Slf4j
public class SSHUtil {
    
    private static final int DEFAULT_TIMEOUT = 60000; // 60秒超时
    
    /**
     * 执行SSH命令
     */
    public static String executeCommand(ServerInstance server, String command, Integer timeoutSeconds) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        
        try {
            // 创建SSH会话
            session = jsch.getSession(server.getUsername(), server.getServerIp(), server.getSshPort());
            session.setPassword(server.getPassword());
            
            // 跳过主机密钥检查
            session.setConfig("StrictHostKeyChecking", "no");
            
            // 设置超时
            int timeout = timeoutSeconds != null ? timeoutSeconds * 1000 : DEFAULT_TIMEOUT;
            session.setTimeout(timeout);
            
            log.info("正在连接SSH服务器: {}@{}:{}", server.getUsername(), server.getServerIp(), server.getSshPort());
            session.connect();
            log.info("SSH连接建立成功");
            
            // 创建命令执行通道
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            // 获取输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            
            channel.setOutputStream(outputStream);
            channel.setErrStream(errorStream);
            
            // 执行命令
            log.info("执行命令: {}", command);
            channel.connect();
            
            // 等待命令执行完成
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("命令执行被中断", e);
                }
            }
            
            // 获取执行结果
            String output = outputStream.toString("UTF-8");
            String error = errorStream.toString("UTF-8");
            int exitCode = channel.getExitStatus();
            
            log.info("命令执行完成，退出码: {}", exitCode);
            
            // 构建结果
            StringBuilder result = new StringBuilder();
            result.append("命令: ").append(command).append("\n");
            result.append("退出码: ").append(exitCode).append("\n");
            result.append("执行时间: ").append(System.currentTimeMillis()).append("\n\n");
            
            if (!output.trim().isEmpty()) {
                result.append("标准输出:\n").append(output).append("\n");
            }
            
            if (!error.trim().isEmpty()) {
                result.append("错误输出:\n").append(error).append("\n");
            }
            
            if (exitCode != 0 && output.trim().isEmpty()) {
                throw new RuntimeException("命令执行失败，退出码: " + exitCode + 
                    (error.trim().isEmpty() ? "" : "，错误信息: " + error));
            }
            
            return result.toString();
            
        } catch (JSchException e) {
            log.error("SSH连接失败", e);
            throw new RuntimeException("SSH连接失败: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("命令执行失败", e);
            throw new RuntimeException("命令执行失败: " + e.getMessage(), e);
        } finally {
            // 关闭连接
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            log.info("SSH连接已关闭");
        }
    }
    
    /**
     * 测试SSH连接
     */
    public static boolean testConnection(ServerInstance server) {
        try {
            String result = executeCommand(server, "echo 'SSH连接测试成功'", 30);
            return result.contains("SSH连接测试成功");
        } catch (Exception e) {
            log.error("SSH连接测试失败", e);
            return false;
        }
    }
}
