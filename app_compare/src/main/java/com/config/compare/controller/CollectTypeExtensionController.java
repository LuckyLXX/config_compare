package com.config.compare.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.config.compare.collect.handler.CollectHandler;
import com.config.compare.collect.manager.CollectHandlerManager;
import com.config.compare.common.request.PageRequest;
import com.config.compare.common.result.Result;
import com.config.compare.entity.CollectTypeExtension;
import com.config.compare.service.CollectTypeExtensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采集类型扩展Controller
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/collect-type-extensions")
@RequiredArgsConstructor
@Tag(name = "采集类型扩展管理")
@Validated
public class CollectTypeExtensionController {

    private final CollectTypeExtensionService collectTypeExtensionService;
    private final CollectHandlerManager handlerManager;

    @Operation(summary = "分页查询采集类型扩展")
    @PostMapping("/page")
    public Result<IPage<CollectTypeExtension>> pageQuery(@Valid @RequestBody PageRequest pageRequest) {
        try {
            IPage<CollectTypeExtension> result = collectTypeExtensionService.pageQuery(pageRequest);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询采集类型扩展失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据类型分类查询采集类型")
    @GetMapping("/by-category/{typeCategory}")
    public Result<List<CollectTypeExtension>> listByTypeCategory(@Parameter(description = "类型分类") @PathVariable String typeCategory) {
        try {
            List<CollectTypeExtension> result = collectTypeExtensionService.listByTypeCategory(typeCategory);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询采集类型失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取所有启用的采集类型")
    @GetMapping("/enabled")
    public Result<List<CollectTypeExtension>> listEnabledTypes() {
        try {
            List<CollectTypeExtension> result = collectTypeExtensionService.listEnabledTypes();
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取启用的采集类型失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据类型编码获取采集类型")
    @GetMapping("/by-code/{typeCode}")
    public Result<CollectTypeExtension> getByTypeCode(@Parameter(description = "类型编码") @PathVariable String typeCode) {
        try {
            CollectTypeExtension result = collectTypeExtensionService.getByTypeCode(typeCode);
            if (result == null) {
                return Result.error("采集类型不存在");
            }
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("根据类型编码获取采集类型失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取采集类型扩展")
    @GetMapping("/{id}")
    public Result<CollectTypeExtension> getById(@Parameter(description = "类型ID") @PathVariable Long id) {
        try {
            CollectTypeExtension result = collectTypeExtensionService.getById(id);
            if (result == null) {
                return Result.error("采集类型扩展不存在");
            }
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("根据ID获取采集类型扩展失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "创建采集类型扩展")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CollectTypeExtension collectTypeExtension) {
        try {
            boolean success = collectTypeExtensionService.createTypeExtension(collectTypeExtension);
            if (success) {
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建采集类型扩展失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新采集类型扩展")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "类型ID") @PathVariable Long id, 
                               @Valid @RequestBody CollectTypeExtension collectTypeExtension) {
        try {
            collectTypeExtension.setId(id);
            boolean success = collectTypeExtensionService.updateTypeExtension(collectTypeExtension);
            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新采集类型扩展失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除采集类型扩展")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "类型ID") @PathVariable Long id) {
        try {
            boolean success = collectTypeExtensionService.deleteTypeExtension(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除采集类型扩展失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "启用/禁用采集类型")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "类型ID") @PathVariable Long id,
                                     @Parameter(description = "状态") @RequestParam Integer status) {
        try {
            boolean success = collectTypeExtensionService.updateStatus(id, status);
            if (success) {
                return Result.success("状态更新成功");
            } else {
                return Result.error("状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新采集类型状态失败", e);
            return Result.error("状态更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "检查类型编码是否存在")
    @GetMapping("/check-code")
    public Result<Boolean> checkTypeCode(@Parameter(description = "类型编码") @RequestParam String typeCode,
                                         @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        try {
            boolean exists = collectTypeExtensionService.checkTypeCodeExists(typeCode, excludeId);
            return Result.success("检查完成", exists);
        } catch (Exception e) {
            log.error("检查类型编码失败", e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }

    @Operation(summary = "同步处理器信息到数据库")
    @PostMapping("/sync-handlers")
    public Result<Void> syncHandlers() {
        try {
            boolean success = collectTypeExtensionService.syncHandlersToDatabase();
            if (success) {
                return Result.success("同步成功");
            } else {
                return Result.error("同步失败");
            }
        } catch (Exception e) {
            log.error("同步处理器信息失败", e);
            return Result.error("同步失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取所有注册的处理器信息")
    @GetMapping("/handlers")
    public Result<Map<String, Object>> getAllHandlers() {
        try {
            Collection<CollectHandler> handlers = handlerManager.getAllHandlers();
            Map<String, Object> result = new HashMap<>();
            
            for (CollectHandler handler : handlers) {
                Map<String, Object> handlerInfo = new HashMap<>();
                handlerInfo.put("typeCode", handler.getTypeCode());
                handlerInfo.put("typeName", handler.getTypeName());
                handlerInfo.put("description", handler.getDescription());
                handlerInfo.put("configSchema", handler.getConfigSchema());
                handlerInfo.put("handlerClass", handler.getClass().getName());
                
                result.put(handler.getTypeCode(), handlerInfo);
            }
            
            return Result.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取处理器信息失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取处理器配置架构")
    @GetMapping("/{typeCode}/config-schema")
    public Result<String> getConfigSchema(@Parameter(description = "类型编码") @PathVariable String typeCode) {
        try {
            CollectHandler handler = collectTypeExtensionService.getHandler(typeCode);
            if (handler == null) {
                return Result.error("找不到指定的采集类型处理器");
            }
            
            String schema = handler.getConfigSchema();
            return Result.success("获取成功", schema);
        } catch (Exception e) {
            log.error("获取配置架构失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    @Operation(summary = "测试采集类型连接")
    @PostMapping("/{typeCode}/test-connection")
    public Result<Boolean> testConnection(@Parameter(description = "类型编码") @PathVariable String typeCode,
                                         @Parameter(description = "配置参数JSON") @RequestBody(required = false) String config) {
        try {
            boolean success = collectTypeExtensionService.testTypeConnection(typeCode, config);
            return Result.success(success ? "连接测试成功" : "连接测试失败", success);
        } catch (Exception e) {
            log.error("测试采集类型连接失败", e);
            return Result.error("连接测试失败：" + e.getMessage());
        }
    }
}