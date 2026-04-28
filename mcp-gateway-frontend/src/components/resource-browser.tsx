"use client";

import * as React from "react";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";

import type { McpResource } from "@/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
                  "w-full justify-start",
                  isSelected && "bg-secondary"
                )}
                style={{ paddingLeft: `${level * 12 + 8}px` }}
                onClick={() => node.resource && onSelect(node.resource)}
              >
                {node.name}
              </Button>
            ) : (
              <div
                className="px-2 text-xs font-semibold text-muted-foreground"
                style={{ paddingLeft: `${level * 12 + 8}px` }}
              >
                {node.name}
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
      <Card className="h-full">
        <CardHeader>
          <CardTitle>Resources</CardTitle>
        </CardHeader>
        <CardContent>
          <ScrollArea className="h-[420px]">
            {resources.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                No resources advertised.
              </p>
            ) : (
              <ResourceTree
                nodes={tree}
                selectedUri={selectedUri}
                onSelect={onRead}
              />
            )}
          </ScrollArea>
        </CardContent>
      </Card>
      <Card className="h-full">
        <CardHeader>
          <CardTitle>Content Preview</CardTitle>
        </CardHeader>
        <CardContent>
          {content ? (
            <ScrollArea className="h-[420px]">
              <SyntaxHighlighter
                language={mimeType?.split("/")[1] ?? "json"}
                style={vscDarkPlus}
                customStyle={{
                  background: "transparent",
                  margin: 0,
                  fontSize: "0.75rem",
                }}
              >
                {content}
              </SyntaxHighlighter>
            </ScrollArea>
          ) : (
            <p className="text-sm text-muted-foreground">
              Select a resource to read its content.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
