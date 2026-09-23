# Quality standard and release review

Status: proposed gates and empty evidence ledger, 2026-09-23. No browser, performance, accessibility, or learner-review result is claimed by this document.

## 1. Definition of done

A redesigned feature is complete only when its preserved behavior works, its layout is readable with realistic content, its keyboard/focus behavior is correct, and its failure states are accurate. A release is complete when all F01–F25 obligations and all renderer/input families have evidence, required automated checks pass, and the browser/usability gates below are satisfied.

Critical failures block release: lost feature access, wrong algorithm or run shown, erased input drafts on view changes, invalid source synchronization, unreachable catalogue results, inaccessible primary controls, clipped essential content, misleading execution status, and crashes in the primary flow.

Minor visual defects may be recorded for follow-up only if they do not violate those gates and the release review explicitly accepts them. Do not relabel unresolved mandatory checks as optional to meet an estimate.

## 2. Existing automated gates

The inspected frontend package defines `test` and `build`; the existing CI runs Vitest and Vite. Run from the repository root in the current Linux/WSL shell:

```sh
npm --prefix frontend test
npm --prefix frontend run build
```

When backend contracts/algorithms change, or for the established full release CI gate, run from `backend`:

```sh
mvn -B test
```

If startup behavior changes, the repository has:

```sh
bash start-smoke-test.sh
```

Run the actual CI-required suites before merge. For frontend-only iterations, use affected tests and the build, then the full frontend suite at the integration gate. Existing backend CI still must pass before release/merge; do not claim a local backend run if only CI was used.

No `lint`, `typecheck`, browser-test, or accessibility command is currently declared in the frontend package inspected for this plan. If browser automation is added, document its actual script and setup before making it a required command. Documentation-only changes require link/consistency checks, not unrelated application test runs.

## 3. Behavioral regression cases

| Case | Required observation |
| --- | --- |
| Library full reachability | A known entry beyond result 50 is reachable; filters and Load more do not duplicate/omit IDs |
| Search reuse | Ranking, highlighting, recents, runnable/category scope, and keyboard selection match their intended semantics |
| Library return | Open a result after filtering/loading more; Back restores query, filters, result availability, and practical scroll position |
| Direct entry | Open problem and view URLs directly; refresh and unknown IDs render the correct state |
| Single execution selection | Changing problems issues intended detail/execute requests without redundant catalogue fetches |
| Stale response | A slow earlier problem/run response cannot overwrite a later selection/run |
| Async specification | An input contract arriving after mount initializes correct defaults without overwriting typed values |
| Draft preservation | Edit without running, change all views, return; exact draft and validation state remain |
| Shared step | Seek to a noninitial step; change views; diagram/source/variables/history all reference that step |
| Run identity | Edit a successful run's input; old trace stays labeled with old input until a new successful run |
| Rejected input | A 400 preserves draft and prior valid run, presents field errors, and supports correction/resubmission |
| Failed run | Network/server error is explicit; previous run is identified if retained; no fabricated replacement |
| Limited run | Truncation remains visible and final status does not claim successful completion |
| Playback lifecycle | View/dialog/document visibility transitions pause; no duplicate timers; final-step/empty-step controls are coherent |
| Focus and resize | Enter/exit Focus and change code split; diagram stays usable, state unchanged, focus restored |
| Modal behavior | Keyboard opens search, background is inert, Escape dismisses, trigger regains focus |
| Follow source | Manual source scrolling is respected; Return to active line resumes following |
| Preference failure | Invalid or unavailable localStorage does not break startup, search, or theme changes |

Prefer tests that cross component boundaries and assert observable behavior. Avoid snapshot-only checks or tests asserting the same constants the implementation defines. Preserve existing tests for trace decoding, registry mapping, input serialization, companion state, code-line mapping, and design-token contrast.

## 4. Representative algorithm matrix

Confirm exact IDs and capability matches against the current catalogue during R0-01. The IDs below are candidates grounded in current repository documentation/source; they are not a live execution audit.

| Family | Candidate fixture | Required inspection |
| --- | --- | --- |
| Array | `two-sum`, `kadane-algo` | Indices, pointers, narration, input changes, code line |
| Shared array mappings | A current Window, SearchSpace, String, Bits, and PriorityQueue entry | All five mappings render their actual payload without new semantic claims |
| Graph + queue | `bfs-traversal`, `dijkstra-min-heap` | Edge/node labels; queue remains present when temporarily empty |
| Matrix + queue | `rotting-oranges` | Grid readability and stable companion |
| Tree | `tree-preorder`, `tree-level-order` | Balanced and skewed structure, active node, code |
| Linked list | `reverse-linked-list`, `clone-ll-random-pointer` | Pointer identity, long chain, random edge |
| Stack + grid | `maximum-rectangles-binary-matrix` | Top marker; last-known grid retained |
| Queue | A current Queue-mapped implementation | FIFO markers, empty/populated/final states |
| DP | `climbing-stairs`, `edit-distance`, `ninja-and-his-friends` | 1D, 2D and slice context, labels, predecessors |
| Trie | `implement-trie`, `word-break-trie` | Shared prefixes and terminal state |
| Recursion | `n-queens` or a current RecursionTree-mapped entry | Actual mapping confirmed; branching/backtracking and frames |
| DSU | `disjoint-set-dsu` | Group and parent-state readability |
| Intervals | `merge-intervals` | Bounds, overlap, selected/result state |

