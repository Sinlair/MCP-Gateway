FROM node:20-alpine AS deps

WORKDIR /app
COPY package*.json ./
RUN npm ci

FROM node:20-alpine AS builder

WORKDIR /app
ARG NEXT_PUBLIC_MCP_ENDPOINT="/mcp"
ARG NEXT_PUBLIC_MCP_NAME="MCP Gateway"
ARG NEXT_PUBLIC_MCP_TRANSPORT="sse"
ARG NEXT_PUBLIC_APP_VERSION="0.1.0"
ENV NEXT_PUBLIC_MCP_ENDPOINT=$NEXT_PUBLIC_MCP_ENDPOINT
ENV NEXT_PUBLIC_MCP_NAME=$NEXT_PUBLIC_MCP_NAME
ENV NEXT_PUBLIC_MCP_TRANSPORT=$NEXT_PUBLIC_MCP_TRANSPORT
ENV NEXT_PUBLIC_APP_VERSION=$NEXT_PUBLIC_APP_VERSION
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:20-alpine AS runner

WORKDIR /app
ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000

COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules

EXPOSE 3000

CMD ["npm", "run", "start"]
