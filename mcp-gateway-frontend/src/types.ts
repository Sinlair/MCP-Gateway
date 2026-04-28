export type JsonRpcId = string | number;

export interface JsonRpcRequest<TParams = unknown> {
  jsonrpc: "2.0";
  id: JsonRpcId;
  method: string;
  params?: TParams;
}

export interface JsonRpcNotification<TParams = unknown> {
  jsonrpc: "2.0";
  method: string;
  params?: TParams;
}

export interface JsonRpcSuccess<TResult = unknown> {
  jsonrpc: "2.0";
  id: JsonRpcId;
  result: TResult;
}

export interface JsonRpcErrorObject {
  code: number;
  message: string;
  data?: unknown;
}

export interface JsonRpcErrorResponse {
  jsonrpc: "2.0";
  id: JsonRpcId | null;
  error: JsonRpcErrorObject;
}

export type JsonRpcMessage<TParams = unknown, TResult = unknown> =
  | JsonRpcRequest<TParams>
  | JsonRpcNotification<TParams>
  | JsonRpcSuccess<TResult>
  | JsonRpcErrorResponse;

export type JsonSchema = {
  title?: string;
  description?: string;
  type?: "string" | "number" | "integer" | "boolean" | "object" | "array";
  enum?: string[];
  default?: unknown;
  properties?: Record<string, JsonSchema>;
  required?: string[];
  items?: JsonSchema;
};

export interface McpTool {
  name: string;
  description?: string;
  inputSchema: JsonSchema;
}

export interface McpResource {
  uri: string;
  name?: string;
  description?: string;
  mimeType?: string;
}

export interface McpPrompt {
  name: string;
  description?: string;
  template?: string;
}

export interface McpServerCapabilities {
  tools: McpTool[];
  resources: McpResource[];
  prompts: McpPrompt[];
}

export type TransportType = "sse" | "websocket" | "stdio";

export type ServerStatus =
  | "disconnected"
  | "connecting"
  | "ready"
  | "busy"
  | "error";

export interface McpServer {
  id: string;
  name: string;
  endpoint: string;
  transport: TransportType;
  status: ServerStatus;
  capabilities?: McpServerCapabilities;
  latencyMs?: number;
  uptimeMs?: number;
  lastConnectedAt?: string;
  lastError?: string;
}

export type TrafficDirection = "inbound" | "outbound";
export type TrafficKind = "request" | "response" | "error" | "notification";

export interface TrafficEntry {
  id: string;
  serverId: string;
  timestamp: string;
  direction: TrafficDirection;
  kind: TrafficKind;
  payload: JsonRpcMessage;
}

export interface McpServerState {
  servers: McpServer[];
  traffic: TrafficEntry[];
  selectedServerId?: string;
  selectedTool?: string;
  retentionLimit: number;
}

export interface McpServerActions {
  addServer: (server: McpServer) => void;
  updateServer: (serverId: string, updates: Partial<McpServer>) => void;
  removeServer: (serverId: string) => void;
  selectServer: (serverId?: string) => void;
  selectTool: (toolName?: string) => void;
  recordTraffic: (entry: TrafficEntry) => void;
  clearTraffic: () => void;
  setCapabilities: (serverId: string, capabilities: McpServerCapabilities) => void;
}

export type McpServerStore = McpServerState & McpServerActions;
