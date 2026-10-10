# DP solution approaches: implementation and UX plan

Status: **D0–D4 merged; first D5 candidate (Frog Jump) merged #216 / `4ad9fc3`; 54 other candidates and D6/D7 pending.**
Current repository work: early P9 cleanup merged #218 / `089cdd9`; the resumed completion goal starts bounded layout/accessibility polish before further DP batches.
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

## 11. Implementation progress — reconciled 2026-10-09

| Package | Current status | Evidence / boundary |
| --- | --- | --- |
| D0 | Classified | `dp-approaches/classifications.tsv` and generated `inventory.json`: all 56 candidates, current source digests, canonical specs/complexity, actual state/transition, reconstruction and applicability |
| D1 | Verified | Provider registry, canonical adapters, optional API selection, committed execution metadata, normalized cache keys and refusal tests |
| D2 | Verified | Genuine recursion/memoization/tabulation, independently compiled source, oracle and boundary tests; `dp-approaches/d2-results.json` |
| D3 | Verified pilot | Explicit selector, committed metadata, approach-aware links/presets/input comparison, truthful memo/recursive views; `dp-approaches/d3-results.json` |
| D4 | Merged #215 / `2709d8b` | Audited teaching metadata, isolated two-approach comparison, passing PR/post-merge CI; dated D4 evidence below |
| D5 | First candidate merged #216 / `4ad9fc3` | Frog Jump code/API/full suites and28 final browser rows pass; remaining54 candidates not implemented |
| D6/D7 | Pending | Broad accessibility/performance, coverage reconciliation and release gates |

Inventory distinctions: binary-search LIS remains an optimized default; ninja-and-friends
uses rolling row slices; rectangle-all-ones uses histogram stacks rather than a DP table;
count-square-submatrices is real in-place DP rendered as Matrix; palindromic-subsequence
counting lives under Strings. Alternative safety limits remain unmeasured except for
the merged Climbing Stairs and Frog Jump packages documented below.

Print LIS's undefined `collect()`/`answer()` helpers are repaired in the locally verified
2026-10-10 complete-source helper batch, now owner-authorized for publication after CI.
Its original parent-chain recurrence, strict tie updates and limits are unchanged;
the exact displayed standalone Java compiles and agrees with traces and independent cases.
This resolves the canonical source prerequisite only, not its D5 alternative forms.
The preceding 27-source repair and this five-listing batch do not certify all sources.

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

At the D2 handoff this was the backend pilot, not the complete user journey: the selector, committed
source/type/complexity, approach-aware sharing/presets/comparison and memo-cache UI
were D3 work, verified below. The other 55 candidates' alternatives remain unimplemented/unmeasured.

### D3 verification

The Climbing Stairs user journey now offers a native labelled selector in all three views.
Changing it pauses and prepares, without executing or changing the draft/run/share link.
A selected-vs-shown notice and explicit Run make the boundary clear. Successful runs
atomically carry source, anchors, type, complexity, approach and input; Code and Analysis
read that committed metadata, never the candidate. Missing input/truncation metadata and
substituted problem/approach identities are rejected; absent complexity stays unavailable.

`approach` joins the one existing URL writer. Default approach is omitted; old links remain
canonical. Restoration runs the requested approach/input before seeking. Unavailable or
rejected links drop their step, label the refusal and share only the actually retained run.
Same-page canonical navigation, Back, StrictMode, failed default loads, body-decode races,
late detail and user edits during restoration are covered. Retry keeps its original approach
and input. Presets run the candidate with its own bounds; Compare inputs uses the displayed
approach and that approach's declared alternate (now served in the summary).

Memo tables are real step snapshots, disclosed on demand in Playground/Code/Analysis.
Unknown is distinct from zero. Recursion call/cache state has a text alternative. Returning
an empty stack now finishes every tree frame. Other approaches cannot inherit canonical
demo arrays/trees. Fixed-coordinate recursion SVGs keep their actual width and scroll in a
keyboard-accessible region capped at 480px high; arrows scroll there rather than seeking.
Source follow/scroll persists for preparation and view changes, but resets for a successful
change of source identity. Only one primary stage mounts at a time.

