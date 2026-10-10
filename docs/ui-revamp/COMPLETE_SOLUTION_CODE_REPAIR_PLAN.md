# Complete Java solution code repair

Baseline: `9d0aa10`, 2026-10-07. Implementation authorized after the owner requested this plan.

## Confirmed defect

A read-only scan of all 431 live problem-detail responses found 27 containing the exact
placeholder family `initialiseBaseCases`, `evaluateTransitionCandidates`, `extractAnswer`
or `reconstructChosenSolution`; all 27 are in Dynamic Programming. This signature scan
does not certify that every other displayed solution is complete.

`RemainingDpTracer.annotatedCode()` inherits `DpTraceSupport.CODE`. Four reconstruction
tracers explicitly use `CODE_WITH_RECONSTRUCTION`. Real algorithms execute in `run`, but
the API serves the shared sketch as their Java solution. The frontend displays that
source as supplied. Existing anchor tests verify line bounds and reachability, which
the three-line sketch satisfies; they do not compile or execute displayed source.

## Scope and acceptance

Repair all 27 confirmed problems without waiting for the separate three-approach DP
feature. The code must be complete Java 17: typed inputs/results, imports, class,
initialization, actual recurrence, loops, helpers, reconstruction where needed, and
return value. It must compile without app-specific tracing classes and match the
algorithm being visualized, including tie-breaking and numeric types. Safe visualizer
input limits remain enforced by the existing backend boundary.

Do not replace the sketch with a different optimized algorithm that disagrees with the
trace. Preserve trace data/narration/results; intentional source line and anchor changes
are reviewed explicitly. Replacing unrelated catalogue examples cannot fix the source
selected by the real execution API.

## Ordered work

1. Add a regression test that rejects the known sketches across the registry. Add a
   Java-compiler harness for every affected tracer, deriving targets from their shared
   implementation family rather than maintaining a second production routing list.
   Prove it fails before changing source.
2. Store complete annotated Java solutions in backend resources, loaded strictly by
   the affected tracer's own ID. Missing resources fail clearly; no generic fallback.
   Resource files are readable and compile independently after marker removal.
3. Replace the five core sources: LCS, minimum partition difference, DP assign cookies,
   target sum and rod cutting.
4. Replace the seven related string sources: print LCS, LPS, minimum insertions to
   palindrome, minimum insertions/deletions between strings, shortest common
   supersequence, longest common substring and distinct subsequences.
5. Replace the six stock sources: one/two/unlimited/k transactions, cooldown and fee.
   Preserve each problem's actual state, action cap, terminal rows and fee timing.
6. Replace the four subsequence sources: string chain, bitonic subsequence, number of
   LIS and divisible subset. Include predecessor checking and parent reconstruction.
7. Replace the five interval/partition sources: both matrix-chain entries, boolean
   parenthesization, palindrome cuts and partition-array maximum sum.
8. Split shared highlight anchors when one old `fill` label represents different
   statements (buy/sell, forward/reverse, palindrome table/cut computation). Keep a
   meaningful highlighted statement for each emitted event; never hardcode line numbers.
9. Compile all 27 displayed classes and execute their default/alternate and relevant
   edge inputs; compare answers with the corresponding trace and independent known
   cases. A compiling constant-return stub must fail. Add focused phase/anchor tests.
10. Verify HTTP detail and full/delta execution source agree after marker removal.
    Inspect Code view against the real backend, including reconstruction, a helper
    method, custom input and failed rerun; retain scroll/follow/keyboard behavior.
11. Regenerate goldens only for intentional affected output changes. Compare parsed
    old/new snapshots and ensure source/anchors/activeLine are the only changes unless
    another defect was separately diagnosed. Read representative source and anchor
    diffs, then run complete backend/frontend/build checks.
12. Record actual results and remaining limitations. Keep publication separate from
    implementation unless authorized. The three-approach plan remains a separate feature.

## Regression requirements

- Registry-wide known-placeholder guard; complete source required for the affected family.
- Compile the exact displayed source with Java 17, not a hand-maintained test duplicate.
- Run compiled source with parameters drawn from the authoritative input specification.
- Include reconstruction outputs, ties, repeated values, zeros and boundary cases.
- Source and execution anchors must refer to the same committed run; API exposes no marker comments.
- Existing correctness, input-budget, statement, full/delta and catalogue-count contracts stay green.
- No frontend formatting workaround, algorithm substitution, new dependency or fabricated source.

## Relationship to the DP approaches plan

Use these complete canonical solutions as the starting point for
`DP_SOLUTION_APPROACHES_PLAN.md`. Every future recursion/memoization/tabulation option
must meet the same source completeness and execution agreement contract. Repairing
these 27 sources does not mean that additional approaches have been implemented.

