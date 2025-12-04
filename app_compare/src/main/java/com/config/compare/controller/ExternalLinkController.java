package com.config.compare.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.config.compare.common.request.PageRequest;
import com.config.compare.common.result.Result;
import com.config.compare.entity.ExternalLink;
import com.config.compare.service.ExternalLinkService;
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
 * 外部链接Controller
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-28
 */
@Slf4j
@RestController
@RequestMapping("/external-links")
@RequiredArgsConstructor
@Tag(name = "外部链接管理")
@Validated
public class ExternalLinkController {

    private final ExternalLinkService externalLinkService;

    @Operation(summary = "分页查询外部链接")
    @GetMapping
    public Result<IPage<ExternalLink>> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "链接名称") @RequestParam(required = false) String linkName,
            @Parameter(description = "打开方式") @RequestParam(required = false) Integer openType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        try {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(page.longValue());
            pageRequest.setSize(size.longValue());
            
            IPage<ExternalLink> result = externalLinkService.pageQuery(pageRequest, linkName, openType, status);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询外部链接失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取所有启用的外部链接")
    @GetMapping("/enabled")
    public Result<List<ExternalLink>> getEnabledLinks() {
        try {
            List<ExternalLink> result = externalLinkService.getEnabledLinks();
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取启用的外部链接失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取外部链接")
    @GetMapping("/{id}")
    public Result<ExternalLink> getById(@Parameter(description = "链接ID") @PathVariable Long id) {
        try {
            ExternalLink result = externalLinkService.getById(id);
            if (result == null) {
                return Result.error("外部链接不存在");
            }
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("根据ID获取外部链接失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "创建外部链接")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ExternalLink externalLink) {
        try {
            // 检查链接名称是否已存在
            if (externalLinkService.checkLinkNameExists(externalLink.getLinkName(), null)) {
                return Result.error("链接名称已存在");
            }
            
            boolean success = externalLinkService.createLink(externalLink);
            if (success) {
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建外部链接失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新外部链接")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "链接ID") @PathVariable Long id,
                               @Valid @RequestBody ExternalLink externalLink) {
        try {
            // 检查链接是否存在
            if (externalLinkService.getById(id) == null) {
                return Result.error("外部链接不存在");
            }
            
            // 检查链接名称是否已存在（排除当前记录）
            if (externalLinkService.checkLinkNameExists(externalLink.getLinkName(), id)) {
                return Result.error("链接名称已存在");
            }
            
            externalLink.setId(id);
            boolean success = externalLinkService.updateLink(externalLink);
            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新外部链接失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除外部链接")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "链接ID") @PathVariable Long id) {
        try {
            boolean success = externalLinkService.deleteLink(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除外部链接失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "批量删除外部链接")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的链接");
            }
            
            boolean success = externalLinkService.batchDeleteLinks(ids);
            if (success) {
                return Result.success("批量删除成功");
            } else {
                return Result.error("批量删除失败");
            }
        } catch (Exception e) {
            log.error("批量删除外部链接失败", e);
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新外部链接状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "链接ID") @PathVariable Long id,
                                     @RequestBody Map<String, Integer> request) {
        try {
            Integer status = request.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return Result.error("状态值无效");
            }
            
            boolean success = externalLinkService.updateStatus(id, status);
            if (success) {
                return Result.success(status == 1 ? "启用成功" : "禁用成功");
            } else {
                return Result.error("更新状态失败");
            }
        } catch (Exception e) {
            log.error("更新外部链接状态失败", e);
            return Result.error("更新状态失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新排序")
    @PutMapping("/{id}/sort")
    public Result<Void> updateSortOrder(@Parameter(description = "链接ID") @PathVariable Long id,
                                        @RequestBody Map<String, Integer> request) {
        try {
            Integer sortOrder = request.get("sortOrder");
            if (sortOrder == null) {
                return Result.error("排序值不能为空");
            }
            
            boolean success = externalLinkService.updateSortOrder(id, sortOrder);
            if (success) {
                return Result.success("更新排序成功");
            } else {
                return Result.error("更新排序失败");
            }
        } catch (Exception e) {
            log.error("更新外部链接排序失败", e);
            return Result.error("更新排序失败：" + e.getMessage());
        }
    }
}
