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
