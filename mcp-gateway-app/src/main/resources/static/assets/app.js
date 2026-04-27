const state = {
  authMode: "apiKey",
  credential: "demo-admin-key",
  environment: "dev",
  sessionId: "",
};

const $ = (selector) => document.querySelector(selector);

const setStatus = (message, requestId = "-") => {
  $("#statusText").textContent = message;
  $("#requestText").textContent = requestId || "-";
};

const requestHeaders = () => {
  const headers = {
    "Content-Type": "application/json",
  };
  if (state.sessionId) {
    headers["X-Session-Id"] = state.sessionId;
  }
  if (state.authMode === "bearer") {
    headers.Authorization = `Bearer ${state.credential}`;
  } else {
    headers["X-API-Key"] = state.credential;
  }
  return headers;
};

const api = async (path, options = {}) => {
  const response = await fetch(path, {
    ...options,
    headers: {
      ...requestHeaders(),
      ...(options.headers || {}),
    },
  });
  const payload = await response.json();
  setStatus(payload.message, payload.requestId || response.headers.get("X-Request-Id"));
  if (!response.ok) {
    throw new Error(payload.message || `Request failed: ${response.status}`);
  }
  return payload.data;
};

const escapeHtml = (value) =>
  String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");

const table = (columns, rows) => {
  if (!rows.length) {
    return "<p>No records yet.</p>";
  }
  const head = columns.map((column) => `<th>${escapeHtml(column.label)}</th>`).join("");
  const body = rows
    .map((row) => `<tr>${columns.map((column) => `<td>${column.render(row)}</td>`).join("")}</tr>`)
    .join("");
  return `<table class="table"><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`;
};

const loadOverview = async () => {
  const data = await api(`/api/v1/gateway/overview?environment=${encodeURIComponent(state.environment)}`);
  $("#overviewCards").innerHTML = [
    ["Caller", data.callerId],
    ["Upstreams", data.totalUpstreams],
    ["Enabled", data.enabledUpstreams],
    ["Routable", data.routableUpstreams],
    ["Visible Tools", data.discoverableTools],
  ]
    .map(([label, value]) => `<div class="stat"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`)
    .join("");
  $("#overviewServers").innerHTML = data.enabledServers.length
    ? data.enabledServers
        .map(
          (server) =>
            `<span class="pill ${server.healthStatus === "UP" ? "" : "down"}">${escapeHtml(server.serverCode)} · ${escapeHtml(server.healthStatus)}</span>`
        )
        .join("")
    : "<p>No enabled upstreams yet.</p>";
};

const loadUpstreams = async () => {
  const data = await api(`/api/v1/admin/upstreams?environment=${encodeURIComponent(state.environment)}`);
  $("#upstreamList").innerHTML = table(
    [
      { label: "Server", render: (row) => `<strong>${escapeHtml(row.serverCode)}</strong><br>${escapeHtml(row.name)}` },
      { label: "Base URL", render: (row) => escapeHtml(row.baseUrl) },
      { label: "Transport", render: (row) => escapeHtml(row.transportType) },
      { label: "Status", render: (row) => `${escapeHtml(row.enabled)} / ${escapeHtml(row.healthStatus)}` },
      {
        label: "Action",
        render: (row) =>
          `<button class="ghost-button refresh-upstream" data-server="${escapeHtml(row.serverCode)}">Refresh</button>`,
      },
    ],
    data
  );
};

const loadTools = async () => {
  const data = await api(`/api/v1/admin/tools?environment=${encodeURIComponent(state.environment)}`);
  $("#toolList").innerHTML = table(
    [
      { label: "Identifier", render: (row) => `<strong>${escapeHtml(row.toolIdentifier)}</strong>` },
      { label: "Server", render: (row) => escapeHtml(row.serverCode) },
      { label: "Description", render: (row) => escapeHtml(row.description || "-") },
      { label: "Enabled", render: (row) => escapeHtml(row.enabled) },
    ],
    data
  );
};

