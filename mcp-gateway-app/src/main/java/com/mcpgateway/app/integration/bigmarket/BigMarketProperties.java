package com.mcpgateway.app.integration.bigmarket;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "managed-systems.big-market")
public class BigMarketProperties {

    private String name = "big-market-71772-z";
    private String repoPath = "E:\\Internship\\program\\big-market-71772-z";
    private String baseUrl = "http://127.0.0.1:8091";
    private String apiVersion = "v1";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
}