Evidence: real-backend Chromium `d3-journey.cjs` exercises all three algorithms, source and
complexity, selected limits, HTTP400 focus/retained link/draft, shared approach/input/step
restoration, and no re-execution from presentation at five widths (320/390/768/1366/1440)
in both themes. Largest recursion n10 and memo n30 are checked at 390 dark /1366 light:
native SVG widths7,920/2,728px stay intact, both ends are locally reachable, and the scroll
region is480px, with no page-wide overflow. Screenshots inspected and retained beside the
results. Selector target is44px or larger; existing token/focus/theme guards remain green.

RED evidence:14/15 original approach-session cases failed against D2; all six original UI
cases and the recursive-state summary failed before selector/parity wiring. Extra regressions
failed for canonical demo inheritance (two), empty-stack completion (one), native scroll key
ownership (one), missing input/truncation metadata (two), and selected alternate-input JSON
(one API case). The tightened geometry probe failed: a7,920px tree was clipped into226px
with no horizontal scroll. Assertions were strengthened after screenshots exposed it.

This verifies one pilot, not all56 problems, cross-engine/real-device or human learning gates.
D4 teaching/two-approach comparison and D5 family rollout still follow; D6/D7 broad polish,
performance, ledger reconciliation and release remain open, as do P8/P9's original gaps.

Final verification: 78 frontend files / 706 passing tests (28 more than D2), production
build JS 371.55kB (gzip 118.32), CSS 104.90kB (gzip 18.77); 8,007 backend tests,
zero failures/errors, unchanged 500 skips. The backend API test count is unchanged:
an existing case now checks selected alternate-input metadata. The live matrix has ten
rows and four maximum-tree geometry checks. Viewports are 900px high; this is not a
certification of the original 768px-high stage-position target. No dependencies added.

### D4 verification — 2026-10-08

The pilot definitions now publish audited state, base cases, recurrence, evaluation order
and memo key. Current-step notes are keyed to emitted event values or real source anchors,
not guessed from narration. The closed-by-default teaching disclosure describes the shown
approach and sits after the transport/inspector, keeping the canvas's starting position.
Canonical adapters without audited teaching stay silent and add no empty spacing row.

Compare approaches is separate from Compare inputs. Opening it, changing its pair, seeking
its sliders or switching views does not execute. Run comparison explicitly snapshots the
displayed run's resolved input for at most two independently validated requests. It never
changes the main draft, run, playback position, progress or URL. Both declared limits are
visible; an out-of-limit side is refused without shrinking input, and the other side can
remain available. Retry is explicit. The panel persists through view changes, but a new
committed main run gives it a fresh identity and retires any old pending comparison.

Summaries show recorded answers, actual function/state/cache counters where emitted,
trace-event counts, response JSON UTF-8 bytes before compression, and returned theoretical
complexity. Uninstrumented counters and incomplete/uninstrumented answers are unavailable,
not zero. Neither event counts, JSON weight nor latency are called algorithm runtime.
Each side has its own labelled step slider; ordinal positions are never synchronized.
Phone summaries stack. No additional primary canvas, timer, endpoint or dependency.
Local playback keys stay local; modifier chords, including Ctrl/Meta+K, stay global.
The disclosure's aria-controls target remains present while closed, without mounting
comparison requests; its missing-target regression also failed before the fix.

The separate Compare inputs path now applies the same honesty boundary. It checks returned
problem/approach identity (including canonical), supported encoding, nonempty numbered
events, truncation status and all explicitly submitted input fields. Server-filled defaults
remain valid. Its pair commits atomically under problem/approach identity; closing or leaving
retires requests even after body decoding. Cut-short results keep their available events
with an explicit incomplete notice. Local keyboard controls cannot seek the main run,
while Ctrl/Meta+K still opens the switcher.
The input-comparison action waits for a committed, non-offline run rather than guessing
the executable during the first load; its early-open regression failed before the guard.

Final verification: 80 frontend files / 753 tests (47 new since D3), no frontend skips;
production build JS381.28kB/gzip121.58, CSS106.81kB/gzip19.07. Backend 8,008 tests /
zero failures/errors / unchanged 500 skips (rerun 19:47 UTC). Startup process-tree smoke
and the inventory source/spec guard also pass.
The refreshed 56-candidate ledger passes its source/spec guard; only date/pilot status
change. No source listing, tracer algorithm, golden, package manifest or lockfile changes.