const loadPolicies = async () => {
  const data = await api(`/api/v1/admin/policies?environment=${encodeURIComponent(state.environment)}`);
  $("#policyList").innerHTML = table(
    [
      { label: "Subject", render: (row) => `<strong>${escapeHtml(row.subjectType)}</strong><br>${escapeHtml(row.subjectId)}` },
      { label: "Tool", render: (row) => escapeHtml(row.toolIdentifier) },
      { label: "Decision", render: (row) => escapeHtml(row.decision) },
      { label: "Reason", render: (row) => escapeHtml(row.reason || "-") },
    ],
    data
  );
};

const loadDiscovery = async () => {
  const data = await api(`/api/v1/gateway/tools?environment=${encodeURIComponent(state.environment)}`);
  $("#discoveryList").innerHTML = data.length
    ? data
        .map(
          (tool) =>
            `<span class="pill">${escapeHtml(tool.toolIdentifier)} · ${escapeHtml(tool.description || "No description")}</span>`
        )
        .join("")
    : "<p>No discoverable tools for the current caller.</p>";
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
};

const submitJson = async (path, body) =>
  api(path, {
    method: "POST",
    body: JSON.stringify(body),
  });

$("#authForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  state.environment = $("#environment").value.trim() || "dev";
  state.authMode = $("#authModeSelect").value;
  state.credential = $("#credential").value.trim();
  state.sessionId = $("#sessionId").value.trim();
  $("#activeCredential").textContent = state.credential;
  await refreshAll();
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
  event.currentTarget.reset();
  await loadUpstreams();
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
  event.currentTarget.reset();
  await loadTools();
  await loadDiscovery();
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
  event.currentTarget.reset();
  await loadPolicies();
  await loadDiscovery();
});

$("#invokeForm").addEventListener("submit", async (event) => {
  try {
    await invokeTool(event);
  } catch (error) {
    $("#invokeResult").textContent = error.message;
  }
});

$("#refreshOverview").addEventListener("click", loadOverview);
$("#loadUpstreams").addEventListener("click", loadUpstreams);
$("#loadTools").addEventListener("click", loadTools);
$("#loadPolicies").addEventListener("click", loadPolicies);
$("#loadDiscovery").addEventListener("click", loadDiscovery);

$("#useAdmin").addEventListener("click", async () => {
  state.authMode = "apiKey";
  state.credential = "demo-admin-key";
  $("#authModeSelect").value = "apiKey";
  $("#credential").value = state.credential;
  $("#activeCredential").textContent = state.credential;
  await refreshAll();
});

$("#useApp").addEventListener("click", async () => {
  state.authMode = "apiKey";
  state.credential = "demo-app-key";
  $("#authModeSelect").value = "apiKey";
  $("#credential").value = state.credential;
  $("#activeCredential").textContent = state.credential;
  await refreshAll();
});

document.addEventListener("click", async (event) => {
  const button = event.target.closest(".refresh-upstream");
  if (!button) {
    return;
  }
  const serverCode = button.dataset.server;
  await submitJson(`/api/v1/admin/upstreams/${encodeURIComponent(serverCode)}/refresh?environment=${encodeURIComponent(state.environment)}`, {});
  await refreshAll();
});

const refreshAll = async () => {
  try {
    await loadOverview();
    if (state.credential === "demo-admin-key" || state.credential === "demo-admin-token") {
      await Promise.all([loadUpstreams(), loadTools(), loadPolicies()]);
    } else {
      $("#upstreamList").innerHTML = "<p>Admin credentials are required to view upstreams.</p>";
      $("#toolList").innerHTML = "<p>Admin credentials are required to view tools.</p>";
      $("#policyList").innerHTML = "<p>Admin credentials are required to view policies.</p>";
    }
    await loadDiscovery();
  } catch (error) {
    setStatus(error.message, "-");
  }
};

refreshAll();
