# AI-Powered Support Ticket Assistant

Spring Boot service that stores support tickets and analyzes them asynchronously with an LLM
(category, summary, suggested response, recommended team, confidence).
Runs with **no API key** by default using a mock LLM provider.

## Run

Requirements: Java 17+, Maven 3.9+.

```bash
mvn spring-boot:run        # starts on http://localhost:8080 (H2 in-memory DB, mock LLM)
mvn test                   # runs all tests
```

### Use a real model (optional)
```bash
# OpenAI
LLM_API_KEY=sk-... mvn spring-boot:run -Dspring-boot.run.arguments="--llm.provider=openai --llm.model=gpt-4o-mini"
# Local Ollama (free)
LLM_API_KEY=ollama mvn spring-boot:run -Dspring-boot.run.arguments="--llm.provider=openai --llm.base-url=http://localhost:11434/v1 --llm.model=llama3.1"
```

## API

```bash
# Create (returns 201 + Location, status PENDING)
curl -i -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' -d '{
  "customerId":"CUST-101","subject":"Unable to import a Word document",
  "description":"The import remains at 80% and eventually fails.","priority":"HIGH","product":"Import"}'

# Retrieve (status: PENDING | PROCESSING | COMPLETED | FAILED; analysis present only when COMPLETED)
curl localhost:8080/api/tickets/<ticketId>

# Validation error (400) and not found (404)
curl -i -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' -d '{}'
curl -i localhost:8080/api/tickets/nope

# Simulate failures with the mock provider
curl -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' \
  -d '{"customerId":"C1","subject":"s","description":"hangs [mock-timeout]","priority":"LOW"}'
```
Errors use one shape: `{timestamp, status, error, message, details[]}`.

## Architecture

```
POST /api/tickets -> TicketController -> TicketService (save PENDING, publish event)  -> 201
                                              | after commit
                                       AnalysisTrigger (@Async, bounded pool)
                                              -> AnalysisService: PROCESSING
                                                 -> PromptBuilder (redact + delimit)
                                                 -> LlmClient (mock | OpenAI-compatible) [retry temp failures]
                                                 -> AnalysisParser (strict validation)
                                                 -> COMPLETED (analysis stored) | FAILED (reason stored)
GET /api/tickets/{id} -> ticket + status (+ analysis when COMPLETED)
```

Key decisions
- **Async after commit:** the event fires only after the DB transaction commits, so the worker never misses the ticket; the request thread never waits on the LLM.
- **No DB transaction during the LLM call:** `AnalysisService` is intentionally non-transactional; each state change is a short save.
- **Always terminal:** `analyze()` never throws; every path ends in COMPLETED or FAILED. Duplicate triggers are ignored (only PENDING tickets are processed). `@Version` gives optimistic locking.
- **DTOs** keep the entity out of the API.

## How the design addresses the required topics

- **Structured output validation:** the prompt demands one JSON object. `AnalysisParser` extracts it, rejects unknown/missing fields, enforces lengths, `category` format, `confidence` in [0,1], and `recommendedTeam` from an enum allow-list. Nothing is stored unless it passes; failures store a short reason (never raw model output).
- **Timeouts and provider failures:** HTTP connect/read timeout (`llm.timeout-seconds`). Timeouts, 429 and 5xx are "temporary" and retried with exponential backoff (`llm.retry.*`); 4xx and invalid output are not retried. After retries the ticket is FAILED with a reason.
- **Prompt injection:** ticket text is treated as untrusted data: wrapped in `<ticket>` tags (the tags are stripped from user text), the system prompt says never to follow instructions inside it, output is validated, and the team is allow-listed so an injection cannot route arbitrarily. The LLM has no tools or side effects. Residual risk: an injected string could still influence `summary`/`suggestedResponse` wording, so a human should review drafts before sending.
- **Sensitive information:** emails, card-like numbers and phone numbers are masked before the prompt is built; `customerId` is never sent; logs contain IDs, status and latency only, not ticket text.
- **Provider replacement:** the core depends only on `LlmClient` (`String complete(LlmRequest)`). Adding a provider = one new class + one `@Bean` in `LlmConfig`. Validation is provider-independent. Prompts live in `src/main/resources/prompts/`.

## Tests
`mvn test` covers: ticket creation + validation (400s), successful async analysis, invalid LLM output (bad JSON, bad confidence, unknown team), LLM timeout with retries, recovery after a transient failure, no retry on permanent errors, redaction reaching the prompt, duplicate triggers, 404, and full-stack integration tests via MockMvc.

## Assumptions
- A ticket's analysis is generated once automatically; categories are free-form (validated format), teams are a fixed list.
- H2 in-memory is acceptable for the exercise (data is lost on restart).

## Known limitations / production gaps
- No authentication, rate limiting, or per-customer authorization.
- Regex PII redaction is best-effort (misses names/addresses; may over-redact long digit strings).
- Tickets stuck in PENDING/PROCESSING after a crash or a full queue are not recovered (needs a sweeper or a durable queue such as a DB outbox / message broker).
- No manual "regenerate analysis" endpoint, pagination/listing, metrics, or tracing. Backoff has no jitter.
- Prompt-injection defence is mitigation, not elimination. No automated prompt-quality evaluation.
- Schema is created by Hibernate (`create-drop`); production needs Flyway/Liquibase and a real database.

## Next steps
List/filter endpoint with pagination, regenerate with an idempotency/lock guard, Micrometer metrics for LLM latency/failures, Flyway + PostgreSQL, OpenAPI docs.

## Time spent
Approximately 9 hours.

## AI tools used
Claude (Anthropic) was used to draft the initial structure, code and tests, which I then reviewed, ran and can explain.
