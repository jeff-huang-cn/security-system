package com.webapp.security.sso.third.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.security.core.entity.SysGithubUser;
import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.service.SysGithubUserService;
import com.webapp.security.core.service.SysUserService;
import com.webapp.security.sso.third.UserLoginService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GitHub用户服务
 * 处理GitHub OAuth2认证和用户信息获取
 */
@Service
@RequiredArgsConstructor
public class GitHubUserService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubUserService.class);

    private final GitHubOAuth2Config githubConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SysGithubUserService sysGithubUserService;
    private final SysUserService sysUserService;
    private final UserLoginService userLoginService;

    /**
     * 获取GitHub授权URL
     */
    public String getAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(githubConfig.getAuthorizeUrl())
                .queryParam("client_id", githubConfig.getAppId())
                .queryParam("redirect_uri", githubConfig.getRedirectUri())
                .queryParam("scope", githubConfig.getScope())
                .queryParam("state", state)
                .build().toUriString();
    }

    /**
     * 使用授权码交换访问令牌
     */
    public GitHubAccessToken getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", githubConfig.getAppId());
        body.add("client_secret", githubConfig.getAppSecret());
        body.add("code", code);
        body.add("redirect_uri", githubConfig.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    githubConfig.getTokenUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                GitHubAccessToken token = objectMapper.readValue(response.getBody(), GitHubAccessToken.class);
                if (token != null && token.getAccessToken() != null) {
                    return token;
                } else {
                    logger.error("获取GitHub访问令牌失败: {}", response.getBody());
                }
            }
        } catch (RestClientException | JsonProcessingException e) {
            logger.error("调用GitHub访问令牌接口异常", e);
        }

        return null;
    }

    /**
     * 获取GitHub用户信息
     */
    public GitHubUserInfo getUserInfo(GitHubAccessToken accessToken) {
        if (accessToken == null || accessToken.getAccessToken() == null) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken.getAccessToken());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    githubConfig.getUserInfoUrl(),
                    HttpMethod.GET,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                GitHubUserInfo userInfo = objectMapper.readValue(response.getBody(), GitHubUserInfo.class);

                // 如果用户没有公开邮箱，尝试获取邮箱列表
                if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
                    String email = getPrimaryEmail(accessToken.getAccessToken());
                    if (email != null) {
                        userInfo.setEmail(email);
                    }
                }

                return userInfo;
            }
        } catch (RestClientException | JsonProcessingException e) {
            logger.error("调用GitHub用户信息接口异常", e);
        }

        return null;
    }

    /**
     * 获取主要邮箱
     */
    private String getPrimaryEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    githubConfig.getUserEmailsUrl(),
                    HttpMethod.GET,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, Object>> emails = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {
                        });

                for (Map<String, Object> email : emails) {
                    Boolean primary = (Boolean) email.get("primary");
                    Boolean verified = (Boolean) email.get("verified");
                    if (primary != null && primary && verified != null && verified) {
                        return (String) email.get("email");
                    }
                }
            }
        } catch (RestClientException | JsonProcessingException e) {
            logger.error("调用GitHub用户邮箱接口异常", e);
        }

        return null;
    }

    /**
     * 获取用户信息（一次性完成code到用户信息的转换）
     */
    public GitHubUserInfo getUserInfo(String code) {
        GitHubAccessToken accessToken = getAccessToken(code);
        return getUserInfo(accessToken);
    }

    /**
     * 处理GitHub用户登录
     * 
     * @param githubUser GitHub用户信息
     * @return 可选的系统用户ID，如果已绑定则返回用户ID，否则返回空
     */
    @Transactional
    public Optional<Long> processGithubUser(GitHubUserInfo githubUser) {
        try {
            if (githubUser == null || githubUser.getId() == null) {
                logger.error("GitHub用户信息无效");
                return Optional.empty();
            }

            // 查询是否已存在GitHub用户
            SysGithubUser sysGithubUser = sysGithubUserService.getByGithubId(githubUser.getId());

            if (sysGithubUser == null) {
                // 创建新的GitHub用户记录
                sysGithubUser = createGithubUser(githubUser);
                return Optional.empty();
            } else if (sysGithubUser.getUserId() != null) {
                // 已绑定系统用户，更新信息并返回用户ID
                SysUser user = sysUserService.getById(sysGithubUser.getUserId());
                if (user != null) {
                    // 更新GitHub用户信息
                    updateGithubUser(sysGithubUser, githubUser);
                    return Optional.of(user.getUserId());
                }
            }

            // GitHub用户存在但未绑定系统用户，或绑定的系统用户不存在
            return Optional.empty();

        } catch (Exception e) {
            logger.error("处理GitHub登录失败", e);
            return Optional.empty();
        }
    }

    /**
     * 处理GitHub用户登录 (旧方法，保留向后兼容)
     * 
     * @param githubUser GitHub用户信息
     * @return 处理结果，包含token或用户信息
     */
    @Transactional
    public Map<String, Object> processLogin(GitHubUserInfo githubUser) {
        try {
            if (githubUser == null || githubUser.getId() == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("error", "GitHub用户信息无效");
                return result;
            }

            // 查询是否已存在GitHub用户
            SysGithubUser sysGithubUser = sysGithubUserService.getByGithubId(githubUser.getId());

            if (sysGithubUser == null) {
                // 创建新的GitHub用户记录
                sysGithubUser = createGithubUser(githubUser);

                // 返回未绑定状态，前端需要展示绑定/注册选项
                Map<String, Object> result = new HashMap<>();
                result.put("bound", false);
                result.put("githubId", githubUser.getId());
                result.put("githubUser", sysGithubUser);
                return result;
            } else if (sysGithubUser.getUserId() != null) {
                // 已绑定系统用户，直接登录
                SysUser user = sysUserService.getById(sysGithubUser.getUserId());
                if (user != null) {
                    // 更新GitHub用户信息
                    updateGithubUser(sysGithubUser, githubUser);

                    // 生成token
                    Map<String, Object> tokenInfo = userLoginService.generateUserToken(user);

                    Map<String, Object> result = new HashMap<>();
                    result.put("bound", true);
                    result.put("token", tokenInfo);
                    result.put("user", user);
                    return result;
                }
            }

            // GitHub用户存在但未绑定系统用户，或绑定的系统用户不存在
            Map<String, Object> result = new HashMap<>();
            result.put("bound", false);
            result.put("githubId", githubUser.getId());
            result.put("githubUser", sysGithubUser);
            return result;

        } catch (Exception e) {
            logger.error("处理GitHub登录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "处理GitHub登录失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 绑定现有用户
     * 
     * @param githubId GitHub用户ID
     * @param username 用户名
     * @param password 密码
     * @return 绑定结果
     */
    @Transactional
    public Map<String, Object> bindExistingUser(Long githubId, String username, String password) {
        try {
            // 检查用户是否存在
            SysUser user = sysUserService.getByUsername(username);
            if (user == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 验证密码
            // 使用PasswordEncoder验证密码
            if (!userLoginService.verifyPassword(password, user.getPassword())) { // 使用密码验证服务
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "用户名或密码错误");
                return result;
            }

            // 绑定用户
            boolean bindSuccess = sysGithubUserService.bindUser(githubId, user.getUserId());
            if (!bindSuccess) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "绑定失败，GitHub用户不存在");
                return result;
            }

            // 生成token
            Map<String, Object> tokenInfo = userLoginService.generateUserToken(user);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("token", tokenInfo);
            result.put("user", user);
            return result;

        } catch (Exception e) {
            logger.error("绑定用户失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "绑定用户失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 创建新用户并绑定
     * 
     * @param githubId GitHub用户ID
     * @param username 用户名
     * @param password 密码
     * @return 创建结果
     */
    @Transactional
    public Map<String, Object> createAndBindUser(Long githubId, String username, String password) {
        try {
            // 检查GitHub用户是否存在
            SysGithubUser githubUser = sysGithubUserService.getByGithubId(githubId);
            if (githubUser == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "GitHub用户不存在");
                return result;
            }

            // 创建新用户
            SysUser newUser = userLoginService.createUser(
                    username,
                    password,
                    githubUser.getEmail(),
                    githubUser.getName());

            // 绑定GitHub用户
            sysGithubUserService.bindUser(githubId, newUser.getUserId());

            // 生成token
            Map<String, Object> tokenInfo = userLoginService.generateUserToken(newUser);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("token", tokenInfo);
            result.put("user", newUser);
            return result;

        } catch (Exception e) {
            logger.error("创建并绑定用户失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "创建并绑定用户失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 创建GitHub用户
     */
    private SysGithubUser createGithubUser(GitHubUserInfo githubUser) {
        SysGithubUser sysGithubUser = new SysGithubUser();
        sysGithubUser.setGithubId(githubUser.getId());
        sysGithubUser.setLogin(githubUser.getLogin());
        sysGithubUser.setName(githubUser.getName());
        sysGithubUser.setEmail(githubUser.getEmail());
        sysGithubUser.setAvatarUrl(githubUser.getAvatarUrl());
        sysGithubUser.setCreateTime(LocalDateTime.now());
        sysGithubUser.setUpdateTime(LocalDateTime.now());

        sysGithubUserService.save(sysGithubUser);
        return sysGithubUser;
    }

    /**
     * 更新GitHub用户信息
     */
    private void updateGithubUser(SysGithubUser sysGithubUser, GitHubUserInfo githubUser) {
        sysGithubUser.setLogin(githubUser.getLogin());
        sysGithubUser.setName(githubUser.getName());
        sysGithubUser.setEmail(githubUser.getEmail());
        sysGithubUser.setAvatarUrl(githubUser.getAvatarUrl());
        sysGithubUser.setUpdateTime(LocalDateTime.now());

        sysGithubUserService.updateById(sysGithubUser);
    }

    /**
     * GitHub访问令牌响应
     */
    @Data
    public static class GitHubAccessToken {
        private String accessToken;
        private String tokenType;
        private String scope;

        // Jackson映射字段名
        public void setAccess_token(String accessToken) {
            this.accessToken = accessToken;
        }

        public void setToken_type(String tokenType) {
            this.tokenType = tokenType;
        }
    }

    /**
     * GitHub用户信息响应
     */
    @Data
    public static class GitHubUserInfo {
        private Long id;
        private String login;
        private String name;
        private String email;
        private String avatarUrl;
        private String bio;
        private String location;
        private String company;

        // Jackson映射字段名
        public void setAvatar_url(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
    }
}