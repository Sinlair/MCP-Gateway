# Repository Guidelines

## Project Structure & Module Organization
This repository is intended for a Java-based MCP Gateway using DDD and a multi-module Maven layout. Keep modules split by responsibility: `mcp-gateway-app` for Spring Boot startup and configuration, `mcp-gateway-domain` for aggregates, value objects, domain services, and repository ports, `mcp-gateway-infrastructure` for database, cache, and external MCP/provider adapters, `mcp-gateway-trigger` for REST/SSE/WebSocket entry points, and `mcp-gateway-types` for shared enums, exceptions, and response models. Place code in `src/main/java`, tests in `src/test/java`, and configs in `src/main/resources`.

## Build, Test, and Development Commands
Use Maven from the repository root.

- `mvn clean install` builds all modules and runs tests.
- `mvn test` runs the full test suite.
- `mvn -pl mcp-gateway-app spring-boot:run` starts the gateway locally.
- `mvn -pl mcp-gateway-app -DskipTests package` builds a runnable package quickly.

## Coding Style & Naming Conventions
Use Java 17+, UTF-8, and 4-space indentation. Package names stay lowercase, class names use `UpperCamelCase`, methods and fields use `lowerCamelCase`, and constants use `UPPER_SNAKE_CASE`. Prefer constructor injection and immutable value objects. Keep controllers, adapters, and persistence objects thin; domain rules belong in aggregates and domain services. If formatting tools are added, use Spotless for formatting and Checkstyle for static style checks.

## Testing Guidelines
Use JUnit 5 for unit tests, Mockito for mocks, and Testcontainers for integration tests that touch MySQL, Redis, or message brokers. Name unit tests `*Test` and integration tests `*IT`. Aim for at least 80% coverage in domain and application service code, with priority on routing, session lifecycle, authorization, and tool invocation flows.

## Design Principles
Model the gateway around bounded contexts such as `session`, `routing`, `tool-execution`, and `auth`. Let aggregates enforce invariants and expose intent-rich behavior instead of anemic getters/setters. Infrastructure implements ports; it must not own business decisions. Translate MCP protocol details at the edge, then hand clean commands into the domain layer.

## Commit & Pull Request Guidelines
The repository does not yet have a usable commit history, so start with Conventional Commits such as `feat: add routing policy` or `fix: prevent duplicate session registration`. Each pull request should include scope, affected modules, test evidence, configuration changes, and sample request/response data when gateway behavior changes.

## Security & Configuration Tips
Store secrets in environment variables or `application-local.yml`; do not commit provider tokens, database passwords, or private endpoints. Keep `dev`, `test`, and `prod` profiles separate, and document all required keys when introducing new integrations.
