package com.webapp.security.sso.api.token;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.Collection;
import java.util.Map;

/**
 * 增强的OAuth2认证主体
 * 用于携带增强后的属性和权限
 */
public class EnhancedOAuth2AuthenticatedPrincipal implements OAuth2AuthenticatedPrincipal {

    private final String name;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * 构造函数
     *
     * @param name        名称
     * @param attributes  属性
     * @param authorities 权限
     */
    public EnhancedOAuth2AuthenticatedPrincipal(
            String name,
            Map<String, Object> attributes,
            Collection<? extends GrantedAuthority> authorities) {
        this.name = name;
        this.attributes = attributes;
        this.authorities = authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getName() {
        return this.name;
    }
}