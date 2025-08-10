package com.webapp.security.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.mapper.SysClientCredentialMapper;
import com.webapp.security.core.service.SysClientCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SysClientCredentialServiceImpl extends ServiceImpl<SysClientCredentialMapper, SysClientCredential>
        implements SysClientCredentialService {

    private final SysClientCredentialMapper credentialMapper;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public SysClientCredential findByAppId(String appId) {
        return credentialMapper.findByAppId(appId);
    }

    @Override
    public boolean disableByAppId(String appId) {
        SysClientCredential cred = credentialMapper.findByAppId(appId);
        if (cred == null)
            return false;
        cred.setStatus(0);
        return credentialMapper.updateById(cred) > 0;
    }

    @Override
    public SysClientCredential createCredential(String remark) {
        String appId = generateId();
        String plain = generateSecret();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;

        SysClientCredential entity = new SysClientCredential();
        entity.setAppId(appId);
        entity.setAppSecret(ENCODER.encode(plain));
        entity.setClientId("openapi");
        entity.setCreateBy(username);
        entity.setUpdateBy(username);
        entity.setRemark(remark);
        entity.setStatus(1);
        this.save(entity);

        // 将明文暂存在 transient 字段或返回由 controller 自行管理（此处仅返回实体）
        return entity;
    }

    @Override
    public boolean updateStatus(String appId, Integer status, String operator) {
        SysClientCredential cred = credentialMapper.findByAppId(appId);
        if (cred == null)
            return false;
        cred.setStatus(status);
        cred.setUpdateBy(operator);
        return credentialMapper.updateById(cred) > 0;
    }

    private String generateId() {
        byte[] b = new byte[12];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String generateSecret() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}