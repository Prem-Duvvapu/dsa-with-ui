# DSA Visualizer

[![CI](https://github.com/Prem-Duvvapu/dsa-with-ui/actions/workflows/ci.yml/badge.svg)](https://github.com/Prem-Duvvapu/dsa-with-ui/actions/workflows/ci.yml)

A full-stack visualizer for data structures and algorithms. Pick a problem, give it your
own input, and watch the algorithm execute step by step with the matching line of Java
highlighted as it runs.

**Status: 431 problems catalogued, all 431 with real execution traces.** The API still
reports catalogued, traced, and untraced counts independently — see
[Coverage](#coverage-catalogued-vs-traced) below. Complete execution traces now cover all
30 **Stack & Queue** problems, all 12 **Sliding Window** problems, all 54 **Binary Trees &
BST** problems, all 53 **Advanced Graphs** problems, all 14 unique **Greedy** problems,
all 17 **Heaps & PriorityQueue** problems, all 18 **Bit Manipulation & Advanced Math**
problems, all 24 **Strings** problems, all 32 **Binary Search** problems, and all 25
**Recursion & Backtracking** problems, all 31 **Linked List & Doubly LL** problems, and all
55 **Dynamic Programming** problems.

---

## Architecture

| Tier | Technology | Notes |
| :--- | :--- | :--- |
| Backend | Spring Boot 3.2.3, Java 17 | `http://localhost:8923` |
| Frontend | React 18 + Vite | `dsType` registry routing 17 problem types to 15 canvases |
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
                                     # Linux/WSL use setsid; macOS falls back to perl setpgrp,
                                     # so each service still leads its own process group

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
are **gone**. They were compatibility endpoints during the migration; with all 431 problems
traced there was nothing left for them to serve, and their controllers have been deleted —
those routes now 404. `/api/problems` is the only API.

The eighteen `service/*Service` classes survive as **catalogue providers**: they own the
`ProblemDetail` metadata `ProblemCatalog` merges, and nothing else. Their step generation
is deleted.

---

## Full system architecture

See **[`ARCHITECTURE.md`](ARCHITECTURE.md)** for the system in diagrams — the request path
and its guards, the tracer contract, how a `dsType` selects a canvas, and where the
cross-tier contracts sit. It also answers the two questions that come up most: there is no
database and none is needed, and the scaling bottleneck is CPU on `/execute` rather than
storage.

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
| Graphs | 58 |
| Dynamic Programming | 55 |
| Arrays | 40 |
| Binary Trees | 38 |
| Binary Search | 32 |
| Linked List | 31 |
| Stack & Queue | 30 |
| Recursion & Backtracking | 25 |
| Strings | 24 |
| Bit Manipulation | 18 |
| Heaps & PriorityQueue | 17 |
| BST | 16 |
| Greedy Algorithms | 14 |
| Learn the Basics | 14 |
| Sliding Window | 12 |
| Sorting Algorithms | 5 |
| Tries & Prefixes | 2 |
| **Total registrations** | **431** |
| **Unique ids** | **431** |

Graph BFS/DFS and Advanced Graph Algorithms are catalogued as one **Graphs** topic (see
below); Binary Trees and BST are two separate categories that both draw on
`BinaryTreeLayout`.

**No id is registered twice.** Seven were, for a long time — `dfs-traversal`, `flood-fill`,
`longest-common-prefix`, `longest-substring-without-repeating`, `merge-intervals`,
`number-of-islands` and `surrounded-regions` — and four further problems escaped that count
entirely by differing only in word order or tense (`rotten-oranges` / `rotting-oranges`,
`cycle-undirected-bfs` / `undirected-cycle-bfs`, and the two DFS cycle pairs).

All eleven came from one cause: `AdvancedGraphService` and `GraphBfsDfsService` catalogued
the same problems. They are now a single **Graphs** topic, and `DuplicateProblemTest` fails
on either shape — an id claimed twice, or two ids that are the same words rearranged.

Two of those pairs survive on purpose, because they teach the same problem two standard
ways and their titles say which: directed cycle detection by CLRS edge classification
against the two-boolean recursion path, and undirected cycle detection that reconstructs
the cycle against one that only answers whether it exists.

### Coverage detail

All 431 catalogued problems are traced — there is no partial list to maintain here.
For a specific problem's tracer, read its source in
`backend/src/main/java/com/dsa/ui/tracer/impl/`, or query the live catalogue:

```bash
curl -s http://localhost:8923/api/problems | python3 -c \
  "import json,sys; [print(p['id']) for p in json.load(sys.stdin)]"
```

A few design decisions from the migration are worth keeping in prose, because they explain
*why* a tracer looks the way it does rather than just *that* it exists:

Sixteen problems emit labelled, recurrence-aware `DpTable` traces: the three LIS
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
activity) and reads the other two. `edit-distance` and `wildcard-matching` are the first
two **string-alignment** tables: row and column are labelled by the two input strings'
own characters rather than indices, and a mismatch cell in `edit-distance` compares all
three neighbours at once (replace, delete, insert) to say which was actually cheapest,
while `wildcard-matching`'s `*` cells are the only ones in any traced table whose value is
an OR of two predecessors rather than a single read. `ninja-and-his-friends` is the only
genuinely **3D** DP problem traced so far — state is `(row, col1, col2)`, which does not fit
a single 2D `DpTable` — so each step instead shows a col1-by-col2 **slice** for the row
currently being computed, with the row index driving which slice is on screen counting down
from the last row to row 0; a slice reads only the previous row's finished values (named in
the narration, since that table has already been retired from view) and is never a fixed
narration for a fixed shape. Migrated ids answer
**410 Gone** on their old execute endpoint rather than risking a substitute trace.

`lru-cache` is the first traced problem whose input is a *sequence of operations* on one
stateful object rather than a single before/after computation — the same shape that got
the Heaps & PriorityQueue topic rejected outright (see `PROMPT-J-full-roadmap.md`). It
fits the tracer contract without a new `FieldType`: the whole `put`/`get` sequence is one
`FieldType.STRING` in a small semicolon-separated mini-language (`"put 1 1;put 2 2;get
1"`), validated with the same `.constraint("pattern", regex)` approach `kmp-lps-algo`
uses for its alphabet. Because the cache's own eviction order IS a doubly linked list —
most- to least-recently-used, front to back — it traces as `LinkedList` rather than
`Stack`, with each node labelled `key:value`.

`flattening-ll` (LeetCode 430) and `clone-ll-random-pointer` (LeetCode 138) are the first
two `LinkedList` traces to need a pointer beyond `next`/`prev`, which is why they were
deferred out of the Linked List batch that traced `find-starting-point-loop` and
`reverse-ll-group-k`. `ListNode` gained nullable `childId`/`randomId` fields first, as its
own additive, independently-verified PR, before either tracer was written — every other
`LinkedList` tracer leaves both null. `flattening-ll`'s input is a `FieldType.GRAPH` (a
weight-0 edge is a next pointer, a weight-1 edge is a child pointer) rather than an
`INT_GRID` of raw pointer indices, because a pointer-chasing traversal over a grid whose
rows `TracerContractTest`'s auto-grower duplicates would strand every added row as
unreachable — `GRAPH` is intentionally outside that grower's scope, so
`stepCountGrowsWithInput` skips it rather than failing on an input shape it cannot safely
scale. `clone-ll-random-pointer` has no such problem (its `next` chain is always a plain
sequential list; only the random pointer varies), so it reads a straightforward `INT_GRID`
row per node (`[value, randomIndex]`) and the growth test exercises it normally. Both
tracers' canvas edges are new: `LinkedListCanvas` resolves `childId`/`randomId` by node id
rather than array position, drawing a dashed violet line for a child pointer and a dotted
orange line for a random pointer, distinct from the existing solid "next" arrow.

