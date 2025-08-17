package com.webapp.security.sso.third.model;

import lombok.Data;

@Data
public class ChoiceResponse {
    private final String encryptedOpenId;
    private final String nickname;
    private final String headimgurl;
}