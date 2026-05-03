"use client";

import * as React from "react";
import {
  Activity,
  Bell,
  Blocks,
  CheckCircle2,
  Clock3,
  FileCode2,
  Gauge,
  GitBranch,
  LayoutDashboard,
  Network,
  Radio,
  RotateCcw,
  Search,
  ServerCog,
  ShieldCheck,
  TerminalSquare,
  Wrench,
  Zap,
} from "lucide-react";

import { useMcp } from "@/hooks/useMcp";
import { useMcpServerStore } from "@/store/useMcpServerStore";
import { TrafficInspector } from "@/components/traffic-inspector";
import { ResourceBrowser } from "@/components/resource-browser";
import { ServerSidebar } from "@/components/server-sidebar";
import { ToolRenderer } from "@/components/tool-renderer";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { McpResource, McpTool, ServerStatus } from "@/types";
import { cn } from "@/lib/utils";

const EMPTY_TOOLS: McpTool[] = [];
const EMPTY_RESOURCES: McpResource[] = [];

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

type ConsoleNavId = "workbench" | "catalog" | "tools" | "audit" | "versions";
type WorkspaceTab = "tools" | "resources" | "terminal";

const navItems: Array<{
  id: ConsoleNavId;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  targetId: string;
}> = [
  { id: "workbench", label: "网关工作台", icon: LayoutDashboard, targetId: "workbench" },
  { id: "catalog", label: "服务目录", icon: ServerCog, targetId: "service-catalog" },
  { id: "tools", label: "工具发布", icon: Wrench, targetId: "tool-publish" },
  { id: "audit", label: "流量审计", icon: Activity, targetId: "traffic-audit" },
  { id: "versions", label: "配置版本", icon: GitBranch, targetId: "config-versions" },
];

function Metric({
  icon: Icon,
  label,
  value,
  hint,
  tone = "text-slate-700",
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string | number;
  hint: string;
  tone?: string;
}) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <div className="space-y-1">
          <p className="text-xs font-medium text-slate-500">{label}</p>
          <p className="text-2xl font-semibold text-slate-950">{value}</p>
        </div>
        <div className={cn("rounded-md bg-slate-100 p-2", tone)}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
      <p className="mt-3 text-xs text-slate-500">{hint}</p>
    </div>
  );
}

