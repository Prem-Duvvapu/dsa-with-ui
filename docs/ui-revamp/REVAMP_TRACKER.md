# Revamp execution tracker

Authoritative specification: [IMPLEMENTATION_HANDOFF.md](IMPLEMENTATION_HANDOFF.md).
P0 design decisions: [REFERENCE_DESIGN.md](REFERENCE_DESIGN.md). Evidence narrative: [IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md).

Status vocabulary: planned → in progress → review → done. A package is done only after
its gate and evidence are complete.

## Package ledger

| Package | Status | PR / commit | Gate evidence / remaining work |
| --- | --- | --- | --- |
| P0 Baseline and reference designs | Done | #146 / `bc9829f` | Fresh tests, live API, 16 browser captures, manifests and decisions recorded below |
| P1 Session/input/sharing correctness | Done (reopened, fixed) | #147 / `748b489`; hardening #149 / `de94c1c` | B1–B7 fixed regression-first (29 new tests red on `bc9829f`). An independent review of `3db06a5` found cross-problem, post-unmount, restoration and same-page link defects; fixed in #149 (12 new tests red on `3db06a5`). Re-verified in Chromium against the real backend on the follow-up branch |
| P2 Foundations/library refinement | Done (reopened, fixed) | #148 / `3db06a5`; follow-ups #149/#150 | Library gates met at 320–1440px in both themes; fast typing fixed. Review #5 (own-write detection) fixed in #149; #9 (control-boundary contrast) and #10 (44px chips/recents) fixed in #150 |
| P3 Shell/Playground reference | Done | #151 / `b0da81f`; sole route since #153 | The workspace was initially behind `dsa-ui:workspace`; P4b removed that flag and legacy App. Array, Graph+Queue, 2D DP custom runs and view switching verified in Chromium against the real backend; no overflow at 320/390/1366 in both themes. Historical P3 frame y389 at1366. The 2026-10-10 local P8 log records improved current coordinates, separating card/frame; useful-frame target exceptions remain open |
| P4 Code/Analysis/integrated route | Done | P4a #152 / `ce99754`; P4b #153; review fixes #155–#157, #191 | Reconciled 2026-10-05: the gate journeys (P4a/P4b rows below) were recorded on the merged route and the two independent reviews' P4 findings are fixed. Legacy leaf components left for P9 (list in log) |
| P5 Switcher/focus/guidance | Done (screen-reader pass pending) | P5a #194 / `175e3d7`; P5b #195 / `e6faaee`; P5c #196 / `158e55d` | Gate met in Chromium on the real backend: keyboard-only switch/open/cancel/current-problem, inert background and focus return (switcher, help, welcome, tour); Focus keeps run/step/draft/URL with 0 executions; nested Escape (dialog → Focus); no seek behind dialogs (`CodeAnalysis.test.jsx`); every tour step spotlights a mounted, visible, in-view target at 1366 and 390. Pending: a real screen reader on the combobox/tour (none available here) |
| P6 History/comparison/completion | Done | P6a #198 / `26ed24a`; P6b #199 / `3373c48` | Gate met on the real backend: all-family bounded history with synced seek (P6a row); comparison on demand for every family with labelled, independent default/other-case runs, its scope and ordinal caveat stated, a submitted main run called out, stale/retired requests aborted; end-of-run actions only for a complete live run (none for truncated/offline/pending), curriculum boundary → library |
| P7 Renderer/input families | Review (device and broader acceptance gates pending) | Inputs #201; fix #200; backend wording #202; renderer fixes #203–#207; cleanup verification #218 | Final cleanup build passes200/200 renderer rows in each of Chromium and Firefox:17 registry keys /50 catalogue-derived problems, default+alternate,1366 dark /390 light, first/middle/final steps. `evidence/p9/verification.json` identifies complete runs and excluded attempts; historical P7/P8 manifests remain. Seven input contracts have real-backend phone checks. Drawing/overflow/error checks are not proof of every answer, every viewport/state in Firefox, or a real device/virtual keyboard. Those broader acceptance gates remain open |
| P8 Accessibility/performance/usability | In progress | Bounded polish #208 / `9d0aa10`; layout package #219 / `e68e6f9` | Keyboard/narration/header fixes merged. Step5 passes788 frontend tests, build,8,023 backend tests,60 Chromium +60 Firefox layout rows and39 tour checks. Saved-input target/form/focus defects fixed; expanded reading and phone setup improved. PR CI, post-merge CI and Vercel passed. `evidence/p8/layout-polish/`, dated log and `layout-journey.cjs`. Frame-position exceptions and broad device/zoom/screen-reader/performance/learner gates remain open; no full P8 sign-off |
| P9 Cleanup/release | In progress (cleanup slice merged) | #218 / `089cdd9`; PR CI, post-merge CI and Vercel passed |19 unreachable files retired, same115 loaded modules;784 frontend tests, build,90 Chromium parity/chrome checks,14 real-backend input checks,200 final renderer rows in each of Firefox and Chromium. `evidence/p9/verification.json`; dated log accounts for64 retired/49 added tests and exclusions. Final project acceptance, release and rollback gates remain open |

