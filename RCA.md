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
- **Status:** Open — deferred to visualization Phase 3
- **Symptom and impact:** activating a real trie trace would not connect/render nodes
  correctly: the canvas expects `char`, `isEnd`, and child-id arrays, while the backend emits
  `character`, `endOfWord`, and character-to-id child maps.
- **Root cause:** the canvas was created before live trie transport was wired and has never
  been exercised by a tracer.
- **Resolution required:** define one JSON shape at the decoder/canvas boundary, add a fixture
  from the real backend serializer, then activate the canvas. Do not hide the mismatch with
  catalogue defaults or an Array fallback.
- **Regression guard required:** a backend-shaped trie fixture that renders its exact node and
  edge set, plus empty/malformed payload cases.

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
