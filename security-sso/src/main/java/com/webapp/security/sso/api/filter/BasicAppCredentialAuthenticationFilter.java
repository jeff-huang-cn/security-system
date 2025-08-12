package com.webapp.security.sso.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.mapper.SysClientCredentialMapper;
import com.webapp.security.sso.api.config.ApiTokenProperties;
import com.webapp.security.sso.api.util.BasicCredentialUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BasicAppCredentialAuthenticationFilter extends OncePerRequestFilter {

    private final SysClientCredentialMapper clientCredentialMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final ApiTokenProperties tokenProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (shouldSkip(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String base64 = BasicCredentialUtil.resolveBasicBase64(request);
        if (!StringUtils.hasText(base64)) {
            unauthorized(response, "缺少Basic凭证");
            return;
        }

        String[] creds = BasicCredentialUtil.decodeBasicPair(base64);
        if (creds == null || creds.length != 2) {
            unauthorized(response, "Basic认证格式无效");
            return;
        }

        String appId = creds[0];
        String appSecret = creds[1];

        try {
            SysClientCredential credential = clientCredentialMapper.findByAppId(appId);
            if (credential == null || credential.getStatus() != 1) {
                unauthorized(response, "客户端凭证不存在或已禁用");
                return;
            }
            if (!passwordEncoder.matches(appSecret, credential.getAppSecret())) {
                unauthorized(response, "应用密钥不正确");
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    appId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("Basic客户端凭证校验异常", ex);
            SecurityContextHolder.clearContext();
            unauthorized(response, "认证失败: " + ex.getMessage());
        }
    }

    private boolean shouldSkip(String uri) {
        List<String> whitelist = tokenProperties.getWhitelistPaths();
        if (CollectionUtils.isEmpty(whitelist)) {
            return false;
        }
        for (String open : whitelist) {
            if (uri.equals(open))
                return true;
        }
        return false;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ResponseResult.failed(message)));
    }
}