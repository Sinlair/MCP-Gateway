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

## Gateway execution safeguards

The backend uses configurable tool-execution safeguards with a mock fallback mode:

```yaml
mcp:
  gateway:
    tool-execution:
      mode: mock
      timeout-ms: 3000
      max-retries: 1
```

- `mode`: currently `mock` is enabled as the safe fallback.
- `timeout-ms`: per-attempt execution timeout.
- `max-retries`: retry count after a failed attempt.

## CI quality gates

Repository CI now runs:
- `mvn test` for backend modules.
- `npm run lint`, `npm run test`, `npm run build` for `mcp-gateway-frontend`.