Real backend Chromium matrix: ten rows at 320/390/768/1366/1440px, both themes, 900px
viewport height, repeated against the production preview. At n7 recursion records answer21/calls41, memoization answer21/calls13/
cacheHits5/computedStates8; tabulation does not invent missing counters. All rows check
same-input/draft/link isolation, independent positions/keyboard, view/pair preparation,
44px targets, phone stacking, partial refusal and no overflow/errors. Screenshots inspected.
Three additional production-preview rows cover 680×360 light, 390×900 dark, and denied
local/session storage at 390×900 light, with reduced motion, real cache-hit teaching,
keyboard-visible focus and global switcher availability.
The ten-row production-preview pilot journey also passes in Firefox 155.0 with inspected
desktop/light and phone/dark screenshots (`d4-firefox-results.json`, `d4-firefox-*.png`).
This is a pilot-level second-engine check, not broad-family/device/accessibility sign-off.
After the final guard, four further Firefox release-build rows at 320/1366px in both themes
also pass (`d4-firefox-release-results.json`). The earlier ten-row evidence is retained.

RED proof: four initial workspace cases and the API teaching case failed on D3. Removing
the post-decode generation guard makes both retired-body regressions fail. Empty teaching
spacing and swallowed Ctrl+K each failed before their fix. The evidence pacer's old glob
failed to match query-bearing execution URLs; its new pathname predicate passes a Node
behavioral test without altering the backend's normal rate limit. Final full suites have
no frontend skips; the targeted RED selections' deliberate skips are not coverage claims.

Continuation RED proof: nine initial input-comparison integrity cases failed on the old
hook, as did the cut-short display and main-player keyboard cases. Four further failures
proved unknown encoding/invalid ordinal/blank-narration checks in the comparison paths.
Removing the main-run comparison key makes the workspace pending-body/new-run case fail.
Two workspace cases also exercise closing a pending comparison and replacing its main run,
including late decoding after a newer comparison has already completed.

Evidence: `d4-journey.cjs`, `d4-results.json`, `d4-accessibility-journey.cjs`,
`d4-accessibility-results.json`, `d4-input-comparison-journey.cjs`,
`d4-input-comparison-results.json`, and two screenshots in `dp-approaches/`.
The four input-comparison rows use real canonical Two Sum and memoized Climbing Stairs
runs at 320px dark and 1366px light. They verify identity/input echoes, independent controls,
main step/link retention, keyboard ownership, switcher availability and no overflow/errors.
Truncated and empty response checks in that journey are explicitly injected faults, not
claims that accepted backend inputs actually exceed their budget.
Only the pilot's automated checks are verified. D5's other 55 candidates, broader-family correctness/budgets
and cross-engine/zoom journeys, real devices, screen readers, human learning and
production performance/release gates remain open. P8/P9 are not closed by this package.

### D4 continuation — 2026-10-09

Historical verification snapshot: this package subsequently merged as #215 / `2709d8b`
with passing PR and post-merge CI. Its local/pre-publication wording below describes
the checks before that merge, not the current publication status.

Verified on `feat/dp-approach-teaching`, based on merged D3 `fb96744`. The owner approved
commit/push/PR merge on2026-10-09 after local verification; CI must pass before merge.
No D5 implementation is stacked on this package.

Additional lifecycle/honesty hardening:

- Retained callbacks cannot run after unmount, cross problem/approach identity, or reopen
  a closed input comparison. Invalid pairs release pending state. Ownership and the
  alternate input are adopted only after commit; three real suspended-transition tests
  fail against render-time ref adoption.
- Both comparison paths reject malformed debug maps/values; recorded string values
  (including zero) and genuine absent/null variables remain supported.
- Teaching definitions reject blank required text/notes and nonexistent source anchors.
  The UI ignores invalid optional teaching without breaking the trace or leaving a blank
  row. Own-property notes only; ambiguous aliased source lines stay silent.
