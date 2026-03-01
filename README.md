# Aegis Orchestrator

**A graph-based multi-agent AI orchestration platform for structured decision-making.**

Built with Java 21, Spring Boot 3, LangGraph4j, and React. Designed for production environments where single-prompt LLM calls are insufficient and complex analytical workflows require coordinated, validated, multi-step reasoning.

---

## The Problem

Most LLM-powered applications follow a trivial pattern: accept a prompt, call a model, return a response. This works for chatbots. It fails catastrophically for systems that require:

- **Multi-perspective analysis** — a single model call cannot simultaneously plan, research, assess risk, and critique its own output.
- **Structured, validated outputs** — raw LLM text is unreliable for downstream systems that expect strict JSON contracts.
- **Bounded reasoning loops** — without explicit control flow, reflection-based architectures either loop infinitely or never self-correct.
- **Deterministic grounding** — LLMs hallucinate. Financial projections, market data, and risk scores must come from deterministic sources.

Aegis Orchestrator solves this by decomposing complex queries into a **directed acyclic graph of specialized agents**, each with a defined role, validated output schema, and access to deterministic tool pipelines.

---

## What Is Multi-Agent Orchestration?

Multi-agent orchestration is the coordination of multiple AI agents — each with a distinct persona, prompt template, and output schema — through a structured execution graph. Unlike simple sequential chains:

| Feature           | Simple Chain         | Graph-Based Orchestration           |
| ----------------- | -------------------- | ----------------------------------- |
| Execution flow    | Linear, fixed        | Conditional, branching              |
| Self-correction   | None                 | Bounded reflection loops            |
| State management  | Pass-through strings | Shared typed state object           |
| Output validation | None                 | JSON schema enforcement per node    |
| Tool integration  | Ad-hoc               | Deterministic, grounded pipelines   |
| Failure handling  | Crash                | Graceful degradation with fallbacks |

Aegis implements **graph-based orchestration** using LangGraph4j, where each node is an autonomous agent and edges define conditional transitions based on runtime state.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend                          │
│              Home  ·  Timeline (SSE)  ·  Result             │
└──────────────────────────┬──────────────────────────────────┘
                           │  HTTP / SSE
┌──────────────────────────▼──────────────────────────────────┐
│                   Spring Boot Backend                       │
│                                                             │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────┐   │
│  │   REST API  │───▶│ Orchestrator │───▶│  Agent Graph  │   │
│  │  /analyze   │    │   Service    │    │  (LangGraph)  │   │
│  └─────────────┘    └──────────────┘    └───────┬───────┘   │
│                                                 │           │
│  ┌──────────────────────────────────────────────▼────────┐  │
│  │                    Agent Nodes                        │  │
│  │                                                       │  │
│  │  ┌─────────┐  ┌──────────┐  ┌──────┐  ┌───────────┐  │  │
│  │  │ Planner │─▶│ Research │─▶│ Risk │─▶│  Critic   │  │  │
│  │  └─────────┘  └────┬─────┘  └──────┘  └─────┬─────┘  │  │
│  │                    │                         │        │  │
│  │              ┌─────▼─────┐          ┌────────▼──────┐ │  │
│  │              │   Tools   │          │  Conditional  │ │  │
│  │              │ Market    │          │   Routing     │ │  │
│  │              │ Finance   │          │               │ │  │
│  │              │ Risk Score│          │ APPROVE → Syn │ │  │
│  │              └───────────┘          │ REVISE  → Plan│ │  │
│  │                                     └───────────────┘ │  │
│  │                         ┌──────────────┐              │  │
│  │                         │ Synthesizer  │──▶ END       │  │
│  │                         └──────────────┘              │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────┐  ┌───────────────┐  ┌──────────────────┐  │
│  │   Validator  │  │  Node Tracer  │  │ Exception Handler│  │
│  │ (JSON Schema)│  │  (traceId)    │  │  (Global)        │  │
│  └──────────────┘  └───────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Execution Lifecycle

The orchestration graph executes in the following sequence:

```
START
  │
  ▼
┌─────────┐     ┌──────────┐     ┌──────┐     ┌─────────┐
│ Planner │────▶│ Research │────▶│ Risk │────▶│ Critic  │
└─────────┘     └──────────┘     └──────┘     └────┬────┘
                                                   │
                                        ┌──────────┴──────────┐
                                        │                     │
                                   verdict ==            verdict ==
                                   "APPROVE"             "REVISE"
                                   OR loops ≥ 2          AND loops < 2
                                        │                     │
                                        ▼                     ▼
                                  ┌────────────┐        ┌─────────┐
                                  │ Synthesizer│        │ Planner │
                                  └─────┬──────┘        │ (retry) │
                                        │               └─────────┘
                                        ▼
                                       END
```

