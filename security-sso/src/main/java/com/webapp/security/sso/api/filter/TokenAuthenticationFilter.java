package com.webapp.security.sso.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.sso.api.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * 令牌认证过滤器
 * 用于拦截请求，验证Bearer令牌
 */
@RequiredArgsConstructor
@Slf4j
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 排除令牌获取接口
        if (request.getRequestURI().equals("/api/v1/token")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");

        // 如果没有Authorization头或不是Bearer token，继续后续过滤器
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(response, "缺少有效的Authorization头");
            return;
        }

        // 提取令牌
        String token = authHeader.substring(7);

        try {
            // 验证令牌是否有效
            if (!tokenService.validateToken(token)) {
                sendUnauthorizedResponse(response, "无效的访问令牌");
                return;
            }

            // 获取令牌关联的应用ID
            String appId = tokenService.getAppIdFromToken(token);
            if (appId == null) {
                sendUnauthorizedResponse(response, "无法识别的访问令牌");
                return;
            }

            // 创建认证对象并设置到SecurityContext
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    appId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT")));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 继续过滤器链
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("验证访问令牌时发生错误", e);
            sendUnauthorizedResponse(response, "令牌验证失败: " + e.getMessage());
        }
    }

    /**
     * 发送未授权响应
     * 
     * @param response HTTP响应
     * @param message  错误消息
     * @throws IOException 如果发送响应时发生I/O错误
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ResponseResult.failed(message)));
    }
}