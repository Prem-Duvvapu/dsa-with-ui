# PROMPT J: Full roadmap for the 39 candidate list (+ Tries)

**Status at time of writing:** planned, not yet implemented, except Strings (batch 3,
`PROMPT-I-strings-batch.md`, in progress — `feat/trace-strings-medium` has a first draft
tracer, not yet tested). `traced` is 76 of 433 and will be 80 once Strings ships. This
doc plans everything else from the 32-problem list and the 7-problem Strings/Tries
addendum given in conversation (39 total), plus the 2 Trie problems added on request —
41 ids in all, 4 of which (Strings) are already covered by `PROMPT-I` and not repeated
here.

**What this doc is, and isn't.** Batch-level plan docs so far (`PROMPT-G`, `PROMPT-H`,
`PROMPT-I`) scope one topic (or three) at a time, immediately before implementation, with
every id's current bug hand-verified by reading its actual generator method. This doc
does the same first-pass verification — **every one of the 41 ids below was traced to
its owning service and its exact current `case`/delegate/fallback via grep before being
placed in a batch**, the same discipline that caught the Strings batch actually living in
`AdvancedGraphService` rather than `StringService`. What this doc does *not* do is
hand-compute every algorithm's exact default/alternate output the way `PROMPT-I` did for
four problems — at 41 problems that depth of upfront verification is not a good use of
time before any of it is implemented. Each batch below gets that same hand-verification
treatment in its own short plan update immediately before its PR, exactly as `PROMPT-I`
did for Strings.

## The full list, categorized by current bug pattern

Three distinct bug shapes turned up, not one. Naming them because they need different
handling:

- **Fallback delegate** — a `case` (or `default:`) returns another problem's *unrelated*
  generator wholesale, e.g. `return generateGraphIntroSteps();`. The classic bug. Fix:
  write the real tracer, replace the delegate call with
  `throw new LegacyTraceRetiredException(...)`.
- **Hardcoded narration** — a real, multi-line generator method exists, but it is a fixed
  script for one hardcoded example (a literal array or string baked into the method),
  not driven by a reusable algorithm class. It shows genuinely correct steps for that one
  input and cannot answer any other. Functionally this is the same problem as a fallback
  delegate (no real algorithm to port, full rewrite needed) but it will not show up by
  grepping for one-line delegate methods — it has to be read to tell apart from the next
  category.
- **Real legacy algorithm** — a genuine algorithm class under `com.dsa.ui.algorithm.*`,
  invoked through the old `ListTraceRecorder`/`TraceEvent` mechanism (the `knapsack-01`,
  `n-queens`, `subsets-i` pattern from batch 2). Correct for any input; needs porting to
  the `AlgorithmTracer` contract, not rewriting from scratch.