### Agent Responsibilities

| Agent           | Role                                                                           | Output Schema                                                               |
| --------------- | ------------------------------------------------------------------------------ | --------------------------------------------------------------------------- |
| **Planner**     | Decomposes the user query into a structured execution plan with numbered steps | `{ objective, steps[], reasoning }`                                         |
| **Research**    | Executes the plan by gathering data via tool calls and LLM reasoning           | `{ query, findings[], summary }`                                            |
| **Risk**        | Evaluates potential risks, assigns severity levels, and proposes mitigations   | `{ context, risks[], overallRiskLevel, assessment }`                        |
| **Critic**      | Reviews all prior outputs for quality, completeness, and consistency           | `{ reviewedContent, issues[], score, verdict }`                             |
| **Synthesizer** | Produces the final structured response from all accumulated evidence           | `{ inputSummary, synthesis, keyPoints[], recommendation, confidenceLevel }` |

---

## State Management

All agents share a single typed state object (`AegisState`) that accumulates outputs across the graph:

```java
public class AegisState {
    String   userQuery;          // Original input
    String   plannerOutput;      // JSON from Planner
    String   researchOutput;     // JSON from Research
    String   riskOutput;         // JSON from Risk
    String   criticOutput;       // JSON from Critic
    String   criticVerdict;      // APPROVE | REVISE | REJECT
    int      revisionCount;      // Tracks reflection iterations
    String   synthesizerOutput;  // Final JSON output
}
```

Each node reads from the state, writes its output back, and the graph engine manages propagation. There is no direct inter-agent communication — all coordination flows through shared state.

---

## Reflection Loop Control

The Critic agent acts as a **quality gate**. After evaluating all prior outputs, it emits a verdict:

- **APPROVE** → route to Synthesizer (terminal path)
- **REVISE** → route back to Planner for re-planning (reflection loop)
- **REJECT** → treated as REVISE with logged warning

Reflection is **bounded to 2 iterations** to prevent infinite loops:

```java
if ("APPROVE".equals(verdict) || revisionCount >= MAX_REVISIONS) {
    return SYNTHESIZER;  // Force-exit after 2 loops
}
return PLANNER;  // Re-enter the graph for revision
```

This guarantees termination while allowing the system to self-correct when initial outputs are insufficient.

---

## Deterministic Tool Integration

LLMs hallucinate. For financial projections, market analysis, and risk scoring, Aegis does **not** rely on the model to generate numbers. Instead, deterministic tools provide grounded data:

| Tool                      | Function                                                | Output             |
| ------------------------- | ------------------------------------------------------- | ------------------ |
| `MarketAnalysisTool`      | Returns market size, growth rate, competitive landscape | Deterministic JSON |
| `FinancialProjectionTool` | Calculates revenue projections, margins, break-even     | Deterministic JSON |
| `RiskScoringTool`         | Computes composite risk scores from weighted factors    | Deterministic JSON |

These tools are **not LLM-generated**. They execute fixed business logic and return structured data that agents incorporate into their reasoning. This eliminates hallucinated statistics and grounds the analysis in verifiable computations.

---

## Reliability & Safety Controls

### JSON Schema Validation

Every agent output is validated against a strict schema before propagation:

```
Agent generates response
        │
        ▼
  ┌─────────────┐     ┌──────────────┐
  │ Strip MD    │────▶│ Parse JSON   │
  │ backticks   │     │ (Jackson)    │
  └─────────────┘     └──────┬───────┘
                             │
                      ┌──────▼───────┐
                      │ Validate     │
                      │ required     │
                      │ fields,      │     FAIL → AgentValidationException
                      │ types,       │────────────────────────────────────▶
                      │ enums        │
                      └──────┬───────┘
                             │ PASS
                             ▼
                      Write to state
```

If validation fails, an `AgentValidationException` is thrown with a detailed error summary identifying exactly which fields or constraints were violated.

### Global Exception Handling

A `@RestControllerAdvice` handler catches all exception types and returns structured JSON errors:

- `AgentValidationException` → `422 Unprocessable Entity`
- `IllegalArgumentException` → `400 Bad Request`
- `RuntimeException` → `500 Internal Server Error` with root cause message

No stack traces are ever leaked to the client.

### Observability

Every request is tagged with a unique `traceId` (UUID) via an MDC filter. All log entries include this ID, enabling end-to-end request tracing:

