# PROMPT E — Per-topic visualizations: make each data structure draw as itself

> **Implementation status — 2026-08-31.** This file preserves the original verified
> diagnosis and six-phase specification; line references and baseline test counts below are
> historical. Phase 0 landed in PR #25, Phase 1 in PR #26, and Phase 2 is complete with one
> audited regeneration of all 34 golden traces. PR #27 delivered `DpTableCanvas` plus real,
> labelled tables for the three LIS tracers, so those are an intentional partial delivery of
> Phases 3/4. The remaining dedicated canvases and tracer retags have **not** started;
> `CaptureStrip` Phase 5 is also open except that DP tables intentionally omit the redundant
> strip. Trie transport is now carried end to end, but actual Trie activation remains blocked
> on the node-shape mismatch recorded in `RCA.md`. Documentation was refreshed early; that
> does not mean the sequential visualization roadmap is complete.

> **Superseded in part by `PROMPT-F-visual-fidelity.md` (2026-09-02).** F continues this
> file and corrects two of its decisions: phases must be sliced **vertically** (one dsType
> per PR — canvas, tracer retag and emitted payload together) rather than as one canvas
> phase followed by one retag phase; and a single `ExecutionStep` can carry several
> structures at once, so a canvas has a *hero* structure plus *companion panes*. F also
> adds the `INTERVAL` type this taxonomy missed. **Where E and F disagree, F wins.**

> Paste everything below the line into the implementing agent. It is written to be
> self-contained: it assumes no prior conversation and no access to this file's history.

---

## SHARED CONTEXT

```
Repo: dsa-with-ui.  Backend: Spring Boot 3.2.3 / Java 17, port 8923.
Frontend: React 18 + Vite, dev 5180, docker 5174.
Build:  cd backend && mvn test        (~401 tests, green today)
        cd frontend && npm ci && npx vitest run   (131 tests, green today)
        cd frontend && npx vite build

Branch discipline: NEVER commit or push on `main`. Cut a working branch from main.
Publishing (commit / push / PR create / PR merge) requires the owner's approval — do the
work, describe the change set, and let the owner decide when it lands. Read-only git is free.

Land this as a SEQUENCE OF SMALL PRs, one phase per PR, each independently reviewable and
each leaving both suites green. Do not ship all six phases as one branch.

BACKGROUND — why this codebase is paranoid
An audit found 433 catalogued problems producing only 137 distinct visualizations: 303 ids
returned a DIFFERENT algorithm's animation via one-line delegate generators and a
step-returning `default:` in each of 18 service switches. The old suite could not detect it
because its only per-problem assertion was that the step list was non-empty — which the
fallback guaranteed. Every rule below exists because of that.

NON-NEGOTIABLE WORKING RULES
 1. Never make one problem show another problem's data. No `default:` fallbacks that
    substitute a different structure's payload.
 2. Every fix gets a test, and you MUST verify the test FAILS against the broken code
    before accepting it. Temporarily revert the fix, watch it go red, restore it. A green
    test that would not have caught the bug is worth nothing here.
 3. Do not weaken or delete an existing assertion to make something pass.
 4. Report honestly. If a phase is partly done, say which part.
 5. Both suites green before you call anything finished.
 6. No new npm dependencies without asking. Draw with SVG/`<canvas>` as the existing
    canvases do.
```

---

## THE TASK

Every topic currently animates as an array of boxes. DP shows no table, linked lists show
no pointers, graphs show a stale diagram, stacks and queues show nothing at all. Make each
topic render as the structure it actually is: DP → tabulation table, linked list → node/
pointer chain, graph → node-link diagram driven by the real input, sliding window → a
window frame over the array, stack/queue → stack and queue diagrams, heap → array + tree
duality, trie → prefix tree.

**Do not start by writing canvases.** The symptom has four independent causes, three of
them in the backend and the data path. A frontend-only fix will produce prettier components
that still draw the wrong data.

---

## VERIFIED ROOT CAUSES

Each of these was confirmed by reading the code. File and line references are current.
Re-verify before you rely on any of them; do not take this document as authority over the
source.

### Cause 1 — Half the `dsType` vocabulary dead-ends in ArrayCanvas

`frontend/src/App.jsx:263-285`:

