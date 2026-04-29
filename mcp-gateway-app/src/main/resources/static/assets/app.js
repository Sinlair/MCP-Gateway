const MCP_HISTORY_KEY = "mcp_gateway_workspace_mcp_history";

function loadMcpHistory() {
    try {
        const raw = localStorage.getItem(MCP_HISTORY_KEY);
        return raw ? JSON.parse(raw) : [];
    } catch (error) {
        return [];
    }
}

const state = {
    currentView: "dashboard",
    session: null,
    overview: null,
    upstreams: [],
    tools: [],
    policies: [],
    bigMarket: null,
    lastRequestId: "-",
    mcp: {
        source: null,
        postUrl: null,
        status: "idle",
        statusText: "未连接",
        tools: [],
        selectedTool: null,
        traffic: [],
        result: "暂无结果",
        history: loadMcpHistory(),
        pending: new Map()
    }
};

$(document).ready(function () {
    if (!window.MCPGateway.readAuth()) {
        window.location.href = "/";
        return;
    }

    syncAuthSummaryLabel();
    bindShellEvents();
    loadView("dashboard");
});

function bindShellEvents() {
    $("#logoutBtn").on("click", async function (event) {
        event.preventDefault();
        try {
            await apiPost(window.MCPGateway.config.endpoints.revoke, {});
        } catch (error) {
            // Ignore revoke failure; local logout still matters.
        } finally {
            cleanupMcpConnection();
            window.MCPGateway.clearAuth();
            window.location.href = "/";
        }
    });

    $(".nav-link[data-target]").on("click", function (event) {
        event.preventDefault();
        const targetId = $(this).data("target");
        $(".nav-link").removeClass("active");
        $(this).addClass("active");
        loadView(targetId);
    });
}

function syncAuthSummaryLabel() {
    $("#authSummaryLabel").html(
        `<i class="bi bi-person-circle"></i> ${escapeHtml(window.MCPGateway.authLabel())}`
    );
}

function loadView(targetId) {
    state.currentView = targetId;
    const viewPath = `/views/${targetId}.html`;
    $("#main-content-wrapper").html(
        '<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div><div class="mt-2 text-muted">加载中...</div></div>'
    );

    $("#main-content-wrapper").load(viewPath, function (response, status, xhr) {
        if (status === "error") {
            $("#main-content-wrapper").html(
                `<div class="alert alert-danger m-4">页面加载失败：${xhr.status} ${xhr.statusText}</div>`
            );
            return;
        }
        initViewLogic(targetId);
    });
}

function initViewLogic(targetId) {
    if (targetId === "dashboard") {
        initDashboard();
        return;
    }
    if (targetId === "gateway-list") {
        initUpstreamsView();
        return;
    }
    if (targetId === "gateway-tool") {
        initToolsView();
        return;
    }
    if (targetId === "gateway-auth") {
        initPoliciesView();
        return;
    }
    if (targetId === "gateway-test") {
        initGatewayTestView();
    }
}

function apiGet(url, data) {
    return apiRequest(url, "GET", data);
}

function apiPost(url, data) {
    return apiRequest(url, "POST", data);
}

function apiRequest(url, method, data) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url,
            type: method,
            data: method === "GET" ? data : JSON.stringify(data || {}),
            contentType: method === "GET" ? undefined : "application/json",
            headers: window.MCPGateway.authHeaders(),
            success(response, _textStatus, xhr) {
                state.lastRequestId = xhr.getResponseHeader("X-Request-Id") || response.requestId || "-";
                if (!response || response.code !== "0000") {
                    reject(new Error(response && response.message ? response.message : "请求失败"));
                    return;
                }
                resolve(response.data);
            },
            error(xhr) {
                const message =
                    (xhr.responseJSON && xhr.responseJSON.message) ||
                    (xhr.responseJSON && xhr.responseJSON.error && xhr.responseJSON.error.message) ||
                    xhr.statusText ||
                    "网络请求失败";
                reject(new Error(message));
            }
        });
    });
}

async function ensureSession(force = false) {
    if (state.session && !force) {
        return state.session;
    }
    try {
        state.session = await apiGet(window.MCPGateway.config.endpoints.session);
        syncAuthSummaryLabel();
        return state.session;
    } catch (error) {
        showToast("当前 Token 已失效，请重新进入工作台", false);
        window.MCPGateway.clearAuth();
        window.location.href = "/";
        throw error;
    }
}

