import type {
  JsonRpcMessage,
  JsonRpcNotification,
  JsonRpcRequest,
} from "@/types";
import type { McpTransport, TransportHandlers, TransportOptions } from "./types";

export interface WebSocketTransportOptions extends TransportOptions {
  protocols?: string | string[];
}

export class WebSocketTransport implements McpTransport {
  public readonly type = "websocket" as const;
  private socket?: WebSocket;
  private handlers: TransportHandlers = {};
  private reconnectAttempts = 0;
  private heartbeatTimer?: ReturnType<typeof setInterval>;
  private lastMessageAt = Date.now();
  private closed = false;

  constructor(private options: WebSocketTransportOptions) {}

  setHandlers(handlers: TransportHandlers) {
    this.handlers = handlers;
  }

  async connect() {
    this.closed = false;
    this.openSocket();
    this.startHeartbeat();
  }

  disconnect() {
    this.closed = true;
    this.socket?.close();
    this.socket = undefined;
    this.clearHeartbeat();
    this.handlers.onStatus?.("disconnected");
  }

  async send(message: JsonRpcRequest | JsonRpcNotification) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      throw new Error("WebSocket is not connected.");
    }
    this.socket.send(JSON.stringify(message));
  }

  private openSocket() {
    if (this.closed) {
      return;
    }
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      return;
    }
    this.socket = new WebSocket(this.options.endpoint, this.options.protocols);
    this.socket.onopen = () => {
      this.reconnectAttempts = 0;
      this.handlers.onStatus?.("connected");
    };
    this.socket.onmessage = (event) => {
      this.lastMessageAt = Date.now();
      try {
        const parsed = JSON.parse(event.data) as JsonRpcMessage;
        this.handlers.onMessage?.(parsed);
      } catch (error) {
        this.handlers.onError?.(error as Error);
      }
    };
    this.socket.onerror = () => {
      this.handlers.onStatus?.("error");
    };
    this.socket.onclose = () => {
      this.handlers.onStatus?.("disconnected");
      this.scheduleReconnect();
    };
  }

  private scheduleReconnect() {
    if (this.closed) {
      return;
    }
    const maxAttempts = this.options.reconnectAttempts ?? 5;
    const delay = this.options.reconnectDelayMs ?? 1000;
    if (this.reconnectAttempts >= maxAttempts) {
      this.handlers.onError?.(
        new Error("WebSocket reconnect attempts exhausted.")
      );
      return;
    }
    this.reconnectAttempts += 1;
    setTimeout(() => this.openSocket(), delay * this.reconnectAttempts);
  }

  private startHeartbeat() {
    this.clearHeartbeat();
    const intervalMs = this.options.heartbeatIntervalMs ?? 15000;
    const timeoutMs = this.options.heartbeatTimeoutMs ?? 45000;
    this.heartbeatTimer = setInterval(() => {
      const now = Date.now();
      if (now - this.lastMessageAt > timeoutMs) {
        this.handlers.onError?.(new Error("WebSocket heartbeat timeout."));
        this.scheduleReconnect();
      }
      const ping: JsonRpcNotification = {
        jsonrpc: "2.0",
        method: "ping",
      };
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify(ping));
      }
    }, intervalMs);
  }

  private clearHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = undefined;
    }
  }
}
