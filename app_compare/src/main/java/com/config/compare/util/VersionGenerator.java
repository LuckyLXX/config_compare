package com.config.compare.util;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Predicate;

/**
 * 基线版本号生成工具类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
public class VersionGenerator {

    private static final String VERSION_PREFIX = "V";
    private static final String VERSION_DATE_FORMAT = "yyyyMMddHHmmss";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(VERSION_DATE_FORMAT);
    
    /**
     * 生成基于时间戳的版本号
     * 格式: V + yyyyMMddHHmmss
     * 例如: V20251104183033
     * 
     * @return 生成的版本号
     */
    public static String generateVersion() {
        String timestamp = DATE_FORMAT.format(new Date());
        String version = VERSION_PREFIX + timestamp;
        log.debug("生成版本号: {}", version);
        return version;
    }
    
    /**
     * 生成基于时间戳的版本号，如果已存在则追加序号
     * 格式: V + yyyyMMddHHmmss 或 V + yyyyMMddHHmmss_01
     * 
     * @param existsChecker 版本号存在性检查函数，返回true表示版本号已存在
     * @return 生成的唯一版本号
     */
    public static String generateUniqueVersion(Predicate<String> existsChecker) {
        String baseVersion = generateVersion();
        
        // 如果基础版本号不存在，直接返回
        if (!existsChecker.test(baseVersion)) {
            return baseVersion;
        }
        
        // 如果已存在，追加序号
        int suffix = 1;
        String versionWithSuffix;
        do {
            versionWithSuffix = String.format("%s_%02d", baseVersion, suffix);
            suffix++;
            
            // 防止无限循环，最多尝试99次
            if (suffix > 99) {
                log.warn("版本号生成失败，已达到最大重试次数: {}", baseVersion);
                throw new RuntimeException("版本号生成失败，请稍后重试");
            }
        } while (existsChecker.test(versionWithSuffix));
        
        log.debug("生成唯一版本号: {} (基础版本已存在)", versionWithSuffix);
        return versionWithSuffix;
    }
    
    /**
     * 验证版本号格式是否正确
     * 
     * @param version 待验证的版本号
     * @return true表示格式正确，false表示格式错误
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        
        // 格式1: V + 14位数字
        if (version.matches("^V\\d{14}$")) {
            return true;
        }
        
        // 格式2: V + 14位数字 + _ + 2位数字
        if (version.matches("^V\\d{14}_\\d{2}$")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 比较两个版本号的大小
     * 
     * @param version1 版本号1
     * @param version2 版本号2
     * @return 负数表示version1 < version2，0表示相等，正数表示version1 > version2
     */
    public static int compareVersion(String version1, String version2) {
        if (!isValidVersion(version1) || !isValidVersion(version2)) {
            throw new IllegalArgumentException("版本号格式不正确");
        }
        
        // 去掉前缀V进行比较
        String v1 = version1.substring(1);
        String v2 = version2.substring(1);
        
        return v1.compareTo(v2);
    }
}