export function McpDashboard() {
  const { callTool, readResource, connect, disconnect, initialize, getClient } = useMcp();
  const servers = useMcpServerStore((state) => state.servers);
  const selectedServerId = useMcpServerStore((state) => state.selectedServerId);
  const selectedTool = useMcpServerStore((state) => state.selectedTool);
  const selectTool = useMcpServerStore((state) => state.selectTool);
  const traffic = useMcpServerStore((state) => state.traffic);
  const selectedServer = useMcpServerStore((state) =>
    state.servers.find((item) => item.id === selectedServerId)
  );

  const [toolResult, setToolResult] = React.useState<unknown>(null);
  const [resourceContent, setResourceContent] = React.useState<string>();
  const [resourceMime, setResourceMime] = React.useState<string>();
  const [selectedResource, setSelectedResource] = React.useState<string>();
  const [activeNavId, setActiveNavId] = React.useState<ConsoleNavId>("workbench");
  const [activeWorkspaceTab, setActiveWorkspaceTab] =
    React.useState<WorkspaceTab>("tools");

  const tools = selectedServer?.capabilities?.tools ?? EMPTY_TOOLS;
  const resources = selectedServer?.capabilities?.resources ?? EMPTY_RESOURCES;
  const activeTool = tools.find((tool) => tool.name === selectedTool);
  const status = selectedServer?.status ?? "disconnected";

  const handleExecute = async (args: unknown) => {
    if (!selectedServer || !activeTool) {
      return;
    }
    const result = await callTool(selectedServer.id, activeTool.name, args);
    setToolResult(result ?? { message: "暂无服务响应" });
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

  const ensureConnected = async () => {
    if (!selectedServer) {
      return false;
    }

    if (
      !getClient(selectedServer.id) ||
      selectedServer.status === "disconnected" ||
      selectedServer.status === "error"
    ) {
      await connect(selectedServer.id);
    }

    return true;
  };

  const handlePrimaryAction = async () => {
    if (!selectedServer) {
      return;
    }
    try {
      if (selectedServer.status === "disconnected" || selectedServer.status === "error") {
        if (await ensureConnected()) {
          await initialize(selectedServer.id);
        }
        return;
      }

      disconnect(selectedServer.id);
    } catch (error) {
      console.error("Failed to update MCP connection.", error);
    }
  };

  const handleInitialize = async () => {
    try {
      if (selectedServer && (await ensureConnected())) {
        await initialize(selectedServer.id);
      }
    } catch (error) {
      console.error("Failed to initialize MCP server.", error);
    }
  };

  const handleNavClick = (item: (typeof navItems)[number]) => {
    setActiveNavId(item.id);
    const target = document.getElementById(item.targetId);
    if (target) {
      target.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  };

  return (
    <div className="min-h-screen overflow-x-hidden bg-[#f6f8fb] text-slate-950">
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 border-r border-slate-200 bg-white lg:flex lg:flex-col">
        <div className="flex h-16 items-center gap-3 border-b border-slate-200 px-5">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 text-white">
            <Network className="h-5 w-5" />
          </div>
          <div>
            <p className="text-sm font-semibold text-slate-950">智能服务网关</p>
            <p className="text-xs text-slate-500">SaaS 运营控制台</p>
          </div>
        </div>
        <nav className="flex-1 space-y-7 px-3 py-5">
          <div>
            <p className="px-3 text-xs font-medium text-slate-400">控制面</p>
            <div className="mt-2 space-y-1">
              {navItems.map((item) => (
                <button
                  key={item.label}
                  className={cn(
                    "flex h-10 w-full items-center gap-3 rounded-lg px-3 text-sm transition-colors",
                    activeNavId === item.id
                      ? "bg-blue-50 font-medium text-blue-700"
                      : "text-slate-600 hover:bg-slate-100 hover:text-slate-950"
                  )}
                  aria-current={activeNavId === item.id ? "page" : undefined}
                  onClick={() => handleNavClick(item)}
                  type="button"
                >
                  <item.icon className="h-4 w-4" />
                  {item.label}
                </button>
              ))}
            </div>
          </div>
        </nav>
        <div className="border-t border-slate-200 p-4">
          <div className="flex items-center gap-3 rounded-lg bg-slate-50 p-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-md bg-emerald-100 text-emerald-700">
              <ShieldCheck className="h-4 w-4" />
            </div>
            <div className="min-w-0">
              <p className="text-sm font-medium text-slate-800">运营控制台</p>
              <p className="text-xs text-slate-500">基础管理权限</p>
            </div>
          </div>
        </div>
      </aside>

      <main className="min-h-screen lg:pl-64">
        <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur">
          <div className="grid min-h-16 grid-cols-1 gap-3 px-4 py-3 sm:flex sm:flex-nowrap sm:items-center sm:justify-between sm:px-6">
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <h1 className="truncate text-lg font-semibold text-slate-950">
                  网关工作台
                </h1>
                <Badge className={statusTone[status]}>{statusLabel[status]}</Badge>
              </div>
                <p className="mt-0.5 text-xs text-slate-500">
                  {selectedServer?.name ?? "智能服务网关"} ·{" "}
                  {selectedServer?.transport.toUpperCase() ?? "SSE"}
                </p>
            </div>
            <div className="hidden min-w-0 flex-1 justify-center px-6 md:flex">
              <div className="flex h-10 w-full max-w-md items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-400">
                <Search className="h-4 w-4" />
                <span className="truncate">搜索路由、工具、会话 ID</span>
              </div>
            </div>
            <div className="grid w-full min-w-0 max-w-full grid-cols-2 gap-2 sm:ml-auto sm:flex sm:w-auto sm:shrink-0 sm:items-center">
              <Button
                variant="outline"
                size="sm"
                className="min-w-0 w-full sm:w-auto"
                onClick={handleInitialize}
              >
                <RotateCcw className="h-4 w-4" />
                初始化
              </Button>
              <Button
                size="sm"
                className="min-w-0 w-full sm:w-auto"
                onClick={handlePrimaryAction}
              >
                <Zap className="h-4 w-4" />
                {status === "disconnected" || status === "error" ? "连接" : "断开"}
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="hidden sm:inline-flex"
                aria-label="通知"
              >
                <Bell className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </header>

        <div className="space-y-5 p-4 sm:p-6">
          <section
            id="workbench"
            className="scroll-mt-24 grid gap-4 md:grid-cols-2 xl:grid-cols-4"
          >
            <Metric
              icon={ServerCog}
              label="服务实例"
              value={servers.length}
              hint="当前控制台可见服务"
              tone="text-blue-700"
            />
            <Metric
              icon={Blocks}
              label="可发布工具"
              value={tools.length}
              hint={tools.length > 0 ? "已完成能力发现" : "等待服务初始化"}
              tone="text-emerald-700"
            />
            <Metric
              icon={Activity}
              label="审计事件"
              value={traffic.length}
              hint="当前浏览器会话"
              tone="text-amber-700"
            />
            <Metric
              icon={Gauge}
              label="延迟"
              value={selectedServer?.latencyMs ? `${selectedServer.latencyMs}ms` : "--"}
              hint="最近一次初始化耗时"
              tone="text-violet-700"
            />
          </section>

          <section className="grid gap-5 xl:grid-cols-[300px_minmax(0,1fr)] 2xl:grid-cols-[300px_minmax(0,1fr)_380px]">
            <div id="service-catalog" className="scroll-mt-24">
              <ServerSidebar />
            </div>

            <div
              id="tool-publish"
              className="scroll-mt-24 min-w-0 rounded-lg border border-slate-200 bg-white shadow-sm"
            >
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3">
                <div>
                  <p className="text-sm font-semibold text-slate-950">能力工作区</p>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {selectedServer
                      ? `${selectedServer.transport.toUpperCase()} 接入通道`
                      : "未选择服务"}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant="outline">
                    <Radio className="mr-1 h-3 w-3" />
                    {selectedServer?.transport.toUpperCase() ?? "SSE"}
                  </Badge>
                  {status === "ready" ? (
                    <Badge className="border-emerald-200 bg-emerald-50 text-emerald-700">
                      <CheckCircle2 className="mr-1 h-3 w-3" />
                      已就绪
                    </Badge>
                  ) : null}
                </div>
              </div>

              <Tabs
                value={activeWorkspaceTab}
                onValueChange={(value) =>
                  setActiveWorkspaceTab(value as WorkspaceTab)
                }
                className="p-4"
              >
                <TabsList className="bg-slate-100">
                  <TabsTrigger
                    value="tools"
                    onClick={() => setActiveWorkspaceTab("tools")}
                  >
                    <Wrench className="mr-2 h-4 w-4" />
                    工具
                  </TabsTrigger>
                  <TabsTrigger
                    value="resources"
                    onClick={() => setActiveWorkspaceTab("resources")}
                  >
                    <FileCode2 className="mr-2 h-4 w-4" />
                    资源
                  </TabsTrigger>
                  <TabsTrigger
                    value="terminal"
                    onClick={() => setActiveWorkspaceTab("terminal")}
                  >
                    <TerminalSquare className="mr-2 h-4 w-4" />
                    调试
                  </TabsTrigger>
                </TabsList>
                <TabsContent value="tools">
                  <div className="grid gap-4 lg:grid-cols-[230px_1fr]">
                    <div className="rounded-lg border border-slate-200 bg-slate-50">
                      <div className="border-b border-slate-200 px-3 py-3">
                        <p className="text-sm font-semibold text-slate-900">工具目录</p>
                        <p className="mt-0.5 text-xs text-slate-500">{tools.length} 个工具</p>
                      </div>
                      <div className="max-h-[520px] space-y-1 overflow-auto p-2">
                        {tools.map((tool) => (
                          <Button
                            key={tool.name}
                            variant={tool.name === activeTool?.name ? "secondary" : "ghost"}
                            className={cn(
                              "h-auto w-full justify-start whitespace-normal px-3 py-2 text-left",
                              tool.name === activeTool?.name && "bg-white shadow-sm"
                            )}
                            onClick={() => selectTool(tool.name)}
                          >
                            <Wrench className="h-4 w-4 shrink-0" />
                            <span className="min-w-0 truncate">{tool.name}</span>
                          </Button>
                        ))}
                        {tools.length === 0 ? (
                          <div className="rounded-lg border border-dashed border-slate-300 bg-white p-4 text-sm text-slate-500">
                            暂无可发布工具
                          </div>
                        ) : null}
                      </div>
                    </div>
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
                <TabsContent value="terminal">
                  <div className="rounded-lg border border-slate-200 bg-slate-950 p-4 text-sm text-slate-100">
                    <div className="mb-3 flex items-center gap-2 text-slate-400">
                      <Clock3 className="h-4 w-4" />
                      <span>最近请求</span>
                    </div>
                    <pre className="overflow-auto text-xs leading-6 text-slate-300">
                      {traffic.length > 0
                        ? JSON.stringify(traffic[traffic.length - 1]?.payload, null, 2)
                        : "暂无调试记录"}
                    </pre>
                  </div>
                </TabsContent>
              </Tabs>
            </div>

            <div
              id="traffic-audit"
              className="scroll-mt-24 min-w-0 rounded-lg border border-slate-200 bg-white shadow-sm xl:col-span-2 2xl:col-span-1"
            >
              <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
                <div>
                  <p className="text-sm font-semibold text-slate-950">流量审计</p>
                  <p className="mt-0.5 text-xs text-slate-500">JSON-RPC 请求与响应</p>
                </div>
                <Badge variant="outline">{traffic.length} 条</Badge>
              </div>
              <div className="h-[560px] p-4 2xl:h-[680px]">
                <TrafficInspector />
              </div>
            </div>
          </section>

          <section
            id="config-versions"
            className="scroll-mt-24 grid gap-4 lg:grid-cols-3"
          >
            <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
              <p className="text-sm font-semibold text-slate-900">租户隔离</p>
              <p className="mt-2 text-sm text-slate-500">演示环境 · 基础权限</p>
            </div>
            <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
              <p className="text-sm font-semibold text-slate-900">接入通道</p>
              <p className="mt-2 text-sm text-slate-500">
                {selectedServer?.transport.toUpperCase() ?? "SSE"} 协议已配置
              </p>
            </div>
            <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
              <p className="text-sm font-semibold text-slate-900">发布状态</p>
              <p className="mt-2 text-sm text-slate-500">等待配置发布</p>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}
