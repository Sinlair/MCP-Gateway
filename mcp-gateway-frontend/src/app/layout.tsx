import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "MCP Gateway Frontend",
  description: "Developer console for MCP Gateway servers",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full antialiased dark">
      <body className="min-h-full bg-background text-foreground">{children}</body>
    </html>
  );
}