```js
switch (dsType) {
  case 'Tree':          return <TreeCanvas {...props} />;
  case 'LinkedList':    return <LinkedListCanvas {...props} />;
  case 'RecursionTree': return <RecursionTreeCanvas {...props} />;
  case 'Trie':          return <TrieCanvas {...props} />;
  case 'Graph':
  case 'Queue':         ... return <GraphCanvas {...props} />;
  case 'Stack':
  case 'PriorityQueue':
  case 'Array':
  case 'Matrix':
  default:              ... return <ArrayCanvas {...props} />;
}
```

`Stack` and `PriorityQueue` are *named cases that fall through to ArrayCanvas*. There is no
StackCanvas, QueueCanvas, HeapCanvas, DpTableCanvas or SlidingWindowCanvas anywhere in the
tree. `Queue` routes to GraphCanvas (correct for BFS's graph, but the queue itself is never
drawn). Selection is also polluted by two sniffs ahead of the switch: an `isDsu` check on
`problem.title`, and a `hasGrid` check on the payload.

### Cause 2 — Tracers emit a `dsType` too coarse to route on

Across the 34 tracers in `backend/src/main/java/com/dsa/ui/tracer/impl/`:
**22 emit `"Array"`**, 4 `"Matrix"`, 4 `"Tree"`, 2 `"Graph"`, 1 `"Queue"`, 1 `"LinkedList"`.

- DP tracers emit `"Matrix"` (`LongestIncreasingSubsequenceTracer`,
  `CountSquareSubmatricesTracer`, `MaxRectangleAreaTracer`) or `"Array"`
  (`LisBinarySearchTracer`, `PrintLisTracer`) — so a DP table renders as a raw int grid
  with no row/column headers and no way to show which cells the recurrence *read*.
- Sliding-window tracers (`LongestSubarraySumKPositivesTracer`, `MaxConsecutiveOnesTracer`)
  emit `"Array"` — no window frame.
- `dsType` is a free-form `String` on both `ExecutionStep` and `ProblemDetail`. Nothing
  validates it. A typo produces an array silently.

### Cause 3 — The wire format cannot express the missing structures

`backend/src/main/java/com/dsa/ui/tracer/StepEmitter.java` exposes exactly:
`using, at, push, pop, say, var, array(×3), arrayState, grid, list, tree, nodes, edges`.

There is **no way to emit stack contents, queue contents, a trie, a labelled DP table, or
window bounds.** Two specific defects:

**(a) The call stack and the queue/stack field are the same field.** `StepEmitter.step()`
builds the `ExecutionStep` passing `List.copyOf(callStack)` as the 4th constructor
argument, which `ExecutionStep`'s 13-arg constructor assigns to `queueOrStackState`. So
`emit.push()/pop()` (recursion frames) and a genuine stack's contents are physically the
same slot — a tracer cannot show both. This is user-visible: `MemoryComplexityCard.jsx:17`
reads `currentStep?.queueOrStackState` and labels it `dsElements`, so a recursive tree
traversal displays its call frames labelled as the data structure.

**(b) `trieState` is dead end-to-end.** `StepEmitter.step()` passes a literal `null` for
the `trieState` argument; `TraceEncoder.encode()`
(`backend/src/main/java/com/dsa/ui/tracer/wire/TraceEncoder.java:50-62`) does not include
it among the delta'd fields; `frontend/src/trace/decodeTrace.js`'s `CARRIED` list does not
list it. `TrieCanvas.jsx:115` reads `activeStep?.trieState || activeStep?.treeNodes`, so
the first operand is always `undefined`. The Trie path cannot work no matter what a tracer
does.

### Cause 4 — Tree and Graph draw STATIC catalogue metadata, not the trace

This is the most damaging one, and it is a correctness bug rather than a styling gap.

```js
// frontend/src/components/TreeCanvas.jsx:6-7
const treeNodes  = problem?.defaultTreeNodes || [];      // static catalogue metadata
const nodeStates = activeStep?.nodeStates || {};         // from the trace

// frontend/src/components/GraphCanvas.jsx:43-44
const nodes = problem?.defaultGraphNodes || [];          // static
const edges = problem?.defaultGraphEdges || [];          // static
```

