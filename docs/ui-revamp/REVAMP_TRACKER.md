# Revamp execution tracker

Authoritative specification: [IMPLEMENTATION_HANDOFF.md](IMPLEMENTATION_HANDOFF.md).
P0 design decisions: [REFERENCE_DESIGN.md](REFERENCE_DESIGN.md). Evidence narrative: [IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md).

Status vocabulary: planned → in progress → review → done. A package is done only after
its gate and evidence are complete.

## Package ledger

| Package | Status | PR / commit | Gate evidence / remaining work |
| --- | --- | --- | --- |
| P0 Baseline and reference designs | Done | #146 / `bc9829f` | Fresh tests, live API, 16 browser captures, manifests and decisions recorded below |
| P1 Session/input/sharing correctness | Review | this PR | B1–B7 fixed regression-first (29 new tests red on `bc9829f`); existing shell verified in Chromium with the real backend |
| P2 Foundations/library refinement | Planned | — | Build on #145; phone filters, hierarchy and return navigation |
| P3 Shell/Playground reference | Planned | — | Array, Graph+Queue, 2D DP; isolated until integration gate |
| P4 Code/Analysis/integrated route | Planned | — | One session across views; real input and shared-link journeys |
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
| F01 Catalogue & metadata | P2 | Library rows beyond first batch | Existing: `AlgorithmLibrary.test.jsx` | P0 library captures | Baseline OK |
| F02 Ranked search/highlight | P2, P5 | Library + switcher | Existing: `scoreProblem.test.js`, `useProblemSearch.test.js` | — | Baseline OK |
| F03 Category/runnable filters | P2 | Library | Existing: `AlgorithmLibrary.test.jsx` | — | Phone filters below fold (fails gate) |
| F04 Recents/storage safety | P2 | Library search | Existing: `useProblemSearch.test.js` | — | Baseline OK |
| F05 Search shortcut/keyboard | P5 | One Ctrl/Cmd+K owner | Existing: App command palette tests | — | Two owners today (library + App) |
| F06 Direct problem links | P1 | `/problem/:id` | `App.session.test.jsx` unknown id; `AppRouter.test.jsx` | P1 probe: unknown id stays on its URL with a not-found state | Done (P1) |
| F07 Catalogue load/retry/dedup | P2 | `CatalogProvider` | Existing: App catalogue tests | — | Needs lifecycle tests |
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
| F18 Java/active line | P4 | Code view + split | Existing: `CodeViewer.test.jsx` | P0 Graph capture | Baseline OK |
| F19 Variables/frames/containers | P4 | Analysis | Existing: `MemoryComplexityCard.test.jsx` | — | Behind a toggle |
| F20 Complexity | P4 | Analysis | Existing: `MemoryComplexityCard.test.jsx` | — | Baseline OK |
| F21 Error/empty/malformed/unavailable | P1, P3 | Distinct status states | App trace error surface tests; `useTrace.session.test.js` rerun failures | — | Rerun failures keep the prior run, labelled (P1) |
| F22 Truncation | P6 | Persistent status | Existing: `useTrace` truncated tests | — | Watched is set even on a truncated run |
| F23 Offline sample | P3 | Labelled sample state | Existing: `useTrace` offline test | — | Baseline OK |
| F24 Collapse/focus intent | P3–P5 | Views, split, Focus | Existing: `useLayoutPreferences.test.js` | — | Replaced by views |
| F25 Theme/reduced motion | P2 | Header theme control | Existing: `useTheme.test.js`, `designTokens.test.js` | P0 both themes | Baseline OK |
| F26 Continue/daily/streak/starred | P2 | Library learning section | Existing: `Dashboard.test.jsx` | P0 library | Continue hidden in closed disclosure |
| F27 Watched/starred | P2, P3 | Library + header | Existing: `useProgress.test.jsx` | — | Baseline OK |
| F28 Curriculum prev/next | P3 | Context header | Existing: `SectionNav.test.jsx` | P0 Graph capture | Baseline OK |
| F29 Statement/examples | P3 | Disclosure before rail | Existing: `ProblemStatement.test.jsx` | P0 Graph capture | Open by default on desktop |
| F30 Alternate/saved inputs | P1 | Editor | `InputPanel.test.jsx` preset/other-case sync | — | Done (P1) |
| F31 Other-case comparison | P6 | On-demand comparison | Existing: App comparison tests | — | Hidden for Graph/Tree/DP |
| F32 Shared input/step/copy link | P1 | Route adapter | `App.session.test.jsx` sharing + restore, `useShareableView.test.jsx` merges | P1 probe: reload restores step 3 of 7 with target 18; bad link explained | Done (P1); `view` is carried but has no UI until P4 |
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
| P1 session journeys | P1 branch dev | Chromium 1366×768, system theme | Real backend | `evidence/p1/session-probe.cjs`, `p1-notfound.jpg`, `p1-badlink.jpg` | Unknown id → not-found; echo target 18; draft 18 after reopen; 400 leaves URL unchanged and focuses the error summary; reload restores step 3/7 + target 18; step=400 and a rejected link explained and cleaned from the URL; 0 page errors |

### Decision and risk log

| Date/package | Evidence/problem | Decision | Tradeoff | Follow-up/gate |
| --- | --- | --- | --- | --- |
| 2026-09-29 / planning | Old plan predates existing sharing/comparison and dedicated renderers | New handoff takes precedence | Older docs retained as historical context | Implementing agent reads handoff first |
| 2026-09-29 / P0 | `/mnt/c` vitest run takes 10+ minutes | Implement in a git worktree on the WSL Linux filesystem (`/home/prem/dsa-ui-revamp`), same repository and branches | One extra worktree entry in `git worktree list` | Remove the worktree after P9 |
| 2026-09-29 / P0 | No browser harness in the repo | Use Playwright 1.63 from the npx cache via `evidence/browser-audit.cjs`; no new dependency | Not in CI; a human must run it | Revisit at P8 if CI browser coverage is required |
| 2026-09-29 / P0 | HLD comparison | D1–D10 in REFERENCE_DESIGN.md | Keeps Bench palette instead of HLD green/orange | Reviewed at P2/P3 captures |
| 2026-09-29 / P1 | A link whose input is rejected also carries a step | Do not apply that step to the default run; start at step 1 and say so | A learner loses the position the sender meant, but is never shown a different execution's step N | Revisit only if a link format gains a run identity |
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
