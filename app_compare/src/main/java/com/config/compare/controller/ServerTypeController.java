package com.config.compare.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.config.compare.common.request.PageRequest;
import com.config.compare.common.result.Result;
import com.config.compare.entity.ServerType;
import com.config.compare.service.ServerTypeService;
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
 * 服务器类型Controller
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/server-types")
@RequiredArgsConstructor
@Tag(name = "服务器类型管理")
@Validated
public class ServerTypeController {

    private final ServerTypeService serverTypeService;

    @Operation(summary = "分页查询服务器类型")
    @PostMapping("/page")
    public Result<IPage<ServerType>> pageQuery(@Valid @RequestBody PageRequest pageRequest) {
        try {
            IPage<ServerType> result = serverTypeService.pageQuery(pageRequest);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询服务器类型失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取所有启用的服务器类型")
    @GetMapping("/enabled")
    public Result<List<ServerType>> listEnabled() {
        try {
            List<ServerType> result = serverTypeService.listEnabled();
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取启用的服务器类型失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取所有服务器类型")
    @GetMapping("/list")
    public Result<List<ServerType>> listAll() {
        try {
            List<ServerType> result = serverTypeService.list();
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取所有服务器类型失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取服务器类型")
    @GetMapping("/{id}")
    public Result<ServerType> getById(@Parameter(description = "服务器类型ID") @PathVariable Long id) {
        try {
            ServerType result = serverTypeService.getById(id);
            if (result == null) {
                return Result.error("服务器类型不存在");
            }
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("根据ID获取服务器类型失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "创建服务器类型")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ServerType serverType) {
        try {
            boolean success = serverTypeService.createServerType(serverType);
            if (success) {
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建服务器类型失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新服务器类型")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "服务器类型ID") @PathVariable Long id, 
                               @Valid @RequestBody ServerType serverType) {
        try {
            serverType.setId(id);
            boolean success = serverTypeService.updateServerType(serverType);
            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新服务器类型失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除服务器类型")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "服务器类型ID") @PathVariable Long id) {
        try {
            boolean success = serverTypeService.deleteServerType(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除服务器类型失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "检查类型编码是否存在")
    @GetMapping("/check-code")
    public Result<Boolean> checkTypeCode(@Parameter(description = "类型编码") @RequestParam String typeCode,
                                         @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        try {
            boolean exists = serverTypeService.checkTypeCodeExists(typeCode, excludeId);
            return Result.success("检查完成", exists);
        } catch (Exception e) {
            log.error("检查类型编码失败", e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }
}