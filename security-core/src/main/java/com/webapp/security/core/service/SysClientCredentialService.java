package com.webapp.security.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.webapp.security.core.entity.SysClientCredential;

public interface SysClientCredentialService extends IService<SysClientCredential> {

    SysClientCredential findByAppId(String appId);

    boolean disableByAppId(String appId);

    /**
     * 在服务中生成 appId 与 appSecret（加密存储），并返回持久化后的实体
     */
    SysClientCredential createCredential(String remark);

    boolean updateStatus(String appId, Integer status, String operator);
}