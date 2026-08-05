# Support Desk: Spring AI + MCP course

> [!IMPORTANT]
> **This is the course starting point, not a finished project.** It contains a working
> Spring Boot order service and a React frontend, and **no AI or MCP code at all**. We add
> that together across the course. `main` is the default branch so that cloning it puts you
> exactly where Class 1 begins.

Companion repository for the [Spring AI + MCP course](https://themcpguy.com/docs/mcp-spring-ai/why-spring-ai)
on themcpguy.com.

## What you get

`order-service` is an ordinary Spring Boot application: JPA entities, Spring Data
repositories, a business service, a REST controller, and seed data for 200 orders across
42 customers. None of it is taught in the course. It is here so the classes can spend their
time on MCP rather than on Spring Boot.

```
order-service/          the application. MCP is added to it from Class 2.
frontend/               React + Vite. Never taught, never changed.
support-kb/             the support team's notes, served over MCP from Class 9
support-kb-archive/     the pre-2024 versions of the same notes, added in Class 10
```

The product policies live on the classpath at
`order-service/src/main/resources/policies/`, and Class 4 exposes them as MCP resources.

## Prerequisites

- JDK 21 or later
- Maven 3.9 or later (or use the wrapper)
- Node.js 20 or later, for the frontend and, from Class 9, for one MCP server on npm
- An API key from Anthropic or OpenAI, **or** [Ollama](https://ollama.com) running locally.
  Not needed until Class 7: Classes 1 to 6 need no model at all.

## Running the backend

```bash
mvn -pl order-service spring-boot:run
```

- REST API: `http://localhost:8080/api/orders/ORD-10001`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:orders`, user `sa`,
  empty password)

From Class 2 this same application also publishes an MCP endpoint on `/mcp`.

## Running the frontend

```bash
cd frontend
npm install     # first time only
npm run dev
```

Open `http://localhost:5173`. The order list works immediately. The chat panel reports
that the agent is not running until Class 7 builds it, and the progress bar and
confirmation dialog come alive in Classes 11 and 12.

## Running the tests

```bash
mvn test
```

Ten tests covering the seed data and the business logic. They exist so that a change here
is caught before it reaches a lesson that quotes the numbers.

## The data

Three orders are fixed because the course quotes them:

| Order | Status | Customer | Total |
| --- | --- | --- | --- |
| `ORD-10001` | SHIPPED | Ana Ruiz (`CUST-42`) | 179.99 |
| `ORD-10002` | PENDING | Marcus Adeyemi (`CUST-17`) | 34.99 |
| `ORD-10003` | DELIVERED | Ana Ruiz (`CUST-42`) | 599.00 |

The other 197 are generated from the index rather than randomly, so every run produces the
same data. The spread is 87 shipped, 50 delivered, 30 pending, 25 processing and
8 cancelled.

The database is H2 in memory and is seeded at startup, so restarting throws away anything
the course changed. That is deliberate: nothing you do while following along is permanent.

## Changing the port

If 8080 is taken, change it in `order-service/src/main/resources/application.yaml` and
point the frontend proxy at the same port in `frontend/vite.config.js`.