Consequences, both confirmed:

1. **Tree traversals animate nothing.** `TreePreorderTracer` (and its inorder / postorder /
   level-order siblings) emit topology *and* per-node state via `.tree(tree.render(states))`
   and **never call `.nodes(...)`**. `TreeCanvas` ignores `activeStep.treeNodes` entirely
   and colours from `nodeStates`, which is therefore `{}` on every step. Every node renders
   in its default colour for the whole run.
2. **Editing the input does not change the picture.** The input panel is the project's
   headline feature. A user who edits the graph or tree gets a re-coloured drawing of the
   *old* default topology, with trace node-ids that may not correspond to the drawn nodes.

By contrast `ArrayCanvas.jsx:6-9`, `LinkedListCanvas.jsx:6` and `GridCanvas.jsx:14` all
correctly prefer the trace and fall back to defaults. Tree, Graph and Trie are the outliers.

---

## THE DESIGN

### Design rule 1 — `dsType` becomes a closed vocabulary with exactly one canvas each

Introduce a Java `enum DsType` and make it the only legal value. `TracerRegistry` already
fails application startup on a blank or duplicate `id()`; extend that posture — a tracer
emitting an unknown `dsType` must fail startup, not render an array.

| `dsType` | Canvas | Covers |
| --- | --- | --- |
| `ARRAY` | `ArrayCanvas` | arrays, sorting, two-pointer, basics |
| `WINDOW` | `SlidingWindowCanvas` | sliding window, variable-size window |
| `SEARCH_SPACE` | `SearchSpaceCanvas` | **binary search (32 problems)** |
| `MATRIX` | `GridCanvas` | grids, islands, matrix traversal |
| `DP_TABLE` | `DpTableCanvas` | all DP, 1-D and 2-D |
| `STRING` | `StringCanvas` | **strings (16 problems)** |
| `BITS` | `BitsCanvas` | **bit manipulation (18 problems)** |
| `TREE` | `TreeCanvas` | binary tree, BST |
| `GRAPH` | `GraphCanvas` | graphs, BFS/DFS, advanced graphs |
| `LINKED_LIST` | `LinkedListCanvas` | singly / doubly linked list |
| `STACK` | `StackCanvas` | stack, monotonic stack |
| `QUEUE` | `QueueCanvas` | queue, deque |
| `HEAP` | `HeapCanvas` | heaps, priority queue |
| `TRIE` | `TrieCanvas` | tries |
| `RECURSION_TREE` | `RecursionTreeCanvas` | recursion, backtracking |
| `DSU` | `DsuCanvas` | disjoint set union |

`DSU` becoming a real `dsType` deletes the `isDsu` title-sniff at `App.jsx:252-254`.
`WINDOW`, `SEARCH_SPACE`, `HEAP` and `BITS` are *rendering modes over `arrayState`* (see
design rule 3), not new payloads.

**Three of these need their pedagogy spelled out, because "it operates on an array" is
true of all three and is exactly the reasoning that produced the current bug.**

**`SEARCH_SPACE` — binary search.** An array of boxes with one highlighted index cannot
show the thing binary search *is*: half the search space being permanently discarded. The
canvas must draw `lo` / `mid` / `hi` and strike out the eliminated region so the span
visibly collapses. It has **two sub-shapes, and missing the second leaves roughly half the
category still wrong**:

  1. *Search over an array* — indices are the space; render the array with eliminated
     halves struck out.
  2. *Binary search on the answer* (Koko eating bananas, min days, ship capacity, split
     array largest sum) — the space is a **numeric range** like `[1, 10^9]`, not the input
     array, and cannot be enumerated as boxes. Render a **number line** with `lo`/`hi`
     converging and each probed candidate marked feasible / infeasible. This is what
     `HANDOFF.md`'s "one search space, many predicates" note refers to.