```
2026-02-28 22:15:03.412 [thread-1] [traceId=a3f8c2e1-...] INFO  AgentGraphConfig — Completed node: planner
2026-02-28 22:15:05.891 [thread-1] [traceId=a3f8c2e1-...] INFO  AgentGraphConfig — Completed node: research
```

---

## API Contract

### `POST /api/agent/analyze`

**Request:**

```json
{
  "question": "Should a startup enter the AI insurance market?"
}
```

**Response:**

```json
{
  "analysis": "{\"inputSummary\": \"...\", \"synthesis\": \"...\", \"keyPoints\": [...], \"recommendation\": \"...\", \"confidenceLevel\": \"HIGH\"}",
  "confidence": 0.85,
  "traceId": "a3f8c2e1-7b4d-4e9f-b2a1-9c3d5e7f1a2b"
}
```

### `POST /api/agent/analyze-stream` (SSE)

Streams real-time agent step completions via Server-Sent Events:

```
event: step
data: {"node": "planner", "status": "done", "durationMs": 2341}

event: step
data: {"node": "research", "status": "done", "durationMs": 1872}

event: step
data: {"node": "risk", "status": "done", "durationMs": 1203}

event: step
data: {"node": "critic", "status": "done", "durationMs": 1567}

event: done
data: {"analysis": "...", "confidence": 0.85, "traceId": "..."}
```

### `GET /api/agent/eval`

Runs the evaluation harness against a stored dataset and returns aggregate metrics:

```json
{
  "totalQuestions": 5,
  "averageConfidence": 0.72,
  "averageReflections": 1.2,
  "averageExecutionTimeMs": 8432,
  "results": [...]
}
```

---

## Evaluation Harness

The system includes a built-in evaluation module that benchmarks pipeline quality:

1. A dataset of 5 representative questions is stored in `src/main/resources/dataset.json`
2. Each question is processed through the full agent pipeline
3. Metrics are collected per run:
   - **Reflection count** — how many revision loops were triggered
   - **Execution time** — end-to-end latency
   - **Confidence score** — extracted from the Synthesizer output
4. Aggregate statistics are computed and exposed via `GET /api/agent/eval`

This provides a repeatable, automated mechanism for measuring system quality after model changes, prompt modifications, or agent refactoring.

---

## Tech Stack

| Layer            | Technology                        |
| ---------------- | --------------------------------- |
| Language         | Java 21                           |
| Framework        | Spring Boot 3.4                   |
| Orchestration    | LangGraph4j                       |
| LLM Integration  | LangChain4j (OpenAI / Ollama)     |
| Frontend         | React 19, Vite                    |
| Containerization | Docker, Docker Compose            |
| Observability    | SLF4J + Logback, MDC traceId      |
| Validation       | Jackson + custom schema validator |

---

## Running Locally

### Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 18+
- [Ollama](https://ollama.com/) (for local model) **or** an OpenAI API key

### 1. Start the LLM Backend

**Option A — Local model (Ollama, no API key needed):**

```bash
ollama pull llama3.2
ollama serve
```

**Option B — OpenAI API:**
Update `aegis-backend/src/main/resources/application.yml` to use the OpenAI starter and set:

```bash
export OPENAI_API_KEY="sk-..."
```

### 2. Start the Backend

```bash
cd aegis-backend
mvn spring-boot:run
```

Backend starts on `http://localhost:8080`.

### 3. Start the Frontend

```bash
cd aegis-frontend
npm install
npm run dev
```

Frontend starts on `http://localhost:5173`.

### 4. Docker Compose (Alternative)

```bash
docker-compose up --build
```

This starts both backend and frontend in containers. The frontend is served via Nginx on port 80.

---

## Project Structure

```
aegis-multi-agent/
├── aegis-backend/
│   ├── src/main/java/com/aegis/backend/
│   │   ├── agent/           # Agent classes, state, graph config, validator
│   │   ├── config/          # Spring config, CORS, tracing filter
│   │   ├── controller/      # REST endpoints, SSE streaming, error handler
│   │   ├── service/         # Orchestration service, evaluation service
│   │   └── tools/           # Deterministic tools (Market, Finance, Risk)
│   └── src/main/resources/
│       ├── application.yml  # LLM provider config
│       ├── dataset.json     # Evaluation dataset
│       └── logback-spring.xml
├── aegis-frontend/
│   ├── src/
│   │   ├── pages/           # Home, Timeline, Result
│   │   ├── api/             # Backend API client (SSE + REST)
│   │   └── index.css        # Design system
│   └── vite.config.js
├── docker-compose.yml
└── README.md
```

---

## License

MIT
