package com.config.compare.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.config.compare.common.request.PageRequest;
import com.config.compare.common.result.Result;
import com.config.compare.entity.ServerInstance;
import com.config.compare.service.ServerInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 服务器实例Controller
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/servers")
@RequiredArgsConstructor
@Tag(name = "服务器实例管理")
@Validated
public class ServerInstanceController {

    private final ServerInstanceService serverInstanceService;

    @Operation(summary = "分页查询服务器实例")
    @PostMapping("/page")
    public Result<IPage<Map<String, Object>>> pageQuery(@Valid @RequestBody PageRequest pageRequest) {
        try {
            IPage<Map<String, Object>> result = serverInstanceService.pageQueryWithDetails(pageRequest);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询服务器实例失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据系统ID和服务器类型ID查询实例列表")
    @GetMapping("/list")
    public Result<List<ServerInstance>> listBySystemAndType(
            @Parameter(description = "系统ID") @RequestParam Long systemId,
            @Parameter(description = "服务器类型ID") @RequestParam(required = false) Long serverTypeId) {
        try {
            List<ServerInstance> result;
            if (serverTypeId != null) {
                result = serverInstanceService.listBySystemAndType(systemId, serverTypeId);
            } else {
                result = serverInstanceService.listBySystemId(systemId);
            }
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询服务器实例列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取服务器实例")
    @GetMapping("/{id}")
    public Result<ServerInstance> getById(@Parameter(description = "实例ID") @PathVariable Long id) {
        try {
            ServerInstance result = serverInstanceService.getById(id);
            if (result == null) {
                return Result.error("服务器实例不存在");
            }
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("根据ID获取服务器实例失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "创建服务器实例")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ServerInstance serverInstance) {
        try {
            boolean success = serverInstanceService.createInstance(serverInstance);
            if (success) {
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建服务器实例失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新服务器实例")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "实例ID") @PathVariable Long id, 
                               @Valid @RequestBody ServerInstance serverInstance) {
        try {
            serverInstance.setId(id);
            boolean success = serverInstanceService.updateInstance(serverInstance);
            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新服务器实例失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除服务器实例")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "实例ID") @PathVariable Long id) {
        try {
            boolean success = serverInstanceService.deleteInstance(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除服务器实例失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "测试服务器连接")
    @PostMapping("/{id}/test-connection")
    public Result<Boolean> testConnection(@Parameter(description = "实例ID") @PathVariable Long id) {
        try {
            boolean success = serverInstanceService.testConnection(id);
            return Result.success(success ? "连接测试成功" : "连接测试失败", success);
        } catch (Exception e) {
            log.error("连接测试失败", e);
            return Result.error("连接测试失败：" + e.getMessage());
        }
    }

    @Operation(summary = "检查实例名称是否存在")
    @GetMapping("/check-name")
    public Result<Boolean> checkInstanceName(
            @Parameter(description = "系统ID") @RequestParam Long systemId,
            @Parameter(description = "服务器类型ID") @RequestParam Long serverTypeId,
            @Parameter(description = "实例名称") @RequestParam String instanceName,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        try {
            boolean exists = serverInstanceService.checkInstanceNameExists(systemId, serverTypeId, instanceName, excludeId);
            return Result.success("检查完成", exists);
        } catch (Exception e) {
            log.error("检查实例名称失败", e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }
}