For each distinct renderer, exercise default input and one valid input that changes structure or execution. Include small/empty states where the contract permits, long labels, and supported larger inputs. Unsupported empty input should produce validation rather than a made-up visual case.

Broad coverage combines registry/contract tests with representative browser checks. Do not claim every algorithm has been manually reviewed because one example per renderer passed.

## 5. Browser and accessibility matrix

| Environment | Required checks |
| --- | --- |
| 1366×768 laptop | Stage prominence, header height, code split minima, input discovery, playback/narration access |
| 1440×900 desktop | Balanced workbench, all views, resizing, large structures |
| 768×1024 tablet | Reflow, switcher, input editors, local overflow |
| 390×844 phone | Single-column journey, source/diagram selection, touch targets, virtual keyboard |
| 320px narrow width | No page-wide horizontal overflow; essential controls remain available |
| 200% browser zoom | Reflow, labels, focus, modal bounds, no loss of controls |
| Both themes + system | Contrast, state semantics, theme persistence/fallback; system preference change |
| Reduced motion | No distracting transitions; all step interactions still understandable |
| Keyboard only | Complete find → open → edit → run → step → inspect → return journey |
| Screen reader | Header/landmarks, tabs, search/dialog, field errors, state description, nonflooding playback announcements |

Use at least Chromium and Firefox for the desktop critical flow. Check mobile Safari or WebKit for the phone layout when available; label unavailable browser coverage explicitly. Test real focus and layout in a browser, since jsdom cannot establish geometry, native dialog usability, or actual scrolling.

The accessibility target is WCAG 2.2 AA. Treat this as a target requiring verification, not a certification. Use appropriate tooling plus manual review; an automated accessibility scan alone cannot establish conformance.

## 6. Performance review

Capture baseline and redesigned measurements under the same machine, browser, build mode, and fixture. Record:

- Production JS/CSS output and transfer sizes where measurable.
- Catalogue render/filter latency and mounted result count.
- Time from receiving a trace to usable playback.
- Step/seek responsiveness for representative short and near-limit traces.
- Mounted history-row count and memory trend across repeated problem switches.
- Resize/theme-switch responsiveness for a graph and DP table.

Set numeric budgets after recording baseline, before signing off the reference slice. Suggested review trigger: a repeatable regression over 20% in a comparable measured path needs investigation and explanation, not automatic acceptance. Any multi-second UI freeze or unbounded list growth blocks release regardless of percentage.

Do not time Java execution and call the result a frontend improvement. Separate network/backend duration from browser rendering. Do not use a tiny fixture to claim near-limit trace performance.

## 7. Learner walkthroughs

Perform a task-based review with the owner and, if available, one newcomer. Do not invent participant results when only an implementer walkthrough is available; record that limitation.

Tasks:

1. Find a named algorithm without scrolling through categories manually.
2. Find a problem in a category and difficulty without knowing its exact name.
3. Change input, predict the result, run, and reach a specific event.
4. Explain which Java line caused the visible state change.
5. Find the current variable/call-stack state and explain time/space cost.
6. Return to the same step after switching views.
7. Recover from an invalid input and find execution history.
8. Complete the core run/step/inspect flow on a phone layout.

Observe hesitation, missed controls, unnecessary scrolling, lost state, and terminology confusion. Record whether the task was completed without coaching. Compare the same task against the baseline where practical. Fix the most consequential confusion and repeat that task; visual preference alone is insufficient.

## 8. Evidence ledger

Every record should include date, commit, browser/version, viewport, theme, fixture/problem ID, real-backend vs mocked source, result, and artifact or test reference. Store browser artifacts under an agreed review location; do not check in generated build output or large videos by default.

| Gate | Status | Evidence |
| --- | --- | --- |
| Planning document links and consistency | Passed, 2026-09-23 | Six documents; 27 local links resolve; 23 unique work-item IDs; all work-item references resolve; tracked diff whitespace check passed |
| Baseline browser/automated checks | Not run | R0-01 |
| F01–F25 preservation matrix | Pending implementation | Inventory and future verification records |
| Library and switcher journey | Not run | R1 integration |
| Reference Array/Graph/DP journey | Not run | R2-06 |
| All renderer and input editor coverage | Not run | R3 |
| Theme/responsive/zoom review | Not run | R3-04 |
| Keyboard/screen-reader/reduced motion | Not run | R4-01 |
| Performance comparison | Not run | R4-02 |
| Learner walkthroughs | Not run | R4-03 |
| Full frontend tests and build | Not run for redesign | R4-04 |
| Required backend CI / applicable local tests | Not run for redesign | R4-04 |
| Legacy cleanup and accurate docs | Not started | R4-04 |
| Release decision | Not ready | R4-05 |

## 9. Release handoff

Provide the changed user journeys, key desktop/phone screenshots, feature-preservation checklist, test/build results, measured limitations, known browser gaps, and rollback reference. Distinguish implemented behavior from future ideas. The UI is not “finished” while a required feature or release gate remains unresolved.
