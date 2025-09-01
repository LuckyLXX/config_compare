package com.config.compare.compare.manager;

import com.config.compare.compare.algorithm.CompareAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 比对算法管理器
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Component
public class CompareAlgorithmManager implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final Map<String, CompareAlgorithm> algorithmMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        // 从Spring容器中获取所有比对算法
        Map<String, CompareAlgorithm> algorithms = applicationContext.getBeansOfType(CompareAlgorithm.class);
        
        for (CompareAlgorithm algorithm : algorithms.values()) {
            String algorithmType = algorithm.getAlgorithmType();
            if (algorithmMap.containsKey(algorithmType)) {
                log.warn("发现重复的比对算法类型：{}，原算法：{}，新算法：{}", 
                         algorithmType, algorithmMap.get(algorithmType).getClass().getName(), algorithm.getClass().getName());
            }
            algorithmMap.put(algorithmType, algorithm);
            log.info("注册比对算法：{} -> {}", algorithmType, algorithm.getClass().getName());
        }
        
        log.info("比对算法管理器初始化完成，共注册{}个算法", algorithmMap.size());
    }

    /**
     * 根据算法类型获取算法
     * 
     * @param algorithmType 算法类型
     * @return 比对算法
     */
    public CompareAlgorithm getAlgorithm(String algorithmType) {
        return algorithmMap.get(algorithmType);
    }

    /**
     * 根据内容类型智能选择算法
     * 
     * @param contentType 内容类型
     * @return 比对算法
     */
    public CompareAlgorithm getAlgorithmByContentType(String contentType) {
        if (contentType == null) {
            // 默认使用文本算法
            return algorithmMap.get("TEXT");
        }
        
        // 查找支持该内容类型的算法
        for (CompareAlgorithm algorithm : algorithmMap.values()) {
            if (algorithm.supports(contentType)) {
                return algorithm;
            }
        }
        
        // 如果没有找到，返回文本算法作为默认
        return algorithmMap.get("TEXT");
    }

    /**
     * 检查算法是否存在
     * 
     * @param algorithmType 算法类型
     * @return 是否存在
     */
    public boolean hasAlgorithm(String algorithmType) {
        return algorithmMap.containsKey(algorithmType);
    }

    /**
     * 获取所有算法
     * 
     * @return 所有算法
     */
    public Collection<CompareAlgorithm> getAllAlgorithms() {
        return algorithmMap.values();
    }

    /**
     * 获取所有支持的算法类型
     * 
     * @return 算法类型集合
     */
    public Collection<String> getSupportedAlgorithmTypes() {
        return algorithmMap.keySet();
    }

    /**
     * 动态注册算法
     * 
     * @param algorithm 比对算法
     */
    public void registerAlgorithm(CompareAlgorithm algorithm) {
        String algorithmType = algorithm.getAlgorithmType();
        algorithmMap.put(algorithmType, algorithm);
        log.info("动态注册比对算法：{} -> {}", algorithmType, algorithm.getClass().getName());
    }

    /**
     * 注销算法
     * 
     * @param algorithmType 算法类型
     */
    public void unregisterAlgorithm(String algorithmType) {
        CompareAlgorithm removed = algorithmMap.remove(algorithmType);
        if (removed != null) {
            log.info("注销比对算法：{} -> {}", algorithmType, removed.getClass().getName());
        }
    }
}