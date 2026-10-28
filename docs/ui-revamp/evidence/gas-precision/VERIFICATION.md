# Gas Stations precision and continuous search verification

2026-10-27 · base `33992133f3d4daa44f555fa46b761dc943559c99` · branch
`fix/gas-station-precision` · local verification snapshot before publication.
The owner subsequently authorized publication on2026-10-28. GitHub PR/check/merge
records determine the final publication outcome; this snapshot is not a live status.

## Problem and result

The original fifteen halvings could report `333.34351` for stations `[0,1000]`, K=2,
whose optimum is 1000/3. Displayed Java also called an undefined `max` helper, the
statement claimed unsupported accuracy, generic complexity omitted the gap scans,
and the canvas counted real-number intervals as inclusive integers.

Runtime and exact standalone Java now perform forty halvings. At the unchanged maximum
initial gap of 1000, the bracket width is below 1e-9. Five-decimal answers use Locale.ROOT.
Each emitted snapshot carries paired raw bounds, interval width and the explicit
continuous domain. Bound updates precede their snapshots; completed decisions and the
final answer have no stale midpoint. The canvas renders true continuous width, preserves
legacy integer semantics, and wraps raw values on phones. Authored examples and served
canonical complexity agree with the source and runtime. No dependency, endpoint, input
limit, default, global budget, timeout or worker setting changed.

## Checks actually completed

| Check | Observed result |
|---|---|
| Backend RED before implementation | 8 failures / 10 cases, 2 controls passing |
| New continuous canvas RED | 13 failures / 15 cases, 2 controls passing |
| Continuous and unchanged discrete canvas tests | 41 passed |
| Final normal `mvn -B test` | 8,311 cases, 0 failures/errors, 500 unchanged skips |
| Normal `npx vitest run` | 891 passed / 85 files, 161.81 seconds |
| `npx vite build` | Passed, 1,588 transformed modules |
| Launcher process cleanup | Passed |
| Golden regeneration | 432 cases passed; only Gas fixture changed |
| Real API | 8 scenarios × full/delta + 4 rejected requests; all 20 passed |
| Gas production-preview browsers | 24 complete scenarios, 12 per engine |
| Discrete production-preview browsers | 16 complete scenarios, 8 per engine |
| Temporary service cleanup | Owned process groups stopped; ports 8923/5180 free |

Tests run against the exact displayed Java with an isolated Java 17 compiler/classloader.
An independent rational oracle enumerates gap/pieces candidates and uses integer
feasibility and integer HALF_UP rounding. It checks 280 small/seeded traced inputs and
51,011 compiled-source entries, including repeats: eleven examples, 1,000 seeded
multigap cases, and all 50,000 permitted two-station gap/K combinations. This is bounded
execution evidence, not exhaustive testing of every multi-station arrangement.

Three deliberate mutations were observed failing and then restored: source returns
high+1, snapshot omits raw high, runtime reverts to fifteen halvings. Failure summaries
and retained-log hashes are in [red-mutation-summary.json](red-mutation-summary.json).
The first complete suite exposed the existing resource guard's inheritance assumption;
the guard now checks actual registered source ownership, with reverse coverage retained
through CompleteSourceTracer's final resource path. The final full suite passes.

The reviewed default golden changes from 77 to 202 steps and answer `4.00012` to
`4.00000`, retaining the actual input and gap values. Source, anchors, forty actual
iterations and complete raw snapshots account for the remaining changes.

## Actual API and browser evidence

[api-results.json](api-results.json) records defaults, alternate, three original precision
counterexamples, a half-cent case and both maximum-sized examples. Maximum inputs finish
in 642 steps; the largest measured full response is 664,165 bytes and the largest measured
delta response is 529,661 bytes, under the unchanged 2,000,000-byte budget. Actual decoded
full/delta steps match. Invalid K and unsorted positions receive field-specific 400s with
no steps. Live stats report 431/431 traced, zero untraced, duplicate or orphaned owners.

