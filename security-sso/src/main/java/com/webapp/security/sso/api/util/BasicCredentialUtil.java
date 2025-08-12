package com.webapp.security.sso.api.util;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class BasicCredentialUtil {

    private BasicCredentialUtil() {
    }

    /**
     * 从请求中提取 Basic 的 base64 内容，支持：
     * 1) Header: Authorization: Basic <base64>
     * 2) URL 参数: access_token=Basic <base64> 或 access_token=<base64>
     */
    public static String resolveBasicBase64(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Basic ")) {
            return header.substring(6).trim();
        }
        String tokenParam = request.getParameter("access_token");
        if (StringUtils.hasText(tokenParam)) {
            if (tokenParam.startsWith("Basic ")) {
                return tokenParam.substring(6).trim();
            }
            return tokenParam.trim();
        }
        return null;
    }

    /**
     * 将 base64(appId:appSecret) 解码为 [appId, appSecret]
     */
    public static String[] decodeBasicPair(String base64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            return decoded.split(":", 2);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}