function currentEnvironment() {
    return window.MCPGateway.currentEnvironment();
}

function formatDateTime(value) {
    if (!value) {
        return "-";
    }
    try {
        return new Date(value).toLocaleString("zh-CN");
    } catch (error) {
        return value;
    }
}

function escapeHtml(value) {
    return $("<div>").text(value == null ? "" : String(value)).html();
}

function showToast(message, isSuccess = true) {
    const toastEl = $("#liveToast");
    const iconHtml = isSuccess
        ? '<i class="bi bi-check-circle-fill"></i>'
        : '<i class="bi bi-exclamation-triangle-fill"></i>';

    $("#toastMessage").html(`${iconHtml} <span>${escapeHtml(message)}</span>`);
    if (isSuccess) {
        toastEl.removeClass("bg-danger").addClass("bg-success");
    } else {
        toastEl.removeClass("bg-success").addClass("bg-danger");
    }
    bootstrap.Toast.getOrCreateInstance(toastEl[0]).show();
}

function buildScopeBadges(scopes) {
    if (!scopes || !scopes.length) {
        return '<span class="badge bg-secondary-subtle text-secondary">无 scopes</span>';
    }
    return scopes
        .map((scope) => `<span class="tool-chip">${escapeHtml(scope)}</span>`)
        .join("");
}

function renderStateMessage(selector, message, tone = "muted") {
    const toneClass = tone === "danger" ? "text-danger" : "text-muted";
    const $target = $(selector);
    if (!$target.length) {
        return;
    }
    const tagName = ($target.prop("tagName") || "").toLowerCase();
    if (tagName === "tbody") {
        const columnCount = $target.closest("table").find("thead th").length || 1;
        $target.html(
            `<tr><td colspan="${columnCount}" class="text-center ${toneClass} py-4"><i class="bi bi-info-circle me-2"></i>${escapeHtml(message)}</td></tr>`
        );
        return;
    }
    $target.html(
        `<div class="text-center ${toneClass} py-4"><i class="bi bi-info-circle me-2"></i>${escapeHtml(message)}</div>`
    );
}

async function initDashboard() {
    $("#current-date").text(new Date().toLocaleDateString("zh-CN", {
        weekday: "long",
        year: "numeric",
        month: "long",
        day: "numeric"
    }));

    $("#refreshDashboardBtn").off("click").on("click", initDashboard);

    const session = await ensureSession();
    try {
        const overview = await apiGet(window.MCPGateway.config.endpoints.overview, {
            environment: currentEnvironment()
        });
        state.overview = overview;
        renderDashboard(session, overview);
    } catch (error) {
        renderDashboard(session, null);
        showToast(error.message, false);
    }
}

function renderDashboard(session, overview) {
    $("#stat-total-upstreams").text(overview ? overview.totalUpstreams : 0);
    $("#stat-routable-upstreams").text(overview ? overview.routableUpstreams : 0);
    $("#stat-discoverable-tools").text(overview ? overview.discoverableTools : 0);
    $("#stat-environment").text(currentEnvironment());

    $("#dashboard-profile").text(session.profile || "-");
    $("#dashboard-mode").text(window.MCPGateway.authLabel());
    $("#dashboard-caller").text(overview ? overview.callerId : session.profile || "-");
    $("#dashboard-request-id").text(state.lastRequestId || "-");
    $("#dashboard-expire-time").text(formatDateTime(session.expiresAt));
    $("#dashboard-request-tip").text("-");
    $("#dashboard-scope-badges").html(buildScopeBadges(session.scopes));

    if (!overview || !overview.enabledServers || !overview.enabledServers.length) {
        renderStateMessage("#dashboard-upstream-grid", "暂无数据");
        return;
    }

    $("#dashboard-upstream-grid").html(
        overview.enabledServers
            .map((server) => `
                <div class="metric-panel">
                    <div class="metric-label">${escapeHtml(server.serverCode)}</div>
                    <div class="metric-value">${escapeHtml(server.name)}</div>
                    <div class="text-muted small mt-2">${escapeHtml(server.baseUrl)}</div>
                    <div class="mt-3">
                        <span class="badge ${server.enabled ? "bg-success-subtle text-success" : "bg-secondary-subtle text-secondary"}">${server.enabled ? "已启用" : "已禁用"}</span>
                        <span class="badge ${server.healthStatus === "UP" ? "bg-success-subtle text-success" : "bg-warning-subtle text-warning-emphasis"}">${escapeHtml(server.healthStatus)}</span>
                    </div>
                </div>
            `)
            .join("")
    );
}

