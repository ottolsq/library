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

/**
 * 未认证处理器
 *
 * <p>用户未登录访问受保护资源时返回统一 JSON 响应。
 */
@Slf4j
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("未认证访问: uri={}, msg={}", request.getRequestURI(), authException.getMessage());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