- Native keyboard testing reproduced a busy-button focus loss and sticky-rail occlusion.
  Busy submission uses aria-disabled/aria-busy plus an explicit guard, preserving focus
  without allowing another request. Comparison controls reserve 64px scroll clearance.

Latest full suites: **81 frontend files / 788 tests**, no frontend skips; **8,014 backend
tests**, zero failures/errors, unchanged 500 skips. Production JS382.11kB/gzip121.84,
CSS106.95kB/gzip19.10: versus D3, JS+10.56kB/gzip+3.52, CSS+2.05kB/gzip+0.33.
The launcher smoke and three Node evidence-tool checks pass. No displayed algorithm
source listing, tracer computation, golden, package manifest, lockfile or runtime
dependency change.

Bundle review trigger (§5/verification of the handoff): versus P0, final JS gzip is
**+12.4%** and CSS gzip **+18.1%**; versus D3 this package adds **+3.0% JS / +1.8% CSS**.
A separate temporary sourcemapped build leaves the served release untouched. Inspection
finds the existing React/router/icon/analytics libraries, no new runtime library; the four
new teaching/comparison/validation modules total12,031 unminified source bytes. Source-map
bytes are not emitted or gzip attribution. The addition is explicit same-input comparison
and audited teaching/lifecycle safety, not a performance improvement claim. Optional-panel
code splitting is a future profiling candidate, not implemented here: loading boundaries
must preserve pending/focus/run ownership. Long-trace timing and P8 performance sign-off
remain open; the >10% P0 trigger has been reviewed, not silently waived or marked passed.

Evidence tools now record the actually served JS/CSS asset names. Native 200% zoom uses
`chrome.tabs.setZoom/getZoom` in disposable bundled-Chromium profiles; it verifies a
1366×768 physical viewport becomes 683×384 CSS pixels, DPR2, with CSS zoom still1.
The five-width normal sweep remains distinct from the 640/780/1366/1440px physical
zoom sweep (320/390/683/720px CSS). Native captures verify full pixel dimensions without
overriding viewport metrics; the ordinary CSS-sized full-page clip was cropped and was
replaced. This is targeted pilot evidence, not a real-device/screen-reader certification.

Boundary journeys cover n1, recursive ceiling n10, memo/table ceiling n30, and an actual
n31 HTTP400. Answers use an independent iterative reference; calls/cache hits/computed
states are checked separately. A comparison after the rejection keeps input30, its main
step/link, and the rejected draft31/error intact. No input is silently shortened.

Final browser evidence: **99 completed rows in twelve result files**, all served
`index-D9b0gbod.js` / `index-DkVb_nYT.css`. This includes Chromium10 + Firefox10 at
common height900, native zoom8, keyboard4+4, reduced-motion/storage3, input-comparison4,
boundary12+12+12, and exact handoff sizes10+10. The exact-size Chromium/Firefox sweeps
cover 320×568, 390×844, 768×1024, 1366×768 and 1440×900 in both themes; separate
artifacts are `d4-standard-results.json` / `d4-firefox-standard-results.json` and
their inspected screenshots. Counts describe repeated pilot scenarios, not99 unique
problems. `noOverflowOrErrors` means page-wide overflow and uncaught page errors, not
every console/network message. No all-family or human-accessibility certification.

Verification exclusions: one repeat browser run ended with a closed browser (cause
unverified); its incomplete result is not counted. A targeted frontend run overlapped
dependency reinstall and lost a worker module; clean full reruns above supersede it.
A partial exact-size run lost the local backend (exit143, cause unverified); after a
serial restart both exact-size matrices pass20/20. The partial attempt is excluded.
`npm audit` reports nine inherited advisories (4 moderate/3 high/2 critical); dependencies
are identical to D3, and no forced or major-version upgrade is folded into this UI package.

### Next-package preflight — read-only, not D5 implementation

Historical preflight: D4 subsequently merged as #215 / `2709d8b`. The Frog Jump-only
package below implements this preflight; its remaining browser gates are still open.

Start only after D4 is approved, passes CI and merges; pull that merge before cutting the
next branch. The first D5 PR should cover **Frog Jump alone**, not all four 1D examples.
The existing canonical tracer accepts `heights` length2–20, values0–999, and starts at
index0 with energy0. Preserve that public contract and its existing tabulation/source.
Do not import Climbing Stairs' path-counting base cases into a minimum-energy problem.

