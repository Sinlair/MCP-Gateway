import { describe, expect, it } from "vitest";

import { buildDefaultValues, schemaToZod } from "@/lib/schema";
import type { JsonSchema } from "@/types";

describe("schema helpers", () => {
  it("builds default values for nested objects", () => {
    const schema: JsonSchema = {
      type: "object",
      properties: {
        name: { type: "string" },
        count: { type: "integer" },
        enabled: { type: "boolean" },
      },
    };

    expect(buildDefaultValues(schema)).toEqual({
      name: "",
      count: 0,
      enabled: false,
    });
  });

  it("respects enum defaults", () => {
    const schema: JsonSchema = {
      type: "string",
      enum: ["alpha", "beta"],
    };

    expect(buildDefaultValues(schema)).toBe("alpha");
  });

  it("validates required properties", () => {
    const schema: JsonSchema = {
      type: "object",
      required: ["name"],
      properties: {
        name: { type: "string" },
        optional: { type: "string" },
      },
    };

    const zodSchema = schemaToZod(schema);
    expect(() => zodSchema.parse({ name: "ok" })).not.toThrow();
    expect(() => zodSchema.parse({})).toThrow();
  });
});