**`STRING`.** `ArrayElement` is `{int index, int value, String state}` — `value` is an
`int`, so characters cannot be represented at all today. This is the one place a new field
is unavoidable (see design rule 3). Split the category:

  - *Single-string* (palindrome, reverse, anagram, longest substring without repeating) →
    a character track with pointers; `STRING` plus the `label` field below.
  - *String DP* (edit distance, LCS, distinct subsequences) → **already covered by
    `DP_TABLE`**, using the two strings as `rowLabels` / `colLabels`. Do not build a
    separate canvas for these.
  - *Two-string pattern matching* (KMP, Rabin-Karp, Z-algorithm) → needs a two-track
    alignment view (text fixed, pattern shifting beneath) plus the LPS/failure table. This
    is a genuinely separate canvas and a second track in the payload. **Defer it to its own
    phase; do not block the other thirteen string problems on it.**

**`BITS`.** Set-bit counting and XOR tricks cannot be taught with array boxes — the lesson
lives in the binary expansion. Two shapes, both derivable:

  - *One integer* → `arrayState` where the index is the bit position (31…0), the value is
    0 or 1, and `label` carries the position. Highlight the bit under test; show masks and
    shifts as they move.
  - *Several integers* (XOR of a list, single-number, two-numbers) → **`DP_TABLE`**, rows
    being the operands plus the running result, columns the bit positions. This shows *why*
    XOR cancels pairs, which is the actual lesson. `SingleNumberTracer` currently emits
    `"Array"` over `nums`, so the running XOR's bit pattern — the entire point — is invisible.

Replace the `switch` in `renderCanvas()` with a single registry map in its own module:

```js
// frontend/src/canvas/registry.js
export const CANVAS_BY_DSTYPE = { ARRAY: ArrayCanvas, WINDOW: SlidingWindowCanvas, ... };
```

An unknown `dsType` must render an explicit "no visualization for <dsType>" empty state.
**It must never fall back to ArrayCanvas** — that is the same class of bug as the old
`default:` branch, in a new place.

### Design rule 2 — The trace is the source of truth for structure, always

Every canvas takes topology *and* state from the current step, falling back to the
problem's `default*` metadata only when the step carries nothing (the untraced/501 case).
Tree, Graph and Trie must be brought in line with Array/LinkedList/Grid.

For graphs this means the trace must carry topology. Today it carries only
`nodeStates: Map<Integer,String>` and `activeEdges: List<String>`. Add emitter support for
graph nodes and edges so a user-supplied graph draws as itself.

### Design rule 3 — Add as few wire fields as possible

Each new `ExecutionStep` field costs six edits (`ExecutionStep`, `StepEmitter`,
`TraceEncoder`, `StepEmitter.estimateBytes`, `decodeTrace.js` `CARRIED`, and a regeneration
of all 34 golden files). So derive wherever the data already exists:

| Structure | Approach | New field? |
| --- | --- | --- |
| Sliding window | Extend the `ArrayElement` state vocabulary (`window`, `lo`, `hi`) and let `SlidingWindowCanvas` read `arrayState` + `variables` | **no** |
| Binary search | Same trick — add `lo`, `mid`, `hi`, `eliminated` to the state vocabulary. The number-line variant carries probes as `arrayState` (index = probe order, value = candidate, state = feasible/infeasible) with `lo`/`hi` in `variables` | **no** |
| Bits | `arrayState` over bit positions for one integer; `dpTable` (rows = operands, cols = bit positions) for several | **no** |
| Heap | A heap *is* an array; `HeapCanvas` derives the tree from indices (`2i+1`, `2i+2`) over `arrayState` | **no** |
| Strings | **New optional `label` on `ArrayElement`** — `value` is an `int`, so characters are unrepresentable today. A nested field, not a new top-level step field, so it does not enlarge the delta encoder's field list — but `estimateBytes` (currently `arrayState.size() * 52L`) must account for it. Also improves heap and bit labels | yes |
| Trie | The `trieState` field already exists — fix the plumbing that drops it | **no** |
| Stack / Queue | Reuse `queueOrStackState`, freed by separating the call stack out | **no** |
| Call stack | **New `callStack` field** — separates recursion frames from real stack/queue contents | yes |
| DP table | **New `dpTable` field** — needs row/column labels and a per-cell state, which `gridState` (`int[][]`) cannot express | yes |
| Graph topology | **New `graphNodes` / `graphEdges` on the step** so topology follows the input | yes |

That is four new step fields plus one nested `ArrayElement.label`, added in **one**
wire-format PR, with **one** golden regeneration.