1. Define `energy(i)` with base `energy(0)=0`; for `i=1`, only the one-step predecessor
   exists. Both recursive and memoized implementations must evaluate the same legal
   predecessors and add the corresponding absolute height difference. Repeated indices
   in recursion are separate calls; a memoized zero cost is a valid cached value.
2. Before advertising recursion, measure its worst-case call/event/byte counts by array
   length, with the unchanged global step cap. A canonical length20 allowance is not
   permission to run an exponential trace. Pick and publish a separate measured cap;
   preserve a longer entered draft and show a refusal, never truncate the input to fit.
3. Use independently enumerated valid jump paths as a small-input answer reference.
   Include `[5,5]→0`, `[0,999]→999`, `[10,50,40,30]→40`,
   `[30,10,60,10,20]→30`, equal-height/zero-cost inputs, tied candidates, and arrays
   where either predecessor wins. Test the eventual maximum and one over its bound.
4. Compile the full displayed Java17 methods independently, check every emitted anchor,
   full/delta equivalence, actual parent/call IDs, cache-hit-without-children behavior,
   and unknown-vs-computed memo cells. Do not label an energy result as a path count.
5. Add registry-owned teaching and genuine counters, then exercise the existing selector,
   Code/Analysis, restoration and both comparisons without adding another session owner
   or canvas. Shared arrays, refused recursion and a valid zero answer must remain honest.
6. Repeat desktop/phone, both-theme, keyboard and native-zoom journeys on the served
   production bundle, refresh only this ledger row, run all six review gates, then ask
   to publish this one PR. Only after its merge start the next candidate.

Follow-up hazards already checked in the current canonical source: K-distance Frog Jump
has a **joint length/K branching budget**, not just a length cap (current length2–16,
K1–8); non-adjacent sum allows a singleton and zero values (length1–20); circular House
Robber starts at length2 and requires both exclusion passes (length2–16). Its future memo
identity must include the pass/boundary or use separate caches, not borrow the first
pass's index-only results. These are implementation prerequisites, not measured new
approach limits or completed rollout work. The remaining 55 rows are still pending.

### Reproduce the D4 browser checks

From the repository root, after the full frontend/backend suites and production build,
run the backend on8923 and the Vite **production preview** on5180 in separate terminals.
Keep the build unchanged throughout the sweeps. Set `PLAYWRIGHT_MODULE` to an external
Playwright module with matching Chromium/Firefox installed; it is not a repo dependency.
The journeys require a served production JS/CSS bundle and retain its asset identity.
Run them serially; each respects the backend's unchanged60/minute execution limit.

```sh
# Normal five widths in both themes; historical common height900 stays a separate check.
node docs/ui-revamp/dp-approaches/d4-journey.cjs
AUDIT_ENGINE=firefox AUDIT_OUTPUT_TAG=release node docs/ui-revamp/dp-approaches/d4-journey.cjs
# Exact handoff sizes: 320x568, 390x844, 768x1024, 1366x768, 1440x900.
AUDIT_HEIGHTS=568,844,1024,768,900 AUDIT_OUTPUT_TAG=standard node docs/ui-revamp/dp-approaches/d4-journey.cjs
AUDIT_HEIGHTS=568,844,1024,768,900 AUDIT_OUTPUT_TAG=standard AUDIT_ENGINE=firefox node docs/ui-revamp/dp-approaches/d4-journey.cjs
# Native tab zoom, not emulated scaling. Widths are physical pixels.
AUDIT_NATIVE_ZOOM=2 AUDIT_WIDTHS=640,780,1366,1440 AUDIT_HEIGHT=768 AUDIT_OUTPUT_TAG=zoom-200 node docs/ui-revamp/dp-approaches/d4-journey.cjs
AUDIT_NATIVE_ZOOM=1 AUDIT_WIDTHS=320,1366 node docs/ui-revamp/dp-approaches/d4-keyboard-journey.cjs
AUDIT_NATIVE_ZOOM=2 node docs/ui-revamp/dp-approaches/d4-keyboard-journey.cjs
node docs/ui-revamp/dp-approaches/d4-accessibility-journey.cjs
node docs/ui-revamp/dp-approaches/d4-input-comparison-journey.cjs
node docs/ui-revamp/dp-approaches/d4-boundaries-journey.cjs
AUDIT_ENGINE=firefox node docs/ui-revamp/dp-approaches/d4-boundaries-journey.cjs
AUDIT_NATIVE_ZOOM=2 node docs/ui-revamp/dp-approaches/d4-boundaries-journey.cjs
node --test docs/ui-revamp/evidence/pace-executions.test.cjs docs/ui-revamp/evidence/served-build-identity.test.cjs
node docs/ui-revamp/dp-approaches/refresh-inventory.cjs
```