[chromium-results.json](chromium-results.json) and
[firefox-results.json](firefox-results.json) each cover default, maximum and half-cent
inputs at 320×568 and 1366×768, light and dark themes. Each checks five actual snapshots,
raw ranges, normalized CSS ruler values, gap reads/highlights, final answer/probe state,
exact source and active line, Analysis values, no re-execution on view switches,
input-then-step link restoration, and a rejected K=0 run preserving the prior source,
input echo, step, successful URL and edited draft, with error-summary focus.

[chromium-discrete-results.json](chromium-discrete-results.json) and
[firefox-discrete-results.json](firefox-discrete-results.json) each cover the same widths
and themes for binary-search-1d and book-allocation. Initial/probe/final snapshots retain
inclusive counts, index versus answer semantics, current input reads, source/line identity,
step continuity and no extra executions. Book Allocation's exhausted final interval is
preserved. Every browser row asserts no page-wide overflow or page errors.

Two early Chromium attempts stopped at an overly strict probe assertion: the CSSOM
serializes 33.333301544…% as 33.3333%. The probe now compares exact browser-normalized
CSS declarations while keeping exact raw interval assertions. Those partial attempts
are excluded; both final complete engine matrices pass. No product change was needed.

All manifests identify the same frozen JAR SHA-256
`2ad7da8f2c296a68c36938e2ab4e7e2d964e99ce41a80702fdef059b9b77fe88`.
[packaged-identity.json](packaged-identity.json) records byte equality for all 613 compiled
production classes and the exact source resource. Browser phases identify served assets
`index-W_0F_aS5.js` and `index-D-Z30BYf.css`; build output is 390.03 KB / 124.29 KB gzip JS,
98.95 KB / 17.94 KB gzip CSS. The JAR did not change during API/browser verification.
No Maven commands ran while the services were live. Four selected final-state screenshots
were visually inspected, one phone/dark and desktop/light capture per engine.

## Reproduce

Run normal backend/frontend tests and build, regenerate/read goldens, then package the
backend before starting services. Never rebuild or run Maven while serving these journeys.
Start the packaged backend on 8923 and Vite production preview on 5180. Run sequentially:

```sh
node docs/ui-revamp/evidence/gas-precision/api.cjs
AUDIT_ENGINE=chromium node docs/ui-revamp/evidence/gas-precision/journey.cjs
AUDIT_ENGINE=firefox node docs/ui-revamp/evidence/gas-precision/journey.cjs
AUDIT_ENGINE=chromium node docs/ui-revamp/evidence/gas-precision/discrete-compatibility.cjs
AUDIT_ENGINE=firefox node docs/ui-revamp/evidence/gas-precision/discrete-compatibility.cjs
```

Use PLAYWRIGHT_MODULE to point to an installed Playwright module; no repository dependency
was added. BACKEND_URL, FRONTEND_URL and GAS_EVIDENCE_DIR optionally change endpoints/output.
Raw validation logs are retained locally at `/home/prem/validation/dsa-gas-precision/`.

## Limits and remaining project work

This package fixes this owner's known defect. It is not whole-catalogue source/algorithm
certification, native-zoom, real-device, screen-reader, learner, performance, public rollout
or P7–P9 acceptance. Other widths and tallest unrelated canvases are not newly certified.
No screen-reader session was performed. DP remains 23/56 verified and merged, with 33
outside verification. Phone Combinations budgeting, Count Squares narration, random-input
contracts, other source/metadata repairs, dependency remediation and broader acceptance
remain separate work. No overall completion or publication approval is inferred.

Analytics is a separate merged/deployed package (#234, `3399213`): production JavaScript
delivery works. Its SDK skips automated browsers, so normal visitor/event collection and
authenticated dashboard totals remain unverified; automated probes did not bypass that
filter or manufacture visits. Existing URL-redaction tests pass separately.
