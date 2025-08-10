package com.webapp.security.admin.controller.syscredentialresourcerel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.webapp.security.admin.controller.syscredentialresourcerel.dto.AssignDTO;
import com.webapp.security.core.entity.SysCredentialResourceRel;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.service.SysCredentialResourceRelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sys-credential-resource-rel")
@RequiredArgsConstructor
public class SysCredentialResourceRelController {

    private final SysCredentialResourceRelService relService;

    @GetMapping("/{credentialId}/resource-ids")
    @PreAuthorize("hasAuthority('OPENAPI_PERMISSION_QUERY')")
    public ResponseResult<List<Long>> listResourceIds(@PathVariable Long credentialId) {
        return ResponseResult.success(relService.listResourceIdsByCredentialId(credentialId));
    }

    @PostMapping("/{credentialId}/assign")
    @PreAuthorize("hasAuthority('OPENAPI_PERMISSION_ASSIGN')")
    @Transactional
    public ResponseResult<Void> assign(@PathVariable Long credentialId, @Validated @RequestBody AssignDTO dto) {
        relService.replaceAssignments(credentialId, dto.getResourceIds(), dto.getOperator());
        return ResponseResult.success(null, "授权成功");
    }
}