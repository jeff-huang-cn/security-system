package com.webapp.security.sso.oauth2.controller;

import com.webapp.security.core.config.ClientIdConfig;
import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.entity.SysWechatUser;
import com.webapp.security.core.service.SysUserService;
import com.webapp.security.core.service.SysWechatUserService;
import com.webapp.security.sso.oauth2.service.OAuth2Utils;
import com.webapp.security.sso.oauth2.service.WechatOAuth2StateService;
import com.webapp.security.sso.oauth2.service.WechatUserService;
import com.webapp.security.sso.oauth2.service.WechatUserService.WechatUserInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 微信OAuth2控制器
 * 处理微信登录授权流程
 */
@RestController
@RequestMapping("/oauth2/wechat")
@RequiredArgsConstructor
public class WechatOAuth2Controller {
    @Value("${wechat.oauth2.token:wechat2025}")
    private String token;

    private static final Logger logger = LoggerFactory.getLogger(WechatOAuth2Controller.class);
    private static final String ENCRYPTION_KEY = "WechatOAuth2KeyWechatOAuth2Key"; // 32字节密钥
    private static final String ENCRYPTION_ALGORITHM = "AES";

    private final WechatUserService wechatUserService;
    private final SysWechatUserService sysWechatUserService;
    private final SysUserService userService;
    private final PasswordEncoder passwordEncoder;

    // OAuth2相关依赖
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

    // 添加OAuth2Utils和ClientIdConfig依赖
    private final OAuth2Utils oAuth2Utils;
    private final ClientIdConfig clientIdConfig;

    // 添加WechatOAuth2StateService依赖
    private final WechatOAuth2StateService stateService;

    /**
     * 发起微信授权请求
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(HttpServletResponse response) {
        // 生成并存储state，用于验证回调请求
        String state = stateService.generateAndSaveState();

        // 获取微信授权URL
        String authorizeUrl = wechatUserService.getAuthorizeUrl(state);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(authorizeUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * 处理微信授权回调
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam String code, @RequestParam String state) {
        // 验证state，防止CSRF攻击
        if (!stateService.validateState(state)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("无效的state参数，可能是CSRF攻击"));
        }

        // 获取微信用户信息
        WechatUserInfo userInfo = wechatUserService.getUserInfo(code);
        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("获取微信用户信息失败"));
        }

        // 查询是否已关联系统用户
        Optional<Long> userId = sysWechatUserService.processWechatUser(
                userInfo.getOpenid(),
                userInfo.getUnionid(),
                userInfo.getNickname(),
                userInfo.getHeadimgurl(),
                null, // 这里可以传入accessToken
                null // 这里可以传入refreshToken
        );

        if (userId.isPresent()) {
            // 已关联，直接生成JWT
            Map<String, Object> tokenResponse = generateTokenResponse(userId.get());
            return ResponseEntity.ok(tokenResponse);
        } else {
            // 未关联，返回选择页面URL
            String encryptedOpenId = encryptOpenId(userInfo.getOpenid());
            return ResponseEntity
                    .ok(new ChoiceResponse(encryptedOpenId, userInfo.getNickname(), userInfo.getHeadimgurl()));
        }
    }

    /**
     * 绑定已有账号
     */
    @PostMapping("/bind")
    public ResponseEntity<?> bindExistingAccount(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String encryptedOpenId) {

        try {
            // 解密OpenID
            String openid = decryptOpenId(encryptedOpenId);

            // 验证用户名密码
            SysUser user = userService.getByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户名或密码错误"));
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("用户名或密码错误"));
            }

            // 绑定账号
            SysWechatUser wechatUser = sysWechatUserService.bindWechatToUser(
                    openid, null, null, null, null, null, user.getUserId());

