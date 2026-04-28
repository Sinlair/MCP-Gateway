import type {
  JsonRpcMessage,
  JsonRpcNotification,
  JsonRpcRequest,
} from "@/types";
import type { McpTransport, TransportHandlers, TransportOptions } from "./types";

export interface SseTransportOptions extends TransportOptions {
  postUrl?: string;
}

export class SseTransport implements McpTransport {
  public readonly type = "sse" as const;
  private eventSource?: EventSource;
  private handlers: TransportHandlers = {};
  private reconnectAttempts = 0;
  private heartbeatTimer?: ReturnType<typeof setInterval>;
  private lastMessageAt = Date.now();
  private closed = false;

  constructor(private options: SseTransportOptions) {}

  setHandlers(handlers: TransportHandlers) {
    this.handlers = handlers;
  }

  async connect() {
    this.closed = false;
    this.openEventSource();
    this.startHeartbeat();
  }

  disconnect() {
    this.closed = true;
    this.eventSource?.close();
    this.eventSource = undefined;
    this.clearHeartbeat();
    this.handlers.onStatus?.("disconnected");
  }

  async send(message: JsonRpcRequest | JsonRpcNotification) {
    const postUrl = this.options.postUrl ?? this.options.endpoint;
    await fetch(postUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(message),
    });
  }

  private openEventSource() {
    if (this.closed) {
      return;
    }
    this.eventSource?.close();
    this.eventSource = new EventSource(this.options.endpoint);
    this.eventSource.onopen = () => {
      this.reconnectAttempts = 0;
      this.handlers.onStatus?.("connected");
    };
    this.eventSource.onmessage = (event) => {
      this.lastMessageAt = Date.now();
      if (!event.data) {
        return;
      }
      try {
        const parsed = JSON.parse(event.data) as JsonRpcMessage;
        this.handlers.onMessage?.(parsed);
      } catch (error) {
        this.handlers.onError?.(error as Error);
      }
    };
    this.eventSource.onerror = () => {
      this.handlers.onStatus?.("error");
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
        new Error("SSE reconnect attempts exhausted.")
      );
      return;
    }
    this.reconnectAttempts += 1;
    setTimeout(() => this.openEventSource(), delay * this.reconnectAttempts);
  }

  private startHeartbeat() {
    this.clearHeartbeat();
    const intervalMs = this.options.heartbeatIntervalMs ?? 15000;
    const timeoutMs = this.options.heartbeatTimeoutMs ?? 45000;
    this.heartbeatTimer = setInterval(() => {
      const now = Date.now();
      if (now - this.lastMessageAt > timeoutMs) {
        this.handlers.onError?.(new Error("SSE heartbeat timeout."));
        this.eventSource?.close();
        this.scheduleReconnect();
      }
      const ping: JsonRpcNotification = {
        jsonrpc: "2.0",
        method: "ping",
      };
      void this.send(ping);
    }, intervalMs);
  }

  private clearHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = undefined;
    }
  }
}
