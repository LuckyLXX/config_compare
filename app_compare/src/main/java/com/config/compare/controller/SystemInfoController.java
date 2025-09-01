package com.config.compare.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.config.compare.common.request.PageRequest;
import com.config.compare.common.result.Result;
import com.config.compare.entity.SystemInfo;
import com.config.compare.entity.ServerInstance;
import com.config.compare.entity.ServerType;
import com.config.compare.service.ServerInstanceService;
import com.config.compare.service.ServerTypeService;
import com.config.compare.service.SystemInfoService;
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
 * 系统信息Controller
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/systems")
@RequiredArgsConstructor
@Tag(name = "系统信息管理")
@Validated
public class SystemInfoController {

    private final SystemInfoService systemInfoService;
    private final ServerTypeService serverTypeService;
    private final ServerInstanceService serverInstanceService;

    @Operation(summary = "分页查询系统信息")
    @GetMapping
    public Result<IPage<SystemInfo>> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "系统名称") @RequestParam(required = false) String systemName,
            @Parameter(description = "环境类型") @RequestParam(required = false) String envType) {
        try {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current.longValue());
            pageRequest.setSize(size.longValue());
            // TODO: 需要实现查询条件设置逻辑
            // 当前PageRequest类没有addCondition方法
            
            IPage<SystemInfo> result = systemInfoService.pageQuery(pageRequest);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询系统信息失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取所有系统信息")
    @GetMapping("/list")
    public Result<List<SystemInfo>> listAll() {
        try {
            List<SystemInfo> result = systemInfoService.list();
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取所有系统信息失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取系统信息")
    @GetMapping("/{id}")
    public Result<SystemInfo> getById(@Parameter(description = "系统ID") @PathVariable Long id) {
        try {
            SystemInfo result = systemInfoService.getById(id);
            if (result == null) {
                return Result.error("系统信息不存在");
            }
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("根据ID获取系统信息失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "创建系统信息")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody SystemInfo systemInfo) {
        try {
            boolean success = systemInfoService.createSystem(systemInfo);
            if (success) {
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建系统信息失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新系统信息")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "系统ID") @PathVariable Long id, 
                               @Valid @RequestBody SystemInfo systemInfo) {
        try {
            systemInfo.setId(id);
            boolean success = systemInfoService.updateSystem(systemInfo);
            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新系统信息失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除系统信息")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "系统ID") @PathVariable Long id) {
        try {
            boolean success = systemInfoService.deleteSystem(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除系统信息失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "检查系统名称是否存在")
    @GetMapping("/check-name")
    public Result<Boolean> checkSystemName(@Parameter(description = "系统名称") @RequestParam String systemName,
                                           @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        try {
            boolean exists = systemInfoService.checkSystemNameExists(systemName, excludeId);
            return Result.success("检查完成", exists);
        } catch (Exception e) {
            log.error("检查系统名称失败", e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据系统ID获取服务器类型列表")
    @GetMapping("/{systemId}/server-types")
    public Result<List<ServerType>> getServerTypesBySystem(@Parameter(description = "系统ID") @PathVariable Long systemId) {
        try {
            List<ServerType> result = serverTypeService.listBySystemId(systemId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取系统服务器类型失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据系统ID获取服务器实例列表")
    @GetMapping("/{systemId}/servers")
    public Result<List<ServerInstance>> getServerInstancesBySystem(
            @Parameter(description = "系统ID") @PathVariable Long systemId,
            @Parameter(description = "服务器类型ID列表") @RequestParam(required = false) String serverTypeIds) {
        try {
            List<ServerInstance> result;
            if (serverTypeIds != null && !serverTypeIds.trim().isEmpty()) {
                // 根据系统ID和服务器类型ID列表查询
                String[] typeIdArray = serverTypeIds.split(",");
                result = serverInstanceService.listBySystemAndTypes(systemId, typeIdArray);
            } else {
                // 只根据系统ID查询
                result = serverInstanceService.listBySystemId(systemId);
            }
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取系统服务器实例失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}