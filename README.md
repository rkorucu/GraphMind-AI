# Aegis Multi-Agent Platform

Aegis is a sophisticated, event-driven multi-agent orchestration platform built with **Spring Boot**, **LangGraph4j**, and **React**. It employs a targeted team of autonomous AI agents to break down complex queries, gather external intelligence, assess risks, and critically review findings through an iterative reflection loop before synthesizing a final response.

## 🏗️ Architecture Design

```text
                                   +------------------------------------------------+
                                   |                 LangGraph Pipeline             |
   +----------------+              |                                                |
   |                |  SSE Stream  |  +---------+   +----------+   +---------+      |
   | React Frontend | <----------- |  | Planner |-->| Research |-->|  Risk   |      |
   |                |              |  +---------+   +----------+   +---------+      |
   +-------+--------+              |      ^                              |          |
           | REST Post             |      |                              |          |
           v                       |      |         +----------+         v          |
   +-------+--------+              |      +---------|  Critic  |<--------+          |
   | Spring Boot    |              |      [REVISE]  +----------+                    |
   | Orchestrator   |=============>|                     | [APPROVE]                |
   +-------+--------+              |                     v                          |
           |                       |                +-------------+                 |
           | Tool Calling          |                | Synthesizer |                 |
           v                       |                +-------------+                 |
   +----------------+              |                                                |
   | External APIs  |              +------------------------------------------------+
   | (Web, Search)  |
   +----------------+
```

## 🧠 Multi-Agent Topology

Aegis implements a highly specialized, role-based multi-agent paradigm. Each node in the orchestration graph is an independent LLM agent with a specific mandate and system instruction set:

- **Planner Agent**: Analyzes the initial prompt and decomposes it into a structured, step-by-step intelligence gathering plan.
- **Research Agent**: Equipped with dynamic tool-calling capabilities to query live web data, financial indices, and literature. Retrieves contextually relevant grounding data.
- **Risk Assessment Agent**: Evaluates the gathered intelligence against systemic, financial, and operational risk taxonomies. Appends severity scores and mitigation strategies.
- **Critic Agent (The Gatekeeper)**: A secondary evaluation layer that reviews the cumulative state. Implements the reflection loop by either approving the data or enforcing a revision cycle with specific delta requirements.
- **Synthesizer Agent**: Compiles the validated, multi-dimensional state object into a coherent, high-fidelity final response, complete with dynamically generated confidence scores.

## 🔄 Orchestration & Reflection Loop

The core orchestration operates as a **directed cyclic graph (DCG)** via LangGraph4j. State management is handled through a shared, immutable state object (`AegisState`) that accumulates context, tool outputs, and inter-agent messages as it traverses the graph.

**Reflection Loop Logic:**
The Critic Agent serves as a conditional routing node. Upon receiving the working state:

1. It analyzes the depth, accuracy, and risk assessment of the gathered data against the original prompt.
2. It yields a deterministic JSON verdict: `APPROVE`, `REVISE`, or `REJECT`.
3. If `REVISE` is triggered, the graph execution loops back to the **Planner Agent**.
4. The Planner incorporates the Critic's specific feedback, generating a dynamically revised plan, and the pipeline re-executes.
5. A deterministic `revisionCount` threshold prevents infinite looping, forcing synthesis if a maximum depth is reached.

## 🛠️ Tool Calling Design

Tool calling is decoupled and injected dynamically into specific agents (primarily Research and Risk). Tools are defined as Java interfaces with declarative `@Tool` annotations, binding natural language descriptions to executable Java methods.

When an agent reaches a deliberation phase, the LLM autonomously parses the JSON schema capabilities provided by the Orchestrator runtime, determining the necessity, sequence, and arguments for tool invocation. This allows Aegis to maintain a minimal foundational context window while dynamically pulling real-time data on demand.

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Node.js 20+ (20.19+ or 22.12+ recommended)
- Maven
- An active API key for Google Gemini (or a compatible LLM provider)

### Environment Setup

Configure the backend environmental variables. Create `application.yml` or set them in your terminal:

```bash
export GEMINI_API_KEY="your_api_key_here"
```

### Running Locally

**1. Start the Backend (Spring Boot)**

```bash
cd aegis-backend
./mvnw spring-boot:run
```

_The backend runs on `http://localhost:8080`. It exposes the execution endpoints, including the real-time Server-Sent Events (SSE) trace path at `/api/agent/analyze-stream` and the automated metrics tool at `/api/agent/eval`._

**2. Start the Frontend (React + Vite)**

```bash
cd aegis-frontend
npm install
npm run dev
```

_The React application runs on `http://localhost:5173`. Submit a complex business or technical query to watch the real-time agent execution stream, timeline visualization, and reflection loops._

## 🎙️ Interview Talking Points

If discussing this architecture in a technical interview, emphasize the following system design decisions:

1. **State Management & Consistency**: Highlight how the immutable `AegisState` acts as the single source of truth across the distributed agent nodes, mitigating hallucination drift by forcing stateless agents to re-hydrate from a verified context object during each DAG traversal.
2. **Server-Sent Events (SSE) over WebSockets**: Explain the deliberate choice of SSE for the frontend streaming timeline. Unlike bi-directional WebSockets, SSE is natively unidirectional, operates over a standard HTTP connection, is easier to load-balance, and is semantically perfectly aligned with an append-only event trace log.
3. **The Critic Reflection Loop**: Position this as the key differentiator from standard linear "chain" LLM applications. It proves an understanding of compound AI systems, where self-correction and iterative refinement yield demonstrably higher-quality outputs than zero-shot prompts.
4. **Resiliency and Guardrails**: Discuss the `revisionCount` threshold to prevent infinite LLM loops and the implementation of strict JSON schema adherence to guarantee that untyped LLM output safely binds to strongly-typed internal Java objects via Jackson.
