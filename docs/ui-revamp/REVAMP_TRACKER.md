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
| P2 Foundations/library refinement | Done (reopened, fixed) | #148 / `3db06a5`; follow-ups in this PR | Library gates met at 320–1440px in both themes; fast typing fixed. Review #5 (own-write detection) fixed in #149; #9 (control-boundary contrast) and #10 (44px chips/recents) fixed in this PR |
| P3 Shell/Playground reference | Done | #151 / `b0da81f` | New workspace to playground-concept.png behind `dsa-ui:workspace` flag; Array, Graph+Queue, 2D DP custom runs and view switching verified in Chromium against the real backend; no overflow at 320/390/1366 in both themes. Stage top y389 at 1366 (target ~280; documented exception, see log) |
| P4 Code/Analysis/integrated route | Review | P4a #152 / `ce99754`; P4b this PR | P4a: split, subviews, follow, Analysis. P4b: the workspace IS `/problem/:id` (flag and old App shell removed); App integration/session/lifecycle suites ported to the workspace; journeys re-run with no flag. Legacy leaf components left for P9 (list in log) |
| P5 Switcher/focus/guidance | Planned | — | Keyboard/focus/modal/tour verification |
| P6 History/comparison/completion | Planned | — | Bounded history, actual comparison, truthful completion |
| P7 Renderer/input families | Planned | — | All registry keys and seven input contracts |
| P8 Accessibility/performance/usability | Planned | — | Evidence matrix and observed-friction iteration |
| P9 Cleanup/release | Planned | — | Complete preservation ledger, checks and rollback |

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

Destination names refer to the handoff's product structure (§4). "Existing" proof is the
test that guards the capability today; it must survive or be rewritten to reach the
feature at its new location.

| Feature ID | Implementing package | Destination | Automated proof | Browser proof | Status / limitation |
| --- | --- | --- | --- | --- | --- |
| F01 Catalogue & metadata | P2 | Library rows beyond first batch | `AlgorithmLibrary.test.jsx` (63 rows, load-more focus) | P2 captures 320–1440px | Done (P2) |
| F02 Ranked search/highlight | P2, P5 | Library + switcher | `scoreProblem.test.js`, `useProblemSearch.test.js` | P2: fast typing keeps "binary search tree" (8 results) | Library done; switcher P5 |
| F03 Category/runnable filters | P2 | Library | `AlgorithmLibrary.test.jsx` chips, scoped clear, disclosure, invalid params | P2 phone journey: Filters (1), chip removal | Done (P2) |
| F04 Recents/storage safety | P2 | Library search | `useProblemSearch.test.js`; denied-storage library test | — | Done (P2) |
| F05 Search shortcut/keyboard | P5 | One Ctrl/Cmd+K owner | Existing: App command palette tests | — | Two owners today (library + App) |
| F06 Direct problem links | P1 | `/problem/:id` | `App.session.test.jsx` unknown id; `AppRouter.test.jsx` | P1 probe: unknown id stays on its URL with a not-found state | Done (P1) |
| F07 Catalogue load/retry/dedup | P2 | `CatalogProvider` | Provider lifecycle tests (retained live list, non-array body, abort on unmount); sample-vs-live retry | — | Done (P2) |
| F08 Default detail/execution | P1 | Problem session | Existing: `useTrace.test.js` | P0 captures | Baseline OK |
| F09 Custom execution | P1 | Controlled editor | `App.session.test.jsx` (echo, draft survival), `useTrace.session.test.js` | P1 probe: summary says target 18 after the run | Done (P1); shell moves in P3 |
| F10 Randomize/defaults | P1 | Draft-only actions | Existing: `randomizeInput.test.js`, Reset test | — | Baseline OK |
| F11 Help/bounds/field errors | P1, P7 | Every input form | Existing: field-error integration test | — | Labels not programmatically linked |
| F12 Play/pause/step/seek/speed | P3 | One controller for all views | Existing: `Controls.test.jsx`, `useTrace.test.js` | — | Baseline OK |
| F13 Playback shortcuts | P5 | Global handler | Existing: J/L after button test | — | Tabs/sliders not yet exempted |
| F14 Narration/position | P3 | Stage narration | Existing: `LiveTraceTicker` via App tests | — | Truncated to one line on phone |
| F15 Canvas/legend | P3, P7 | Stage, all registry keys | Existing: `registry.test.js`, canvas tests | P0 Array/Graph/DP | Stage too short on desktop |
| F16 Queue/grid companions | P3 | Stage companions | Existing: `companions.test.js` | P0 Graph capture | Baseline OK |
| F17 Capture/click-to-seek | P6 | History | Existing: `CaptureStrip.test.jsx` | — | No textual step list |
| F18 Java/active line | P4a | Code view + split | `CodeAnalysis.test.jsx` (split, follow, kept scroll); `CodeViewer.test.jsx` | P4a journey: BFS split at 1366, active line 10 highlighted | Done behind flag; public in P4b |
| F19 Variables/frames/containers | P4a | Analysis | `CodeAnalysis.test.jsx` (null/empty explicit, frame order and current frame, Top/Front/Back, run-level presence) | P4a journey: BFS queue Front/Back; fib call stack with Current frame | Done behind flag |
| F20 Complexity | P4a | Analysis | `CodeAnalysis.test.jsx` (backend values, "unavailable") | P4a journey | Done behind flag |
| F21 Error/empty/malformed/unavailable | P1, P3 | Distinct status states | App trace error surface tests; `useTrace.session.test.js` rerun failures | — | Rerun failures keep the prior run, labelled (P1) |
| F22 Truncation | P1 (watched), P6 (status UI) | Persistent status | `useTrace` truncated tests; `App.session.test.jsx` "does not mark a truncated run as watched" | — | A truncated or offline run no longer marks watched (P1). Persistent status UI remains P6 |
| F23 Offline sample | P3 | Labelled sample state | Existing: `useTrace` offline test | — | Baseline OK |
| F24 Collapse/focus intent | P3–P5 | Views, split, Focus | Existing: `useLayoutPreferences.test.js` | — | Replaced by views |
| F25 Theme/reduced motion | P2 | Header theme control | `useTheme.test.js`, `designTokens.test.js` concept palette guard | P2 captures both themes | Tokens done; workspace in P3 |
| F26 Continue/daily/streak/starred | P2 | Library progress line + learning section | `Dashboard.test.jsx`; Continue-outside-disclosure test | P2 captures | Done (P2) |
| F27 Watched/starred | P2, P3 | Library + header | Existing: `useProgress.test.jsx` | — | Baseline OK |
| F28 Curriculum prev/next | P3 | Context header | Existing: `SectionNav.test.jsx` | P0 Graph capture | Baseline OK |
| F29 Statement/examples | P3 | Disclosure before rail | Existing: `ProblemStatement.test.jsx` | P0 Graph capture | Open by default on desktop |
| F30 Alternate/saved inputs | P1 | Editor | `InputPanel.test.jsx` preset/other-case sync | — | Done (P1) |
| F31 Other-case comparison | P6 | On-demand comparison | Existing: App comparison tests | — | Hidden for Graph/Tree/DP |
| F32 Shared input/step/copy link | P1 + #149 | Route adapter | `App.session.test.jsx`, `App.lifecycle.test.jsx` (cross-problem, same-page, unreadable input, step with no run), `useShareableView.test.jsx`, `useLatestSearchParams.test.jsx` | Session probe (reload restores step 3/7, target 18); review probe (Previous after a restored link sends no POST to the new problem) | Done; `view` is carried but has no UI until P4 |
| F33 Anchor coverage | P4 | Source inspector | Existing: "branches not taken" test | — | Baseline OK |
| F34 Welcome/tour/help | P5 | Help + adapted targets | Existing: `TourGuide.test.jsx`, `WelcomeGuide.test.jsx` | — | Targets tied to old layout |
| F35 Extended keys/speed | P5 | Workspace controls | Existing: speed persistence test | — | Baseline OK |

