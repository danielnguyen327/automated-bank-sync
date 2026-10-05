# LedgerSync

Links bank accounts and credit cards through Plaid, keeps transactions in sync on a schedule,
and uses Claude to categorize spending and explain what changed.

- `backend/`: Java 25 + Spring Boot, PostgreSQL (Flyway), Plaid, Claude
- `frontend/`: Next.js + TypeScript, which forwards `/api/*` to the backend

## Run it locally (needs Java 25, Node 24 and Docker)

```bash
cd backend && ./mvnw spring-boot:run   # starts Postgres automatically; API on :8080
cd frontend && npm install && npm run dev   # in a second terminal; web on :3000
```

## Checks

`cd backend && ./mvnw verify` · `cd frontend && npm run lint && npm run typecheck && npm test && npm run build`