const TOKEN_STORAGE_KEY = "mcp_console_access_token";

const state = {
  accessToken: "",
  environment: "dev",
  sessionId: "",
  session: null,
  activities: [],
};

const $ = (selector) => document.querySelector(selector);

const timeStamp = () =>
  new Date().toLocaleTimeString("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  });

const escapeHtml = (value) =>
  String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");

const emptyState = (title, detail) =>
  `<div class="empty-state"><div><strong>${escapeHtml(title)}</strong><small>${escapeHtml(
    detail
  )}</small></div></div>`;

const badge = (label, tone = "neutral") =>
  `<span class="badge badge-${tone}">${escapeHtml(label)}</span>`;

const table = (columns, rows, emptyTitle, emptyDetail) => {
  if (!rows.length) {
    return emptyState(emptyTitle, emptyDetail);
  }
  const head = columns.map((column) => `<th>${escapeHtml(column.label)}</th>`).join("");
  const body = rows
    .map((row) => `<tr>${columns.map((column) => `<td>${column.render(row)}</td>`).join("")}</tr>`)
    .join("");
  return `<table class="table"><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`;
};

const isAdminSession = () =>
  Boolean(state.session && Array.isArray(state.session.roles) && state.session.roles.includes("ADMIN"));

const currentProfile = () => (state.session?.profile ? state.session.profile : "未认证");

const setStatus = (message, requestId = "-") => {
  $("#statusText").textContent = message || "就绪";
  $("#requestText").textContent = requestId || "-";
};

const setGateMessage = (message, tone = "neutral") => {
  const el = $("#gateMessage");
  el.textContent = message;
  el.dataset.tone = tone;
};

const rememberToken = (token) => {
  state.accessToken = token;
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
};

const clearToken = () => {
  state.accessToken = "";
  state.session = null;
  localStorage.removeItem(TOKEN_STORAGE_KEY);
};

const requestHeaders = () => ({
  "Content-Type": "application/json",
  Authorization: `Bearer ${state.accessToken}`,
});

const logActivity = (title, detail, tone = "info") => {
  state.activities.unshift({
    title,
    detail,
    tone,
    time: timeStamp(),
  });
  state.activities = state.activities.slice(0, 14);
  renderActivityFeed();
};

const renderActivityFeed = () => {
  const container = $("#activityFeed");
  if (!container) {
    return;
  }
  if (!state.activities.length) {
    container.innerHTML = emptyState("暂无事件", "加载数据或执行操作后，这里会出现事件流。");
    return;
  }
  container.innerHTML = state.activities
    .map(
      (entry) => `
        <article class="activity-line ${entry.tone === "warn" ? "warn" : entry.tone === "error" ? "error" : ""}">
          <div class="activity-time">${escapeHtml(entry.time)}</div>
          <div class="activity-dot"></div>
          <div>
            <strong>${escapeHtml(entry.title)}</strong>
            <small>${escapeHtml(entry.detail)}</small>
          </div>
        </article>`
    )
    .join("");
};

const showGate = () => {
  $("#accessGate").classList.remove("hidden");
  $("#consoleApp").classList.add("hidden");
};

const showConsole = () => {
  $("#accessGate").classList.add("hidden");
  $("#consoleApp").classList.remove("hidden");
};

const syncSessionIdentity = () => {
  if (!state.session) {
    return;
  }
  $("#activeProfile").textContent = state.session.profile;
  $("#activeCredential").textContent = `${state.accessToken.slice(0, 16)}...`;
  $("#activeEnvironment").textContent = state.session.environment;
  $("#activeRoles").textContent = state.session.roles.join(", ");
  $("#activeExpiresAt").textContent = new Date(state.session.expiresAt).toLocaleString("zh-CN");
  $("#tokenContextPanel").innerHTML = [
    ["访问画像", state.session.profile, "当前控制台令牌绑定的调用方身份"],
    ["权限角色", state.session.roles.join(", "), "决定可访问模块与动作范围"],
    ["令牌环境", state.session.environment, "控制台默认作用的网关环境"],
    ["过期时间", new Date(state.session.expiresAt).toLocaleString("zh-CN"), "令牌过期后需要重新签发"],
  ]
    .map(
      ([label, value, detail]) => `
        <div class="metric-card">
          <span>${escapeHtml(label)}</span>
          <strong>${escapeHtml(value)}</strong>
          <small>${escapeHtml(detail)}</small>
        </div>`
    )
    .join("");
};

const parseApiPayload = async (response) => {
  const payload = await response.json();
  const requestId = payload.requestId || response.headers.get("X-Request-Id");
  setStatus(payload.message, requestId);
  if (!response.ok) {
    throw new Error(payload.message || `请求失败: ${response.status}`);
  }
  return payload.data;
};

const api = async (path, options = {}) => {
  const response = await fetch(path, {
    ...options,
    headers: {
      ...requestHeaders(),
      ...(options.headers || {}),
    },
  });
  return parseApiPayload(response);
};

const issueDemoToken = async (profile) => {
  const response = await fetch("/api/v1/public/console/tokens/demo", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      profile,
      environment: "dev",
    }),
  });
  const payload = await parseApiPayload(response);
  rememberToken(payload.accessToken);
  $("#gateTokenInput").value = payload.accessToken;
  setGateMessage(`已签发 ${payload.profile} 演示令牌，正在进入控制台。`, "success");
  await enterConsoleWithToken(payload.accessToken, true);
};

