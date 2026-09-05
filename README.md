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

`aggressive-cows`, `alien-dictionary`, `asteroid-collision`,
`bellman-ford`,
`bfs-traversal`, `binary-search-1d`, `book-allocation`, `burst-balloons`,
`check-sorted-ii`, `combination-sum-i`, `correct-bst-swap`, `count-inversions`,
`count-square-submatrices`,
`climbing-stairs`, `dfs-traversal`, `dijkstra-min-heap`, `edit-distance`, `find-missing-number`,
`find-min-rotated-sorted`, `find-starting-point-loop`, `four-sum`,
`frog-jump`, `frog-jump-k-distance`, `grid-unique-paths`, `house-robber-2`,
`implement-trie`,
`kadane-algo`, `kmp-lps-algo`, `knapsack-01`, `koko-eating-bananas`, `kosaraju-scc`, `kth-element-2-sorted-arrays`, `largest-rectangle-histogram`, `lower-bound`, `minimum-falling-path-sum`,
`ninja-and-his-friends`, `ninjas-training`,
`largest-element`, `leaders-in-array`, `left-rotate-k`, `left-rotate-one`, `linear-search`,
`lis-binary-search`, `longest-happy-prefix`, `longest-increasing-subsequence`, `longest-subarray-sum-k-positives`,
`lru-cache`,
`majority-element`, `matrix-chain-multiplication`, `max-consecutive-ones`, `max-rectangle-area-all-ones`,
`max-sum-non-adjacent`, `median-2-sorted-arrays`, `merge-two-sorted-arrays`, `min-stack`, `morris-inorder`, `move-zeros-end`,
`n-meetings-in-one-room`, `n-queens`, `next-greater-element-2`, `next-permutation`, `number-of-islands`, `print-lis`,
`remove-duplicates-sorted`, `repeating-missing-number`,
`reverse-linked-list`, `reverse-ll-group-k`, `reverse-pairs`, `search-rotated-sorted`, `second-largest-element`,
`serialize-deserialize-bt`, `shortest-palindrome`,
`single-element-sorted`, `single-number`, `sliding-window-maximum`,
`sort-0-1-2`, `split-array-largest-sum`,
`stock-buy-sell`, `subsets-i`, `sudoku-solver`, `sum-subarray-minimums`, `three-sum`, `tree-burn-time`, `tree-inorder`, `tree-lca`, `tree-level-order`, `tree-max-path-sum`, `tree-postorder`, `tree-preorder`,
`trapping-rainwater`,
`triangle-min-path-sum`, `two-sum`, `unbounded-knapsack`, `unique-paths-2`, `upper-bound`,
`vertical-order-traversal`, `wildcard-matching`, `word-break-trie`, `word-ladder-1`,
`z-function-algo`, and `zigzag-traversal`.

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

`implement-trie` and `word-break-trie` are the first two `Trie`-canvas traces, following
RCA-012's fix: `implement-trie` ports the legacy character-by-character insert (create a
child, or traverse into an existing one, then mark the final node as an end of word)
across a list of words rather than one fixed call, so the shared-prefix branching the
problem is about is visible; `word-break-trie` is a full rewrite that walks a real trie
alongside the segment-DP array, stopping the moment the trie has no edge for the next
character and setting `dp[j+1]` the moment it reaches an end-of-word node.

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