| Id | Difficulty | Owning service | Current bug | dsType (existing canvas) |
|---|---|---|---|---|
| `three-sum` | Medium | `ArrayService` | Hardcoded narration | `ARRAY` |
| `four-sum` | Medium | `ArrayService` | Hardcoded narration | `ARRAY` |
| `set-matrix-zeroes` | Medium | `ArrayService` | Hardcoded narration | `MATRIX` |
| `spiral-matrix` | Medium | `ArrayService` | Hardcoded narration | `MATRIX` |
| `repeating-missing-number` | Hard | `ArrayService` | Hardcoded narration | `ARRAY` |
| `merge-two-sorted-arrays` | Hard | `ArrayService` | Hardcoded narration | `ARRAY` |
| `koko-eating-bananas` | Medium | `BinarySearchService` | Fallback delegate → `generateBs1dSteps()` | `SEARCH_SPACE`/`ARRAY` (verify) |
| `split-array-largest-sum` | Hard | `BinarySearchService` | Fallback delegate → `generateBs1dSteps()` | same |
| `painters-partition` | Hard | `BinarySearchService` | Fallback delegate → `generateBs1dSteps()` | same |
| `median-2-sorted-arrays` | Hard | `BinarySearchService` | Fallback delegate → `generateBs1dSteps()` | same |
| `kth-element-2-sorted-arrays` | Hard | `BinarySearchService` | Fallback delegate → `generateBs1dSteps()` | same |
| `min-stack` | Medium | `StackQueueService` | Fallback (`default:`) → `generateBalancedParenthesesSteps()` | `STACK` |
| `sum-subarray-minimums` | Medium | `StackQueueService` | Fallback (`default:`) → same | `STACK` |
| `remove-k-digits` | Medium | `StackQueueService` | Fallback (`default:`) → same | `STACK` |
| `celebrity-problem` | Medium | `StackQueueService` | Fallback (`default:`) → same | `STACK` |
| `sliding-window-maximum` | Hard | `StackQueueService` | Fallback (`default:`) → same | `STACK`/`QUEUE` |
| `lru-cache` | Hard | `StackQueueService` | Hardcoded narration, **and see risk note below** | `STACK`? (verify — see note) |
| `find-starting-point-loop` | Medium | `LinkedListService` | Fallback delegate → `generateReverseSteps()` | `LINKED_LIST` |
| `reverse-ll-group-k` | Hard | `LinkedListService` | Fallback delegate → `generateReverseSteps()` | `LINKED_LIST` |
| `flattening-ll` | Hard | `LinkedListService` | Fallback delegate → `generateReverseSteps()` | **blocked — see below** |
| `clone-ll-random-pointer` | Hard | `LinkedListService` | Fallback delegate → `generateReverseSteps()` | **blocked — see below** |
| `vertical-order-traversal` | Hard | `TreeService` | Fallback (`default:`) → `generatePreorderSteps()` | `TREE` |
| `tree-burn-time` | Hard | `TreeService` | Fallback delegate → `generatePreorderSteps()` | `TREE` |
| `morris-inorder` | Hard | `TreeService` | Fallback (`default:`) → `generatePreorderSteps()` | `TREE` |
| `correct-bst-swap` | Hard | `TreeService` | Fallback (`default:`) → `generatePreorderSteps()` | `TREE` |
| `word-ladder-1` | Hard | `AdvancedGraphService` | Fallback delegate → `generateGraphIntroSteps()` | `GRAPH` — **see input-shape note** |
| `alien-dictionary` | Hard | `AdvancedGraphService` | Fallback delegate → same | `GRAPH` — **see input-shape note** |
| `bellman-ford` | Medium | `AdvancedGraphService` | Fallback delegate → same | `GRAPH` |
| `kosaraju-scc` | Hard | `AdvancedGraphService` | Fallback delegate → same | `GRAPH` |
| `edit-distance` | Hard | `DpService` | Fallback (`default:`) → `generateClimbingStairsSteps()` | `DP_TABLE` |
| `wildcard-matching` | Hard | `DpService` | Fallback (`default:`) → same | `DP_TABLE` |
| `ninja-and-his-friends` | Hard | `DpService` | Fallback (`default:`) → same | `DP_TABLE`? — **see 3D note** |
| `kmp-lps-algo` | Medium | `AdvancedGraphService` | *(Strings batch — see `PROMPT-I`)* | `STRING` |
| `z-function-algo` | Medium | `AdvancedGraphService` | *(Strings batch)* | `STRING` |
| `longest-happy-prefix` | Hard | `AdvancedGraphService` | *(Strings batch)* | `STRING` |
| `shortest-palindrome` | Hard | `AdvancedGraphService` | *(Strings batch)* | `STRING` |
| `rabin-karp-algo` | Medium | `AdvancedGraphService` | Fallback delegate → `generateGraphIntroSteps()` | `STRING` — deferred, see `PROMPT-I` |
| `longest-palindromic-substring` | Medium | `StringService` | Hardcoded narration | `STRING`? (verify) — deferred, see `PROMPT-I` |
| `count-palindromic-subsequences` | Hard | `AdvancedGraphService` | Fallback delegate → same | `DP_TABLE`? — deferred, see `PROMPT-I` |
| `implement-trie` | Medium | `TrieService` | **Real legacy algorithm** (`ImplementTrie` + `ListTraceRecorder`) | `TRIE` — **blocked, see below** |
| `word-break-trie` | Medium | `TrieService` | Hardcoded narration | `TRIE` — **blocked, see below** |

