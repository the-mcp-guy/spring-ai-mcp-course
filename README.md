# Support Desk: Spring AI + MCP course

> [!NOTE]
> **This branch is Class 7.** It implements
> [Class 7: Handing the Tools to a Model](https://themcpguy.com/docs/mcp-spring-ai/tools-to-a-model),
> which gives the Class 6 client a model: a ChatClient that hands the discovered MCP
> tools to Claude or a local Ollama model, a console chat behind the `cli` profile, and
> a small web API for the frontend. Classes 2 to 5 built the server side:
> [tools](https://themcpguy.com/docs/mcp-spring-ai/rest-app-to-mcp-server),
> [more tools](https://themcpguy.com/docs/mcp-spring-ai/tools-in-depth),
> [resources](https://themcpguy.com/docs/mcp-spring-ai/resources) and
> [prompts](https://themcpguy.com/docs/mcp-spring-ai/prompts-and-completion), and
> [Class 6](https://themcpguy.com/docs/mcp-spring-ai/connecting-a-client) connected the
> client. `main` stays at the course starting point, with no AI or MCP code at all, so
> clone that branch to follow along from Class 1.

Companion repository for the [Spring AI + MCP course](https://themcpguy.com/docs/mcp-spring-ai/why-spring-ai)
on themcpguy.com.

## What you get

`order-service` is an ordinary Spring Boot application: JPA entities, Spring Data
repositories, a business service, a REST controller, and seed data for 200 orders across
42 customers. None of it is taught in the course. It is here so the classes can spend their
time on MCP rather than on Spring Boot.

```
order-service/          the application. MCP is added to it from Class 2.
support-agent/          the agent: an MCP client since Class 6, with a model since Class 7.
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
- An API key from Anthropic, **or** [Ollama](https://ollama.com) running locally. Needed
  from this class on: the agent hands the order tools to a model.

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

From Class 7 the agent has a model. Set the API key in the terminal that will run it
(skip this if you use Ollama):

```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

Leave the order service running, then start the console chat:

```bash
mvn -pl support-agent spring-boot:run -Dspring-boot.run.profiles=cli
```

On startup it still prints what the server offers, the Class 6 listing:

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

then waits for input. Ask about an order; type `quit` to leave. Every fact in an answer
comes from the four tools, and the DEBUG lines in between show each tool call as it
happens. The order service must be up first, on the port named in
`support-agent/src/main/resources/application.yaml`. Note that `order://{orderId}` does
not appear in the listing: it is a template, and templates are not returned by
`resources/list`.

Started without the `cli` profile,

```bash
mvn -pl support-agent spring-boot:run
```

it serves the same agent to the frontend as `POST /api/chat` on port 8081 instead of
reading the terminal.

To run on Ollama instead of Anthropic, pull the model once with `ollama pull qwen3:8b`,
then pick the provider on the command line. No API key is needed:

```bash
mvn -pl support-agent spring-boot:run \
  -Dspring-boot.run.profiles=cli \
  -Dspring-boot.run.arguments=--spring.ai.model.chat=ollama
```

Class 8 reads the server's resources and prompts into the conversation.

## Running the frontend

```bash
cd frontend
npm install     # first time only
npm run dev
```

Open `http://localhost:5173`. The order list works immediately, and with the agent
running in web mode the chat panel talks to it on port 8081. The progress bar and
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

If 8081 is taken, change `server.port` in
`support-agent/src/main/resources/application.yaml` and point the three agent entries of
the same proxy (`/api/chat`, `/api/events`, `/api/confirmations`) at the new port.

If 5173 is taken, change `server.port` in `frontend/vite.config.js`, or pass the port on
the command line for a single run:

```bash
npm run dev -- --port 5174
```

Nothing in the backend refers to the frontend port, so this needs no other change. Open
the new port instead of 5173.