function initUpstreamsView() {
    loadUpstreamsView();

    $("#form-upstream-search").off("submit").on("submit", function (event) {
        event.preventDefault();
        renderUpstreamTable();
    });

    $("#resetUpstreamSearchBtn").off("click").on("click", function () {
        $("#form-upstream-search")[0].reset();
        renderUpstreamTable();
    });

    $("#refreshUpstreamsBtn").off("click").on("click", loadUpstreamsView);

    $("#upstreamForm").off("submit").on("submit", async function (event) {
        event.preventDefault();
        const payload = {
            environment: currentEnvironment(),
            serverCode: $(this).find("[name='serverCode']").val().trim(),
            name: $(this).find("[name='name']").val().trim(),
            baseUrl: $(this).find("[name='baseUrl']").val().trim(),
            transportType: $(this).find("[name='transportType']").val(),
            authMode: $(this).find("[name='authMode']").val().trim(),
            enabled: $(this).find("[name='enabled']").is(":checked"),
            timeoutMs: Number($(this).find("[name='timeoutMs']").val() || 3000)
        };
        try {
            await apiPost(window.MCPGateway.config.endpoints.upstreams, payload);
            showToast("上游服务保存成功");
            bootstrap.Modal.getOrCreateInstance(document.getElementById("upstreamModal")).hide();
            this.reset();
            $("#upstream-enabled").prop("checked", true);
            await loadUpstreamsView();
        } catch (error) {
            showToast(error.message, false);
        }
    });

    $(document).off("click", ".btn-refresh-upstream").on("click", ".btn-refresh-upstream", async function () {
        const serverCode = $(this).data("server");
        try {
            await apiPost(`${window.MCPGateway.config.endpoints.upstreams}/${encodeURIComponent(serverCode)}/refresh?environment=${encodeURIComponent(currentEnvironment())}`, {});
            showToast(`已刷新 ${serverCode} 的健康状态`);
            await loadUpstreamsView();
        } catch (error) {
            showToast(error.message, false);
        }
    });
}

async function loadUpstreamsView() {
    try {
        state.upstreams = await apiGet(window.MCPGateway.config.endpoints.upstreams, {
            environment: currentEnvironment()
        });
        renderUpstreamTable();
    } catch (error) {
        renderStateMessage("#upstreamTableBody", error.message, "danger");
    }
}

function renderUpstreamTable() {
    const code = ($("#search-upstream-code").val() || "").trim().toLowerCase();
    const name = ($("#search-upstream-name").val() || "").trim().toLowerCase();
    const items = state.upstreams.filter((item) => {
        return (!code || item.serverCode.toLowerCase().includes(code))
            && (!name || item.name.toLowerCase().includes(name));
    });

    if (!items.length) {
        renderStateMessage("#upstreamTableBody", "没有匹配的上游服务");
        return;
    }

    $("#upstreamTableBody").html(
        items.map((item) => `
            <tr>
                <td><code>${escapeHtml(item.serverCode)}</code></td>
                <td>${escapeHtml(item.name)}</td>
                <td class="text-truncate" style="max-width: 280px;" title="${escapeHtml(item.baseUrl)}">${escapeHtml(item.baseUrl)}</td>
                <td>
                    <span class="badge bg-secondary-subtle text-secondary">${escapeHtml(item.transportType)}</span>
                    <span class="badge bg-info-subtle text-info-emphasis">${escapeHtml(item.authMode)}</span>
                </td>
                <td>
                    <span class="badge ${item.enabled ? "bg-success-subtle text-success" : "bg-secondary-subtle text-secondary"}">${item.enabled ? "已启用" : "已禁用"}</span>
                    <span class="badge ${item.healthStatus === "UP" ? "bg-success-subtle text-success" : "bg-warning-subtle text-warning-emphasis"}">${escapeHtml(item.healthStatus)}</span>
                </td>
                <td>
                    <button class="btn btn-sm btn-outline-primary btn-refresh-upstream" data-server="${escapeHtml(item.serverCode)}">
                        <i class="bi bi-arrow-clockwise"></i> 探活
                    </button>
                </td>
            </tr>
        `).join("")
    );
}