const loadSession = async () => {
  const response = await fetch("/api/v1/console/session", {
    headers: requestHeaders(),
  });
  const session = await parseApiPayload(response);
  state.session = session;
  state.environment = session.environment;
  syncSessionIdentity();
  return session;
};

const renderSignals = (overview) => {
  const totalUpstreams = Math.max(overview.totalUpstreams, 1);
  const enabledShare = Math.round((overview.enabledUpstreams / totalUpstreams) * 100);
  const routableShare = Math.round((overview.routableUpstreams / totalUpstreams) * 100);
  const discoverableShare = Math.min(100, overview.discoverableTools * 10);
  const privilegeShare = isAdminSession() ? 100 : 55;

  $("#overviewSignals").innerHTML = [
    ["注册激活率", `${overview.enabledUpstreams}/${overview.totalUpstreams} 已启用`, enabledShare],
    ["路由就绪率", `${overview.routableUpstreams}/${overview.totalUpstreams} 可路由`, routableShare],
    ["工具暴露率", `${overview.discoverableTools} 个工具可见`, discoverableShare],
    ["控制平面权限", isAdminSession() ? "管理员令牌" : "调用方令牌", privilegeShare],
  ]
    .map(
      ([title, detail, percent]) => `
        <div class="signal-line">
          <div class="signal-line-head">
            <strong>${escapeHtml(title)}</strong>
            <small>${escapeHtml(detail)}</small>
          </div>
          <div class="signal-bar"><span style="width:${percent}%"></span></div>
        </div>`
    )
    .join("");
};

const renderHealthGrid = (servers) => {
  $("#overviewServers").innerHTML = servers.length
    ? servers
        .map(
          (server) => `
            <article class="health-card">
              <strong>${escapeHtml(server.serverCode)}</strong>
              <small>${escapeHtml(server.name)} · ${escapeHtml(server.transportType)}</small>
              <small>${escapeHtml(server.baseUrl)}</small>
              <footer>
                ${badge(server.enabled ? "已启用" : "已禁用", server.enabled ? "success" : "danger")}
                ${badge(
                  server.healthStatus,
                  server.healthStatus === "UP"
                    ? "success"
                    : server.healthStatus === "DOWN"
                    ? "danger"
                    : "neutral"
                )}
              </footer>
            </article>`
        )
        .join("")
    : emptyState("暂无启用上游", "先注册并刷新上游，才能进入路由可用态。");
};

const loadOverview = async () => {
  const data = await api(
    `/api/v1/gateway/overview?environment=${encodeURIComponent(state.environment)}`
  );
  $("#overviewCards").innerHTML = [
    ["调用方", data.callerId, "当前通过控制台令牌映射出来的网关身份"],
    ["上游总数", data.totalUpstreams, "当前环境下已注册的上游服务数"],
    ["已启用", data.enabledUpstreams, "允许参与网关治理的上游"],
    ["可路由", data.routableUpstreams, "健康且可转发的上游"],
    ["可见工具", data.discoverableTools, "基于令牌权限过滤后的工具集"],
  ]
    .map(
      ([label, value, detail]) => `
        <div class="metric-card">
          <span>${escapeHtml(label)}</span>
          <strong>${escapeHtml(value)}</strong>
          <small>${escapeHtml(detail)}</small>
        </div>`
    )
    .join("");

  renderSignals(data);
  renderHealthGrid(data.enabledServers);
  logActivity("网关总览已刷新", `${data.callerId} 在 ${state.environment} 环境可见 ${data.discoverableTools} 个工具。`);
};