### Renderer and input coverage

Representatives are chosen from the live catalogue (431 entries). Counts per key are live
`dsType` totals.

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
| Library first load | `82f2808` dev | Chromium 1.63, 1366×768 + 390×844, light + dark | Real backend | `evidence/p0/p0-library-*.jpg`; search top 491 / 549px; first row 691–777px at 1366 | Fails search and first-row gates → P2 |
| Array workspace (Two Sum) | `82f2808` dev | same | Real backend | `evidence/p0/p0-array-1366x768-light.jpg`; stage 705×223 at y219 | Stage too short → P3 |
| Graph+Queue workspace (BFS) | `82f2808` dev | same | Real backend | `evidence/p0/p0-graph-*.jpg`; stage 705×359 | Below 440px target → P3 |
| 2D DP workspace (LCS) | `82f2808` dev | same | Real backend | `evidence/p0/p0-dp2d-390x844-dark.jpg`; stage 705×355 desktop | Below 440px target → P3 |
| Horizontal overflow, console errors | `82f2808` dev | all 16 captures | Real backend | `evidence/p0/measurements.json` | None found |
| Session defects B1–B4 | `82f2808` dev | Chromium 1366×768 | Real backend | Probe script run, recorded in the table above | Confirmed → P1 |
| P2 library gates | P2 branch dev | Chromium 320×568, 390×844, 768×1024, 1366×768, 1440×900; light + dark | Real backend | `evidence/p2/*.jpg`; search top 280px (1366), 287px (390/320); first row 456–534px at 1366 | Pass: gates ≤360 / ≤320 met; row fully visible; no overflow, 0 errors |
| P2 keyboard + Back journey | P2 branch dev | Chromium 1366×768 + 390×844 dark | Real backend | journey probe | Ctrl+K focuses search with a 3px ring; tab order search → clear → selects; Back restores query, filters and batch; phone Filters (1) and chip removal work |
| P2 fast typing | P2 branch dev | Chromium 1366×768 | Real backend | typing probe | Before the fix, "binary search tree" typed fast gave `q=e`. After, the full text and URL are kept, and a filter merges onto it |
| P4b default route (no flag) | P4b branch dev | Chromium 320/390/1366; light + dark | Real backend | `evidence/p4/p4b-*.jpg`; P3 and P4a journeys (flag removed) | Same results as behind the flag: custom runs on Array/Graph/DP, 0 extra executions across views, tab keys, shared-link reload with view, not-found, split and subviews, Analysis queue. 24 captures: no overflow, 0 errors; library gates unchanged |
| P4a Code walkthrough + Analysis | P4a branch dev (flag on) | Chromium 1366×768, 390×844 | Real backend | `evidence/p4/*.jpg`, `evidence/p4/code-analysis-journey.cjs` | 1366: split shown; pointer drag → 45%, ArrowRight → 50%, panes 614/614px; step unchanged. 390: Diagram/Source subviews, same step. Analysis: BFS shows Queue contents (Front/Back), no phantom call stack; fib shows the frame order and Current frame. No overflow; 0 extra executions on view change; 0 errors |
| P3 workspace reference slice | P3 branch dev (flag on) | Chromium 320×568, 390×844, 1366×768; light + dark | Real backend | `evidence/p3/*.jpg`, `evidence/p3/workspace-journey.cjs`, `browser-audit.cjs --workspace next` | Desktop diagram frame 1250×360 (Array) / 1250×440 (Graph, DP) at y389 (P0: 705×223 / 705×359). Phone 322×300 / 322×460. No overflow in 18 captures, 0 errors. Journey: Edit input focuses the editor; custom run updates the echo and returns focus; 3 view switches keep step 3 and the draft with 0 extra POSTs; tab arrows don't step playback; reload restores input, view and step; Graph custom run with queue pane; DP Other case; unknown id → not-found |
| Review follow-ups (#1, #9, #10) and regressions | `de94c1c` + follow-up branch dev | Chromium 1366×768; 390×844 light + dark; library at 320/390/1366 | Real backend | `evidence/review-probe.cjs`; `evidence/p1/session-probe.cjs`; `evidence/browser-audit.cjs` | #1: after a restored two-sum link, Previous opens the next problem at step 1 with no POST to it. #10: chips measure 44px tall. #9: search border renders `#858176` (light) / `#66757b` (dark), 3.47–4.01:1 on page, card and recessed surfaces. Library gates unchanged (y280 / y287), P1 journeys unchanged, 0 page errors |
| P1 session journeys | P1 branch dev | Chromium 1366×768, system theme | Real backend | `evidence/p1/session-probe.cjs`, `p1-notfound.jpg`, `p1-badlink.jpg` | Unknown id → not-found; echo target 18; draft 18 after reopen; 400 leaves URL unchanged and focuses the error summary; reload restores step 3/7 + target 18; step=400 and a rejected link explained and cleaned from the URL; 0 page errors |

### Decision and risk log

| Date/package | Evidence/problem | Decision | Tradeoff | Follow-up/gate |
| --- | --- | --- | --- | --- |
| 2026-09-29 / planning | Old plan predates existing sharing/comparison and dedicated renderers | New handoff takes precedence | Older docs retained as historical context | Implementing agent reads handoff first |
| 2026-09-29 / P0 | `/mnt/c` vitest run takes 10+ minutes | Implement in a git worktree on the WSL Linux filesystem (`/home/prem/dsa-ui-revamp`), same repository and branches | One extra worktree entry in `git worktree list` | Remove the worktree after P9 |
| 2026-09-29 / P0 | No browser harness in the repo | Use Playwright 1.63 from the npx cache via `evidence/browser-audit.cjs`; no new dependency | Not in CI; a human must run it | Revisit at P8 if CI browser coverage is required |
| 2026-09-29 / P0 | HLD comparison | D1–D10 in REFERENCE_DESIGN.md | Keeps Bench palette instead of HLD green/orange | Reviewed at P2/P3 captures |
| 2026-09-29 / P1 | A link whose input is rejected also carries a step | Do not apply that step to the default run; start at step 1 and say so | A learner loses the position the sender meant, but is never shown a different execution's step N | Revisit only if a link format gains a run identity |
| 2026-09-29 / P2 | Owner supplied playground-concept.png | Adopt it as the visual target; forest-green chrome accent (revises D1/D2), paper/card surfaces | A second use of green; guarded by a luminance gap from `--settled` | P3 implements the workspace to it |
| 2026-09-29 / P2 | Router navigations render in transitions, so URL-bound inputs lost keystrokes | One `useLatestSearchParams` hook: synchronous local copy, committed-only adoption of external navigation; used by the library and the share adapter | A small custom hook instead of `useSearchParams` directly | — |
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
