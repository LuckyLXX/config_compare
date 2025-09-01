package com.config.compare.collect.manager;

import com.config.compare.collect.handler.CollectHandler;
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
 * 采集处理器管理器
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Component
public class CollectHandlerManager implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final Map<String, CollectHandler> handlerMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        // 从Spring容器中获取所有采集处理器
        Map<String, CollectHandler> handlers = applicationContext.getBeansOfType(CollectHandler.class);
        
        for (CollectHandler handler : handlers.values()) {
            String typeCode = handler.getTypeCode();
            if (handlerMap.containsKey(typeCode)) {
                log.warn("发现重复的采集类型编码：{}，原处理器：{}，新处理器：{}", 
                         typeCode, handlerMap.get(typeCode).getClass().getName(), handler.getClass().getName());
            }
            handlerMap.put(typeCode, handler);
            log.info("注册采集处理器：{} -> {}", typeCode, handler.getClass().getName());
        }
        
        log.info("采集处理器管理器初始化完成，共注册{}个处理器", handlerMap.size());
    }

    /**
     * 根据类型编码获取处理器
     * 
     * @param typeCode 类型编码
     * @return 采集处理器
     */
    public CollectHandler getHandler(String typeCode) {
        return handlerMap.get(typeCode);
    }

    /**
     * 检查处理器是否存在
     * 
     * @param typeCode 类型编码
     * @return 是否存在
     */
    public boolean hasHandler(String typeCode) {
        return handlerMap.containsKey(typeCode);
    }

    /**
     * 获取所有处理器
     * 
     * @return 所有处理器
     */
    public Collection<CollectHandler> getAllHandlers() {
        return handlerMap.values();
    }

    /**
     * 获取所有支持的类型编码
     * 
     * @return 类型编码集合
     */
    public Collection<String> getSupportedTypeCodes() {
        return handlerMap.keySet();
    }

    /**
     * 动态注册处理器
     * 
     * @param handler 采集处理器
     */
    public void registerHandler(CollectHandler handler) {
        String typeCode = handler.getTypeCode();
        handlerMap.put(typeCode, handler);
        log.info("动态注册采集处理器：{} -> {}", typeCode, handler.getClass().getName());
    }

    /**
     * 注销处理器
     * 
     * @param typeCode 类型编码
     */
    public void unregisterHandler(String typeCode) {
        CollectHandler removed = handlerMap.remove(typeCode);
        if (removed != null) {
            log.info("注销采集处理器：{} -> {}", typeCode, removed.getClass().getName());
        }
    }
}