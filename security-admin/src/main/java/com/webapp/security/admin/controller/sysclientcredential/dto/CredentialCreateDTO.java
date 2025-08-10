package com.webapp.security.admin.controller.sysclientcredential.dto;

import lombok.Data;

@Data
public class CredentialCreateDTO {
    private Long creatorUserId;
    private String creatorUsername;
    private String remark;
}