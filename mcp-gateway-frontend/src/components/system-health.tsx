"use client";

import * as React from "react";

import { useMcpServerStore } from "@/store/useMcpServerStore";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { ServerStatus } from "@/types";

const statusTone: Record<ServerStatus, "success" | "warning" | "destructive" | "info"> = {
  ready: "success",
  busy: "warning",
  connecting: "info",
  disconnected: "warning",
  error: "destructive",
};

function formatDuration(milliseconds?: number) {
  if (!milliseconds || milliseconds < 0) {
    return "--";
  }
  const totalSeconds = Math.floor(milliseconds / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  }
  if (minutes > 0) {
    return `${minutes}m ${seconds}s`;
  }
  return `${seconds}s`;
}

export function SystemHealth() {
  const servers = useMcpServerStore((state) => state.servers);
  const [, setTick] = React.useState(0);

  React.useEffect(() => {
    const interval = setInterval(() => setTick((prev) => prev + 1), 1000);
    return () => clearInterval(interval);
  }, []);

  return (
    <Card>
      <CardHeader>
        <CardTitle>System Health</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {servers.map((server) => {
          const lastConnected = server.lastConnectedAt
            ? Date.parse(server.lastConnectedAt)
            : undefined;
          const uptime =
            server.status === "ready" || server.status === "busy"
              ? Date.now() - (lastConnected ?? Date.now())
              : undefined;
          return (
            <div
              key={server.id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border/60 p-3"
            >
              <div>
                <p className="text-sm font-semibold">{server.name}</p>
                <p className="text-xs text-muted-foreground">{server.endpoint}</p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant={statusTone[server.status]}>
                  {server.status}
                </Badge>
                <Badge variant="outline">
                  {server.latencyMs ? `${server.latencyMs}ms` : "latency --"}
                </Badge>
                <Badge variant="outline">
                  uptime {formatDuration(uptime)}
                </Badge>
              </div>
              {server.lastError ? (
                <p className="w-full text-xs text-destructive">
                  {server.lastError}
                </p>
              ) : null}
            </div>
          );
        })}
        {servers.length === 0 ? (
          <p className="text-sm text-muted-foreground">No servers configured.</p>
        ) : null}
      </CardContent>
    </Card>
  );
}