Parallel, separately scoped DP work (2026-10-09): D0/D1 #212 (`b111b72`), D2 #213
(`b7ce137`), D3 #214 (`fb96744`) and D4 #215 (`2709d8b`) are merged with passing CI.
The first D5 candidate, Frog Jump, merged as #216 / `4ad9fc3` on 2026-10-09:
backend/API budgets,799 frontend tests, build and28 final Chromium/Firefox/native-zoom
browser rows pass. Its viewport regression has four RED guards. Climbing Stairs and Frog Jump
are the two merged, UI-verified three-form candidates (2/56), not a completed family rollout.
MIT licensing merged separately as #217 / `9de01cd`. The owner resumed implementation on
2026-10-10; the early P9 cleanup slice merged as #218 / `089cdd9` with green CI/deployment.
The owner's subsequent completion goal resumes step5; it does not certify remaining gates.
The other54 candidate implementations, D6/D7, and P8/P9
gates remain open. See [DP_SOLUTION_APPROACHES_PLAN.md](DP_SOLUTION_APPROACHES_PLAN.md)
and the dated D4 continuation in the log for current results; #211 repaired complete DP
source listings and must not be counted as completing P8.

Source completeness follow-up (2026-10-10): the first separate helper batch is locally
implemented on `fix/complete-source-helpers-batch1`, based on #219. Five complete Java
solutions pass 17 executable source contracts, full backend (8,040 tests/500 existing
skips), unchanged frontend (788 tests/build), 20 real-backend Chromium Code-view rows
and a custom-run/400 retention journey. Five goldens change source/highlight positions
only. `evidence/source-code/core-batch1/` and the dated log record evidence and limits.
The owner approved publication, gated on CI; PR/merge are pending. Forty-eight other known non-platform omissions,
77 platform contexts, C++ labelling, broader semantic/source review and all outstanding
DP rollout/human/release gates remain open. The locked dependency audit also reports nine
findings; it is an open release issue, not resolved by either source or layout work.

## Baseline (P0, 2026-09-29)

