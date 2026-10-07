# DP solution approaches: implementation and UX plan

Status: **implementation in progress: D0–D2 verified; D3–D7 pending.**
Prepared 2026-10-07 against merged main `9d0aa10` (PR #208).
This is a separate feature from the unfinished P8/P9 revamp gates; it does not close them.
The owner authorized implementation on 2026-10-07 after merging PR #210.
The original plan-only stop instruction has been superseded.

## 1. Outcome and boundaries

For a DP problem, let a learner follow the same solution through:

1. **Recursion:** express the state, base cases and recurrence; see repeated subproblems.
2. **Memoization:** keep that recursive structure and reuse previously computed states.
3. **Tabulation:** evaluate the same dependencies in a valid bottom-up order.

These must be real implementations, not three labels on the existing trace or three
generated explanations. Each offered approach has its own anchored Java source,
execution trace, complexity, visualization semantics and safe input limits.

Keep the existing single-canvas workspace, input editing, playback, Code, Analysis,
history, saved inputs, alternate-input comparison, share links and progress intact.
No simultaneous three-column workspace, new npm dependency, new problem IDs, or
mass regeneration of existing goldens is required.

The three approaches are the initial learning path, not a claim that every algorithm
has three equally useful forms. Preserve existing space-optimized or non-DP solutions;
do not rename a rolling-array, histogram or greedy solution “tabulation” inaccurately.

## 2. Verified starting point

Read `CLAUDE.md`, all six gates in `REVIEW.md`, RCA-054/RCA-056, the session contract
in `IMPLEMENTATION_HANDOFF.md` §6, and the owner concept before implementation.

The live catalogue on 2026-10-07 reports:

| Inventory | Observed |
| --- | --- |
| Dynamic Programming category | 55 problems |
| Renderers within that category | 53 DpTable, 1 Array, 1 Matrix |
| Array exception | `max-rectangle-area-all-ones` |
| Matrix exception | `count-square-submatrices` |
| DpTable outside that category | `count-palindromic-subsequences`, under Strings |

This yields **56 initial review candidates**, not 56 guaranteed three-approach
implementations. Regenerate the inventory from the live catalogue/registries when work
starts. Do not use category counts or renderer type as proof of a particular algorithm.

Current architectural constraints:

- `TracerRegistry` accepts one canonical `AlgorithmTracer` per problem ID and rejects
  duplicates. Registering three same-ID Spring tracer beans would break startup.
- `ProblemsController` selects one tracer by ID. Its default-run cache currently keys
  by ID and requested encoding; approach identity must be included after this change.
- `TraceRunner` owns input validation, anchors, step/byte budgets and real execution.
  All approaches must pass through it, including comparison and default execution.
- `ExecutionTrace`/`TraceResponse` already carry problem ID, source, anchors, resolved
  input and truncation; they do not yet carry approach identity or approach complexity.
- `useProblemSession`/`useTrace` commit a run atomically. Their current request/run
  identity is problem-based; introducing approaches requires extending that identity.
- `useShareableView` is the single URL writer. Do not create a second approach writer.
- Code/Analysis currently consume merged problem metadata. They must not show the
  selected candidate approach's source or complexity over a retained prior run.
- `useComparisonTrace` currently compares default input with alternate input. That
  feature must remain distinct from comparing approaches on the same input.
- Existing RecursionTree/DpTable renderers are useful building blocks, not proof that
  all required memo-cache and dependency semantics already exist.

## 3. UX: one solution selector, one active workspace

### 3.1 Primary interaction

Place one compact labelled **Solution approach** selector in the stage setup area,
near Edit input, above the canvas. Keep the three existing view tabs unchanged.

- Desktop: a compact native select initially; do not add another wide tab rail.
- Phone: the same labelled control, wrapping or full-width when needed, minimum 44px.
- Only render the selector for a problem with multiple genuinely available approaches.
- Available options follow Recursion → Memoization → Tabulation when those exist.
- Preserve a correctly named existing optimized approach where necessary; an Advanced
  disclosure can contain extra approaches after the initial three-form feature works.
- Show a one-sentence purpose for the selected approach; longer teaching material lives
  in an optional “How this approach works” disclosure, not permanently above the stage.

Changing this selector is **preparation, not execution**. Pause playback and retain the
last valid run. When the candidate differs, show a clear “Selected: Memoization;
showing: Tabulation” notice and an explicit **Run memoization** action. No extra button
is needed when candidate and displayed approach match. A successful run updates all
run surfaces together and starts at step 1; a failed run keeps the prior run labelled.

On first visit, retain the existing canonical approach as the default. Do not impose
exponential recursion as the new default for established users or existing links.
Offer an optional “Learn step by step” journey that starts with recursion on a safe
example. It must never silently shrink the user's input to make recursion runnable.

### 3.2 Teaching the actual transformation

Use the same state definition and recurrence across the three solutions. Explain:

| Approach | Show and explain |
| --- | --- |
| Recursion | Function parameters, base cases, branch choices, returned values and genuinely repeated states |
| Memoization | Same recursive state, cache key, unknown/known cache entries, cache lookups/hits/stores and returned values |
| Tabulation | Base-cell initialization, dependency direction, loop order, reads/writes and answer extraction |

Code remains the actual solution source with anchors; annotations explain why a change
works. Analysis shows the current call stack/cache/table as applicable and the correct
approach-specific time and space costs, including recursive stack space.

Examples of explanations: “This state was reached earlier,” “Return the stored value,”
and “These dependencies are already known because of the fill order.” Display them
only when supported by actual emitted events, not by parsing narration text.

### 3.3 Comparison without crowding

Add **Compare approaches** only after the single-approach journey is reliable:

- On demand, compare at most two approaches at a time, using the same input snapshot.
- Summarize answer, calls/state evaluations/cache hits where actually instrumented,
  trace size and theoretical complexity; keep diagrams optional and one primary canvas.
- Phone: stacked labelled summaries, not squeezed side-by-side canvases.
- Never synchronize traces by matching step numbers: different approaches have different
  events. Use independent controls or explicitly labelled semantic checkpoints.
- Preserve **Compare inputs** as a separate action. Existing comparison must use the
  displayed run's approach, not quietly fall back to the canonical approach.
- Do not present server latency or animation step count as algorithm runtime complexity.
- Out-of-limit comparison input is refused honestly; offer an explicit smaller example
  without changing the main draft/run or pretending the inputs were identical.

## 4. Backend and API contract

### 4.1 Approach definitions and discovery

Keep the canonical `TracerRegistry` and 431 unique catalogue IDs unchanged. Introduce
a separate approach registry keyed by **(problemId, approachId)**, with immutable
definitions containing: stable ID/label, canonical/default flag, teaching summary,
input spec, visualization type, complexity, annotated source and the executable tracer.

Recommended initial implementation: approach providers own non-bean `AlgorithmTracer`
instances, while the canonical tracer is adapted into its actual audited approach.
Only providers are discovered as new Spring beans; alternative tracers must not be
added to Spring's existing `List<AlgorithmTracer>`. This reuses `TraceRunner` without
loosening canonical duplicate detection. If a shared executable interface becomes
necessary, refactor it in its own tested change rather than weakening the registry.

Startup/contract checks must reject duplicate pairs, orphan problems, missing defaults,
blank source, incorrect renderer declarations, invalid default/alternate inputs and
unbounded approach specs. Treat definitions and shared snapshots as immutable.

### 4.2 Additive API proposal

Keep existing URLs and input-body shape working:

| Request | Proposed behavior |
| --- | --- |
| `GET /api/problems/{id}` | Existing detail plus available-approach summaries and defaultApproachId |
| `GET /api/problems/{id}?approach=memoization` | Selected approach detail: actual source/spec/type/complexity, clearly identified |
| `GET /api/problems/{id}/execute?approach=memoization` | Real selected approach on its declared defaults |
| `POST /api/problems/{id}/execute?approach=memoization` | Existing plain input map validated against the selected approach |
| `GET /api/problems/{id}/input-spec?approach=memoization` | The selected approach's authoritative bounds |

The optional `approach` query is orthogonal to the existing encoding query. Omission
uses the unchanged canonical/default behavior. Add approachId, approach dsType and
approach complexity to the committed execution response alongside its existing code,
anchors and input; Code/Analysis must not reconstruct these from mutable selection.

Unknown problem remains 404; untraced canonical problem remains 501. A recognized
problem with an unknown/unavailable approach returns a documented client error with
an explicit explanation and available IDs. It must never execute a substitute approach.
Invalid input remains an ordinary field-level 400. Distinguish unavailable approach
from a known approach whose selected input exceeds safe limits.

Resolve and validate approach/encoding before default-cache lookup. Key default traces
by canonicalized (problem, approach, effective encoding); never cache caller-supplied
inputs. Bound the key space to registered approaches and supported encodings.
Preserve execution rate limiting on every approach request.

### 4.3 Execution correctness and safety

- No hand-authored trace masquerading as execution; no delegating recursion to tabulation.
- Memoization must check/store the actual recursive state's key and avoid recomputation
  on cache hits. Tabulation must implement the same recurrence in a justified order.
- Source must show the actual recurrence/branches, not generic placeholder helpers.
- Keep normal step and byte budgets, selected-approach input caps, and cancellation/
  interruption safeguards. Instrument recursion often enough that budget checks bound
  computation, not merely the number of final output steps.
- Measure safe recursion bounds per problem. Some input-dependent branching requires
  combined constraints, not just independent maximum field sizes.
- Validate each approach's largest advertised input completes within trace budgets.
  Unexpected truncation remains visibly incomplete and cannot mark progress/completion.
- Preserve overflow behavior, Unicode rules, zero/base cases, tie-breaking and result
  reconstruction. Equivalent optimal answers may differ: validate feasibility/optimality
  rather than falsely requiring identical reconstructed strings or chosen subsets.
- Unknown memo entries are unknown, not zero. Count distinct computed states separately
  from function invocations and trace events if those counters are offered.

## 5. Session, draft and URL rules

Extend the existing contract; do not add independent state copies in view components.

1. Distinguish selected candidate approach from the approach in the committed run.
2. Request identity includes problem ID, approach ID, request generation and navigation
   identity. Re-check after response body decoding. Retire work on switch/unmount.
3. Atomically commit approach, source, complexity, type, steps, anchors, resolved input,
   submitted snapshot and truncation. A response for recursion cannot land as memoization.
4. Approach selection alone never overwrites the draft, commits a trace or updates the
   share link. Preserve entered values; show newly applicable bounds without clamping.
5. Seed shared/restored input before executing; do not overwrite edits made while waiting
   for approach metadata. Keep source scroll/follow state keyed to problem+approach.
6. One playback controller and one primary canvas remain. View/subview/theme/layout
   changes do not execute again. Approach changes pause; only explicit Run executes.
7. A successful approach run resets the step and source follow context. Failure retains
   the prior approach's code/complexity/trace/URL together with a clear failure notice.
8. The single URL adapter writes approach+input+step+view together on successful commit.
   Proposed public link: `/problem/climbing-stairs?approach=memoization&input=…&step=3&view=code`.
9. Restore in order: resolve approach → obtain spec → validate/execute input → apply step
   → resume mirroring. Invalid/unavailable approaches do not borrow another run's step;
   explain the refusal and offer an explicit canonical-run action.
10. Old links without approach remain compatible. Back/Forward and same-page external
    navigation restore committed approach identity, not the pending selector choice.
11. Avoid new unbounded per-approach trace caches. Start with one committed run; preserve
    drafts/presets through existing ownership. Saved inputs remain problem-scoped when
    fields are compatible, with revalidation against the selected approach.
12. Existing problem-level watched/starred semantics remain; no visit/selector change or
    truncated/offline/failed run marks completion. Per-approach progress is optional later.

## 6. Visualization contracts

- Recursion: actual call tree and current stack; repeated calls have distinct call IDs
  even when their arguments match. No collapsed state-DAG pretending to be a call tree.
- Memoization: the real call structure plus an optional stable memo-state companion;
  cache hits are marked and do not invent recursive children that were never visited.
- Tabulation: cells show genuinely initialized/computed values, dependency reads and
  writes. The evaluation order and answer cell are visible and explained.
- Use existing renderer/companion contracts where truthful. Add structured events/model
  fields only where needed, with full/delta round-trip tests and text alternatives.
- Derive companion presence from the run, including empty states; retain last-payload
  semantics. Cache/table panels must not blink or fabricate data on missing payloads.
- Keep large trees/tables locally scrollable, fit/reset controls keyboard-accessible where
  present, and full values inspectable. No page-wide overflow or hidden earlier calls.

## 7. Ordered implementation packages

One small/medium PR at a time, each independently verified and merged before the next.
Ask before publishing as required by CLAUDE.md; do not silently roll this into P8/P9.

### D0 — Inventory and classification

Generate an approach coverage ledger from the current catalogue and canonical registry.
For every candidate record the actual current implementation, recurrence/state, proposed
forms, reconstruction needs, safety bounds, renderer requirements and applicability.
Review category exceptions and related problems outside the category explicitly.

**Gate:** every candidate classified; no assumption that a dsType implies an algorithm.

### D1 — Additive backend contract

Implement approach providers/registry, metadata, optional API selection, bounded cache
keys and execution identity. Adapt canonical behavior without changing existing results.
Add strict no-fallback, duplicate/default, encoding, caching and validation tests first.

**Gate:** old API tests/goldens stay green; no catalogue/stats regression; invalid
approaches never run an alternative. No new options advertised without real executions.

### D2 — Backend pilot: Climbing Stairs

Keep the existing real tabulation. Add genuine recursion and memoization using the same
state/base cases. Annotate actual source; instrument calls/cache events; measure caps.
Verify common safe inputs with an independent reference and examples, including `n=1`.
Check each approach's largest supported input and one over its bound.

**Gate:** equal correct answers on shared valid inputs, materially distinct execution,
memo cache hits proved, source anchors correct, advertised limits finish safely.

### D3 — Pilot session and selector

Extend session/request/URL identity before adding selector markup. Bind Code/Analysis to
committed execution metadata; implement selected-vs-shown notice, explicit Run, loading,
retained-run errors, approach-aware presets/comparison and old-link compatibility.

**Gate:** a complete Climbing Stairs journey through all approaches works by mouse,
keyboard and phone; share/reload/Back restore the exact approach/input/step; race tests
fail on prior problem-only identity. No re-execution from presentation changes.

### D4 — Teaching and optional two-approach comparison

Add concise recurrence/base/cache/fill-order explanations backed by emitted events.
Add on-demand two-approach comparison separately from Compare inputs, with explicit
common-input limits and no ordinal synchronization or unsupported performance claims.

**Gate:** independent counters/answers are correct; failure/partial comparison does not
alter the main run; no comparison fetch until requested; phone layout remains spacious.

### D5 — Roll out by recurrence family

Use the generated ledger, not a hand-maintained ID switch. Proposed order:

1. One-dimensional recurrence examples: frog jump, non-adjacent sum and related states.
2. Grid/path recurrences: demonstrate boundaries and valid row/column dependency order.
3. String alignment/matching: LCS, edit distance and applicable related problems.
4. Subsequence/sum/knapsack/coin families: zeros, duplicates, item reuse and counting.
5. Stock/LIS families: multidimensional state; preserve optimized existing alternatives.
6. Interval/partition families: interval length order, split choices and reconstruction.
7. DP-adjacent/category exceptions: audit independently; mark inapplicable forms with a
   reason instead of forcing an unrelated algorithm into the three-form template.

Each batch gets independent answer references, boundary budgets, anchor/shape tests,
real-backend desktop/phone journeys, and coverage-ledger updates. Split difficult
reconstruction or input-safety changes into separate reviewable PRs.

**Gate per batch:** every newly advertised approach is implemented, served, selectable,
correct and tested. A hidden backend implementation or static source is not completion.

### D6 — Accessibility, responsiveness and performance

Run five standard widths, short landscape and 200% real browser zoom in both themes;
second engine and real device where available. Include maximum bounded recursion trees,
memo tables, long titles/values and all input editors used by approaches.
Check keyboard ownership/focus, visible labels, 44px targets, contrast, reduced motion,
text alternatives, manual narration and a real screen-reader journey.
Measure production decode/render/play/seek/history/view-switch performance on the same
machine/build mode as baseline. Record trace sizes, counters, CPU/browser and raw/gzip
bundle deltas; investigate repeated >200ms stalls instead of hiding them.

**Gate:** no critical task failure or loss of run identity; omissions are recorded and
not silently treated as accessibility/performance sign-off.

### D7 — Coverage reconciliation and release

Complete the per-problem/per-approach ledger, update API/product docs and tours/help,
remove pilot flags/dead adapters only after import checks, and run complete frontend,
backend, build and startup suites. Record default behavior, migration/rollback commit,
known omissions and retained P8/P9 gates. Land only after authorized publication and CI.

**Gate:** every shipped option matches its code/trace/complexity; old links/non-DP
features still work; no falsely completed three-approach coverage claim.

## 8. Required regression matrix

| Area | Tests that must notice a broken implementation |
| --- | --- |
| Identity | Rapid approach runs; approach/problem switch during fetch/body decode; unmount; StrictMode |
| Retained run | 400/network/malformed/empty response keeps prior approach source, trace, complexity, input and URL together |
| Draft | Late spec, tighter recursion limits, dirty input, saved preset, selector/view changes, restoration pending |
| URL | No approach old link; valid/invalid/unavailable approach; custom input; out-of-range step; Back/Forward; same-tick writes |
| Backend | Registry duplicates/orphans/defaults; selected spec validation; no fallback; cache separation; budget boundary; both encodings |
| Algorithms | Independent reference; same mathematical problem; negative/zero/base/tie cases; valid reconstruction; actual memo reuse |
| Views | No run on Code/Analysis/theme/split/Focus changes; scroll/follow isolation; one timer/canvas; error-boundary reset per run identity |
| Comparison | Explicit same input across approaches; different input within the same approach; retired responses; failure/limits; no unsolicited fetch |
| Completion | No mark-watched or completed state for pending, failed, malformed, truncated or offline work |
| Accessibility | Native select keys not stolen; Run focus; 400 summary focus; no clipped/sticky-hidden focus; announcements; full text alternatives |

Prove regression tests fail against the prior implementation where applicable, using a
temporary worktree/controlled patch rather than destructive changes to the user's branch.
Run the six review roles for each PR. Independent correctness checks must not merely
compare all three approaches to each other: shared mistakes can agree perfectly.

## 9. Effort, milestones and stop conditions

This is a **backend + session + teaching feature**, not a small cosmetic selector change.
Planning estimate, to revise after D0/D2:

- Inventory/contract decisions: 1–2 engineering days.
- Complete real pilot with safe execution, UI/session/sharing/tests: roughly 4–7 days.
- Broad candidate coverage: multiple weeks, depending on applicable forms,
  reconstruction, trace budgets and independent verification. Do not promise all 56
  candidates in one short session or one PR.

First release milestone: Climbing Stairs has all three genuine forms, an uncluttered
selector, truthful teaching, correct share restoration and meaningful error handling.
Second milestone: add a grid and a string example to validate that the design generalizes.
Only then commit to the larger family rollout estimate.

Stop and request direction if an approach cannot safely support the advertised input
contract, cannot truthfully reuse a renderer, changes the problem definition/tie contract,
requires a new dependency, or would remove an existing approach/feature. Record “not
yet implemented” separately from “not applicable”; never fake coverage to close a phase.

## 10. Implementation handoff

When authorized to start: read this plan and the repository review/session rules, refresh
main/live inventory, create a new feature branch, and execute D0 → D1 → D2 → D3 in
reviewable packages. Complete the pilot before expanding to D4/D5. Preserve the canonical
API, catalogue IDs, existing defaults and all successful-run honesty invariants.

## 11. Implementation progress — 2026-10-07

| Package | Current status | Evidence / boundary |
| --- | --- | --- |
| D0 | Classified | `dp-approaches/classifications.tsv` and generated `inventory.json`: all 56 candidates, current source digests, canonical specs/complexity, actual state/transition, reconstruction and applicability |
| D1 | Verified | Provider registry, canonical adapters, optional API selection, committed execution metadata, normalized cache keys and refusal tests |
| D2 | Verified | Genuine recursion/memoization/tabulation, independently compiled source, oracle and boundary tests; `dp-approaches/d2-results.json` |
| D3 | Pending | Selector, committed approach identity, sharing/restoration, Code/Analysis binding and UI pilot |
| D4–D7 | Pending | Teaching/comparison, family rollout and final release gates |

Inventory distinctions: binary-search LIS remains an optimized default; ninja-and-friends
uses rolling row slices; rectangle-all-ones uses histogram stacks rather than a DP table;
count-square-submatrices is real in-place DP rendered as Matrix; palindromic-subsequence
counting lives under Strings. Alternative safety limits are explicitly unmeasured.

Print LIS still displays undefined `collect()`/`answer()` helpers. Its real parent-chain
algorithm is classified, but the displayed-source repair is a prerequisite for its D5
batch. The preceding repair certified only its known 27-source family, not all sources.

D1 keeps omitted-approach requests on the existing executable. Climbing Stairs exposes
one audited `tabulation` definition; other problems identify their existing executable
as `canonical` / “Current solution” without guessing algorithm types. No extra solution
option or selector is advertised until it has a real implementation. The frontend pilot
and approach-aware shared URLs remain D3 work.

The API accepts `approach` on detail, input-spec, GET execute and POST execute. Unknown
or empty approach values are refused with `unavailable_approach` and available IDs;
unknown problems remain 404 and untraced problems 501. Supported encodings are `delta`
and `full` (case-insensitive), with omission meaning delta. Unsupported encoding strings
now return `unsupported_encoding` rather than silently choosing delta. Validated effective
encoding and approach ID enter the bounded default cache; custom inputs never do.

Regression evidence so far: 7/9 new API cases failed against the prior implementation
(the other two preserved existing validation/404 behavior). A separate array-ceiling
guard failed before typed ceiling validation. Registry checks cover duplicate pairs,
orphan/incorrect IDs, missing/multiple/default substitution, missing renderer/source,
unsafe specs, invalid inputs, frozen metadata and real selected-executable dispatch.

Final D0/D1 verification: 7,999 backend tests, zero failures/errors, 500 skipped;
76 frontend files / 678 tests passed; production build passed (JS 364.60kB, gzip 116.09;
CSS 103.99kB, gzip 18.63). Startup cleanup smoke passed. No dependencies, algorithm
results, canonical sources or golden fixtures changed in this package. The 32 added
tests include inventory/source drift detection, registry safety and API/cache seams.
The real-backend `dp-approaches/d1-journey.cjs` inspected all 431 canonical detail
identities, both encodings, custom input and refusal responses; four Chromium rows
covered 320/1366px in both themes, source display, Code→Analysis step retention,
page overflow and page errors. Results are in `dp-approaches/d1-results.json`.
That probe is a historical D1 baseline: its one-option/refused-memoization assertions
are intentionally superseded by D2, not current acceptance assertions.

### D2 verification

Climbing Stairs now exposes real Recursion, Memoization and Tabulation, with Tabulation
still the default. Alternatives are not canonical tracer beans. Their recursive state
is `ways(n)`, with `ways(0)=ways(1)=1`; memoization adds an actual nullable cache.
Every call has a unique ID, and cache-hit events return without expanding children.
Each option owns a complete standalone Java 17 class with reachable source anchors.

| Approach | Advertised `n` | Largest trace | Full / delta bytes | Answer at maximum |
| --- | --- | --- | --- | --- |
| Recursion | 1–10 | 709 steps, 177 calls | 358,446 / 204,990 | 89 |
| Memoization | 1–30 | 238 steps, 59 calls, 28 hits, 31 computed states | 435,653 / 230,064 | 1,346,269 |
| Tabulation | 1–30 (unchanged) | 32 steps | 57,391 / 51,801 | 1,346,269 |

All advertised integers agree with an independent combinatorial oracle, and compiled
displayed classes agree as well. Max+1 is refused for each form. A deliberately reduced
3-step budget stops recursive expansion without an answer. Nine real HTTP full/delta
pairs round-trip through the production frontend decoder; only absent null top-level
fields are normalized in comparison (empty stacks and all memo snapshots must match).
Four canonical UI rows cover 320/1366px and both themes; no overflow/page errors.

Only the Climbing Stairs canonical golden is regenerated. Parsed before/after data is
identical after removing source, anchors and active lines; the recurrence, snapshots,
answer and narration do not change. Inventory source digest refreshed accordingly.
Full verification: 8,007 backend tests, zero failures/errors, unchanged 500 skips;
76 frontend files / 678 tests; production build unchanged at JS 364.60kB (gzip 116.09),
CSS 103.99kB (gzip 18.63). Six of the initial seven pilot tests failed meaningfully
against D1 (three assertions, three unavailable-approach errors); the canonical-anchor
case preserved existing behavior. No dependency added.

This is the backend pilot, not the complete user journey: the selector, committed
source/type/complexity, approach-aware sharing/presets/comparison and memo-cache UI
remain D3. The other 55 candidates' alternatives remain unimplemented/unmeasured.
