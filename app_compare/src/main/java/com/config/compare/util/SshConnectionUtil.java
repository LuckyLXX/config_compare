package com.config.compare.util;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * SSH连接工具类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-26
 */
@Slf4j
public class SshConnectionUtil {

    private static final int DEFAULT_TIMEOUT = 10000; // 10秒超时

    /**
     * 测试SSH连接
     * 
     * @param host SSH服务器地址
     * @param port SSH端口
     * @param username 用户名
     * @param password 密码
     * @return 连接测试结果
     */
    public static boolean testSshConnection(String host, Integer port, String username, String password) {
        if (!StringUtils.hasText(host) || !StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("SSH连接参数不完整: host={}, port={}, username={}", host, port, username);
            return false;
        }

        JSch jsch = new JSch();
        Session session = null;
        
        try {
            // 创建会话
            session = jsch.getSession(username, host, port != null ? port : 22);
            session.setPassword(password);

            // 设置连接属性
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // 不检查主机密钥
            config.put("UserKnownHostsFile", "/dev/null"); // 不保存已知主机
            config.put("PreferredAuthentications", "password,publickey"); // 优先使用密码认证
            config.put("PubkeyAuthentication", "no"); // 禁用公钥认证
            session.setConfig(config);


            // 设置超时时间
            session.setTimeout(DEFAULT_TIMEOUT);
            
            // 尝试连接
            log.info("正在测试SSH连接: {}@{}:{}", username, host, port);
            session.connect();
            
            if (session.isConnected()) {
                log.info("SSH连接测试成功: {}@{}:{}", username, host, port);
                return true;
            } else {
                log.warn("SSH连接测试失败: 连接未建立");
                return false;
            }
            
        } catch (JSchException e) {
            String errorMsg = e.getMessage();
            String errorType = "其他错误";
            
            // 检查是否为Kerberos相关错误
            if (errorMsg != null && errorMsg.contains("205084")) {
                errorType = "Kerberos认证失败";
                log.error("SSH连接测试失败: {}@{}:{}, 错误类型: {}, 详细信息: {}",
                    username, host, port, errorType, errorMsg);
            } else {
                log.error("SSH连接测试失败: {}@{}:{}, 错误: {}", username, host, port, errorMsg);
            }
            return false;
        } catch (Exception e) {
            log.error("SSH连接测试异常: {}@{}:{}", username, host, port, e);
            return false;
        } finally {
            // 关闭连接
            if (session != null && session.isConnected()) {
                session.disconnect();
                log.debug("SSH会话已关闭");
            }
        }
    }

    /**
     * 测试SSH连接（带重试机制）
     * 
     * @param host SSH服务器地址
     * @param port SSH端口
     * @param username 用户名
     * @param password 密码
     * @param retryCount 重试次数
     * @return 连接测试结果
     */
    public static boolean testSshConnectionWithRetry(String host, Integer port, String username, String password, int retryCount) {
        for (int i = 0; i <= retryCount; i++) {
            if (i > 0) {
                log.info("SSH连接测试重试 {}/{}: {}@{}:{}", i, retryCount, username, host, port);
            }
            
            boolean result = testSshConnection(host, port, username, password);
            if (result) {
                return true;
            }
            
            if (i < retryCount) {
                try {
                    Thread.sleep(1000); // 等待1秒后重试
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.error("SSH连接测试失败，已重试{}次: {}@{}:{}", retryCount, username, host, port);
        return false;
    }
}
