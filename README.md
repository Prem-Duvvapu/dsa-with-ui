# DSA Visualizer

[![CI](https://github.com/Prem-Duvvapu/dsa-with-ui/actions/workflows/ci.yml/badge.svg)](https://github.com/Prem-Duvvapu/dsa-with-ui/actions/workflows/ci.yml)

A full-stack visualizer for data structures and algorithms. Pick a problem, give it your
own input, and watch the algorithm execute step by step with the matching line of Java
highlighted as it runs.

**Status: 433 problems catalogued, 8 with real execution traces.** Those two numbers are
different on purpose, and the API reports both — see
[Coverage](#coverage-catalogued-vs-traced) below.

---

## Architecture

| Tier | Technology | Notes |
| :--- | :--- | :--- |
| Backend | Spring Boot 3.2.3, Java 17 | `http://localhost:8923` |
| Frontend | React 18 + Vite | Multi-mode canvases: graph, tree, array, linked list, recursion tree, grid |
| Testing | JUnit 5 + Vitest | ~332 backend, 44 frontend |
| Deployment | Docker Compose | One command for both tiers |

### How a trace is produced

An `AlgorithmTracer` runs the real algorithm and emits a step at each meaningful state
change. It declares its inputs machine-readably, so the frontend renders an editor for
any problem without per-problem form code.

```java
public interface AlgorithmTracer {
    String id();                              // "kadane-algo"
    InputSpec inputSpec();                    // declared inputs, bounds, defaults
    String annotatedCode();                   // Java source carrying // @a anchors
    void run(Inputs in, StepEmitter emit);    // executes the algorithm for real
}
```

Two details matter:

**There is no fallback.** `TracerRegistry` indexes tracers by id and returns nothing for
an unregistered one, so the API answers 404 or 501 rather than substituting a different
algorithm's animation.

**Lines are named, not numbered.** A tracer writes `emit.at("loop.compare")`, and the
`// @a loop.compare` marker is stripped from the source before it reaches the code
viewer. The highlight therefore provably refers to the code on screen, which a
hand-written line number does not.

```java
// @a loop.compare
if (running > best) best = running;
```

---

## Running it

### Docker — both tiers, one command

```bash
docker-compose up --build
```

Open **http://localhost:5174**.

### Locally

```bash
cd backend && mvn spring-boot:run     # http://localhost:8923
cd frontend && npm install && npm run dev   # http://localhost:5180, proxied to 8923
```

---

## API

The current API is a single catalogue with runnable problems.

| Endpoint | Purpose |
| :--- | :--- |
| `GET /api/problems` | The whole catalogue. Each entry carries `traced` and, when traced, its `inputSpec`. |
| `GET /api/problems/stats` | `catalogued`, `traced`, `untraced`, plus known duplicate ids. |
| `GET /api/problems/{id}` | Full detail, including the anchor-stripped source. |
| `GET /api/problems/{id}/execute` | Runs the problem on its declared default input. |
| `POST /api/problems/{id}/execute` | Runs it on **your** input. |
| `GET /api/problems/{id}/input-spec` | The input contract alone, for building a form first. |

Running a problem with your own input:

```bash
curl -X POST http://localhost:8923/api/problems/binary-search-1d/execute \
  -H 'Content-Type: application/json' \
  -d '{"nums":[2,4,6,8,10,12],"target":10}'
```

Status codes are meaningful:

- **404** — no such problem
- **501** — the problem is catalogued but has no tracer yet
- **400** — your input was rejected, with a message per field:

```json
{
  "error": "invalid_input",
  "fieldErrors": { "nums": "This algorithm needs a sorted list; 3 comes after 9." }
}
```

Input size is capped per problem, and every trace has two ceilings. The **step budget**
(default 5000) bounds CPU — it matters once a caller can set `n` on a factorial-time
algorithm. The **byte budget** (default 2 MB) bounds the response, because every step
carries a snapshot of the data structure, so the payload grows as steps x n: a 5000-step
trace of a 40-element array is roughly 11 MB of JSON. Hitting either returns
`truncated: true` with a `truncationReason` naming which one stopped the run.

The eighteen legacy per-topic endpoints (`/api/arrays/...`, `/api/trees/...`, and so on)
still work while the frontend migrates, and will be removed once it has.

---

## Coverage: catalogued vs traced

The catalogue lists every problem the project intends to cover. A problem is **traced**
only when a real `AlgorithmTracer` executes it.

This distinction exists because it was previously absent. 303 of the catalogued problems
returned another algorithm's animation — 122 one-line delegate methods plus a
step-returning `default:` in each of the eighteen service switches — and the test suite
could not tell, because its only per-problem assertion was that the step list was
non-empty, which the fallback guaranteed.

`GET /api/problems/stats` is the authoritative number. Nothing in this README is
hand-maintained coverage data.

| Category | Catalogued |
| :--- | ---: |
| Advanced Graphs & Graph Strings | 62 |
| Dynamic Programming | 55 |
| Binary Trees & BST | 54 |
| Arrays & Matrices | 40 |
| Binary Search | 32 |
| Linked List & Doubly LL | 31 |
| Stack & Queue | 30 |
| Recursion & Backtracking | 25 |
| Bit Manipulation & Advanced Math | 18 |
| Heaps & PriorityQueue | 17 |
| Strings | 16 |
| Greedy | 15 |
| Sliding Window & Two Pointer | 12 |
| Graphs: BFS & DFS | 11 |
| Basic Math | 7 |
| Basic Recursion | 7 |
| Sorting | 5 |
| Tries | 3 |
| **Total registrations** | **440** |
| **Unique ids** | **433** |

Seven ids are claimed by two services with different content: `dfs-traversal`,
`flood-fill`, `longest-common-prefix`, `longest-substring-without-repeating`,
`merge-intervals`, `number-of-islands` and `surrounded-regions`.
The catalogue surfaces these in `stats.duplicateIds` rather than hiding them; resolving
them means moving problems between services.

### Traced so far

`two-sum`, `kadane-algo`, `binary-search-1d`, `tree-preorder`, `tree-inorder`,
`reverse-linked-list`, `bfs-traversal`, `number-of-islands`.

Chosen to exercise every input kind and the worst-covered categories — Binary Search had
32 problems sharing one animation, and Binary Trees had 54 sharing a single three-step
stub.

---

## Tests

```bash
cd backend  && mvn test        # ~332 tests
cd frontend && npx vitest run  #   44 tests
```

The suite is built to catch fake work, not just crashes:

- **`TracerContractTest.traceRespondsToItsInput`** runs every tracer on two materially
  different inputs and fails if the traces match. A canned narration cannot survive it.
- **`noTwoTracersProduceIdenticalTraces`** applies the same idea across the registry.
- **`stepCountGrowsWithInput`** runs each tracer at two sizes and fails if the step count
  does not rise — catching a narration that varies its wording but not its length, which
  the distinctness tests above cannot see.
- **`anchorsAreAllReachable`** fails on a `// @a` marker no step ever highlights. It used
  to assert only that *something* was emitted, and six of the eight tracers failed the
  moment it started checking what it claimed to.
- **`ApiContractTest`** is parameterized over all eighteen legacy controllers rather than
  testing one by hand — hand-testing one is what let three copy-pasted variants diverge,
  with eight of them silently dropping their 404 guard.
- **`designTokens.test.js`** fails the build on any unresolvable CSS `var()`. Fifteen
  custom properties were once deleted while five components still referenced them, and
  nothing noticed.
- **`InputValidatorTest`** covers every field kind and rejection path, since that
  validator is the only trust boundary between a request body and a running algorithm.

---

## Documentation

| File | What it is |
| :--- | :--- |
| `plan.md` | The v2 tracing architecture. Accurate; the source of the current design. |
| `references.md` | UI/UX research and the design-token system. |
| `PROJECT_CONTEXT.md` | Pedagogical principles behind the visualizations. |
| `HANDOFF.md` | **Temporary.** Implementation prompts for the remaining phases. Delete once the migration is complete. |
