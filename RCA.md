# Root-cause ledger

This file records defects that could recur across tracing, visualization, and local tooling.
It is an incident ledger, not a blame log. Before changing those areas, search this file for
the symptom and preserve the listed regression guard.

For a new incident, add: date discovered, status, symptom/impact, root cause, resolution,
and the test or operational check that prevents recurrence. Record the introducing commit
only when Git history proves it. Use **Open** when the safe fix belongs to a later scoped
phase; do not describe unfinished work as resolved.

## RCA-001 — Unknown work silently displayed another visualization

- **Discovered:** historical audit; guard completed 2026-08-29
- **Status:** Resolved
- **Symptom and impact:** hundreds of catalogue ids could return or render a different
  algorithm's trace. An unknown `dsType` also fell through to `ArrayCanvas`.
- **Root cause:** step-returning service `default:` branches, delegate generators, and a
  frontend default canvas optimized for non-empty output instead of truthful output.
- **Resolution:** unknown problem ids now return 404/501, `DsType` is a closed backend enum,
  startup validates tracer ids/types, and `CANVAS_BY_DSTYPE` renders an explicit unsupported
  state for an unknown type.
- **Regression guard:** `ApiContractTest`, `TracerContractTest`, `TracerRegistryTest`, and
  the cross-tier `frontend/src/canvas/registry.test.js` fixture. The registry test was proved RED
  with a temporary enum-only value before the value was removed.

## RCA-002 — LIS table was wrapped in duplicate canvas chrome

