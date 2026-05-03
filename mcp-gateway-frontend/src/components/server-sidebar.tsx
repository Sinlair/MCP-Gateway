"use client";

import { AlertTriangle, CircleCheck, LoaderCircle, Power, Radio, RotateCw, ServerCog } from "lucide-react";

import { useMcp } from "@/hooks/useMcp";
import { useMcpServerStore } from "@/store/useMcpServerStore";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import type { McpServer, ServerStatus } from "@/types";

const statusLabel: Record<ServerStatus, string> = {
  ready: "运行中",
  busy: "执行中",
  connecting: "连接中",
  disconnected: "未连接",
  error: "异常",
};

const statusTone: Record<ServerStatus, string> = {
  ready: "border-emerald-200 bg-emerald-50 text-emerald-700",
  busy: "border-amber-200 bg-amber-50 text-amber-700",
  connecting: "border-sky-200 bg-sky-50 text-sky-700",
  disconnected: "border-slate-200 bg-slate-50 text-slate-600",
  error: "border-rose-200 bg-rose-50 text-rose-700",
};

const statusIcon: Record<ServerStatus, typeof CircleCheck> = {
  ready: CircleCheck,
  busy: LoaderCircle,
  connecting: Radio,
  disconnected: Power,
  error: AlertTriangle,
};

export function ServerSidebar() {
  const { connect, disconnect, initialize, getClient } = useMcp();
  const servers = useMcpServerStore((state) => state.servers);
  const selectedServerId = useMcpServerStore((state) => state.selectedServerId);
  const selectServer = useMcpServerStore((state) => state.selectServer);

  const ensureConnected = async (server: McpServer) => {
    if (
      !getClient(server.id) ||
      server.status === "disconnected" ||
      server.status === "error"
    ) {
      await connect(server.id);
    }
  };

  const handleConnectionToggle = async (server: McpServer) => {
    const canConnect =
      server.status === "disconnected" || server.status === "error";
    try {
      if (canConnect) {
        await ensureConnected(server);
        await initialize(server.id);
        return;
      }
      disconnect(server.id);
    } catch (error) {
      console.error("Failed to update MCP server connection.", error);
    }
  };

  const handleInitialize = async (server: McpServer) => {
    try {
      await ensureConnected(server);
      await initialize(server.id);
    } catch (error) {
      console.error("Failed to initialize MCP server.", error);
    }
  };

  return (
    <section className="h-full rounded-lg border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <div>
          <p className="text-sm font-semibold text-slate-950">服务接入</p>
          <p className="mt-0.5 text-xs text-slate-500">管理服务通道与运行状态</p>
        </div>
        <div className="flex h-8 w-8 items-center justify-center rounded-md bg-blue-50 text-blue-700">
          <ServerCog className="h-4 w-4" />
        </div>
      </div>
      <div className="p-3">
        <ScrollArea className="h-[620px] pr-2">
          <div className="space-y-3">
            {servers.map((server) => {
              const StatusIcon = statusIcon[server.status];
              const isSelected = selectedServerId === server.id;
              const canConnect =
                server.status === "disconnected" || server.status === "error";

              return (
                <article
                  key={server.id}
                  className={cn(
                    "rounded-lg border p-3 transition-colors",
                    isSelected
                      ? "border-blue-200 bg-blue-50/70 shadow-sm"
                      : "border-slate-200 bg-white hover:border-slate-300"
                  )}
                >
                  <button
                    type="button"
                    onClick={() => selectServer(server.id)}
                    className="flex w-full flex-col items-start gap-1 text-left"
                  >
                    <span className="line-clamp-1 text-sm font-semibold text-slate-950">
                      {server.name}
                    </span>
                    <span className="text-xs leading-5 text-slate-500">
                      已配置 {server.transport.toUpperCase()} 接入通道
                    </span>
                  </button>
                  <div className="mt-3 flex flex-wrap items-center gap-2">
                    <Badge className={statusTone[server.status]}>
                      <StatusIcon className="mr-1 h-3 w-3" />
                      {statusLabel[server.status]}
                    </Badge>
                    <Badge className="border-slate-200 bg-white text-slate-600">
                      {server.transport.toUpperCase()}
                    </Badge>
                  </div>
                  {server.lastError ? (
                    <p className="mt-2 line-clamp-2 rounded-md bg-rose-50 px-2 py-1.5 text-xs leading-5 text-rose-700">
                      {server.lastError}
                    </p>
                  ) : null}
                  <div className="mt-3 grid grid-cols-2 gap-2">
                    <Button
                      size="sm"
                      variant={canConnect ? "default" : "outline"}
                      onClick={() => void handleConnectionToggle(server)}
                    >
                      <Power className="h-4 w-4" />
                      {canConnect ? "连接" : "断开"}
                    </Button>
                    <Button
                      size="sm"
                      variant="secondary"
                      className="bg-slate-100 text-slate-700 hover:bg-slate-200"
                      onClick={() => void handleInitialize(server)}
                    >
                      <RotateCw className="h-4 w-4" />
                      初始化
                    </Button>
                  </div>
                </article>
              );
            })}
            {servers.length === 0 ? (
              <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-500">
                暂未配置服务端点
              </div>
            ) : null}
          </div>
        </ScrollArea>
      </div>
    </section>
  );
}
