package com.librarycommon.handler;

import com.librarycommon.exception.BizException;
import com.librarycommon.result.Result;
import com.librarycommon.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>统一把 Controller 抛出的异常转换为 {@link Result} 返回给前端，
 * 避免泄露堆栈、内部路径等敏感信息。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常（主动抛出）
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验失败（@Valid / @Validated 作用于 @RequestBody / @ModelAttribute）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    /**
     * 缺少必须的请求参数（@RequestParam required=true）
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String msg = "缺少必要参数: " + e.getParameterName();
        log.warn(msg);
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    /**
     * 请求体无法解析（JSON 格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, "请求体格式错误");
    }

    /**
     * 请求方法不支持（GET 接口用 POST 调）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return Result.fail(ResultCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 静态资源未找到（Spring Boot 3 默认抛 NoResourceFoundException）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /**
     * 兜底异常：日志记详细堆栈，对外只返回通用提示
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + " " + (fe.getDefaultMessage() == null ? "不合法" : fe.getDefaultMessage());
    }
}
