import { create, type StateCreator } from "zustand";
import { createStore } from "zustand/vanilla";

import type {
  McpServer,
  McpServerStore,
  TrafficEntry,
  TransportType,
} from "@/types";

const allowedTransports: TransportType[] = ["sse", "websocket", "stdio"];
const defaultTransport = allowedTransports.includes(
  process.env.NEXT_PUBLIC_MCP_TRANSPORT as TransportType
)
  ? (process.env.NEXT_PUBLIC_MCP_TRANSPORT as TransportType)
  : "sse";
const defaultEndpoint =
  process.env.NEXT_PUBLIC_MCP_ENDPOINT ??
  "http://localhost:8080/mcp?api_key=demo-admin-key&environment=dev";
const defaultName = process.env.NEXT_PUBLIC_MCP_NAME ?? "Local MCP Server";

const defaultServers: McpServer[] = [
  {
    id: "local-sse",
    name: defaultName,
    endpoint: defaultEndpoint,
    transport: defaultTransport,
    status: "disconnected",
  },
];

const createState: StateCreator<McpServerStore> = (set) => ({
  servers: defaultServers,
  traffic: [],
  selectedServerId: defaultServers[0]?.id,
  selectedTool: undefined,
  retentionLimit: 500,
  addServer: (server) =>
    set((state: McpServerStore) => ({
      servers: [...state.servers, server],
    })),
  updateServer: (serverId, updates) =>
    set((state: McpServerStore) => ({
      servers: state.servers.map((server) =>
        server.id === serverId ? { ...server, ...updates } : server
      ),
    })),
  removeServer: (serverId) =>
    set((state: McpServerStore) => {
      const remaining = state.servers.filter((server) => server.id !== serverId);
      const selectedServerId =
        state.selectedServerId === serverId ? remaining[0]?.id : state.selectedServerId;
      return { servers: remaining, selectedServerId };
    }),
  selectServer: (serverId) => set({ selectedServerId: serverId }),
  selectTool: (toolName) => set({ selectedTool: toolName }),
  recordTraffic: (entry: TrafficEntry) =>
    set((state: McpServerStore) => {
      const updated = [...state.traffic, entry];
      if (updated.length > state.retentionLimit) {
        updated.splice(0, updated.length - state.retentionLimit);
      }
      return { traffic: updated };
    }),
  clearTraffic: () => set({ traffic: [] }),
  setCapabilities: (serverId, capabilities) =>
    set((state: McpServerStore) => {
      const tools = capabilities?.tools ?? [];
      return {
        servers: state.servers.map((server) =>
          server.id === serverId ? { ...server, capabilities } : server
        ),
        selectedTool:
          state.selectedTool && tools.some((tool) => tool.name === state.selectedTool)
            ? state.selectedTool
            : tools[0]?.name,
      };
    }),
});

export const createMcpServerStore = () =>
  createStore<McpServerStore>()(createState);

export const useMcpServerStore = create<McpServerStore>()(createState);