| Item | Value |
| --- | --- |
| Base commit | `origin/main` = `82f2808` (PR #145); nothing newer upstream |
| Environment | WSL2 (kernel 6.18.33.2), 4 CPUs, 6 GB RAM, Node 20.19.4, npm 10.8.2, OpenJDK 17.0.20.1, Maven 3.8.7, Chromium via Playwright 1.63.0 (not a repo dependency) |
| Frontend tests | `npx vitest run`: 62 files / 472 tests passed (51s on the Linux filesystem; 10m23s on `/mnt/c`, same result) |
| Frontend build | `npx vite build`: JS 347.59 kB (gzip 108.38 kB), CSS 89.92 kB (gzip 16.17 kB) |
| Backend tests | `mvn -B test`: 7,784 run, 0 failures, 0 errors, 511 skipped (1m35s) |
| Live API | `GET /api/problems/stats`: catalogued 431, traced 431, untraced 0, `duplicateIds` {}, `orphanedTracerIds` [] |
| Catalogue | 431 unique ids, 17 categories; Medium 241, Easy 123, Hard 67; every entry has an `inputSpec` with at least one field |
| Baseline failures | None |

### Current-behaviour defects confirmed in a real browser (P1 targets)

| # | Reproduction on `82f2808` with the real backend | Observed |
| --- | --- | --- |
| B1 | Open `/problem/does-not-exist` | URL is replaced with `/problem/two-sum` and Two Sum is shown |
| B2 | Two Sum → Edit input → target 18 → Run → Done editing | "Running on" still says `target 9`, although the trace ran target 18 |
| B3 | Same run, then reopen the editor | Draft is back to `target 9`; the edited value is lost on remount |
| B4 | Edit target to 999 → Run (server returns 400) | URL `input=` now encodes the rejected 999 while the old run stays on screen |
| B5 | Code reading: `InputPanel` preset **Load** | Calls `onRun(preset.values)` without updating the visible fields |
| B6 | Code reading: `InputPanel` initial state | Initialized from `inputSpec` at mount/problem change only, so a spec that arrives later (detail fetch) is never applied |
| B7 | Code reading: `useTrace.runInput` failures | Network/malformed/empty reruns clear the previous valid run instead of retaining it with a label |

## Required evidence ledgers

### Feature preservation

Destination names refer to the handoff's product structure (§4). Reconciled2026-10-09
against the live workspace and current test paths. Named browser evidence is the dated
package evidence, not a fresh project-wide certification. Historical P0 defects remain
above; current limitations belong in this ledger and the package table.

| Feature ID | Implementing package | Destination | Automated proof | Browser proof | Status / limitation |
| --- | --- | --- | --- | --- | --- |
| F01 Catalogue & metadata | P2 | Library rows beyond first batch | `AlgorithmLibrary.test.jsx` (63 rows, load-more focus) | P2 captures 320–1440px | Done (P2) |
| F02 Ranked search/highlight | P2, P5 | Library + switcher | `scoreProblem.test.js`, `useProblemSearch.test.js`, `Workspace.integration.test.jsx` | P2 search + P5 switcher journeys | Implemented; switcher screen-reader pass pending |
| F03 Category/runnable filters | P2 | Library | `AlgorithmLibrary.test.jsx` chips, scoped clear, disclosure, invalid params; merged #218 `AlgorithmLibrary.categoryContract.test.jsx` all17 exact categories | P2 phone journey: Filters (1), chip removal | Done (P2); #218 strengthens the live contract |
| F04 Recents/storage safety | P2 | Library search | `useProblemSearch.test.js`; denied-storage library test | — | Done (P2) |
| F05 Search shortcut/keyboard | P5 | Route-owned Ctrl/Cmd+K | `Workspace.integration.test.jsx`, `AlgorithmLibrary.test.jsx` | P5 switcher journey | Implemented; one active route's action |
| F06 Direct problem links | P1 | `/problem/:id` | `Session.integration.test.jsx` unknown id; `AppRouter.test.jsx` | P1 probe: unknown id stays on its URL with a not-found state | Done (P1) |
| F07 Catalogue load/retry/dedup | P2 | `CatalogProvider` | Provider lifecycle tests (retained live list, non-array body, abort on unmount); sample-vs-live retry | — | Done (P2) |
| F08 Default detail/execution | P1 | Problem session | Existing: `useTrace.test.js` | P0 captures | Baseline OK |
| F09 Custom execution | P1 | Controlled editor | `Session.integration.test.jsx` (echo, draft survival), `useTrace.session.test.js` | P1 probe: summary says target 18 after the run | Implemented in the sole workspace route |
| F10 Randomize/defaults | P1 | Draft-only actions | Existing: `randomizeInput.test.js`, Reset test | — | Baseline OK |
| F11 Help/bounds/field errors | P1, P7 | Every input form | `InputPanel.test.jsx`, `Workspace.integration.test.jsx` | P7 seven-editor phone journey | Labels/help/errors linked; real virtual keyboard pending |
| F12 Play/pause/step/seek/speed | P3 | PlaybackBar + one trace controller | `ProblemWorkspace.test.jsx`, `Workspace.integration.test.jsx`, `useTrace.test.js` | Workspace package journeys | Implemented across views |
| F13 Playback shortcuts | P5 | Global handler + local widget ownership | `Workspace.integration.test.jsx`, `CodeAnalysis.test.jsx`, `ProblemWorkspace.test.jsx` | P5 keyboard + P8 Menu journey | Tabs/sliders/dialogs/source/Menu own their keys |
| F14 Narration/position | P3, P8 | Stage narration | `ProblemWorkspace.test.jsx`, `Workspace.integration.test.jsx` | P8 accessibility journey | Manual narration wraps; autoplay announcements gated; real speech check pending |
| F15 Canvas/legend | P3, P7 | Stage, all registry keys | `registry.test.js`, canvas tests | P7/P8 renderer manifests | Implemented; full second-engine/device acceptance and stage-position target remain open |
| F16 Queue/grid companions | P3 | Stage companions | Existing: `companions.test.js` | P0 Graph capture | Baseline OK |
| F17 Capture/click-to-seek | P6 | History | `CaptureStrip.test.jsx`, `StepHistory.test.jsx`, `Workspace.integration.test.jsx` | P6a all-family history journey | Textual history and capture seeking implemented; DOM bounded |
| F18 Java/active line | P4a | Code view + split | `CodeAnalysis.test.jsx` (split, follow, kept scroll); `CodeViewer.test.jsx` | P4a journey: BFS split at 1366, active line 10 highlighted | Public sole route;27-source repair merged; Print LIS source helpers still require repair |
| F19 Variables/frames/containers | P4a | Analysis | `CodeAnalysis.test.jsx` (null/empty, frame order, container labels) | P4a/P4 review journeys | Public sole route; non-Stack/Queue containers neutrally labelled |
| F20 Complexity | P4a | Analysis | `CodeAnalysis.test.jsx` (backend values, "unavailable") | P4a journey | Public sole route; values from the shown executable |
| F21 Error/empty/malformed/unavailable | P1, P3 | Distinct status states | `ErrorParity.integration.test.jsx`, `Session.integration.test.jsx`, `useTrace.session.test.js` | Session/review journeys | Rerun failures retain explicitly labelled prior run |
| F22 Truncation | P1, P6 | Persistent status + completion guard | `Session.integration.test.jsx`, `Workspace.integration.test.jsx`, `useTrace.test.js` | P6b completion journey | Truncated/offline runs do not mark watched or offer genuine-finish actions |
| F23 Offline sample | P3 | Labelled sample state | Existing: `useTrace` offline test | — | Baseline OK |
| F24 Collapse/focus intent | P3–P5 | Views, split, Focus | `Workspace.integration.test.jsx`, `CodeAnalysis.test.jsx` | P5b Focus journey | Implemented; preserves session and nested Escape ownership |
| F25 Theme/reduced motion | P2 | Shared theme control | `useTheme.test.js`, `designTokens.test.js` | P2/P8 themes + D4 reduced-motion evidence | Implemented; broad final acceptance remains P8 |
| F26 Continue/daily/streak/starred | P2 | Library progress line + learning section | `Dashboard.test.jsx`; Continue-outside-disclosure test | P2 captures | Done (P2) |
| F27 Watched/starred | P2, P3 | Library + header | Existing: `useProgress.test.jsx` | — | Baseline OK |
| F28 Curriculum prev/next | P3 | Context header + end-of-run actions | `Navigation.integration.test.jsx`, `Workspace.integration.test.jsx`; merged #218 `curriculum.test.js` | Lifecycle/completion journeys | Implemented, boundary returns to library; old neighbour watched badge is not a live workspace feature |
| F29 Statement/examples | P3 | Disclosure before rail | `ProblemWorkspace.test.jsx` full statement/disclosure cases, `Workspace.integration.test.jsx` | P3 context + statement rollout evidence | Collapsed initially; authored examples/constraints retained; legacy component test retired in #218 |
| F30 Alternate/saved inputs | P1 | Editor | `InputPanel.test.jsx` preset/other-case sync and target/wrap/inset-focus guards | P8 two-engine populated-preset/open-save-form checks, `evidence/p8/layout-polish/` | P1 behavior implemented; step5 fixes44px preset targets, clipped keyboard rings and save-form overflow merged as #219 / `e68e6f9` |
| F31 Other-case comparison | P6 | On-demand comparison | `Workspace.integration.test.jsx`, `useComparisonTrace.test.js` | P6b + D4 integrity journeys | Available across families; explicitly default vs other-case, not custom main |
| F32 Shared input/step/copy link | P1 + #149 | Single route adapter | `Session.integration.test.jsx`, `Lifecycle.integration.test.jsx`, `useShareableView.test.jsx`, `useLatestSearchParams.test.jsx`; merged #218 `Sharing.integration.test.jsx` clipboard outcomes/timer and all-view entry points | Session/review probes | Implemented input-before-step restoration and all three views; approach sharing guarded by approach tests |
| F33 Anchor coverage | P4 | Source inspector | `Workspace.integration.test.jsx`, `CodeAnalysis.test.jsx`, backend anchor contracts | Source-repair journeys | Backend-owned anchors; branches-not-taken inspector retained |
| F34 Welcome/tour/help | P5 | Help + current-view targets | `TourGuide.test.jsx`, `WelcomeGuide.test.jsx`, `Workspace.integration.test.jsx` | P5c mounted/visible-target journey | Adapted to workspace/phone; real screen-reader tour check pending |
| F35 Extended keys/speed | P5 | Workspace controls | `Workspace.integration.test.jsx` speed/shortcuts; `CodeAnalysis.test.jsx` ownership | P5 keyboard journeys | Implemented; persisted speed and local/global key boundaries |

### Renderer and input coverage

The table below is the historical P0 gap inventory, not the current acceptance status.
Its counts and defect descriptions belong to that baseline; for example, the INT-empty
and GRAPH-label defects were subsequently fixed in #201. Current automated/browser
coverage is recorded in the feature ledger, P7/P8/P9 package rows and dated journeys.
The completed cleanup sweeps cover all17 registry families, but do not certify every
answer/input, a real virtual keyboard, or the full final acceptance matrix.

| Key/type | Representative problem/input | Why representative | Theme/viewport | Default/custom/error proof | Status |
| --- | --- | --- | --- | --- | --- |
| Array (68) | `two-sum`; `lru-page-replacement` | Values/indices/pointers; Array + queue companion | P0: both themes, 390 and 1366 (two-sum) | — | Baseline captured |
| Array + grid companion | `max-rectangle-area-all-ones` | Only Array hero with a grid companion | — | — | Not verified |
| Bits (12) | `count-set-bits` | Fixed 32-wide track on ArrayCanvas | — | — | Not verified |
| Window (12) | `longest-substring-without-repeating` | Moving bounds over a STRING input | — | — | Not verified |
| SearchSpace (27) | `binary-search-1d` | low/mid/high and discarded regions | — | — | Not verified |
| String (26) | `kmp-lps-algo` | Characters and positions | — | — | Not verified |
| PriorityQueue (16) | `kth-largest-element` | Heap tree plus array | — | — | Not verified |
| Matrix (30) | `number-of-islands`; `rotting-oranges` | Board semantics; queue companion | — | — | Not verified |
| DpTable (54) | `longest-common-subsequence`; `climbing-stairs` | 2D table with two STRING inputs; 1D slice | P0: both themes, 390 and 1366 (LCS) | — | Baseline captured |
| Tree (54) | `tree-level-order` | Sparse BINARY_TREE input | — | — | Not verified |
| Graph (40) | `bfs-traversal`; `dijkstra-min-heap` | Graph + queue companion; weighted | P0: both themes, 390 and 1366 (BFS) | — | Baseline captured |
| LinkedList (36) | `reverse-linked-list`; `clone-ll-random-pointer` | next links; random links via INT_GRID | — | — | Not verified |
| Stack (25) | `balanced-parentheses`; `maximum-rectangles-binary-matrix` | Top/empty; grid companion | — | — | Not verified |
| Queue (4) | `queue-array-impl` | Front/back, STRING operations | — | — | Not verified |
| Trie (2) | `implement-trie` | Shared prefixes, terminals | — | — | Not verified |
| RecursionTree (19) | `subsets-i` | Growth, return, backtracking | — | — | Not verified |
| Dsu (3) | `disjoint-set-dsu` | Parent/group changes, GRAPH input | — | — | Not verified |
| Interval (3) | `insert-interval` | Bounds/overlap, INT_GRID input | — | — | Not verified |
| INT (176 fields) | `two-sum.target`, `climbing-stairs.n` | Bounds, temporary empty | — | Existing: rejected target test | Empty is coerced to 0 today |
| STRING (95) | `longest-common-subsequence.first` | Unicode, maximum length | — | — | Not verified |
| INT_ARRAY (175) | `two-sum.nums` | Add/remove, length bounds | — | Existing: `IntArrayField.test.jsx` | Not verified in browser |
| LINKED_LIST (35) | `reverse-linked-list.values` | Sequence, single node | — | — | Not verified |
| BINARY_TREE (53) | `tree-level-order.tree` | null slots | — | Existing: `IntArrayField.test.jsx` | Not verified in browser |
| INT_GRID (39) | `number-of-islands.grid` | Rectangular edits, row/column bounds | — | Existing: `GridField.test.jsx` | Not verified in browser |
| GRAPH (39) | `bfs-traversal.graph`, `dijkstra-min-heap.graph` | Weighted edges, endpoints | — | Existing: `GraphField.test.jsx` | Labels are not programmatic |

### Browser and usability evidence

| Task/state | Base commit/build | Browser/viewport/theme | Real backend or fixture | Artifact/measurement | Result and fix |
| --- | --- | --- | --- | --- | --- |
| Re-audit R1/R2 | `ea5e579` + fix/ui-revamp-reaudit change set | Chromium 1366×844, 390×844, light/dark, reduced motion | Explicit API fixtures | `evidence/reaudit/results.json`, `r1-r2-journey.cjs`, `source-*.png` | 40 checks pass, 0 page errors: native source arrows scroll without seeking, sharing or fetching; unavailable defaults + rejected/malformed links truthfully describe no run or offline sample; Analysis recovery focuses the editor. Full suite 616 tests; 11 defect cases RED on base |
| Library first load | `82f2808` dev | Chromium 1.63, 1366×768 + 390×844, light + dark | Real backend | `evidence/p0/p0-library-*.jpg`; search top 491 / 549px; first row 691–777px at 1366 | Fails search and first-row gates → P2 |
| Array workspace (Two Sum) | `82f2808` dev | same | Real backend | `evidence/p0/p0-array-1366x768-light.jpg`; stage 705×223 at y219 | Stage too short → P3 |
| Graph+Queue workspace (BFS) | `82f2808` dev | same | Real backend | `evidence/p0/p0-graph-*.jpg`; stage 705×359 | Below 440px target → P3 |
| 2D DP workspace (LCS) | `82f2808` dev | same | Real backend | `evidence/p0/p0-dp2d-390x844-dark.jpg`; stage 705×355 desktop | Below 440px target → P3 |
| Horizontal overflow, console errors | `82f2808` dev | all 16 captures | Real backend | `evidence/p0/measurements.json` | None found |
| Session defects B1–B4 | `82f2808` dev | Chromium 1366×768 | Real backend | Probe script run, recorded in the table above | Confirmed → P1 |
| P5a switcher journey | P5a branch dev (`06e30c8` + P5a) | Chromium 1366×768 dark, 390×844 light | Real backend | `evidence/p5/switcher-journey.cjs`, `p5a-switcher-*.png` | 18/18 pass: Enter on the trigger opens with the query focused; page behind inert; Tab contained; active option kept in view; Escape closes, focus back on the trigger, nothing left inert, same step and 0 extra executions; current problem tagged and chosen without a rerun; View all → `/?q=sum` with the library query filled; library Ctrl+K focuses its query; phone Menu → switcher, no overflow, 44px options, focus back on a visible control; 0 page errors |
| P5b Focus gate | P5b branch dev (`175e3d7` + P5b) | Chromium 1366×768 dark, 390×844 light; resize to 900/360, scheme flipped mid-journey | Real backend | `evidence/p5/focus-journey.cjs`, `p5b-focus-*.png` | 22/22 pass: custom run (target 13) at step 3 plus a dirty draft (21) → Focus. Exit focus takes focus; stage moves y389→y105 (1366) / y529→y101 (390), diagram 1252×447 / 324×343, card fits the viewport. Editor and tabs hidden; switcher and help open over Focus, and help's Escape keeps Focus; a second Escape leaves it with focus on Focus. Draft 21 and "Changes not run" kept; same step, same URL, 0 extra executions; no overflow; 0 errors |
| P5c guidance | P5c branch dev (`e6faaee` + P5c) | Chromium 1366×768 dark, 390×844 light; first visit | Real backend | `evidence/p5/tour-journey.cjs`, `p5c-tour-*.png` | 40/40 pass: welcome copy names the switcher and Code walkthrough, page inert behind it; tour from the welcome by keyboard walks 9 steps (1366) / 8 (390: the switcher is in the closed Menu and is skipped), each spotlighting a mounted, visible, in-view target (exact box match); afterwards focus is on a visible element, nothing inert, 0 executions; `?` → Tab → Take the guided tour → Escape ends it cleanly; 0 errors. The first probe run caught a `display:none` desktop link being spotlighted on phones — fixed with `checkVisibility()` |
| P6a history | P6a branch dev (`158e55d` + P6a) | Chromium 1366×768 dark, 390×844 light | Real backend, n-queens n=7 (3,090 steps, truncated) | `evidence/p6/history-journey.cjs`, `p6a-history-*.png` | 22/22 pass: 50 entries rendered of 3,090; "Steps 1–50 of 3090"; 61 Next pages with no seek and no execution; Step 3090 reachable and choosing it moves narration, counter and URL `step=`; DOM still ≤50 entries; Jump to current; Code→Playground keeps step 3090 with 0 executions; a new run (n=4) restarts the list; no overflow, 0 errors. Timings were taken under a host load average of ~30 from unrelated processes and are not a performance result (P8b) |
| P6b comparison + completion | P6b branch dev (`26ed24a` + P6b) | Chromium 1366×768 dark | Real backend: two-sum, number-of-provinces (Graph), n-queens n=7 (truncated) | `evidence/p6/compare-completion-journey.cjs`, `p6b-compare-*.png` | 13/13 pass: 0 comparison requests until opened, then exactly 2 completed; each side shows its own input; moving one side moves neither the other nor the main run; scope and ordinal caveat shown; the Graph problem compares as text (no capture strip); end-of-run row only at the last step of a complete run (Replay / Run the other case / library at the section end); Run the other case = 1 execution starting at step 1; a submitted main run is called out; truncated n-queens at its last step gets no row; 0 errors. Dev StrictMode double-mounts the panel; the first pair is aborted, so the probe counts completed requests |
| P7 input contracts | P7 inputs branch dev (`3373c48` + P7 inputs) | Chromium 390×844 light, touch/mobile emulation (no real virtual keyboard) | Real backend: two-sum (INT), shortest-palindrome (STRING), largest-element (INT_ARRAY), reverse-linked-list (LINKED_LIST), tree-preorder (BINARY_TREE), flood-fill (INT_GRID), graph-intro (GRAPH) | `evidence/p7/inputs-journey.cjs` | 14/14 pass: "-5" typed from empty (old code: "05"); emptied INT stays empty, server answers "Expected a whole number." tied to the field via aria-describedby, URL unchanged; Unicode/whitespace string refused with its field error; array add/negative/duplicate/remove runs; list edit runs; sparse tree (node cleared to null) runs; grid add row/column + cell runs; out-of-range graph endpoint refused with a tied error, 6 vertices with isolated ones run; no overflow; 0 errors. Real virtual-keyboard/device check pending |
| P7 renderer sweep | `3373c48` dev | Chromium 1366×768 dark, 390×844 light | Real backend: 50 problems across all 17 dsTypes, default and declared alternate input | `evidence/p7/renderer-sweep.cjs`, `renderer-manifest.json`, `sweep-*.png` | 192/200: every family draws (no "No visualization"/empty frame) at first, middle and final step with 0 page errors. Failures: 7 phone final steps overflowed 1–78px (DpTable longest-palindromic-subsequence ×2, Bits divide-two-numbers-bitwise alternate, Window number-substrings-all-three-chars ×2, Tree children-sum-property ×2) — the P6b end-of-run row, fixed in #200 and re-checked at 0px (`completion-overflow-recheck.cjs`); 1 page.goto timeout (divide-two-numbers-bitwise default, host load ~30). The first run's "nothing drawn" results were a probe bug (CSS-class guess), fixed before this run |
| P2 library gates | P2 branch dev | Chromium 320×568, 390×844, 768×1024, 1366×768, 1440×900; light + dark | Real backend | `evidence/p2/*.jpg`; search top 280px (1366), 287px (390/320); first row 456–534px at 1366 | Pass: gates ≤360 / ≤320 met; row fully visible; no overflow, 0 errors |
| P2 keyboard + Back journey | P2 branch dev | Chromium 1366×768 + 390×844 dark | Real backend | journey probe | Ctrl+K focuses search with a 3px ring; tab order search → clear → selects; Back restores query, filters and batch; phone Filters (1) and chip removal work |
| P2 fast typing | P2 branch dev | Chromium 1366×768 | Real backend | typing probe | Before the fix, "binary search tree" typed fast gave `q=e`. After, the full text and URL are kept, and a filter merges onto it |
| Review db8683b S7/S8 | S7/S8 branch dev | Chromium 1366×768 light + dark; 390×844 | Real backend (execute forced to 500 / POST held) | `evidence/review-db8683b/s7s8-probe.cjs`, `s7b.cjs` | S7: one alert "Could not load this trace from the backend." in Playground, Code and Analysis (bfs-traversal; two-sum correctly shows its labelled offline sample instead). Rejected run while in Analysis → "Your input could not run" + "Fix it in the editor" → Playground, focus on the error summary. S8: not-found Help opens; Switch problem navigates to two-sum. 0 page errors |
| Review db8683b fixes (#155 + S5/S6 PR) | `d40b9cb` + S5/S6 branch dev | Chromium 1366×768, 1000×768, 390×844 | Real backend (POST delayed by Playwright route) | `evidence/review-db8683b/*.cjs` | B1: held link POST aborted at same-page nav, default (target 9) stays. B2: rejected same-page link keeps {target 7}, notice "previous run is still shown", URL re-points to it. S1: no-step/step=400 → step 1. B3: flood-fill companion "Container", 0 front tags, Analysis "Container contents". S2: End in source and L/Home behind Help leave the step alone. S3: subview change pauses. S4: horizontal 60px restored at 390px. S5: 1000px announces 58% = measured 58%; abandoned drag leaves ratio unchanged. S6: roving stop follows focus, tabs → tabpanel, 1 main. Found and fixed: page-load history entries share key "default", so key-only identity missed same-page navigations in Chromium (jsdom never showed it) |
| P4b default route (no flag) | P4b branch dev | Chromium 320/390/1366; light + dark | Real backend | `evidence/p4/p4b-*.jpg`; P3 and P4a journeys (flag removed) | Same results as behind the flag: custom runs on Array/Graph/DP, 0 extra executions across views, tab keys, shared-link reload with view, not-found, split and subviews, Analysis queue. 24 captures: no overflow, 0 errors; library gates unchanged |
| P4a Code walkthrough + Analysis | P4a branch dev (flag on) | Chromium 1366×768, 390×844 | Real backend | `evidence/p4/*.jpg`, `evidence/p4/code-analysis-journey.cjs` | 1366: split shown; pointer drag → 45%, ArrowRight → 50%, panes 614/614px; step unchanged. 390: Diagram/Source subviews, same step. Analysis: BFS shows Queue contents (Front/Back), no phantom call stack; fib shows the frame order and Current frame. No overflow; 0 extra executions on view change; 0 errors |
| P3 workspace reference slice | P3 branch dev (flag on) | Chromium 320×568, 390×844, 1366×768; light + dark | Real backend | `evidence/p3/*.jpg`, `evidence/p3/workspace-journey.cjs`, `browser-audit.cjs --workspace next` | Desktop diagram frame 1250×360 (Array) / 1250×440 (Graph, DP) at y389 (P0: 705×223 / 705×359). Phone 322×300 / 322×460. No overflow in 18 captures, 0 errors. Journey: Edit input focuses the editor; custom run updates the echo and returns focus; 3 view switches keep step 3 and the draft with 0 extra POSTs; tab arrows don't step playback; reload restores input, view and step; Graph custom run with queue pane; DP Other case; unknown id → not-found |
| Review follow-ups (#1, #9, #10) and regressions | `de94c1c` + follow-up branch dev | Chromium 1366×768; 390×844 light + dark; library at 320/390/1366 | Real backend | `evidence/review-probe.cjs`; `evidence/p1/session-probe.cjs`; `evidence/browser-audit.cjs` | #1: after a restored two-sum link, Previous opens the next problem at step 1 with no POST to it. #10: chips measure 44px tall. #9: search border renders `#858176` (light) / `#66757b` (dark), 3.47–4.01:1 on page, card and recessed surfaces. Library gates unchanged (y280 / y287), P1 journeys unchanged, 0 page errors |
| P1 session journeys | P1 branch dev | Chromium 1366×768, system theme | Real backend | `evidence/p1/session-probe.cjs`, `p1-notfound.jpg`, `p1-badlink.jpg` | Unknown id → not-found; echo target 18; draft 18 after reopen; 400 leaves URL unchanged and focuses the error summary; reload restores step 3/7 + target 18; step=400 and a rejected link explained and cleaned from the URL; 0 page errors |

### Decision and risk log

| Date/package | Evidence/problem | Decision | Tradeoff | Follow-up/gate |
| --- | --- | --- | --- | --- |
| 2026-10-03 / re-audit R1/R2 | Horizontal source arrows leaked to playback; absent/offline run treated as default | Source owns both horizontal arrows; refusal carries explicit available-run state; step and Analysis notices respect absence | Precise sample input stays unknown; no claim that an unavailable run is shown | Implemented and verified; historical review findings retained. Fixture browser evidence, no fresh real-backend certification |
| 2026-09-29 / planning | Old plan predates existing sharing/comparison and dedicated renderers | New handoff takes precedence | Older docs retained as historical context | Implementing agent reads handoff first |
| 2026-09-29 / P0 | `/mnt/c` vitest run takes 10+ minutes | Implement in a git worktree on the WSL Linux filesystem (`/home/prem/dsa-ui-revamp`), same repository and branches | One extra worktree entry in `git worktree list` | Remove the worktree after P9 |
| 2026-09-29 / P0 | No browser harness in the repo | Use Playwright 1.63 from the npx cache via `evidence/browser-audit.cjs`; no new dependency | Not in CI; a human must run it | Revisit at P8 if CI browser coverage is required |
| 2026-09-29 / P0 | HLD comparison | D1–D10 in REFERENCE_DESIGN.md | Keeps Bench palette instead of HLD green/orange | Reviewed at P2/P3 captures |
| 2026-09-29 / P1 | A link whose input is rejected also carries a step | Do not apply that step to the default run; start at step 1 and say so | A learner loses the position the sender meant, but is never shown a different execution's step N | Revisit only if a link format gains a run identity |
| 2026-09-29 / P2 | Owner supplied playground-concept.png | Adopt it as the visual target; forest-green chrome accent (revises D1/D2), paper/card surfaces | A second use of green; guarded by a luminance gap from `--settled` | P3 implements the workspace to it |
| 2026-09-29 / P2 | Router navigations render in transitions, so URL-bound inputs lost keystrokes | One `useLatestSearchParams` hook: synchronous local copy, committed-only adoption of external navigation; used by the library and the share adapter | A small custom hook instead of `useSearchParams` directly | — |
| 2026-10-01 / review N1 | Stage frame at y389 at 1366×768 vs ~280 target | Recorded as an UNMET target pending owner acceptance, not a passed gate; a spacing-compaction experiment is planned for P8 | Owner hierarchy kept meanwhile | P8 re-measure |
| 2026-10-01 / review N2 | Two useLatestSearchParams instances in one route would lose a same-tick write | Documented single-writer rule in the hook; today one instance per route | No runtime enforcement | Revisit before P5 adds any URL consumer |
| 2026-10-01 / review N3 | Cleanup inventory missed search/groupBySection.js, search/normalizeCategory.js, and SearchBox (reached only via dead Sidebar) | Added to the P9 reachability cleanup list | — | P9 |
| 2026-10-01 / owner | Owner's solutions repo (Prem-Duvvapu/LeetCode-and-GFG-problems-solutions) | Trace the owner's code when correct and optimal; otherwise the optimal version in their style, with a "what changed" note; num-provinces uses their recursive DFS | Per-problem tracer rewrites | Topic-batch PRs together with own-words statements |
| 2026-10-01 / review | B3: queueOrStackState is written by both .queue() and .stack() | Neutral "Container" wording for Graph/Grid/Array heroes in Analysis AND the companion pane; Stack/Queue heroes keep Top / Front-Back | BFS loses its "Queue" label until the backend says which | Backend containerKind field as a separate scoped change |
| 2026-10-01 / review | B2: same-page link that cannot run while a custom run is shown | Keep the custom run, say so, and re-point the link at it (rather than silently running defaults) | No extra request; the learner keeps their run | — |
| 2026-10-01 / owner | Full problem statements requested (e.g. Number of Provinces) | Own-words statements + tracer-verified examples + factual constraints + link to the original; not verbatim LeetCode/GFG text | Large content effort, in topic batches | After S5–S8 |
| 2026-09-30 / P3 | Concept hierarchy (header, breadcrumb, title, summary, statement bar, tabs, card title) leaves the diagram frame at y389 at 1366×768, not ~280 | Keep the owner's hierarchy; compact paddings; document as exception rather than drop context | Diagram starts ~110px lower than the handoff target; still fully visible with its narration on scroll | Re-measure at P8; consider a compact context once a learner walkthrough shows need |
| 2026-09-30 / P3 | Workspace grids sized columns to content, so a wide SVG or long input value widened the page on phones | `minmax(0, 1fr)` tracks on every workspace grid; InputSummary shrinks with an ellipsis | — | Guard in P7 renderer pass |
| 2026-09-30 / review | Independent review of `3db06a5`: 2 blocking, 8 should-fix | Fix all ten before P3 (#149 for #1–#8; this PR for #9/#10 and tracker). Own-write detection moved from query-string equality to a per-write token in location state | A token in history state for every URL write | Record combined-revision verification, not only per-package |
| 2026-09-30 / review | Border contrast of controls | New `--control-border` (≥3:1 on page, card and recessed surfaces), used for fields, selects and buttons; decorative `--rule-*` stays subtle | One more token in three theme blocks | Workspace controls adopt it in P3 |
| 2026-09-29 / P2 | "Clear filters" also cleared the search | Scoped actions: Clear filters (filters only), Clear search (×), Clear search and filters (empty state) | One more button in the empty state | — |
| 2026-09-29 / P1 | Share links over 4,000 encoded characters | Refuse to encode them, drop `input` and label it | A huge custom input cannot be shared by link (a preset still saves it) | — |

### Release sign-off

- [ ] All package gates complete or any scope change explicitly approved and recorded.
- [ ] F01–F35 have evidence; all renderers and input types covered.
- [ ] Full frontend/backend/build/startup checks pass on release candidate.
- [ ] Responsive, theme, keyboard, zoom, reduced-motion and failure states reviewed.
- [ ] Screen-reader and learner review performed, or missing evidence explicitly disclosed for owner decision.
- [ ] Performance/bundle changes measured and reviewed.
- [ ] Legacy/development-only code removed and documentation reflects actual shipped behavior.
- [ ] Release commit, rollback boundary, known limitations and publication authorization recorded.