            // 生成JWT
            Map<String, Object> tokenResponse = generateTokenResponse(user.getUserId());
            return ResponseEntity.ok(tokenResponse);

        } catch (Exception e) {
            logger.error("绑定微信账号失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("绑定失败"));
        }
    }

    /**
     * 创建新账号
     */
    @PostMapping("/create")
    public ResponseEntity<?> createNewAccount(
            @RequestParam String encryptedOpenId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String headimgurl) {

        try {
            // 解密OpenID
            String openid = decryptOpenId(encryptedOpenId);

            // 创建新用户
            SysUser user = new SysUser();
            user.setUsername("wx_" + openid.substring(0, 8)); // 生成唯一用户名
            // 设置用户信息
            user.setRealName(nickname != null ? nickname : "微信用户");
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // 随机密码
            user.setStatus(1); // 1表示启用

            // 创建用户
            userService.createUser(user);

            // 绑定微信账号
            SysWechatUser wechatUser = sysWechatUserService.bindWechatToUser(
                    openid, null, nickname, headimgurl, null, null, user.getUserId());

            // 生成JWT
            Map<String, Object> tokenResponse = generateTokenResponse(user.getUserId());
            return ResponseEntity.ok(tokenResponse);

        } catch (Exception e) {
            logger.error("创建新账号失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("创建账号失败"));
        }
    }

    /**
     * 生成令牌响应
     * 复用OAuth2Controller中的令牌生成逻辑
     */
    private Map<String, Object> generateTokenResponse(Long userId) {
        try {
            // 使用webapp客户端ID（与账号密码登录相同）
            String clientId = clientIdConfig.getWebappClientId();

            // 使用公共方法获取注册的客户端
            RegisteredClient registeredClient = oAuth2Utils.getRegisteredClient(clientId);

            // 创建认证对象
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId.toString(), null, authorities);

            // 创建OAuth2授权
            OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                    .withRegisteredClient(registeredClient)
                    .principalName(authentication.getName())
                    .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                    .authorizedScopes(registeredClient.getScopes());

            // 生成Access Token
            OAuth2AccessToken accessToken = generateAccessToken(authentication, registeredClient, authorizationBuilder);

            // 保存授权信息
            OAuth2Authorization authorization = authorizationBuilder.build();
            authorizationService.save(authorization);

            // 计算过期时间（秒）
            long expiresIn = 0;
            if (accessToken.getExpiresAt() != null) {
                expiresIn = Duration.between(Instant.now(), accessToken.getExpiresAt()).getSeconds();
            }

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", accessToken.getTokenValue());
            response.put("token_type", accessToken.getTokenType().getValue());
            response.put("expires_in", expiresIn);
            response.put("scope", String.join(" ", accessToken.getScopes()));
            response.put("user_id", userId);

            return response;
        } catch (Exception e) {
            logger.error("生成令牌失败", e);
            throw new RuntimeException("生成令牌失败", e);
        }
    }

    /**
     * 生成Access Token - 使用OAuth2TokenContext
     */
    private OAuth2AccessToken generateAccessToken(Authentication authentication,
            RegisteredClient registeredClient,
            OAuth2Authorization.Builder authorizationBuilder) {

        // 创建OAuth2TokenContext
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(authentication)
                .authorizationServerContext(oAuth2Utils.createAuthorizationServerContext()) // 使用公共方法
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizedScopes(registeredClient.getScopes())
                .build();

        // 使用TokenGenerator生成令牌
        OAuth2Token generatedToken = tokenGenerator.generate(tokenContext);
        if (generatedToken == null) {
            throw new IllegalStateException("令牌生成失败");
        }

        // 将令牌包装为OAuth2AccessToken
        OAuth2AccessToken accessToken;
        if (generatedToken instanceof OAuth2AccessToken) {
            accessToken = (OAuth2AccessToken) generatedToken;
        } else {
            accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    generatedToken.getTokenValue(),
                    generatedToken.getIssuedAt(),
                    generatedToken.getExpiresAt(),
                    registeredClient.getScopes());
        }

        // 将令牌添加到授权构建器
        authorizationBuilder.accessToken(accessToken);

        return accessToken;
    }

    /**
     * 加密OpenID
     */
    private String encryptOpenId(String openid) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(openid.getBytes());
            return Base64.getUrlEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.error("加密OpenID失败", e);
            return null;
        }
    }

    /**
     * 解密OpenID
     */
    private String decryptOpenId(String encryptedOpenId) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getUrlDecoder().decode(encryptedOpenId);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            logger.error("解密OpenID失败", e);
            return null;
        }
    }

    /**
     * 选择响应
     */
    @Data
    public static class ChoiceResponse {
        private final String encryptedOpenId;
        private final String nickname;
        private final String headimgurl;
    }

    /**
     * 错误响应
     */
    @Data
    public static class ErrorResponse {
        private final String error;
    }

    @GetMapping("/token")
    public String verifyToken(
            @RequestParam(name = "signature") String signature,
            @RequestParam(name = "timestamp") String timestamp,
            @RequestParam(name = "nonce") String nonce,
            @RequestParam(name = "echostr") String echostr) {

        // 1. 将token、timestamp、nonce三个参数进行字典序排序
        String[] arr = new String[] { token, timestamp, nonce };
        Arrays.sort(arr);

        // 2. 将三个参数字符串拼接成一个字符串进行sha1加密
        StringBuilder content = new StringBuilder();
        for (String str : arr) {
            content.append(str);
        }

        String tmpStr = sha1(content.toString());

        // 3. 将sha1加密后的字符串与signature对比，标识该请求来源于微信
        if (tmpStr.equals(signature)) {
            return echostr;
        } else {
            return "Token 验证失败";
        }
    }


    /**
     * SHA1 加密工具方法
     */
    private String sha1(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(input.getBytes());
            StringBuilder hexStr = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexStr.append('0');
                hexStr.append(hex);
            }
            return hexStr.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA1 加密失败", e);
        }
    }
}