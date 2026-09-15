package com.librarycommon.exception;

import com.librarycommon.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 *
 * <p>Service/Controller 抛出此异常，由全局异常处理器统一转换为 {@code Result}。
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务/HTTP 状态码
     */
    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.SYSTEM_ERROR.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.SYSTEM_ERROR.getCode();
    }
}