## Implementation result — 2026-10-07

Steps 1–12 are complete on `fix/complete-dp-solution-code`; the owner authorized
commit, push and merge. All 27 sources live in `backend/src/main/resources/solutions/dp/`. The
shared sketch constants and reconstruction overrides are removed. No frontend
production code, dependency, input limit or algorithm result was changed.

- Initial source regression: 28/28 cases failed against the original implementation.
- Final source contract: 57 tests; independently compiled Java 17, default/alternate
  inputs, all 66 published examples, resource ownership, HTTP full/delta/detail agreement
  and phase-specific highlighted statements.
- Full backend: 7,967 tests, zero failures/errors, 500 skipped (unchanged skip count).
- Full frontend: 75 files / 675 tests passed. An earlier concurrent run had one lifecycle
  timeout; its isolated rerun passed 8/8, then the full rerun passed without test changes.
- Production build passed: JS 359.47kB / gzip 114.08; CSS 103.99kB / gzip 18.63.
- Live API stats: 431 catalogued and traced, zero untraced, duplicates or orphan tracers.
- Real Chromium: 24/24 source journeys passed (six representative problems × 320/1366px
  × both themes), checking exact live-source highlights at first/middle/final steps,
  source keyboard ownership, view/step continuity, page overflow and page errors.
- Real custom LCS run and HTTP 400 rerun passed: source/input agreement, successful
  sharing, error-summary focus, prior source/step/shared-input retention.
- Exactly 27 goldens changed. Parsed comparison proves all changes are confined to
  `code`, `anchors`, and `steps[].activeLine`; results, narration and data are identical.

Reproducible probes and results: `evidence/source-code/`. Run the browser probes with
both local servers running and `PLAYWRIGHT_MODULE` pointing to an installed Playwright
module (or make `playwright` available normally). `compare-goldens.cjs` defaults to the
baseline `9d0aa10`. RCA-057 records the root cause and regression guard.

Limits: the compiler/execution certification covers these 27 sources, not all 431;
browser checks cover the stated representatives in Chromium, not all devices or inputs.
The recursion/memoization/tabulation selector remains a separate planned feature.

## Broader source inventory — read-only preflight, 2026-10-10

The original 27-source repair remains complete. This additional inventory does not
reopen that bounded package or claim the remaining sources are certified. The current
default-source goldens were just matched against the entire live registry by the fresh
432-case `GoldenTraceTest` in the 8,023-test backend suite on `089cdd9`.

A diagnostic Java 17 compiler scan of those 431 exact default listings reports:

| Result | Listings | What the result does / does not establish |
| --- | ---: | --- |
| Compiles with declared JDK context | 300 | Includes `java.util`/`java.math` imports and an outer class for method snippets; not complete standalone-source or answer certification |
| Platform-node context unresolved | 77 | Diagnostics currently name only `Node`, `TreeNode` or `ListNode`; further omissions may be hidden until authoritative per-problem definitions are supplied |
| Other Java compiler failures | 53 | Undefined helpers, state, constants or supporting types; not fixed by standard imports/outer wrapper |
| Intentional C++ lesson | 1 | `graph-rep-cpp` compiles as C++17 with standard vector/utility includes; Java compiler rejection is not a Java algorithm defect |

Evidence: `evidence/source-code/compile-inventory.json`, with source hashes, diagnostic
messages and explicit scaffolding/limits. No algorithm helper, platform-node stub or
application class was supplied. A helper-class-plus-method listing (`tree-rep-java`)
was retried in an outer class and compiles; its initial context error is excluded.
The C++ lesson is nevertheless incorrectly labelled "Source · Java" / "Java source"
by `SourcePane`/`CodeViewer`; preserve the intentional C++ lesson and fix language honesty.

Concrete examples requiring subsequent source repair: Print LIS calls undefined
`collect`/`answer`; BFS returns undefined `order`; Balanced Parentheses calls undefined
`opener`; N Queens calls undefined `snapshot`/`isSafe`. Complete tracing does not close
these displayed-source gaps. None was implemented in this read-only preflight.

After the current layout package merges, repair the compiler failures in medium topic
batches, starting with simple missing helpers/constants, then graph/backtracking state
and custom support types. Compile the exact displayed sources; add typed default,
alternate, boundary and independent-result tests, plus HTTP source/anchor agreement.
Review authoritative platform definitions separately, and include source-language
metadata/labelling in its own coherent change. The 300 compiler successes also need
semantic/input agreement review; do not limit correctness certification to failures.
Retain the existing no-substitution/source-line/golden discipline. Publication still
requires owner authorization; this inventory is planning evidence, not another stacked
implementation package or a full-project completion claim.

## Helper repair batch 1 — merged #220, 2026-10-10

