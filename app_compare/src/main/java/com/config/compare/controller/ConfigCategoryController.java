package com.config.compare.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.config.compare.common.request.PageRequest;
import com.config.compare.common.result.Result;
import com.config.compare.entity.ConfigCategory;
import com.config.compare.service.ConfigCategoryService;
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
 * 配置分类Controller
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "配置分类管理")
@Validated
public class ConfigCategoryController {

    private final ConfigCategoryService configCategoryService;

    @Operation(summary = "分页查询配置分类")
    @PostMapping("/page")
    public Result<IPage<ConfigCategory>> pageQuery(@Valid @RequestBody PageRequest pageRequest) {
        try {
            IPage<ConfigCategory> result = configCategoryService.pageQuery(pageRequest);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询配置分类失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取所有配置分类")
    @GetMapping
    public Result<List<ConfigCategory>> listAll() {
        try {
            List<ConfigCategory> result = configCategoryService.list();
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取所有配置分类失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取所有启用的配置分类")
    @GetMapping("/enabled")
    public Result<List<ConfigCategory>> listEnabled() {
        try {
            List<ConfigCategory> result = configCategoryService.listEnabled();
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取启用的配置分类失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取配置分类树")
    @GetMapping("/tree")
    public Result<List<ConfigCategory>> getCategoryTree() {
        try {
            List<ConfigCategory> result = configCategoryService.getCategoryTree();
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取配置分类树失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据父分类ID查询子分类")
    @GetMapping("/children/{parentId}")
    public Result<List<ConfigCategory>> listByParentId(@Parameter(description = "父分类ID") @PathVariable Long parentId) {
        try {
            List<ConfigCategory> result = configCategoryService.listByParentId(parentId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询子分类失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据服务器类型ID查询适用的配置分类")
    @GetMapping("/by-server-type/{serverTypeId}")
    public Result<List<ConfigCategory>> listByServerTypeId(@Parameter(description = "服务器类型ID") @PathVariable Long serverTypeId) {
        try {
            List<ConfigCategory> result = configCategoryService.listByServerTypeId(serverTypeId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询适用配置分类失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据系统ID查询适用的配置分类")
    @GetMapping("/by-system/{systemId}")
    public Result<List<ConfigCategory>> listBySystemId(@Parameter(description = "系统ID") @PathVariable Long systemId) {
        try {
            List<ConfigCategory> result = configCategoryService.listBySystemId(systemId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("根据系统ID查询配置分类失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取配置分类")
    @GetMapping("/{id}")
    public Result<ConfigCategory> getById(@Parameter(description = "分类ID") @PathVariable Long id) {
        try {
            ConfigCategory result = configCategoryService.getById(id);
            if (result == null) {
                return Result.error("配置分类不存在");
            }
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("根据ID获取配置分类失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "创建配置分类")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ConfigCategory configCategory) {
        try {
            boolean success = configCategoryService.createCategory(configCategory);
            if (success) {
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建配置分类失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新配置分类")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "分类ID") @PathVariable Long id, 
                               @Valid @RequestBody ConfigCategory configCategory) {
        try {
            configCategory.setId(id);
            boolean success = configCategoryService.updateCategory(configCategory);
            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新配置分类失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除配置分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        try {
            boolean success = configCategoryService.deleteCategory(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除配置分类失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "启用/禁用配置分类")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "分类ID") @PathVariable Long id,
                                     @Parameter(description = "状态") @RequestParam Integer status) {
        try {
            boolean success = configCategoryService.updateStatus(id, status);
            if (success) {
                return Result.success("状态更新成功");
            } else {
                return Result.error("状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新配置分类状态失败", e);
            return Result.error("状态更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "检查分类编码是否存在")
    @GetMapping("/check-code")
    public Result<Boolean> checkCategoryCode(@Parameter(description = "分类编码") @RequestParam String categoryCode,
                                             @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        try {
            boolean exists = configCategoryService.checkCategoryCodeExists(categoryCode, excludeId);
            return Result.success("检查完成", exists);
        } catch (Exception e) {
            log.error("检查分类编码失败", e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }

    @Operation(summary = "检查分类是否有子分类")
    @GetMapping("/{id}/has-children")
    public Result<Boolean> hasChildren(@Parameter(description = "分类ID") @PathVariable Long id) {
        try {
            boolean hasChildren = configCategoryService.hasChildren(id);
            return Result.success("检查完成", hasChildren);
        } catch (Exception e) {
            log.error("检查子分类失败", e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }
}