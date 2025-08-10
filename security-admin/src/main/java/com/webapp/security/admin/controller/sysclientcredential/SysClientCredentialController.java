package com.webapp.security.admin.controller.sysclientcredential;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.webapp.security.admin.controller.sysclientcredential.dto.CredentialCreateDTO;
import com.webapp.security.admin.controller.sysclientcredential.dto.CredentialCreateResultDTO;
import com.webapp.security.admin.controller.sysclientcredential.vo.CredentialVO;
import com.webapp.security.admin.converter.CredentialConverter;
import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.model.PagedDTO;
import com.webapp.security.core.model.PagedResult;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.service.SysClientCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import cn.hutool.core.util.StrUtil;

@RestController
@RequestMapping("/api/sys-client-credentials")
@RequiredArgsConstructor
public class SysClientCredentialController {

    private final SysClientCredentialService credentialService;
    private final CredentialConverter credentialConverter;

    @PostMapping("/paged")
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_QUERY')")
    public ResponseResult<PagedResult<CredentialVO>> paged(@RequestBody PagedDTO paged) {
        Page<SysClientCredential> page = new Page<>(paged.getPageNum(), paged.getPageSize());
        String keyword = paged.getKeyword();
        LambdaQueryWrapper<SysClientCredential> qw = new LambdaQueryWrapper<SysClientCredential>()
                .like(StrUtil.isNotBlank(keyword), SysClientCredential::getAppId, keyword)
                .or().like(SysClientCredential::getRemark, keyword);
        Page<SysClientCredential> result = credentialService.page(page, qw);
        return ResponseResult
                .success(new PagedResult<>(credentialConverter.toVOList(result.getRecords()), result.getTotal()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_QUERY')")
    public ResponseResult<List<CredentialVO>> all() {
        return ResponseResult.success(credentialConverter.toVOList(credentialService.list()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_CREATE')")
    public ResponseResult<CredentialCreateResultDTO> create(@Validated @RequestBody CredentialCreateDTO req) {
        SysClientCredential entity = credentialService.createCredential(req.getRemark());
        if (entity == null || entity.getId() == null) {
            return ResponseResult.failed("创建失败");
        }
        // 明文密钥不在此返回（仅创建时展示，当前按你的要求仅返回实体信息）
        return ResponseResult.success(new CredentialCreateResultDTO(entity.getAppId(), null));
    }

    @GetMapping("/download/{appId}")
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_QUERY')")
    public ResponseEntity<byte[]> download(@PathVariable String appId) {
        SysClientCredential cred = credentialService.findByAppId(appId);
        if (cred == null) {
            return ResponseEntity.notFound().build();
        }
        String content = "appid=" + cred.getAppId() + "\n" +
                "appsecret=<仅在创建时返回的明文，请妥善保管>\n";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=credential-" + cred.getAppId() + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

    @PatchMapping("/{appId}/status")
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_UPDATE')")
    public ResponseResult<Void> updateStatus(@PathVariable String appId, @RequestParam Integer status,
            @RequestParam String operator) {
        boolean ok = credentialService.updateStatus(appId, status, operator);
        return ok ? ResponseResult.success(null) : ResponseResult.failed("更新失败或不存在");
    }
}