const loadUpstreams = async () => {
  const data = await api(
    `/api/v1/admin/upstreams?environment=${encodeURIComponent(state.environment)}`
  );
  $("#upstreamList").innerHTML = table(
    [
      {
        label: "服务",
        render: (row) =>
          `<strong>${escapeHtml(row.serverCode)}</strong><br><small>${escapeHtml(row.name)}</small>`,
      },
      {
        label: "地址",
        render: (row) =>
          `<code>${escapeHtml(row.baseUrl)}</code><br><small>${escapeHtml(row.transportType)} · ${escapeHtml(
            row.authMode
          )}</small>`,
      },
      {
        label: "状态",
        render: (row) =>
          `${badge(row.enabled ? "已启用" : "已禁用", row.enabled ? "success" : "danger")}
           ${badge(
             row.healthStatus,
             row.healthStatus === "UP"
               ? "success"
               : row.healthStatus === "DOWN"
               ? "danger"
               : "neutral"
           )}`,
      },
      {
        label: "操作",
        render: (row) =>
          `<button class="inline-button refresh-upstream" data-server="${escapeHtml(
            row.serverCode
          )}">刷新健康状态</button>`,
      },
    ],
    data,
    "暂无上游服务",
    "可以先从左侧表单注册一个上游服务。"
  );
  logActivity("上游注册表已加载", `共加载 ${data.length} 条上游记录。`);
};

const loadTools = async () => {
  const data = await api(
    `/api/v1/admin/tools?environment=${encodeURIComponent(state.environment)}`
  );
  $("#toolList").innerHTML = table(
    [
      {
        label: "工具",
        render: (row) =>
          `<strong>${escapeHtml(row.toolName)}</strong><br><small>${escapeHtml(
            row.toolIdentifier
          )}</small>`,
      },
      {
        label: "服务",
        render: (row) => badge(row.serverCode, "neutral"),
      },
      {
        label: "描述",
        render: (row) => escapeHtml(row.description || "-"),
      },
      {
        label: "状态",
        render: (row) => badge(row.enabled ? "已启用" : "已禁用", row.enabled ? "success" : "danger"),
      },
    ],
    data,
    "暂无工具定义",
    "至少存在一个上游服务后，再注册工具定义。"
  );
  logActivity("工具目录已加载", `共加载 ${data.length} 个工具定义。`);
};

const loadPolicies = async () => {
  const data = await api(
    `/api/v1/admin/policies?environment=${encodeURIComponent(state.environment)}`
  );
  $("#policyList").innerHTML = table(
    [
      {
        label: "主体",
        render: (row) =>
          `<strong>${escapeHtml(row.subjectId)}</strong><br><small>${escapeHtml(
            row.subjectType
          )}</small>`,
      },
      {
        label: "工具",
        render: (row) => `<code>${escapeHtml(row.toolIdentifier)}</code>`,
      },
      {
        label: "决策",
        render: (row) =>
          badge(row.decision === "ALLOW" ? "允许" : "拒绝", row.decision === "ALLOW" ? "success" : "danger"),
      },
      {
        label: "原因",
        render: (row) => escapeHtml(row.reason || "-"),
      },
    ],
    data,
    "暂无访问策略",
    "可以按调用方或角色添加允许/拒绝规则。"
  );
  logActivity("策略矩阵已加载", `共加载 ${data.length} 条访问控制策略。`);
};

const loadDiscovery = async () => {
  const data = await api(
    `/api/v1/gateway/tools?environment=${encodeURIComponent(state.environment)}`
  );
  $("#discoveryList").innerHTML = data.length
    ? data
        .map(
          (tool) => `
            <article class="tool-card">
              <strong>${escapeHtml(tool.toolIdentifier)}</strong>
              <small>${escapeHtml(tool.description || "暂无描述")}</small>
            </article>`
        )
        .join("")
    : emptyState("当前无可见工具", "当前令牌在这个环境下没有工具暴露。");
  logActivity("工具发现结果已刷新", `${currentProfile()} 当前可见 ${data.length} 个工具。`);
};

const loadBigMarketOverview = async () => {
  const data = await api("/api/v1/admin/systems/big-market");
  $("#bigMarketOverview").innerHTML = `
    <div class="metric-card">
      <span>系统名称</span>
      <strong>${escapeHtml(data.systemName)}</strong>
      <small>通过 MCP 网关代理控制的业务系统</small>
    </div>
    <div class="metric-card">
      <span>仓库路径</span>
      <strong>${escapeHtml(data.repoPath)}</strong>
      <small>本地项目目录</small>
    </div>
    <div class="metric-card">
      <span>基础地址</span>
      <strong>${escapeHtml(data.baseUrl)}</strong>
      <small>big-market HTTP 入口</small>
    </div>
    <div class="metric-card">
      <span>可达性</span>
      <strong>${data.reachable ? "在线" : "离线"}</strong>
      <small>${data.reachable ? "8091 服务可访问" : "请先启动 big-market 服务"}</small>
    </div>
    <div class="metric-card">
      <span>已接操作</span>
      <strong>${data.supportedOperations.length}</strong>
      <small>${escapeHtml(data.supportedOperations.join(" / "))}</small>
    </div>`;
  logActivity(
    "受管系统状态已刷新",
    `${data.systemName} 当前${data.reachable ? "在线" : "离线"}，地址 ${data.baseUrl}。`,
    data.reachable ? "info" : "warn"
  );
};

