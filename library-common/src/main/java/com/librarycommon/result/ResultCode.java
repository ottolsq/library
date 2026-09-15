package com.librarycommon.result;

import lombok.Getter;

/**
 * 统一错误码枚举
 *
 * <p>约定：
 * <ul>
 *   <li>200  成功</li>
 *   <li>4xx  客户端错误（参数、权限、资源不存在等）</li>
 *   <li>5xx  服务端错误</li>
 *   <li>1xxx 业务错误码（按模块区分，例如 10xx 用户、11xx 图书、12xx 借阅）</li>
 * </ul>
 */
@Getter
public enum ResultCode {

    /* ========== 通用 ========== */
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    SYSTEM_ERROR(500, "系统异常，请稍后重试"),

    /* ========== 业务（按模块分段，先占位，用到再细分） ========== */
    PARAM_INVALID(1000, "参数校验失败"),
    DATA_NOT_FOUND(1001, "数据不存在"),
    DATA_ALREADY_EXISTS(1002, "数据已存在"),
    ;

    /**
     * 业务/HTTP 状态码
     */
    private final int code;

    /**
     * 提示信息
     */
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
