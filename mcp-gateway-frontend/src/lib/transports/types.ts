import type {
  JsonRpcMessage,
  JsonRpcNotification,
  JsonRpcRequest,
  TransportType,
} from "@/types";

export type TransportStatus = "connected" | "disconnected" | "error";

export interface TransportHandlers {
  onMessage?: (message: JsonRpcMessage) => void;
  onStatus?: (status: TransportStatus) => void;
  onError?: (error: Error) => void;
}

export interface TransportOptions {
  endpoint: string;
  heartbeatIntervalMs?: number;
  heartbeatTimeoutMs?: number;
  reconnectAttempts?: number;
  reconnectDelayMs?: number;
}

export interface McpTransport {
  type: TransportType;
  connect: () => Promise<void>;
  disconnect: () => void;
  send: (message: JsonRpcRequest | JsonRpcNotification) => Promise<void>;
  setHandlers: (handlers: TransportHandlers) => void;
}
