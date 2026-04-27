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

const syncIdentity = () => {
  const isAdmin =
    state.credential === "demo-admin-key" || state.credential === "demo-admin-token";
  $("#activeProfile").textContent = isAdmin ? "demo-admin" : "demo-app";
  $("#activeCredential").textContent = state.credential;
  $("#activeAuthMode").textContent =
    state.authMode === "bearer" ? "Bearer Token" : "API Key";
  $("#activeEnvironment").textContent = state.environment;
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

const emptyState = (title, detail) =>
  `<div class="empty-state"><div><strong>${escapeHtml(title)}</strong><small>${escapeHtml(detail)}</small></div></div>`;

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

const loadOverview = async () => {
  const data = await api(`/api/v1/gateway/overview?environment=${encodeURIComponent(state.environment)}`);
  $("#overviewCards").innerHTML = [
    ["Caller", data.callerId, "Authenticated gateway identity"],
    ["Upstreams", data.totalUpstreams, "Registered in current environment"],
    ["Enabled", data.enabledUpstreams, "Available for management operations"],
    ["Routable", data.routableUpstreams, "Healthy and ready for invocation"],
    ["Visible Tools", data.discoverableTools, "Filtered by current caller policy"],
  ]
    .map(
      ([label, value, detail]) =>
        `<div class="metric-card"><span>${escapeHtml(label)}</span><strong>${escapeHtml(
          value
        )}</strong><small>${escapeHtml(detail)}</small></div>`
    )
    .join("");

  $("#overviewServers").innerHTML = data.enabledServers.length
    ? data.enabledServers
        .map(
          (server) => `
            <article class="server-card">
              <div>
                <strong>${escapeHtml(server.serverCode)}</strong>
                <small>${escapeHtml(server.name)} · ${escapeHtml(server.transportType)} · ${escapeHtml(
                  server.baseUrl
                )}</small>
              </div>
              <div>${badge(server.healthStatus, server.healthStatus === "UP" ? "success" : "danger")}</div>
            </article>`
        )
        .join("")
    : emptyState("No enabled upstreams", "Register and refresh an upstream to make it routable.");
};

const loadUpstreams = async () => {
  const data = await api(`/api/v1/admin/upstreams?environment=${encodeURIComponent(state.environment)}`);
  $("#upstreamList").innerHTML = table(
    [
      {
        label: "Server",
        render: (row) =>
          `<strong>${escapeHtml(row.serverCode)}</strong><br><small>${escapeHtml(row.name)}</small>`,
      },
      {
        label: "Endpoint",
        render: (row) =>
          `<code>${escapeHtml(row.baseUrl)}</code><br><small>${escapeHtml(row.transportType)} · ${escapeHtml(
            row.authMode
          )}</small>`,
      },
      {
        label: "State",
        render: (row) =>
          `${badge(row.enabled ? "Enabled" : "Disabled", row.enabled ? "success" : "danger")}
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
        label: "Action",
        render: (row) =>
          `<button class="inline-button refresh-upstream" data-server="${escapeHtml(
            row.serverCode
          )}">Refresh status</button>`,
      },
    ],
    data,
    "No upstreams yet",
    "Register the first upstream from the form on the left."
  );
};

const loadTools = async () => {
  const data = await api(`/api/v1/admin/tools?environment=${encodeURIComponent(state.environment)}`);
  $("#toolList").innerHTML = table(
    [
      {
        label: "Tool",
        render: (row) =>
          `<strong>${escapeHtml(row.toolName)}</strong><br><small>${escapeHtml(
            row.toolIdentifier
          )}</small>`,
      },
      {
        label: "Server",
        render: (row) => badge(row.serverCode, "neutral"),
      },
      {
        label: "Description",
        render: (row) => escapeHtml(row.description || "-"),
      },
      {
        label: "Status",
        render: (row) => badge(row.enabled ? "Enabled" : "Disabled", row.enabled ? "success" : "danger"),
      },
    ],
    data,
    "No tools yet",
    "Register tool metadata after at least one upstream exists."
  );
};

const loadPolicies = async () => {
  const data = await api(`/api/v1/admin/policies?environment=${encodeURIComponent(state.environment)}`);
  $("#policyList").innerHTML = table(
    [
      {
        label: "Subject",
        render: (row) =>
          `<strong>${escapeHtml(row.subjectId)}</strong><br><small>${escapeHtml(
            row.subjectType
          )}</small>`,
      },
      {
        label: "Tool",
        render: (row) => `<code>${escapeHtml(row.toolIdentifier)}</code>`,
      },
      {
        label: "Decision",
        render: (row) => badge(row.decision, row.decision === "ALLOW" ? "success" : "danger"),
      },
      {
        label: "Reason",
        render: (row) => escapeHtml(row.reason || "-"),
      },
    ],
    data,
    "No policies yet",
    "Add allow or deny rules for caller identities."
  );
};

const loadDiscovery = async () => {
  const data = await api(`/api/v1/gateway/tools?environment=${encodeURIComponent(state.environment)}`);
  $("#discoveryList").innerHTML = data.length
    ? data
        .map(
          (tool) => `
            <article class="tool-card">
              <strong>${escapeHtml(tool.toolIdentifier)}</strong>
              <small>${escapeHtml(tool.description || "No description")}</small>
            </article>`
        )
        .join("")
    : emptyState("No discoverable tools", "Current caller has no visible tools in this environment.");
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
  syncIdentity();
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
  await loadOverview();
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
  await loadOverview();
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
  await loadOverview();
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
  syncIdentity();
  await refreshAll();
});

$("#useApp").addEventListener("click", async () => {
  state.authMode = "apiKey";
  state.credential = "demo-app-key";
  $("#authModeSelect").value = "apiKey";
  $("#credential").value = state.credential;
  syncIdentity();
  await refreshAll();
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
  await refreshAll();
});

const refreshAll = async () => {
  try {
    syncIdentity();
    await loadOverview();
    if (state.credential === "demo-admin-key" || state.credential === "demo-admin-token") {
      await Promise.all([loadUpstreams(), loadTools(), loadPolicies()]);
    } else {
      $("#upstreamList").innerHTML = emptyState(
        "Admin credential required",
        "Switch to demo-admin to manage upstreams."
      );
      $("#toolList").innerHTML = emptyState(
        "Admin credential required",
        "Switch to demo-admin to manage tools."
      );
      $("#policyList").innerHTML = emptyState(
        "Admin credential required",
        "Switch to demo-admin to manage policies."
      );
    }
    await loadDiscovery();
  } catch (error) {
    setStatus(error.message, "-");
    $("#invokeResult").textContent = error.message;
  }
};

syncIdentity();
refreshAll();
