package com.mcpgateway.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mcpgateway")
public class McpGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpGatewayApplication.class, args);
    }
}