const bigMarketPayload = () => ({
  userId: $("#bmUserId").value.trim(),
  activityId: Number($("#bmActivityId").value),
});

const runBigMarketAction = async (path, body, title) => {
  try {
    const data = await api(path, {
      method: "POST",
      body: JSON.stringify(body),
    });
    $("#bigMarketResult").textContent = JSON.stringify(data, null, 2);
    logActivity(title, `${data.message || "操作完成"} · ${data.targetPath}`, "warn");
  } catch (error) {
    $("#bigMarketResult").textContent = error.message;
    logActivity(title, error.message, "error");
  }
};

const invokeTool = async (event) => {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  const body = {
    environment: state.environment,
    toolIdentifier: formData.get("toolIdentifier"),
    arguments: JSON.parse(formData.get("arguments")),
  };
  const data = await api("/api/v1/gateway/tools/invoke", {
    method: "POST",
    body: JSON.stringify(body),
  });
  $("#invokeResult").textContent = JSON.stringify(data, null, 2);
  logActivity("工具调用完成", `${data.toolIdentifier} 返回 ${data.status}。`);
};

const submitJson = async (path, body) =>
  api(path, {
    method: "POST",
    body: JSON.stringify(body),
  });

const loadAdminSurfaces = async () => {
  await Promise.all([loadUpstreams(), loadTools(), loadPolicies(), loadBigMarketOverview()]);
};

const renderAdminLockedStates = () => {
  $("#upstreamList").innerHTML = emptyState("需要管理员令牌", "切换到管理员令牌后才能管理上游。");
  $("#toolList").innerHTML = emptyState("需要管理员令牌", "切换到管理员令牌后才能管理工具。");
  $("#policyList").innerHTML = emptyState("需要管理员令牌", "切换到管理员令牌后才能管理策略。");
  $("#bigMarketOverview").innerHTML = emptyState("需要管理员令牌", "切换到管理员令牌后才能控制 big-market。");
  $("#bigMarketResult").textContent = "当前令牌没有 big-market 控制权限。";
};

const refreshAll = async () => {
  try {
    await loadSession();
    syncSessionIdentity();
    showConsole();
    await loadOverview();
    if (isAdminSession()) {
      await loadAdminSurfaces();
    } else {
      renderAdminLockedStates();
    }
    await loadDiscovery();
  } catch (error) {
    clearToken();
    showGate();
    setGateMessage(`访问失败：${error.message}`, "error");
    $("#gateTokenInput").value = "";
    setStatus("令牌失效", "-");
  }
};

const enterConsoleWithToken = async (token, fromIssue = false) => {
  rememberToken(token);
  if (!fromIssue) {
    setGateMessage("令牌验证中，请稍候…", "neutral");
  }
  await refreshAll();
  if (state.session) {
    setGateMessage(`已通过 ${state.session.profile} 令牌进入控制台。`, "success");
    logActivity(
      "控制台访问成功",
      `${state.session.profile} 已进入 ${state.session.environment} 环境控制台。`,
      "warn"
    );
  }
};

const bootFromExistingToken = async () => {
  const urlToken = new URL(window.location.href).searchParams.get("token");
  const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
  const token = urlToken || storedToken;
  if (!token) {
    showGate();
    return;
  }
  $("#gateTokenInput").value = token;
  await enterConsoleWithToken(token);
};

$("#gateEnterButton").addEventListener("click", async () => {
  const token = $("#gateTokenInput").value.trim();
  if (!token) {
    setGateMessage("请先输入访问令牌。", "error");
    return;
  }
  await enterConsoleWithToken(token);
});

$("#gateClearButton").addEventListener("click", () => {
  $("#gateTokenInput").value = "";
  setGateMessage("支持 URL `?token=...` 自动带入，也会记住本地最近一次令牌。");
});

$("#issueAdminTokenButton").addEventListener("click", async () => {
  await issueDemoToken("demo-admin");
});

$("#issueAppTokenButton").addEventListener("click", async () => {
  await issueDemoToken("demo-app");
});

$("#switchAdminTokenButton").addEventListener("click", async () => {
  await issueDemoToken("demo-admin");
});