## Three findings that change scope from the original 39-item list

### 1. Linked List: two of the four candidates are not buildable without a model change

Confirmed by reading `com.dsa.ui.model.ListNode` directly: it has `id`, `val`, `nextId`,
`prevId`, `state` — **no child pointer, no random pointer**. `flattening-ll` (LeetCode's
multilevel list needs a `child` pointer per node) and `clone-ll-random-pointer` (needs a
`random` pointer per node) cannot be modeled on the current `ListNode`. This is the exact
same finding `PROMPT-H` used to drop the entire Linked List topic from batch 2 — it still
holds, and still blocks these two specifically. `find-starting-point-loop` and
`reverse-ll-group-k` need only `next`/`prev` and are unaffected.

Adding `childId`/`randomId` to `ListNode` (plus the matching `FieldType`/`InputValidator`
support) is a real, separate task — a model change touching the wire contract other
tracers already depend on, not something to fold into a routine two-tracer PR. Left
explicitly out of scope here; Linked List batch below is two ids, not four.

### 2. `lru-cache` (and by the same logic, `word-ladder-1`/`alien-dictionary`'s *style* of
   input) needs a design decision before implementation, not just a tracer

`PROMPT-H` rejected the entire Heaps & PriorityQueue topic because its Hard candidates
(`merge-k-sorted-lists`, `design-twitter`, `median-data-stream`) are "a sequence of API
calls on a stateful object, not a single before/after trace" — the tracer contract's
`InputSpec` → single `run()` → single trace shape doesn't fit a `put`/`get`/`put`
sequence naturally. **`lru-cache` is the same shape of problem** (LeetCode presents it as
exactly that kind of operation sequence) and was included in the original list without
checking it against that already-established rejection reason. Two ways to make it fit,
to decide before this PR, not during it:
- Model the input as a fixed *list* of operations (`["put(1,1)", "put(2,2)", "get(1)",
  ...]`) as a `STRING` or a small custom encoding, and trace that whole sequence in one
  `run()` call — this keeps the one-input-one-trace shape, just with a richer input.
- Or accept it belongs with Heaps' rejected trio and drop it from this batch too.
This doc does not decide between them; the Stack & Queue batch below flags it as an open
question rather than assuming an answer.

### 3. `ninja-and-his-friends` is a 3D DP problem; the existing `DpTable` model is 2D

`DpTable` (`rowLabels`, `colLabels`, `cells: List<List<DpCell>>`) is inherently
two-dimensional. `ninja-and-his-friends` (two people moving through the same grid
simultaneously, state is `(row, col1, col2)`) does not fit a single 2D table. Before
scoping this one, decide whether to show one "slice" at a time (fix `row`, render a
`col1`-by-`col2` table, and let the row advance drive which slice is visible — a real
option, since the recurrence only depends on the previous row) or to treat this
differently entirely. Flagged as a design question for whoever picks up the DP batch
below, not assumed answered here.

### 4. Advanced Graphs: two candidates don't have a "graph" as their natural input

`word-ladder-1` (input: a word list + start/end words; the transformation graph is
*implicit*, built by the algorithm) and `alien-dictionary` (input: a list of words in
sorted order; the character-precedence graph is built from adjacent-word comparisons) are
not naturally `FieldType.GRAPH` inputs the way `bellman-ford`/`kosaraju-scc` are (a
literal vertex/edge list makes sense for those). This project has not yet defined an
InputSpec for "a list of strings that implies a graph" — `bellman-ford` and
`kosaraju-scc` are the safer, more directly-graph-shaped pair to attempt first in this
topic; `word-ladder-1`/`alien-dictionary` need their own small design pass on what the
`InputField` actually looks like before they can be scoped as confidently as everything
else in this doc.

## Batch sequencing (batch 3 = Strings, already planned)

