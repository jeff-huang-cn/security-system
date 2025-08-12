package com.webapp.security.sso.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.security.core.mapper.SysClientCredentialMapper;
import com.webapp.security.sso.api.filter.BasicAppCredentialAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ApiTokenProperties.class)
@RequiredArgsConstructor
public class ApiSecurityConfig {

    private final SysClientCredentialMapper clientCredentialMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final ApiTokenProperties tokenProperties;

    @Bean
    public BasicAppCredentialAuthenticationFilter basicAppCredentialAuthenticationFilter() {
        return new BasicAppCredentialAuthenticationFilter(clientCredentialMapper, passwordEncoder, objectMapper,
                tokenProperties);
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .antMatcher("/api/**")
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(tokenProperties.getWhitelistPaths().toArray(new String[0])).permitAll()
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(basicAppCredentialAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}