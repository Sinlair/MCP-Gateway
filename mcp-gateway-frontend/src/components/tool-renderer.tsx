"use client";

import * as React from "react";
import { Play, TerminalSquare, Wrench } from "lucide-react";

import type { McpTool } from "@/types";
import { DynamicForm } from "@/components/dynamic-form";
import { ScrollArea } from "@/components/ui/scroll-area";

interface ToolRendererProps {
  tool?: McpTool;
  onExecute: (args: unknown) => Promise<void> | void;
  result?: unknown;
}

export function ToolRenderer({ tool, onExecute, result }: ToolRendererProps) {
  if (!tool) {
    return (
      <section className="flex min-h-[420px] items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
        <div>
          <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-lg bg-slate-100 text-slate-500">
            <Wrench className="h-5 w-5" />
          </div>
          <p className="mt-3 text-sm font-semibold text-slate-800">
            选择一个工具开始调试
          </p>
          <p className="mt-1 text-xs text-slate-500">
            初始化服务后，可在左侧工具目录选择并执行能力。
          </p>
        </div>
      </section>
    );
  }

  return (
    <div className="space-y-4">
      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex items-start justify-between gap-3 border-b border-slate-200 px-4 py-3">
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-slate-950">
              {tool.name}
            </p>
            <p className="mt-0.5 text-xs text-slate-500">工具参数与执行</p>
          </div>
          <div className="flex h-8 w-8 items-center justify-center rounded-md bg-emerald-50 text-emerald-700">
            <Play className="h-4 w-4" />
          </div>
        </div>
        <div className="p-4">
          {tool.description ? (
            <p className="mb-4 rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm leading-6 text-slate-600">
              {tool.description}
            </p>
          ) : null}
          <DynamicForm
            schema={tool.inputSchema}
            submitLabel="执行工具"
            onSubmit={onExecute}
          />
        </div>
      </section>
      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <div>
            <p className="text-sm font-semibold text-slate-950">执行结果</p>
            <p className="mt-0.5 text-xs text-slate-500">服务返回的原始响应</p>
          </div>
          <TerminalSquare className="h-4 w-4 text-slate-400" />
        </div>
        <div className="p-4">
          <ScrollArea className="max-h-72 rounded-lg bg-slate-950">
            <pre className="min-h-28 overflow-auto p-4 text-xs leading-6 text-slate-200">
              {result ? JSON.stringify(result, null, 2) : "暂无执行结果"}
            </pre>
          </ScrollArea>
        </div>
      </section>
    </div>
  );
}
