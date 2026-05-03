"use client";

import * as React from "react";
import { useVirtualizer } from "@tanstack/react-virtual";

import { useMcpServerStore } from "@/store/useMcpServerStore";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { TrafficEntry, TrafficKind } from "@/types";

const kindLabel: Record<TrafficKind, string> = {
  request: "请求",
  response: "响应",
  error: "异常",
  notification: "通知",
};

const directionLabel: Record<TrafficEntry["direction"], string> = {
  inbound: "入站",
  outbound: "出站",
};

const kindTone: Record<TrafficKind, string> = {
  request: "border-sky-200 bg-sky-50 text-sky-700",
  response: "border-emerald-200 bg-emerald-50 text-emerald-700",
  error: "border-rose-200 bg-rose-50 text-rose-700",
  notification: "border-amber-200 bg-amber-50 text-amber-700",
};

export function TrafficInspector() {
  const traffic = useMcpServerStore((state) => state.traffic);
  const clearTraffic = useMcpServerStore((state) => state.clearTraffic);
  const [filters, setFilters] = React.useState<Record<TrafficKind, boolean>>({
    request: true,
    response: true,
    error: true,
    notification: true,
  });

  const filtered = React.useMemo(
    () => traffic.filter((entry) => filters[entry.kind]),
    [traffic, filters]
  );

  const parentRef = React.useRef<HTMLDivElement>(null);
  const virtualizer = useVirtualizer({
    count: filtered.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 96,
    overscan: 8,
  });

  const toggleFilter = (kind: TrafficKind) => {
    setFilters((prev) => ({ ...prev, [kind]: !prev[kind] }));
  };

  return (
    <div className="flex h-full flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap gap-2">
          {(Object.keys(filters) as TrafficKind[]).map((kind) => (
            <Button
              key={kind}
              size="sm"
              variant={filters[kind] ? "secondary" : "outline"}
              className={cn(
                "h-8 border-slate-200",
                filters[kind]
                  ? "bg-slate-900 text-white hover:bg-slate-800"
                  : "bg-white text-slate-500 hover:bg-slate-50"
              )}
              onClick={() => toggleFilter(kind)}
            >
              {kindLabel[kind]}
            </Button>
          ))}
        </div>
        <div className="flex items-center gap-2">
          <Badge className="border-slate-200 bg-white text-slate-600">
            {filtered.length} 条
          </Badge>
          <Button
            size="sm"
            variant="outline"
            className="h-8 border-slate-200 bg-white text-slate-600"
            onClick={clearTraffic}
          >
            清空
          </Button>
        </div>
      </div>
      <div
        ref={parentRef}
        className="flex-1 overflow-auto rounded-lg border border-slate-200 bg-slate-50"
      >
        {filtered.length === 0 ? (
          <div className="flex h-full min-h-64 items-center justify-center p-6 text-center">
            <div>
              <p className="text-sm font-medium text-slate-700">暂无流量记录</p>
              <p className="mt-1 text-xs text-slate-500">
                连接并初始化服务后，这里会展示 JSON-RPC 请求与响应。
              </p>
            </div>
          </div>
        ) : (
          <div
            style={{
              height: `${virtualizer.getTotalSize()}px`,
              width: "100%",
              position: "relative",
            }}
          >
            {virtualizer.getVirtualItems().map((virtualRow) => {
              const entry = filtered[virtualRow.index] as TrafficEntry;
              return (
                <div
                  key={entry.id}
                  className="absolute left-0 top-0 w-full p-3"
                  style={{
                    transform: `translateY(${virtualRow.start}px)`,
                  }}
                >
                  <div className="rounded-lg border border-slate-200 bg-white p-3 text-xs shadow-sm">
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex min-w-0 items-center gap-2">
                        <Badge className={kindTone[entry.kind]}>
                          {kindLabel[entry.kind]}
                        </Badge>
                        <span className="text-slate-500">
                          {directionLabel[entry.direction]}
                        </span>
                        <Badge className="border-slate-200 bg-white text-slate-600">
                          {entry.serverId}
                        </Badge>
                      </div>
                      <span className="shrink-0 text-slate-400">
                        {new Date(entry.timestamp).toLocaleTimeString()}
                      </span>
                    </div>
                    <pre className="mt-3 max-h-36 overflow-auto whitespace-pre-wrap rounded-md bg-slate-950 p-3 text-[11px] leading-5 text-slate-200">
                      {JSON.stringify(entry.payload, null, 2)}
                    </pre>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
