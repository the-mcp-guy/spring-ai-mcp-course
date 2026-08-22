# Support Desk: Spring AI + MCP course

> [!NOTE]
> **This branch is Class 12.** It implements
> [Class 12: Elicitation](https://themcpguy.com/docs/mcp-spring-ai/elicitation),
> which lets a tool ask the person for confirmation before it acts: a new
> `cancel_order` tool on the order service pauses mid-call with `context.elicit(...)`
> and waits for a yes or a no. The agent answers with `@McpElicitation` handlers:
> the console chat asks in the terminal, and web mode shows a dialog in the browser,
> declining on its own when nobody answers. `update_order_status` refuses the
> CANCELLED status from this class on, so the confirming tool is the only way to
> cancel an order. Classes 2 to 5 built the server side:
> [tools](https://themcpguy.com/docs/mcp-spring-ai/rest-app-to-mcp-server),
> [more tools](https://themcpguy.com/docs/mcp-spring-ai/tools-in-depth),
> [resources](https://themcpguy.com/docs/mcp-spring-ai/resources) and
> [prompts](https://themcpguy.com/docs/mcp-spring-ai/prompts-and-completion),
> [Class 6](https://themcpguy.com/docs/mcp-spring-ai/connecting-a-client) connected the
> client, [Class 7](https://themcpguy.com/docs/mcp-spring-ai/tools-to-a-model) gave it
> a model,
> [Class 8](https://themcpguy.com/docs/mcp-spring-ai/consuming-resources-and-prompts/)
> put the server's resources and prompts to work on the client side,
> [Class 9](https://themcpguy.com/docs/mcp-spring-ai/a-server-we-did-not-write/)
> connected the first filesystem server over `support-kb/`,
> [Class 10](https://themcpguy.com/docs/mcp-spring-ai/several-servers/) added the
> archive server and tamed the resulting tool list, and
> [Class 11](https://themcpguy.com/docs/mcp-spring-ai/progress-and-logging/) made a
> long-running tool report progress and logs while it works. `main` stays at the course
> starting point, with no AI or MCP code at all, so clone that branch to follow along
> from Class 1.

Companion repository for the [Spring AI + MCP course](https://themcpguy.com/docs/mcp-spring-ai/why-spring-ai)
on themcpguy.com.

## What you get

`order-service` is an ordinary Spring Boot application: JPA entities, Spring Data
repositories, a business service, a REST controller, and seed data for 200 orders across
42 customers. None of it is taught in the course. It is here so the classes can spend their
time on MCP rather than on Spring Boot.

```
order-service/          the application. MCP is added to it from Class 2.
support-agent/          the agent: an MCP client since Class 6, with a model since Class 7,
                        reading resources and running prompts since Class 8, talking to
                        a second server since Class 9 and a third since Class 10, relaying
                        server progress to the browser since Class 11, and answering the
                        server's confirmation questions since Class 12.
frontend/               React + Vite. Never taught, never changed.
support-kb/             the support team's notes, served over MCP since Class 9
support-kb-archive/     the pre-2024 versions of the same notes, served since Class 10
```

The product policies live on the classpath at
`order-service/src/main/resources/policies/`, and Class 4 exposes them as MCP resources.
They are ordinary Markdown, so editing one changes what the server serves on the next
restart.

## Prerequisites

- JDK 21 or later
- Maven 3.9 or later
- Node.js 20 or later, for the frontend and, from Class 9, for the filesystem MCP
  server on npm
- An API key from Anthropic or OpenAI, **or** [Ollama](https://ollama.com) running
  locally. Needed from Class 7 on: the agent hands the order tools to a model.

## Running the backend

```bash
mvn -pl order-service spring-boot:run
```

- REST API: `http://localhost:8080/api/orders/ORD-10001`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:orders`, user `sa`,
  empty password)

From Class 2 this same application also publishes an MCP endpoint on `/mcp`. On this branch
it registers six tools, all defined in
`order-service/src/main/java/com/themcpguy/supportdesk/orders/mcp/OrderTools.java`:

| Tool                   | What it does                                  | Read only |
|------------------------|-----------------------------------------------|-----------|
| `get_order`            | One order by ID                               | yes       |
| `get_customer_orders`  | Every order for one customer                  | yes       |
| `get_orders_by_status` | Every order in one status                     | yes       |
| `update_order_status`  | Move an order to a new status                 | no        |
| `recheck_shipments`    | Refresh estimates for every in-transit order  | no        |
| `cancel_order`         | Cancel an order, after the person confirms    | no        |

`recheck_shipments` is the Class 11 tool: it walks every shipped order and reports
progress and log messages through the MCP request context while it does.

`cancel_order` is the Class 12 tool. For a PENDING or PROCESSING order it pauses
mid-call with `context.elicit(...)` and asks the person to confirm; only an accepted
answer with `confirmed: true` cancels the order and starts the refund. From this class
`update_order_status` rejects the CANCELLED status and points at `cancel_order`
instead, so the model cannot route around the question. The elicitation request needs
an open connection back to the client, which is why the server runs the `STREAMABLE`
protocol rather than `STATELESS`, and its `request-timeout` is raised to 90 seconds
to give a human time to answer.

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
Registered tools: 6
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

On startup it prints what every connected server offers, and since this class there are
three connections. The agent reaches the order service over HTTP, and it starts the
reference filesystem server from npm twice, as child processes it talks to over stdio:
once for `support-kb/`, the current notes, and once for `support-kb-archive/`, the
notes as they stood before 2024. Nothing gets installed for that:
`npx -y @modelcontextprotocol/server-filesystem <directory>` fetches the server on
first use and hands it the one directory it may touch. The connections are declared in
`support-agent/src/main/resources/application.yaml`, and their relative paths resolve
at the repository root because the parent `pom.xml` pins the Spring Boot plugin's
working directory there. On Windows `npx` needs a `cmd.exe /c` wrapper; the Class 9
lesson shows the variant.

Two copies of the same server offer the same fourteen tool names, so this class also
decides what the model is given. `ServerNamePrefixGenerator` names each file tool
after its connection, `knowledge_base_read_text_file` against
`knowledge_base_archive_read_text_file`, and leaves the order-service tools unprefixed.
`ReadOnlyKnowledgeBaseFilter` then drops every file tool that can change the notes,
keeping three read-only tools per connection. The inspector prints the per-connection
listings as before, with `secure-filesystem-server 0.2.0` now appearing twice, and
closes with the result of both:

```
The model is given 12 tools:
  knowledge_base_archive_read_text_file
  knowledge_base_archive_list_directory
  knowledge_base_archive_list_allowed_directories
  knowledge_base_read_text_file
  knowledge_base_list_directory
  knowledge_base_list_allowed_directories
  cancel_order
  get_customer_orders
  get_order
  get_orders_by_status
  recheck_shipments
  update_order_status
```

then it waits for input. Ask about an order; type `quit` to leave. Every fact in an
answer comes from the tools, and the DEBUG lines in between show each call as it
happens. The order service must be up first, on the port named in
`support-agent/src/main/resources/application.yaml`. Templates are not returned by
`resources/list`, so the inspector asks for them separately with
`resources/templates/list`.

A question that needs the notes shows the servers working together. Ask

```
ORD-10001 was shipped with DHL and has not arrived. What should I do?
```

and the model calls `get_order` on the order service, then reads `carriers.md` through
the filesystem server for the DHL claims window and contact number. Escalation rules and
the refund procedure live in the same directory, so "When do I escalate a delayed order
to a manager?" is answered from `escalation.md` without touching the order service. Ask
what is in `/etc/passwd` and the answer is a refusal: the filesystem server does not
reach outside the directory it was given.

The archive earns its place with orders that predate 2024. Ask

```
A customer has come back about ORD-10004. They say they returned it in 2023 and were
never refunded. What did our process say at the time?
```

and the model calls `get_order`, sees an order from November 2023, and reads the refund
process from the archive rather than from the current notes: the system prompt tells it
to pick the source by the order's date, and to say which rules it is quoting.

Since Class 11 a long tool call reports while it runs. Ask

```
Refresh the delivery estimates for everything that's still in transit
```

and the model calls `recheck_shipments`, which walks all 87 shipped orders. The server
sends a log line, one progress notification per order, and a closing log line, and the
agent's `@McpProgress` and `@McpLogging` handlers print them as they arrive:

```
  INFO Rechecking 87 shipments
  [cli] 1%
  [cli] 2%
  ...
  [cli] 100%
  INFO Finished. 13 estimates changed.
```

The token in brackets is the conversation ID the agent put into the tool context. The
progress handler uses it to tell conversations apart; in web mode it routes each
percentage over the `/api/events` stream to the browser tab that asked, and the
frontend's progress bar fills as they arrive. Log messages carry a level and text but
no token, so they stay in the terminal. Ask the same question again and the summary
says `0 estimates changed`: the estimate is derived from the shipping date, so a
second pass arrives at the same answer, which is what the tool's idempotent hint
claims.

Since Class 12 a destructive tool asks before it acts. Ask

```
Please cancel ORD-10002, the customer changed their mind
```

and the model calls `cancel_order`. The tool pauses on the server, sends an
elicitation request back over the same connection, and the agent's
`ConfirmationHandler` presents it in the terminal:

```
Cancel order ORD-10002 for Marcus Adeyemi? The total is 34.99 and a refund will be started.
Type 'yes' to confirm:
```

Type `yes` and the tool resumes, cancels the order and reports the refund; any other
answer declines, and the tool reports that the order stayed as it was. The system
prompt tells the model to call the tool and let it ask its own question rather than
asking for permission in chat first: a model-side question is only a suggestion,
while the one inside the tool runs every time the code does. In web mode a different
handler answers, `BrowserConfirmationHandler`, selected by Spring profile. It pushes
the question over the `/api/events` stream to the browser tab that started the
conversation, identified by the `conversationId` the server copied into the
request's metadata, and parks the calling thread on a `SynchronousQueue` until the
dialog answers or 60 seconds pass. The three timeouts are layered so the innermost
one always fires first: the dialog gives up after 60 seconds, the server's
elicitation request after 90, the agent's own client requests after 2 minutes. If
the tab has gone away, or the question arrives without a conversation ID the
browser is watching, the handler declines immediately, which the tool treats the
same as a "no".

Started without the `cli` profile,

```bash
mvn -pl support-agent spring-boot:run
```

it serves the agent to the frontend on port 8081 instead of reading the terminal. Since
Class 8 that surface has grown:

- `POST /api/chat` reads `policy://returns` on every call and puts the policy into the
  system prompt, so a question about returns is answered from the file the server is
  serving right now, not from whatever the model remembers. When the request carries an
  `orderId`, the agent also reads `order://{orderId}` and attaches that order to the
  conversation. Both reads go through `McpResources`.
- `POST /api/refund-email` takes an `orderId` and a `reason`, fetches the server's
  `draft_refund_email` prompt with those arguments, sends the resulting messages to the
  model, and returns the drafted email. The work happens in `RefundEmailService`.
- `POST /api/confirmations/{id}` carries the browser's answer to a confirmation
  question back to the handler thread that is parked waiting for it. Only the
  confirmation dialog calls it, with the one-time ID the question arrived with.

To see the policy read happening, edit
`order-service/src/main/resources/policies/returns.md`, restart the order service, and
ask the same returns question again: the answer follows the file.

To run on Ollama instead of Anthropic, pull the model once with `ollama pull qwen3:8b`,
then pick the provider on the command line. No API key is needed:

```bash
mvn -pl support-agent spring-boot:run \
  -Dspring-boot.run.profiles=cli \
  -Dspring-boot.run.arguments=--spring.ai.model.chat=ollama
```

The same switch selects OpenAI: set `OPENAI_API_KEY` and pass
`--spring.ai.model.chat=openai`.

## Running the frontend

```bash
cd frontend
npm install     # first time only
npm run dev
```

Open `http://localhost:5173`. The order list works immediately, and with the agent
running in web mode the chat panel talks to it on port 8081. Selecting an order attaches
it to the conversation, and the **Draft refund email** button on a shipped, delivered or
cancelled order calls `/api/refund-email`; both are answered by the agent from Class 8
on. Since Class 11 the progress bar in the chat panel fills while the server rechecks
shipments in bulk, and since Class 12 asking the agent to cancel an order opens the
confirmation dialog: it shows the server's question with a 60 second countdown, and
**Confirm** or **Decline** posts the answer to `/api/confirmations`. Letting the
countdown run out counts as walking away, and the order stays as it was.

## Running the tests

```bash
mvn test
```

Ten tests covering the seed data and the business logic. They exist so that a change here
is caught before it reaches a lesson that quotes the numbers.

## The data

Four orders are fixed because the course quotes them:

| Order       | Status    | Customer                   | Total   |
|-------------|-----------|----------------------------|---------|
| `ORD-10001` | SHIPPED   | Ana Ruiz (`CUST-42`)       | €179.99 |
| `ORD-10002` | PENDING   | Marcus Adeyemi (`CUST-17`) | €34.99  |
| `ORD-10003` | DELIVERED | Ana Ruiz (`CUST-42`)       | €599.00 |
| `ORD-10004` | DELIVERED | Marcus Adeyemi (`CUST-17`) | €89.99  |

`ORD-10004` was delivered in November 2023 with Parcelforce, a carrier only the
archived notes cover, so Class 10 has an order that predates the 2024 policy change.
The other 196 are generated from the index rather than randomly, so every run produces
the same data. The spread is 87 shipped, 50 delivered, 30 pending, 25 processing and
8 cancelled.

The database is H2 in memory and is seeded at startup, so restarting throws away anything
the course changed. That is deliberate: nothing you do while following along is permanent.

## Changing the port

If 8080 is taken, change it in `order-service/src/main/resources/application.yaml` and
point the frontend proxy at the same port in `frontend/vite.config.js`.

If 8081 is taken, change `server.port` in
`support-agent/src/main/resources/application.yaml` and point the four agent entries of
the same proxy (`/api/chat`, `/api/refund-email`, `/api/events`, `/api/confirmations`)
at the new port.

If 5173 is taken, change `server.port` in `frontend/vite.config.js`, or pass the port on
the command line for a single run:

```bash
npm run dev -- --port 5174
```

Nothing in the backend refers to the frontend port, so this needs no other change. Open
the new port instead of 5173.
