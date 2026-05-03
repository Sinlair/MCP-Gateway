"use client";

import * as React from "react";
import { FileCode2, Folder, PanelsTopLeft } from "lucide-react";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";

import type { McpResource } from "@/types";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";

type ResourceNode = {
  name: string;
  path: string;
  resource?: McpResource;
  children: ResourceNode[];
};

type ResourceNodeInternal = {
  name: string;
  path: string;
  resource?: McpResource;
  children: Record<string, ResourceNodeInternal>;
};

const mimeLanguageMap: Record<string, string> = {
  "application/json": "json",
  "text/json": "json",
  "application/javascript": "javascript",
  "text/javascript": "javascript",
  "application/typescript": "typescript",
  "application/x-typescript": "typescript",
  "text/typescript": "typescript",
  "text/plain": "text",
  "text/markdown": "markdown",
  "text/html": "html",
  "text/css": "css",
  "application/xml": "xml",
  "text/xml": "xml",
  "application/x-yaml": "yaml",
  "text/yaml": "yaml",
};

function resolveLanguage(mimeType?: string) {
  if (!mimeType) {
    return "text";
  }
  return mimeLanguageMap[mimeType] ?? mimeType.split("/")[1] ?? "text";
}

function buildResourceTree(resources: McpResource[]): ResourceNode[] {
  const root: Record<string, ResourceNodeInternal> = {};

  resources.forEach((resource) => {
    const normalized = resource.uri.replace("://", "/");
    const segments = normalized.split("/").filter(Boolean);
    let current = root;
    let currentPath = "";

    segments.forEach((segment, index) => {
      currentPath = `${currentPath}/${segment}`;
      if (!current[segment]) {
        current[segment] = {
          name: segment,
          path: currentPath,
          children: {},
        };
      }
      if (index === segments.length - 1) {
        current[segment].resource = resource;
      }
      current = current[segment].children;
    });
  });

  const buildChildren = (
    nodeMap: Record<string, ResourceNodeInternal>
  ): ResourceNode[] =>
    Object.values(nodeMap).map((node) => ({
      name: node.name,
      path: node.path,
      resource: node.resource,
      children: buildChildren(node.children),
    }));

  return buildChildren(root);
}

function ResourceTree({
  nodes,
  level = 0,
  selectedUri,
  onSelect,
}: {
  nodes: ResourceNode[];
  level?: number;
  selectedUri?: string;
  onSelect: (resource: McpResource) => void;
}) {
  return (
    <div className="space-y-1">
      {nodes.map((node) => {
        const isSelected = node.resource?.uri === selectedUri;
        return (
          <div key={node.path} className="space-y-1">
            {node.resource ? (
              <Button
                variant="ghost"
                size="sm"
                className={cn(
                  "h-auto w-full justify-start gap-2 whitespace-normal rounded-md px-2 py-2 text-left text-slate-600 hover:bg-slate-100 hover:text-slate-950",
                  isSelected && "bg-blue-50 text-blue-700 hover:bg-blue-50"
                )}
                style={{ paddingLeft: `${level * 12 + 8}px` }}
                onClick={() => node.resource && onSelect(node.resource)}
              >
                <FileCode2 className="h-4 w-4 shrink-0" />
                <span className="min-w-0 truncate">{node.name}</span>
              </Button>
            ) : (
              <div
                className="flex items-center gap-2 px-2 py-1 text-xs font-semibold text-slate-500"
                style={{ paddingLeft: `${level * 12 + 8}px` }}
              >
                <Folder className="h-3.5 w-3.5" />
                <span className="truncate">{node.name}</span>
              </div>
            )}
            {node.children.length > 0 ? (
              <ResourceTree
                nodes={node.children}
                level={level + 1}
                selectedUri={selectedUri}
                onSelect={onSelect}
              />
            ) : null}
          </div>
        );
      })}
    </div>
  );
}

interface ResourceBrowserProps {
  resources: McpResource[];
  selectedUri?: string;
  content?: string;
  mimeType?: string;
  onRead: (resource: McpResource) => void;
}

export function ResourceBrowser({
  resources,
  selectedUri,
  content,
  mimeType,
  onRead,
}: ResourceBrowserProps) {
  const tree = React.useMemo(() => buildResourceTree(resources), [resources]);

  return (
    <div className="grid gap-4 lg:grid-cols-[280px_1fr]">
      <section className="h-full rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <div>
            <p className="text-sm font-semibold text-slate-950">资源目录</p>
            <p className="mt-0.5 text-xs text-slate-500">{resources.length} 个资源</p>
          </div>
          <PanelsTopLeft className="h-4 w-4 text-slate-400" />
        </div>
        <div className="p-3">
          <ScrollArea className="h-[470px]">
            {resources.length === 0 ? (
              <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-500">
                暂无已暴露资源
              </div>
            ) : (
              <ResourceTree
                nodes={tree}
                selectedUri={selectedUri}
                onSelect={onRead}
              />
            )}
          </ScrollArea>
        </div>
      </section>
      <section className="h-full rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <div>
            <p className="text-sm font-semibold text-slate-950">内容预览</p>
            <p className="mt-0.5 text-xs text-slate-500">
              {mimeType ?? "选择资源后展示内容"}
            </p>
          </div>
          <FileCode2 className="h-4 w-4 text-slate-400" />
        </div>
        <div className="p-4">
          {content ? (
            <ScrollArea className="h-[470px] rounded-lg bg-slate-950">
              <SyntaxHighlighter
                language={resolveLanguage(mimeType)}
                style={vscDarkPlus}
                customStyle={{
                  background: "transparent",
                  margin: 0,
                  fontSize: "0.75rem",
                  padding: "1rem",
                }}
              >
                {content}
              </SyntaxHighlighter>
            </ScrollArea>
          ) : (
            <div className="flex h-[470px] items-center justify-center rounded-lg border border-dashed border-slate-300 bg-slate-50 p-6 text-center">
              <div>
                <p className="text-sm font-medium text-slate-700">
                  选择资源查看内容
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  支持 JSON、文本、Markdown、HTML 等常见格式预览。
                </p>
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
