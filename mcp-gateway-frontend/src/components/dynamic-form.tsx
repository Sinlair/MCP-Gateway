"use client";

import * as React from "react";
import { FormProvider, useFieldArray, useForm, useFormContext } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import type { JsonSchema } from "@/types";
import { buildDefaultValues, schemaToZod } from "@/lib/schema";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";

interface DynamicFormProps {
  schema: JsonSchema;
  submitLabel?: string;
  onSubmit: (values: unknown) => void | Promise<void>;
  disabled?: boolean;
}

function FieldLabel({
  label,
  description,
}: {
  label: string;
  description?: string;
}) {
  return (
    <div className="space-y-1">
      <p className="text-sm font-medium text-foreground">{label}</p>
      {description ? (
        <p className="text-xs text-muted-foreground">{description}</p>
      ) : null}
    </div>
  );
}

function PrimitiveField({
  name,
  schema,
}: {
  name: string;
  schema: JsonSchema;
}) {
  const { register, setValue, watch } = useFormContext();

  if (schema.enum && schema.enum.length > 0) {
    return (
      <Select
        onValueChange={(value) => setValue(name, value)}
        defaultValue={(watch(name) as string) ?? schema.enum[0]}
      >
        <SelectTrigger>
          <SelectValue placeholder="Select value" />
        </SelectTrigger>
        <SelectContent>
          {schema.enum.map((option) => (
            <SelectItem key={option} value={option}>
              {option}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    );
  }

  switch (schema.type) {
    case "number":
    case "integer":
      return <Input type="number" {...register(name, { valueAsNumber: true })} />;
    case "boolean":
      return (
        <Switch
          checked={Boolean(watch(name))}
          onCheckedChange={(value) => setValue(name, value)}
        />
      );
    case "string":
      if (schema.description && schema.description.length > 80) {
        return <Textarea rows={3} {...register(name)} />;
      }
      return <Input {...register(name)} />;
    default:
      return <Input {...register(name)} />;
  }
}

function ArrayField({
  name,
  schema,
}: {
  name: string;
  schema: JsonSchema;
}) {
  const { control } = useFormContext();
  const itemsSchema = schema.items ?? { type: "string" };
  const { fields, append, remove } = useFieldArray({ control, name });

  return (
    <div className="space-y-3 rounded-lg border border-border/60 p-3">
      {fields.length === 0 ? (
        <p className="text-xs text-muted-foreground">No items yet.</p>
      ) : null}
      {fields.map((field, index) => (
        <div
          key={field.id}
          className="flex items-start gap-3 rounded-md border border-border/60 p-3"
        >
          <div className="flex-1 space-y-2">
            <FieldRenderer
              name={`${name}.${index}`}
              schema={itemsSchema}
              label={`Item ${index + 1}`}
            />
          </div>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => remove(index)}
          >
            Remove
          </Button>
        </div>
      ))}
      <Button
        type="button"
        variant="secondary"
        size="sm"
        onClick={() => append(buildDefaultValues(itemsSchema))}
      >
        Add item
      </Button>
    </div>
  );
}

function ObjectField({
  name,
  schema,
}: {
  name: string;
  schema: JsonSchema;
}) {
  const properties = schema.properties ?? {};
  return (
    <div className="space-y-4 rounded-lg border border-border/60 p-4">
      {Object.entries(properties).map(([key, value]) => (
        <FieldRenderer
          key={`${name}.${key}`}
          name={`${name}.${key}`}
          schema={value}
          label={value.title ?? key}
        />
      ))}
    </div>
  );
}

function FieldRenderer({
  name,
  schema,
  label,
}: {
  name: string;
  schema: JsonSchema;
  label: string;
}) {
  if (schema.type === "object") {
    return (
      <div className="space-y-2">
        <FieldLabel label={label} description={schema.description} />
        <ObjectField name={name} schema={schema} />
      </div>
    );
  }

  if (schema.type === "array") {
    return (
      <div className="space-y-2">
        <FieldLabel label={label} description={schema.description} />
        <ArrayField name={name} schema={schema} />
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <FieldLabel label={label} description={schema.description} />
      <PrimitiveField name={name} schema={schema} />
    </div>
  );
}

export function DynamicForm({
  schema,
  submitLabel = "Run Tool",
  onSubmit,
  disabled = false,
}: DynamicFormProps) {
  const form = useForm({
    resolver: zodResolver(z.object({ root: schemaToZod(schema) })),
    defaultValues: { root: buildDefaultValues(schema) },
    mode: "onChange",
  });

  return (
    <FormProvider {...form}>
      <form
        className="space-y-6"
        onSubmit={form.handleSubmit((values) => onSubmit(values.root))}
      >
        <FieldRenderer
          name="root"
          schema={{
            ...schema,
            type: schema.type ?? "object",
            title: schema.title ?? "Inputs",
          }}
          label={schema.title ?? "Inputs"}
        />
        <div className="flex items-center gap-2">
          <Button type="submit" disabled={!form.formState.isValid || disabled}>
            {submitLabel}
          </Button>
          <Button
            type="button"
            variant="secondary"
            onClick={() => form.reset()}
          >
            Reset
          </Button>
          {form.formState.isSubmitting ? (
            <span className={cn("text-xs text-muted-foreground")}>Running…</span>
          ) : null}
        </div>
      </form>
    </FormProvider>
  );
}