`dpTable` should carry: `rowLabels`, `colLabels`, and cells with a `state` drawn from the
Bench state vocabulary — critically including **`read`** (the hollow ring), which is what
lets a recurrence show the cells it depended on. This is the single highest-value piece of
the whole prompt: a DP table that shows `dp[i] = max(dp[i-1], dp[i-2]+n)` by ringing
`dp[i-1]` and `dp[i-2]` while filling `dp[i]` teaches the recurrence. A grid of numbers does not.

### Design rule 4 — Honour the Bench design system

Read `HANDOFF.md`'s PROMPT B for the full spec; the short version:

- **Five states, never colour alone**: `probe` (filled amber + ▼), `read` (hollow amber
  ring), `known` (neutral fill), `resolved` (filled green + ✓), `void` (dashed outline).
- Every colour resolves through a token in `index.css`. **No component may hardcode a
  colour.** `designTokens.test.js` statically fails the build on a `var()` or `className`
  that `index.css` does not define, and it reads JSX as text — so write class names out in
  full rather than interpolating them (`CanvasShell.jsx` explains why).
- New canvases plug into `<CanvasShell>` (`title` / `meta` / `legend` / `children` /
  `footer`); the shell owns the chrome and the legend. Do not draw your own.
- Respect `prefers-reduced-motion`. Motion is pedagogy: a pointer should travel, a swap
  should visibly swap.
- Both themes must work, and light is not an inversion of dark.

---

## PHASES — one PR each

### Phase 0 — Close the vocabulary and add the guards (no visual change)

1. Add `DsType` enum; use it on `ExecutionStep` and `ProblemDetail` (serialize to the same
   strings the API sends today, or migrate deliberately and update the frontend in the same PR).
2. Fail application startup on a tracer emitting an unknown `dsType`, in `TracerRegistry`,
   alongside the existing blank/duplicate-id guards.
3. `frontend/src/canvas/registry.js` — the `CANVAS_BY_DSTYPE` map. `renderCanvas()` becomes
   a lookup. Delete the `isDsu` title sniff and the pre-switch `hasGrid` special case.
4. **Cross-tier contract test**: every value the backend enum can emit has an entry in the
   frontend registry. Keep the list in one generated or checked-in fixture so the two tiers
   cannot drift.

*Prove it:* the registry test must go RED if you add an enum value without a canvas.

### Phase 1 — Make Tree and Graph draw the trace (the correctness fix)

1. `TreeCanvas` renders `activeStep.treeNodes` (which already carry `x`/`y` from
   `BinaryTreeLayout` and a `state`), falling back to `problem.defaultTreeNodes`.
2. `GraphCanvas` renders step-supplied topology, falling back to
   `problem.defaultGraphNodes/Edges`.
3. Add emitter support for graph topology and use it in `BfsTraversalTracer`,
   `DfsTraversalTracer`, `DijkstraTracer`.

*Prove it:* write the regression test with a fixture where the trace topology and the
default topology **genuinely differ** — the same discipline `HANDOFF.md` records for the
`App per-problem detail merge` test, where an earlier version passed only because both
endpoints returned the same object. Assert the rendered node set comes from the trace.
Then confirm by hand that a tree traversal now visibly changes colour step to step; today
it does not.

### Phase 2 — The wire-format PR (one golden regeneration)

1. Separate `callStack` from `queueOrStackState`. Point `MemoryComplexityCard.jsx:17` at
   the right one — it currently labels call frames as data-structure elements.
2. Fix `trieState`: emit it from `StepEmitter`, add it to `TraceEncoder`, add it to
   `decodeTrace.js`'s `CARRIED`, add it to `estimateBytes`.
3. Add `dpTable`, `graphNodes`, `graphEdges` through the same six places.
4. Add the optional `label` to `ArrayElement` and give it a cost in `estimateBytes`.
5. Add `emit.stack(...)`, `emit.queue(...)`, `emit.trie(...)`, `emit.dpTable(...)`,
   `emit.graph(...)`, `emit.chars(...)` and `emit.bits(...)` to `StepEmitter`.

**Three traps in this phase:**
- `StepEmitter.estimateBytes` has hand-calibrated per-field costs and
  `byteEstimateTracksActualPayload` fails if the estimate drifts from the measured payload.
  Every new field needs a cost.
