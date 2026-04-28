import { describe, expect, it } from "vitest";

import { createMcpServerStore } from "@/store/useMcpServerStore";

describe("useMcpServerStore", () => {
  it("adds and updates servers", () => {
    const store = createMcpServerStore();
    store.getState().addServer({
      id: "test",
      name: "Test Server",
      endpoint: "http://localhost:9999",
      transport: "websocket",
      status: "disconnected",
    });

    const updated = store.getState().servers.find((server) => server.id === "test");
    expect(updated?.name).toBe("Test Server");

    store.getState().updateServer("test", { status: "ready" });
    const afterUpdate = store.getState().servers.find((server) => server.id === "test");
    expect(afterUpdate?.status).toBe("ready");
  });

  it("trims traffic based on retention", () => {
    const store = createMcpServerStore();
    store.setState({ retentionLimit: 1 });
    store.getState().recordTraffic({
      id: "1",
      serverId: "local-sse",
      timestamp: new Date().toISOString(),
      direction: "outbound",
      kind: "request",
      payload: { jsonrpc: "2.0", id: "1", method: "ping" },
    });
    store.getState().recordTraffic({
      id: "2",
      serverId: "local-sse",
      timestamp: new Date().toISOString(),
      direction: "inbound",
      kind: "response",
      payload: { jsonrpc: "2.0", id: "1", result: "pong" },
    });

    expect(store.getState().traffic).toHaveLength(1);
    expect(store.getState().traffic[0]?.id).toBe("2");
  });
});