### Batch 4 — Arrays
| Difficulty | Ids |
|---|---|
| Hard (2) | `repeating-missing-number`, `merge-two-sorted-arrays` |
| Medium (2) | `three-sum`, `four-sum` |

All four are hardcoded-narration bugs in `ArrayService` — straightforward rewrites, no
model or canvas risk (`ArrayCanvas` already handles everything these need).
`set-matrix-zeroes`/`spiral-matrix` (both Medium, both `MATRIX`-shaped) are good
candidates for a follow-up Arrays batch, not included here to keep this one at four.

### Batch 5 — Binary Search
Does not split cleanly 2 Hard + 2 Medium: the selected candidates are one Medium and four
Hard, and two of the four Hard ids are near-duplicate algorithms (`split-array-largest-sum`
and `painters-partition` are the identical "binary search on the answer, greedy
feasibility check" recurrence under two different cover stories). Proposed as two PRs:

1. `koko-eating-bananas` (Medium) + `split-array-largest-sum` (Hard) — pairing across
   difficulty deliberately, same reasoning as the Strings batch: the Medium teaches the
   binary-search-on-the-answer mechanism, the Hard reuses it with a harder feasibility
   check.
2. `median-2-sorted-arrays` + `kth-element-2-sorted-arrays` (Hard pair) — partition-based
   binary search across two arrays; `kth-element` is a direct generalization of `median`
   (median is just k = (n+m)/2), a natural "same idea, more general" pairing like batch
   2's DP interval pair.

`painters-partition` is recommended to skip or defer explicitly — tracing it right after
`split-array-largest-sum` would be a near-clone of the same tracer under a different
name, not new pedagogical content.

### Batch 6 — Stack & Queue
| Difficulty | Ids |
|---|---|
| Hard (2) | `sliding-window-maximum`, `lru-cache` *(pending the design decision above)* |
| Medium (2) | `min-stack`, `sum-subarray-minimums` |

`remove-k-digits`/`celebrity-problem` (both Medium) are good candidates for a follow-up.
If `lru-cache` is decided against (kept with the rejected stateful-sequence group), this
batch's Hard slot needs a substitute — `sliding-window-maximum` alone plus one of the
deferred Medium pair promoted, or drop to a Hard-plus-three-Medium shape like Binary
Search above.

### Batch 7 — Linked List (reduced to two ids, per finding #1)
| Difficulty | Ids |
|---|---|
| Medium (1) | `find-starting-point-loop` |
| Hard (1) | `reverse-ll-group-k` |

Not a 2+2 batch — only two of the original four candidates are buildable on the current
`ListNode` model. Ship both in one PR rather than force a second pair that doesn't exist
yet; revisit `flattening-ll`/`clone-ll-random-pointer` only after (and if) the model gets
a `childId`/`randomId` field, which is out of scope for this doc.

### Batch 8 — Binary Trees (second pass; batch 2 already traced the first four)
All four candidates are Hard — no Medium was identified in the original list for this
second Trees pass:
1. `tree-burn-time` + `vertical-order-traversal` — BFS-with-bookkeeping pair (a
   parent-map burn simulation; a column/row coordinate assignment with tie-breaking).
2. `morris-inorder` + `correct-bst-swap` — pointer-threading pair (Morris traversal's
   temporary links; an inorder walk that tracks two candidate swapped nodes).

All four currently resolve to `TreeService`'s `generatePreorderSteps()` fallback (three
via `default:`, one — `tree-burn-time` — via an explicit delegate case). `TreeCanvas`/
`BinaryTreeLayout` already handle everything these need; no new field type.

### Batch 9 — Advanced Graphs
Per finding #4, split into a safer pair and a design-pass-needed pair:
1. `bellman-ford` (Medium) + `kosaraju-scc` (Hard) — both take a literal vertex/edge
   list, the most direct `FieldType.GRAPH` fit. `bellman-ford` is relaxation repeated
   across rounds; `kosaraju-scc` is two DFS passes with a finish-order stack between
   them — different graph algorithm shapes, both real vertex/edge inputs.
