# DSA Visualizer

[![CI](https://github.com/Prem-Duvvapu/dsa-with-ui/actions/workflows/ci.yml/badge.svg)](https://github.com/Prem-Duvvapu/dsa-with-ui/actions/workflows/ci.yml)

A full-stack visualizer for data structures and algorithms. Pick a problem, give it your
own input, and watch the algorithm execute step by step with the matching line of Java
highlighted as it runs.

**Status: 433 problems catalogued, 44 with real execution traces.** Those two numbers are
different on purpose, and the API reports both — see
[Coverage](#coverage-catalogued-vs-traced) below.

---

## Architecture

| Tier | Technology | Notes |
| :--- | :--- | :--- |
| Backend | Spring Boot 3.2.3, Java 17 | `http://localhost:8923` |
| Frontend | React 18 + Vite | `dsType` registry with nine current canvases, including DP tables |
| Testing | JUnit 5 + Vitest | Contract, golden-trace, accessibility, and design-token guards |
| Deployment | Docker Compose | One command for both tiers |

### How a trace is produced

An `AlgorithmTracer` runs the real algorithm and emits a step at each meaningful state
change. It declares its inputs machine-readably, so the frontend renders an editor for
any problem without per-problem form code.

```java
public interface AlgorithmTracer {
    String id();                              // "kadane-algo"
    DsType dsType();                          // closed visualization vocabulary
    InputSpec inputSpec();                    // declared inputs, bounds, defaults
    Map<String, Object> alternateInput();     // materially different test input
    String annotatedCode();                   // Java source carrying // @a anchors
    void run(Inputs in, StepEmitter emit);    // executes the algorithm for real
}
```

Two details matter:

**There is no fallback.** `TracerRegistry` indexes tracers by id and returns nothing for
an unregistered one, so the API answers 404 or 501 rather than substituting a different
algorithm's animation. Canvas routing is a checked 16-value `dsType` registry; an unknown
type renders “no visualization” rather than silently becoming an array.

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
./start.sh                            # installs frontend deps if needed; Ctrl+C stops both

# Or run the tiers separately:
cd backend && mvn spring-boot:run     # http://localhost:8923
cd frontend && npm ci && npm run dev  # http://localhost:5180, proxied to 8923
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
algorithm. The **byte budget** (default 2 MB) bounds collected structure data. On the wire,
periodic keyframes plus field-level deltas avoid retransmitting an unchanged full snapshot at
every step. Hitting either ceiling returns `truncated: true` with a `truncationReason` naming
which one stopped the run.

The eighteen legacy per-topic endpoints (`/api/arrays/...`, `/api/trees/...`, and so on)
remain compatibility-tested while migration continues. The frontend itself uses the unified
v2 `/api/problems` endpoints.

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

`aggressive-cows`,
`bfs-traversal`, `binary-search-1d`, `book-allocation`, `check-sorted-ii`, `count-inversions`,
`count-square-submatrices`,
`climbing-stairs`, `dfs-traversal`, `dijkstra-min-heap`, `find-missing-number`,
`frog-jump`, `frog-jump-k-distance`, `grid-unique-paths`, `house-robber-2`,
`kadane-algo`, `lower-bound`, `minimum-falling-path-sum`,
`ninjas-training`,
`largest-element`, `leaders-in-array`, `left-rotate-k`, `left-rotate-one`, `linear-search`,
`lis-binary-search`, `longest-increasing-subsequence`, `longest-subarray-sum-k-positives`,
`majority-element`, `max-consecutive-ones`, `max-rectangle-area-all-ones`,
`max-sum-non-adjacent`, `move-zeros-end`,
`n-meetings-in-one-room`, `next-permutation`, `number-of-islands`, `print-lis`,
`remove-duplicates-sorted`,
`reverse-linked-list`, `reverse-pairs`, `search-rotated-sorted`, `second-largest-element`, `single-number`,
`sort-0-1-2`,
`stock-buy-sell`, `tree-inorder`, `tree-level-order`, `tree-postorder`, `tree-preorder`,
`triangle-min-path-sum`, `two-sum`, `unique-paths-2`, and `upper-bound`.

Thirteen problems emit labelled, recurrence-aware `DpTable` traces: the three LIS
variants, plus `climbing-stairs`, `frog-jump`, `frog-jump-k-distance`,
`max-sum-non-adjacent`, `house-robber-2`, `grid-unique-paths`, `unique-paths-2`,
`minimum-falling-path-sum`, `triangle-min-path-sum` and `ninjas-training`. The five
basic-DP ones trace
against the full O(N) table rather than the rolling variables a space-optimised
version keeps — the dependency between cells is the lesson, and it is invisible once
the table collapses. `house-robber-2` carries both circle-breaking passes as their own
rows, with the forbidden house voided out, on a default input where the two passes
disagree. `grid-unique-paths` and `unique-paths-2` are genuinely two-dimensional
tables — rows and columns are the problem's own grid — and `unique-paths-2` traces an
obstacle as permanently excluded rather than merely zero.
`minimum-falling-path-sum` is the first with up to three live predecessors per cell
and an answer that reduces over an entire row rather than one fixed corner.
`triangle-min-path-sum` fills bottom-up, reading two children below instead of two
predecessors above, with its ragged rows carried as permanently excluded cells in a
square grid. `ninjas-training` is the first three-way choice rather than a fixed
neighbour shape: each cell excludes exactly one same-column predecessor (yesterday's
activity) and reads the other two. Migrated ids answer
**410 Gone** on their old execute endpoint rather than risking a substitute trace.

---

## Tests

```bash
cd backend  && mvn test
cd frontend && npm ci && npx vitest run
cd frontend && npx vite build
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
| `RCA.md` | Root causes, resolutions, open debt, and the regression guard for each recurring incident. |
