"use client";

import * as React from "react";

import { useMcp } from "@/hooks/useMcp";
import { useMcpServerStore } from "@/store/useMcpServerStore";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import type { ServerStatus } from "@/types";

const statusBadge: Record<ServerStatus, "success" | "warning" | "destructive" | "info"> = {
  ready: "success",
  busy: "warning",
  connecting: "info",
  disconnected: "warning",
  error: "destructive",
};

export function ServerSidebar() {
  const { connect, disconnect, initialize } = useMcp();
  const { servers, selectedServerId, selectServer } = useMcpServerStore(
    (state) => ({
      servers: state.servers,
      selectedServerId: state.selectedServerId,
      selectServer: state.selectServer,
    })
  );

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle>Servers</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <ScrollArea className="h-[420px] pr-2">
          <div className="space-y-3">
            {servers.map((server) => (
              <div
                key={server.id}
                className={cn(
                  "rounded-lg border border-border/60 p-3 transition-colors",
                  selectedServerId === server.id && "bg-secondary/40"
                )}
              >
                <button
                  type="button"
                  onClick={() => selectServer(server.id)}
                  className="flex w-full flex-col items-start gap-1 text-left"
                >
                  <span className="text-sm font-semibold">{server.name}</span>
                  <span className="text-xs text-muted-foreground">
                    {server.endpoint}
                  </span>
                </button>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  <Badge variant={statusBadge[server.status]}>
                    {server.status}
                  </Badge>
                  <Badge variant="outline">{server.transport}</Badge>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  {server.status === "disconnected" ? (
                    <Button size="sm" onClick={() => connect(server.id)}>
                      Connect
                    </Button>
                  ) : (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => disconnect(server.id)}
                    >
                      Disconnect
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => initialize(server.id)}
                  >
                    Initialize
                  </Button>
                </div>
              </div>
            ))}
            {servers.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                No servers configured.
              </p>
            ) : null}
          </div>
        </ScrollArea>
      </CardContent>
    </Card>
  );
}
