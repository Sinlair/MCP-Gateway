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
  private postUrl?: string;
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
    await this.openEventSource();
    this.startHeartbeat();
  }

  disconnect() {
    this.closed = true;
    this.eventSource?.close();
    this.eventSource = undefined;
    this.postUrl = undefined;
    this.clearHeartbeat();
    this.handlers.onStatus?.("disconnected");
  }

  async send(message: JsonRpcRequest | JsonRpcNotification) {
    const postUrl = this.postUrl ?? this.options.postUrl;
    if (!postUrl) {
      throw new Error("SSE message endpoint is not ready.");
    }
    await fetch(postUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(message),
    });
  }

  private openEventSource() {
    if (this.closed) {
      return Promise.resolve();
    }
    return new Promise<void>((resolve, reject) => {
      this.eventSource?.close();
      this.postUrl = undefined;
      this.eventSource = new EventSource(this.options.endpoint);
      let settled = false;

      const timeout = setTimeout(() => {
        if (settled) {
          return;
        }
        settled = true;
        this.eventSource?.close();
        this.handlers.onStatus?.("error");
        reject(new Error("SSE endpoint negotiation timeout."));
      }, 10000);

      const resolveConnected = () => {
        if (settled) {
          return;
        }
        settled = true;
        clearTimeout(timeout);
        this.reconnectAttempts = 0;
        this.handlers.onStatus?.("connected");
        resolve();
      };

      this.eventSource.onopen = () => {
        this.reconnectAttempts = 0;
      };
      this.eventSource.addEventListener("endpoint", (event) => {
        this.lastMessageAt = Date.now();
        this.postUrl = (event as MessageEvent<string>).data;
        resolveConnected();
      });
      this.eventSource.onmessage = (event) => {
        this.handleMessageEvent(event);
      };
      this.eventSource.onerror = () => {
        this.handlers.onStatus?.("error");
        this.scheduleReconnect();
        if (!settled) {
          settled = true;
          clearTimeout(timeout);
          reject(new Error("SSE connection failed."));
        }
      };
    });
  }

  private handleMessageEvent(event: MessageEvent<string>) {
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
    setTimeout(() => {
      void this.openEventSource().catch((error) =>
        this.handlers.onError?.(error as Error)
      );
    }, delay * this.reconnectAttempts);
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
      void this.send(ping).catch((error) =>
        this.handlers.onError?.(error as Error)
      );
    }, intervalMs);
  }

  private clearHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = undefined;
    }
  }
}