$("#switchAppTokenButton").addEventListener("click", async () => {
  await issueDemoToken("demo-app");
});

$("#logoutTokenButton").addEventListener("click", () => {
  clearToken();
  showGate();
  $("#gateTokenInput").value = "";
  setGateMessage("令牌已清除，请重新输入或重新签发访问令牌。", "warn");
  $("#bigMarketResult").textContent = "big-market 操作结果会显示在这里。";
  $("#invokeResult").textContent = "工具调用结果会显示在这里。";
});

$("#refreshOverview").addEventListener("click", loadOverview);
$("#loadUpstreams").addEventListener("click", loadUpstreams);
$("#loadTools").addEventListener("click", loadTools);
$("#loadPolicies").addEventListener("click", loadPolicies);
$("#loadDiscovery").addEventListener("click", loadDiscovery);
$("#bmLoadOverview").addEventListener("click", loadBigMarketOverview);

$("#bmActivityArmory").addEventListener("click", async () => {
  await runBigMarketAction(
    "/api/v1/admin/systems/big-market/activity-armory",
    bigMarketPayload(),
    "big-market 活动装配"
  );
});

$("#bmStrategyArmory").addEventListener("click", async () => {
  await runBigMarketAction(
    "/api/v1/admin/systems/big-market/strategy-armory",
    { strategyId: Number($("#bmStrategyId").value) },
    "big-market 策略装配"
  );
});

$("#bmAwardList").addEventListener("click", async () => {
  await runBigMarketAction(
    "/api/v1/admin/systems/big-market/award-list",
    bigMarketPayload(),
    "big-market 奖品列表查询"
  );
});

$("#bmUserAccount").addEventListener("click", async () => {
  await runBigMarketAction(
    "/api/v1/admin/systems/big-market/user-account",
    bigMarketPayload(),
    "big-market 活动账户查询"
  );
});

$("#bmDraw").addEventListener("click", async () => {
  await runBigMarketAction(
    "/api/v1/admin/systems/big-market/draw",
    bigMarketPayload(),
    "big-market 执行抽奖"
  );
});

$("#upstreamForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  await submitJson("/api/v1/admin/upstreams", {
    environment: state.environment,
    serverCode: formData.get("serverCode"),
    name: formData.get("name"),
    baseUrl: formData.get("baseUrl"),
    transportType: formData.get("transportType"),
    authMode: formData.get("authMode"),
    enabled: formData.get("enabled") === "on",
    timeoutMs: Number(formData.get("timeoutMs") || 3000),
  });
  logActivity("上游服务已注册", `${formData.get("serverCode")} 已加入环境 ${state.environment}。`);
  event.currentTarget.reset();
  await refreshAll();
});

$("#toolForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  await submitJson("/api/v1/admin/tools", {
    environment: state.environment,
    serverCode: formData.get("serverCode"),
    toolName: formData.get("toolName"),
    description: formData.get("description"),
    inputSchema: formData.get("inputSchema"),
    enabled: formData.get("enabled") === "on",
  });
  logActivity(
    "工具定义已注册",
    `${formData.get("serverCode")}:${formData.get("toolName")} 已加入工具目录。`
  );
  event.currentTarget.reset();
  await refreshAll();
});

$("#policyForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  await submitJson("/api/v1/admin/policies", {
    environment: state.environment,
    subjectType: formData.get("subjectType"),
    subjectId: formData.get("subjectId"),
    toolIdentifier: formData.get("toolIdentifier"),
    decision: formData.get("decision"),
    enabled: formData.get("enabled") === "on",
    reason: formData.get("reason"),
  });
  logActivity(
    "访问策略已保存",
    `${formData.get("subjectId")} 对 ${formData.get("toolIdentifier")} 的策略已更新。`
  );
  event.currentTarget.reset();
  await refreshAll();
});

$("#invokeForm").addEventListener("submit", async (event) => {
  try {
    await invokeTool(event);
  } catch (error) {
    $("#invokeResult").textContent = error.message;
    logActivity("工具调用失败", error.message, "error");
  }
});

document.addEventListener("click", async (event) => {
  const button = event.target.closest(".refresh-upstream");
  if (!button) {
    return;
  }
  const serverCode = button.dataset.server;
  await submitJson(
    `/api/v1/admin/upstreams/${encodeURIComponent(serverCode)}/refresh?environment=${encodeURIComponent(
      state.environment
    )}`,
    {}
  );
  logActivity("上游健康探测已执行", `${serverCode} 已完成一次手动探活。`, "warn");
  await refreshAll();
});

showGate();
renderActivityFeed();
bootFromExistingToken();