function initToolsView() {
    loadToolsView();

    $("#form-tool-search").off("submit").on("submit", function (event) {
        event.preventDefault();
        renderToolTable();
    });

    $("#resetToolSearchBtn").off("click").on("click", function () {
        $("#form-tool-search")[0].reset();
        renderToolTable();
    });

    $("#refreshToolsBtn").off("click").on("click", loadToolsView);

    $("#toolForm").off("submit").on("submit", async function (event) {
        event.preventDefault();
        const payload = {
            environment: currentEnvironment(),
            serverCode: $(this).find("[name='serverCode']").val().trim(),
            toolName: $(this).find("[name='toolName']").val().trim(),
            description: $(this).find("[name='description']").val().trim(),
            inputSchema: $(this).find("[name='inputSchema']").val().trim(),
            enabled: $(this).find("[name='enabled']").is(":checked")
        };
        try {
            await apiPost(window.MCPGateway.config.endpoints.tools, payload);
            showToast("工具定义保存成功");
            bootstrap.Modal.getOrCreateInstance(document.getElementById("toolModal")).hide();
            this.reset();
            $("#tool-enabled").prop("checked", true);
            await loadToolsView();
        } catch (error) {
            showToast(error.message, false);
        }
    });
}

async function loadToolsView() {
    try {
        state.tools = await apiGet(window.MCPGateway.config.endpoints.tools, {
            environment: currentEnvironment()
        });
        renderToolTable();
    } catch (error) {
        renderStateMessage("#toolTableBody", error.message, "danger");
    }
}

function renderToolTable() {
    const server = ($("#search-tool-server").val() || "").trim().toLowerCase();
    const toolName = ($("#search-tool-name").val() || "").trim().toLowerCase();
    const items = state.tools.filter((item) => {
        return (!server || item.serverCode.toLowerCase().includes(server))
            && (!toolName || item.toolName.toLowerCase().includes(toolName));
    });
    if (!items.length) {
        renderStateMessage("#toolTableBody", "没有匹配的工具定义");
        return;
    }
    $("#toolTableBody").html(
        items.map((item) => `
            <tr>
                <td>
                    <strong>${escapeHtml(item.toolIdentifier)}</strong>
                    <div class="text-muted small">${escapeHtml(item.toolName)}</div>
                </td>
                <td><code>${escapeHtml(item.serverCode)}</code></td>
                <td>${escapeHtml(item.description || "-")}</td>
                <td><pre class="mb-0 text-wrap small">${escapeHtml(item.inputSchema || "{}")}</pre></td>
                <td><span class="badge ${item.enabled ? "bg-success-subtle text-success" : "bg-secondary-subtle text-secondary"}">${item.enabled ? "已启用" : "已禁用"}</span></td>
            </tr>
        `).join("")
    );
}

function initPoliciesView() {
    loadPoliciesView();

    $("#form-policy-search").off("submit").on("submit", function (event) {
        event.preventDefault();
        renderPolicyTable();
    });

    $("#resetPolicySearchBtn").off("click").on("click", function () {
        $("#form-policy-search")[0].reset();
        renderPolicyTable();
    });

    $("#refreshPoliciesBtn").off("click").on("click", loadPoliciesView);

    $("#policyForm").off("submit").on("submit", async function (event) {
        event.preventDefault();
        const payload = {
            environment: currentEnvironment(),
            subjectType: $(this).find("[name='subjectType']").val(),
            subjectId: $(this).find("[name='subjectId']").val().trim(),
            toolIdentifier: $(this).find("[name='toolIdentifier']").val().trim(),
            decision: $(this).find("[name='decision']").val(),
            enabled: $(this).find("[name='enabled']").is(":checked"),
            reason: $(this).find("[name='reason']").val().trim()
        };
        try {
            await apiPost(window.MCPGateway.config.endpoints.policies, payload);
            showToast("访问策略保存成功");
            bootstrap.Modal.getOrCreateInstance(document.getElementById("policyModal")).hide();
            this.reset();
            $("#policy-enabled").prop("checked", true);
            await loadPoliciesView();
        } catch (error) {
            showToast(error.message, false);
        }
    });
}

