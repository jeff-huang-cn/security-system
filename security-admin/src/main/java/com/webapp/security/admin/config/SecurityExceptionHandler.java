package com.webapp.security.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.security.core.model.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义安全异常处理器
 * 将认证和授权异常统一处理为401响应
 */
@Slf4j
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理认证异常
     * 当用户未登录或token无效时触发
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        log.error("认证异常: {}", authException.getMessage(), authException);

        ResponseResult<Object> result = ResponseResult.failed("unauthorized", "认证失败，请重新登录");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 处理授权异常
     * 当用户已登录但没有足够权限时触发
     * 我们也将其转换为401响应，以便前端统一处理
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.error("授权异常: {}", accessDeniedException.getMessage(), accessDeniedException);

        ResponseResult<Object> result = ResponseResult.failed("unauthorized", "权限不足或登录已过期，请重新登录");

        // 将403转换为401，以便前端统一处理
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}