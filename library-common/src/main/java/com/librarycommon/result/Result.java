package com.librarycommon.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应体
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码（与 {@link ResultCode#getCode()} 对齐）
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 业务数据，失败时为 null
     */
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /* ========== 成功 ========== */

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /* ========== 失败 ========== */

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMsg(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 当前响应是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode() == (this.code == null ? -1 : this.code);
    }
}