async function loadPoliciesView() {
    try {
        state.policies = await apiGet(window.MCPGateway.config.endpoints.policies, {
            environment: currentEnvironment()
        });
        renderPolicyTable();
    } catch (error) {
        renderStateMessage("#policyTableBody", error.message, "danger");
    }
}

function renderPolicyTable() {
    const subject = ($("#search-policy-subject").val() || "").trim().toLowerCase();
    const tool = ($("#search-policy-tool").val() || "").trim().toLowerCase();
    const items = state.policies.filter((item) => {
        return (!subject || item.subjectId.toLowerCase().includes(subject))
            && (!tool || item.toolIdentifier.toLowerCase().includes(tool));
    });
    if (!items.length) {
        renderStateMessage("#policyTableBody", "没有匹配的策略");
        return;
    }
    $("#policyTableBody").html(
        items.map((item) => `
            <tr>
                <td>
                    <strong>${escapeHtml(item.subjectId)}</strong>
                    <div class="text-muted small">${escapeHtml(item.subjectType)}</div>
                </td>
                <td><code>${escapeHtml(item.toolIdentifier)}</code></td>
                <td><span class="badge ${item.decision === "ALLOW" ? "bg-success-subtle text-success" : "bg-danger-subtle text-danger"}">${escapeHtml(item.decision)}</span></td>
                <td><span class="badge ${item.enabled ? "bg-success-subtle text-success" : "bg-secondary-subtle text-secondary"}">${item.enabled ? "生效中" : "已停用"}</span></td>
                <td>${escapeHtml(item.reason || "-")}</td>
            </tr>
        `).join("")
    );
}

function initBigMarketView() {
    loadBigMarketOverview();
    $("#refreshBigMarketBtn").off("click").on("click", loadBigMarketOverview);

    $("#bmActivityArmory").off("click").on("click", () => runBigMarketAction(
        window.MCPGateway.config.endpoints.bigMarketActivityArmory,
        { activityId: Number($("#bmActivityId").val()) },
        "活动装配"
    ));
    $("#bmStrategyArmory").off("click").on("click", () => runBigMarketAction(
        window.MCPGateway.config.endpoints.bigMarketStrategyArmory,
        { strategyId: Number($("#bmStrategyId").val()) },
        "策略装配"
    ));
    $("#bmAwardList").off("click").on("click", () => runBigMarketAction(
        window.MCPGateway.config.endpoints.bigMarketAwardList,
        bigMarketPayload(),
        "查询奖品列表"
    ));
    $("#bmUserAccount").off("click").on("click", () => runBigMarketAction(
        window.MCPGateway.config.endpoints.bigMarketUserAccount,
        bigMarketPayload(),
        "查询活动账户"
    ));
    $("#bmDraw").off("click").on("click", () => runBigMarketAction(
        window.MCPGateway.config.endpoints.bigMarketDraw,
        bigMarketPayload(),
        "执行抽奖"
    ));
}

function initGatewayTestView() {
    initBigMarketView();
    initMcpLabView();
}

function bigMarketPayload() {
    return {
        userId: $("#bmUserId").val().trim(),
        activityId: Number($("#bmActivityId").val())
    };
}

async function loadBigMarketOverview() {
    try {
        state.bigMarket = await apiGet(window.MCPGateway.config.endpoints.bigMarket);
        $("#bigMarketOverviewGrid").html(
            [
                ["系统名称", state.bigMarket.systemName],
                ["仓库路径", state.bigMarket.repoPath],
                ["基础地址", state.bigMarket.baseUrl],
                ["API 版本", state.bigMarket.apiVersion],
                ["连通状态", state.bigMarket.reachable ? "在线" : "离线"],
                ["支持操作", (state.bigMarket.supportedOperations || []).join(" / ")]
            ].map((item) => `
                <div class="metric-panel">
                    <div class="metric-label">${escapeHtml(item[0])}</div>
                    <div class="metric-value">${escapeHtml(item[1] || "-")}</div>
                </div>
            `).join("")
        );
    } catch (error) {
        renderStateMessage("#bigMarketOverviewGrid", error.message, "danger");
        $("#bigMarketResult").text(error.message);
    }
}

async function runBigMarketAction(url, payload, actionName) {
    try {
        const result = await apiPost(url, payload);
        $("#bigMarketResult").text(JSON.stringify(result, null, 2));
        showToast(`${actionName}已完成`);
    } catch (error) {
        $("#bigMarketResult").text(error.message);
        showToast(error.message, false);
    }
}

