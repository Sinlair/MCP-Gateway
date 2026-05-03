import type {
  JsonRpcId,
  JsonRpcMessage,
  JsonRpcNotification,
  JsonRpcRequest,
  JsonRpcSuccess,
  JsonRpcErrorResponse,
} from "@/types";
import type { McpTransport, TransportStatus } from "@/lib/transports";

let idCounter = 0;

export function createJsonRpcId(): JsonRpcId {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  idCounter += 1;
  return `${Date.now()}-${idCounter}`;
}

export function createJsonRpcRequest<TParams = unknown>(
  method: string,
  params?: TParams
): JsonRpcRequest<TParams> {
  return {
    jsonrpc: "2.0",
    id: createJsonRpcId(),
    method,
    params,
  };
}

export interface JsonRpcClientOptions {
  timeoutMs?: number;
  onSend?: (message: JsonRpcRequest | JsonRpcNotification) => void;
  onReceive?: (message: JsonRpcMessage) => void;
  onStatus?: (status: TransportStatus) => void;
  onError?: (error: Error) => void;
}

export class JsonRpcClient {
  private pending = new Map<
    JsonRpcId,
    {
      resolve: (value: unknown) => void;
      reject: (reason?: unknown) => void;
      timeout?: ReturnType<typeof setTimeout>;
    }
  >();

  constructor(
    private transport: McpTransport,
    private options: JsonRpcClientOptions = {}
  ) {
    this.transport.setHandlers({
      onMessage: (message) => this.handleMessage(message),
      onStatus: (status) => this.options.onStatus?.(status),
      onError: (error) => this.options.onError?.(error),
    });
  }

  async connect() {
    await this.transport.connect();
  }

  disconnect() {
    this.transport.disconnect();
  }

  async request<TResult = unknown, TParams = unknown>(
    method: string,
    params?: TParams
  ): Promise<TResult> {
    const message = createJsonRpcRequest(method, params);
    const pendingResponse = new Promise<TResult>((resolve, reject) => {
      const timeoutMs = this.options.timeoutMs ?? 30000;
      const timeout = setTimeout(() => {
        this.pending.delete(message.id);
        reject(new Error(`Request timeout for ${method}`));
      }, timeoutMs);

      this.pending.set(message.id, {
        resolve: (value) => resolve(value as TResult),
        reject,
        timeout,
      });
    });

    this.options.onSend?.(message);
    try {
      await this.transport.send(message);
    } catch (error) {
      const pending = this.pending.get(message.id);
      if (pending) {
        if (pending.timeout) {
          clearTimeout(pending.timeout);
        }
        this.pending.delete(message.id);
        pending.reject(error);
      }
    }

    return pendingResponse;
  }

  async notify<TParams = unknown>(method: string, params?: TParams) {
    const message: JsonRpcNotification = {
      jsonrpc: "2.0",
      method,
      params,
    };
    this.options.onSend?.(message);
    await this.transport.send(message);
  }

  private handleMessage(message: JsonRpcMessage) {
    this.options.onReceive?.(message);

    if ("id" in message && "result" in message) {
      this.resolve(message as JsonRpcSuccess);
      return;
    }

    if ("id" in message && "error" in message) {
      this.reject(message as JsonRpcErrorResponse);
    }
  }

  private resolve(message: JsonRpcSuccess) {
    const pending = this.pending.get(message.id);
    if (!pending) {
      return;
    }
    if (pending.timeout) {
      clearTimeout(pending.timeout);
    }
    pending.resolve(message.result);
    this.pending.delete(message.id);
  }

  private reject(message: JsonRpcErrorResponse) {
    if (message.id === null) {
      return;
    }
    const pending = this.pending.get(message.id);
    if (!pending) {
      return;
    }
    if (pending.timeout) {
      clearTimeout(pending.timeout);
    }
    pending.reject(message.error);
    this.pending.delete(message.id);
  }
}