Only completed result files count as evidence. Exclude interrupted attempts and inspect
the captured phone/desktop/native-zoom screenshots. Exact-size and native-zoom overflow
checks do not close the pre-existing stage-y-position exception or any human/device gate.

## D5 first candidate — Frog Jump, local work on 2026-10-09

Historical preflight: this package subsequently merged as #216 / `4ad9fc3` with passing
PR and post-merge CI. Its local/publication-pending wording below records the earlier
verification state. There are now two merged candidates;54 other candidates remain open.
The owner's 2026-10-10 continuation starts a separate cleanup package, not the next DP batch.

This is one unpublished candidate package, not the entire 1D family. Preserve the existing
canonical tabulation tracer, source, input defaults and public `canonical` approach ID.
Its label is now truthfully Tabulation; old `?approach=canonical` links still resolve.
The new recursion and memoization are real implementations of minimum energy, not
Climbing Stairs path counting. Zero is a known cached cost, distinct from null/unknown.

| Approach | Published lengths | Measured upper-length shape | Actual calls / events | Full / delta JSON bytes |
| --- | --- | --- | --- | --- |
| Recursion | 2–10 | Alternating 0/999, length10 | 143 / 573 | 313,271 / 187,545 |
| Memoization | 2–20 | Alternating 0/999, length20 | 38 / 154 | 217,782 / 118,302 |
| Tabulation (`canonical`) | Existing 2–20 | Alternating 0/999, length20 | Not instrumented / 22 | 45,743 / 41,933 |

All accept height values0–999. The unchanged 5,000-step / 2,000,000-byte budgets apply.
The recursion length cap also protects the existing 220-node recursion renderer: length10
has143 calls, while length11 would have232. Accepted inputs must show every call, not
silently truncate the tree. Call/event counts depend on length; the measured JSON sizes
are for these shapes, not a proof of maximal bytes over every possible height array.
No latency or performance-win claim is derived from event counts or JSON sizes.

Evidence: `dp-approaches/frog-jump-measurements.json` contains19 completed real-API rows
(38 paced execution requests), full/delta equivalence through the actual frontend decoder,
answer oracles, stack depth and actual derived recursion nodes. Equal heights at length7
produce memoized answer0,12 calls,5 real cache hits and7 computed states. This evidence is
not a browser UI journey. `frontend/src/test/frogApproaches.json` is generated from the
three genuine complete default API responses, including their sources and anchors.

Backend tests independently enumerate forward jump paths for360 small inputs per form,
plus named cases, all accepted lengths, strict refusals, cache-zero behavior, real budget
retirement, Java17 compilation of displayed sources, API identity and emitted anchors.
Before implementation, all nine backend tests failed/errored against canonical-only main.
Two of the three array-limit presentation tests failed before the UI fix; the absent-limit
positive is not claimed as RED. New array-session integration tests cover legacy links,
an eleven-chip rejected recursion draft, shared memoized input/step/source restoration,
and comparison isolation from edited chips and the main step/link. Their real-API fixture
does not invent successful custom traces for inputs it has not recorded.

The comparison's existing scalar-bound copy now also displays declared collection length
and value bounds, including zero. After the browser continuation below, its inventory
status is **backend-and-UI-candidate-verified; family rollout pending**. This is local
verification, not publication or broad human/device acceptance.
No dependency, endpoint, session owner, primary canvas or canonical golden changed.
Canonical source/spec digests and all other candidate statuses remain unchanged.
Current checks: backend8,023 tests (0 failures/errors, unchanged500 skips; backend unchanged
since that run), frontend83 files /799 passing tests, production build JS383.15kB/gzip122.13
and CSS106.95/gzip19.10. The additional four RED viewport regressions are described below.

