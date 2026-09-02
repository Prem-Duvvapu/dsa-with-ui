# PROMPT F — Visual fidelity: companion panes, recurrence provenance, shape-driven layout

> **Read `PROMPT-E-canvases.md` first.** This file continues it and *corrects two of its
> design decisions*. Where the two disagree, F wins.
>
> **Status when this was written — 2026-09-02, `main` at `bb73df8`.** Verified against a
> running backend, not read from a document. 433 catalogued / 39 traced / 394 untraced.
> PROMPT E phases 0, 1 and 2 have landed; DP is an intentional partial delivery of phases
> 3/4. The mechanical trace sweep
> (`.claude/skills/review-trace-simulation/check_trace.py`) reports **35 of 39 tracers
> clean** — this codebase's tracer discipline is good. The gap this prompt addresses is
> *coverage and visual fidelity*, not craft.
>
> **Slice F1 — landed same day.** Companion panes (D1) built and wired: `bfs-traversal`
> and `dijkstra-min-heap` now emit `.queue(...)` every step and render a Queue companion
> pane beside the Graph hero, verified against a live backend and screenshotted mid-run
> (see `RCA-016`). `bfs-traversal`'s dsType moved `Queue` → `Graph`. No `StackCanvas` was
> built — no tracer feeds one yet, and building it unfed would repeat the exact mistake
> this prompt names in PROMPT E's phase 2. F2–F10 have not started.

---

## SHARED CONTEXT

```
Repo: dsa-with-ui.  Backend: Spring Boot 3.2.3 / Java 17, port 8923.
Frontend: React 18 + Vite, dev 5180, docker 5174.
Build:  cd backend && mvn test
        cd frontend && npm ci && npx vitest run
        cd frontend && npx vite build

Branch discipline: NEVER commit or push on `main`. Cut a working branch from main.
Publishing (commit / push / PR create / PR merge) requires the owner's approval.
Read-only git is free.

BACKGROUND — why this codebase is paranoid
An audit found 433 catalogued problems producing only 137 distinct visualizations: 303 ids
returned a DIFFERENT algorithm's animation. The old suite could not detect it because its
only per-problem assertion was that the step list was non-empty. Every rule below exists
because of that.

NON-NEGOTIABLE WORKING RULES
 1. Never make one problem show another problem's data. No fallbacks that substitute a
    different structure's payload. An unknown dsType renders an explicit unsupported state.
 2. Every fix gets a test, and you MUST verify the test FAILS against the broken code
    before accepting it. Revert the fix, watch it go red, restore. A green test that would
    not have caught the bug is worth nothing here.
 3. Do not weaken or delete an existing assertion to make something pass.
 4. Report honestly. If a slice is partly done, say which part.
 5. Both suites green before you call anything finished.
 6. No new npm dependencies without asking. Draw with SVG/`<canvas>`.
 7. Read `RCA.md` before touching tracing or visualization, and add an entry for any new
    class of defect you fix.
```

---

## WHAT THIS PROMPT CORRECTS ABOUT PROMPT E

**1. E's phasing was wrong. Slice vertically, not horizontally.**
E put *all* canvases in phase 3 and *all* tracer retags in phase 4. The consequence is
visible on `main` today: phase 2 shipped a complete emitter surface — `StepEmitter` now
offers `stack`, `queue`, `bits`, `chars`, `trie`, `dpTable`, `graph` — and **no tracer
calls any of them.** A queue payload renders nothing until a QueueCanvas exists, and a
QueueCanvas shows nothing until a tracer emits a queue. Neither half is visible alone.

**From here on, one dsType per PR: canvas + tracer retag + emitted payload together**, so
every PR makes something visibly correct on screen.

**2. E missed that a step can carry several structures at once.**
`ExecutionStep` now has 17 independently-nullable fields. A single step can carry a graph
*and* a queue *and* a distance array. E's "one dsType → one canvas" model cannot express
that, and it is the reason for the defects in the next section. See Design D1.

**3. E's taxonomy is still missing `INTERVAL`.** See Design D7.

---

## VERIFIED FINDINGS (2026-09-02, against a running backend)

### Finding 1 — The narration describes structures the canvas never draws

This is the sharpest statement of "the UI doesn't match the topic". All four confirmed by
reading the live trace payloads:

| Problem | What every step *says* | What the payload carries |
| --- | --- | --- |
| `bfs-traversal` | "Seed the queue", "Dequeue 0", "enqueue it behind nothing" | graph nodes/edges/states — **no queue**. `Queue` routes to `GraphCanvas` |
| `dijkstra-min-heap` | "Pop the smallest entry in the queue" | **no heap payload at all** |
| `longest-subarray-sum-k-positives` | "Expand window right… shrink window from left" | plain `arrayState`, `dsType: Array` |
| `single-number` | correct XOR narration | only the input array — the bit pattern is never shipped |

`dijkstra-min-heap` is the clearest: the min-heap is in the problem's *name*, and the
animation is visually identical to plain BFS.

