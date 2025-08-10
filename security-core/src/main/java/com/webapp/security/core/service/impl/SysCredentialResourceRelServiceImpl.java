package com.webapp.security.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysCredentialResourceRel;
import com.webapp.security.core.mapper.SysCredentialResourceRelMapper;
import com.webapp.security.core.service.SysCredentialResourceRelService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysCredentialResourceRelServiceImpl
        extends ServiceImpl<SysCredentialResourceRelMapper, SysCredentialResourceRel>
        implements SysCredentialResourceRelService {

    @Override
    public List<Long> listResourceIdsByCredentialId(Long credentialId) {
        List<SysCredentialResourceRel> list = this.list(new LambdaQueryWrapper<SysCredentialResourceRel>()
                .eq(SysCredentialResourceRel::getCredentialId, credentialId));
        return list.stream().map(SysCredentialResourceRel::getResourceId).collect(Collectors.toList());
    }

    @Override
    public void replaceAssignments(Long credentialId, List<Long> resourceIds, String operator) {
        // 删除旧的
        this.remove(new LambdaQueryWrapper<SysCredentialResourceRel>()
                .eq(SysCredentialResourceRel::getCredentialId, credentialId));
        // 新增
        for (Long rid : resourceIds) {
            SysCredentialResourceRel rel = new SysCredentialResourceRel();
            rel.setCredentialId(credentialId);
            rel.setResourceId(rid);
            rel.setCreateBy(operator);
            this.save(rel);
        }
    }
}