- `TraceEncoderTest.decode()` and `frontend/src/trace/decodeTrace.js` are a matched pair,
  each written as the specification for the other. Change one, change both.
- Regenerating goldens (`mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true`)
  rewrites all 34 files. **Read the diff.** Per `CLAUDE.md`: regenerating without reading
  records a bug as expected. State in the PR what changed and why.

### Phase 3 — Build the canvases

`DpTableCanvas`, `SearchSpaceCanvas`, `StackCanvas`, `QueueCanvas`, `BitsCanvas`,
`StringCanvas`, `HeapCanvas`, `SlidingWindowCanvas`, and activate `TrieCanvas`. Each plugs
into `<CanvasShell>`, uses only tokens, and renders the five Bench states with glyphs. A
smoke-render test per canvas, plus a test that each handles an empty/missing payload
without throwing (`ErrorBoundary` exists because a canvas throw used to blank the whole
app — do not rely on it).

Priority order, by catalogued problems unblocked — note that binary search is the *second*
largest cluster in the whole catalogue and must not be left to last:

**DP table (55) → search space (32) → bits (18) → strings (16) → stack/queue (30) →
heap (17) → sliding window (12) → trie (3).**

`SearchSpaceCanvas` is two renderers behind one `dsType` (array span and number line) —
build both, or half of Binary Search still animates wrongly. The KMP-style two-track
alignment view is explicitly **out of scope for this phase**; see design rule 1.

### Phase 4 — Retag the 34 tracers and emit the new payloads

Move each tracer to its correct `dsType` and emit the structure it deserves: the five DP
tracers to `DP_TABLE` with a real recurrence-annotated table, the two window tracers to
`WINDOW`, `BinarySearch1DTracer` and `RotatedArraySearchTracer` to `SEARCH_SPACE`,
`SingleNumberTracer` to `BITS` (its running XOR bit pattern is the lesson and is invisible
today), `BfsTraversalTracer` to emit its queue alongside the graph, and so on. Group by
topic and do them in batches, keeping every commit green.

`FieldType.STRING` has **never been exercised by any tracer**, so the input panel's string
editor is untested against a real backend response. The first `STRING` tracer you land is
also the first test of that editor — expect to fix something there.

**`noTwoTracersProduceIdenticalTraces` and `traceRespondsToItsInput` will catch a lazy
copy-paste here — expect that and welcome it.** Also re-run the `review-trace-simulation`
skill's checks: a table that fills but never rings its `read` cells is a shallow trace that
passes the contract tests anyway.

### Phase 5 — CaptureStrip rows follow `dsType`

`CaptureStrip.jsx:17-51` derives rows by sniffing the payload in a fixed order
(`arrayState` → `treeNodes` → `listState` → `gridState` → `nodeStates`). A step carrying
none of them draws no strip. Make row *meaning* follow `dsType` — index / vertex / table
entry / list node / stack slot — while keeping **one** component. `HANDOFF.md` is explicit:
if the strip becomes one component per category, the Bench design has been lost.

### Phase 6 — Documentation

Update `README.md` (the traced list and any coverage claim), and `HANDOFF.md`'s PROMPT B
progress table. Do not hand-edit coverage numbers — read them from
`GET /api/problems/stats`.

---

## VERIFICATION

Per phase:

```bash
cd backend  && mvn test        # all green, count must not fall
cd frontend && npx vitest run  # all green
cd frontend && npx vite build  # clean
```

End to end, against a live backend (`cd backend && mvn spring-boot:run`, then
`cd frontend && npm run dev`):

- Open one problem per `dsType` and confirm it draws as that structure — not as an array.
- **Edit the input** on a tree problem and a graph problem and confirm the *drawn topology*
  changes, not just the colours. This is the Cause-4 regression and the reason this work exists.
- Step through a DP problem and confirm the table fills cell by cell with the recurrence's
  source cells ringed.
- Confirm a tree traversal visibly changes node state step to step.
- Check both themes, and at 320px width.
- Confirm no component hardcodes a colour.

Report the before/after `dsType` distribution across the 34 tracers, and say plainly which
phases are complete and which are not.
