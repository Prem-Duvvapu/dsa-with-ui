# DSA UI/UX revamp — complete implementation handoff

Date: 2026-09-29. Audience: the implementing agent and the project owner.

**This is the authoritative remaining-work plan.** It supersedes conflicting requirements, baseline descriptions, ordering and estimates in the September 23 planning documents. Those documents remain historical design context, not a second competing backlog. Preserve existing repository contribution and tracer-correctness rules.

This deliverable is documentation only. No new UI implementation, fresh browser audit, performance result, or accessibility certification is claimed here. Implementation requires a separate instruction from the owner. Do not infer permission to publish from this file.

## 1. Goal and definition of “better than HLD”

Make the product a comfortable learning workspace where someone can find an algorithm, understand the problem, change an example, watch execution, inspect Java and state, and explain the result without managing a cramped dashboard.

The goal is not to show fewer capabilities. It is to show the right capabilities at the right time, preserve context between them, and make every capability easy to find.

“The best” cannot be guaranteed by choosing a palette. For this project, an improvement over the reference means:

1. HLD-like clarity: readable page hierarchy, deliberate spacing, named learning activities, restrained surfaces and normal page scrolling.
2. Better continuity for DSA: changing views never loses a draft, run, selected step, speed or useful source position.
3. Better evidence density: the diagram has enough space to teach, and its narration, code and variables always describe the same execution step.
4. Better discoverability at scale: every catalogue entry is reachable; search, return navigation and switching remain fast across hundreds of problems.
5. Better small-screen parity: a phone user can run, step, inspect source, edit every input type, compare cases and share the current run.
6. Better trust: errors, incomplete traces, sample mode and submitted input are unmistakable. Nothing is fabricated to fill a beautiful empty panel.

### Scope

Required: library refinement; shared visual foundations; problem session reliability; Playground, Code walkthrough and Analysis; statement/examples; input editing; optional source split; focus mode; history; existing comparison, progress, curriculum, presets, sharing, theme, shortcuts and guidance; all renderer/input families; responsive and accessibility review; cleanup and verification.

Excluded: new algorithms, new curriculum or practice question banks, accounts, cloud storage, AI tutor, code editor/compiler, backend framework upgrades, TypeScript migration, replacement UI framework, analytics services, new gamification and a general-purpose graph editor. Fix a backend contract only when a demonstrated frontend requirement cannot be met correctly otherwise; scope and test that change separately.

## 2. Verified starting point and source of truth

