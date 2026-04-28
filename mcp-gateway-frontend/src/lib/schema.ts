import { z } from "zod";

import type { JsonSchema } from "@/types";

export function schemaToZod(schema: JsonSchema): z.ZodTypeAny {
  if (schema.enum && schema.enum.length > 0) {
    return z.enum([...schema.enum] as [string, ...string[]]);
  }

  const resolvedType =
    schema.type ?? (schema.properties ? "object" : schema.items ? "array" : undefined);

  switch (resolvedType) {
    case "string":
      return z.string();
    case "number":
      return z.number();
    case "integer":
      return z.number().int();
    case "boolean":
      return z.boolean();
    case "array": {
      const itemSchema = schema.items ? schemaToZod(schema.items) : z.any();
      return z.array(itemSchema);
    }
    case "object": {
      const shape: Record<string, z.ZodTypeAny> = {};
      const required = new Set(schema.required ?? []);
      Object.entries(schema.properties ?? {}).forEach(([key, value]) => {
        const zodSchema = schemaToZod(value);
        shape[key] = required.has(key) ? zodSchema : zodSchema.optional();
      });
      return z.object(shape);
    }
    default:
      return z.any();
  }
}

export function buildDefaultValues(schema: JsonSchema): unknown {
  if (schema.default !== undefined) {
    return schema.default;
  }

  if (schema.enum && schema.enum.length > 0) {
    return schema.enum[0];
  }

  const resolvedType =
    schema.type ?? (schema.properties ? "object" : schema.items ? "array" : undefined);

  switch (resolvedType) {
    case "string":
      return "";
    case "number":
    case "integer":
      return 0;
    case "boolean":
      return false;
    case "array":
      return [];
    case "object": {
      const result: Record<string, unknown> = {};
      Object.entries(schema.properties ?? {}).forEach(([key, value]) => {
        result[key] = buildDefaultValues(value);
      });
      return result;
    }
    default:
      return null;
  }
}
