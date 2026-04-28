# MCP-Gateway

## Frontend (Next.js 15)

The MCP Gateway frontend lives in `mcp-gateway-frontend` and is independent from the Maven modules.

```bash
cd mcp-gateway-frontend
npm install
npm run dev
```

### Useful scripts

```bash
npm run lint
npm run test
npm run build
```

### Optional environment variables

```bash
# default MCP server connection
NEXT_PUBLIC_MCP_ENDPOINT=http://localhost:3000/mcp
NEXT_PUBLIC_MCP_NAME=Local MCP Server
NEXT_PUBLIC_MCP_TRANSPORT=sse

# override UI version string
NEXT_PUBLIC_APP_VERSION=0.1.0
```
