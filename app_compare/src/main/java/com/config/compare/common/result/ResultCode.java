package com.config.compare.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结果码枚举
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 系统错误
     */
    ERROR(500, "系统错误"),

    /**
     * 参数校验失败
     */
    PARAM_ERROR(400, "参数校验失败"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 数据已存在
     */
    DATA_EXISTS(409, "数据已存在"),

    /**
     * 连接失败
     */
    CONNECTION_ERROR(1001, "连接失败"),

    /**
     * 采集失败
     */
    COLLECT_ERROR(1002, "采集失败"),

    /**
     * 比对失败
     */
    COMPARE_ERROR(1003, "比对失败"),

    /**
     * 基线不存在
     */
    BASELINE_NOT_FOUND(1004, "基线不存在"),

    /**
     * 服务器实例不存在
     */
    SERVER_INSTANCE_NOT_FOUND(1005, "服务器实例不存在"),

    /**
     * 系统不存在
     */
    SYSTEM_NOT_FOUND(1006, "系统不存在"),

    /**
     * 模板不存在
     */
    TEMPLATE_NOT_FOUND(1007, "模板不存在"),

    /**
     * 任务执行失败
     */
    TASK_EXECUTE_ERROR(1008, "任务执行失败"),

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_ERROR(1009, "文件上传失败"),

    /**
     * 文件下载失败
     */
    FILE_DOWNLOAD_ERROR(1010, "文件下载失败");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;
}