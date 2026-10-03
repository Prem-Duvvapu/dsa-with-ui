# Independent re-audit of the review fixes

Date: 2026-10-03

Implementation follow-up: R1 and R2 are fixed in the `fix/ui-revamp-reaudit`
change set, based on `ea5e579`. The historical findings below are retained as written.
See the 2026-10-03 entry in `IMPLEMENTATION_LOG.md` and `evidence/reaudit/results.json`:
616 tests pass, 11 defect regressions fail against the original implementation, and 40
fixture-backed Chromium checks pass in light/dark at 1366px and 390px. Public deployment
verification is separate from these local implementation checks.

## Verdict and scope

**No blocking defect confirmed in this re-audit. Two should-fix issues remain.** The remediation is substantive, but the documentation's blanket completion claim is too strong.

Reviewed the changes addressing `INDEPENDENT_REVIEW_DB8683B.md`:

| PR | Commit | Purpose |
| --- | --- | --- |
| #155 | `d40b9cb` | Session cancellation, link truth, container semantics, keyboard and source continuity |
| #156 | `30c08be` | Separator, tabs, same-key navigation identity |
| #157 | `d2cf4cf` | Cross-view errors, not-found controls, restored migration assertions |

Inspected the individual changes and their combined result at `d2cf4cf`. The intervening footer change (#154) was not treated as review remediation. Later algorithm/content changes are outside this audit.

The user's checkout remains on `docs/ui-revamp-agent-handoff` at `6bae4aa`; it was not switched or reset. Testing used isolated archives under `/tmp`. Remote refs were refreshed. The affected session/source/shortcut implementations and notice-copy file are unchanged between `d2cf4cf` and fetched `origin/main` (`ea5e579`), so the two findings below are not already resolved there. This is not a full audit of that later revision.

All source references below are relative to `frontend/src/`, at **d2cf4cf**.

## Blocking

None confirmed. This is not an assertion that every possible concurrent-render or browser interaction is safe; verification limits are listed below.

## Should fix

### R1. Horizontal source-scroll keys still seek the algorithm

**Files:** `workspace/SourcePane.jsx:6`, `:58`; `hooks/useKeyboardShortcuts.js` (global ArrowLeft/ArrowRight player bindings).

**Reproduction:**

1. Open a problem in Code walkthrough with multiple trace steps.
2. Focus the `Java source` region. A long source line makes horizontal scrolling particularly relevant.
3. Press ArrowRight.
4. Playback advances from step 1 to step 2 instead of leaving the selected algorithm step unchanged.

ArrowLeft similarly reaches the global previous-step binding. Neither key is in `MANUAL_KEYS`, so the source handler neither stops propagation nor suspends following. The global handler prevents the key's default action and seeks the run.

**Evidence:** A temporary test using the real `ProblemWorkspace`, Router, and source region expected narration `n=1 step 1` after ArrowRight; it received `n=1 step 2`. The existing End/Space regression passes, so it does not cover this variant.

**Why it matters:** Reading a long line changes execution unexpectedly; advancing to the last step can also affect watched progress. S2's source-keyboard ownership is only partially fixed.

**Suggested fix:** Include horizontal arrows in source scrolling ownership, preserve their native scrolling behavior, and suspend follow. Keep global arrow stepping outside the source region. Add regressions for both directions and a real-browser long-line check that verifies `scrollLeft`, unchanged step/URL, and suspended follow.

### R2. Failed-link notices claim a default run exists when no run exists

**Files:** `hooks/useProblemSession.js:127`–`:134`, `:148`–`:151`; `workspace/sessionCopy.js:28`–`:30`, `:44`–`:45`.

**Reproduction:**

1. Open a shared custom-input link with `step=3`.
2. Let the initial default GET return HTTP 500, with no offline sample for that problem.
3. Let the linked POST return HTTP 400 with field errors.
4. There are no trace steps, but the shared-link notice says both **“Showing the default input.”** and **“playback starts at step 1.”**

The temporary workspace fixture used `{n:99}` rejected by a declared maximum of 50. This is not dependent on that synthetic problem: the failure is in common session/copy logic. A malformed link after an unavailable default also reaches the unsupported default-display claim.

`refuse()` distinguishes a retained custom submission from everything else, not a retained default run from no run or an offline sample. `linkNoticeText()` then interprets `kept: false` as a displayed default. The rejected-input branch emits `step-dropped` before considering whether any run exists.

**Evidence:** A temporary real-workspace test received:

> The input in this link could not be run: the server rejected it. Showing the default input. The rejected values are in the editor. Its step 3 belonged to that run, so playback starts at step 1.

The new session-level load-error alert also exists, making the status messages contradictory rather than resolving the false claim.

**Why it matters:** The application describes unavailable data as displayed. B2's original retained-custom scenario is fixed, but failure-state honesty is not complete.

**Suggested fix:** Derive notices from an explicit outcome such as retained custom, retained default, offline sample, or no run. Only claim default execution when a matching committed default run actually exists. When there is no run, say the requested step cannot be shown instead of saying playback starts. Test default failure combined with rejected and malformed links, with and without requested steps.

## Notes

### N1. The completion claim needs qualification

**File:** `docs/ui-revamp/IMPLEMENTATION_LOG.md:390`–`:399` (repository root).

The remediation is marked complete and S2 fully fixed. R1 contradicts that claim; R2 leaves an adjacent honesty case unresolved. Update the dispositions after implementing and verifying the remaining fixes. Do not erase historical evidence of the original fixes.

### N2. Dependency audit warnings are pre-existing, not introduced here

`npm ci --prefer-offline` succeeded but reported seven advisories: four moderate, two high, one critical. Neither dependency manifest nor lockfile changed in this review range. Exploitability and dependency remediation were not investigated; do not attribute these advisories to the three fixes or blindly apply a force upgrade.

## Previous finding dispositions

“Fixed” below means the reported defect is addressed in the inspected code and relevant automated coverage; it does not imply a fresh cross-browser certification.

| Finding | Status | Evidence / qualification |
| --- | --- | --- |
| B1 obsolete same-page execution | Fixed in tested cases | POST generation retirement, navigation identity guard; both new workspace cancellation tests pass and fail against the old implementation. |
| B2 retained custom run mislabeled as defaults | Original scenario fixed; related gap remains | Rejected/malformed same-page links re-share the retained custom input and say previous run. Both regressions discriminate old/new code. No-run variant is R2. |
| B3 fabricated queue semantics | Fixed | Graph/Matrix/Array containers now use neutral labels in Analysis and companions; known Stack/Queue heroes retain their explicit semantics. |
| S1 same-page step reset | Fixed | Missing and out-of-range steps explicitly seek zero; regressions fail against old code. |
| S2 shortcut ownership | Partially fixed | Dialog, End/Space, separator navigation and handled modifier cases pass. Horizontal source arrows still leak: R1. |
| S3 subview pause | Fixed | Callback pauses before selecting; regression begins playing and fails on the old implementation. |
| S4 source continuity | Fixed in tested cases | Both axes saved; problem-tagged memory and keyed pane reset follow. Added audit sequence visiting another problem outside Code also passed. |
| S5 separator lifecycle/geometry | Main defects fixed | Pointer capture replaces global drag listeners; cancel/lost capture handlers exist; ratio bounds derive from container size. Regression tests discriminate old/new behavior. Exact browser geometry not remeasured here. |
| S6 tab semantics | Fixed | Main landmark retained; referenced element is labeled tabpanel; focused tab controls roving tabindex. |
| S7 cross-view errors | Fixed for original cases | Code/Analysis receive session-level load errors; rejected input has an editor recovery route. Five relevant new tests fail against old code. |
| S8 not-found controls | Fixed | Shared overlays rendered in not-found branch; Help and switcher tests pass and fail against old code. |
| N1 stage y389 | Explicitly deferred | Tracker calls this an unmet target pending acceptance, with a P8 experiment. Not remeasured. |
| N2 URL single writer | Documented | One-writer limitation is explicit; no runtime enforcement added. |
| N3 cleanup inventory | Documented/deferred | Search helpers and transitive SearchBox reachability included for P9. |

## Tests and verification

- Clean, unmodified merged test suite: **71 files / 593 tests passed** at `d2cf4cf`.
- Production build passed: JS **339.77 kB / 107.66 kB gzip**; CSS **96.42 kB / 17.43 kB gzip**.
- Regression sensitivity spot-check: transplanted the merged `Navigation.integration`, `CodeAnalysis`, and `ErrorParity.integration` suites into an isolated `db8683b` source tree. **22 failed / 14 passed, 36 total.** Failures correspond to the old session, keyboard, continuity, separator, container and error-parity behavior, not missing imports.
- Independent extra workspace probes reproduced R1 and R2. A source-state reset sequence passed. Another suspected early-submission failure was discarded: the actual editor disables submission during the initial GET, so that UI scenario was not reproducible and is not a finding.
- No implementation, tests, branches, commits, pushes or PRs were changed in the user's checkout. Only this report was added. Diagnostic test edits were confined to temporary archives.

### Test quality and remaining coverage gaps

The new tests are not merely cosmetic; the old-code experiment confirms useful regression sensitivity. The migration repairs restore DP renderer/shell/legend assertions, meaningful phone view content, catalogue-count coverage, and URL mirroring.

Remaining limitations:

- `CodeAnalysis.test.jsx:148` covers source End/Space, not horizontal arrows. Extend it for R1.
- `Navigation.integration.test.jsx` rejection tests begin with a usable default/custom run. Add a missing-default cross-product for R2.
- `CodeAnalysis.test.jsx:238` checks that cancellation leaves the ratio unchanged, but does not first prove an active pointer move changes it. A broken drag-start/move handler could satisfy that assertion. Add a positive drag before cancellation and verify subsequent moves stop changing it.
- Separator geometry tests use mocked element widths and assert ARIA percentages; they do not measure rendered pane widths or actual hit regions.
- The editor-recovery test checks navigation and visible field text, not the resulting focus target. Add a focus assertion if that is to serve as the focus-management guard.

### Claims not independently verified

- Historical exact per-PR red-test counts; the 22/36 experiment is an aggregate spot-check, not a reproduction of every claimed count.
- Historical browser measurements/screenshots, both-theme viewport sweeps, pointer capture outside the window, real native source scrolling, screen-reader behavior, 200% zoom and reduced motion. No fresh browser pass was performed in this re-audit.
- Backend tests: backend code was not changed by these three fixes and its suite was not rerun.
- Exhaustive concurrent-render, stale-history-token and interrupted-navigation interleavings.
- Advisory exploitability from npm's install summary.

## Recommended next action

Fix R1 and R2 with failing-first regression tests, then run a focused browser check before marking remediation complete. P5's riskiest area remains **keyboard and navigation ownership**: the switcher, Focus mode, source panes, dialogs and tour must share an explicit policy rather than accumulating independent global handlers.
