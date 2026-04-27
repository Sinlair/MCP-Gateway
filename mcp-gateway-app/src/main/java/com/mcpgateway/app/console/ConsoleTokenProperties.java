package com.mcpgateway.app.console;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.console.token")
public class ConsoleTokenProperties {

    private String issuer = "mcp-gateway";
    private String secret = "change-this-console-token-secret-at-runtime";
    private long ttlHours = 12;
    private long clockSkewSeconds = 30;
    private int auditRetention = 200;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(long ttlHours) {
        this.ttlHours = ttlHours;
    }

    public long getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(long clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public int getAuditRetention() {
        return auditRetention;
    }

    public void setAuditRetention(int auditRetention) {
        this.auditRetention = auditRetention;
    }
}

