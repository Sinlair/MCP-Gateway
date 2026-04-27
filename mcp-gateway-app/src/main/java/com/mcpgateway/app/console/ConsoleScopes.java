package com.mcpgateway.app.console;

import java.util.List;

public final class ConsoleScopes {

    public static final String CONSOLE_READ = "console:read";
    public static final String CONSOLE_ADMIN = "console:admin";
    public static final String GATEWAY_INVOKE = "gateway:invoke";
    public static final String UPSTREAM_MANAGE = "upstream:manage";
    public static final String TOOLS_MANAGE = "tools:manage";
    public static final String POLICIES_MANAGE = "policies:manage";
    public static final String SYSTEM_BIG_MARKET_READ = "system:big-market:read";
    public static final String SYSTEM_BIG_MARKET_OPERATE = "system:big-market:operate";

    public static final String BIG_MARKET_SYSTEM = "big-market-71772-z";

    public static final List<String> DEMO_ADMIN_SCOPES = List.of(
            CONSOLE_READ,
            CONSOLE_ADMIN,
            GATEWAY_INVOKE,
            UPSTREAM_MANAGE,
            TOOLS_MANAGE,
            POLICIES_MANAGE,
            SYSTEM_BIG_MARKET_READ,
            SYSTEM_BIG_MARKET_OPERATE
    );

    public static final List<String> DEMO_APP_SCOPES = List.of(
            CONSOLE_READ,
            GATEWAY_INVOKE,
            SYSTEM_BIG_MARKET_READ
    );

    private ConsoleScopes() {
    }
}