- **Discovered:** 2026-08-30, after `c15e142` (PR #27)
- **Status:** Resolved
- **Symptom and impact:** the DP table rendered a second header/legend inside the application
  shell, reducing the table's usable area. The execution capture also dominated the LIS view.
- **Root cause:** `DpTableCanvas` owned a `CanvasShell` even though `App` already supplied it;
  the capture strip was shown without considering the DP-table teaching goal.
- **Resolution:** the DP component renders only its stage, with one shell owned by `App`;
  `DpTable` traces omit the capture strip.
- **Regression guard:** `App.integration.test.jsx` asserts one shell header and one current-
  action region for LIS.

## RCA-003 — Malformed DP payloads were presented as real tables

- **Discovered:** 2026-08-31
- **Status:** Resolved
- **Symptom and impact:** labels-only, ragged, or non-cell-object payloads could fabricate a
  plausible-looking DP grid.
- **Root cause:** rendering validation checked presence rather than the complete rectangular
  cell contract.
- **Resolution:** `DpTableCanvas` accepts only a non-empty rectangular matrix of cell objects;
  invalid payloads render an explicit empty state.
- **Regression guard:** malformed-payload cases in `DpTableCanvas.test.jsx`.

## RCA-004 — Tracer and catalogue metadata disagreed

- **Discovered:** 2026-08-31
- **Status:** Resolved
- **Symptom and impact:** seven traced catalogue entries advertised a canvas type different
  from the tracer, and all three LIS entries advertised `Array` plus inaccurate complexity.
- **Root cause:** the catalogue and tracer registries evolved independently without a join
  assertion.
- **Resolution:** align the seven entries, mark the three LIS problems `DpTable`, and publish
  each LIS implementation's actual time/space complexity.
- **Regression guard:** `CatalogTracerMetadataTest` joins every live tracer to its winning
  catalogue entry and pins the three LIS complexity records.

## RCA-005 — Shell launcher failed on Linux/WSL

- **Discovered:** 2026-08-31, after `201f35b` (PR #28)
- **Status:** Resolved
- **Symptom and impact:** `bash start.sh` stopped at the function declaration with a syntax
  error.
- **Root cause:** the checked-in shell script used CRLF line endings.
- **Resolution:** normalize shell scripts to LF and enforce `*.sh text eol=lf`.
- **Regression guard:** `.gitattributes`, `bash -n`, and the CI startup smoke test.

## RCA-006 — Shell launcher left backend/frontend descendants running

- **Discovered:** 2026-08-31, after `201f35b` (PR #28)
- **Status:** Resolved
- **Symptom and impact:** stopping the launcher could terminate Maven/npm while leaving the
  Java or Vite child alive, causing occupied ports and confusing later starts.
- **Root cause:** cleanup targeted only the two immediate parent PIDs.
- **Resolution:** run each service in its own process group; terminate the groups, wait with a
  bounded grace period, then force only surviving groups.
- **Regression guard:** `start-smoke-test.sh` creates child process trees and asserts that
  both groups and the launcher exit after SIGTERM; CI runs it.

## RCA-007 — Duplicate catalogue ids reached React

- **Discovered:** 2026-08-31
- **Status:** Resolved
- **Symptom and impact:** the sidebar counted and rendered duplicate problem ids, producing a
  React duplicate-key warning and ambiguous selection.
- **Root cause:** a test named as a de-duplication check actually asserted the duplicated
  count, while `App` accepted the backend list verbatim.
- **Resolution:** keep the first valid catalogue entry per id and discard empty/duplicate ids
  at the UI boundary.
- **Regression guard:** the catalogue integration test expects 18 unique rows from a
  19-entry duplicate fixture and fails on the old React warning/count.

## RCA-008 — Recursive frames and algorithm queues shared one wire field

- **Discovered:** specified in `PROMPT-E-canvases.md`; completed 2026-08-31
- **Status:** Resolved
- **Symptom and impact:** recursive frames appeared as queue/stack contents, and a trace could
  not represent a call stack alongside the algorithm's own queue, stack, or heap.
- **Root cause:** both meanings were stored in `ExecutionStep.queueOrStackState`; the legacy
  `TraceEvent` contract carried the same ambiguity.
- **Resolution:** add independent `callStack` transport, named queue/stack emitter helpers,
  and a named legacy factory for data-structure state.
- **Regression guard:** `StructurePayloadWireTest`, `TraceEncoderTest`, decoder tests, and
  `ListTraceRecorderStateSeparationTest` cover recursion, BFS/rotting queues, and a heap.

## RCA-009 — Mutable structured input changed earlier snapshots

- **Discovered:** 2026-08-31 during Phase 2 review
- **Status:** Resolved
- **Symptom and impact:** mutating a trie or graph object after one step rewrote that earlier
  step. Conversely, rebuilding equal graph/trie content as new objects forced redundant
  deltas because model equality was identity-based.
- **Root cause:** emitter helpers copied only outer lists, retained mutable nodes/maps, and
  graph models had no content equality.
- **Resolution:** snapshot every trie/graph node and edge at emission time, freeze child maps,
  and give graph models content equality.
- **Regression guard:** `StepEmitterStructurePayloadTest` mutates source objects after emission
  and rebuilds equal graph content; `StructurePayloadWireTest` does the same equality check for
  trie content.

## RCA-010 — Fetch failures invented a successful trace

- **Discovered:** 2026-08-31
- **Status:** Resolved
- **Symptom and impact:** rejected, non-OK, empty, or malformed execution responses could be
  replaced with a one-step Array animation; controls then reported `1 of 1`, and unrelated
  sample narration, complexity, and LeetCode-3 Java code looked authoritative.
- **Root cause:** `useTrace` treated failed `Promise.allSettled` results as null and then
  constructed a generic step; dependent components also defaulted missing facts.
- **Resolution:** only checked-in offline problem steps may act as an offline trace. Network,
  empty, and malformed results stay explicit errors with zero steps; controls disable,
  narration stays neutral, unknown complexity is shown as unavailable, and missing code gets
  a code-unavailable state with no active-line badge.
- **Regression guard:** RED-first cases in `useTrace.test.js`, `App.test.jsx`,
  `App.integration.test.jsx`, `MemoryComplexityCard.test.jsx`, and `CodeViewer.test.jsx`.

## RCA-011 — Weighted-graph input allowed an invalid Dijkstra domain

- **Discovered:** 2026-08-31
- **Status:** Resolved
- **Symptom and impact:** Dijkstra accepted negative edge weights; in the undirected graph
  representation, a negative edge permits repeated lowering and can exhaust the trace budget
  or overflow. Separately, weight `0` was not drawn because the UI used a truthiness check.
- **Root cause:** graph-field validation had no weight bounds; it also narrowed arbitrary
  JSON numbers with `intValue()`, allowing fractional/out-of-range values to wrap or truncate.
  Rendering separately conflated zero with absence.
- **Resolution:** graph input fields can declare weight bounds, Dijkstra declares a
  non-negative range, validation parses integers exactly and reports out-of-range edges, and
  the canvas renders any non-null weight including zero.
- **Regression guard:** graph input-validator/Dijkstra rejection tests and the zero-weight
  `GraphCanvas` rendering test.

## RCA-012 — Trie canvas and backend node shapes do not yet agree

- **Discovered:** 2026-08-31
- **Status:** Resolved
- **Symptom and impact:** activating a real trie trace would not connect/render nodes
  correctly: the canvas expects `char`, `isEnd`, and child-id arrays, while the backend emits
  `character`, `endOfWord`, and character-to-id child maps.
- **Root cause:** the canvas was created before live trie transport was wired and has never
  been exercised by a tracer.
- **Resolution:** the backend's real `TrieNodeModel` serialization is now the one canonical
  shape at the decoder/canvas boundary: `{ id, character, endOfWord, x, y, children: {char:
  childId}, state }`. `TrieCanvas` derives its edge list from each node's `children` map
  (rather than an id array) and renders the backend-supplied `x`/`y` directly — the same
  convention `TreeCanvas` already uses for `treeNodes` — instead of computing its own
  recursive layout from a shape the backend never sent. The speculative `treeNodes`
  fallback path is removed; `trieState` is the only accepted input.
- **Regression guard:** `TrieCanvas.test.jsx` renders a fixture shaped exactly like the
  backend serializer (character/endOfWord/children-map), proves it round-trips through
  `decodeTrace`'s delta carry-forward, and covers the empty and absent-`trieState` cases.

## RCA-013 — New wire helpers still need activation conventions

- **Discovered:** 2026-08-31 Phase 2 audit
- **Status:** Open — resolve before the corresponding Phase 3/4 activation
- **Symptom and impact:** `chars` indexes Unicode code points while Java algorithms often hold
  UTF-16 offsets; `stack(Iterable)` accepts whatever iteration order a collection exposes
  while the UI labels the last item “Top.” Some direct legacy `ExecutionStep` producers also
  still place recursive frames in the historical queue field.
- **Root cause:** transport helpers were added before a live string/stack tracer fixed their
  caller conventions; compatibility services predate the separated `callStack` field.
- **Resolution required:** document/test code-point index conversion for real string tracers,
  define one bottom-to-top stack order, and migrate remaining direct legacy recursion producers
  before their responses are presented as separated memory state.
- **Regression guard required:** emoji-index and deque-order fixtures plus a scan/test covering
  direct `ExecutionStep` recursion producers.
- **Partial progress (2026-09-05):** `kmp-lps-algo` and `z-function-algo` are the first
  tracers to call `chars()`, confirming the helper's `arrayState` shape renders correctly
  through the existing `ArrayCanvas` with no frontend change needed. The codepoint/UTF-16
  divergence itself is still open — both tracers sidestep it rather than resolve it, by
  constraining their `STRING` `InputField`s to lowercase ASCII (`[a-z]+`), where every
  character is one code point and one UTF-16 unit by construction. The general case
  (astral characters / surrogate pairs) is untouched; do not read this as RCA-013 closed.

## RCA-014 — New compact tabs failed contrast in the dark theme

- **Discovered:** 2026-08-31
- **Status:** Resolved
- **Symptom and impact:** the small active Memory/Complexity tab label measured 3.77:1 against
  its violet background, below WCAG AA's 4.5:1 requirement for normal-sized text.
- **Root cause:** `--text-primary` was reused as an on-accent color without measuring that
  foreground/background pair.
- **Resolution:** introduce `--text-on-accent` and use the measured 4.62:1 pair.
- **Regression guard:** `designTokens.test.js` calculates the contrast in both theme token
  resolutions and fails below 4.5:1.

## RCA-015 — New payload byte costs undercounted escaped Unicode

- **Discovered:** 2026-08-31 Phase 2 audit
- **Status:** Resolved
- **Symptom and impact:** a synthetic label/trie/queue/call-stack step estimated 14,221 bytes
  for 23,812 serialized bytes, so the 2 MB budget could be exceeded before truncation.
- **Root cause:** string costs used UTF-16 length and did not conservatively charge JSON escaping
  and UTF-8 expansion; no registered tracer exercised the new helper mix yet.
- **Resolution:** charge the worst relevant JSON/UTF-8 representation for user-visible strings.
- **Regression guard:** `StepEmitterStructurePayloadTest` calibrates the estimate against a
  serialized payload containing emoji, control characters, labels, trie state, queue state,
  and call frames.

## RCA-016 — Narrated queue/heap had nowhere to render

- **Discovered:** 2026-09-02, PROMPT-F-visual-fidelity.md review against a running backend
- **Status:** Resolved (bfs-traversal, dijkstra-min-heap only — see below)
- **Symptom and impact:** bfs-traversal's steps said "Seed the queue", "Dequeue 0", "enqueue
  it behind nothing"; dijkstra-min-heap's said "Pop the smallest entry in the queue" — but
  neither step carried a queue payload, and both routed to `GraphCanvas`, which has no
  queue to draw. The min-heap in Dijkstra's own name was invisible; its animation was
  pixel-identical to plain BFS.
- **Root cause:** `AlgorithmTracer.dsType()` picks exactly one hero canvas per problem, but
  `ExecutionStep` can carry several populated structure fields on the same step (a graph
  AND a queue, here). The one-canvas-per-dsType frontend model had no way to render the
  second structure even once a tracer emitted it — and neither tracer was emitting it.
- **Resolution:** a companion-pane layer (`frontend/src/canvas/companions.js`,
  `.stage-with-companions`/`.canvas-hero`/`.companion-pane` in index.css) renders any OTHER
  populated structure beside the hero, derived from the payload alone — never from problem
  id or title. `BfsTraversalTracer` and `DijkstraTracer` now call `.queue(...)` every step;
  `bfs-traversal`'s dsType moved `Queue` → `Graph` since the graph topology is the point and
  the queue is auxiliary (catalogue metadata updated to match, see `CatalogTracerMetadataTest`).
  Companion **presence** is decided from the whole run (`allSteps.some(...)`), not the
  current step alone — bfs-traversal's queue is empty on its init and done steps and
  non-empty on nearly everything between, so a per-step presence check made the pane pop in
  and out on almost every click.
- **Regression guard:** `QueueCompanionTraceTest` (backend, proved RED against the tracers
  with `.queue(...)` reverted before the fix landed), `companions.test.js` and
  `QueueCanvas.test.jsx` (frontend). Golden files for both tracers regenerated and diffed —
  only `dsType` and `queueOrStackState` changed, nothing else.
- **Not yet resolved:** no other tracer emits `.stack()`, `.bits()`, `.chars()` or `.trie()`
  — those wire methods still sit unused, same as before this fix, just narrower now. See
  `PROMPT-F-visual-fidelity.md` slices F4–F7.

## RCA-017 — A child's layout effect ran before its parent's ref attached

- **Discovered:** 2026-09-03, building DP provenance arrows (PROMPT-F-visual-fidelity.md,
  design D4)
- **Status:** Resolved
- **Symptom and impact:** `DpTableCanvas`'s new provenance-arrow overlay never rendered.
  Debug logging showed its `useLayoutEffect` DID run on mount, but `wrapRef.current` — a
  ref owned by the parent, pointing at the parent's own `.dp-table-wrap` div — was `null`
  inside it, so the effect bailed out before ever attaching its `resize` listener. Every
  later recompute attempt was silently a no-op for the rest of the component's life.
- **Root cause:** React commits refs and layout effects bottom-up (children before
  parents). A component can read `ref.current` on its OWN DOM node reliably inside its own
  `useLayoutEffect`, but not on an ANCESTOR's ref passed down as a prop — the child's
  layout effect fires before the parent's own commit step, which is where the parent's ref
  actually gets attached. `ProvenanceArrows` held the `useLayoutEffect` while `DpTableCanvas`
  held the ref, so the guarantee didn't hold. No compiler or lint rule catches this; it only
  showed up as "the feature silently does nothing."
- **Resolution:** the ref, the `arrows`/`size` state, and the `useLayoutEffect` all moved
  into `DpTableCanvas`, the component that owns the DOM node. `ProvenanceArrows` became a
  pure presentational component taking `{ arrows, size }` as props — no ref, no effect.
  General rule for this codebase: a ref and the layout effect that reads it belong in the
  same component, never split across a parent/child boundary.
- **Regression guard:** `DpTableCanvas.test.jsx`'s `provenance arrows` block mocks
  `getBoundingClientRect` per cell (jsdom has no real layout engine) and asserts exact
  arrow endpoint coordinates — proved RED against the broken parent/child split before the
  fix landed, green after. A weaker test (just "an `.dp-arrow` element exists") would not
  have caught this, since the broken version rendered zero arrows outright rather than
  wrong ones — existence alone was already a meaningful assertion here, but the exact-
  coordinate version is what would catch a *regression* to the same bug later.

## RCA-018 — Guarding a null-safe byte estimator against null undercounted it

- **Discovered:** 2026-09-03, adding `DpTable.formula`/`substitution` (design D3) — caught
  by `TracerContractTest.byteEstimateTracksActualPayload` before it ever reached a commit.
- **Status:** Resolved
- **Symptom and impact:** `climbing-stairs` estimated 4759 bytes for a payload that
  actually serialised to 5307 (ratio 0.90, below the test's floor). `frog-jump` failed the
  same way. Both were **regressions on tracers the new fields didn't even touch yet** —
  the two new `DpTable` fields are null on every tracer except the six that later adopted
  D3, so this would have undercounted the byte budget on every `DP_TABLE` trace in the
  catalogue, not just the ones being changed.
- **Root cause:** `StepEmitter.estimateBytes` wrote
  `if (table.formula() != null) { bytes += jsonStringBytes(table.formula()) + 4L; }` — but
  `jsonStringBytes` already returns `4` (the length of the JSON `null` token) when passed
  `null`; it exists specifically to be called unconditionally. The `if` guard *skipped the
  call entirely* on the common (null) case, so the estimate charged nothing for a field
  Jackson still serialises as `"formula":null` on every step. Every other optional-field
  estimate in this method already calls `jsonStringBytes` unconditionally (see
  `getDescription()`); this one broke the pattern by re-deriving a null check the helper
  had already solved.
- **Resolution:** removed the guard; both lines now call `jsonStringBytes(...)` directly,
  matching the rest of the method. General rule: when adding an estimate for a new
  optional field, check whether `jsonStringBytes` (or an equivalent null-aware helper)
  already exists before writing a new null check around it — a second null check on top of
  one that already returns the right answer is the bug, not a safety margin.
- **Regression guard:** `TracerContractTest.byteEstimateTracksActualPayload`, which already
  existed for this exact purpose (see `RCA-015`) — it caught this on the very first test
  run after adding the fields, before any golden file was touched or any tracer adopted
  D3. No new test was needed; this entry exists so the next optional-field addition does
  not repeat the mistake the existing guard already knows how to catch.

## RCA-019 — Partition-search tracers can fail stepCountGrowsWithInput on a lucky default

- **Discovered:** 2026-09-05, tracing `median-2-sorted-arrays` and
  `kth-element-2-sorted-arrays` (Binary Search batch 5) — caught by
  `TracerContractTest.stepCountGrowsWithInput` before either tracer was committed.
- **Status:** Resolved (per-tracer default choice), pattern documented for future
  partition-search tracers.
- **Symptom and impact:** Both tracers' first-draft defaults (their own LeetCode examples)
  emitted *fewer* steps on `stepCountGrowsWithInput`'s auto-scaled "larger" input than on
  the plain default — the opposite of what the test requires. This is not the canned-
  narration bug the test exists to catch; both tracers already passed
  `traceRespondsToItsInput` and `noTwoTracersProduceIdenticalTraces` at the time.
- **Root cause:** every other `INT_ARRAY`-driven tracer that passes this test does real
  *per-element* work inside its search loop (`aggressive-cows`' `canPlace` scans every
  stall, `koko-eating-bananas`' feasibility check scans every pile), so a longer array
  directly means more steps per binary-search round. Partition-based binary search across
  two arrays has no such inner scan — each round does exactly four boundary lookups
  regardless of array length, so total steps are `O(log(min(n1,n2)))` rounds, not
  `O(n)`. `stepCountGrowsWithInput`'s auto-grower (`TracerContractTest.growList`) always
  prepends smaller values below the existing first element for a `.sorted()` field, and
  for a partition search that specific reshaping can converge in *fewer* rounds than the
  original values did — round count here depends on where the correct cut falls, not
  simply on array length, so growing the array is not guaranteed to add rounds the way it
  would for a per-element scan.
- **Resolution:** chose each tracer's default input by hand-simulating `growList`'s exact
  transformation in Python first and picking a default whose auto-grown counterpart
  provably takes more rounds (not just a larger array) — see
  `MedianTwoSortedArraysTracer`/`KthElementTwoSortedArraysTracer`'s defaults, verified
  against a Python port of `growList` before writing any Java. The chosen defaults no
  longer double as the cleanest LeetCode examples; a code comment was not enough context,
  so this entry carries the reasoning instead.
- **Regression guard:** `TracerContractTest.stepCountGrowsWithInput` already existed and
  caught this before either tracer was committed — no new test was needed. The guidance
  for the next partition-based tracer (e.g. `matrix-median`, still untraced): hand-simulate
  `growList`'s transformation against candidate defaults *before* committing to one, the
  same way anchor coverage is hand-verified — round count for this algorithm shape is a
  property of the specific values, not the array length alone.

### Recurrence, 2026-09-13 — `next-permutation`, and a case where the two goals are mutually exclusive

The Arrays audit found `next-permutation`'s default `[3,2,1]` was the *last* permutation: no
pivot exists, so the algorithm skipped its entire body — 4 steps and 4 of its 7 anchors dead.
Replacing it with `[1,3,5,4,2]` (7 steps, 0 dead anchors) failed `stepCountGrowsWithInput`
immediately: 7 steps grown, 7 steps at defaults.

Hand-simulating `growList` the way this entry prescribes explains why, and the answer is
stronger than "pick a better default":

| candidate | pivot? | scan steps, default → grown |
| --- | --- | --- |
| `[1,3,5,4,2]` | yes | 2 → 2 |
| `[1,5,4,3,2]` | yes | 3 → 3 |
| `[5,4,3,2,1]` | no | 4 → 9 |

The scan length is the length of the **trailing descending run**, and `growList`'s general
branch *prepends* filler at or above `maxValue`. Prepending above every existing value cannot
lengthen a trailing run that already terminates at a pivot, so for this algorithm shape
*every* default containing a pivot is flat under growth, and only a pivot-less one grows. The
two goals — exercise the whole algorithm, and grow with input — are not both reachable here.

- **Resolution:** kept a growable default but made it `[5,4,3,2,1]` rather than `[3,2,1]`, so
  the wrap-around case at least shows the scan running its full length (4 narrated steps
  instead of 2). `alternateInput()` carries `[2,3,1]`, which exercises the pivot, the swap
  and the suffix reverse; between the two, all 7 anchors are reached, which is what
  `anchorsAreAllReachable` checks. The reasoning lives in a comment at the `defaultValue`
  call, because the next person to read `[5,4,3,2,1]` will otherwise "fix" it.
- **Widened guidance:** hand-simulate before committing, as above — but also ask whether the
  step count is a function of input *length* at all. When it is a function of input *shape*
  (where a pivot, cut or breakpoint falls), no default satisfies both goals, and the honest
  move is a growable default plus an alternate that covers the rest, not a contorted default
  that games the ratio.

## RCA-020 — Adding nullable ListNode fields undercounted every LINKED_LIST tracer's byte estimate

- **Discovered:** 2026-09-05, adding `childId`/`randomId` to `ListNode` for `flattening-ll`
  and `clone-ll-random-pointer` — caught before commit by re-running
  `TracerContractTest.byteEstimateTracksActualPayload` across all `LINKED_LIST` tracers.
- **Status:** Resolved
- **Symptom and impact:** `StepEmitter.estimateBytes` charged a flat `88` bytes per
  `ListNode`, calibrated against the original five-field shape. Adding two more nullable
  `Integer` fields means Jackson now serialises `"childId":null,"randomId":null` on every
  step of every pre-existing `LINKED_LIST` tracer too, not just the two new ones — the same
  shape of bug as RCA-018 (a fixed per-element constant silently drifting low the moment a
  model field is added), just on the linked-list byte estimator instead of the DP one.
- **Root cause:** the estimator's per-node constant was never revisited when the model
  gained fields; nothing forces the two to change together.
- **Resolution:** bumped the per-node constant from 88 to 130, covering the worst case
  (both new fields absent, `,"childId":null,"randomId":null` ≈ 33 bytes) with margin.
- **Regression guard:** `TracerContractTest.byteEstimateTracksActualPayload`, parameterized
  over every tracer including `reverse-linked-list`, `find-starting-point-loop`, and
  `reverse-ll-group-k` — none of which populate the new fields, which is exactly what makes
  them the tracers that would have caught a re-introduction of this bug first.

## RCA-021 — `TracerContractTest`'s BINARY_TREE auto-grower never produces a valid BST

- **Discovered:** 2026-09-06, tracing `bst-validate`, `bst-lca`, `bst-kth-smallest`
- **Status:** Resolved 2026-09-07 by path (a); the three held-back tracers now ship
- **Symptom and impact:** `stepCountGrowsWithInput`'s `growTree` helper builds its "larger"
  input as a level-order array `[1, 2, ..., target]` — a complete binary tree whose value at
  index `i` is always `i + 1`. This is provably **never a valid BST** for any `target >= 3`:
  the deepest-left leaf always holds a *larger* value than its ancestors (level-order fills
  breadth-first, so depth and value both increase together), which is the opposite of what
  an inorder walk requires. Feeding this to a tracer whose algorithm exploits the BST
  ordering property to skip work — `bst-validate`'s inorder-strictly-increasing check,
  `bst-lca`'s left/right descent, `bst-kth-smallest`'s early stop at the kth visit — makes
  it terminate *early* on the "larger" input, the same class of tension `RCA-019` documents
  for partition-search binary search, just triggered by tree shape instead of array values.
  Hand-simulation confirmed this is not a matter of picking better defaults: the violation
  `bst-validate` needs to detect appears at the second inorder-visited node for *any*
  `target >= 3`, so no choice of default tree size changes the outcome.
- **Root cause:** `growTree` optimizes for "produces a structurally valid level-order array
  of the right size" and has no concept of BST-ordering, because every other `BINARY_TREE`
  tracer traced so far (`tree-lca`, `correct-bst-swap`, `morris-inorder`, ...) treats the
  tree as a plain binary tree, where level-order shape is all that matters and ordering is
  irrelevant.
- **A real bug found and fixed while investigating this:** `BstDeleteTracer.delete()` had no
  `node == null` base case before this — deleting a key genuinely absent from the tree (not
  just via `growTree`'s malformed input) threw a `NullPointerException` in real use.
  `bst-delete` itself passes `stepCountGrowsWithInput` cleanly once fixed, since a longer
  search path before hitting the (now-handled) "not found" case is itself more real work.
- **Resolution:** path (a). `InputField.Builder.bstOrdered()` sets a `bstOrdered` constraint
  and `TracerContractTest.growBst` reads it: same complete shape as before, but the values
  are handed out in INORDER position order, which is precisely the definition of a BST. No
  BST-insert sequence was needed — a binary tree is a BST exactly when its values increase
  in inorder, so one in-order walk over the complete shape assigning consecutive integers
  produces a valid BST of any size. Plain-binary-tree tracers do not opt in and keep today's
  shape-only growth unchanged.
  Path (b) was rejected on two grounds. First, count: the ordering-sensitive BST ids are a
  group, not a special case, so it would have meant a growing list of named exemptions where
  one growth mode does. Second, there was no exemption mechanism in the test to extend — its
  only skip path is the `assumeTrue` for "declares no growable field" — so (b) meant building
  a new opt-out as well, and an opt-out reads as "this tracer is excused from proving it
  reads its input", which is exactly the shape of hole this suite exists to close.
  `bstOrdered()` is deliberately a GROWTH HINT rather than a validation rule: `InputValidator`
  does not read it and a caller may still POST any tree. `bst-validate` declares it *because*
  its job is to detect a non-BST — the flag says "the meaningful large input for this
  algorithm is a real BST", not "reject anything else".
- **Not every BST id was affected, and they were checked individually rather than as a
  class:** `bst-min-max` walks one side following child pointers, so a deeper tree of ANY
  shape already meant a longer walk — verified it passes both with and without the flag.
  `construct-bst-preorder` takes a single preorder `INT_ARRAY`, so `growTree` never applies
  to it. `merge-two-bsts` and `largest-bst-in-bt` traverse every node unconditionally.
  `largest-bst-in-bt` in particular must NOT declare the flag: it searches for an embedded
  valid BST inside a possibly-invalid general tree, so a tree that is not a BST is exactly
  the input worth growing it with.
- **Proved RED before the fix was accepted:** with `growBst` disabled and every other line of
  the batch in place, `stepCountGrowsWithInput` fails on exactly three ids and passes on the
  rest — `bst-validate` (8 steps on its defaults, 3 on the "larger" input), `bst-lca` (3 vs
  2) and `bst-floor` (4 vs 4). Re-enabling it turns all three green. Three of the ids that
  declare the flag do NOT need it — `bst-intro` never exits early, `bst-search`'s default
  target is deliberately absent so the walk runs the full depth either way, and
  `bst-min-max` only follows child pointers — they declare it because the flag states an
  input contract, not because the test forced it.
- **Regression guard:** `TracerContractTest.stepCountGrowsWithInput`, unchanged in intent and
  now actually exercised by the ordering-sensitive tracers. `bst-validate` is the clearest
  case: its default is deliberately LeetCode 98's invalid `[5,1,4,null,null,3,6]`, so the
  early-exit path is still exactly what its golden file pins, while the grown input is a real
  BST that has to be walked in full.

## RCA-022 — The per-step envelope constant was 50 bytes light on every step ever emitted

- **Discovered:** 2026-09-06, tracing the Stack & Queue Learning cluster — `stack-queue-impl`
  failed `TracerContractTest.byteEstimateTracksActualPayload` at ratio 0.896 against a 0.9
  floor.
- **Status:** Resolved
- **Symptom and impact:** `StepEmitter.estimateBytes` opens with a flat constant for the step
  envelope — the field names, the numbers, the `dsType`, and the `"field":null` Jackson
  writes for every structure a step does NOT carry. It was `190`. Measured, an
  `ExecutionStep` with an empty description, no variables and no structure at all serialises
  to **239** bytes. Every step ever estimated, by every tracer, was therefore ~50 bytes
  light. The byte budget exists to stop a trace before it becomes a response the browser
  cannot use, and the estimator's own doc says it must lean HIGH because under-estimating
  lets a trace past the ceiling it exists to enforce — so this was the failure direction that
  matters, on every tracer, silently.
- **Root cause:** the constant was calibrated against real responses whose steps all carried
  an `arrayState` or a graph. Those per-element constants lean generously high (52 bytes per
  `ArrayElement`, 96 per `GraphNode`), and on a step with a dozen elements that surplus more
  than covered a 50-byte envelope shortfall. The shortfall only becomes visible on a step
  whose *only* payload is `queueOrStackState`, which no tracer emitted until the Stack &
  Queue "implement X using Y" problems — they have no array to show, only the structure being
  implemented. `task-scheduler`, already live, was sitting at 0.901: one existing tracer was
  already within 0.001 of the same failure and nobody had cause to look.
- **Resolution:** bumped the envelope constant from 190 to 240, one byte above the measured
  239. Ratios across the whole registry now sit between ~1.0 and ~1.31, against a permitted
  band of 0.9–2.5, so the estimate leans high everywhere with wide margin at both ends.
- **Regression guard:** `TracerContractTest.byteEstimateTracksActualPayload`, which is what
  caught this. It is parameterized over every registered tracer, and the tracers that will
  catch a re-introduction first are the structure-light ones — `stack-queue-impl`,
  `queue-stack-impl`, `stack-array-impl`, `task-scheduler` — because they have no array
  payload whose own constant can absorb the error. Note the guard is a *ratio* band, so it
  cannot be satisfied by inflating the constant without limit either.

## RCA-023 — Two tracers declared a dsType whose canvas reads a field they never emit

- **Discovered:** 2026-09-06, reviewing the completed Stack & Queue category against the
  live API — `celebrity-problem` and `number-greater-elements-right` both reported
  `dsType: Stack` while their traces carried no `queueOrStackState` at all.
- **Status:** Resolved
- **Symptom and impact:** `dsType` decides which canvas renders a trace
  (`frontend/src/canvas/registry.js`), and each canvas reads one particular field off the
  step. Both tracers declared `STACK`, so the UI routed them to `StackCanvas` — which reads
  `queueOrStackState` and renders its explicit "empty" state when that field is absent.
  Neither tracer ever called `emit.stack(...)`: `celebrity-problem` emits only `gridState`
  (the knows-matrix) and `number-greater-elements-right` only `arrayState`. So both problems
  animated as a permanently empty stack panel beside narration describing an elimination or
  a sweep the viewer could not see, while the structure each one *did* compute and put on
  the wire was never drawn by anything. This is the failure `StackCanvas` was itself created
  to fix ("the actual stack was computed but never drawn"), inverted: here the canvas is
  right and the payload is missing.
- **Root cause:** `CatalogTracerMetadataTest` checks that the CATALOGUE entry and the TRACER
  agree on a dsType. That is one half of the contract, and on its own it is satisfied by two
  files agreeing with each other while both are wrong about the trace — `bulkDsType()`
  defaults every Stack & Queue id to `STACK`, and both tracers returned `STACK`, so the two
  halves matched and every test passed. Nothing compared the declared type against the
  payload the run actually produced. CLAUDE.md's rule ("emit the structure that `dsType()`
  promises") was written down but not enforced.
- **Resolution:** declared the dsType of the structure each one really emits rather than
  inventing payload — `celebrity-problem` to `MATRIX` (it reads an N x N matrix with two
  integer pointers; the classic stack elimination is a different algorithm from the
  two-pointer one implemented) and `number-greater-elements-right` to `ARRAY` (counting is a
  rank query, and the popping that makes a monotonic stack cheap discards exactly the
  information a count has to keep). `bulkDsType()` updated to match both. The regenerated
  golden files differ **only** in the `dsType` field — no description, variable or
  highlighted line moved — which is the evidence that this changed routing and not
  behaviour.
- **Regression guard:** `DsTypePayloadContractTest.declaredDsTypeIsBackedByEmittedPayload`,
  new, parameterized over every registered tracer. It maps each dsType to the field its
  canvas reads (mirroring `canvas/registry.js`) and fails if no step populates it. Proven
  RED first: against the unfixed code it failed on exactly these two ids and passed on the
  other 140 it checks. dsTypes whose canvas has no single required field are skipped rather
  than passed, so the gap is visible in the Surefire report instead of looking like coverage.

## RCA-024 — `CelebrityProblemTracer` startup crash: `ClassNotFoundException: Inputs`

- **Discovered:** 2026-09-07
- **Status:** Resolved
- **Symptom and impact:** the Spring Boot application failed to start with
  `BeanCreationException` for `celebrityProblemTracer`. The full chain was
  `NoClassDefFoundError: Inputs` → `ClassNotFoundException: Inputs` (note: unqualified, not
  `com.dsa.ui.tracer.Inputs`). Every backend endpoint was down.
- **Root cause:** stale incremental compilation output. Maven's incremental compiler
  (`maven-compiler-plugin` default) can leave a `.class` file from a previous compilation
  round intact when its source hasn't changed but a dependency class *has*. The
  `CelebrityProblemTracer` source is correct — it uses `import com.dsa.ui.tracer.*` which
  covers `Inputs` — but its bytecode was compiled (or retained) from a build where
  `Inputs.class` was not yet in the output directory. The JVM resolves `Inputs` lazily when
  it first introspects `run(Inputs, StepEmitter)`, and at that point finds no class on the
  classpath matching the unresolved constant-pool entry. This exact failure mode is specific
  to incremental builds where the tracer package gained new classes (`Inputs.java` was added
  in PR #7) and the downstream file wasn't recompiled.
- **Resolution:** `mvn clean compile` — a full clean build recompiles all sources and
  resolves the constant pool correctly. No source code change was needed.
- **Regression guard:** this is a build-environment issue, not a code defect, so no new test
  is appropriate. The operational guard is: always run `mvn clean test` (not `mvn test`)
  after pulling a branch that added or renamed files in the `tracer` package. The CI workflow
  already runs `mvn -B test` on a fresh checkout (no stale `target/`), so this failure cannot
  reach `main`.

## RCA-025 — `resolvedInput` read off a step, which never carries it

- **Discovered:** 2026-09-12, during the canvas audit
- **Status:** Resolved
- **Symptom and impact:** five branches across two canvases had never executed, and the
  failure was not merely dead code — it drew the **wrong data**. `IntervalCanvas` resolves
  its intervals through a chain of fallbacks; with the `resolvedInput` rungs dead it fell
  through to the `inputSpec` **defaults**. For `n-meetings-in-one-room` that is 15 of 16
  steps, so running your own meetings drew `[1,3,0]/[2,4,6]` while the narration described
  yours. Measured before and after on a real custom run: 7 of 8 steps showed the defaults.
- **Root cause:** `resolvedInput` is a property of the **trace** — `ExecutionTrace` carries
  it once and `TraceResponse` serialises it at the top level — but four call sites read
  `step.resolvedInput`, and nothing has ever placed it on a step. The mistake survived
  because it is invisible at the point of use: `activeStep?.resolvedInput` is valid
  JavaScript that silently yields `undefined`.
- **Resolution:** `useTrace` captures `resolvedInput` from the execute response and clears it
  at request start so a stale echo cannot outlive its trace; `App` passes it to the canvases;
  `IntervalCanvas` and `CaptureStrip` take it as a prop. `CaptureStrip`'s `useMemo`
  dependencies gained it too — without that it would have kept a previous run's intervals
  after a re-run.
- **Regression guard:** `IntervalCanvas.test.jsx` asserts a run's own intervals appear **and**
  that the spec defaults do not; written RED first, where the failure rendered
  `#1[1, 2]#2[3, 4]#3[0, 6]` — the defaults, exactly as a user saw them.
- **The general lesson:** before adding a canvas, check where the field you are reading
  actually lives. Trace-level data (`resolvedInput`, `anchors`, `code`) is not on a step, and
  optional chaining will not tell you.

## RCA-026 — Tests asserted payload shapes the server has never sent

- **Discovered:** 2026-09-12
- **Status:** Resolved
- **Symptom and impact:** dead code reported itself as covered. Three separate cases:
  `IntervalCanvas.test.jsx` and `CaptureStrip.test.jsx` hung `resolvedInput` on a step, so
  they exercised the branches of RCA-025 that cannot run in production; and a test for the
  new `constraints` field asserted against the **in-memory catalogue** while
  `/api/problems/{id}` silently dropped the field, so the feature shipped populated
  correctly and serving nothing, green throughout.
- **Root cause:** a hand-written fixture is only as true as its author's model of the wire.
  Nothing compared the fixture to a real response, and `/api/problems/{id}` builds its map
  field by field, so a model field is not served until someone names it there.
- **Resolution:** the fixtures now carry what the running backend actually returns, verified
  against it rather than assumed. `DetailResponseContractTest` walks `ProblemDetail` by
  reflection and fails on any field the wire does not carry, with an `INTENTIONALLY_ABSENT`
  set so an omission stays a decision.
- **Regression guard:** `DetailResponseContractTest`, proven RED by deleting the
  `constraints` line again — the failure names the field.
- **The general lesson:** assert over the wire, not over the object. A fixture invented at
  the keyboard can validate code that can never execute.

## RCA-027 — Retagging a `dsType` misses the entries registered outside the bulk helper

- **Discovered:** 2026-09-12 and 2026-09-13, three times in a row
- **Status:** Resolved (recurring; the guard is what catches it)
- **Symptom and impact:** every batch retag left catalogue entries behind, because the same
  topic registers problems in two ways — a bulk table driven by a `bulkDsType`-style helper,
  and individual `problems.put(...)` calls with their own literal. Sliding Window left one
  (`longest-substring-without-repeating`), Binary Search left fourteen, Recursion &
  Backtracking left two. Untouched, those render through the wrong canvas.
- **Root cause:** there is no single place a topic's `dsType` is decided, so "change the
  topic's type" has no single edit.
- **Resolution:** none needed beyond finishing each retag — but the workflow now assumes a
  second pass. Run `CatalogTracerMetadataTest` after changing any tracer's `dsType`; it names
  the stragglers by id.
- **Regression guard:** `CatalogTracerMetadataTest.catalogueDsTypesMatchEveryRegisteredTracer`,
  which caught all three batches and reported the ids verbatim.
- **The general lesson:** a retag is not done when the tracers compile. Grep the service for
  the literal as well as the helper, and let the cross-tier test decide.

## RCA-028 — A payload contract written narrower than the canvas it describes

- **Discovered:** 2026-09-13
- **Status:** Resolved
- **Symptom and impact:** `DsTypePayloadContractTest` failed on correct tracers. Its
  `RECURSION_TREE` rule required `treeNodes`, and its `HEAP` rule required `arrayState`, but
  both canvases read **either** source: `RecursionTreeCanvas` rebuilds the tree from
  `callStack` when a tracer emits no `treeNodes`, and `HeapCanvas` derives whichever of the
  tree or the array was not emitted, since the mapping is arithmetic. The failures named
  `generate-binary-strings`, `heaps-theory` and `implement-min-heap` as broken when they
  were not.
- **Root cause:** the requirement was written from one tracer's habit rather than from what
  the canvas reads, and the two drifted the moment a canvas learned a second source.
- **Resolution:** both rules accept either field, with the reason recorded beside them.
  Neither present is still a failure — that renders an empty canvas, which is the thing the
  test exists to prevent.
- **Regression guard:** the test itself; it is doing its job correctly in both directions.
  What changed is the rule, not the enforcement.
- **The general lesson:** this contract describes the **canvas**, not the tracer. When a
  canvas gains a fallback source, the rule has to gain it in the same commit, or the test
  starts reporting healthy tracers as broken.

## RCA-029 — Truncating a label from the front removed the only part that varied

- **Discovered:** 2026-09-13, reported against the permutations visualizer
- **Status:** Resolved
- **Symptom and impact:** every node in a derived recursion tree read `backtrack(` and the
  tree was unreadable — 16 identical boxes where the whole point is that they differ.
- **Root cause:** the node label was truncated to its first 11 characters. In a recursion
  tree every node calls the *same* function, so the name is the one part carrying no
  information, repeated once per node, while the arguments are the only thing distinguishing
  siblings. Truncating from the front kept exactly the wrong half.
- **Resolution:** nodes show what is inside the parentheses — `idx=0`, `open=1,close=0` —
  with the whole frame kept in a `<title>` for hover, and the node widened to suit.
- **Regression guard:** `RecursionTreeCanvas.derived.test.jsx` asserts two sibling frames
  render distinguishably and that the bare function name is not what appears.
- **The general lesson:** when truncating for display, keep the part that varies. A cap
  chosen for layout can silently destroy the information the element exists to convey, and
  no test catches it unless one asserts that two different inputs look different.

## RCA-030 — A retired topic left its `switch` `default:` returning steps

- **Discovered:** 2026-09-12, by the full-catalogue audit in `AUDIT.md` (F1)
- **Status:** Resolved, permanently — the layer that held it is deleted
- **Symptom and impact:** twelve of the eighteen legacy services still ended their
  `switch (problemId)` in a `default:` that **returned** steps. With every catalogued id
  traced on `/api/problems`, those branches had no legitimate consumer left and existed only
  to serve a wrong animation to whatever id nobody had explicitly retired. Seven id/route
  pairs were live: `/api/graphs/advanced/execute/dijkstra-min-heap` answered 200 with the
  graph-intro animation.
- **Root cause:** retiring a topic was done id by id, and `default:` was treated as the
  holding pen for "the ones not done yet". Nothing marked the moment the holding pen should
  have become a throw. Worse, the refusals were pinned by **hand-maintained lists** of
  retired ids — a 430-entry `RETIRED_IDS` in `ApiContractTest` and a per-topic `retired` set
  in eight service tests — and drift in those lists is what kept the stragglers invisible
  through four rounds of cleanup.
- **Resolution:** all eighteen services were made to throw, then the legacy layer was deleted
  outright: eighteen controllers, all eighty step generators, and twenty-six test classes.
  The services survive as `ProblemProvider`s owning catalogue metadata only.
- **Regression guard:** `ProblemsApiTest.legacyRoutesNoLongerExist` asserts the routes are
  gone, so reintroducing one fails a test. `ProblemProviderContractTest` is parameterized
  over the providers Spring actually registers rather than over a typed list.
- **The general lesson:** never write a per-id list in a test to express "which ids are in
  state X". Derive it from the registry or the catalogue. A list is a second source of truth
  that drifts silently and hides exactly the cases it was meant to pin.

## RCA-031 — A canvas invented a plausible structure when its state was missing

- **Discovered:** 2026-09-12 (`AUDIT.md` F7)
- **Status:** Resolved
- **Symptom and impact:** `DsuCanvas` read four magic string keys out of `step.variables` and
  defaulted each to a hardcoded literal, so a renamed key or a step without them drew a
  **fabricated seven-element DSU** that looked entirely plausible. Nothing failed; the user
  was shown a lie. This is RCA-001's defect moved into the render path.
- **Root cause:** the DSU has no structural payload field — its state is smuggled through
  `variables` as human-readable strings and re-parsed with a regex in the browser — so the
  canvas had no way to distinguish "absent" from "not yet set" and chose to look complete.
- **Resolution:** missing state renders an explicit "DSU state unavailable" panel.
  `DsTypePayloadContractTest` pins the `parent[]` key, making the variable names a wire
  contract rather than a convention.
- **Regression guard:** three `DsuCanvas.test.jsx` cases, proven RED against the old
  component with "Unable to find `[data-testid=dsu-state-unavailable]`".
- **Still open:** the transport itself. DSU state is still string round-tripping through
  `variables`, which is the most fragile payload path left in the app.

## RCA-032 — A layout refactor removed three panels from mobile with every test green

- **Discovered:** 2026-09-12, while reading a diff — not by a failure
- **Status:** Resolved
- **Symptom and impact:** moving the code panel beside the canvas restructured a ternary and
  made the mobile branch unreachable. The phone layout silently lost the code panel, the
  input editor and the complexity card **at once**, and all 245 tests stayed green.
- **Root cause:** the desktop and mobile layouts share one conditional expression, and
  rewriting its condition changed which branch mobile reaches. No test rendered the app at a
  narrow viewport and asserted the mobile-only affordances exist.
- **Resolution:** the branches were rewritten so each viewport has its own explicit arm.
- **Regression guard:** a mobile-viewport test asserting the tab card's Code control is
  present, proven RED by disabling the branch.
- **The general lesson:** this codebase catches fake work inside a unit very well and wrong
  wiring between units very poorly. A structural change needs a smoke test per breakpoint,
  because "the component still renders" is not the same as "the user can still reach it".

## RCA-033 — Contrast verified in one theme, failing in the other

- **Discovered:** 2026-09-13, while tokenising the canvas role colours
- **Symptom and impact:** every algorithm role fill failed 4.5:1 against its white label in
  the **dark** theme — `#3b82f6` at 3.68:1, `#10b981` at 2.54:1, `#f59e0b` at 2.15:1 — and
  had done so for as long as those values existed as literals in five canvases.
- **Status:** Resolved
- **Root cause:** two compounding. The colours were hardcoded, so the token system's measured
  4.5:1 standard never applied to them. And the first guard written for them checked only the
  light theme — it passed on the first run, which was the tell.
- **Resolution:** `--role-ink` now flips with the theme exactly as `--probe-on` does: dark
  mode keeps bright fills that read against the dark ground and takes near-black ink, light
  mode darkens the fills and goes white. Every role clears 4.5:1 on both sides, measured.
- **Regression guard:** `designTokens.test.js` checks both themes, proven RED by lightening
  `--role-current` for light mode the way anyone adapting a colour naturally would: 1.80:1.
- **The general lesson:** a guard that passes on its first run has not been shown to work.
  For anything theme-dependent, assert both themes in the same test — checking one is a
  coin flip that looks like diligence.

## RCA-034 — A structure that persists between steps rendered as empty on the steps that did not restate it

- **Discovered:** 2026-09-13, by the Stack & Queue topic audit
- **Status:** Resolved
- **Symptom and impact:** `StackCanvas` and `QueueCanvas` read
  `activeStep?.queueOrStackState || []`, which collapses "the stack is empty" and "this step
  did not mention the stack" into the same render. Tracers restate a structure only on the
  steps that change it and narrate in between, so the stack **blinked empty between every
  push**. Measured: **60 steps across 21 of the 24** Stack & Queue problems displayed an
  empty stack while it held items. `stock-span-problem` did it on every other step — push,
  empty, push, empty — which destroys the only thing a monotonic-stack problem teaches.
- **Root cause:** `|| []` treats absence as a value. The payload is genuinely optional per
  step by design; the canvas simply had no way to say "unchanged".
- **Resolution:** `trace/lastPayload.js` returns the most recent stated value, looking back
  no further than the step being shown. An explicit `[]` is a real value and still renders
  empty; only absence looks back.
- **Regression guard:** cases in `StackCanvas.test.jsx` and `QueueCanvas.test.jsx` asserting
  both halves — that a silent step keeps the contents, and that an explicit empty array
  still reads empty. Proven RED against the old expression.
- **Related:** the same shape as RCA-025 and RCA-031 — a missing payload rendered as a
  confident wrong answer. Three canvases have now made this mistake in three different ways.
  Before writing a canvas, decide explicitly what "the trace did not say" should look like,
  and make sure it is distinguishable from a real value.

## RCA-035 — Seven canvases drew the catalogue's data over the caller's own input

- **Discovered:** 2026-09-13, by the Binary Trees / BST audit; the audit's own findings were
  clean and this was found while checking for the RCA-034 shape
- **Status:** Resolved
- **Symptom and impact:** the same defect as RCA-025, present in seven more canvases and far
  wider than any single one. A tracer restates a structure only on the steps that change it;
  every canvas that read `activeStep?.field || problem?.defaultX` therefore fell through to
  the **catalogue default** on every narration step. Run a tree of `[9,8,7,6]` through
  `tree-balanced` and seven of its thirteen steps drew `1,2,3,4,5`.

  Measured across the catalogue: **1506 steps in 142 of 232 problems** drew a catalogue
  default mid-run. `construct-bst-preorder` did it on 84% of its steps.

  | dsType | steps | problems |
  |---|---:|---:|
  | Graph | 612 | 40 |
  | Matrix | 608 | 31 |
  | Array | 176 | 40 |
  | LinkedList | 36 | 9 |
  | RecursionTree | 27 | 3 |
  | Bits | 24 | 9 |
  | String | 23 | 10 |

- **Root cause:** `||` collapses "this step did not restate the structure" into "there is no
  structure", and the chosen replacement was the catalogue's own sample data — which looks
  entirely plausible and is wrong for any run the user configured. The defaults are honest in
  exactly one situation, before any step has emitted anything, and that is the only one the
  expression got right.
- **Resolution:** every affected canvas now uses `trace/lastPayload.js`, which returns the
  most recent stated value and looks back no further than the step being shown. The
  catalogue default survives only as the pre-run fallback. `GraphCanvas` needed more than a
  one-line change: nodes and edges must come from the **same** step, or a carried topology
  renders with no edges at all — a graph as a field of disconnected dots.
- **Regression guard:** a carry test per canvas, each asserting the emitted value survives a
  narration step **and** that the default no longer appears; plus the pre-run case, which
  must still show the default. `TreeCanvas`'s was proven RED first.
- **The general lesson, now fifth time:** this is RCA-025, RCA-031, RCA-034 and this entry —
  four separate canvases inventing data when the trace was silent, in four different ways
  (spec defaults, a hardcoded literal, an empty array, catalogue defaults). **Before writing
  a canvas, decide what "the trace did not say" renders as.** The reflex `|| something`
  is the bug: it always produces a confident answer, and a confident wrong picture is worse
  than an honest blank one.

## RCA-036 — Good delta hygiene in a tracer blanked the canvas that needed the pair

- **Discovered:** 2026-09-13, by the Binary Search audit
- **Status:** Resolved
- **Symptom and impact:** `count-occurrences`, `first-last-occurrence` and
  `floor-ceil-sorted-array` ran correct binary searches beside a canvas that said
  **"No search range for this step."** on every step of every run. Three problems in the
  topic whose whole subject is the search space, with no search space drawn.
- **Root cause:** `SearchSpaceCanvas` reads `low` and `high` off the **same** step, on
  purpose — taking one bound from one moment and the other from another is precisely the
  defect RCA-034/035 ended with in `GraphCanvas`. The three tracers each named only the
  bound that had just moved:

  ```java
  emit.at("lowerMid").var("lb", lb).var("high", mid - 1)   // low never mentioned
  emit.at("lowerMid").var("low", mid + 1)                  // high never mentioned
  ```

  That reads like careful delta hygiene, and against a canvas that carries values forward
  it would be. Here no step ever carried a pair, so there was nothing to carry, and the
  canvas's own guard (`if (low === null || high === null)`) rendered the empty state
  forever. The tracers were right about the algorithm and wrong about the transport.
- **The general shape:** when a canvas needs **two fields together** to mean anything, a
  tracer emitting them separately produces not a degraded picture but no picture at all,
  and every existing contract passes — `DsTypePayloadContractTest` was satisfied because
  all three do emit `arrayState`. The missing payload was in `variables`, which no contract
  looked at.
- **A second finding from the same sweep:** `median-2-sorted-arrays` and
  `kth-element-2-sorted-arrays` did state a range, and it never moved. Both defaults landed
  on a valid partition with the very first guess — 4 and 3 steps, no shrink branch, no
  halving. Same class as RCA-019's `next-permutation`: the default input is the one every
  visitor sees, and a binary search that resolves on probe one animates a single static
  interval.
- **Resolution:** the three tracers now state both bounds (and `mid`) on every search step;
  the two partition tracers took defaults that need three probes. Their cells row was
  rebuilt at the same time — it had been `concat(a, b)` built **after** the swap, so cell 0
  silently changed which array it belonged to mid-trace, and the row stopped being sorted in
  any direction. `PartitionCutView` keeps the caller's order and marks the four boundary
  elements, which is both the algorithm's actual insight and what keeps the canvas out of
  index mode.
- **Regression guard:** `SearchSpaceContractTest`, parameterized over every `SEARCH_SPACE`
  tracer. `statesBothBoundsOnSomeStep` fails when no step carries a numeric low and high
  together; `rangeNarrowsOnDefaults` fails when the default input draws fewer than two
  distinct ranges. Proven RED first: 8 failures naming exactly those five ids.
- **What to check when adding a canvas:** if it needs more than one field to render, say so
  in a contract test at the same time. A canvas whose empty state is reachable from a
  *complete* trace is a canvas with an unwritten contract.

### Follow-on, same sweep — the canvas changed its mind about what it was drawing

`SearchSpaceCanvas` decides per step whether `[low, high]` indexes the cells or names a
range of candidate answers, and its own header warns that "is high small" only coincides
with the answer. The discriminator it chose coincides too. `aggressive-cows` searches
distances 1..8 over five cow positions: answer space, correctly, until `high` shrinks below
five — and then the badge flips to `indices [3, 3]` and starts calling a distance an index,
halfway through the animation. `floor-ceil-sorted-array` flipped the other way.

A tracer does not change what it is searching partway through a run, so the kind is read
once now, from the first step that states a range, and held for the whole trace. Guarded by
`SearchSpaceCanvas.test.jsx`, "does not change its mind about what the range means
mid-animation", proven RED first.

The lesson is narrower than the heuristic: **a per-step inference about a whole-trace fact
will eventually disagree with itself**, and the disagreement is visible to the user as the
picture rewriting its own axis.

## RCA-037 — The recurrence blinked out on every step that did not restate it

- **Discovered:** 2026-09-13, by the Dynamic Programming audit
- **Status:** Resolved
- **Symptom and impact:** the recurrence panel - the headline teaching device of the DP
  canvas - appeared and vanished as the viewer stepped through a trace. **164 of 1133 table
  steps across 26 problems** dropped it after it had already been shown; `print-lis` lost it
  on 30 of its 47 steps, `partition-equal-subset-sum` on 22 of 51, `matrix-chain-
  multiplication` on 18 of 32.
- **Root cause:** `DpTable` carries two fields that look like a pair and are not.
  `formula` is a **constant of the problem** (`dp[i] = dp[j] + 1, ...`); `substitution` is
  **one step's arithmetic** (`dp[3] = dp[2] + 1 = 2`). Tracers attach both together, on the
  steps that actually compute a cell — correctly. `DpTableCanvas` then rendered the block
  only when the *current* step carried both, so base cases, comparison steps and the closing
  summary showed nothing at all. The comparison steps are exactly where a learner needs the
  rule they are evaluating against.
- **The general shape, third occurrence:** this is RCA-034/035 again, one level down. There
  the missing value was a structure at the top of a step; here it is a field nested inside
  `dpTable`, which is why the `lastPayload` sweep did not reach it. **A canvas that reads a
  persistent value off the current step alone will blank it**, wherever that value lives.
- **Resolution:** `DpTableCanvas` carries the *formula* forward from the most recent step
  that stated one, and never carries the substitution — held over, it would caption the
  wrong arithmetic. The block renders whenever a rule is known; a bare substitution still
  renders nothing, because arithmetic with no rule above it is an unexplained fact, while a
  bare rule is the recurrence standing over a step it does not cover, which is what a base
  case is.
- **Regression guard:** `DpTableCanvas.test.jsx` — "holds the rule on screen across the
  steps that do not restate it" and "does not carry a rule backwards to steps before it was
  stated". Proven RED first.

## RCA-038 — A DP table printed unwritten array memory as settled values

- **Discovered:** 2026-09-13, by the Dynamic Programming audit
- **Status:** Resolved
- **Symptom and impact:** `knapsack-01` and `unbounded-knapsack` drew every cell in the
  `known` state from step 1, so the whole table read as already solved. Item 4 at capacity 5
  showed `0` on the first frame and finishes at `13` — and a viewer had no way to tell that
  `0` from a computed one, because both carried the same glyph. The one thing a DP table
  exists to show, unknown cells becoming known, was the one thing these two did not show.
- **Root cause:** the table builder assigned `known` to everything that was not the probe or
  a read. `dp` is a plain `int[][]`, so the unreached cells were Java's zero-fill being
  printed as data. `DpCell` already has the right word for this - `void` - and these two
  tracers never used it.
- **Why no contract test:** the tempting rule ("a cell that later changes value must not
  have been presented as settled") flags five correct tracers. `print-lis`,
  `number-of-lis`, `longest-string-chain`, `largest-divisible-subset` and
  `longest-bitonic-subsequence` all start every cell at `1` and improve it, and their
  narration says so: that 1 is a **genuine lower bound the algorithm holds**, not unwritten
  memory. No structural rule separates the two — the difference is semantic. The golden
  files pin cell state, so the fix is guarded there, and this note exists so the next person
  does not go looking for the test that cannot be written.
- **Resolution:** both builders mark cells past the fill frontier `void` with a `·`, and the
  closing step marks the completed table `resolved` rather than `known` — the "table is
  finished" frame the other DP tracers already end on.

## RCA-039 — A dsType named the structure the trace mentions least

- **Discovered:** 2026-09-13, by the Dynamic Programming audit
- **Status:** Resolved
- **Symptom and impact:** `max-rectangle-area-all-ones` declared `MATRIX`, so `GridCanvas`
  drew the binary board — which the tracer states twice and which never changes. The other
  **60 of its 62 steps** narrate a histogram of column heights and a monotonic stack popping
  through it, emitted as `arrayState`, which a Matrix hero has no renderer for. The viewer
  watched a static board while the words described something not on screen.
- **Root cause:** `DsTypePayloadContractTest` asks whether the declared type's field is
  *ever* populated — `anyMatch`. Two steps out of sixty-two satisfies it. The contract
  proves a tracer is not lying about its canvas; it cannot tell whether the canvas was
  pointed at the run's main structure or its backdrop.
- **The probe that did not work, recorded so it is not repeated:** "the declared type's
  field must not be out-counted by another structure field" flags **33 tracers**, nearly all
  correct — a Stack tracer legitimately emits the input array on every step and the stack
  only where it changes, and `lastPayload` carries the sparse one. Step counts do not
  separate a backdrop from a hero. Nothing mechanical does; this is a judgement about which
  structure the narration is about, and the audit has to make it by reading.
- **Resolution:** retagged `ARRAY`, so the histogram it narrates is the hero, and the board
  is a companion pane — the same split `maximum-rectangles-binary-matrix`, the other tracer
  for this problem, already makes. `canvas/companions.js` now offers the grid companion to
  an Array hero as it already did to a Stack hero; both entries name a real emitter, per
  that file's own rule.
- **Regression guard:** `companions.test.js`, "adds a grid companion for an Array hero when
  any step carries a grid", proven RED first. The dsType itself is pinned by the golden and
  by `CatalogTracerMetadataTest`.

## RCA-040 — The canned-fallback shape, found a third time, in a canvas nobody had reached it in

- **Discovered:** 2026-09-13, by the Greedy Algorithms audit
- **Status:** Resolved
- **Symptom and impact:** none yet — and that is the point of the entry. `IntervalCanvas`
  ended its five-branch interval-resolution chain with a hardcoded literal:

  ```js
  intervals = [
    { id: 1, label: '#1', start: 1, end: 2, state: 'settled' },
    { id: 2, label: '#2', start: 3, end: 4, state: 'probe' },
    ...
  ];
  ```

  Those are `n-meetings-in-one-room`'s own default meetings, pasted in, with invented
  `settled` and `probe` states. Any Interval run that fell off the end of the chain would
  have drawn another problem's data as if it were its own — confidently, with state colours
  it made up. Checked step by step against both live Interval tracers: **0 of 26 steps**
  reach it today.
- **Root cause:** the same instinct as RCA-025 (`IntervalCanvas`'s `resolvedInput`),
  RCA-031 (`DsuCanvas`) and RCA-035 (seven canvases): when a canvas cannot tell what to
  draw, draw *something*. A blank panel looks broken in review; a plausible one does not.
- **Resolution:** replaced with an explicit "No intervals in this step." Its test previously
  pinned the fabricated version as intended behaviour ("renders default intervals when step
  is empty"), so the test was rewritten to state the rule instead of the accident.
- **Why record an unreachable defect:** it was reachable until the RCA-025 fix landed, and
  `insert-interval` moving into this canvas in the same audit is exactly the kind of change
  that makes a dead branch live again. Unreachable is the cheapest moment to delete a
  landmine, not a reason to leave it.

## RCA-041 — A hero canvas that could not draw the structure the run was about (second instance)

- **Discovered:** 2026-09-13, by the Greedy Algorithms audit
- **Status:** Resolved
- **Symptom and impact:** two more of the shape RCA-039 named, found by looking for it
  deliberately rather than by accident:
  - `lru-page-replacement` is `ARRAY`-hero and emits the recency queue — the structure the
    entire algorithm is about — on **13 of its 16 steps**. `ArrayCanvas` never references
    `queueOrStackState`, so it was computed and drawn nowhere.
  - `insert-interval` is a merge along a timeline, labels every cell it emits `"[a,b]"`, and
    is named in `IntervalCanvas`'s own header as one of its problems — yet was tagged
    `ARRAY`, so it drew a bar chart whose bar heights were each interval's *end time*.
- **Root cause:** as RCA-039. `DsTypePayloadContractTest` proves a tracer is not lying about
  its canvas; it cannot tell whether the canvas was pointed at the run's main structure.
- **Resolution:** `insert-interval` retagged `INTERVAL`; `lru-page-replacement` keeps the
  reference string as its hero and gains the existing queue companion, which
  `canvas/companions.js` now offers to an `Array` hero as it already did to `Graph` and
  `Matrix`.
- **A test that was quietly wrong:** `companions.test.js` asserted "adds nothing for a
  non-Graph, non-Matrix hero, even with a populated queueOrStackState", using `Array` as the
  stand-in. Its stated reason — "a dsType whose OWN hero already draws queueOrStackState
  must not also get a companion" — is true of `Stack` and `Queue` and **not** of `Array`,
  which never touches the field. The example had hardened an arbitrary choice into a rule,
  and the rule was blocking the fix. It now names `Stack` and `Queue`, the types the reason
  actually applies to.
- **How to find the next one:** for each tracer, list the structure fields its steps
  populate and ask whether the declared dsType's canvas renders the one the *narration* is
  about. Counting steps does not work — see RCA-039.

## RCA-042 — Five heaps were drawn fully sorted, teaching the misconception the canvas exists to correct

- **Discovered:** 2026-09-13, by the Heaps & PriorityQueue audit
- **Status:** Resolved
- **Symptom and impact:** `HeapCanvas` puts slot `i`'s children at `2i+1` and `2i+2` and
  prints the index under every slot, because "the tree and the array are the SAME structure"
  is the entire lesson (its own header says so). Five tracers fed it a **sorted** snapshot:

  ```java
  List<Integer> values = new ArrayList<>(heap);
  Collections.sort(values);              // <- every step, every one of these problems
  ```

  So the tree drawn was perfectly ordered at every step, on every input — which says a
  priority queue keeps all of its elements in order. It does not; only the root is
  guaranteed, and that is the single most common misconception about heaps. Affected:
  `kth-largest-stream`, `sort-k-sorted-array`, `maximum-sum-combination`, `design-twitter`,
  and `min-cost-connect-sticks`, which was not using a heap at all — it kept a sorted
  `ArrayList` with `remove(0)` and an insert-in-place, while its code panel showed
  `PriorityQueue.poll()`.
- **Root cause:** `java.util.PriorityQueue` does not expose its array. `toArray()` is
  documented to return the elements "in no particular order" — it happens to return the
  internal heap on every mainstream JVM, but that is not something a teaching visualization
  should rest on. Sorting was the reachable way to get *a* deterministic order, and a sorted
  array genuinely is a valid heap, so nothing was ever wrong enough to fail.
- **Resolution:** `ArrayHeap`, a small explicit binary heap with `offer`/`poll`/`slots()`,
  where `slots()` returns the array the class actually sifts. `TaskSchedulerTracer` and
  `ImplementMinHeapTracer` already owned their heap array this way and were always honest;
  this is the same thing, shared. `offer` returns the index the value settled at, which is
  the slot worth highlighting.
- **Verification:** all four changed goldens produce byte-identical answers, every emitted
  array satisfies its heap property, and no trace is fully sorted any more —
  checked programmatically across every step, not by eye. `design-twitter`'s golden did not
  move: its heap never holds more than three elements, where sorted order and heap order
  coincide. It was converted anyway, since the input is caller-supplied.
- **What this says about the class of bug:** the trace was *correct*, the canvas was
  *correct*, the contract tests were satisfied, and the picture still taught the opposite of
  the truth. Nothing mechanical catches that. The question to ask of a visualization is not
  "is this data valid" but "what would a learner conclude from watching it".

## RCA-043 — An empty structure reported as a missing one

- **Discovered:** 2026-09-13, by the Heaps & PriorityQueue audit
- **Status:** Resolved
- **Symptom and impact:** `HeapCanvas` rendered "No heap contents for this step." on **12
  steps across 5 problems** where the heap was genuinely, correctly empty.
  `task-scheduler` was 8 of its 15 steps — and "nothing is schedulable, every remaining task
  is still cooling down, the CPU sits idle" beside an empty heap is precisely that problem's
  lesson. A correct trace looked like a broken canvas.
- **Root cause:** `heapSlots` returned `{slots: [], source: null}` for an empty heap and for
  a step that never mentioned one, so the canvas could not tell them apart. It reported both
  as missing payload.
- **The near-miss worth recording:** the surrounding shape — a structure absent from most
  steps of a trace — is RCA-034/035, which this audit pass had already found three times
  (RCA-037, and twice more in Greedy). Applying that fix here, carrying the last heap
  forward, would have shown `task-scheduler` a heap that the algorithm had *just drained*,
  on the exact steps where its emptiness is the point. Reading the tracer first is what
  separated them: every one of the 12 steps calls `.array(toArray(heap))` or
  `.arrayState(render(heap))` and passes an empty heap. **Absence and emptiness need
  opposite fixes, and they look identical from the payload alone.**
- **Resolution:** `heapSlots` keeps the source when the field was stated but empty, and the
  canvas says "The heap is empty." for that case, reserving "No heap contents for this step."
  for a step that stated neither field.
- **Regression guard:** `HeapCanvas.test.jsx`, "distinguishes an empty heap from a step that
  never mentioned one", proven RED first.

## RCA-044 — Two tracers ran a different algorithm from the one on screen beside them

- **Discovered:** 2026-09-13, by the Heaps & PriorityQueue audit
- **Status:** Resolved
- **Symptom and impact:** `merge-k-sorted-lists` showed
  `PriorityQueue<ListNode> heap ... heap.poll() ... heap.add(smallest.next)` in its code
  panel and executed a **linear scan of three heads** for the minimum. Eighteen steps, and
  not one of them said the word "heap" — the structure the problem exists to teach and the
  reason it is catalogued under Heaps. `min-cost-connect-sticks` was the same shape: code
  panel `PriorityQueue.poll()`, trace a sorted `ArrayList` with `remove(0)`.
- **Root cause:** with exactly three lists, a linear scan returns the same minimum in the
  same order as a heap, so the trace was *correct* and every test passed. `AlgorithmTracer`'s
  contract says `run` "executes the algorithm for real", and nothing enforces that the
  algorithm it executes is the one `annotatedCode()` shows. The anchors line up either way,
  because both versions have a "pick the smallest" line and an "append" line.
- **Resolution:** both now run a real `ArrayHeap` (see RCA-042), and the narration names it:
  the root being popped, the head that takes its place, the heap shrinking when a list runs
  out, and the empty heap as the termination condition. Merged output is unchanged.
- **Two narration bugs this introduced and the golden caught**, which is what golden files
  are for: "push list 2's next head in its place" was emitted on the steps where that list
  was exhausted and nothing was pushed, and "the smallest of the 1 live heads ... without
  comparing them" was both ungrammatical and vacuous. Regenerating a golden without reading
  it would have shipped both.
- **How to find the next one:** read `annotatedCode()` beside `run()` and ask whether the
  same data structure appears in both. A tracer whose narration never names the structure
  its code panel is built around is the tell — `merge-k-sorted-lists` said "smallest head
  among the three candidates" eighteen times.
