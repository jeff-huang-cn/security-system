package com.webapp.security.sso.config;

import com.webapp.security.sso.interceptor.ClientIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ClientIdInterceptor clientIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientIdInterceptor)
                .addPathPatterns("/oauth2/**")  // 只对OAuth2认证接口生效
                .excludePathPatterns("/oauth2/health");  // 排除健康检查接口
    }
}