The layout package merged as #219 / `e68e6f9` with PR/post-merge CI and deployment green.
The next separate branch, `fix/complete-source-helpers-batch1`, repairs five known omissions:
Print LIS, Balanced Parentheses, Asteroid Collision, Next Permutation and Quick Sort.
Each owns a standalone Java 17 `Solution` resource through `CompleteSourceTracer`;
missing resources fail explicitly and no generic source is substituted.

The original algorithms, input specifications, narration and state are unchanged.
Reconstruction, stack-to-left/right conversion and reversal/swap helpers are complete;
typed `solve` parameters match the authoritative fields. Sorting and permutation remain
in-place, preserving Quick Sort's first-element pivot and LIS's strict tie updates.

Seventeen source contracts pass: compilation with an isolated classpath/no app scaffolds,
defaults/alternates, all 12 published examples, bounded edge/seeded cases, ownership,
phase anchors and HTTP detail/full/delta agreement. Twelve failed on the original listings;
two compiling wrong-answer mutations failed selectively and were restored. The full backend
passes 8,040 tests with 500 existing skips; the unchanged frontend passes 79 files/788
tests and its production build. Five goldens differ only in source/anchors/activeLine.

Evidence and reproducible commands: `evidence/source-code/core-batch1/`. The owner approved
publication; PR #220 merged as `7a5120e`, with PR CI, post-merge CI and Vercel frontend
deployment passing. A public backend source check timed out; frontend deployment status
does not certify the deployed backend source. The baseline
inventory is retained as historical evidence; 48 of its other non-platform Java failures,
77 platform contexts, the C++ label and semantic review of compiler successes remain.
No new DP approach was introduced; the remaining 54 candidate rows and D6/D7 stay open.

## Helper repair batch 2 — locally verified, 2026-10-10

Branch `fix/complete-source-helpers-batch2` starts from merged #220 / `7a5120e`.
Four missing-helper listings are repaired: Book Allocation, Painter's Partition,
Shipping Within D Days and Minimum Days for Bouquets. Strict existing resource ownership
is reused; `run`, input specifications, state, results and narration are unchanged.
The source midpoint uses the equivalent overflow-safe expression. No frontend production
code, dependency or DP approach was changed.

The core source contract now passes29 cases across nine owners, including21 published
examples, exhaustive partitions of selected seeded short arrays, independent bouquet
interval selection, singleton/impossible/idle-worker cases and visualizer-cap boundaries.
Four added tests failed before repair; two compiling wrong-answer mutations failed and
were restored. Full backend:8,052 tests,0 failures/errors,500 existing skips. Frontend:
79 files/788 tests, fresh locked install and unchanged production build. Four parsed
goldens change only source/anchors/activeLine. The incremental comparison discovers new
untracked resources without treating the original five as newly repaired; intentional
non-source golden mutation is still rejected.

Real-backend Chromium:36/36 source-view rows (all nine core owners,320/1366px,both themes),
with source hashes matching current goldens and unchanged identified production assets.
Custom Book Allocation [1,2,3,4],m=2 returns6; rejected m=13 produces400 and preserves
source, step and decoded shared input with error focus. Strengthened existing Balanced
Parentheses and default DP/LCS custom probes also pass. Evidence: `evidence/source-code/core-batch2/`.

The owner initially authorized implementation and parallel read-only audits. On continuation
after the publication request, the owner approved commit/push/PR/merge of this batch after
CI passes; publication is in progress. Forty-four of the historical
53 non-platform failures remain after the two helper batches;77 platform contexts,
C++ labelling, compiler-success semantics and wider acceptance/release work remain open.
Books/painters are not certified for arbitrary unbounded arrays outside declared caps.

### Separately diagnosed corrections — pending, not included in this source-only batch

- Smallest Divisor accepts nums=[1,2],threshold=1 and returns2 despite impossibility.
  Its statement requires threshold>=length; enforce the authoritative cross-field contract
  with runner/HTTP400/boundary regressions before certifying its replacement source.
- Matrix Median accepts unsorted [[9,1,2]] and returns9 rather than2. Enforce sorted rows
  and reconcile the even default/lower-median behavior with the authored odd-cell contract.
  Do not silently sort inputs or substitute another problem's median semantics.
- SearchSpaceCanvas retains the last step containing both bounds, ignoring partial bound
  updates. At Book Allocation's final answer113, it still renders answers[112,112] and
  probing112. Repair actual-state reconstruction/terminal presentation in a separate
  frontend regression-first slice. Source-only parsed goldens prove this predates batch2.

The source browser gates check exact code/highlights and session continuity, not every
canvas label's mathematical truth. These discovered defects stay open even though those
bounded source gates pass. Do not mark P7/P8 or final release complete.
