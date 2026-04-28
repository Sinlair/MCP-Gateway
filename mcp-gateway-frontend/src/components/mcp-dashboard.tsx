"use client";

import * as React from "react";

import { useMcp } from "@/hooks/useMcp";
import { useMcpServerStore } from "@/store/useMcpServerStore";
import { TrafficInspector } from "@/components/traffic-inspector";
import { ResourceBrowser } from "@/components/resource-browser";
import { ServerSidebar } from "@/components/server-sidebar";
import { SystemHealth } from "@/components/system-health";
import { ToolRenderer } from "@/components/tool-renderer";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { McpResource } from "@/types";
import { cn } from "@/lib/utils";

export function McpDashboard() {
  const { callTool, readResource } = useMcp();
  const {
    selectedServer,
    tools,
    resources,
    selectedTool,
    selectTool,
  } = useMcpServerStore((state) => {
    const server = state.servers.find(
      (item) => item.id === state.selectedServerId
    );
    return {
      selectedServer: server,
      tools: server?.capabilities?.tools ?? [],
      resources: server?.capabilities?.resources ?? [],
      selectedTool: state.selectedTool,
      selectTool: state.selectTool,
    };
  });

  const [toolResult, setToolResult] = React.useState<unknown>(null);
  const [resourceContent, setResourceContent] = React.useState<string>();
  const [resourceMime, setResourceMime] = React.useState<string>();
  const [selectedResource, setSelectedResource] = React.useState<string>();

  const activeTool = tools.find((tool) => tool.name === selectedTool);

  const handleExecute = async (args: unknown) => {
    if (!selectedServer || !activeTool) {
      return;
    }
    const result = await callTool(selectedServer.id, activeTool.name, args);
    setToolResult(result ?? { message: "No response received." });
  };

  const handleRead = async (resource: McpResource) => {
    if (!selectedServer) {
      return;
    }
    setSelectedResource(resource.uri);
    setResourceMime(resource.mimeType);
    const result = await readResource(selectedServer.id, resource.uri);
    setResourceContent(
      typeof result === "string" ? result : JSON.stringify(result, null, 2)
    );
  };

  return (
    <div className="flex min-h-screen flex-col gap-6 bg-background p-6">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">
            MCP Gateway Frontend
          </h1>
          <p className="text-sm text-muted-foreground">
            Monitor and control connected MCP servers.
          </p>
        </div>
        {selectedServer ? (
          <Badge variant="outline">
            Active: {selectedServer.name}
          </Badge>
        ) : (
          <Badge variant="outline">No server selected</Badge>
        )}
      </header>

      <SystemHealth />

      <div className="grid gap-6 lg:grid-cols-[280px_1fr_360px]">
        <ServerSidebar />

        <Card className="h-full">
          <CardHeader>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <CardTitle>Main Stage</CardTitle>
              <Badge variant="outline">
                {selectedServer?.transport ?? "no transport"}
              </Badge>
            </div>
          </CardHeader>
          <CardContent>
            <Tabs defaultValue="tools">
              <TabsList>
                <TabsTrigger value="tools">Tools</TabsTrigger>
                <TabsTrigger value="resources">Resources</TabsTrigger>
              </TabsList>
              <TabsContent value="tools">
                <div className="grid gap-4 lg:grid-cols-[220px_1fr]">
                  <Card className="h-full">
                    <CardHeader>
                      <CardTitle>Tools</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <ScrollArea className="h-[420px] pr-2">
                        <div className="space-y-2">
                          {tools.map((tool) => (
                            <Button
                              key={tool.name}
                              variant={
                                tool.name === activeTool?.name
                                  ? "secondary"
                                  : "ghost"
                              }
                              className={cn("w-full justify-start")}
                              onClick={() => selectTool(tool.name)}
                            >
                              {tool.name}
                            </Button>
                          ))}
                          {tools.length === 0 ? (
                            <p className="text-sm text-muted-foreground">
                              No tools advertised.
                            </p>
                          ) : null}
                        </div>
                      </ScrollArea>
                    </CardContent>
                  </Card>
                  <ToolRenderer
                    tool={activeTool}
                    onExecute={handleExecute}
                    result={toolResult}
                  />
                </div>
              </TabsContent>
              <TabsContent value="resources">
                <ResourceBrowser
                  resources={resources}
                  selectedUri={selectedResource}
                  content={resourceContent}
                  mimeType={resourceMime}
                  onRead={handleRead}
                />
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>

        <Card className="h-full">
          <CardHeader>
            <CardTitle>Traffic Inspector</CardTitle>
          </CardHeader>
          <CardContent className="h-[620px]">
            <TrafficInspector />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
