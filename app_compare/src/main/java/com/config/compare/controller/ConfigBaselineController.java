package com.config.compare.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.config.compare.common.request.BaselinePageRequest;
import com.config.compare.common.result.Result;
import com.config.compare.entity.ConfigBaseline;
import com.config.compare.service.ConfigBaselineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 配置基线Controller
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/baselines")
@RequiredArgsConstructor
@Tag(name = "配置基线管理")
@Validated
public class ConfigBaselineController {

    private final ConfigBaselineService configBaselineService;

    @Operation(summary = "分页查询配置基线")
    @PostMapping("/page")
    public Result<IPage<ConfigBaseline>> pageQuery(@Valid @RequestBody BaselinePageRequest pageRequest) {
        try {
            log.info("收到分页查询请求: systemId={}, serverTypeId={}, categoryId={}, baselineName={}", 
                pageRequest.getSystemId(), pageRequest.getServerTypeId(), 
                pageRequest.getCategoryId(), pageRequest.getBaselineName());
            IPage<ConfigBaseline> result = configBaselineService.pageQuery(pageRequest);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询配置基线失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据系统、服务器类型和配置分类查询基线列表")
    @GetMapping("/list")
    public Result<List<ConfigBaseline>> listBySystemTypeCategory(
            @Parameter(description = "系统ID") @RequestParam Long systemId,
            @Parameter(description = "服务器类型ID") @RequestParam Long serverTypeId,
            @Parameter(description = "配置分类ID") @RequestParam Long categoryId) {
        try {
            List<ConfigBaseline> result = configBaselineService.listBySystemTypeCategory(systemId, serverTypeId, categoryId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询配置基线列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取默认基线")
    @GetMapping("/default")
    public Result<ConfigBaseline> getDefaultBaseline(
            @Parameter(description = "系统ID") @RequestParam Long systemId,
            @Parameter(description = "服务器类型ID") @RequestParam Long serverTypeId,
            @Parameter(description = "配置分类ID") @RequestParam Long categoryId) {
        try {
            ConfigBaseline result = configBaselineService.getDefaultBaseline(systemId, serverTypeId, categoryId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取默认基线失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取版本历史")
    @GetMapping("/version-history")
    public Result<List<ConfigBaseline>> getVersionHistory(
            @Parameter(description = "系统ID") @RequestParam Long systemId,
            @Parameter(description = "服务器类型ID") @RequestParam Long serverTypeId,
            @Parameter(description = "配置分类ID") @RequestParam Long categoryId,
            @Parameter(description = "基线名称") @RequestParam String baselineName) {
        try {
            log.info("查询版本历史: 系统[{}], 服务器类型[{}], 配置分类[{}], 基线名称[{}]", 
                systemId, serverTypeId, categoryId, baselineName);
            List<ConfigBaseline> result = configBaselineService.getVersionHistory(systemId, serverTypeId, categoryId, baselineName);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取版本历史失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取配置基线")
    @GetMapping("/{id}")
    public Result<ConfigBaseline> getById(@Parameter(description = "基线ID") @PathVariable Long id) {
        try {
            ConfigBaseline result = configBaselineService.getById(id);
            if (result == null) {
                return Result.error("配置基线不存在");
            }
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("根据ID获取配置基线失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "创建配置基线")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ConfigBaseline configBaseline) {
        try {
            boolean success = configBaselineService.createBaseline(configBaseline);
            if (success) {
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建配置基线失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新配置基线")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "基线ID") @PathVariable Long id, 
                               @Valid @RequestBody ConfigBaseline configBaseline) {
        try {
            configBaseline.setId(id);
            boolean success = configBaselineService.updateBaseline(configBaseline);
            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新配置基线失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除配置基线")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "基线ID") @PathVariable Long id) {
        try {
            boolean success = configBaselineService.deleteBaseline(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除配置基线失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "设置默认基线")
    @PutMapping("/{id}/set-default")
    public Result<Void> setDefaultBaseline(@Parameter(description = "基线ID") @PathVariable Long id,
                                          @Parameter(description = "切换原因") @RequestParam(required = false) String reason) {
        try {
            boolean success = configBaselineService.setDefaultBaseline(id, reason);
            if (success) {
                return Result.success("设置默认基线成功");
            } else {
                return Result.error("设置默认基线失败");
            }
        } catch (Exception e) {
            log.error("设置默认基线失败", e);
            return Result.error("设置失败：" + e.getMessage());
        }
    }

    @Operation(summary = "复制基线")
    @PostMapping("/{id}/copy")
    public Result<Void> copyBaseline(@Parameter(description = "源基线ID") @PathVariable Long id,
                                    @Parameter(description = "新基线名称") @RequestParam String newName,
                                    @Parameter(description = "新版本号") @RequestParam String newVersion,
                                    @Parameter(description = "描述") @RequestParam(required = false) String description) {
        try {
            boolean success = configBaselineService.copyBaseline(id, newName, newVersion, description);
            if (success) {
                return Result.success("复制基线成功");
            } else {
                return Result.error("复制基线失败");
            }
        } catch (Exception e) {
            log.error("复制基线失败", e);
            return Result.error("复制失败：" + e.getMessage());
        }
    }

    @Operation(summary = "比较基线差异")
    @GetMapping("/compare")
    public Result<String> compareBaselines(@Parameter(description = "基线1 ID") @RequestParam Long baseline1Id,
                                          @Parameter(description = "基线2 ID") @RequestParam Long baseline2Id) {
        try {
            String result = configBaselineService.compareBaselines(baseline1Id, baseline2Id);
            return Result.success("比较完成", result);
        } catch (Exception e) {
            log.error("比较基线失败", e);
            return Result.error("比较失败：" + e.getMessage());
        }
    }

    @Operation(summary = "检查基线名称是否存在")
    @GetMapping("/check-name")
    public Result<Boolean> checkBaselineName(@Parameter(description = "系统ID") @RequestParam Long systemId,
                                            @Parameter(description = "服务器类型ID") @RequestParam Long serverTypeId,
                                            @Parameter(description = "配置分类ID") @RequestParam Long categoryId,
                                            @Parameter(description = "基线名称") @RequestParam String baselineName,
                                            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        try {
            boolean exists = configBaselineService.checkBaselineNameExists(systemId, serverTypeId, categoryId, baselineName, excludeId);
            return Result.success("检查完成", exists);
        } catch (Exception e) {
            log.error("检查基线名称失败", e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }

    @Operation(summary = "晋级采集版本为基线")
    @PostMapping("/promote")
    public Result<ConfigBaseline> promoteToBaseline(
            @Parameter(description = "系统ID") @RequestParam Long systemId,
            @Parameter(description = "服务器类型ID") @RequestParam Long serverTypeId,
            @Parameter(description = "配置分类ID") @RequestParam Long categoryId,
            @Parameter(description = "基线名称") @RequestParam(required = false) String baselineName,
            @Parameter(description = "采集版本内容") @RequestParam String currentContent,
            @Parameter(description = "文件名") @RequestParam(required = false) String fileName,
            @Parameter(description = "描述") @RequestParam(required = false) String description) {
        try {
            log.info("接收到晋级请求: 系统[{}], 服务器类型[{}], 配置分类[{}], 基线名称[{}]", 
                systemId, serverTypeId, categoryId, baselineName);
            
            // 如果未提供 baselineName，从当前默认基线获取
            String finalBaselineName = baselineName;
            if (finalBaselineName == null || finalBaselineName.trim().isEmpty()) {
                ConfigBaseline currentDefault = configBaselineService.getDefaultBaseline(systemId, serverTypeId, categoryId);
                if (currentDefault != null && currentDefault.getBaselineName() != null) {
                    finalBaselineName = currentDefault.getBaselineName();
                    log.info("未提供基线名称，从当前默认基线获取: {}", finalBaselineName);
                } else {
                    // 如果还是没有，自动生成一个
                    finalBaselineName = "基线_" + systemId + "_" + serverTypeId + "_" + categoryId;
                    log.warn("无法获取基线名称，自动生成: {}", finalBaselineName);
                }
            }
            
            ConfigBaseline newBaseline = configBaselineService.promoteToBaseline(
                systemId, serverTypeId, categoryId, finalBaselineName, currentContent, fileName, description);
            return Result.success("晋级成功", newBaseline);
        } catch (Exception e) {
            log.error("晋级为基线失败", e);
            return Result.error("晋级失败：" + e.getMessage());
        }
    }

    @Operation(summary = "切换到指定历史版本")
    @PutMapping("/{id}/switch")
    public Result<Void> switchToVersion(
            @Parameter(description = "基线ID") @PathVariable Long id,
            @Parameter(description = "切换原因") @RequestParam(required = false) String reason) {
        try {
            boolean success = configBaselineService.switchToVersion(id, reason);
            if (success) {
                return Result.success("切换版本成功");
            } else {
                return Result.error("切换版本失败");
            }
        } catch (Exception e) {
            log.error("切换版本失败", e);
            return Result.error("切换失败：" + e.getMessage());
        }
    }
}