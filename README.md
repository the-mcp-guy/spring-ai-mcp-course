# Support Desk: Spring AI + MCP course

> [!NOTE]
> **This branch is Class 6.** It implements
> [Class 6: Connecting a Client](https://themcpguy.com/docs/mcp-spring-ai/connecting-a-client),
> which adds a second application, `support-agent`, that connects to the order service as
> an MCP client. Classes 2 to 5 built the server side:
> [tools](https://themcpguy.com/docs/mcp-spring-ai/rest-app-to-mcp-server),
> [more tools](https://themcpguy.com/docs/mcp-spring-ai/tools-in-depth),
> [resources](https://themcpguy.com/docs/mcp-spring-ai/resources) and
> [prompts](https://themcpguy.com/docs/mcp-spring-ai/prompts-and-completion). There is
> still no model involved. `main` stays at the course starting point, with no AI or MCP
> code at all, so clone that branch to follow along from Class 1.

Companion repository for the [Spring AI + MCP course](https://themcpguy.com/docs/mcp-spring-ai/why-spring-ai)
on themcpguy.com.

## What you get

`order-service` is an ordinary Spring Boot application: JPA entities, Spring Data
repositories, a business service, a REST controller, and seed data for 200 orders across
42 customers. None of it is taught in the course. It is here so the classes can spend their
time on MCP rather than on Spring Boot.

```
order-service/          the application. MCP is added to it from Class 2.
support-agent/          the MCP client, added in Class 6. Gets a model in Class 7.
frontend/               React + Vite. Never taught, never changed.
support-kb/             the support team's notes, served over MCP from Class 9
support-kb-archive/     the pre-2024 versions of the same notes, added in Class 10
```

The product policies live on the classpath at
`order-service/src/main/resources/policies/`, and Class 4 exposes them as MCP resources.
They are ordinary Markdown, so editing one changes what the server serves on the next
restart.

## Prerequisites

- JDK 21 or later
- Maven 3.9 or later
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

From Class 2 this same application also publishes an MCP endpoint on `/mcp`. On this branch
it registers four tools, all defined in
`order-service/src/main/java/com/themcpguy/supportdesk/orders/mcp/OrderTools.java`:

| Tool                   | What it does                       | Read only |
|------------------------|------------------------------------|-----------|
| `get_order`            | One order by ID                    | yes       |
| `get_customer_orders`  | Every order for one customer       | yes       |
| `get_orders_by_status` | Every order in one status          | yes       |
| `update_order_status`  | Move an order to a new status      | no        |

From Class 4 it also publishes resources, defined in `PolicyResources.java` and
`OrderResources.java` in the same package:

| Resource             | Kind          | Serves                          |
|----------------------|---------------|---------------------------------|
| `policy://returns`   | fixed         | The returns policy, as Markdown |
| `policy://shipping`  | fixed         | The shipping policy, as Markdown|
| `order://{orderId}`  | URI template  | One order, by ID                |

From Class 5 it publishes one prompt as well, in `RefundPrompts.java`:

| Prompt               | Arguments           | Completion                                  |
|----------------------|---------------------|---------------------------------------------|
| `draft_refund_email` | `orderId`, `reason` | Five fixed reasons, order IDs from the data |

The startup log confirms all of it:

```
Registered tools: 4
Registered resources: 2
Registered prompts: 1
Registered completions: 1
```

The resource template is counted separately from the two fixed resources, so it is listed
by `resources/templates/list` rather than `resources/list`.

## Running the agent

From Class 6 there is a second application. Leave the order service running, then in
another terminal:

```bash
mvn -pl support-agent spring-boot:run
```

It is not a web application. It connects to the order service over MCP, prints what the
server offers, calls one tool and exits:

```
Connected to order-service 1.0.0
  tool     get_customer_orders
  tool     get_order
  tool     get_orders_by_status
  tool     update_order_status
  resource policy://shipping
  resource policy://returns
  prompt   draft_refund_email
```

The order service must be up first, on the port named in
`support-agent/src/main/resources/application.yaml`. Note that `order://{orderId}` does not
appear: it is a template, and templates are not returned by `resources/list`.

There is still no model in the picture. The agent gets one in Class 7.

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

| Order       | Status    | Customer                   | Total   |
|-------------|-----------|----------------------------|---------|
| `ORD-10001` | SHIPPED   | Ana Ruiz (`CUST-42`)       | €179.99 |
| `ORD-10002` | PENDING   | Marcus Adeyemi (`CUST-17`) | €34.99  |
| `ORD-10003` | DELIVERED | Ana Ruiz (`CUST-42`)       | €599.00 |

The other 197 are generated from the index rather than randomly, so every run produces the
same data. The spread is 87 shipped, 50 delivered, 30 pending, 25 processing and
8 cancelled.

The database is H2 in memory and is seeded at startup, so restarting throws away anything
the course changed. That is deliberate: nothing you do while following along is permanent.

## Changing the port

If 8080 is taken, change it in `order-service/src/main/resources/application.yaml` and
point the frontend proxy at the same port in `frontend/vite.config.js`.

If 5173 is taken, change `server.port` in `frontend/vite.config.js`, or pass the port on
the command line for a single run:

```bash
npm run dev -- --port 5174
```

Nothing in the backend refers to the frontend port, so this needs no other change. Open
the new port instead of 5173.
