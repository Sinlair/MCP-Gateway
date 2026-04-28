"use client";

import * as React from "react";

import type { McpTool } from "@/types";
import { DynamicForm } from "@/components/dynamic-form";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";

interface ToolRendererProps {
  tool?: McpTool;
  onExecute: (args: unknown) => Promise<void> | void;
  result?: unknown;
}

export function ToolRenderer({ tool, onExecute, result }: ToolRendererProps) {
  if (!tool) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Select a tool to begin</CardTitle>
        </CardHeader>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>{tool.name}</CardTitle>
        </CardHeader>
        <CardContent>
          {tool.description ? (
            <p className="mb-4 text-sm text-muted-foreground">
              {tool.description}
            </p>
          ) : null}
          <DynamicForm schema={tool.inputSchema} onSubmit={onExecute} />
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>Execution Result</CardTitle>
        </CardHeader>
        <CardContent>
          <ScrollArea className="max-h-72">
            <pre className="text-xs text-foreground/90">
              {result ? JSON.stringify(result, null, 2) : "No results yet."}
            </pre>
          </ScrollArea>
        </CardContent>
      </Card>
    </div>
  );
}