2. `word-ladder-1` + `alien-dictionary` — both Hard, both need a design decision on what
   their `InputField` actually is (a word list, not a vertex/edge list) before they can
   be scoped with the same confidence as pair 1. Do pair 1 first; let it validate the
   `GRAPH` `FieldType` end-to-end (first tracer of this session to use it) before taking
   on the harder input-shape question in pair 2.

### Batch 10 — Dynamic Programming (second pass)
All three remaining Hard DP candidates, no Medium identified this round:
1. `edit-distance` + `wildcard-matching` — both classic 2D string-matching DP tables
   with a 3-or-4-way branch per cell; natural pair, same shape as batch 2's DP Hard pair.
2. `ninja-and-his-friends` — ships alone or paired with a DP Medium picked from outside
   this list, pending the 3D-table design decision in finding #3.

### Batch 11 — Tries (blocked on a prerequisite fix, not a normal batch)
`implement-trie` and `word-break-trie` cannot be scoped as a routine two-tracer PR the
way everything above can. `RCA-012` ("Trie canvas and backend node shapes do not yet
agree", status **Open — deferred to Phase 3**) documents that the frontend `TrieCanvas`
expects `char`/`isEnd`/child-id-array fields while the backend's trie serializer emits
`character`/`endOfWord`/char-to-id maps — a tracer written today passes every backend
test and renders broken in the actual UI. Proposed as two sequential PRs, neither bundled
with anything else:

1. **Prerequisite fix** — define one canonical JSON shape at the decoder/canvas
   boundary, add a backend-shaped trie fixture proving the exact shape round-trips, fix
   `TrieCanvas` to consume it. This is `RCA-012`'s own stated resolution, done as its own
   PR before any trie tracer lands.
2. **Trace both ids**, once the canvas is fixed. `implement-trie` already has a real
   legacy algorithm (`com.dsa.ui.algorithm.ImplementTrie` via `ListTraceRecorder`) — this
   is a port, not a rewrite, the `knapsack-01`/`n-queens` pattern. `word-break-trie` is a
   hardcoded-narration bug and needs a full rewrite.

## What every batch's own PR still needs to do (unchanged discipline)

Each batch above gets its own short plan update immediately before implementation —
mirroring what `PROMPT-I` did for Strings — covering the same things this doc deferred:
hand-verified default/alternate inputs against a known worked example (LeetCode's own
examples where available), the exact anchor set, and any tracer-specific risk (step
budget, byte-estimate, growability interaction) measured empirically rather than
predicted. Then the same checklist every batch has followed since `PROMPT-G`:

1. Write the tracer(s): real algorithm in `run()`, `// @a`-anchored `annotatedCode()`,
   materially different `alternateInput()`.
2. Retire the legacy delegate/fallback in the owning service.
3. Add the id(s) to that service's test's retired-id set (a new `Set` the first time any
   tracer lands in a given service, as batches 3's `AdvancedGraphServiceTest` and this
   doc's `TreeService`/`StackQueueService`/etc. second passes will each need).
4. `mvn test -Dtest=TracerContractTest,ApiContractTest,ProblemsApiTest,<ServiceTest>,CatalogTracerMetadataTest`
   green.
5. Full `mvn test` green.
6. `mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true`, `git status` to confirm
   only the new golden file(s) changed, read and hand-verify each.
7. Restart the backend cleanly (check for and kill any stale process on port 8923 first)
   and live-`curl` default + alternate for every id in the PR, plus the legacy execute
   path (expect 410).
8. Update the README "Traced so far" list, and `RCA.md`/`HANDOFF.md` wherever a batch
   resolves or partially addresses an open item there (this doc's Tries prerequisite fix
   should close or update `RCA-012`; the Strings batch already owes an `RCA-013` update
   per `PROMPT-I`).

If any id turns out, once actually opened, to already be traced, to be a duplicate id, or
to need something this doc didn't anticipate, that single problem gets dropped from its
batch with a note in its PR — not silently forced in, same rule as every batch since
`PROMPT-G`.