`implement-trie` and `word-break-trie` are the first two `Trie`-canvas traces, following
RCA-012's fix: `implement-trie` ports the legacy character-by-character insert (create a
child, or traverse into an existing one, then mark the final node as an end of word)
across a list of words rather than one fixed call, so the shared-prefix branching the
problem is about is visible; `word-break-trie` is a full rewrite that walks a real trie
alongside the segment-DP array, stopping the moment the trie has no edge for the next
character and setting `dp[j+1]` the moment it reaches an end-of-word node.

The first six **Bit Manipulation** traces are also the first to use `DsType.BITS`:
`StepEmitter.bits(value, primaryBit, secondaryBit)` renders a fixed 32-wide MSB-to-LSB
track over the existing `ArrayCanvas`, rather than a new canvas. `check-power-of-2` and
`count-set-bits` are single-number bit tricks (`N & (N-1)` clears the rightmost set bit
either to test for a lone one or to count them one Brian Kernighan pass at a time);
`xor-numbers-in-range` computes `XOR(L..R)` in O(1) via the `f(n) = XOR(0..n)`
pattern-of-4 closed form rather than looping the range; `pow-x-n-math` is binary
exponentiation, folding the base into the result only on the exponent's odd (bit-set)
steps — restricted to a non-negative integer exponent since the tracer contract has no
floating-point `FieldType`. `single-number-1` and `single-number-3` trace as `Array`
instead: the first is the same XOR-cancellation idea as `single-number` (a distinct
catalogue id owned by `BitManipulationService` rather than `ArrayService`, so its default
input had to differ or the two traces would fingerprint identically); the second finds
*two* uniques by XOR-ing everything, isolating their one differing bit, and partitioning
the array into two buckets on it. Tracing these four to `Bits` surfaced a metadata gap in
`BitManipulationService`'s bulk registration — every id had hardcoded `dsType: "Array"`
regardless of what it actually renders, the same category of gap `TreeService.bulkDsType`
and `DpService.bulkDsType` had before their own batches — fixed alongside by giving the
service its own `bulkDsType(id)` allowlist.

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
- **`ProblemProviderContractTest`** is parameterized over all eighteen catalogue providers
  rather than testing one by hand — hand-testing one is what let three copy-pasted
  controller variants diverge, with eight silently dropping their 404 guard. It replaced
  eighteen copy-pasted `*ServiceTest` classes, each of which carried a hand-maintained list
  of retired ids; drift in those lists is what hid the last live fallbacks.
- **`designTokens.test.js`** fails the build on any unresolvable CSS `var()`. Fifteen
  custom properties were once deleted while five components still referenced them, and
  nothing noticed.
- **`InputValidatorTest`** covers every field kind and rejection path, since that
  validator is the only trust boundary between a request body and a running algorithm.

---

## Documentation

| File | What it is |
| :--- | :--- |
| `ARCHITECTURE.md` | The system as it stands, with diagrams. Start here. |
| `plan.md` | The v2 tracing architecture. Accurate; the source of the current design. |
| `AUDIT.md` | Full per-problem audit of the catalogue, with findings fixed and the two left open for an owner decision. |
| `REVIEW.md` | Six review gates every change goes through, each built from a failure this codebase has actually had. |
| `references.md` | UI/UX research and the design-token system. |
| `PROJECT_CONTEXT.md` | Pedagogical principles behind the visualizations. |
| `RCA.md` | Root causes, resolutions, open debt, and the regression guard for each recurring incident. |
| `PROMPT-E-canvases.md`, `PROMPT-F-visual-fidelity.md`, `PROMPT-J-full-roadmap.md` | Historical implementation prompts, kept for design rationale rather than as a live worklist — see each file's status header for what has since shipped. |