`longest-subarray-sum-k-positives` is worse than a routing problem. Its `arrayState` uses
only `current` / `target` / `default`, so **window membership is not in the data** — the
interior of the window is indistinguishable from outside it. A `WindowCanvas` alone could
not fix this; the tracer must emit the window too. This is exactly why slices must be
vertical.

### Finding 2 — Coverage

- **20 of 39** traced problems still emit `ARRAY`.
- `frontend/src/canvas/registry.js` maps **six dsTypes to `ArrayCanvas`**: `Window`,
  `SearchSpace`, `String`, `Bits`, `Stack`, `PriorityQueue`.
- **No tracer calls** `.stack()`, `.queue()`, `.bits()`, `.chars()` or `.trie()`.

### Finding 3 — Two real pedagogy defects in search defaults

From the mechanical sweep. Two of the four flagged tracers are benign — `two-sum`'s `none`
and `linear-search`'s `not-found` are return-empty paths, legitimately unreachable when the
default input has a solution. Document those and move on. The other two are real:

- **`binary-search-1d`** — dead `left` anchor: `else high = mid - 1` never executes on the
  default input. **The learner never sees binary search discard the upper half**, which is
  the entire idea of the algorithm.
- **`search-rotated-sorted`** — dead `rightSorted`: half the algorithm's defining branch
  (deciding which side is sorted) never runs.

Per `HANDOFF.md` PROMPT C and the `review-trace-simulation` skill: a dead anchor is usually
a symptom of a badly chosen default, not a missing `emit` call. **Fix the default, do not
delete the anchor** — deleting it is weakening an assertion.

### Finding 4 — What is already good, and must not regress

- **DP is excellent.** `climbing-stairs` cell states span `void` / `known` / **`read`** /
  `probe` / `resolved`, so recurrence provenance is genuinely implemented, and the
  narration meets the repo standard: *"To stand on stair 3 you arrived either with a
  1-step from stair 2 (which holds 2) or a 2-step from stair 1…"*
- **Tree traversals animate.** The Phase 1 fix works — node state progresses
  `visiting → visited` per step, driven from trace topology rather than static metadata.
- 35 of 39 tracers have zero dead anchors.

---

## DESIGN

### D1 — Primary structure + companion panes  ← the central change

`dsType` selects the **hero** canvas. Any *other* structure present on the same step
renders as a **companion pane** beside or beneath it. This needs **no new dsTypes and no
wire changes** — the fields already exist and are already populated on some steps.

| Problem | Hero | Companion | What it fixes |
| --- | --- | --- | --- |
| `bfs-traversal` | Graph | Queue | "Dequeue 0" gets a queue to point at |
| `dijkstra-min-heap` | Graph | Heap + dist table | the min-heap stops being invisible |
| monotonic stack (future) | Array | Stack | the stack *is* the algorithm |
| any recursive tracer | Tree | Call stack | `callStack` is populated and nothing renders it |
| `lis-binary-search` | DP table | Input array | shows which element drove each update |

Rules:
- The hero is always `dsType`. Companions are derived from which payload fields are
  non-empty — never from the problem title or category (that is the string-sniffing the
  codebase has been removing).
- A companion pane is **collapsible** and never steals space from the hero on narrow
  viewports; below the mobile breakpoint companions stack under the hero.
- A structure already drawn as the hero is never repeated as a companion.
- Companions obey the same Bench state vocabulary and `CanvasShell` chrome rules.

### D2 — DP layout follows the payload's rank, not new enum values

**Do not add `DP_1D` / `DP_2D` / `DP_3D` to `DsType`.** That road does not stop — it leads
to `GRAPH_WEIGHTED`, `TREE_BST`, `LIST_CYCLIC` and a combinatorial enum that every tracer
author must classify into correctly. One `DpTableCanvas`, three layout modes chosen from
the data:

- **1-D** (`rowLabels.size() == 1`) — a linear strip with provenance **arcs** reaching back
  to `dp[i-1]` / `dp[i-2]`. Today `climbing-stairs` ships `rowLabels: ['ways to reach']`
  and renders as a degenerate one-row grid; a strip reads far better.
- **2-D** — a matrix with the three-source **fan** (up / left / diagonal) drawn into the
  probe cell. That fan is the entire lesson in edit distance, LCS and knapsack.
- **3-D** — **the one place a wire change is unavoidable.** `DpTable` is today
  `record DpTable(List<String> rowLabels, List<String> colLabels, cells)` — strictly two
  dimensional, with no layer concept. Add a layer axis (`layers`, plus an axis label) and
  render small multiples when there are few layers, a slider when there are many. Needed by
  stock-with-k-transactions, bitmask+index DP, and similar.

### D3 — The recurrence, substituted  ← highest teaching value per unit of effort

Above the table, render the recurrence symbolically, and beneath it the **live
substitution** for the current step:

```
dp[i] = max(dp[i-1], dp[i-2] + nums[i])
dp[4] = max(5, 3 + 2) = 5
```

This binds code ↔ formula ↔ table in one glance. It is nearly free: the tracer already
computes those numbers — they are inside the narration strings today. Carry them as
structured data (a recurrence template on the `InputSpec` or tracer, plus per-step
substitution values) rather than parsing them back out of prose.

