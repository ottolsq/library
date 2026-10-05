package com.libraryweb.handler;

import com.alibaba.fastjson2.JSON;
import com.librarycommon.result.Result;
import com.librarycommon.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证异常处理器
 *
 * <p>处理场景：未登录或凭证无效。
 *
 * <p>实现要点：
 * <ul>
 *   <li>HTTP 状态码固定为 {@code 401}</li>
 *   <li>响应体统一为 {@code Result.fail(ResultCode.UNAUTHORIZED)} 的 JSON 序列化结果</li>
 *   <li>显式设置 {@code UTF-8} 字符编码，避免中文乱码</li>
 *   <li>不抛异常、不调用过滤器链继续放行，否则响应会被重复写入</li>
 * </ul>
 */
@Slf4j
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("未认证访问: uri={}, msg={}", request.getRequestURI(), authException.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED);
        response.getWriter().write(JSON.toJSONString(result));
    }
}