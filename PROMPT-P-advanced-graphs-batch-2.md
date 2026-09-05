# PROMPT-P — Advanced Graphs, second pass (batch 9 of the full roadmap)

Status: plan only, no implementation yet. This is batch 9 of `PROMPT-J-full-roadmap.md`.

## Scope

| Difficulty | Id | Title |
|---|---|---|
| Medium | `bellman-ford` | Bellman Ford Algorithm |
| Hard | `kosaraju-scc` | Kosaraju's Algorithm (SCC) |

Per `PROMPT-J-full-roadmap.md`'s finding #4, this batch's original scope split into a safer
pair and a design-pass-needed pair. This doc covers only the safer pair. `word-ladder-1` and
`alien-dictionary` are **deferred** — see below — leaving batch 9 as a single PR of two ids
rather than the usual two-PR shape, the same reduction pattern batch 6 (Stack & Queue) used
for `lru-cache` and batch 7 (Linked List) used for `flattening-ll`/`clone-ll-random-pointer`.

Both live in `AdvancedGraphService.java`. `bulkDsType(id, category)` already returns
`DsType.GRAPH` for both (confirmed by reading the method directly — its default case is
`DsType.GRAPH` for everything not Strings-prefixed, `disjoint-set-dsu`, or
`number-of-islands`), so no catalogue metadata fix is needed. `DsType.GRAPH` is already
proven end-to-end by `bfs-traversal`, `dfs-traversal`, and `dijkstra-min-heap` — the first
tracer in *this session's own batches* to use it, not the first ever, so this pair carries
no canvas or wire-format risk.

## Current bug

Both are one-line fallback delegates to `generateGraphIntroSteps()` — confirmed by reading
the switch statement and generator methods directly:

```java
private List<ExecutionStep> generateBellmanFordSteps() { return generateGraphIntroSteps(); }
private List<ExecutionStep> generateKosarajuSteps() { return generateGraphIntroSteps(); }
```

`generateGraphIntroSteps()` is the same 2-step placeholder every other untraced id in this
60-problem category still falls back to.

## Why `word-ladder-1` and `alien-dictionary` are deferred, not included

Both ids' natural input is **a list of strings**, not a vertex/edge list:
`word-ladder-1` takes a word list plus a start/end word (the transformation graph between
words differing by one letter is *implicit*, built by the algorithm, never handed to it
directly); `alien-dictionary` takes a list of words already in the alien language's sorted
order (the character-precedence graph is derived from adjacent-word comparisons).

Confirmed by reading `FieldType.java` directly: no field kind exists today for "a list of
strings" — the seven declared kinds are `INT`, `INT_ARRAY`, `STRING` (one string, not a
list), `INT_GRID`, `GRAPH`, `LINKED_LIST`, `BINARY_TREE`. Also confirmed by searching the
frontend for any existing string-list input rendering: none exists. Adding this pair would
mean adding a new `FieldType`, threading it through `InputValidator`, and building a new
input editor on the frontend — new cross-stack contract work, not a routine tracer
addition, the same category of ask that got `lru-cache` (batch 6) and the Tries pair
(deferred pending `RCA-012`) held out of their routine batches rather than folded in.
Deferred here for the same reason, pending its own small prerequisite PR that defines the
new field kind end-to-end (backend validation + frontend editor) before any tracer tries to
use it.

## Tracer design

### `bellman-ford` (Medium)

- `InputSpec`: `GRAPH` field `"graph"` (`.directed()`, `.weighted()`, weights allowed
  negative — `.weights(-1000, 1000)`, unlike `dijkstra-min-heap`'s non-negative-only
  range, since tolerating negative edges is the entire reason this algorithm exists over
  Dijkstra — `.constraint("maxVertices", 12)`, `.constraint("maxEdges", 24)`), `INT` field
  `"start"` (source vertex).
- Relax every edge exactly `V - 1` times (guaranteed to have found every shortest path by
  then, assuming no negative cycle reachable from the source); then run one more full pass
  — if any edge can still be relaxed, a negative cycle exists and the result is undefined
  rather than a set of distances.
- Anchors: `init`, `relax` (an edge whose relaxation actually improved a distance),
  `noImprovement` (an edge checked but not relaxed — needed so the narration shows the
  algorithm actually checking, not just the successful updates), `roundComplete`,
  `negativeCycleDetected` (only reachable on an input with one), `done` (only reachable on
  an input without one).
- Default: a small DAG-shaped graph with at least one negative edge but no negative cycle,
  so the answer is a real, finite distance set. Alternate: a graph containing a negative
  cycle reachable from the source, so `negativeCycleDetected` is provably reachable and
  `done`'s "found distances" narration is the thing that differs, not just node count —
  both hand-verified against a Python simulation before writing any Java (Bellman-Ford's
  negative-cycle detection is exactly the kind of edge case worth confirming by hand, not
  assuming).

### `kosaraju-scc` (Hard)

- `InputSpec`: `GRAPH` field `"graph"` (`.directed()`, unweighted,
  `.constraint("maxVertices", 12)`, `.constraint("maxEdges", 24)`).
- Three phases, each worth its own narration: (1) a DFS over the original graph that
  pushes each vertex onto a stack *as it finishes* (all its descendants already pushed) —
  this finish-order is what makes phase 3 correct; (2) build the transpose graph (every
  edge reversed); (3) DFS the transpose, popping vertices off the finish-order stack one at
  a time, skipping already-visited ones — each DFS tree launched this way is exactly one
  strongly connected component, which is the fact this whole algorithm rests on and is
  worth stating plainly, not just performing.
- Anchors: `dfsVisit` (phase 1), `pushFinishOrder`, `buildTranspose`, `startNewComponent`
  (a fresh DFS launched from the stack in phase 3 — this is precisely one new SCC),
  `transposeDfsVisit`, `done`.
- Default: a graph with at least two genuinely separate SCCs (not every vertex mutually
  reachable, and not every vertex its own singleton SCC either) so `startNewComponent`
  fires more than once and the answer is a non-trivial partition. Alternate: a graph that
  is a single SCC (every vertex mutually reachable) — verified in Python first, since
  hand-tracing Kosaraju's on paper is exactly where an off-by-one in the finish-order stack
  direction quietly produces a plausible-looking but wrong partition.

## Sequencing

One PR — `feat/trace-advanced-graphs2` — implementing both. Retire the two
`AdvancedGraphService` cases via `LegacyTraceRetiredException`, update
`AdvancedGraphServiceTest`'s retired-id set (already exists, currently covers the four
Strings-batch ids — add these two), update `ApiContractTest`'s `RETIRED_IDS`/
`retiredTraces()` for `/api/graphs/advanced` (confirmed base path), regenerate golden files
and read the diffs, update README's traced list, run `mvn test` full suite, live-verify via
curl, push, open PR, poll CI, merge, sync, delete branch — the same workflow used for
every prior batch this session.

## Explicitly out of scope

- `word-ladder-1` / `alien-dictionary` — deferred, see above. A future prerequisite PR
  would need to: add a `FieldType` for a list of strings (name, wire shape, and
  `InputValidator` rules — likely length bounds on the list and on each string, alphabet
  constraints matching `RCA-013`'s ASCII-only lesson from the Strings batch), and a
  frontend input editor for it, before either tracer can be scoped with the same confidence
  as this pair.
