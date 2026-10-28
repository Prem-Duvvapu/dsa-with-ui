# Phone Combinations contract verification

2026-10-28 · base `00ffc247bdc66858f72cb518c39554ecfa110b35` (merged Gas #235)
· branch `fix/phone-combinations-contract` · local verification before publication.

## What changed

Previously `777777` passed the field bounds but stopped after1,014 of4,096 combinations.
`222227` and `722222` both have972 results, yet only the first fits because branch order
changes intermediate trace volume. The displayed Java lacked KEYPAD, the final state
reported only a count, generic complexity assumed two-way branching, and Randomize
generated letters for a digit field. The recursion strip also displayed character codes
and used fill-label ink on an unfilled surface.

The owner now checks complete shape cost before its first event, reusing the unchanged
StepEmitter byte accounting. It counts prefix widths and decimal capture-count groups,
including final result data; it does not run the enumeration to estimate its cost.
Field validation still crosses InputValidator first. Existing capacity tracers use the
same owner-preflight pattern, so no general callback/schema framework was introduced.
The global5000-step/2MB caps remain mandatory backstops.

The original1–6 digit domain,2–9 pattern, default23 and alternate2 remain. Safe six-digit
inputs still work. Oversized inputs receive a digits400 before partial emission. Complete
standalone Java and runtime use a mutable path, return the actual ordered results, and
preserve traversal. Complexity is O(N*P) time, O(N) auxiliary plus O(N*P) returned output.
The public method remains letterCombinations, with dedicated isolated compilation and
the registry-wide source ownership guard; the existing solve-entry suite is unchanged.

Declared alphabet/randomMaxLength hints let the generic Randomize generator sample valid
2–9 strings up to five positions without changing accepted input limits. Actual completed
results supplement the original count and use Analysis's existing full-value disclosure.
Path strips honor supplied character labels and retain numeric values, including zero.
Both normal and completed strip text use normal theme ink; state bars retain their colours.
No dependency, endpoint, session owner, global budget, timeout or worker setting changed.

## Completed checks

| Check | Observed result |
|---|---|
| Backend RED |8 failures,1 missing-statement error,1 passing control |
| Generator RED |19 failures,18 passing controls |
| Character rendering RED |2 failures,1 passing numeric control |
| Source-return and missing-stack-cost mutations |2/2 fail for intended reasons; restored |
| Focused backend |61 passed, including existing core-source guards |
| Final full backend |8,324 cases,0 failures/errors,500 unchanged skips |
| Final normal frontend |915 passed /86 files,93.43 seconds |
| Production build / launcher cleanup | Passed |
| Golden regeneration |432 cases pass; only Phone changes |
| Real packaged API |18 execution requests pass |
| Final production-preview journeys |26 pass:12 main +1 Randomize per engine |
| Actual contrast |4 engine/theme rows pass:12.81:1 dark,18.20:1 light |
| Final selected visual states |4 rows /8 screenshots, all inspected |
| Owned service cleanup |Both process groups stopped;8923/5180 verified free |

Independent iterative Cartesian expansion checks actual values and order, not just counts.
The exact displayed Java compiles in an isolated Java17 classloader, with only JDK imports
and an outer class if necessary; KEYPAD/helpers are never injected. The source test checks
232 input entries including repeats, invoking each twice, including cases beyond animation
budgets. Trace tests cover88 seeded/edge inputs plus branch-shape and boundary cases.

All64 representative six-digit3/4-way branch patterns are checked directly and through
both HTTP encodings. Only222222 and222227 complete under the current policy; all other
representative shapes are refused before their first event. Cost parity tests compare the
estimator against actual emitter accounting for62 shorter branch patterns plus6 edge
variants, exact step/byte boundaries and an Arabic server locale. A separate three-step
emitter still truncates honestly without final results or a done event.

The default golden retains all22 events, captures, path data, call stacks and descriptions.
Only complete source/line anchors and the actual final result list are added. All other
goldens remain unchanged. See [red-mutation-summary.json](red-mutation-summary.json).

## Actual API and browser evidence

[api-results.json](api-results.json) records defaults, alternate, maximum-five and both
safe-six cases in full/delta, plus unsafe orders, invalid digits and unknown-field400s.
Actual decoded traces agree; source, anchors, type, approach, input and complexity match.
The maximum-five input finishes1,024 results in2,389 steps. The largest measured full
response is1,797,480 bytes; largest delta is1,134,281, both from222227. Live stats remain
431/431 with no untraced, duplicate or orphaned owners.

[chromium-results.json](chromium-results.json) and [firefox-results.json](firefox-results.json)
each cover default, maximum-five and safe-six-late-four at320×568 and1366×768, both
themes, with one extra deterministic Randomize case. They inspect actual path/capture
states and exact source highlights, the complete results disclosure, honest220-node diagram
limits, no view-triggered execution, shared input-before-step restoration and rejected722222
preserving prior source/run/input echo/step/link/draft with error-summary focus. Randomize
changes only the draft to99999 until Run; its actual successful run returns all1,024 words.
Every row checks no page-wide overflow/errors and at most one primary canvas.

The first visual review exposed real path contrast of1.22:1 on the dark panel.
[contrast-before.json](contrast-before.json) is that partial failing measurement, not a
complete matrix. The quantitative CI guard also reproduces completed text at3.77:1 in
light mode. Both CSS rules now use normal text ink. [contrast-results.json](contrast-results.json)
checks actual computed colours against the opaque surrounding panel in both engines/themes.
The guard covers normal/completed strip rules in both explicit and OS light declarations.

The JAR is frozen at SHA-256
`ebf6275e683e36f7824e78f1d64dd518676130013a824456b3a0bccc51649d5a`.
[packaged-identity.json](packaged-identity.json) records byte equality for all615 compiled
production classes and the exact source resource. Final served assets are
`index-BfO8d-Ww.js` and `index-D4rQyCzb.css`:390.65KB/124.49KB gzip JS and
98.96KB/17.94KB gzip CSS. Backend/source/API identity is unchanged through the contrast fix.
Final browser manifests must match those assets. No Maven runs while serving the journeys.

## Exclusions and reproduction

A preliminary frontend run had a sharing-restoration wait failure; neither its assertions
nor timers were changed. The isolated seven-case sharing check passed, followed by a
complete normal suite. Another command ended143 before a summary; absence of surviving
worktree test processes was confirmed before restarting the unchanged normal command
with a persistent runner. Cause of that termination is unproven.

Two initial browser probes incorrectly expected Playground-only controls while still in
Analysis. Those partial runs are excluded; the complete corrected matrix passes. The
first complete browser/visual build then exposed the actual contrast defect. Its artifacts
were archived outside the repo and the complete final frontend/browser/visual checks are
repeated against the corrected bundle. These exclusions do not count as passing evidence.
Raw logs are retained at `/home/prem/validation/dsa-phone-contract/`.

After normal tests/build, package the backend, freeze identities and start that JAR on8923
and Vite production preview on5180. Run sequentially (PLAYWRIGHT_MODULE can point to an
installed module; no repository dependency was added):

```sh
node docs/ui-revamp/evidence/phone-contract/api.cjs
AUDIT_ENGINE=chromium node docs/ui-revamp/evidence/phone-contract/journey.cjs
AUDIT_ENGINE=firefox node docs/ui-revamp/evidence/phone-contract/journey.cjs
node docs/ui-revamp/evidence/phone-contract/contrast.cjs
node docs/ui-revamp/evidence/phone-contract/visual.cjs
```

BACKEND_URL, FRONTEND_URL and PHONE_EVIDENCE_DIR optionally change endpoints/output.
Stop owned services afterward. This is bounded owner-specific evidence, not all-input,
native-zoom, real-device, screen-reader, learner, performance, public-rollout or release
acceptance. DP remains23/56 verified and merged;33 candidates and D6/D7/P7–P9 gates remain.
