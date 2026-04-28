"use client";

import * as React from "react";
import { useVirtualizer } from "@tanstack/react-virtual";

import { useMcpServerStore } from "@/store/useMcpServerStore";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { TrafficEntry, TrafficKind } from "@/types";

const kindColors: Record<TrafficKind, string> = {
  request: "text-info",
  response: "text-success",
  error: "text-destructive",
  notification: "text-warning",
};

export function TrafficInspector() {
  const { traffic, clearTraffic } = useMcpServerStore((state) => ({
    traffic: state.traffic,
    clearTraffic: state.clearTraffic,
  }));
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
              variant={filters[kind] ? "secondary" : "ghost"}
              onClick={() => toggleFilter(kind)}
            >
              {kind}
            </Button>
          ))}
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="outline">{filtered.length} entries</Badge>
          <Button size="sm" variant="outline" onClick={clearTraffic}>
            Clear
          </Button>
        </div>
      </div>
      <div
        ref={parentRef}
        className="flex-1 overflow-auto rounded-lg border border-border/60"
      >
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
                <div className="rounded-lg border border-border/60 bg-background/50 p-3 text-xs">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span
                        className={cn(
                          "font-semibold capitalize",
                          kindColors[entry.kind]
                        )}
                      >
                        {entry.kind}
                      </span>
                      <span className="text-muted-foreground">
                        {entry.direction}
                      </span>
                      <Badge variant="outline">{entry.serverId}</Badge>
                    </div>
                    <span className="text-muted-foreground">
                      {new Date(entry.timestamp).toLocaleTimeString()}
                    </span>
                  </div>
                  <pre className="mt-2 whitespace-pre-wrap text-[11px] text-foreground/90">
                    {JSON.stringify(entry.payload, null, 2)}
                  </pre>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
