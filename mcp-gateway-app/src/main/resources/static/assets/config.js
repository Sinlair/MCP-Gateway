(function (window) {
    const STORAGE_KEY = "mcp_gateway_workspace_auth";
    const DEFAULT_ENVIRONMENT = "dev";

    const CONFIG = {
        storageKey: STORAGE_KEY,
        defaultEnvironment: DEFAULT_ENVIRONMENT,
        managedSystems: ["big-market-71772-z"],
        endpoints: {
            issueDemoToken: "/api/v1/public/console/tokens/demo",
            session: "/api/v1/console/session",
            revoke: "/api/v1/console/tokens/revoke",
            overview: "/api/v1/gateway/overview",
            discovery: "/api/v1/gateway/tools",
            invoke: "/api/v1/gateway/tools/invoke",
            upstreams: "/api/v1/admin/upstreams",
            tools: "/api/v1/admin/tools",
            policies: "/api/v1/admin/policies",
            bigMarket: "/api/v1/admin/systems/big-market",
            bigMarketActivityArmory: "/api/v1/admin/systems/big-market/activity-armory",
            bigMarketStrategyArmory: "/api/v1/admin/systems/big-market/strategy-armory",
            bigMarketDraw: "/api/v1/admin/systems/big-market/draw",
            bigMarketAwardList: "/api/v1/admin/systems/big-market/award-list",
            bigMarketUserAccount: "/api/v1/admin/systems/big-market/user-account"
        }
    };

    function readAuth() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (error) {
            return null;
        }
    }

    function saveAuth(auth) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    }

    function clearAuth() {
        localStorage.removeItem(STORAGE_KEY);
    }

    function authHeaders() {
        const auth = readAuth();
        if (!auth || !auth.token) {
            return {};
        }
        return {
            Authorization: "Bearer " + auth.token
        };
    }

    function authLabel() {
        const auth = readAuth();
        if (!auth) {
            return "Workspace";
        }
        return auth.label || auth.profile || "Workspace";
    }

    function currentEnvironment() {
        const auth = readAuth();
        return auth && auth.environment ? auth.environment : DEFAULT_ENVIRONMENT;
    }

    function buildMcpStreamUrl() {
        const auth = readAuth();
        if (!auth || !auth.token) {
            return null;
        }
        const query = new URLSearchParams({
            access_token: auth.token,
            environment: currentEnvironment()
        });
        return "/mcp?" + query.toString();
    }

    function issueDemoToken(profile) {
        return fetch(CONFIG.endpoints.issueDemoToken, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                profile: profile,
                environment: DEFAULT_ENVIRONMENT,
                managedSystems: CONFIG.managedSystems
            })
        }).then(async (response) => {
            const payload = await response.json();
            if (!response.ok || payload.code !== "0000") {
                throw new Error(payload.message || "签发演示令牌失败");
            }
            return payload.data;
        });
    }

    function validateBearerToken(token) {
        return fetch(CONFIG.endpoints.session, {
            headers: {
                Authorization: "Bearer " + token
            }
        }).then(async (response) => {
            const payload = await response.json();
            if (!response.ok || payload.code !== "0000") {
                throw new Error(payload.message || "令牌校验失败");
            }
            return payload.data;
        });
    }

    window.MCPGateway = {
        config: CONFIG,
        readAuth,
        saveAuth,
        clearAuth,
        authHeaders,
        authLabel,
        currentEnvironment,
        buildMcpStreamUrl,
        issueDemoToken,
        validateBearerToken
    };
})(window);