### D4 — Provenance arrows

The `read` state exists but renders as a passive ring. Draw the actual arrows from the
source cells into the probe cell. Applies to DP tables (D2) and to any structure where one
cell's value is computed from named others.

### D5 — A per-topic invariant strip

Each algorithm family has one sentence worth showing continuously, flashing when it is
restored or violated:

- binary search — `answer ∈ [lo, hi]`, with the interval visibly shrinking
- sliding window — `sum ≤ K`
- heap — `parent ≤ children`
- DP — `every cell < i is final`
- DSU — `each set has one representative`

No current UI element carries this, and it is how these algorithms are actually taught.
Source the text from the tracer so it is per-problem, not per-category.

### D6 — Variants, not new dsTypes

Within one dsType, let the renderer pick a variant from payload characteristics:

- `SEARCH_SPACE` — over an array (indices) vs over an answer range (a number line, which
  cannot be enumerated as boxes). Both are required; see PROMPT E.
- `TREE` — BST (show the ordering constraint and the search path) vs generic binary tree.
- `GRAPH` — weighted / directed variants, chosen by whether weights and direction are present.
- `LINKED_LIST` — cyclic (Floyd's needs the loop drawn, with two pointers moving at
  different speeds) vs straight.

`WINDOW`, `SEARCH_SPACE` and `BITS` are all "an array plus a distinguished region" and
should **share one renderer with a variant flag**, differing in what the region means.
Building three bespoke components triples the surface area for the same teaching value.

### D7 — Add `INTERVAL` to `DsType`

`n-meetings-in-one-room` draws end times as array boxes `[2, 4, 6, 7]` while narrating
"Meeting #1 runs 1→2". Greedy interval problems (meeting rooms, merge intervals, insert
interval, minimum platforms) need a **timeline with spans**, not an array of scalars.
PROMPT E's taxonomy missed this. Add `INTERVAL` with a wire value of `"Interval"` and an
`IntervalCanvas`.

---

## SLICES — one PR each, each visibly correct on screen

Ordered so the earliest PRs fix the confirmed defects in Finding 1.

| # | Slice | Delivers |
| --- | --- | --- |
| **F1** | **Companion panes (D1)** | The shell change plus Queue and Stack companion renderers. Retag/emit `bfs-traversal`'s queue and `dijkstra-min-heap`'s heap in the same PR. Fixes two of the four Finding-1 defects immediately. |
| **F2** | **Recurrence substitution + provenance arrows (D3, D4)** | Applied to the eight existing `DP_TABLE` tracers. No new canvas. |
| **F3** | **DP rank-driven layout (D2, 1-D and 2-D)** | `climbing-stairs` / `house-robber-2` / `frog-jump` as strips; the 2-D fan for the matrix tracers. |
| **F4** | **`SEARCH_SPACE` (D6)** | Canvas with both variants, retag `binary-search-1d` and `search-rotated-sorted`, **and fix their defaults so `left`, `rightSorted` and `miss` are exercised** (Finding 3). |
| **F5** | **`WINDOW`** | Shared renderer with F4's. Emit real window membership states from both window tracers — the data does not exist today. |
| **F6** | **`BITS`** | Shared renderer again. Retag `single-number`; emit the running XOR's bit pattern. |
| **F7** | **`INTERVAL` (D7)** | Timeline canvas; retag `n-meetings-in-one-room`. |
| **F8** | **3-D DP layers (D2)** | The `DpTable` wire change plus small-multiples / slider. |
| **F9** | **Invariant strip (D5)** | Across every canvas shipped above. |
| **F10** | **Remaining canvases** | `STRING`, `TRIE` activation, `HEAP` standalone, `DSU`, `RECURSION_TREE` polish. |

`CaptureStrip` row-meaning following `dsType` (PROMPT E phase 5) stays open and can land
alongside whichever slice first needs it.

---

## VERIFICATION

Per slice:

```bash
cd backend  && mvn test          # green; count must not fall
cd frontend && npx vitest run    # green
cd frontend && npx vite build    # clean
```

Then, against a running backend (`cd backend && mvn spring-boot:run`):

```bash
python3 .claude/skills/review-trace-simulation/check_trace.py <problem-id>
```

- **No slice may reduce the 35/39 clean-anchor baseline.** F4 must *raise* it.
- Any wire-format change regenerates all golden traces
  (`mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true`). **Read the diff and say
  what changed** — regenerating without reading records a bug as expected.
- `designTokens.test.js` fails the build on any `var()` or className `index.css` does not
  define, and it reads JSX as text — write class names out in full, never interpolated.
- The cross-tier `frontend/src/canvas/registry.test.js` fixture must cover every new
  `DsType` value. Prove it goes RED for a value with no canvas before accepting it.
- For every retagged tracer, confirm by hand that the narration and the picture now agree —
  that is the whole point of this prompt, and no automated test asserts it.

Report the before/after `dsType` distribution and the clean-anchor count with each slice.
