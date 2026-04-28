import type {
  JsonRpcNotification,
  JsonRpcRequest,
} from "@/types";
import type { McpTransport, TransportHandlers, TransportOptions } from "./types";

export class StdioTransport implements McpTransport {
  public readonly type = "stdio" as const;
  private handlers: TransportHandlers = {};

  constructor(private options: TransportOptions) {}

  setHandlers(handlers: TransportHandlers) {
    this.handlers = handlers;
  }

  async connect() {
    this.handlers.onStatus?.("connected");
  }

  disconnect() {
    this.handlers.onStatus?.("disconnected");
  }

  async send(message: JsonRpcRequest | JsonRpcNotification) {
    const payload = JSON.stringify(message);
    this.handlers.onError?.(
      new Error(
        `Stdio transport is not supported in the browser runtime. Attempted to send: ${payload}`
      )
    );
  }
}