`origin/main` was fetched for this planning pass and is `82f2808` (PR #145). The prior planning PR was #144 (`b57044f`). Check again when implementation actually starts. Do not rebuild already merged work.

| Area | Starting state | Remaining responsibility |
| --- | --- | --- |
| Library | `AlgorithmLibrary.jsx` implements search, category/difficulty/progress/runnable filters, URL state, batches of 50, recent queries, return scroll and learning disclosure | Refine hierarchy, phone filters, keyboard/focus, return-scroll tests and empty/error states |
| Shared catalogue | `catalog/CatalogProvider.jsx` is mounted by `AppRouter.jsx` | Preserve one shared loader, cancellation, deduplication and retry; add lifecycle coverage |
| Shared chrome | `LearningHeader.jsx`, `LearningLayout.module.css` | Extend and reuse; do not create an unrelated second design system |
| Problem workspace | `App.jsx` still hosts the old layout | Replace presentation incrementally while preserving behavior |
| Layout | `App.module.css` uses `height: 100vh`, hidden overflow and fixed bottom-panel heights | Replace with a naturally scrolling page and deliberate stage dimensions |
| Execution | `hooks/useTrace.js` owns execution requests, decoding and playback | Retain its contract protections; improve run/input ownership and recovery |
| Sharing | `useShareableView.js` already handles `input` and 1-based `step` query parameters | Preserve existing links and add `view` without parameter races |
| Renderers | Registry has 17 keys and 16 distinct renderer components/variants | Preserve every mapping, including dedicated String, Window, SearchSpace and Heap |
| Backend | Java `/api/problems` tracing API | Keep it authoritative; no old topic endpoints or substitute algorithms |

Historical evidence, not new measurements: the September 23 live API reported 431 catalogued/traced entries, zero duplicates and no orphaned tracers. PR #145 passed frontend/backend CI, security and deployment checks. Its initial local frontend suite passed 472 tests; a subsequent resource-contended run failed four integration tests and a focused rerun passed 68 tests. Capture a fresh baseline rather than assuming this historical count remains current.

Historical browser observations: at 1366×768 the old Two Sum stage was approximately 705×223px. The new library's first result moved from roughly y840 to y703 after reducing its hero; at 390px its first result was still around y843. These explain the priorities, not final design acceptance.

### Read these implementation sources

- Routing/catalogue: `frontend/src/AppRouter.jsx`, `catalog/CatalogProvider.jsx`, `components/AlgorithmLibrary.jsx`, `search/scoreProblem.js`, `search/useProblemSearch.js`.
- Workspace/state: `App.jsx`, `App.module.css`, `hooks/useTrace.js`, `hooks/useShareableView.js`, `hooks/useLayoutPreferences.js`.
- Input: `components/InputPanel.jsx`, `IntArrayField.jsx`, `GridField.jsx`, `GraphField.jsx`, `input/randomizeInput.js`, `hooks/useInputPresets.js`.
- Inspection: `CodeViewer.jsx`, `MemoryComplexityCard.jsx`, `CaptureStrip.jsx`, `CompareStrip.jsx`, `InputSummary.jsx`, `StepStateSummary.jsx`, `LiveTraceTicker.jsx`.
- Navigation/guidance: `CommandPalette.jsx`, `SectionNav.jsx`, `ProblemStatement.jsx`, `ShortcutHelp.jsx`, `WelcomeGuide.jsx`, `TourGuide.jsx`, `hooks/useKeyboardShortcuts.js`, `hooks/useFocusTrap.js`.
- Rendering: `canvas/registry.js`, `canvas/companions.js`, `components/CanvasShell.jsx` and the mapped renderers.
- Compatibility: `usePersistentState.js`, `useTheme.js`, `useProgress.js`, `useStreak.js`, `useLastVisited.js`, tests alongside these modules, `App.integration.test.jsx`, `designTokens.test.js`, `.github/workflows/ci.yml`.

Names below describe boundaries; the agent may choose nearby names when existing code already provides the responsibility. Avoid renames for their own sake.

## 3. How to use the HLD reference

Inspect the sibling repository read-only. Its implementation, not stale introductory docs, is the reference:

| Reference | Borrow | Adapt for DSA / do not copy |
| --- | --- | --- |
| `../hld-with-ui/frontend/src/components/ModuleShell.tsx` | Clear topic header; URL-backed views; visible active tab; keyboard navigation | Three DSA task views rather than five HLD tabs; state above conditional panels |
| `frontend/src/styles.css` in HLD | Paper/surface separation; editorial typography; bounded reading width; restrained borders; focus ring; responsive single column | Do not copy its 570px hero, small metadata or fixed control-column width into DSA |
| `pages/HomePage.tsx` in HLD | Clear learning entry points and meaningful hierarchy | Hundreds of DSA problems need searchable rows and continuation, not hundreds of large cards |
| `pages/RequestFlowPage.tsx` in HLD | Shared shell and explicit loading/error states | Conditional feature mounting is not enough to preserve an algorithm session; use one session above views |
| `features/request-flow/Playground.tsx` in HLD | Visible experiment/observe relationship | DSA graph/tree/DP stages need more room; no permanent narrow input column by default |

No cross-repository imports, copied API contracts or assumptions that HLD tabs are all required here. The visual direction is a coherent DSA sibling, not a skin pasted over the crowded layout.

## 4. Product structure and feature locations

```text
Library /
  Search + results + optional filters
  Your learning: Continue / daily pick / starred / progress

Problem /problem/:id
  All algorithms | Switch problem | Theme | Help
  Title + category + difficulty + star + curriculum navigation
  Problem statement & examples [disclosure]
  Playground | Code walkthrough | Analysis

  Playground: diagram → narration/playback → input editor
  Code:       diagram + source → shared narration/playback
  Analysis:   step context/playback → state + complexity

  Execution history [disclosure, same run]
  Compare other case [on demand, independent second trace]
```

Use explicit names, not unlabeled icons for core actions. The top-level page is not a fixed-height desktop application. The browser owns vertical scrolling. Local scrolling is appropriate for code, oversized structures, an open switcher and bounded history—not every panel.

### 4.1 Library

- Preserve the current shared catalogue and search engine. Refine, do not replace from scratch.
- Compact introduction, one clear search box, actual filtered count and readable result rows. No promotional illustration may push primary discovery out of reach.
- Desktop: labeled category/difficulty/progress controls plus runnable filter. Phone: query and count stay visible; a **Filters (N)** disclosure contains the same controls. Active filter chips remain visible and individually removable. Clearing filters must have an explicit scope.
- Preserve `q`, `category`, `difficulty`, `status`, `runnable`, `limit`. Reset the batch on filter changes. Invalid values normalize safely. Derive labels/counts from actual data.
- Keep Load more with stable ordering and an announcement of added rows. Do not move keyboard focus unexpectedly after loading. All entries remain reachable.
- Restore the exact library history entry: query, filters, number of loaded rows and scroll. Wait for rows to exist before restoring. Direct navigation to `/` starts at its normal top.
- A result title is a real link. Star is a separate button, never nested inside the link. Opening a problem does not automatically mark it watched.
- Returning-learning actions remain available; do not make a closed disclosure the only way a returning learner discovers Continue. Consider a single compact Continue link beside its summary when there is a last-visited entry.
- Distinguish loading, empty catalogue, zero filtered results, stale retained catalogue and labeled offline sample. Retry retries the catalogue, not a random execution.

### 4.2 Header and problem context

- Compact brand/header; explicit All algorithms and Switch problem; theme and Help. On phones use a labeled overflow menu for secondary actions, not for Play, Next, Run or Edit input.
- Title wraps naturally. Show real category/difficulty/type metadata without a badge for every property. Keep Star reachable. Previous/next respects existing authored curriculum ordering.
- Problem statement disclosure appears before the view rail. Keep title, examples and constraints; visually distinguish problem constraints from visualizer input limits.
- Default statement collapsed for stage-first studying; clear summary “Problem & examples.” Opening it uses normal flow. No fabricated summary/outcome if content lacks one.
- Page title includes problem and active view. A unknown ID produces an explicit not-found state and a library link; remove the current silent redirect to another algorithm.

### 4.3 Playground — default view

```text
← All algorithms                             Switch problem
Arrays · Easy       Two Sum                         ☆
▸ Problem & examples                       Previous / Next
[ Playground ] [ Code walkthrough ] [ Analysis ]

Visualization                Edit input   Show code   Focus
┌────────────────────────────────────────────────────────┐
│ Legend, primary structure and relevant companion       │
│ Enough height to read relationships, not leftover space │
└────────────────────────────────────────────────────────┘
Step 4 of 12 — narration for this exact state
[Restart] [Previous] [Play] [Next]   Speed   [Seek slider]
Input used: …                    Draft changed — not run

Try your own input
Schema-driven fields, help and validation
[Run input] [Other case] [Randomize] [Restore defaults]
Saved inputs / Save current input

▸ Execution history (12 steps)       Compare other case
```

- Default stage is full width; code split is opt-in. No persistent catalogue sidebar or fixed bottom diagnostics row.
- Desktop starting heights: sequence stages 360px minimum; spatial/tree/graph/DP stages 440px minimum. These are initial useful sizing targets, not universal hardcoded heights. Grow or locally scroll for larger examples.
- Phone stages start around 280–360px where meaningful; keep labels legible rather than scaling all structures to fit. The page may scroll.
- Input editor starts as a normal-flow section below playback. A prominent Edit input action is visible at the stage. It opens the section if needed, scrolls it below the sticky rail and focuses its heading/first field. Closing returns focus to the trigger.
- The input-used summary is the executed snapshot, never the current editable draft. Show “Changes not run” when they differ.
- Narration wraps in full beside the evidence. Playback controls retain explicit labels; Restart playback and Restore default input are different actions.
- After successful Run, return attention to the result heading/status without auto-playing. Respect reduced motion. Errors keep attention in the editor; they must not scroll the user to an old result.

### 4.4 Code walkthrough and optional split

- One shared run and selected step. Never execute again merely because code became visible.
- Wide Code view shows source and diagram together. Playground's Show code toggle uses the same source component and state, not a second source implementation.
- Begin around 60/40 diagram/source. Enforce actual pane minima around 480px diagram and 360px source plus gap; use available container width, not only viewport width. For space-demanding diagrams raise the diagram minimum or use single-pane mode.
- An adjustable separator supports pointer and keyboard control, a label, current value and reset. Clamp persisted ratios. ResizeObserver-driven canvases must respond after resize and after becoming visible.
- If minima cannot fit, Code view uses Diagram / Source subviews with the same controls/narration and selected step. A Playground Show code action can navigate to Code view in this case, with an accurate label.
- Line numbers and active-line marker remain backend-anchor-derived. Preserve anchor coverage, including uncovered branches.
- Follow execution is explicit. Manual source scrolling suspends auto-follow; Return to active line restores it. Do not steal selection or scroll on every render.
- Keep long lines locally scrollable, source selectable, and source scroll position across view changes. Do not mount two active primary canvases to retain state.

### 4.5 Analysis

- Lead with current step, narration, compact playback and Back to visualization.
- Sections: Variables; Call stack; relevant stack/queue/container contents; Algorithm complexity. Use responsive reading columns, not four tiny equal cards.
- Variable names and values remain readable. Expand long values and structured objects; show null/empty explicitly rather than silently omitting them.
- Label frame ordering, active frame, Front/Back/Top markers. Reuse trace semantics rather than deriving meaning from problem titles.
- Complexity is backend/content metadata, not animation performance. Missing values say unavailable. Do not infer Big-O, allocations or runtime from step count.
- No full diagram is required in this view. It gains reading space by changing the task, not by squeezing another column into Playground.

### 4.6 History and comparison

- History starts closed and includes current position and total steps in its summary. Keep CaptureStrip where meaningful; preserve its current exclusions for Graph, Tree and DP unless evidence justifies changing them.
- Add a textual step list for every trace family. Default page size 50; show the rendered range, Previous/Next page and Jump to current. All steps must be reachable. Seek pauses playback and synchronizes code/diagram/analysis.
- Expanding history alone does not restart, seek or execute. Closing it returns focus safely. Avoid rendering 5,000 full cards/canvases.
- Keep Compare other case as an explicit, discoverable action near history and input. Open on demand; preserve actual independently fetched traces, loading, error and retry.
- Label both inputs and each trace's position. Compare ordinal positions only as ordinal positions, not as equivalent algorithm phases. Do not map custom input to the wrong compared trace or synthesize a second trace.
- If existing comparison only compares defaults and the declared alternate, label that scope. Do not silently expand to arbitrary saved-run comparisons in this revamp.

### 4.7 Focus, switching and help

- Focus is an in-page visualization mode, not required browser fullscreen. Hide optional neighboring panels and extraneous context; retain problem identity, diagram, companions, narration, playback and a visible Exit focus.
- Enter/exit preserves the run and step; pause on entry/exit. Restore the prior split and appropriate trigger focus on exit. Escape exits only when no higher-priority dialog is open. View/problem change exits focus.
- Upgrade the existing command palette into the single switcher; no duplicate new modal. Ctrl/Cmd+K opens it from the workspace and focuses the library query on the library page.
- Modal rules: pause on open; focus search; trap focus; background inert; Escape closes; dismissal restores trigger focus; selection closes and navigates. Selecting current problem closes without resetting its run.
- Search keyboard behavior: arrow selection, Enter opens, visible empty state, bounded results and View all results carrying the query. Preserve ranked matching and recent searches.
- Keep shortcut help, welcome dismissal/replay and guided tour. Update `data-tour` targets. Tour navigation must reveal a hidden target's view/disclosure before attempting to focus it; no arrow pointing at unmounted code.

## 5. Visual and responsive specification

### Design vocabulary

Extend the current Bench tokens with semantic aliases only where required. Keep existing execution-state meanings; a navigation accent must not make “selected tab” look like “algorithm error.” Use neutral page/panel/recessed surfaces, strong text, secondary text, subtle/strong rules and a distinct focus outline. No per-component hex-color patchwork.

| Element | Initial target |
| --- | --- |
| Workspace width | Up to 1440px; library can retain 1320px; centered |
| Reading text width | About 60–75 characters; analysis text does not span the full monitor |
| Gutters | 24–32px desktop; 16px phone |
| Spacing | 4 / 8 / 12 / 16 / 24 / 32 / 48px; larger gaps between tasks, smaller within groups |
| Body / input | 16px default, comfortable line height around 1.5–1.65 |
| Controls / code | 14–16px; source begins at 14px, supports browser zoom |
| Metadata | Usually 12–13px; never reduce essential labels to decorative microtype |
| Title | About 28–36px workspace, not a landing-page billboard |
| Primary targets | At least 44×44px touch area, including icon-only actions |
| Surfaces | Modest radius, quiet borders, shadows reserved for elevation such as dialogs |
| Motion | Short optional transitions; no ambient diagram/chrome animation unrelated to execution |

Light, dark and system are equally supported. Verify disabled controls, active cells/edges, input errors, selection and focus in each. Contrast review targets: 4.5:1 for ordinary text, 3:1 for large text and meaningful UI boundaries. These are project acceptance targets; passing a color check alone is not an accessibility certification.

### Layout by available width

| Width | Behavior |
| --- | --- |
| ≥1200px | Generous stage; split allowed only if actual pane minima fit; readable analysis columns |
| 900–1199px | Single primary stage by default; measured split only when comfortable; wrapping header actions |
| 600–899px | One primary column; Code subviews when needed; normal-flow inputs/analysis |
| 320–599px | Compact header; local tab-rail overflow if necessary; visible search with filter disclosure; wrapping playback; single-column inputs |

Use one sticky view rail initially, not a header + tabs + playback stack. Apply coordinated scroll margins so focused fields/headings are not hidden. Do not add a bottom playback dock until a real walkthrough demonstrates a need; if added, reserve its space and handle safe areas, virtual keyboard and short landscape screens.

No page-wide horizontal overflow. Oversized diagram/source/table regions can scroll locally, with an accessible label and discoverable controls. Do not use `overflow-x: hidden` to conceal off-screen buttons and call it responsive.

### Measurable design gates

- At 1366×768, library search and the complete first result row are visible on initial load with learning disclosure closed and no error banner. Target search top ≤360px; reduce hero/redundant headings if needed, not input target size.
- At 390×844, search and Filters action are visible without scrolling; target query top ≤320px. Filter controls may expand below. No condition that all controls/results fit one screen.
- At 1366×768, default workspace stage begins around/before y280 with statement closed and no exceptional alert. Provide the useful diagram height above; playback can require modest normal scrolling.
- Every core action is reachable without first closing an unrelated panel. No compulsory layout repair.
- When expanded content or long titles exceed these targets, preserve readability and normal scrolling. Document representative exceptions; never clip content to pass a coordinate assertion.
- At 200% zoom and 320px width, all content/actions remain reachable. Use local spatial overflow where layout relationships need it.

## 6. Session architecture and correctness contract

### Ownership

```text
AppRouter + CatalogProvider
  ├─ AlgorithmLibrary
  └─ ProblemPage (stable for current problem ID)
       ├─ useProblemSession → useTrace (single execution/playback owner)
       ├─ route adapter (view + input + step)
       └─ LearningWorkspace
            ├─ Playground OR Code OR Analysis
            ├─ controlled InputEditor
            ├─ History / existing comparison
            └─ Switcher / Help / Tour
```

Keep React/JSX, CSS modules, Router and existing API. A reducer may clarify session transitions; a new state library is not necessary. Do not bury session state inside a conditionally mounted tab. Avoid a new giant component that duplicates App's coupling.

| State | Owner / lifetime |
| --- | --- |
| Catalogue | Existing provider, one application session; retry explicitly |
| Problem ID | URL path |
| View | URL `view`; default Playground; replace history on view changes |
| Draft + dirty flag + draft revision + field errors | Current problem session; survives view/editor remount; reset on different problem |
| Submitted snapshot + request identity | Captured by session before request; independent of further edits |
| Committed run + resolved input + selected step | Trace/session; replaced atomically only by matching accepted result |
| Playing / timer | One trace owner; pause rules below |
| Source scroll + follow state | Session presentation state; survive view changes, reset for different problem |
| Theme / speed / stars / watched / streak / last visited / presets | Existing validated storage adapters/keys |
| Split enabled / ratio | Small validated versioned preference; narrow screens override presentation safely |
| Focus / modal / expanded transient UI | Local presentation; no refresh promise; never persist modal-open state |
| Library return context | URL + history-entry session scroll cache |

Do not promise unsaved drafts survive refresh or problem changes. Saved presets and shareable executed input already cover deliberate persistence. Warn about unsaved drafts on switching away only if the interaction review establishes a real need; avoid blocking routine navigation by default.

### Required correctness fixes before shell migration

1. **Late schema defaults:** InputPanel currently initializes on mount and problem ID changes, not a later matching `inputSpec`. Initialize once for the correct problem/spec when ready; never overwrite an edited draft due to unrelated rerenders or a stale detail response.
2. **Saved input mismatch:** preset load currently calls `onRun(preset.values)` without updating displayed fields. Update the draft and execute the same immutable captured values. Other case likewise loads and runs; Randomize/Restore defaults edit only.
3. **Run snapshot mismatch:** inspect `useTrace.runInput`; the current success path replaces steps without refreshing `resolvedInput`. Commit trace, resolved/submitted input and related run metadata together. Prefer backend `resolvedInput`; otherwise explicitly use captured submitted values plus known defaults, not a later draft.
4. **Sharing before success:** `runAndShare` currently writes input before the run succeeds. A rejected input must not become the URL identity of an old displayed run. Return an explicit success/failure outcome from the run operation or publish from committed run state.
5. **Stale query closures:** coordinate view, step and input changes through one route adapter or functional merging of latest search parameters. Do not use callback dependency suppression as a synchronization strategy.
6. **Unknown ID substitution:** current metadata selection and redirect can use the first catalogue entry. Resolve the requested ID honestly; detail/network/offline states must not substitute a different problem.
7. **Failure recovery:** preserve the last valid run for rejected/failed reruns with a visible “Previous result; new run failed” label. On initial failure there is no old result to retain. An empty valid execution, malformed trace and untraced response need distinct status; do not call any of them successful completed animation.

Write a regression test that fails against each relevant current behavior before implementing its fix. Preserve abort and request-version guards after every asynchronous stage, including body decoding. A delayed older response must never replace the newly selected problem/run.

### Transition rules

| Event | Required transition |
| --- | --- |
| Edit / randomize / default | Draft changes only; current run input stays unchanged |
| Run | Pause; capture immutable input and revision; clear obsolete submission errors; indicate pending; disable duplicate submission |
| Edit while pending | Allowed if draft/submitted revisions remain distinct; response does not overwrite newer edits |
| Run success | Atomically commit matching result/input/status; step first; paused; update share parameters |
| Validation failure | Preserve run and draft; field errors belong to submitted revision; focus error summary/first relevant field |
| Network / malformed failure | Preserve prior run only with explicit old-result label; Retry targets failed submission; never claim draft was run |
| View / Code subview change | Pause, keep draft/run/step; change presentation only |
| Open dialog / enter focus / hidden browser document | Pause; do not auto-resume on return |
| Seek / restart playback | Pause and move within same run; keep draft and input snapshot |
| New problem | Abort/supersede requests; reset problem-bound state; resolve matching defaults; keep global preferences |
| Resize / theme change | Preserve session; remeasure layout; do not fetch or seek |
| Unmount | Cancel requests, timers, observers and listeners; release scroll locks |

### URL and compatibility rules

- Preserve `/problem/:id`, encoded `input`, and 1-based `step`; add `view=code` or `view=analysis`. Default Playground may omit `view`.
- Existing valid shared custom input must execute before restoring its selected step. Invalid input gets an actionable message; do not silently report successful restoration of defaults. Out-of-range step follows a documented safe reset and visible explanation.
- Avoid transient default-run URL mirroring overwriting incoming input/step during restoration. New view changes preserve input/step; a new problem selection intentionally clears the previous problem's input/step.
- Only successfully committed executions change share identity. Copy link identifies the displayed run/view/step, not unsaved edits. Indicate when an input cannot be represented by the existing encoding/length limits.
- Input in a URL is visible to recipients and browser history. Keep sharing deliberate and do not introduce sensitive-data persistence, analytics or logs of raw inputs.
- Retain established storage keys: inspect actual adapters rather than guessing from docs. Theme, speed, progress, presets and recent searches must remain usable after upgrade. Corrupt/denied storage falls back safely.
- Keep watched semantics tied to meaningful trace completion. A truncated trace must not be presented as completed learning. Do not mark a problem watched merely because it rendered.

## 7. Complete feature-preservation checklist

Every row needs a destination and evidence. Existing capabilities are mandatory, even if an old planning document calls them deferred. A moved feature is not a removed feature.

| ID | Capability | New destination / proof |
| --- | --- | --- |
| F01 | Complete catalogue and metadata | Library; every unique ID reachable beyond first batch |
| F02 | Ranked search/highlighting | Library + switcher; retained scoring fixtures |
| F03 | Category/runnable filters | Library; combined query/filter tests |
| F04 | Recents/storage safety | Search; bounded compatible recents, denied storage test |
| F05 | Search shortcut/keyboard selection | Single active shortcut owner; modal/list navigation |
| F06 | Direct problem links | Same path; intended problem on refresh |
| F07 | Catalogue loading/retry/dedup | Shared provider; no extra fetch per problem |
| F08 | Default detail/execution | Session; correct request/response identity |
| F09 | Custom execution | Controlled editor; actual Java response changes with input |
| F10 | Randomize/default restoration | Draft-only actions; bounds retained |
| F11 | Help/bounds/field errors | All input forms; associated errors + previous-run recovery |
| F12 | Play/pause/step/reset/seek/speed | One controller shared by active views |
| F13 | Original playback shortcuts | No event theft from inputs, tabs, sliders or dialogs |
| F14 | Narration/position | Same current step everywhere |
| F15 | Canvas/legend | All registry mappings; honest unsupported state |
| F16 | Queue/grid companions | Stable presence derived from full run, correct prior snapshots |
| F17 | Capture and click-to-seek | History; preserved capture + all-family textual list |
| F18 | Java/active line | Code and split; actual anchor-derived line |
| F19 | Variables/frames/containers | Analysis and necessary companions; full values inspectable |
| F20 | Complexity | Analysis; real metadata, honest absence |
| F21 | Error boundary/empty/malformed/unavailable | Distinct recoverable states; no unrelated animation |
| F22 | Truncation | Persistent status; incomplete is never labeled finished |
| F23 | Checked-in offline sample behavior | Explicit sample identity and valid sample inputs only |
| F24 | Collapse/focus intent | Spacious defaults, focused views, optional split, Focus |
| F25 | Theme/reduced motion | Light/dark/system; semantic execution colors retained |
| F26 | Continue/daily pick/streak/starred review | Library learning section; same selection/storage behavior |
| F27 | Watched/starred progress | Library + header; no mark-watched on visit alone |
| F28 | Curriculum previous/next | Context navigation; actual authored order |
| F29 | Statement/examples | Readable disclosure; constraints retained |
| F30 | Alternate and saved inputs | Editor; load-and-run updates visible draft correctly |
| F31 | Other-case comparison | On-demand comparison; authoritative second execution/errors |
| F32 | Shared input/step/copy link | Route adapter; refresh restores successful run and step |
| F33 | Anchor coverage | Source inspector; covered/uncovered branches still inspectable |
| F34 | Welcome/tour/help | Help and adapted targets; dismissal/replay preserved |
| F35 | Extended player keys/persisted speed | J/K/L, comma/period, Home/End and speed controls preserved |

Maintain the existing keyboard meanings from `ShortcutHelp` and `useKeyboardShortcuts`; do not invent incompatible replacements based on this table's shorthand.

## 8. Input and renderer coverage

### All input contracts

| Type | Required cases |
| --- | --- |
| INT | Bounds, temporary empty value, invalid integer; no NaN submission disguised as zero |
| STRING | Whitespace, Unicode, long values, actual maximum length |
| INT_ARRAY | Add/remove/edit, minimum/maximum length, negatives/duplicates |
| LINKED_LIST | Sequence serialization, one node, longest allowed example |
| BINARY_TREE | Sparse/null slots; null never becomes zero |
| INT_GRID | Rectangular edits, row/column bounds, local overflow, invalid cell |
| GRAPH | Isolated node, weighted edge, endpoint validation, editable labels/controls |

Unknown field types render an explicit unsupported-input explanation. Labels, descriptions and field errors must be programmatically associated. Large form sections scroll with the page. Preserve editor-specific validation; the server remains authoritative.

### All registry keys

| Keys | Existing renderer | Review focus |
| --- | --- | --- |
| Array, Bits | ArrayCanvas | Values/indices/pointers; Bits retains its fixed-width bit semantics |
| Window | WindowCanvas | Moving bounds, entering/leaving elements, window state |
| SearchSpace | SearchSpaceCanvas | Low/mid/high and discarded/remaining regions |
| String | StringCanvas | Characters, positions, Unicode behavior, long sequence |
| PriorityQueue | HeapCanvas | Heap/tree and array relationship, insertion/removal |
| Matrix | GridCanvas | Axes, cells and board semantics; queue companion |
| DpTable | DpTableCanvas | 1D/2D/slice states, recurrence, predecessor/current cell |
| Tree | TreeCanvas | Balanced/skewed structures; edges/labels |
| Graph | GraphCanvas | Directed/weighted/disconnected structures; queue companion |
| LinkedList | LinkedListCanvas | Next/prev/cycle/child/random links |
| Stack | StackCanvas | Top/empty state; grid companion where present |
| Queue | QueueHeroCanvas | Front/back/FIFO and empty state |
| Trie | TrieCanvas | Shared prefixes and terminal markers |
| RecursionTree | RecursionTreeCanvas | Growth/current frame/return/backtracking |
| Dsu | DsuCanvas | Parent/group relationships and changed connections |
| Interval | IntervalCanvas | Bounds/overlap/result alignment |

Generate a coverage manifest from the registry and live catalogue; choose runnable representatives and record why they cover each structural variant. Add fixtures for deterministic tests, but do not substitute fixtures for every real-backend acceptance journey. Reconcile new registry keys before closing release.

Companion presence comes from the whole run so panels do not blink in and out when empty. Preserve existing last-payload semantics. Resizing/view changes must not cache zero geometry. If new pan/zoom is necessary, provide keyboard actions, Fit/Reset and visible scale; avoid interfering with page scrolling.

## 9. Ordered implementation plan and PR boundaries

These are remaining-work packages, not claims of completed work. Execute in order except explicitly independent checks. Each PR should be small-to-medium, coherent, testable and usable after merge. If a package becomes too large, split behavioral extraction from presentation while preserving its acceptance gate. Do not split into broken scaffolding commits that are presented as finished features.

### P0 — Refresh baseline and lock acceptance

**Start here.** Read this file, repo rules, `REVIEW.md`, relevant code/tests and git status. Fetch latest main and create a fresh implementation branch; preserve unrelated user changes.

1. Run existing frontend tests/build and backend tests; record baseline failures separately.
2. Start the actual backend/frontend. Query stats and catalogue; record commit, browser and versions.
3. Capture current library, Array, Graph+Queue and 2D DP at 390×844 and 1366×768 in both themes. Measure stage bounds and first meaningful library content.
4. Create the feature/renderer/input evidence ledger. Confirm current routes, storage and sharing behavior.
5. Produce proposed reference layouts for those same surfaces, including phone Code view and an open editor. Use repo-native JSX/CSS prototypes or annotated screenshots, not fake execution data labeled as real.
6. Compare visual hierarchy to HLD. Record decisions and observed tradeoffs.

**Gate:** baseline evidence exists; every F01–F35 has an owner/destination; no unresolved fundamental navigation decision. Review is a bounded iteration, not permission to invent new product scope.

**Deliverable:** baseline ledger + reference designs, no main-route replacement yet. Estimate 0.5–1 day.

### P1 — Repair input/session truth before changing layout

**Depends on P0. Files:** InputPanel, useTrace, useShareableView/App route orchestration and their tests.

1. Write failing tests for late defaults, preset draft sync, committed custom input, invalid-run URL identity and rapid A→B request races.
2. Introduce stable draft/submission/committed-run boundaries with minimal existing UI change.
3. Make input editors controlled by the session; retain existing field components.
4. Return explicit execution outcomes; commit input/trace/status together; pause on submission.
5. Preserve prior valid result on failure with correct labeling; ensure no stale metadata is paired with new steps.
6. Introduce one query-parameter adapter preserving input/step/view; test restoration order.

**Gate:** current shell still works; a failed run cannot relabel an old run; draft survives an intentional test remount; all new regression tests prove meaningful differences. This is a behavior PR, not a visual rewrite. Estimate 1.5–2.5 days.

### P2 — Shared foundations and library refinement

**Depends on P0; integrate after P1 to keep one active PR. Files:** existing LearningHeader/Layout, AlgorithmLibrary, scoped tokens and search/catalogue tests.

1. Document/adopt spacing, typography, surfaces, focus and control sizes.
2. Reduce library pre-search height to the specified gates; improve Continue visibility.
3. Add mobile filter disclosure and active filter chips; preserve all filters and URL semantics.
4. Validate load more, every ID, no executions from browsing, dedup/retry and return-scroll restoration.
5. Review dark/light/system, keyboard and 320px layout. Remove obsolete dashboard styles only after proving no consumer remains.

**Gate:** improved library screenshots, first-result/search gates, browser Back journey, storage failure and error/empty checks. Do not report the already merged library as newly built. Estimate 0.75–1.25 days.

### P3 — Session-backed shell and Playground reference slice

**Depends on P1–P2. Files:** extract ProblemPage/LearningWorkspace/VisualizationStage around existing App behavior.

1. Add compact context header and statement disclosure; retain star/curriculum/help.
2. Build natural-scroll shell and accessible view rail with real target panels, not dead buttons.
3. Give Array, Graph+Queue and 2D DP useful stage dimensions; use existing registry/companions.
4. Place narration/playback with the stage; controlled editor below and visible Edit input navigation.
5. Integrate explicit unknown route/problem states, run status, retry and renderer boundary.
6. Keep replacement isolated behind a development-only harness/flag until P4 reference gate; do not advertise an incomplete public experience.

**Gate:** reference Playground real default/custom runs work at phone/laptop widths in both themes; no hidden critical input, lost metadata or fixed-height clipping. Estimate 1.5–2 days.

### P4 — Code, Analysis and integrated workspace route

**Depends on P3.** Split into consecutive PRs if necessary; replacement route becomes default only when this gate passes.

1. Implement Code view, active-line follow control and narrow-screen Diagram/Source.
2. Add optional wide split with keyboard resizing, bounded preferences and remeasurement.
3. Implement Analysis with current state, containers/frames and genuine complexity.
4. Connect view URLs; pause without recreating run; verify draft/errors/source position survive.
5. Exercise the full reference slice with Array, Graph+Queue and 2D DP, including edited input and shared-link refresh.
6. Activate replacement for normal routes after the reference slice is green. Keep rollback a normal frontend revert; do not retain two permanent public interfaces.

**Gate:** all three views show one run/step; all seven input contracts remain reachable; direct links and Back work; no feature is stranded in the old shell. Estimate 2–3 days.

### P5 — Switching, focus and guidance integration

**Depends on P4.**

1. Upgrade existing CommandPalette to the single switcher with View all results.
2. Remove permanent sidebar from default layout once its capabilities have destinations.
3. Implement Focus and precise restoration/escape behavior.
4. Consolidate shortcut ownership; respect handled events and native controls.
5. Adapt help, welcome and tour to actual views/disclosures; preserve persisted dismissal.

**Gate:** keyboard-only switch/open/cancel/current-problem flows, inert background, focus return, no accidental playback from tabs/sliders/buttons. Estimate 0.75–1.25 days.

### P6 — History, comparison and completion flow

**Depends on P4–P5.**

1. Add bounded history navigation and preserve capture exclusions.
2. Rehouse existing comparison; label scope/input identity and preserve retry.
3. Preserve copy link, presets, alternate case, progress and previous/next affordances across views.
4. Final-step UI offers Replay, Other case and Next problem without a blocking congratulatory overlay.
5. Make truncated, sample and failed-rerun statuses persistent and truthful.

**Gate:** jumping history synchronizes all evidence; comparison uses actual second trace; completion/progress/sharing stay correct. Estimate 0.75–1.25 days.

### P7 — All renderer and input families

**Depends on P6.** Prefer separate medium PRs for linear/string/search families, spatial/tree families, and grids/DP/companions.

1. Execute the manifest from section 8 on new shell, not an old isolated renderer story.
2. Verify first/middle/final/empty/boundary states and materially different valid inputs.
3. Fix sizing/overflow/label issues in wrappers first; change semantic renderer code only for demonstrated defects.
4. Exercise split resize, view re-entry, focus, long labels and local navigation.
5. Record all seven input editors on phones, including open virtual keyboard and error correction.

**Gate:** every registry key and input type has evidence; no hidden navigation entries or fallback rendering to hide failures. Estimate 1.5–2.5 days.

### P8 — Accessibility, performance and usability iteration

**Depends on P7; perform relevant checks throughout earlier phases too.**

1. Complete the browser/accessibility/state matrix below.
2. Measure playback/seek/history on long traces and compare production bundle against fresh P0 baseline.
3. Perform learner tasks with first-time and returning-user perspectives; ideally obtain independent participants/owner feedback. If only the agent walks through them, label it an expert walkthrough, not user research.
4. Fix observed friction in a bounded iteration; rerun affected tasks. Include a before/after visual comparison against both old DSA and HLD's hierarchy.

**Gate:** no critical task failure; measured evidence, not “looks premium”; outstanding human checks explicitly recorded. Estimate 1.5–2.5 days.

### P9 — Cleanup and release handoff

**Depends on all gates.**

1. Remove dead layout CSS, unused legacy components/routes, stale keyboard handlers and prototype flags after checking imports.
2. Update README and implementation ledger with actual shipped state and limitations. Do not leave “not implemented” text after routing the replacement.
3. Run full frontend/backend/build/startup checks and review generated artifacts/diffs. No unrelated golden regeneration.
4. Complete F01–F35 evidence and renderer/input matrix; record release and rollback commit.
5. If the owner authorizes publishing, push a focused branch, open PR, wait for all required checks and merge. Otherwise leave reviewed work local and report it accurately.

**Gate:** no hidden incomplete phase; clean handoff, reproducible checks, known limitations and next steps. Estimate 0.5–1 day.

### Effort and sequencing summary

Remaining estimate: approximately **11–18 focused engineering days**, including verification and one meaningful design iteration. This is not agent wall-clock time or a delivery promise. P0 should refine it; P4 is the mandatory re-estimation point. Renderers, accessibility and state repair are the main uncertainties. Do not shrink mandatory acceptance to preserve the estimate.

Critical path: P0 → P1/P2 delivered sequentially → P3 → P4 → P5 → P6 → P7 → P8 → P9. No parallel agents are assumed or required. The existing merged library reduces implementation work but does not eliminate its review.

## 10. Verification matrix and release gates

### Automated behavior tests

Add meaningful tests next to owners; do not replace semantic assertions with snapshots of new markup.

- Catalogue: duplicate IDs, retry, cancellation, no per-problem refetch, no execution on library visit, empty/malformed data.
- Library: combined filters, invalid parameters, >50 results, loading continuation, recents, stars, denied storage, return context.
- Session: late spec, dirty draft protection, saved preset sync, success/validation/network/empty/malformed outcomes, matching input snapshot, abort after body decode, older response ignored.
- Navigation: unknown ID never executes a different problem; direct view link; invalid view; Back/Forward; same-problem switch; refresh of valid/invalid custom shared input and out-of-range step.
- State continuity: advance to a nontrivial step, edit draft, switch all views and focus, resize, toggle theme; assert same run/step/draft and no extra execution requests.
- Keyboard: tab arrows/Home/End do not seek; Space on a focused native button does not double-toggle; slider arrows do not also step; inputs/editable fields suppress player shortcuts; modal Escape priority and focus restore.
- History/comparison: bounded DOM, every step accessible, click-to-seek identity, actual second input/trace, errors and retry.
- Progress: only valid completion marks watched; preserve stars/presets; no completed label for truncated/sample failure.
- Renderer contracts, semantic tokens and existing trace decoder tests remain intact.

### Browser matrix

Required sizes: 320×568, 390×844, 768×1024, 1366×768, 1440×900; additionally short landscape and 200% browser zoom. Test Chromium plus a second engine when available; record omissions rather than implying cross-browser coverage. Both light/dark, system switching and reduced motion.

Every renderer: laptop + phone inspection of representative real runs. Deep journey checks: Array, Graph+Queue and 2D DP in both themes. Every input type: phone edit/run/error correction. Include long title, long narration, maximum bounded input and denied storage.

States to exercise: loading catalogue/detail; empty catalogue; zero matches; backend unavailable with/without sample; 404; 501; missing input contract; field 400; execution pending; network error; malformed/empty/truncated trace; renderer exception; final step; comparison failure; invalid shared link; copy failure; corrupt preferences.

### Accessibility checks

- Semantic landmarks, skip link, heading order, labeled controls and one current tabpanel.
- Manual-activation tabs: arrows/Home/End move focus; Enter/Space activate; `aria-selected`, roving tabindex and panel labeling agree.
- Keyboard-only primary journeys; visible focus never clipped/obscured by sticky content.
- Dialog focus containment/inertness/restoration; no nested modal traps; clean unmount behavior.
- Field labels/help/error references and announced validation summary.
- StepStateSummary or equivalent describes actual diagram state. Do not remove textual alternatives while modernizing canvas styling.
- Manual steps announce narration appropriately; autoplay does not flood a live region every animation frame. Let users pause and inspect current state.
- Contrast checks, non-color markers, text selection, zoom, reduced motion and touch target review.
- At least one real screen-reader walkthrough. If unavailable, mark this pending and avoid claiming full accessibility sign-off.

### Performance checks

- Measure against P0 on the same machine/build mode; record browser/CPU/trace size.
- One playback timer, one active primary canvas, no full-trace refetch on tab/theme/layout changes.
- Catalogue loaded once per application session except explicit retry; compare fetched only on demand.
- History default renders at most 50 textual entries; opening it must not mount one diagram per trace step.
- For a near-limit trace, measure initial decode/render, Play responsiveness, seek to beginning/middle/end, view switch and history paging. Target ordinary direct control feedback within ~100ms on the recorded test machine; diagnose repeated >200ms main-thread stalls rather than weakening tests silently.
- Record bundle raw/gzip deltas. Investigate >10% gzip growth relative to P0 and justify any new dependency. This is a review trigger, not permission to omit required UI.
- Check listener/observer cleanup and repeat navigation for leaks. Do not retain unbounded traces/drafts in global caches.

### Learner walkthrough tasks

1. Find a named algorithm from the library and explain what it does using statement/examples.
2. Run the default, step to an interesting change and identify the affected structure.
3. Change an input, run it and explain why the new result differs.
4. Inspect the active Java line and a variable without losing execution position.
5. Correct an invalid input while retaining the last valid result.
6. Try alternate/saved input, inspect history and compare the supported other case.
7. Copy a custom-run link, reload it and confirm input, view and step.
8. Switch problems, return to filtered library and resume the prior search position.
9. Complete tasks 2–5 on a phone and by keyboard.

Record success, wrong turns, hints required, lost context and task time. Primary journeys must finish without instructional assistance beyond the interface's own labels. Compare observations with baseline; do not invent user feedback or claim statistical superiority from one walkthrough.

### Actual project commands

Inside Linux/WSL, from repo root:

```sh
npm --prefix frontend ci
npm --prefix frontend test
npm --prefix frontend run build
mvn -f backend/pom.xml test
bash start-smoke-test.sh
```

Run the startup smoke command as defined by the current CI/repository if it changes. Start backend/frontend using repository scripts or their documented individual commands; backend is 8923 and Vite is 5180 at this baseline. Query `/api/problems/stats` and catalogue after the actual backend is ready.

No checked-in browser-test command is assumed. If adding Playwright/another browser harness, make it a justified dev-only dependency with scripts, deterministic server setup and CI requirements in the same PR. Save reusable test code in the repo; install artifacts outside source control. Avoid simultaneous expensive test suites on a constrained machine and do not dismiss failures without a rerun/investigation.

## 11. Evidence, review and handoff discipline

For each package update `IMPLEMENTATION_LOG.md` and use [REVAMP_TRACKER.md](REVAMP_TRACKER.md). Every entry must include:

```text
Package / status:
Base commit / branch / PR / merge commit (if any):
Learner outcome and exact behavior changed:
F-IDs and renderer/input cases covered:
Automated commands, results and failure investigation:
Browser, viewport, theme, real backend vs fixture:
Screenshots/artifacts and measurements:
Known limitations / blocked checks:
Rollback boundary:
Next unblocked package:
```

Use durable repository-relative evidence references or attached PR artifacts. `/tmp` screenshots are useful during work but are not permanent release evidence. Do not add sensitive inputs or huge generated trace dumps. Prefer a compact before/after set with captions over hundreds of unreviewed screenshots.

Apply all six repository review lenses: backend contract, frontend reliability, UI/UX, product scope, architecture and QA. Backend algorithms are not being redesigned, but the frontend must consume their actual responses correctly.

Status vocabulary: planned → in progress → review → done. A phase is not done because its components exist or tests compile. It is done only after its gate and evidence are complete. A skipped screen-reader/browser/user review remains pending or an explicitly accepted limitation; it is not a pass.

Do not remove failing tests merely because old navigation selectors changed. Rewrite the journey to reach the feature through its new location while preserving the original behavioral assertion. Bugs get regression tests; visual fixes get browser evidence.

## 12. Copy-paste instruction for the implementing agent

> Implement the DSA UI/UX revamp according to `docs/ui-revamp/IMPLEMENTATION_HANDOFF.md`, the authoritative remaining-work plan dated 2026-09-29. First read repository instructions and inspect the actual current code/git state. Treat the older planning documents as historical where they conflict with this handoff. PR #145 already delivered the library and shared catalogue; preserve and refine them rather than rebuilding them.
>
> Start at P0, record a fresh baseline in `REVAMP_TRACKER.md`, then follow P1–P9 in dependency order. Fix session/input/sharing correctness before moving state between views. Preserve F01–F35, all input types, every registered renderer, all existing storage and shared-link behavior. Borrow HLD's hierarchy and natural scrolling, not its exact layout or API. Keep the backend authoritative and do not fabricate data or substitute algorithms.
>
> Work in coherent medium-sized branches/PRs. Keep the product usable between changes; activate the new workspace only after the Array, Graph+Queue and 2D DP reference gate. Do not rewrite the whole app in one pass or add a framework/state library without a demonstrated need. Run regression tests and inspect real browser journeys, including phone/laptop, both themes and keyboard behavior. Update evidence and remaining work after every package. Re-estimate at P4 instead of dropping acceptance criteria.
>
> Follow the owner's current instruction for committing, pushing and merging; this plan does not itself authorize publication. Report completed versus pending work honestly. If a material product decision or contract change exceeds the scope, present the evidence and ask the owner. Do not declare the complete revamp finished until P9's release gate is met.