function initMcpLabView() {
    $("#mcpEnvironment").val(currentEnvironment());
    $("#mcpEndpointPreview").text(window.MCPGateway.buildMcpStreamUrl() || "-");
    renderMcpStatus();
    renderMcpTraffic();
    renderMcpHistoryTable();
    renderMcpToolSelect();
    renderMcpResult();

    $("#reloadMcpLabBtn").off("click").on("click", function () {
        cleanupMcpConnection(true);
        $("#mcpEndpointPreview").text(window.MCPGateway.buildMcpStreamUrl() || "-");
        renderMcpStatus();
        renderMcpTraffic();
        showToast("MCP 连接已重置");
    });

    $("#mcpConnectBtn").off("click").on("click", connectMcpStream);
    $("#mcpInitializeBtn").off("click").on("click", initializeMcpSession);
    $("#mcpListToolsBtn").off("click").on("click", requestMcpToolsList);
    $("#mcpCallToolBtn").off("click").on("click", callSelectedMcpTool);
    $("#mcpToolSelect").off("change").on("change", function () {
        state.mcp.selectedTool = $(this).val() || null;
        renderMcpToolSelect();
    });
    $("#clearMcpTrafficBtn").off("click").on("click", function () {
        state.mcp.traffic = [];
        renderMcpTraffic();
    });
    $("#openMcpHistoryBtn").off("click").on("click", function () {
        renderMcpHistoryTable();
        bootstrap.Modal.getOrCreateInstance(document.getElementById("mcpHistoryModal")).show();
    });
    $("#clearMcpHistoryBtn").off("click").on("click", function () {
        state.mcp.history = [];
        persistMcpHistory();
        renderMcpHistoryTable();
    });
}

function setMcpStatus(status, text) {
    state.mcp.status = status;
    state.mcp.statusText = text;
    renderMcpStatus();
}

function renderMcpStatus() {
    const $dot = $("#mcpStatusDot");
    const $text = $("#mcpStatusText");
    if (!$dot.length) {
        return;
    }
    $dot.removeClass("ready connecting error");
    if (state.mcp.status === "ready") {
        $dot.addClass("ready");
    } else if (state.mcp.status === "connecting") {
        $dot.addClass("connecting");
    } else if (state.mcp.status === "error") {
        $dot.addClass("error");
    }
    $text.text(state.mcp.statusText);
}

function cleanupMcpConnection(clearTools) {
    if (state.mcp.source) {
        state.mcp.source.close();
    }
    state.mcp.source = null;
    state.mcp.postUrl = null;
    state.mcp.pending.forEach((pending) => clearTimeout(pending.timeout));
    state.mcp.pending.clear();
    if (clearTools) {
        state.mcp.tools = [];
        state.mcp.selectedTool = null;
    }
    setMcpStatus("idle", "未连接");
}

function connectMcpStream() {
    const endpoint = window.MCPGateway.buildMcpStreamUrl();
    if (!endpoint) {
        showToast("当前没有可用的访问 Token", false);
        return;
    }

    cleanupMcpConnection(false);
    state.mcp.traffic = [];
    renderMcpTraffic();
    setMcpStatus("connecting", "连接中...");

    const source = new EventSource(endpoint);
    state.mcp.source = source;
    recordMcpTraffic("system", "连接 SSE", { endpoint });

    source.onopen = function () {
        setMcpStatus("connecting", "SSE 已建立，等待 endpoint");
    };

    source.onerror = function () {
        setMcpStatus("error", "连接异常或已中断");
        recordMcpTraffic("error", "SSE 异常", { endpoint });
    };

    source.addEventListener("endpoint", function (event) {
        state.mcp.postUrl = event.data;
        setMcpStatus("ready", "已连接，可发送 JSON-RPC");
        recordMcpTraffic("system", "收到 endpoint", { endpoint: event.data });
    });

    source.addEventListener("message", function (event) {
        try {
            const payload = JSON.parse(event.data);
            recordMcpTraffic("inbound", "收到 JSON-RPC", payload);
            handleMcpMessage(payload);
        } catch (error) {
            recordMcpTraffic("error", "解析消息失败", { raw: event.data });
        }
    });
}