Reproduce measurements and refresh while the backend is running on8923:

```sh
node docs/ui-revamp/dp-approaches/frog-jump-measurements.cjs
node docs/ui-revamp/dp-approaches/refresh-inventory.cjs
```

### D5 continuation — viewport regression and completed automated browser gates

Inspection of the early phone screenshot showed an empty-looking maximum memoization
canvas even though38 nodes existed. As a derived tree gains children, its root shifts
horizontally outside the top-left scroll viewport. The renderer now preserves its root
anchor through layout growth and container resize, while retaining manual panning,
vertical scroll and focus. Replacement runs center their own root; own-tree tracers keep
their original layout. Four geometry/cleanup tests proved RED before the fix.

Final production-browser evidence: **28 completed scenario rows** in
`frog-jump-chromium-results.json` (10), `frog-jump-firefox-results.json` (10), and
`frog-jump-zoom-200-results.json` (8), all under `dp-approaches/`. All served
`index-B8pL5v5E.js` / `index-DkVb_nYT.css`. These repeat Frog Jump tasks, not28 problems.
Normal matrices cover320×568,390×844,768×1024,1366×768 and1440×900 in both themes and
engines. Native Chromium zoom uses physical640×1136,780×1688,1366×768 and1440×900;
CSS viewports are halved (including683×384 short landscape), DPR2 and CSS zoom1.

Each row exercises old canonical links, zero-cost memo hits, preparation without execution,
native comparison keyboard/focus, independent steps, edited-draft isolation, default/other
case comparison, Code/Analysis without execution, eleven-element refusal with old run/link,
maximum143-call recursion and38-call memoization, actual visible roots, expanded20-cell
memo tables, and shared input-before-step/source restoration. Page-wide overflow and
uncaught page errors are checked; not every console/network message or manual speech.
Focus setup is programmatic; subsequent comparison activation/tab/slider actions use
native keys. The matrices are not fully keyboard-only learner tasks or screen-reader tests.
Phone/dark, desktop/light and native-zoom captures were inspected.

Reproduce on the unchanged production preview5180 and backend8923, serially:

```sh
PLAYWRIGHT_MODULE=/path/to/external/playwright node docs/ui-revamp/dp-approaches/frog-jump-journey.cjs
PLAYWRIGHT_MODULE=/path/to/external/playwright AUDIT_ENGINE=firefox node docs/ui-revamp/dp-approaches/frog-jump-journey.cjs
PLAYWRIGHT_MODULE=/path/to/external/playwright AUDIT_NATIVE_ZOOM=2 AUDIT_SIZES=640x1136,780x1688,1366x768,1440x900 node docs/ui-revamp/dp-approaches/frog-jump-journey.cjs
node docs/ui-revamp/dp-approaches/refresh-inventory.cjs
```

The scripts respect the normal60/minute execution limit. The inventory advances a
candidate only for its own complete matching-build matrices; missing evidence, wrong
problem, mixed builds, wrong zoom, duplicate rows and missing visual proof cannot certify another row.
Canonical contracts and other candidate statuses remain unchanged.

Exclusions: pre-fix smoke/old-bundle rows are retained as historical evidence, not counted
in28 final rows. A service restart interrupted the first sweep. A premature restart probe
could not finish a response body; body reads now settle before navigation and failures
cannot become passing evidence. Firefox cannot reliably return from browser chrome after
Tab past the document's last control in this headless setup; the reachability probe now
uses Shift+Tab/Tab within the document, with no production keyboard change.

Publication was subsequently authorized by the owner's merge request; pass PR CI before
merging. The owner requested a stop
after this current package; no cleanup/layout/next-algorithm package is started. There is
one merged three-form pilot plus this locally verified candidate;54 other candidate rows,
real devices/virtual keyboard, screen-reader/learner acceptance, broad performance, D6/D7
and P8/P9 remain open. Do not stack the next candidate before this package merges.
