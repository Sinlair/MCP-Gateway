import { useCallback, useMemo } from "react";

import { JsonRpcClient, createJsonRpcId } from "@/lib/jsonrpc";
import { APP_VERSION } from "@/lib/version";
import {
  SseTransport,
  StdioTransport,
  WebSocketTransport,
  type McpTransport,
} from "@/lib/transports";
import { useMcpServerStore } from "@/store/useMcpServerStore";
import type {
  McpServer,
  McpServerCapabilities,
  TrafficEntry,
  JsonRpcMessage,
} from "@/types";

const clientRegistry = new Map<string, JsonRpcClient>();

function createTransport(server: McpServer): McpTransport {
  const baseOptions = {
    endpoint: server.endpoint,
    heartbeatIntervalMs: 15000,
    heartbeatTimeoutMs: 45000,
    reconnectAttempts: 5,
    reconnectDelayMs: 1000,
  };
  switch (server.transport) {
    case "sse":
      return new SseTransport(baseOptions);
    case "websocket":
      return new WebSocketTransport(baseOptions);
    case "stdio":
      return new StdioTransport(baseOptions);
    default:
      return new SseTransport(baseOptions);
  }
}

function buildTrafficEntry(
  serverId: string,
  direction: "inbound" | "outbound",
  kind: TrafficEntry["kind"],
  payload: JsonRpcMessage
): TrafficEntry {
  return {
    id: `${serverId}-${createJsonRpcId()}`,
    serverId,
    timestamp: new Date().toISOString(),
    direction,
    kind,
    payload,
  };
}

function classifyMessage(message: JsonRpcMessage): TrafficEntry["kind"] {
  if ("id" in message && "result" in message) {
    return "response";
  }
  if ("id" in message && "error" in message) {
    return "error";
  }
  if ("id" in message) {
    return "request";
  }
  return "notification";
}

export function useMcp() {
  const servers = useMcpServerStore((state) => state.servers);
  const updateServer = useMcpServerStore((state) => state.updateServer);
  const recordTraffic = useMcpServerStore((state) => state.recordTraffic);
  const setCapabilities = useMcpServerStore((state) => state.setCapabilities);

  const serverMap = useMemo(
    () => new Map(servers.map((server) => [server.id, server])),
    [servers]
  );

  const getClient = useCallback(
    (serverId: string) => clientRegistry.get(serverId),
    []
  );

  const connect = useCallback(
    async (serverId: string) => {
      const server = serverMap.get(serverId);
      if (!server) {
        return;
      }
      updateServer(serverId, { status: "connecting", lastError: undefined });
      const transport = createTransport(server);
      const client = new JsonRpcClient(transport, {
        onSend: (message) =>
          recordTraffic(
            buildTrafficEntry(
              serverId,
              "outbound",
              classifyMessage(message),
              message
            )
          ),
        onReceive: (message) =>
          recordTraffic(
            buildTrafficEntry(
              serverId,
              "inbound",
              classifyMessage(message),
              message
            )
          ),
        onStatus: (status) => {
          updateServer(serverId, {
            status: status === "connected" ? "ready" : "disconnected",
          });
        },
        onError: (error) => {
          updateServer(serverId, { status: "error", lastError: error.message });
        },
      });
      clientRegistry.set(serverId, client);
      try {
        await client.connect();
        updateServer(serverId, { lastConnectedAt: new Date().toISOString() });
      } catch (error) {
        clientRegistry.delete(serverId);
        updateServer(serverId, {
          status: "error",
          lastError: (error as Error).message,
        });
        throw error;
      }
    },
    [recordTraffic, serverMap, updateServer]
  );

  const disconnect = useCallback(
    (serverId: string) => {
      const client = clientRegistry.get(serverId);
      client?.disconnect();
      clientRegistry.delete(serverId);
      updateServer(serverId, { status: "disconnected" });
    },
    [updateServer]
  );

  const initialize = useCallback(
    async (serverId: string) => {
      const client = clientRegistry.get(serverId);
      if (!client) {
        return;
      }
      updateServer(serverId, { status: "busy" });
      const start = performance.now();
      try {
        const result = (await client.request<McpServerCapabilities>(
          "initialize",
          {
            clientInfo: {
              name: "智能服务网关控制台",
              version: APP_VERSION,
            },
          }
        )) as McpServerCapabilities;
        const latency = Math.round(performance.now() - start);
        updateServer(serverId, { status: "ready", latencyMs: latency });
        if (result) {
          setCapabilities(serverId, result);
        }
      } catch (error) {
        updateServer(serverId, {
          status: "error",
          lastError: (error as Error).message,
        });
        throw error;
      }
    },
    [setCapabilities, updateServer]
  );

  const callTool = useCallback(
    async (serverId: string, toolName: string, params: unknown) => {
      const client = clientRegistry.get(serverId);
      if (!client) {
        return null;
      }
      updateServer(serverId, { status: "busy" });
      try {
        const result = await client.request("tools/call", {
          name: toolName,
          arguments: params,
        });
        updateServer(serverId, { status: "ready" });
        return result;
      } catch (error) {
        updateServer(serverId, {
          status: "error",
          lastError: (error as Error).message,
        });
        return null;
      }
    },
    [updateServer]
  );

  const readResource = useCallback(
    async (serverId: string, uri: string) => {
      const client = clientRegistry.get(serverId);
      if (!client) {
        return null;
      }
      updateServer(serverId, { status: "busy" });
      try {
        const result = await client.request("resources/read", { uri });
        updateServer(serverId, { status: "ready" });
        return result;
      } catch (error) {
        updateServer(serverId, {
          status: "error",
          lastError: (error as Error).message,
        });
        return null;
      }
    },
    [updateServer]
  );

  return {
    connect,
    disconnect,
    initialize,
    callTool,
    readResource,
    getClient,
  };
}