function createRpcId() {
    return `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}

function sendMcpRequest(method, params) {
    if (!state.mcp.postUrl) {
        return Promise.reject(new Error("还没有拿到 POST endpoint，请先建立连接"));
    }
    const request = {
        jsonrpc: "2.0",
        id: createRpcId(),
        method,
        params: params || {}
    };
    recordMcpTraffic("outbound", `发送 ${method}`, request);

    return fetch(state.mcp.postUrl, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(request),
        credentials: "same-origin"
    }).then((response) => {
        if (!response.ok && response.status !== 202) {
            throw new Error(`MCP 请求被拒绝: ${response.status}`);
        }
        return new Promise((resolve, reject) => {
            const timeout = setTimeout(() => {
                state.mcp.pending.delete(request.id);
                reject(new Error(`${method} 响应超时`));
            }, 30000);
            state.mcp.pending.set(request.id, {
                method,
                resolve,
                reject,
                timeout
            });
        });
    });
}

function handleMcpMessage(payload) {
    if (payload && payload.id && state.mcp.pending.has(payload.id)) {
        const pending = state.mcp.pending.get(payload.id);
        clearTimeout(pending.timeout);
        state.mcp.pending.delete(payload.id);
        if (payload.error) {
            pending.reject(payload.error);
        } else {
            pending.resolve(payload.result);
        }
    }
}

async function initializeMcpSession() {
    try {
        const result = await sendMcpRequest("initialize", {
            clientInfo: {
                name: "MCP Gateway Console",
                version: "1.0.0"
            }
        });
        if (result && result.tools) {
            state.mcp.tools = result.tools;
            state.mcp.selectedTool = result.tools.length ? result.tools[0].name : null;
            renderMcpToolSelect();
        }
        state.mcp.result = JSON.stringify(result, null, 2);
        renderMcpResult();
        saveMcpHistory("initialize", "完成 initialize 握手", true);
        showToast("initialize 完成");
    } catch (error) {
        state.mcp.result = JSON.stringify(error, null, 2);
        renderMcpResult();
        saveMcpHistory("initialize", error.message || "initialize 失败", false);
        showToast(error.message || "initialize 失败", false);
    }
}

async function requestMcpToolsList() {
    try {
        const result = await sendMcpRequest("tools/list", {});
        state.mcp.tools = (result && result.tools) || [];
        state.mcp.selectedTool = state.mcp.tools.length ? state.mcp.tools[0].name : null;
        renderMcpToolSelect();
        state.mcp.result = JSON.stringify(result, null, 2);
        renderMcpResult();
        saveMcpHistory("tools/list", `发现 ${state.mcp.tools.length} 个工具`, true);
        showToast("已刷新工具列表");
    } catch (error) {
        state.mcp.result = JSON.stringify(error, null, 2);
        renderMcpResult();
        saveMcpHistory("tools/list", error.message || "tools/list 失败", false);
        showToast(error.message || "tools/list 失败", false);
    }
}

async function callSelectedMcpTool() {
    const selectedTool = state.mcp.tools.find((item) => item.name === $("#mcpToolSelect").val());
    if (!selectedTool) {
        showToast("请先选择一个工具", false);
        return;
    }

    let args;
    try {
        args = JSON.parse($("#mcpArgumentsInput").val() || "{}");
    } catch (error) {
        showToast("参数 JSON 解析失败", false);
        return;
    }

    try {
        const result = await sendMcpRequest("tools/call", {
            name: selectedTool.name,
            arguments: args
        });
        state.mcp.result = JSON.stringify(result, null, 2);
        renderMcpResult();
        saveMcpHistory("tools/call", `调用 ${selectedTool.name} 成功`, true);
        showToast(`已执行 ${selectedTool.name}`);
    } catch (error) {
        state.mcp.result = JSON.stringify(error, null, 2);
        renderMcpResult();
        saveMcpHistory("tools/call", `调用 ${selectedTool.name} 失败`, false);
        showToast(error.message || "tools/call 失败", false);
    }
}

function renderMcpToolSelect() {
    const $select = $("#mcpToolSelect");
    if (!$select.length) {
        return;
    }
    if (!state.mcp.tools.length) {
        $select.html('<option value="">暂无工具</option>');
        $("#mcpSchemaPreview").text("{}");
        return;
    }
    $select.html(
        state.mcp.tools.map((tool) => `
            <option value="${escapeHtml(tool.name)}" ${tool.name === state.mcp.selectedTool ? "selected" : ""}>
                ${escapeHtml(tool.name)}
            </option>
        `).join("")
    );
    const selectedTool = state.mcp.tools.find((item) => item.name === (state.mcp.selectedTool || $select.val()));
    if (selectedTool) {
        state.mcp.selectedTool = selectedTool.name;
        $("#mcpSchemaPreview").text(JSON.stringify(selectedTool.inputSchema || {}, null, 2));
        $("#mcpArgumentsInput").val(JSON.stringify(sampleArgumentsFromSchema(selectedTool.inputSchema || {}), null, 2));
    }
}

function sampleArgumentsFromSchema(schema) {
    if (!schema || typeof schema !== "object") {
        return {};
    }
    if (schema.type === "object" && schema.properties) {
        const result = {};
        Object.keys(schema.properties).forEach((key) => {
            result[key] = sampleArgumentsFromSchema(schema.properties[key]);
        });
        return result;
    }
    if (schema.type === "array") {
        return [];
    }
    if (schema.type === "integer" || schema.type === "number") {
        return 0;
    }
    if (schema.type === "boolean") {
        return false;
    }
    return "";
}

function recordMcpTraffic(direction, title, payload) {
    state.mcp.traffic.push({
        time: new Date().toLocaleTimeString("zh-CN"),
        direction,
        title,
        payload
    });
    if (state.mcp.traffic.length > 80) {
        state.mcp.traffic.shift();
    }
    renderMcpTraffic();
}

function renderMcpTraffic() {
    const $window = $("#mcpTrafficWindow");
    if (!$window.length) {
        return;
    }
    if (!state.mcp.traffic.length) {
        $window.html(`
            <div class="text-center text-muted py-5" id="mcpTrafficPlaceholder">
                <i class="bi bi-broadcast-pin fs-3 d-block mb-2"></i>
                暂无消息
            </div>
        `);
        return;
    }
    $window.html(
        state.mcp.traffic.map((entry) => {
            const bubbleClass = entry.direction === "error"
                ? "chat-bubble chat-bubble-error"
                : entry.direction === "outbound"
                    ? "chat-bubble chat-bubble-user"
                    : "chat-bubble chat-bubble-assistant";
            return `
                <div class="mb-3">
                    <div class="${bubbleClass}">
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <span class="badge bg-light text-dark">${escapeHtml(entry.direction)}</span>
                            <span class="fw-semibold">${escapeHtml(entry.title)}</span>
                            <span class="text-muted small">${escapeHtml(entry.time)}</span>
                        </div>
                        <pre class="mb-0 small">${escapeHtml(JSON.stringify(entry.payload, null, 2))}</pre>
                    </div>
                </div>
            `;
        }).join("")
    );
    $window.scrollTop($window[0].scrollHeight);
}

function renderMcpResult() {
    if ($("#mcpResultOutput").length) {
        $("#mcpResultOutput").text(state.mcp.result || "暂无结果");
    }
}

function saveMcpHistory(method, summary, success) {
    state.mcp.history.unshift({
        time: Date.now(),
        method,
        summary,
        success
    });
    if (state.mcp.history.length > 50) {
        state.mcp.history = state.mcp.history.slice(0, 50);
    }
    persistMcpHistory();
    renderMcpHistoryTable();
}

function persistMcpHistory() {
    localStorage.setItem(MCP_HISTORY_KEY, JSON.stringify(state.mcp.history));
}

function renderMcpHistoryTable() {
    const $tbody = $("#mcpHistoryTableBody");
    if (!$tbody.length) {
        return;
    }
    if (!state.mcp.history.length) {
        $tbody.html('<tr><td colspan="4" class="text-center text-muted py-3">暂无调用历史</td></tr>');
        return;
    }
    $tbody.html(
        state.mcp.history.map((item) => `
            <tr>
                <td>${escapeHtml(new Date(item.time).toLocaleString("zh-CN"))}</td>
                <td><code>${escapeHtml(item.method)}</code></td>
                <td>${escapeHtml(item.summary)}</td>
                <td><span class="badge ${item.success ? "bg-success-subtle text-success" : "bg-danger-subtle text-danger"}">${item.success ? "成功" : "失败"}</span></td>
            </tr>
        `).join("")
    );
}
