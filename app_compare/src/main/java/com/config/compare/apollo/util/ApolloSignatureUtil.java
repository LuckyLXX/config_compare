package com.config.compare.apollo.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Apollo签名工具类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
public class ApolloSignatureUtil {

    private static final String AUTHORIZATION_FORMAT = "Apollo %s:%s";
    private static final String DELIMITER = "\n";
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    
    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    public static final String HTTP_HEADER_TIMESTAMP = "Timestamp";

    /**
     * 生成签名
     */
    public static String signature(String timestamp, String pathWithQuery, String secret) {
        String stringToSign = timestamp + DELIMITER + pathWithQuery;
        return hmacSha1(stringToSign, secret);
    }

    /**
     * 构建HTTP请求头
     */
    public static Map<String, String> buildHttpHeaders(String url, String appId, String secret) {
        long currentTimeMillis = System.currentTimeMillis();
        String timestamp = String.valueOf(currentTimeMillis);

        String pathWithQuery = url2PathWithQuery(url);
        String signature = signature(timestamp, pathWithQuery, secret);

        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP_HEADER_AUTHORIZATION, String.format(AUTHORIZATION_FORMAT, appId, signature));
        headers.put(HTTP_HEADER_TIMESTAMP, timestamp);
        
        log.debug("构建Apollo认证头部 - URL: {}, AppId: {}, Timestamp: {}", url, appId, timestamp);
        
        return headers;
    }

    /**
     * 从URL提取路径和查询参数
     */
    private static String url2PathWithQuery(String urlString) {
        try {
            URL url = new URL(urlString);
            String path = url.getPath();
            String query = url.getQuery();

            String pathWithQuery = path;
            if (query != null && query.length() > 0) {
                pathWithQuery += "?" + query;
            }
            return pathWithQuery;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid url pattern: " + urlString, e);
        }
    }

    /**
     * HMAC-SHA1签名
     */
    private static String hmacSha1(String data, String key) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
}