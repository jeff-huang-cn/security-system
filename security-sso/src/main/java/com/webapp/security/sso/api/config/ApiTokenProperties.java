package com.webapp.security.sso.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "sso.api.token")
public class ApiTokenProperties {
    public enum TokenPolicy {
        CLIENT_CONFIGURED,
        PROGRAM_CONFIGURED
    }

    private TokenPolicy policy = TokenPolicy.CLIENT_CONFIGURED;
    private long programTtlSeconds = 2 * 60 * 60;
    private boolean persistInDb = true;
    private List<String> whitelistPaths = Arrays.asList(
            "/api/v1/oauth/token",
            "/api/v1/validate");

    public TokenPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(TokenPolicy policy) {
        this.policy = policy;
    }

    public long getProgramTtlSeconds() {
        return programTtlSeconds;
    }

    public void setProgramTtlSeconds(long programTtlSeconds) {
        this.programTtlSeconds = programTtlSeconds;
    }

    public boolean isPersistInDb() {
        return persistInDb;
    }

    public void setPersistInDb(boolean persistInDb) {
        this.persistInDb = persistInDb;
    }

    public List<String> getWhitelistPaths() {
        return whitelistPaths;
    }

    public void setWhitelistPaths(List<String> whitelistPaths) {
        this.whitelistPaths = whitelistPaths;
    }
}