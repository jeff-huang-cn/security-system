package com.webapp.security.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.webapp.security.core.entity.SysCredentialResourceRel;

import java.util.List;

public interface SysCredentialResourceRelService extends IService<SysCredentialResourceRel> {

    List<Long> listResourceIdsByCredentialId(Long credentialId);

    void replaceAssignments(Long credentialId, List<Long> resourceIds, String operator);
}