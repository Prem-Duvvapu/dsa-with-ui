# Implementation log

## 2026-09-23 — reconcile the implementation baseline

Implementation started from local `6faea6f`, but fetching GitHub found five newer merged changes through `2b6b709`. They were incorporated before redesign code changes. This baseline update supersedes the earlier source observations wherever they conflict; the product goals and feature-preservation requirement remain unchanged.

Important changes in the current baseline:

- The README now reports 431 problems, not 433. The live API count is still to be measured in the browser audit.
- The home route is already a dashboard with Continue, today's pick, watched progress, streak, and a starred review queue. The new library must preserve those entry points.
- There are 17 renderer keys and 16 distinct renderer components/variants. String, Window, SearchSpace, and PriorityQueue now have dedicated renderers. Only Bits shares ArrayCanvas. Preserve these new renderers.
- Code already appears alongside the desktop canvas. The fixed viewport, narrow persistent sidebar, compact controls, and on-demand bottom-row height remain redesign targets. Mobile defaults were recently fixed; preserve that improvement.
- Theme selection, a command palette, problem statements, curriculum navigation, keyboard help, welcome/tour guidance, watched/starred progress, saved input presets, alternate-case execution, comparison, code anchor coverage, and shareable input/step URLs now exist.
- Input and step restoration from URLs is an existing capability. Earlier plan passages deferring custom-run sharing are superseded: preserve it and add the learning-view query without removing input/step parameters.
- The backend legacy controllers are retired. This frontend redesign must not restore or depend on them.

### Additional preservation obligations

| ID | Capability | Destination and acceptance |
| --- | --- | --- |
| F26 | Dashboard Continue, daily pick, watched count, streak, starred review | Compact library return-learning section; keep existing storage and daily-selection logic |
| F27 | Watched/starred progress | Library filters/indicators and problem header; opening a problem alone must not mark it watched |
| F28 | Curriculum previous/next navigation | Problem context section; preserve authored ordering and watched markers |
| F29 | Problem statement and examples | Readable disclosure above the workspace; retain all available content |
| F30 | Alternate case and saved input presets | Input editor; load-and-run still works, and displayed draft matches the loaded preset |
| F31 | Other-case comparison | Execution history/comparison section; preserve authoritative second trace and failure states |
| F32 | Shareable input/step URLs and copy link | Existing URL contract retained alongside the new view parameter; refresh restores the custom run and selected step |
| F33 | Source anchor coverage | Code walkthrough; covered and uncovered branches remain inspectable |
| F34 | Welcome, guided tour, shortcut help | Adapt guidance and tour targets to new layout; preserve replay/help access |
| F35 | Extended media-player shortcuts and persisted speed | Workspace controls; preserve J/K/L, comma/period, Home/End, speed keys without stealing tab/dialog interactions |

These features are already implemented upstream; they are not new scope or optional enhancements. Inventory/release references to F01–F25 now mean F01–F35.

### Delivery record

- Planning package committed and reconciled with current upstream. Only documentation changes relative to `2b6b709` are included in the first PR.
- Initial tests/build began against the older checkout; those results are not the current baseline. Current-baseline checks and browser audit are in progress and will be recorded separately.
- Further work proceeds in medium-sized PRs, one merged change at a time, as requested by the user.

## 2026-09-23 — library implementation batch

- Planning merged as PR #144 (`b57044f`). Implementation branch: `feat/algorithm-library`.
- Live API audit: 431 catalogued, 431 traced, zero duplicate IDs and zero orphaned tracers.
- Baseline frontend: 61 files / 467 tests passed; production build passed.
- Added a naturally scrolling library with URL-backed search, category/difficulty/progress/runnable filters, incremental results beyond 50, recent searches and return-scroll restoration.
- Preserved Continue, daily pick, watched progress, streak and starred review in a learning disclosure. Existing theme/progress storage remains compatible.
- Extracted shared catalogue loading, cancellation, deduplication and retry; navigation between library and problem no longer fetches the entire catalogue again. Offline samples remain explicitly labeled; no execution is fabricated.
- Added shared header/page styles. Existing problem workspace remains in place; this batch does not claim R2–R4 completion or a completed problem switcher.
- Initial implementation verification: 62 files / 472 frontend tests passed and production build passed. A later parallel local rerun under heavy CPU load had four App integration failures; focused rerun and PR CI must pass before merge. Local backend rerun was terminated (exit 143), so no successful backend result is claimed for that attempt.
- Real-backend browser checks: light/dark at 320, 390, 768, 1366 and 1440px; no horizontal page overflow or page errors. Load-more to 100, search, open and Back preserved query state. Browser artifacts are local in `/tmp/dsa-ui-review` (not permanent release assets).
- Visual review shortened the hero: first result moved from y840 to y703 at 1366×768. Small-screen filters still require scrolling; further mobile refinement belongs to the responsive pass.
- R1 catalogue/library work is implemented and awaiting PR checks/merge; comprehensive accessibility and renderer coverage remain pending.

## 2026-09-29 — P0 baseline and reference design

```text
Package / status: P0 — review (docs-only PR)
Base commit / branch / PR / merge commit: 82f2808 / docs/ui-revamp-p0-baseline / (this PR) / —
Learner outcome and exact behavior changed: none. No product code changes. Adds the authoritative
  handoff and tracker, records the fresh baseline, and adds REFERENCE_DESIGN.md with decisions D1–D10.
F-IDs and renderer/input cases covered: F01–F35 each have an owner package and destination in
  REVAMP_TRACKER.md; 17 registry keys and 7 input types have representatives chosen from the live
  catalogue.
Automated commands, results and failure investigation:
  npx vitest run  → 62 files / 472 tests passed (51s in the Linux worktree; 10m23s on /mnt/c, same result)
  npx vite build  → JS 347.59 kB (108.38 kB gzip), CSS 89.92 kB (16.17 kB gzip)
  mvn -B test     → 7,784 run, 0 failures, 0 errors, 511 skipped
  GET /api/problems/stats → 431 catalogued / 431 traced / 0 untraced / no duplicates / no orphans
Browser, viewport, theme, real backend vs fixture: Chromium (Playwright 1.63.0), 390×844 and 1366×768,
  light and dark, real Spring backend on 8923 and Vite on 5180.
Screenshots/artifacts and measurements: docs/ui-revamp/evidence/p0/*.jpg (6 of the 16 captures) and
  measurements.json. The library search is at y491 (1366) and y549 (390), against targets of ≤360 and
  ≤320. The desktop stage is 705×223 (Array) and 705×359 (Graph). There is no horizontal overflow and
  no console errors.
  A probe confirmed B1–B4: an unknown id redirects to Two Sum; the custom-run echo is stale; the draft
  is lost when the editor remounts; a rejected input is written into the URL.
Known limitations / blocked checks: the reference layouts are annotated wireframes, not code
  prototypes (the first real slice is P3). No second browser engine has been checked. Nothing about
  accessibility has been verified yet.
Rollback boundary: revert this docs-only commit.
Next unblocked package: P1 session/input/sharing correctness.
```

## 2026-09-29 — P1 session, input and sharing correctness

```text
Package / status: P1 — review
Base commit / branch / PR / merge commit: bc9829f / fix/ui-revamp-p1-session / (this PR) / —
Learner outcome and exact behavior changed:
  - What is on screen, the "Running on" line, the editor and the share link now describe the same
    run. A custom run updates the echo; the draft survives the editor closing; Load (preset) and
    Other case show what they ran; "Changes not run" marks a draft that differs from the run.
  - Only a successful run changes the link. A 400 keeps the run and the URL, lists the field
    errors in a focused summary, and keeps per-field messages. A network, malformed, empty,
    untraced or rate-limited rerun keeps the previous result, labelled "Previous result shown.
    The new run failed: …", with Retry (the failed submission) and Dismiss.
  - Shared links: the input runs before the step is restored, and nothing is mirrored into the
    URL until then. An out-of-range step and a rejected input are explained and removed from the
    URL. Inputs too large for a link are not encoded, and this is labelled.
  - An unknown /problem/:id shows "No algorithm called …" with a library link. Nothing else runs.
  - A truncated or offline-sample run no longer marks a problem watched. Unknown ids are no longer
    recorded as the last visited problem. A hidden tab pauses playback.
  - Architecture: useTrace commits a run atomically (tagged with its problem id) and returns an
    explicit outcome. useInputDraft and useProblemSession own the draft and restoration above the
    views. useShareableView is the single merging writer of step/input/view. InputPanel is
    controlled.
F-IDs and renderer/input cases covered: F06, F08, F09, F10, F11 (summary), F21, F22 (watched),
  F30, F32. No renderer changes.
Automated commands, results and failure investigation:
  RED first: the 39 new cases run against bc9829f → 29 failed (B1–B7 behaviours).
  npx vitest run → 64 files / 507 tests passed (baseline 472, +35).
  Seven existing integration tests opened /problem/two-sum against catalogues that do not contain
  it, and had passed only via the unknown-id redirect. They now open their own problem id; their
  assertions are unchanged. One useTrace assertion ("malformed rerun clears steps") now asserts
  the handoff's retained-and-labelled behaviour instead.
  npx vite build → JS 354.46 kB (110.98 kB gzip, +2.4% vs P0); CSS 91.53 kB (16.43 kB gzip).
Browser, viewport, theme, real backend vs fixture: Chromium (Playwright 1.63) at 1366×768 against
  the real backend, using docs/ui-revamp/evidence/p1/session-probe.cjs.
Screenshots/artifacts and measurements: evidence/p1/p1-notfound.jpg, p1-badlink.jpg; probe output
  recorded in REVAMP_TRACKER.md (browser evidence table).
Known limitations / blocked checks: the old fixed-height shell still squeezes the canvas when the
  editor and a notice are both open (baseline behaviour; P3 replaces the shell). The network-failure
  label was verified with fixtures only; the real backend was not taken down. `view` is carried in
  the URL but has no UI until P4.
Rollback boundary: revert the P1 squash commit; no storage keys or API contracts changed.
Next unblocked package: P2 foundations and library refinement.
```

## 2026-09-29 — P2 foundations and library refinement

```text
Package / status: P2 — review
Base commit / branch / PR / merge commit: 748b489 / feat/ui-revamp-p2-library / (this PR) / —
Learner outcome and exact behavior changed:
  - The library opens on a single heading and one sentence, then the progress line, which carries a
    visible "Continue: <title>" link when a last-visited problem exists. After that come the "Your
    learning" disclosure (with an explicit ▸ glyph) and the search. The illustration and the duplicate
    "Browse" jump are gone.
  - Phones: the search, the "Filters (N)" disclosure and the result count are visible without
    scrolling; the disclosure holds the same controls as desktop. Active filters show on every width
    as removable chips.
  - Scoped clearing: "Clear filters" keeps the query, the × clears only the search, and the empty
    state offers "Clear search and filters".
  - Load more announces "Showing X of Y" (role=status). When it removes itself, focus lands on the
    first new row instead of <body>.
  - Catalogue states: an offline sample says it is a sample ("browsing only"); a failed refresh of a
    live list keeps the list and says so; retry refetches only /api/problems.
  - Fixed: typing quickly into the search dropped characters ("binary search tree" became q=e).
    Cause: router navigations render in transitions, and URL state was being read during render.
    The new useLatestSearchParams keeps a synchronous copy and adopts only committed external
    navigation. useShareableView uses the same hook.
  - Foundations: spacing, width, type, target and focus tokens, plus the owner concept's chrome
    palette (paper, cards, forest-green accent) in all three theme declarations. Dashboard.module.css
    is deleted (no consumer).
  - The owner's playground-concept.png is committed and recorded in REFERENCE_DESIGN.md as the P3/P4
    visual target, with honesty adaptations.
F-IDs and renderer/input cases covered: F01, F02 (library), F03, F04, F07, F25 (tokens), F26, F27.
Automated commands, results and failure investigation:
  RED first: of the 14 new library/provider cases, 8 failed on 748b489 (chips, scoped clear,
  disclosure, load-more focus, Continue link, sample label, retained live list, source label). The
  other 6 guard behaviour that was already correct and are recorded as coverage, not as fixes.
  The palette guard failed on the first dark accent (too close to --settled); the accent was adjusted.
  npx vitest run → 65 files / 524 tests passed.
  npx vite build → JS 356.01 kB (111.39 kB gzip, +2.8% vs P0), CSS 93.22 kB.
Browser, viewport, theme, real backend vs fixture: Chromium 1.63 at 320×568, 390×844, 768×1024,
  1366×768 and 1440×900, light and dark, real backend.
Screenshots/artifacts and measurements: evidence/p2/*.jpg. Search top is y280 at 1366 (gate ≤360;
  P0 was y491) and y287 at 390 (gate ≤320; P0 was y549). The first result row (456–534) is fully
  visible at 1366×768. No horizontal overflow and 0 errors in any capture. The P1 session probe was
  re-run on the new adapter and passes.
Known limitations / blocked checks: no second browser engine; no screen-reader pass. The typing race
  cannot be reproduced in jsdom (act() flushes transitions), so the browser probe is the evidence and
  the unit tests cover the merge/adopt contract. The workspace (App) has not been restyled yet; it
  picks up the new page tokens only where it already used them.
Rollback boundary: revert the P2 squash commit; storage keys unchanged.
Next unblocked package: P3 session-backed shell and Playground reference slice, to playground-concept.png.
```

## 2026-09-29/30 — Independent review of P0–P2 and its fixes

```text
Package / status: P1/P2 hardening — #149 merged (de94c1c); follow-ups in review
Base commit / branch / PR / merge commit: 3db06a5 / fix/ui-revamp-session-review / #149 / de94c1c;
  de94c1c / fix/ui-revamp-review-followups / (this PR) / —
Learner outcome and exact behavior changed:
  #149 (review findings 1–8):
  - A shared link no longer leaks into the next problem: its input ran on the problem opened via
    Previous/Next.
  - A page left while its run was pending no longer navigates back when the run finishes.
  - Edits typed while a link restores are kept; the link's values are seeded into the draft first.
  - A newer submission cancels restoration cleanly.
  - A new link to the same problem restores its input and step, or reruns the defaults.
  - An unreadable input, or a step with no run to restore into, is explained.
  - The render that switches problems never shows the previous run.
  Follow-ups (findings 9–10):
  - Field, select and button borders use --control-border (≥3:1).
  - Filter chips and recent searches are 44px tall.
  - The tracker is reconciled.
F-IDs and renderer/input cases covered: F02–F04 (library controls), F06, F09, F21, F22, F32.
Automated commands, results and failure investigation:
  #149: 12 of 13 new cases failed on 3db06a5 (the #1 case POSTed {n:7} to the next problem, exactly as
  reported). 67 files / 537 tests passed. One lifecycle assertion waits for the router's URL transition.
  Follow-ups: the new palette guard and the 44px CSS guard fail with the stylesheets reverted and pass
  with the change. The disclosure-hide guard already passed; it is a guard, not a fix. The "starts at
  the top" test was renamed to what it actually proves.
Browser, viewport, theme, real backend vs fixture: Chromium 1.63, real backend.
  evidence/review-probe.cjs → review #1, #9 and #10 verified; library audit and session probe unchanged.
Screenshots/artifacts and measurements: see REVAMP_TRACKER.md browser table ("Review follow-ups").
Known limitations / blocked checks:
  - Review #2 (leave mid-run) and #3 (edit during restore) are verified in jsdom integration tests only.
  - No second engine and no screen reader.
  - The workspace (old App shell) controls do not yet use --control-border; that happens in P3.
Rollback boundary: revert this PR's squash commit, and/or #149's.
Next unblocked package: P3.
```

## 2026-09-30 — P3 session-backed workspace and Playground reference slice

```text
Package / status: P3 — review (behind the dsa-ui:workspace flag; the public page is unchanged)
Base commit / branch / PR / merge commit: b9f114e / feat/ui-revamp-p3-workspace / (this PR) / —
Learner outcome and exact behavior changed (flag on only):
  - A new problem page built to playground-concept.png: header with All algorithms · Switch problem
    (opens the existing palette) · Help · theme, and a labelled Menu on phones. Context: breadcrumb,
    title, one-line summary (first sentence of the real description; hidden on phones), difficulty,
    star, watched. "Problem & examples" holds the full statement and the original problem's
    constraints, labelled apart from visualizer limits. Previous/Next follow the authored section.
  - A sticky rail of manual-activation tabs (Playground / Code walkthrough / Analysis) with roving
    tabindex. The view lives in ?view and pauses playback on change.
  - Playground card: stage title by diagram family, Edit input (scrolls to and focuses the editor
    heading), Show code; run/link/offline/truncated notices; stage sized by family (360/440px desktop,
    300/340px phone); companions stack on phones; "Current step" narration panel; transport with
    Restart / Previous / Play / Next, seek and Speed. Then "Input used" with Changes-not-run and Copy
    link; the "Try your own input" card (InputPanel section variant: fields first, visible-text
    actions); Execution history (capture strip, same exclusions) and Compare other case.
  - Code walkthrough and Analysis are real panels reusing CodeViewer and MemoryComplexityCard, sharing
    the same narration and transport. The split and the analysis layout are P4.
  - Global player keys no longer act on keys a tab list or dialog already handled.
F-IDs and renderer/input cases covered: F12, F14, F15 (Array, Graph, DpTable), F16 (queue), F17 (capture in
  history), F18/F19/F20 (basic panels), F24 (views), F27, F28, F29, F32 (view param), F34 (help,
  welcome, tour reused). INT, INT_ARRAY, STRING and GRAPH editors exercised in the journey.
Automated commands, results and failure investigation:
  npx vitest run → 68 files / 553 tests passed. 14 new workspace tests (flag gating, context,
  titles, not-found, cross-view continuity with no extra execution, view link restore, tab keys
  not stepping, tabpanel labelling, Edit input focus, run-success/rejection focus, visible-text
  action names, spatial stage family, registry coverage of stage sizes, watched at the last step).
  One full run failed an App.lifecycle case while the browser audit ran concurrently; it passed 3/3
  in isolation and on a clean full rerun (load-related, as in RCA-053).
  npx vite build → JS 378.57 kB (117.74 kB gzip, +5.0% vs P2; both shells ship until P4 retires the old one).
Browser, viewport, theme, real backend vs fixture: Chromium 1.63, real backend, 320/390/1366, both themes.
Screenshots/artifacts and measurements: evidence/p3/*.jpg; REVAMP_TRACKER.md P3 row.
Known limitations / blocked checks:
  - The stage starts at y389 at 1366×768 (target ~280), a documented exception kept for the owner's
    hierarchy.
  - Legends are still the generic five keys; WindowCanvas cells are still small. Both are P7.
  - No Focus mode yet (P5); the tour still points at some old anchors (P5); no split view yet (P4).
  - No second engine and no screen reader.
Rollback boundary: revert the P3 squash commit, or clear the flag.
Next unblocked package: P4 (Code split, Analysis layout, activation as the default route).
```

## 2026-09-30 — P4a Code walkthrough and Analysis (behind the flag)

```text
Package / status: P4a — review; P4b (default route, retire the old shell) follows
Base commit / branch / PR / merge commit: b0da81f / feat/ui-revamp-p4a-code-analysis / (this PR) / —
Learner outcome and exact behavior changed (flag on):
  - Code walkthrough shows the diagram and the source side by side when the MEASURED container fits
    480px + 360px + 24px (not a viewport breakpoint). The separator (role=separator, aria-valuenow/
    valuetext) resizes by pointer or keyboard (←/→ 5%, Home/End bounds 45–70%, Enter or double-click
    resets to 60%). The ratio persists as dsa-ui:codeSplit {v:1, ratio}, clamped on read; an unknown
    version falls back.
  - Narrower containers get Diagram / Source subviews with the same narration, controls and step.
  - Source at 14px, scrolling on both axes inside its own pane. Follow execution keeps the active line
    in view; wheel, touch or scroll keys suspend it; "Return to active line" restores it. Scroll
    position and follow state survive view changes and reset per problem.
  - Analysis: reading columns. Variables are shown in full, with null and empty stated explicitly and
    long values behind a disclosure. The call stack shows its order and marks the current frame.
    Containers show Front/Back or Top. Complexity comes from the backend and reads "unavailable" when
    missing. Sections appear only if the run uses the structure. A Graph, Grid or Array run's
    container reads as its queue, matching canvas/companions.js.
F-IDs and renderer/input cases covered: F12 (shared transport), F18, F19, F20, F24, F33 (unreached badge
  kept in the source header).
Automated commands, results and failure investigation:
  11 new cases in CodeAnalysis.test.jsx. The first 10 FAILED against the P3 workspace (stashed) and
  pass now. The 11th (run-level presence and graph-queue semantics) came from browser evidence: BFS
  showed an empty call stack and an unlabelled queue.
  npx vitest run → 69 files / 564 tests passed. npx vite build → JS 387.74 kB (121.16 kB gzip).
Browser, viewport, theme, real backend vs fixture: Chromium 1.63, real backend, 1366×768 and 390×844.
Screenshots/artifacts and measurements: evidence/p4/*.jpg; REVAMP_TRACKER.md P4a row.
Known limitations / blocked checks:
  - In full-page Playwright captures the fixed skip link can appear. In the live page it is hidden
    (top −66px) until focused.
  - The split has not been checked with a second engine or at 200% zoom (P8).
Rollback boundary: revert the P4a squash commit; the flag keeps it off the public page.
Next unblocked package: P4b.
```

## 2026-09-30 — P4b the workspace becomes the problem page

```text
Package / status: P4b — review (P4 gate: see below)
Base commit / branch / PR / merge commit: ce99754 / feat/ui-revamp-p4b-activate / (this PR) / —
Learner outcome and exact behavior changed:
  - /problem/:id renders the new workspace for everyone. The dsa-ui:workspace flag, App.jsx and
    App.module.css are removed; two public interfaces are never kept.
  - Parity added for the retired shell:
    - a catalogue-failure notice with "Retry loading the catalogue" (the switcher and curriculum depend on it)
    - a guided tour rewritten for the new anchors (switcher, view rail, canvas, controls, input used,
      editor, history, theme), started from Help ("Take the guided tour", above 768px as before) or
      from the welcome screen
    - shortcut help reworded for the switcher
  - Robustness: the Edit input scroll no longer throws where scrollIntoView is unavailable.
P4 gate:
  all three views show one run/step (journey: 0 extra executions) · all seven input contracts reachable
  through the unchanged InputPanel editors · direct links, the view/step/input link and Back work ·
  nothing is stranded in the old shell. Old surfaces and their new homes:
    sidebar → library + switcher
    header badge → switcher search
    code toggle → Code walkthrough
    memory/complexity → Analysis
    mobile tab card → views
    breadcrumb/SectionNav/statement → context header
    Controls copy link → Input used row
    speed buttons → Speed menu
    LiveTraceTicker → narration live region
Automated commands, results and failure investigation:
  App.integration.test.jsx (≈60 cases) ported case by case to workspace/Workspace.integration.test.jsx
  (45 tests, parameterised where the old ones repeated). Every behavioural assertion is kept and reaches
  the feature at its new location; the retired UI's own mechanics (sidebar drawer, backdrop, header
  badge) are replaced by tests of their successors (phone Menu → switcher, switcher search).
  App.session and App.lifecycle moved to workspace/*.integration.test.jsx against the workspace; only
  selectors changed ("Run input"; editor remount via a view switch instead of "Done editing").
  AppRouter.test mocks the workspace instead of App.
  npx vitest run → 69 files / 560 tests passed.
  npx vite build → JS 337.29 kB (106.91 kB gzip), now BELOW the P0 baseline (108.38 kB) with the old
  shell gone.
Browser, viewport, theme, real backend vs fixture: Chromium 1.63, real backend, 320/390/1366, both themes, no flag.
Screenshots/artifacts and measurements: evidence/p4/p4b-*.jpg; REVAMP_TRACKER.md P4b row.
Known limitations / blocked checks:
  - Legacy leaf components are now unused except by their own unit tests, and are left for P9 cleanup:
    Header, Breadcrumb, SectionNav, ProblemStatement, Controls, LiveTraceTicker, Sidebar, SearchBox,
    MemoryComplexityCard, useLayoutPreferences, the InputPanel 'panel' variant, App.test.jsx.
  - README still describes the old layout (P9).
  - Focus mode and switcher upgrades are P5; history paging and completion are P6.
Rollback boundary: revert the P4b squash commit (restores App.jsx and the flag route).
Next unblocked package: P5 switching, focus and guidance.
```

## 2026-10-01 — Remediating docs/ui-revamp/INDEPENDENT_REVIEW_DB8683B.md

```text
Package / status: review remediation — #155 merged (d40b9cb: B1, B2, B3, S1, S2, S3, S4); this PR: S5, S6
  and a browser-found identity defect; S7, S8, migration-assertion restoration and N1–N3 dispositions
  remain
Learner outcome and exact behavior changed:
  #155:
    - A same-page navigation retires an in-flight submission.
    - A link that cannot be run keeps and re-points to the custom run on screen, and says so.
    - Every same-page navigation applies its own step.
    - No queue/stack claims for Graph, Grid or Array containers, in Analysis or in the companion pane.
    - Explicit shortcut ownership: dialogs, the source pane, the separator, defaultPrevented.
    - A subview change pauses.
    - Source memory keeps both axes and is tagged by problem.
  This PR:
    - The separator drags with pointer capture (no window listeners; release, cancel and lost capture
      all end it).
    - The separator applies and announces the ratio the pane minimums allow at the measured width.
    - The separator line uses --control-border, with a 44px hit area.
    - <main> stays the landmark; the tabs control the labelled tabpanel; the roving tab stop follows
      focus.
    - URL identity now includes path and query as well as location.key.
Automated commands, results and failure investigation:
  RED first: 16 cases on db8683b for #155; 5 for S5/S6 (separator listeners, cancel, geometry; main
  landmark/tabpanel; roving focus); 1 for the shared-key identity (low-level Router harness).
  Two older separator tests asserted 70% at a 1200px container, where the pane minimums allow only
  69%; they now use a 1500px container. The geometry case covers the cap.
  npx vitest run → 70 files / 586 tests passed; npx vite build passed.
Browser, viewport, theme, real backend vs fixture: Chromium 1.63, real backend, a POST held via a
  Playwright route, 1366/1000/390 px. Results are in REVAMP_TRACKER.md ("Review db8683b fixes").
  The first browser run of B1 FAILED although jsdom passed: the browser gives page-load history entries
  the key "default", so a navigation between two such entries was invisible. Fixed and re-verified (the
  POST is aborted and the default stays).
Known limitations / blocked checks: S7 and S8 open. Precise queue/stack labels need a backend
  containerKind field. Owner-requested full statements are scheduled after S5–S8. Owner-supplied DFS
  code for num-provinces is queued as its own tracer PR. No second engine; no screen reader.
Rollback boundary: revert this PR's squash commit.
```

## 2026-10-01 — Review db8683b: S7, S8, migration assertions, notes (remediation complete)

Historical disposition: the subsequent re-audit found R1 (horizontal source arrows) and
R2 (failed-link notices without a run). Their fixes and current evidence are recorded below.

```text
Package / status: review remediation — complete with this PR (after #155, #156)
Dispositions (INDEPENDENT_REVIEW_DB8683B.md):
  B1 fixed (#155, plus #156 for page-load key collisions)    B2 fixed (#155)    B3 fixed (#155)
  S1 fixed (#155)   S2 fixed (#155)   S3 fixed (#155)   S4 fixed (#155)   S5 fixed (#156)   S6 fixed (#156)
  S7 fixed (this PR): load failures are a session-level alert in every view; a rejection that arrives while
     the editor is unmounted shows "Your input could not run" with "Fix it in the editor".
  S8 fixed (this PR): the not-found page renders the shared dialogs, so Help and Switch problem work.
  Weakened tests restored (this PR):
    - DP/Graph/Tree full-stage case asserts the DP table, one legend and one shell again (proven: it
      fails when DpTable is routed to ArrayCanvas).
    - The phone case opens Code and Analysis and checks the editor.
    - Merged-catalogue cardinality is asserted through the tour's stated total.
    - The session test asserts URL mirroring.
    - The tab-panel and CodeAnalysis gaps are covered by the S3–S6 tests.
  N1 deferred: y389 recorded as an unmet target pending owner acceptance; P8 experiment.
  N2 documented: single-writer rule in useLatestSearchParams.
  N3 deferred to P9: groupBySection, normalizeCategory and SearchBox (via the dead Sidebar) added to the
     cleanup list.
  Flaky case: the Lifecycle review-#1 URL wait is raised to 4s (same assertion); it failed twice under load.
Automated: 5 new S7/S8 cases failed before the fix (the Playground case was already correct and is a
  guard). npx vitest run → 71 files / 593 tests, passed twice consecutively. npx vite build passed.
Browser: Chromium against the real backend; see the REVAMP_TRACKER.md rows "Review db8683b fixes" and
  "Review db8683b S7/S8".
Remaining limits: no second engine, no screen reader, no 200% zoom pass. Precise queue/stack labels need
  a backend containerKind field.
Next: Number of Provinces DFS tracer (owner's code), then per-topic batches aligning code to the owner's
  solutions with own-words statements, then P5.
```

## Dark mode: one neutral, layered palette (owner request, after #160)

```text
Package / status: owner-requested theme pass — complete with this PR
Problem (measured before the change, evidence/dark-mode/before-*.png):
  - Three dark families were in use at once: body on --bg-page #090d16 (navy), the redesigned
    surfaces on --surface-page #0b1012 (green-black), and the canvas on --bench-fill/--canvas-* navy-slate.
  - Page and card were about 2% apart in lightness, so cards barely lifted off the page.
  - Idle graph nodes and edges were drawn in --bench-rule (a decorative hairline), so a #333-ish rim sat on a
    near-identical fill and the nodes nearly vanished.
  - No color-scheme was declared, so native checkboxes, selects and scrollbars rendered light in dark mode.
  - The welcome and tour primary buttons and the .btn focus ring were still violet (a third chrome hue).
  - Four modal scrims each hardcoded a navy rgba.
Change (index.css dark :root only, unless stated):
  - Neutral layered greys, LeetCode-style: page #1a1a1a, card #262626, well #202020, hairlines #333/#444.
  - Ink #e8e8e8 / #a8a8a8 / #8f8f8f.
  - Legacy --bg-*, --text-*, --border-* and the --canvas-* roles now alias the same family.
  - color-scheme: dark on :root, and light in both light declarations.
  - .btn-primary uses the one chrome accent (--accent / --accent-ink); .btn:focus-visible uses --focus-ring.
  - A --scrim token is defined in all three theme blocks and used by the four dialogs.
  - GraphCanvas: idle node rim and idle edge are --canvas-edge, and the visited rim is --bench-ink-dim, so
    untouched, reached and current stay distinct in both themes.
  - Light values are unchanged apart from the scrim (one value, previously 0.62–0.68).
Automated: designTokens.test.js (15/15, including every 4.5:1 ink role, the 3:1 control edge and the
  accent/settled separation) passes on the new values. npx vitest run → 71 files / 594 tests. vite build OK.
Browser: Chromium against the real backend.
  - Dark at 1440 and 390, light at 1440: library, Graph (step 5), DP table, Code walkthrough, welcome dialog.
  - Body is rgb(26,26,26) with color-scheme dark (light: rgb(238,241,244) with light).
  - Horizontal overflow is 0 on every page; no console or page errors.
  - Evidence: docs/ui-revamp/evidence/dark-mode/.
Not done: syntax colouring in the source pane (a feature, not a palette fix — offered separately); no
  second engine; no 200% zoom pass for this change.
```

## Syntax colouring in the source pane (owner request, after #161)

```text
Package / status: owner-requested — complete with this PR
Change:
  - code/highlightJava.js: a dependency-free Java tokenizer. It reads the whole source before splitting
    into lines, so block comments and text blocks spanning lines are coloured correctly.
  - CodeViewer renders keyword / type / string / number / comment tokens as spans. Plain runs stay text.
  - Five --syntax-* tokens are defined in all three theme blocks. Amber and green are deliberately not
    used: they mean "running now" and "resolved", and a coloured string or comment would compete with the
    execution highlight.
Guards:
  - highlightJava.test.js: the tokens re-join to the exact source. It also covers spanning comments and
    text blocks, a "//" inside a string, classification, and termination on unterminated input.
  - CodeViewer.test.jsx: rendered line text equals the source, and tokens carry their classes. Proven RED
    against the old CodeViewer.
  - designTokens.test.js: each syntax colour is ≥4.5:1 on the code ground (--bg-page) and on the active
    line (--probe-wash), in both themes. It caught the light comment colour at 4.08:1, which is now its
    own #5c6672.
  - The phone integration case asserts the source region's text (the line is now several spans).
Automated: npx vitest run → 72 files / 601 tests. vite build OK.
Browser: Chromium with the real backend; num-provinces and rotting-oranges code views; dark and light at
  1440, dark at 390. Keyword colour resolves per theme (rgb(130,170,255) / rgb(29,78,216)). Overflow 0, no
  errors. Evidence: docs/ui-revamp/evidence/syntax/.
```

## 2026-10-03 — Re-audit R1/R2 and test-strengthening follow-ups

```text
Package / status: implemented and verified in the fix/ui-revamp-reaudit change set
Base: ea5e579 (latest origin/main at start). Separate worktree: /home/prem/dsa-ui-audit-fixes.
R1: the focused source owns ArrowLeft/ArrowRight, preserves native horizontal scrolling,
  suspends following, and does not seek, change the shared URL, move focus or re-execute.
  The same arrows still step playback outside the source.
R2: link refusal describes an explicit committed-run state: custom, default, offline or none.
  No default-input claim for missing runs or unknown-input offline samples. A requested step
  with no run reports that it cannot be shown; it does not announce playback at step 1.
  The Analysis validation alert also makes no previous-run claim when no steps exist.
  Retained custom runs still restore their share identity; oversized inputs get the existing
  length notice rather than falsely claiming that the link identifies the retained run.
Test gaps: cancellation first proves a positive drag, then checks later moves are ignored;
  editor recovery now asserts focus. Existing migration assertions remain intact.
RED: 11/11 new R1/R2 cases fail against ea5e579 (two source directions; rejected/unreadable
  input with/without step, no run/offline sample; no-run Analysis validation alert).
GREEN: targeted 48/48; full frontend 72 files / 616 tests; production build passes.
Build: JS 343.74 kB / 109.23 kB gzip; CSS 97.75 kB / 17.65 kB gzip.
Browser: cached Playwright/Chromium, fixture API, light/dark, 1366x844 and 390x844, reduced
  motion. 40 checks pass; 0 page errors. Real native left/right scroll changes scrollLeft
  with step 3 and URL unchanged, focus retained and no extra GET/POST. Invalid links clear
  input/step parameters and show truthful no-run or offline notices. Recovery returns focus
  to the editor (StrictMode focuses its validation summary). Four source screenshots saved.
Evidence: docs/ui-revamp/evidence/reaudit/r1-r2-journey.cjs, results.json, source-*.png.
Limits: browser API fixtures, not real Java execution; backend unchanged and tests not rerun.
  No second browser engine, screen reader or 200% zoom pass. Stage y389, single URL writer
  enforcement and dead-code retirement remain the previously disclosed P8/P5/P9 work.
```

## Problem statements: Linked List and Stack & Queue (57 problems)

```text
Package / status: statements rollout, topic 1 of 8. Pilot: #159 (num-provinces).
Method:
  - Own-words statements. LeetCode, GfG and the owner's solutions-repo READMEs were used only as fact sources.
  - Every example is a real tracer run, and its answer was checked independently (by hand or by brute force).
  - Constraints are the original problem's, and only where certain; otherwise omitted.
  - Every GfG/takeUforward link was fetched or found by search. LeetCode numbers are public ids (the
    solutions-repo folder numbers are internal ids and differ).
Coverage: 57 of 61. Skipped because the tracer's final step holds no answer: intro-doubly-ll,
  clone-ll-random-pointer, min-stack, queue-stack-impl. A tracer change is needed before they can carry
  checked examples.
Defects found, not in any example:
  - InfixToPostfixTracer and InfixToPrefixTracer treat ^ as left-associative. A^B^C gives AB^C^ and ^^ABC,
    where ABC^^ and ^A^BC are correct. Fix to follow in its own PR.
  - flattening-ll is titled as GfG "Flattening a Linked List" but traces LeetCode 430 (multilevel doubly
    linked list). The statement describes what the tracer does; the catalogue/tracer mismatch is open
    for an owner decision.
Automated: StatementContractTest passes, re-running every example through TraceRunner.

## Problem statements: Binary Trees, BST and Tries (49 problems)

```text
Package / status: statements rollout, topic 2 of 8 (after #163). Same method and checks as #163.
Coverage: 49 of 56. Skipped because the final step holds no answer: construct-bt-pre-in, construct-bt-post-in,
  construct-bst-preorder (only a node count), bst-insert, bst-delete, correct-bst-swap (no final variables),
  implement-trie (only the last word inserted).
Notes:
  - Tree tracers read `tree` heap-indexed (children of i at 2i+1, 2i+2), not LeetCode's level-order format.
    Every statement says so. In 8 examples the two formats would build different trees.
  - Five ids return a multi-part answer. Each example shows one part, and its explanation says which.
Defects found, not in any example:
  - bst-validate's final narration claims a parent-vs-child check would miss [5,1,4,null,null,3,6]. It would
    not: 4 is the root's direct right child.
  - BinaryTreeLayout draws a grandchild of a missing node, e.g. node 4 in [1,null,2,3,null,null,null,4],
    though no traversal can reach it.
Automated: StatementContractTest passes (107 statements).

## Problem statements: Binary Search, Sliding Window and Greedy (56 problems)

```text
Package / status: statements rollout, topic 3 of 8 (after #164). Same method and checks.
Coverage: 56 of 58. Skipped: jump-game-1 (no true/false result variable) and floor-ceil-sorted-array
  (the answer is split across floor and ceil).
Source links: every source in the repo was re-verified mechanically. LeetCode links were checked through
  LeetCode's GraphQL API (scripted page fetches get 403), including that each "LeetCode N" label is that
  problem's real number; every other link must return HTTP 200. 168/168 pass. One dead takeUforward
  link (first-last-occurrence) was removed; that statement keeps its verified LeetCode 34 source.
Tracer defects found here (book-allocation, row-max-ones, lower/upper-bound) are fixed separately, with
  the others, under RCA-055. No example here uses an affected input.
Automated: StatementContractTest passes (163 statements).

## Problem statements: Graphs (48 problems, plus the num-provinces pilot)

```text
Package / status: statements rollout, topic 4 of 8 (after #165). Same method; links verified (45/45).
Coverage: 48 of 57 (num-provinces from #159 makes 49 graph statements). Skipped because no final variable
  holds the answer: bipartite-graph-dfs, cycle-directed-dfs, undirected-cycle-bfs, undirected-cycle-dfs,
  directed-cycle-dfs (no true/false result), distance-nearest-1, nearest-cell-1 (no distance grid),
  flood-fill, surrounded-regions (counts only, not the image/board). These are the ones the owner-code
  alignment of the grid problems will emit properly.
No wrong answers found; the earlier "null variables" skips were a probe artifact (delta encoding), now fixed.
Automated: StatementContractTest passes (211 statements).

## Problem statements: Dynamic Programming (55 problems)

```text
Package / status: statements rollout, topic 5 of 8 (after #166). Same method; links verified (68/68).
Coverage: 55 of 55. Each tracer was also fuzzed (~10 random inputs) against an independent reference.
Defect found (fixed separately under RCA-055): subset-sum, partition-equal, partition-min-diff and
  target-sum returned HTTP 500 on any zero-valued item. No example uses a zero.
Notes: mcm-cost-eval is titled "Mining Diamonds / MCM Cost Eval" but traces plain matrix-chain cost - its
  statement describes the tracer; the title is an open catalogue question.
Automated: StatementContractTest passes (266 statements).

## Problem statements: Arrays, Learn the Basics and Sorting (49 problems)

```text
Package / status: statements rollout, topic 6 of 8 (after #167). Same method; links verified (58/58).
Coverage: 49 of 59. Skipped because no final variable holds the answer: set-matrix-zeroes,
  rotate-matrix-90, merge-two-sorted-arrays, reverse-array-recursion (no final variables), pascals-triangle,
  print-1-to-n, print-n-to-1 (input echoed only), left-rotate-one, move-zeros-end (a helper only),
  repeating-missing-number (answer split in two).
Defects found (fixed separately under RCA-055): max-product-subarray "-0", second-largest-element's -1
  sentinel colliding with negative input, leaders-in-array using > instead of >=. These examples were
  also checked against the fixed tracers: all 315 statements pass on that branch too.
Automated: StatementContractTest passes (315 statements).

## Problem statements: Recursion & Backtracking and Strings (37 problems)

```text
Package / status: statements rollout, topic 7 of 8 (after #168). Same method; links verified (41/41).
  Full backend suite run locally before the PR (the #168 lesson: StatementContractTest alone missed
  ProblemConstraintsTest).
Coverage: 37 of 49. Skipped: sudoku-solver and eleven generators (subsets, permutations, combination sums,
  parentheses, binary strings, power set, phone letters, palindrome partitioning) - their final step
  carries only a count or a flag, not the generated list. n-queens reports a count, so it is written as
  LeetCode 52 (N-Queens II), which asks for exactly that.
Defect found (fixed separately under RCA-055): count-good-numbers swapped its exponents for odd n.
  Examples here use even n only.
Automated: full backend suite passes (352 statements).

## Problem statements: Bit Manipulation and Heaps (31 problems) - rollout complete

```text
Package / status: statements rollout, topic 8 of 8 (after #169). Same method; links verified (32/32);
  full backend suite run locally.
Coverage: 31 of 35. Skipped: single-number-3 and swap-two-numbers (answer split across two variables),
  power-set-bitwise (only a count), min-to-max-heap (only the root).
Rollout total: 383 of 431 problems have full statements (#159, #163-#170). The 48 without one are the
  tracers whose final step never states the answer; each is listed in its batch entry above, and each
  becomes writable when its tracer emits the result. Ten wrong-answer/crash defects found along the way
  are fixed under RCA-055.

## 2026-10-05 — Order 0 reconciliation and P5a switcher

**Order 0.** Started from `dad2640` (#191) and landed the doc cleanup (#193, `06e30c8`): six superseded September 23 planning files removed, the P5–P9 execution plan added. P4's ledger status was still "Review" though its gate journeys and both reviews' fixes are merged; it is now Done. Baseline on `06e30c8` + P5a: 631 frontend tests, `vite build` 347.14 kB / 110.43 kB gzip (+1.2% gzip over #189's 109.11 kB).

**P5a — the switcher.** The existing CommandPalette is the single switcher; no second modal.
- It is a modal combobox: the query keeps focus, `aria-activedescendant` names the active option, and that option is scrolled into view. Enter chooses, and Escape is handled by the dialog itself (preventDefault, so the page's shortcuts stand down).
- Results use the library's ranking and `<mark>` highlighting, bounded to 8. "View all N results in the library" carries the query to `/?q=…`.
- An empty query offers the library's recent searches (same `dsa:recentSearches` key), followed by the existing commands. Choosing a problem from a query records it as recent.
- The open problem is tagged "Current"; choosing it closes without a rerun, as before.
- Loading, catalogue failure and no-match each get their own status line instead of one "No matches".
- New `useModalDialog` gives the switcher and the shortcut list one shared modal contract:
  - initial focus, Tab containment, and `inert` on everything outside the dialog;
  - release on close and on unmount (route change, StrictMode double effects);
  - focus returned to the opener, or to a closed Menu's summary or the main region when the opener has gone.
- Help previously had no trap and no inert page. `useFocusTrap` had no other users and is deleted; its guards moved into `useModalDialog.test.jsx`.

**Red first.** The 8 new `CommandPalette.test.jsx` tests fail on `06e30c8`'s palette (no combobox, no bounding, no View all, no recents, no own Escape, no loading or no-match status). The 10 `useModalDialog` tests cover the old trap's 5 guards plus inert, unmount release, already-inert elements, closed-menu and missing-opener fallbacks, and StrictMode. There are 2 new workspace integration tests: page inert while open with focus back on the trigger, and View all → library with no execution. Existing switcher tests changed only their role queries (`textbox` → `combobox`, command `button` → `option`), because those semantics changed deliberately.

**Browser.** `evidence/p5/switcher-journey.cjs` passes 18/18 against the real backend at 1366×768 dark and 390×844 light (tracker row). A probe pitfall: Playwright's role queries still see the inert speed `<select>` options behind the dialog, so the journey scopes its locators to the dialog.

**Pending in P5.** Focus mode (P5b). One shortcut-ownership policy, and help/welcome/tour adapted to the current views (P5c). Welcome and Tour keep their own focus handling until then. A screen-reader pass of the combobox stays pending (no screen reader in this environment).

## 2026-10-05 — P5b Focus mode

- **Focus** sits in the Playground stage's header.
  - It hides the site header, the context block (title, statement, curriculum), the view rail, the input echo, the editor, history/compare and the footer.
  - They are hidden with the `hidden` attribute, so they stay mounted: an editor mid-edit keeps its local state.
  - A global `[hidden] { display: none !important }` makes that win over a card's own `display: grid`.
- What stays: the stage card, now headed with the problem title and diagram family, its notices (truncation, offline, failure, pending), the diagram and companions, the narration with its step counter, and playback.
- **Exit focus** replaces Edit input / Show code, because those would leave the view.
- **Behaviour.**
  - Entering and leaving both pause. Focus moves to Exit focus on entry and back to Focus on exit.
  - Another problem, or any view change (including Back), ends Focus.
  - Escape leaves Focus only after the switcher, the welcome, help or any other modal dialog has taken its own Escape.
  - Focus is presentation only: no URL writer, no execution, nothing persisted.
- **Room.** The stage takes what the hidden panels freed: `max(360px, 100dvh − 340px)` (phones `max(300px, 100dvh − 520px)`), so the whole card still fits the viewport at 1366×768 and 390×844.
- **Not done here.** Focus is offered from the Playground only. The Code walkthrough already gives the diagram half the screen, and its split/subview state is untouched by Focus. The help line for Esc now mentions Focus; the full help/tour update is P5c.
- **Tests.** 4 integration tests, all red on `175e3d7` (no Focus control): run, step and draft kept, with the editor hidden but mounted; pause on entry and exit; Escape priority behind help; problem change ends Focus. Full suite 635/635; build 349.42 kB / 111.03 kB gzip.
- **Browser.** `evidence/p5/focus-journey.cjs` passes 22/22 on the real backend (tracker row).

## 2026-10-05 — P5c guidance and keyboard ownership (P5 complete)

- **Keyboard ownership** was already one policy in `useKeyboardShortcuts` (#191: handled events, then an open modal, then composite widgets and the source pane, then the player). P5 adds two rules:
  - Escape reaches Focus last: any modal dialog, including the tour, takes its own Escape first.
  - The switcher handles its own Escape and marks it handled.
- The help list's Esc line now says it also leaves Focus.
- **Welcome.**
  - The copy described the retired shell: "the list on the left", `/` focusing a search box, "the panel on the right". It now names Switch problem (Ctrl/⌘ K or /), the Playground and Code walkthrough.
  - It uses `useModalDialog`: focus trap, an inert page, and focus return.
  - Dismissal storage (`dsa-ui:seenWelcome`) and replay from help are unchanged.
- **Tour.**
  - A step is kept only if its target is actually shown: not hidden, not inert, not inside a closed `<details>`, and passing `checkVisibility()` where the browser has it.
  - Each target is scrolled into view (centre; instant, or auto under reduced motion) before it is measured.
  - The page is inert while the tour is up, and focus is returned when it ends.
  - Starting the tour pauses, leaves Focus and returns to the Playground.
  - New step: Focus.
  - The phone restriction (`innerWidth > 768`) is gone: on a phone the steps whose targets are in the closed Menu are skipped, and the rest scroll into view.
- **Tests.** 7 new tests, all red on `e6faaee`'s components:
  - the tour skips hidden or closed-menu targets, scrolls each target into view, and makes the page inert then returns focus;
  - welcome copy is current and the page is inert behind it;
  - the tour started from Focus leaves Focus first;
  - the tour is offered on a phone.
  Full suite 642/642; build 350.44 kB / 111.30 kB gzip (+2.0% over the 109.11 kB recorded before P5).
- **Browser.** `evidence/p5/tour-journey.cjs` passes 40/40 (tracker row). Its first run found the phone tour spotlighting a CSS-hidden desktop link; that is fixed, and the probe now requires an exact box match.
- **P5 disposition.** Done, apart from a screen-reader pass of the combobox and tour, which stays pending because no screen reader is available here.

## 2026-10-05 — P6a textual execution history for every family

- **What it adds.** `StepHistory` (new, `workspace/`) lists the run's steps as text: the step number and the backend's own narration, 50 per page.
  - The pager has "Steps a–b of N", Previous/Next page and Jump to current step (which also moves focus to the current entry).
  - When the current step is on another page, the list says which page it is on.
  - The selected entry carries `aria-current="step"`.
- **Seeking.** Choosing an entry calls the session's one `seek` (which pauses), so the diagram, source, Analysis and URL follow. Paging only changes which entries are listed.
- **Bounded.** No diagram per entry, at most 50 buttons, and the list scrolls inside the disclosure. It renders only while "Execution history" is open: closed, it costs nothing and does not duplicate narration in the DOM.
- **Scope.**
  - "Execution history" now appears for every family with steps. Graph/Tree/DP previously had none, because the disclosure existed only for the capture strip. The capture strip keeps its exclusions and sits above the list where it applies.
  - Keyed by problem and run id, so a new run reopens on its current step's page.
  - The open/closed state survives view changes.
- **Tests.**
  - 6 `StepHistory` tests: bounded page; paging without seek; final step of a 4,999-step run reachable and seekable; current-step marking and "on page N"; Jump to current; opening on the current page; no run.
  - 1 integration test on a Graph-tagged run: the list appears without a capture strip, and seeking updates the narration with no execution. It is red on `158e55d`, where Graph runs had no history disclosure.
  - Full suite 649/649; build 352.56 kB / 112.01 kB gzip.
- **Flaky under load.** One `Lifecycle.integration` test times out at 5 s when the host is saturated. It does so on `158e55d` too, so it is load-related, not this change.
- **Browser.** `evidence/p6/history-journey.cjs` passes 22/22 (tracker row).

## 2026-10-05 — P6b comparison for every family, and truthful end-of-run actions (P6 complete)

**Comparison** (`CompareStrip`, `useComparisonTrace`):
- Offered for every problem with a declared alternate input; it used to be withheld from Graph/Tree/DP.
- Each side (Default input / Other case) is its own labelled region showing:
  - its resolved input;
  - its own Previous/slider/Next, with "Step i of N";
  - that step's narration.
- Families with a capture strip keep it on each side. The others are compared as text rather than not at all.
- A scope line states what is compared, says that step numbers are positions within each run (not matching algorithm moments), and, when the main run was submitted (`run.submittedInput`), says that run is not part of the comparison.
- The panel still fetches only when opened, and never touches the main run, its draft or its URL.
- **Stale requests.** Each load has a request id and an `AbortController`. A retry, a problem change or closing the panel aborts what was in flight, and a late answer can no longer overwrite a newer one.
  - Hook test: red on `26ed24a`, where the late answer won.
  - In the dev build, StrictMode's double mount was sending the comparison pair twice. The first pair is now aborted.

**End of the run:**
- An "End of the run" group appears below the narration in all three views and in Focus, with:
  - **Replay** (seek to step 1 and play);
  - **Run the other case** (fills the draft and runs it once);
  - **Next: ⟨title⟩**, or **Browse all algorithms** at the end of a curriculum section.
- It appears only for a genuine finish: a live, untruncated run of this problem at its last step, with nothing pending or loading. It is a row, not an overlay.
- Watched semantics are unchanged (the same `completable` rule).

**Tests:**
- 2 new component tests (labelled independent sides with a text-only graph comparison; scope line) and 1 hook test (late answer dropped).
- 4 integration tests: 3 red on `26ed24a`. The truncated-run test passes on both, and guards that no row appears.
- Existing comparison tests changed only to find the sides by heading, because the scope line now also mentions "default input".
- Full suite 657/657; build 356.46 kB / 113.15 kB gzip.

**Browser:** `evidence/p6/compare-completion-journey.cjs` passes 13/13 (tracker row).

**P6 disposition:** done.

## 2026-10-05 — Fix: the end-of-run row pushed phones sideways (regression from P6b, #199)

- **Found by** the P7 renderer sweep (`evidence/p7/renderer-sweep.cjs`). On `3373c48` at 390×844, the final step of 8 runs overflowed the page by 1–78 px: DP table, bits, window and tree problems, both inputs.
- **Cause.** Each of those final steps shows the new "End of the run" row. Its "Next: ⟨problem title⟩" button inherits `.control`'s `white-space: nowrap`, so a long title could not wrap. The P6b journey had run at desktop width only.
- **Fix.** Buttons in that row may wrap (`white-space: normal`, `max-width: 100%`).
- **Check.** `evidence/p7/completion-overflow-recheck.cjs` re-runs the 8 failing rows: all 8 at 0 px. Layout cannot be measured in jsdom, so the browser re-check is the regression proof and the full sweep is the guard.
## 2026-10-05 — P7 input contracts: no invented numbers, fields tied to their help and errors

- **Defect: invented numbers.** Three editors turned an empty or half-typed number into a value nobody entered:
  - INT: `'' → 0`;
  - array/list/tree chips: `'' → 0`;
  - graph endpoints and weights: `|| 0`, and vertex count: `|| 1`.
  So a minus sign could not be typed from an empty field, and clearing a field showed a valid-looking 0. In a real browser on `3373c48`: clear → "0", then typing `-5` → "05".
- **Fix.** All of them now keep an empty value empty. Submitted empty, the server's validator answers with a field error ("Expected a whole number.", "Position N is not a whole number.", "The vertex count must be a whole number…"), and the previous run stays on screen.
- **Fields tied to their text.** Each field's label is now its input's `<label for>` (INT/STRING). Composite editors (array, list, tree, grid, graph) are a `role="group"` labelled by the field label. Help and server error are linked with `aria-describedby`, and an erroring field gets `aria-invalid`.
- **Tests.** 4 InputPanel tests and 2 editor tests. 5 are red on `3373c48`. The negative-typing test passes on both, because jsdom cannot reproduce a browser's partial `-`; the browser probe above is that proof.
- **Browser.** `evidence/p7/inputs-journey.cjs` passes 14/14 on a phone viewport with touch emulation (tracker row). A real virtual keyboard on a device is not available here and stays pending.
- **Backend wording defect found (separate PR).** Validation messages number positions and edges from 0 ("Position 1 is not a whole number." for the second chip; "Edge 0 refers to…"), while the editors label them from 1.
## 2026-10-05 — Backend: validation messages number elements as the editors do

A backend contract defect found by the P7 input journey, fixed as its own change, as the plan requires.

- **Defect.** `InputValidator` numbered list positions, grid rows/cells and graph edges from 0, while every editor labels them from 1. Clearing the second chip produced "Position 1 is not a whole number."; a bad second edge produced "Edge 1…"; a short second grid row produced "row 1".
- **Fix.** Messages now count from 1. Grid cells read "Row R, column C" (the grid editor's own words). Vertex ids are unchanged, because they are 0-based ids the reader types, not positions.
- **Tests.** The `InputValidatorTest.Numbering` tests (array, grid cell, edge) and the updated ragged-grid assertion ("row 2" for the second row) are 4 failures on `3373c48`. Full backend suite green.

## 2026-10-05 — P7 renderer coverage sweep (all 17 registry keys)

- **Method.** `evidence/p7/renderer-sweep.cjs` reads the live catalogue (no document counts). For each of the 17 `dsType`s it takes the first, middle and last catalogued problem: 50 problems, structurally different within each family. Each problem runs with its default input and its declared alternate input (as a shared `?input=` link), at 1366×768 dark and 390×844 light. Every run is inspected at the first, middle and final step for:
  - a drawn stage (the canvas frame holds a drawing or text, so an honestly empty structure counts; "No visualization" / "Nothing to draw" fails);
  - no page-wide horizontal overflow;
  - no page errors.
  The full result is in `renderer-manifest.json`.
- **Result on `3373c48`: 192/200.**
  - All 17 families draw at every inspected step with 0 page errors.
  - 7 rows overflowed at the phone's final step. That was the end-of-run row added in P6b, fixed in #200 and re-checked at 0 px.
  - 1 row was a page-load timeout while the host ran unrelated builds (load average ~30).
- **Probe correction.** The first run flagged Matrix and Queue frames as "nothing drawn". Their screenshots showed a correct grid and a correctly empty queue: the check guessed CSS class names. It was replaced before the recorded run.
- **Still open for P7.**
  - A full re-sweep on merged main.
  - A second browser engine and real devices (virtual keyboard), which are not available here.
  - Unknown-`dsType` and unsupported states are covered by the existing registry tests, not by this sweep.

## 2026-10-05 — DP verification, fix 1: inputs outside the problem are refused

**How the problems were checked.** All 56 DP problems were verified independently, in four groups. For each problem a reviewer:
- wrote a brute-force Python reference from the problem definition (not from the tracer);
- checked all 114 statement examples and their explanations;
- compared the tracer with the reference on 60–80 random and edge-case valid inputs, about 5,000 runs in total.

**What was found.**
- 0 wrong answers for inputs the statements allow, 0 wrong example outputs, 0 false explanation claims.
- 13 problems where valid inputs exceed the 2 MB trace budget, so the run stops before the answer (fixed separately).
- Server errors and unchecked inputs, fixed here:
  - **ninjas-training** declared `minCols(3)`, but `InputValidator` never implemented `minCols`. A grid narrower than 3 reached `points[day][2]` and returned a 500. `minCols` is now enforced, which also activates celebrity-problem's declared minimum.
  - **knapsack-01, unbounded-knapsack:** unequal weight and value lists returned a 500 (more weights) or silently dropped values (more values). They are now refused with "Needs exactly one value per weight".
  - **coin-change-2** accepted repeated coins and counted each copy as a different coin: `[2,2]`, amount 4 gave 3, but the right answer is 1. Coins must now be distinct.
  - **largest-divisible-subset** accepted repeats, so `[2,2,4]` came back as a "subset". Values must now be distinct.
- Tests: `TracerAnswerRegressionTest.dpInputsOutsideTheProblemAreRefused` and `InputValidatorTest.rejectsTooNarrow` are both red on `5c4e027`. Full backend suite green.

## 2026-10-05 — DP verification, fix 2: every allowed DP input reaches its answer

- **Problem.** 13 DP problems accepted inputs whose trace exceeded the 2 MB response budget (`InputSpec.DEFAULT_MAX_BYTES`). The run stopped, honestly marked "cut short", but before the answer step, so a learner with a legal input never saw the result.
  - Table problems: count-subsets-with-sum-k, subset-sum-equal-target, count-partitions-given-diff, partition-equal-subset-sum, partition-set-min-abs-diff, target-sum-dp, knapsack-01, unbounded-knapsack, coin-change-2, minimum-coins-dp.
  - Length problems: edit-distance, wildcard-matching, print-lis.
- **Cause.** Every step carries the whole DP table, and the number of steps grows with the number of cells, so the payload grows with cells². 98% of a failing knapsack trace was `dpTable`. The budget is deliberate (it protects the browser), so it stays.
- **Fix.**
  - `DpTraceSupport.requireTableFits` refuses, as a field error, any input whose table would exceed 225 cells (15 × 15, measured as the largest that finishes). The message names the table and what to shrink.
  - The limit is on the table, not on each field, so a few large values and many small ones both remain usable.
  - edit-distance and wildcard-matching max length 15 → 14; print-lis 30 → 22.
- **Tests.** `DpTableBudgetTest` runs each problem's largest allowed input; it must finish with the brute-force answer. One step over the limit must be refused: those 13 rows are red on `c5d39f2`.
- **Test change.** `TracerContractTest.stepCountGrowsWithInput` pads with the smallest allowed value when max-value padding is refused, since a sum-bounded table is a legitimate limit. Full backend suite green.

## 2026-10-05 — DP verification, fix 3: stronger examples

- **Why.** The verification found every DP example correct, but some sets missed the case that defines the problem.
- **Added examples (18).** Each uses our own inputs (not LeetCode's or GfG's), and every output was checked both against the tracer (`StatementContractTest`) and by hand or brute force.
  - zero answers: LCS, longest common substring, and min insertions/deletions with nothing in common; a palindrome needing no insertion or cut;
  - strictness: `[7,7,7,7]` → 1, and equal values in the tails list;
  - ties: two LCSs of length 2;
  - negatives: falling path −3 then −6 = −9;
  - an even total with no equal split: `[2,3,7]`;
  - order mattering: cookies `[2,1]` against greed `[1,2]`; a matrix chain costing 396 left to right but 180 at best;
  - an unsorted divisible subset;
  - words one letter apart that still don't chain;
  - two non-adjacent 9s;
  - two maximal 2×2 rectangles;
  - three single-letter palindromes;
  - a 0 that doubles the sign count.
- **Text fixes.** shortest-common-supersequence now notes that "geeke" is equally correct. mcm-cost-eval drops an internal-commentary sentence; its title stays an open owner decision.

## 2026-10-07 — P8 bounded accessibility and chrome iteration (phase remains open)

Branch: `feat/p8-workspace-polish`, based on merged main `46d98fb` (#207).
The original checkout's untracked handoff documents were left untouched; work is in a
separate worktree. The owner authorized publishing this bounded package through a PR;
the remaining P8 phase gates below are not waived by publishing it.

### Reproduced friction and implemented fixes

- At 320px, keyboard focus could reach an Analysis tab whose right edge was outside
  the rail (370px versus 304px). Focused tabs now reveal locally, and a linked/selected
  tab is revealed on entry and resize without changing selection or moving focus.
  The focus ring is inset so the scrolling rail does not clip it.
- Narration remained a live region on every autoplay tick. It now stays visually
  current but uses `aria-busy` during playback, releasing the atomic current narration
  on pause/end. History announces deliberate page-range changes, not an off-page
  current-step hint on every tick. Actual speech behavior still needs a screen reader.
- An explicitly labelled stress fixture (112-character variable name, 175-character
  value) produced 609px of page overflow at 320px and a zero-width value disclosure.
  The variable-name column is bounded, names and values wrap, disclosures remain
  reachable, and their summary targets are at least 44px. This is layout evidence,
  not a claim about an algorithm's output.
- Space on a native phone Menu summary also reached the player. End on a focused
  link inside the open Menu sought to the final trace step. Native summaries now own
  Space; open disclosures own navigation keys without cancelling the browser's
  action. Escape closes the disclosure and returns focus; blur/outside pointer close
  it without trapping Tab. Both menus clean up their document listeners.
- The problem context and canvas chrome no longer appear as additional page banners.
- Array values above bars used the bar-border color: default values were nearly
  invisible on the light canvas. Values now use `--text-primary`; role colors still
  identify bars. A regression test fails on the old border-ink implementation.

### Compact header and measurements

The five native, same-tab learning-network links are preserved in a labelled
disclosure inside the shared header instead of a separate strip. The phone theme
and network controls are labelled 44px icons. Context/card spacing is reduced; an
empty canvas chrome row is removed, while the DP legend remains visible.

At 1366×768, Two Sum/BFS's drawing frame starts at **y358.39**, versus **y464.39** on
the fresh main baseline: 106px earlier. The stage/card starts at **y357.39/y288.39**.
The DP drawing frame starts at **y391.39**, because its legend is retained. Desktop
minimum drawing heights remain 360px (Array) / 440px (spatial families), and phone
minimum heights are unchanged. **The ~y280 drawing-frame gate is still not met**;
this is progress, not a waiver or a redefinition of the frame as the outer card.
The historical P3 y389 measurement predates the later learning-network strip.

`evidence/p8/chrome-journey.cjs` captures three default runs from the real backend,
then replays those responses for repeatable native-menu/geometry checks. Its final
`chrome-results.json` has **50/50 passing rows**: three problem routes plus library
and not-found, five viewport widths (320/390/768/1366/1440), both themes. All 50 rows
check page overflow. The 30 problem rows additionally check one banner,
closed network navigation absent from the
browser accessibility tree, five network links, Escape/focus return, native phone
Menu Space, 44px theme/network controls, and no executions caused by disclosures.
Representative before/after and stress screenshots accompany the manifest.

### Verification and remaining gates

Fresh main baseline: 75 frontend files / 663 tests pass; production JS 356.99kB
(gzip 113.33), CSS 102.73kB (gzip 18.46). The unchanged backend suite passes:
7,910 tests, zero failures/errors, 500 skipped. API stats remain 431 traced/catalogued,
zero untraced, no duplicate/orphan ids. Startup smoke passes. No dependencies added.

Current production build passes: JS 359.48kB (gzip 114.08), CSS 103.99kB (gzip 18.64).
Compared with the refreshed main baseline, that is +2.49kB JS / +0.75kB compressed
and +1.26kB CSS / +0.18kB compressed; it is a bundle measurement, not a latency claim.

Final candidate frontend suite: **75 files / 675 tests passed** (663 on main).
The added guards cover autoplay narration (all three views and final release),
focused/linked tab reveal, quiet history pagination, native Menu Space/navigation-key
ownership, a single page banner, network disclosure dismissal, and array value ink.
The narration/tab/history/Menu/array-ink regressions were checked red before their
fixes; the exact-link test retains its original five href and same-tab assertions.

The real-backend renderer re-sweep completes **200/200 passing rows**, all 17 dsTypes,
50 problems, default+alternate inputs, desktop dark/phone light, first/middle/final
steps. `evidence/p8/renderer-manifest.json` records this run without replacing the
historical P7 manifest. The reusable probe now checkpoints incomplete runs, closes
the browser in `finally`, and paces executions under the server's normal 60/minute
limit. Aborted requests count toward pacing too. This certifies drawing/overflow/
page errors for those rows, not independent answer correctness or all legal inputs.

The final `accessibility-journey.cjs` run passes **40/40 rows**, across the same five
widths and both themes: three real-backend problems plus the explicitly labelled
long-variable/container fixture. It checks manual tab activation and focus/selected
tab visibility, narration busy/atomic state in all three views, reachable 44px value
disclosures, and no page overflow before/after expansion. The retained baseline
subset (`accessibility-before.json`, 320/1366, both themes) fails 16/16 rows on main;
it is not represented as a full five-width baseline matrix. Final results/screenshots
are in `evidence/p8/accessibility-results.json` and the accompanying PNGs.

Still open: an actual screen-reader speech pass; second-engine and real-device/virtual
keyboard coverage; 200% zoom and the broader browser/state matrix; measured long-trace
playback/seek/history performance; independent learner/owner feedback and the ~y280
frame target. These automated expert checks are not user research. P8 remains in
progress; P9 cleanup/preservation-ledger reconciliation/release work has not begun.

## Complete DP solution source repair — 2026-10-07

Owner-reported placeholder Java was traced to `RemainingDpTracer` and its two shared
sketch constants. All 27 affected IDs now own complete Java 17 source resources,
including real transitions, helpers and reconstruction. Missing resources fail clearly;
no sketch fallback remains. Stock buy/sell, bitonic forward/reverse and palindrome/cuts
events highlight distinct statements. No frontend production changes or dependencies.

Review gates:

1. **Backend:** full suite passes, 7,967 tests / zero failures or errors / 500 skipped.
   All 27 resources compile without app classes and match default/alternate traces and
   66 published examples. HTTP detail and both trace encodings serve matching source.
   Existing validators, budgets and data contracts are unchanged.
2. **Frontend:** 75 files / 675 tests pass; production build passes (JS 359.47kB, gzip
   114.08; CSS 103.99kB, gzip 18.63). An earlier concurrent run hit one existing
   lifecycle timeout; isolated 8/8 and full 675/675 reruns passed without weakening tests.
3. **UI/UX:** real-backend Chromium journeys pass 24/24 across six representative
   problems, phone/desktop and both themes. First/middle/final source highlighting,
   keyboard ownership, view continuity and page overflow are checked; representative
   source screenshots were inspected. Custom LCS and rejected rerun preserve source,
   step and shared input, with HTTP 400 focus on the error summary.
4. **Product:** source now teaches the algorithm that runs. API stats remain 431/431,
   zero untraced/duplicates/orphans. Three-approach DP and P8/P9 outstanding gates are
   not claimed complete; this repair is separate.
5. **Architecture:** strict per-ID immutable source resources; ownership derived from
   the tracer registry in tests. Both shared placeholders and all overrides removed.
6. **QA:** initial 28 source tests failed before the fix; final source suite has 57
   cases. Only 27 goldens regenerated. Parsed old/new comparison proves only source,
   anchors and highlighted lines changed, not any narration, result or snapshot data.

Plan/results: `COMPLETE_SOLUTION_CODE_REPAIR_PLAN.md`; root cause: RCA-057;
reproducible browser and snapshot probes/results: `evidence/source-code/`.
This certifies these 27 displayed sources, not every source in the catalogue. The
owner authorized commit, push and merge; the linked PR records publication and CI.

## DP solution approaches D0/D1 — 2026-10-07

Following PR #210 (Speed Insights, merged with production URL redaction), refreshed the
DP feature baseline to `10ed4fc`. Classified all 56 candidates in the generated coverage
ledger with actual state/recurrence, reconstruction, canonical bounds/complexity/source
digests and applicability. The ledger distinguishes histogram stacks, binary-search
tails, rolling DP and in-place Matrix DP. Print LIS's omitted display helpers are recorded
as a later source-repair prerequisite. Alternatives' safety caps remain unmeasured.

D1 introduces an immutable provider registry separate from the canonical tracer registry.
The pilot's existing Climbing Stairs executable is identified as `tabulation`; other
canonical algorithms retain an honest “Current solution” identity. No new alternatives
or selector are advertised in this package. Optional `approach` is served by detail,
input-spec and GET/POST execute. Responses carry source, approach ID/label, type and
complexity from the selected executable. Unknown approaches are refused, not substituted.
Default trace cache keys now include approach and normalized supported encoding. Custom
input is uncached; unknown encoding is a documented 400. RCA-058 records the previous
unbounded raw-encoding key defect.

Six review gates: HTTP metadata/dispatch/input validation checked; frontend full suite
and real workspace checked; canonical UX unchanged and both themes inspected; inventory
stats remain 431/431 with no false coverage claim; alternatives do not become canonical
Spring tracer beans and source ownership is explicit; seven API regressions and the typed
array-ceiling guard were proved red, all goldens stay unchanged. Final verification:
7,999 backend tests / zero failures or errors / 500 skipped; 76 frontend files / 678 tests;
production build and startup process cleanup passed. Live probe inspected 431 details,
full/delta/custom/refusal API responses and four phone/desktop × light/dark browser rows.
No new dependency. The frontend approach session/selector and genuine pilot forms remain
D2/D3 work. See `DP_SOLUTION_APPROACHES_PLAN.md` and `dp-approaches/d1-results.json`.

## DP approaches D2 — real Climbing Stairs backend pilot — 2026-10-07

Added actual recursive and memoized executables alongside unchanged canonical tabulation.
All use the same base cases/recurrence, with per-request state, genuine cache hits/stores,
distinct invocation IDs, and empty-stack returns. The listings are standalone Java 17
classes, not sketches. Recursion caps n at 10; memoization at 30; canonical n<=30 remains.

Six review gates:

1. Backend: all advertised n agree with an independent combinatorial oracle; isolated
   compiled displayed classes agree. API serves matching selected source/type/complexity.
   Max+1 rejects, and a 3-step cap halts expansion without an answer. Full suite:
   8,007 tests, zero failures/errors, unchanged 500 skips.
2. Frontend: production JS decoder round-trips nine live full/delta pairs, including
   clearing call stacks and immutable memo snapshots. Existing 76 files/678 tests pass;
   build unchanged (JS364.60kB/gzip116.09, CSS103.99kB/gzip18.63).
3. UI/UX: canonical UI preserves tabulation; four real Chromium phone/desktop × both
   themes rows check complete source, Code/Analysis step continuity and no overflow/errors.
   Selector and recursive/cache UI are D3, not certified here.
4. Product: 431/431 canonical coverage remains; only one of 56 candidates gains alternatives.
   Measured largest full payloads: recursion358,446 bytes; memoization435,653 bytes;
   tabulation57,391 bytes. All finish without truncation at advertised maxima.
5. Architecture: alternatives are non-bean tracers owned by the approach provider; no
   duplicate canonical registration, endpoint or dependency. Source resources fail strictly.
6. QA: corrected initial pilot tests failed 6/7 against D1; eight final pilot tests pass.
   Only one canonical golden changes; parsed comparison proves source/highlights only.
   Existing API assertions retain their behavioral checks while recognizing all three IDs.

Reproducible evidence: `dp-approaches/d2-journey.cjs`, `d2-results.json`,
`d2-golden-check.cjs`, `d2-golden-results.json`. D1's recorded one-option probe is historical.
D3–D7 and the existing P8/P9 human/performance/release gates remain open.

## DP approaches D3 — committed approach session and pilot UX — 2026-10-07

Climbing Stairs now has a labelled Recursion / Memoization / Tabulation selector. Selection
prepares and pauses; only a successful explicit run commits the approach together with
input, source, anchors, renderer and complexity. Preparation and rejected runs retain the
prior displayed result/link. Shared links restore approach/input before step; old links
remain canonical. Retry owns its original approach and input. Code/Analysis, source
follow/scroll, presets and the existing input comparison respect the committed/candidate
boundary. This is not D4's two-approach comparison.

Memo cache cells and recursive text alternatives come from actual step snapshots. Empty
call stacks finish all tree frames; unrelated canonical demo arrays/trees are not inherited.
Screenshot inspection caught a wide SVG squeezed into the workspace's generic max-width
rule despite passing page-overflow checks. Native tree widths are now retained inside a
keyboard-accessible scrolling region capped at 480px high, with both ends reachable.

Six review gates:

1. Backend: selected summaries now publish their own alternate input. The strengthened
   API case failed before this seam. Isolated full suite: 8,007 tests, zero failures/errors,
   unchanged 500 skips. No algorithm, source listing or golden changes in D3.
2. Frontend: 78 files / 706 tests pass. Production build JS371.55kB/gzip118.32,
   CSS104.90kB/gzip18.77. Session tests cover StrictMode, decode races, late detail,
   navigation, refused/partial/wrong-identity responses and restoration after default failure.
3. UI/UX: ten real-backend Chromium rows (320/390/768/1366/1440px, both themes) pass;
   all three answers, source/complexity, 400 focus/retained draft/link, shared restoration,
   presentation-only changes, 44px selector and no page overflow/errors checked. Four
   max-tree checks verify native SVG width and local scrolling. Screenshots inspected.
4. Product: one of 56 candidates has a verified three-form user journey; default tabulation
   and canonical coverage remain unchanged. Missing complexity stays unavailable; absent
   cache values are not zero. D4–D7 and original P8/P9 gaps remain explicitly open.
5. Architecture: one shared URL writer, atomic run metadata and existing stage pipeline;
   one primary canvas at a time. No dependency added. Inventory refresh preserves prior
   JSON key order without preserving stale values; only date and pilot status changed.
6. QA: 14/15 original session cases and all six original UI cases failed against D2;
   summary, empty-stack, demo-inheritance, native-key ownership and missing-metadata
   regressions also failed before fixes. The geometry probe failed before the SVG fix.
   Existing tests were not weakened; two URL callback expectations add approachId:null.

Verification incident: the first backend attempt overlapped our dev-server Maven compile,
which removed DsaApplication while a Spring test scanned classes (one setup error). Stopped
the dev compile and reran the full suite isolated: all 8,007 pass. No tests were changed to
hide that failure. Backend was started only after verification for the final live matrix.

Evidence: `dp-approaches/d3-journey.cjs`, `d3-results.json`, `d3-390-dark.png`,
`d3-1366-light.png`; plan: `DP_SOLUTION_APPROACHES_PLAN.md`; RCA-054 follow-up.
The matrix's viewport height is 900px, not proof of the original 768px stage-position gate.
Cross-engine, real-device, human learning and broader-family certification remain pending.

## DP approaches D4 — teaching and isolated comparison — 2026-10-08

Implemented on `feat/dp-approach-teaching` from merged `fb96744`; publication awaits owner
approval. Audited optional teaching metadata belongs to each executable definition and is
served by detail/approach summaries. Current-step explanations bind to actual event values
or source anchors. No recurrence is invented for canonical adapters. The disclosure follows
the main transport/inspector, not the top of the canvas; no blank row on unrelated problems.

Compare approaches is a separate, explicitly requested same-input comparison. It snapshots
the displayed resolved input, not draft/candidate state; each side has independent controls,
returned metadata, counters and outcomes. Refused/truncated/malformed/substituted results
cannot become completed answers. A successful side remains visible beside a refused one.
View changes retain the comparison; pair changes clear old results without fetching; a new
main run retires its comparison. No main-session/progress/URL writes or additional canvas.

Six review gates:

1. Backend: API teaching case proves metadata is served, event names actually emit, anchors
   exist, and canonical adapters stay honest. Full 8,008 tests, zero failures/errors,
   unchanged 500 skips. Algorithms, sources and goldens unchanged.
2. Frontend: final 80 files / 753 tests, no skips. Release build JS381.28kB/gzip121.58,
   CSS106.81kB/gzip19.07. Versus D3: JS+9.73kB/gzip+3.26, CSS+1.91kB/gzip+0.30.
   Explicit requests, immutable input snapshots, identity/truncation checks, partial failure,
   decode races, clear/unmount/problem retirement and UTF-8 byte measurement are tested.
3. UI/UX: ten real-backend Chromium rows at five widths in both themes; independent
   positions, modifier/local keyboard ownership, safe limits/refusal, same-input/draft/link
   boundaries, 44px targets, stacked phone summaries and no overflow/errors. Screenshots
   inspected. Three production-preview rows add short landscape, reduced motion, real
   cache-hit notes, visible keyboard focus and denied local/session storage.
   The ten-row pilot journey also passes in Firefox155 (before the final early-open guard),
   with separate results/screenshots. Four final-build input-comparison rows verify real
   canonical/memoized identity, held-pending initial-load safety, isolated keyboard/steps
   and explicit warnings/errors for injected truncated/empty responses.
   Four additional final-build Firefox rows at 320/1366px in both themes pass; the earlier
   ten-row result is retained separately. Evidence: `d4-firefox-release-results.json`.
4. Product: answer21 at n7 for both forms, recursion41 calls vs memo13 calls/5 hits/8
   distinct computed states. These are recorded counters, not latency/runtime guesses.
   Tabulation's uninstrumented counters are unavailable. Only one of 56 candidates ships
   three forms; D5–D7 and original P8/P9 accessibility/human/performance gaps remain open.
5. Architecture: optional immutable registry metadata; existing validated execute API;
   isolated comparison generation/abort ownership; one URL writer and primary canvas.
   No dependency. The inventory refresh changes date/pilot status only and its guard passes.
6. QA: four UI cases and one API case failed against D3. Two decode-race cases fail with
   the generation guard removed; blank-row and Ctrl+K regressions proved red. No existing
   assertion weakened. A missing aria-controls target regression proved red; the closed
   target now remains present without mounting comparison requests. Query-safe browser
   pacing has a meaningful Node RED/green guard. The ten-row sweep also runs on production.

Continuation hardening: Compare inputs now commits one problem/approach-owned pair, checks
encoding, numbered/nonblank events, truncation and submitted input echoes, and labels partial
traces as incomplete. Playback keys stay local; modifier switcher shortcuts stay global.
Input comparisons cannot start without a committed, non-offline executable. Closing or
replacing a main run aborts pending approach comparisons after decoding, without reopening
old results. Initial nine integrity cases, cut-short UI and main-key ownership cases proved
RED; four stricter comparison-envelope cases also failed before the fix. Removing the
main-run comparison key makes the pending-body/new-run integration fail. The early-open
guard also proved RED. Fixtures were brought to the real API contract, including explicit
canonical query handling and waits for live commit; no behavioural assertion was dropped.

Verification notes: an early browser sweep was interrupted by the local backend process's
SIGTERM exit and is not counted as passing. The restarted ten-row matrix passes. The first
production preview used unapproved origin port5181 and correctly got HTTP403; reran on the
configured port5180 without relaxing CORS. An early frontend sweep overlapped unfinished
feature edits; the stable final suite, recorded in the plan, is the release result. Native-focus checks
use keyboard traversal rather than expecting a focus-visible ring after a pointer click.
The continuation's early-open guard exposed incomplete comparison-test fixtures and a
missing canonical query matcher; intermediate 751/753 runs are not passing release runs.
Local backend processes also exited143 during browser retries; cause unverified. Those
interrupted/early-start retries are excluded. Concurrent Maven startup/test compilation
caused an intermediate testCompile failure; subsequent serial inventory guard passed.
Startup process-tree smoke and the Node pacing guard pass. No dependency/golden change.

Evidence: `dp-approaches/d4-*`; plan: `DP_SOLUTION_APPROACHES_PLAN.md`; RCA-054 follow-up.
Viewport height900 is not certification of the original 768px stage-position target.
Real-device, broader-family cross-engine, screen-reader, real-browser-zoom and human-learning gates remain.

## DP approaches D4 continuation — 2026-10-09

Still unpublished on `feat/dp-approach-teaching` from D3 `fb96744`; owner publishing
approval and CI remain required. No D5 implementation is stacked on this package.
The October8 results above are historical; the following are the current release checks.

Six review gates:

1. Backend: immutable teaching definitions reject blank required copy/notes; registry
   startup rejects notes naming nonexistent source anchors. API tests verify served
   teaching and real emitted events. Full suite: **8,014 tests, 0 failures/errors,
   unchanged 500 skips**. Displayed algorithm listings, tracer computations and goldens
   are unchanged. The live inventory refresh changes only date and Climbing Stairs status;
   canonical source/spec digests and the other55 candidate statuses stay unchanged.
2. Frontend: **81 files / 788 tests**, no skips; production build JS382.11kB/gzip121.84,
   CSS106.95kB/gzip19.10. Comparison callbacks and alternate-input snapshots adopt only
   committed identities, with synchronous layout cleanup and post-decode retirement.
   Both comparison paths reject malformed debug maps; invalid optional teaching cannot
   crash a valid trace or leave an empty disclosure row. Own-property event notes and
   unambiguous source-line notes only. CSS tokens resolve in both themes.
3. UI/UX: the final production bundle has **99 completed real-browser rows** across twelve
   result files: Chromium10, Firefox10, native Chromium200%8, keyboard4+4, reduced-motion/
   storage3, existing input comparison4, boundary12+12+12, and exact handoff sizes10+10.
   The latter cover 320×568, 390×844, 768×1024, 1366×768 and 1440×900 in both themes,
   separately in Chromium and Firefox; the common-height900 sweeps remain separate.
   These are repeated pilot scenario rows, not99 unique problems. All twelve record the same
   actually served assets (`index-D9b0gbod.js`, `index-DkVb_nYT.css`). Keyboard checks use
   native Enter/Tab/Shift+Tab/arrows, including upward focus under the sticky rail and
   switcher focus return. Initial focus setup is programmatic; these are not a manual
   screen-reader or completely keyboard-only task certification. The 200% sweep uses real
   tab zoom, disposable profiles and DPR/reflow checks, not CSS or scale emulation.
4. Product: independent answer references cover n1/n10/n30. Real zero cache hits remain
   visible; recursive refusal is not a shortened input. Actual n31 HTTP400 leaves the
   memoized n30 run, main step/link and rejected draft/error intact even after comparison.
   Step ordinals, JSON bytes and counters are not latency measurements. Only Climbing
   Stairs (1/56 candidates) has three forms; D5–D7 and original P8/P9 remain open.
5. Architecture: no runtime dependency, endpoint, URL owner or primary canvas added.
   Teaching remains definition-owned; on-demand comparison requests are isolated from
   the main session. Busy comparison submission preserves focus via aria-disabled/busy
   and an explicit duplicate-request guard. Shared step validation keeps the two existing
   comparison paths consistent. Temporary source-map analysis does not modify the served
   release. No publishing or modification of the owner's original Windows worktree.
6. QA: six retained-callback retirement cases, three suspended-transition cases, eight
   malformed-debug cases, three invalid-teaching-definition cases, the missing-anchor
   case, inherited/aliased-note regressions and malformed optional-metadata cases proved
   RED before their fixes. Native-browser checks reproduced busy-button focus loss and
   backward-tab rail occlusion before the fixes. Defensive-copy/valid-anchor positives
   are not claimed as RED regressions. Existing behavioral assertions are preserved;
   changed fixtures now carry actual executable identity/encoding/truncation/input echoes.
   Launcher smoke and three Node evidence-tool checks pass.

Bundle growth review (§verification): JS gzip+12.4% / CSS gzip+18.1% versus P0 triggers
review; this D4 package adds JS+3.0% / CSS+1.8% versus D3. A separate temporary source-map
build confirms the existing vendor set, with no new library. Four new feature/validation
modules contain12,031 unminified source bytes, which are not emitted/gzip attribution.
Teaching and explicit comparison account for intentional feature growth; optional-panel
splitting is a future profiling candidate with focus/lifecycle constraints. No long-trace
timing improvement or P8 performance certification is claimed. npm audit reports nine
inherited advisories (4 moderate/3 high/2 critical); manifests/lockfile match D3. No forced
major dependency upgrade is included in this UI package.

Evidence limitations and exclusions: `noOverflowOrErrors` measures page-wide overflow and
uncaught Playwright page errors, not every console/network message. Injected truncated/
empty input-comparison responses are fault checks, not actual budget-exhaustion claims.
An ordinary full-page capture clipped the native-zoom surface; the corrected capture
verifies actual pixel dimensions and unchanged viewport metrics, and was inspected.
One closed-browser retry, a worker-module failure during dependency reinstall, and a
partial exact-size sweep interrupted by local backend exit143 are excluded. Server exit
cause is unverified; completed earlier sweeps remain separate from interrupted retries.
The restarted exact-size sweep subsequently passes20/20 on the unchanged final bundle,
with inspected phone/dark and desktop/light screenshots.
Original stage-position, real-device, screen-reader, broad-family, human-learning and
production performance/release gates remain open. Pilot native zoom is now checked.

Evidence: `dp-approaches/d4-*`; reproducible commands and next-package preflight live in
`DP_SOLUTION_APPROACHES_PLAN.md`. The tracker distinguishes merged D0–D3, local D4 and
pending D5–D7, and does not mark P8/P9 complete.

Publication update — 2026-10-09: the owner explicitly approved commit, push and merge
after the local checks above. Package commits are grouped by backend metadata, frontend
teaching/comparison safety, and reproducible evidence/docs. Publication goes through a
PR and passing CI, not a direct main commit. D5 remains a separate next package.

Publication result — 2026-10-09: D4 merged as #215 / `2709d8b` after passing PR checks.
Post-merge CI also passed. The previous local/unpublished descriptions above are historical.

## DP approaches D5 first candidate — Frog Jump, 2026-10-09

Local only on `feat/dp-frog-jump`, cut from the D4 merge `2709d8b`. No commit, push or PR
is made by this continuation. Frog Jump alone gains genuine recursion and memoization;
the existing canonical tabulation and its public `canonical` ID are preserved. Its label
is Tabulation, so existing explicit canonical links remain supported without an alias.

Six review gates (not all complete):

1. Backend: full Maven suite **8,023 tests, 0 failures/errors, unchanged 500 skips**.
   Nine new tests cover independent forward-path oracles for360 small inputs per form,
   named cases, every accepted length, strict input refusals, cached zero, actual call IDs,
   budget retirement, complete displayed Java17 compilation, API wiring and source anchors.
   All nine failed/errored before alternatives existed (one failure/eight errors).
   Canonical source/tracer/spec/goldens are unchanged; no fixture regeneration is needed.
2. Frontend: clean npm install, **83 files / 795 tests**, production build JS382.31kB /
   gzip121.90; CSS106.95kB / gzip19.10. Seven added frontend tests cover array bounds
   (including zero), legacy links, an eleven-element rejected draft, shared memoized input
   before step restoration, full source and isolated comparison. Real complete API
   responses generate the fixtures; custom successful traces are not invented by mocks.
   Two bounds tests proved RED before the presentation fix; the absent-bounds positive
   and integration seam checks are not claimed as independently proven RED regressions.
3. UI/UX: comparison copy now shows declared array length and element-value bounds rather
   than hiding them behind a generic constraints message. The existing one-canvas shell
   and approach-session controls are reused. **Frog Jump real-browser desktop/phone,
   both-theme, keyboard, native-zoom and screenshot gates remain pending.** Previous
   Climbing Stairs browser evidence does not certify this new input/trace combination.
4. Product:19 complete real-API measurement rows /38 paced execution requests prove
   full/delta agreement through the real frontend decoder, independent answers, recorded
   calls/events/depth and derived tree completeness. Recursion length10 has143 calls /
   573 events /313,271 full JSON bytes; memoization length20 has38 calls /154 events /
   217,782 bytes, both within unchanged budgets. Length11 recursion would exceed the
   renderer's220 nodes (232 calls), so its separate maximum is10. Equal-height length7
   memoization records answer0 with5 cache hits; null remains unknown. These are measured
   shapes and counters, not latency or exhaustive worst-byte/performance certification.
5. Architecture: no new dependency, endpoint, session owner, primary canvas or catalogue
   problem. The immutable provider owns the alternative source/spec/teaching; default
   execution still uses the unchanged registered tracer. Live inventory stays431/431,
   no duplicates/orphans. All56 canonical digests/specs/complexities remain unchanged;
   only Frog Jump's alternative status moves to backend-pilot/UI-pending. Generic safety
   wording no longer cites Climbing Stairs measurements as if they certify every problem.
6. QA: full suites/build pass; API fixtures are regenerated reproducibly, not hand-authored
   answer snapshots. An initial equivalence probe normalized explicit nulls on only one
   side and failed; the corrected probe normalizes absent/null empty fields on both sides
   while retaining all actual state values/frames. Its19 completed rows are the evidence.
   One frontend assertion incorrectly expected no POST for an explicit canonical link;
   the session intentionally restores that executable with `{}`. The corrected test
   asserts the exact canonical request and no custom shared input; production restoration
   logic was not changed. No existing behavioral assertion was weakened.

Evidence: `dp-approaches/frog-jump-measurements.cjs` / `.json`; detailed bounds and
remaining pre-publication gates: `DP_SOLUTION_APPROACHES_PLAN.md` D5 first candidate.
Both full suites ran after stopping our backend server to avoid Maven target contention.
The final build remains `index-QrVyrIy2.js` / `index-DkVb_nYT.css`. npm's nine inherited
advisories remain outside this package; dependency manifests/lockfile are unchanged.
One merged UI-verified three-form pilot and one local candidate are not a family rollout.
Frog Jump UI verification/publication,54 other candidates, D6/D7 and original P8/P9 remain
open. Do not start the next candidate until this package's gates pass and it merges.

## DP D5 continuation — docs, viewport and browser gates, 2026-10-09

The preceding D5 entry is an initial snapshot, not the final verification result.
Current package: `feat/dp-frog-jump`, based on `2709d8b`. The owner requested a stop
after the current changes, then explicitly requested merging the required pending PRs.
No PR was open; publication of this existing package is authorized, gated by PR CI.
No next candidate, cleanup package or other development is started.

Documentation and the feature ledger now describe the live workspace, renderer registry
and evidence. Historical measurements remain dated; P7 acceptance, P8/P9, human/device
checks and the known Print LIS source-helper gap are not falsely marked complete.

Browser inspection found a derived recursion tree whose root moved outside the visible
stage as the tree widened. Four viewport tests proved RED before the fix. Stage-owned
anchoring compensates layout movement while preserving manual pan, vertical scroll and
focus; new runs center their own root. Resize listeners/observers clean up. Tracers with
their own tree keep their existing layout; no extra canvas or session owner was added.

Final frontend: **83 files / 799 passing tests**. Production build: JS383.15kB/gzip122.13,
CSS106.95kB/gzip19.10; served assets `index-B8pL5v5E.js` / `index-DkVb_nYT.css`.
Backend code is unchanged since this package's passing full run: **8,023 tests,
0 failures/errors, unchanged 500 skips**. It was not rerun for the frontend-only follow-up.

Final real-backend production-browser matrices contain **28 passing scenario rows**:
Chromium10, Firefox10 and native Chromium200%8, both themes, matching final assets.
They cover actual maximum trees/root visibility, local scrolling, comparison isolation
and keyboard/focus, rejected drafts/old links, and input-before-step restoration.
Phone/dark and desktop/light screenshots were inspected. Sizes, scenarios, commands
and limitations are recorded in `DP_SOLUTION_APPROACHES_PLAN.md`.

Three Node evidence-tool tests pass. Eight pure validator checks accept complete matrices
and reject missing/mismatched evidence; synthetic metadata is not algorithm evidence.
Inventory refresh preserves all56 canonical contracts and other candidate statuses;
only Frog Jump's alternative status advances to locally UI-verified. Live stats remain
431/431 traced, zero duplicates/orphans. No dependency or golden changes.

Early smoke/viewport captures are separate, not part of28 final rows. Interrupted server
restarts, old-bundle probes and premature body-decode failures are excluded. Firefox's
headless browser-chrome Tab boundary was replaced with within-document ShiftTab/Tab,
not a production keyboard change. Existing behavioral assertions were not weakened.
These are automated scenario checks, not screen-reader, physical-device, latency or
learner-study certification.54 other DP candidates and D6/D7 remain open.

## 2026-10-10 — Early P9 slice: retire the unused shell, preserve live seams

Status: local `chore/retire-legacy-ui`, based on #217 / `9de01cd`; unpublished.
The owner subsequently authorized commit/push/PR/merge on 2026-10-10, conditional on
clean verification. Publication remains gated on the exact PR head's CI.
Frog Jump previously merged as #216 / `4ad9fc3`, followed by MIT licensing #217.
The DP plan, tracker and remaining execution plan now distinguish those merged packages
from this local cleanup. This is owner-directed easy-to-hard step4, not all of P9.

### Production scope and reachability proof

`evidence/p9/production-reachability.mjs` uses the actual installed Vite/Rollup
production loader, including CSS, renderer-registry and lazy imports. Test imports
do not establish reachability. It builds with `write:false`, so checks cannot silently
replace the production bundle served by browser probes. The audit is deliberately
conservative: production-loaded modules count as reachable even if tree-shaken;
unused exports and arbitrary CSS selectors are not certified.

At the clean baseline:134 source/style files,115 production-loaded modules,19 orphans.
After cleanup:115 source/style files, the same115 loaded modules, zero orphans.
The before/after JSON manifests retain the exact inventory and production import graph.
`CanvasShell`, `CaptureStrip`, `CompareStrip`, shared layout utilities and the live
search hook remain. The standalone old `ProblemStatement` was genuinely unreachable:
the current workspace renders the authored statement inline.

Removed: Header, Breadcrumb, SectionNav, old ProblemStatement, Controls, LiveTraceTicker,
Sidebar, SearchBox, MemoryComplexityCard, useLayoutPreferences, groupBySection,
normalizeCategory and their seven exclusive CSS modules. Removed their global search,
sidebar, pulse and transport styles, retaining `.ip-input`, spinner/reduced-motion,
canvas, capture and every live token. Eleven unused shared layout utilities also retire.
The input editor now has only its existing normal-flow workspace layout; labels,
action order, presets, controlled draft and field/error behavior remain. `/` opens
the switcher directly, without the old permanently-false mobile-sidebar branch.
No production backend, dependency, lockfile, golden or persisted-key deletion.

### Retired tests and live successors

The suite count is accounted for:799 baseline −64 retired +49 additions =784.
All existing tests of live behavior keep their assertions; the InputPanel harness now
targets the visible workspace labels instead of the unreachable panel's alternate names.

| Retired suite | Cases | Live successor / explicit retirement |
| --- | ---: | --- |
| App.test.jsx | 6 | Real Workspace/Session integrations: context, playback, narration, empty/unavailable states; mocked old-shell rendering retires |
| Controls.test.jsx | 4 | Sharing.integration: actual restored address/input/approach/step, all-view copy entry points, success/timeout, clipboard denial/absence and exact timer cleanup |
| MemoryComplexityCard.test.jsx | 7 | CodeAnalysis + StateInspector: frames, singleton Front/Back, missing costs and run-level emptiness. Old internal Memory/Complexity tabs and step-only section hiding retire |
| ProblemStatement.test.jsx | 7 | ProblemWorkspace: full authored paragraphs/examples/constraints/safe source links; description fallback, absent metadata and native disclosure/no execution |
| SearchBox.test.jsx | 17 | CommandPalette/library/search seams: ranking, selection/arrows/Escape, recent searches, runnable filters, loading/empty states; new valid combobox IDs and selector-character ID guards. Sidebar section-group headings/full-section progress and its scoped empty-state mechanics retire |
| Sidebar.categories.test.js | 2 |18 live library cases derived from contracts/categories.json: every exact backend category, no duplicate/alias options, real filtered links |
| SectionNav.test.jsx | 10 |5 curriculum helper cases + existing integrated header/navigation/completion: authored order, boundaries, no section/singleton/unknown. Old neighbour watched badge was already absent from the new workspace; not claimed as migrated |
| useLayoutPreferences.test.js | 6 | Obsolete sidebar/mobile/statement/old code-panel toggle preferences retire. Current Code split/Focus/session/storage tests remain; old keys are not erased |
| groupBySection.test.js | 5 | Current curriculum/order tests and flat library/switcher tests; old sidebar grouping is not a live feature |

### RED proof and harness corrections

The new build-graph assertion first named all19 actual orphans. The global-style guard
failed on the old sidebar/search/transport CSS; the normal-flow default-editor guard
failed on the old panel default. After live successor checks passed, surgical temporary
mutations broke category filtering, actual-address copying, curriculum order, singleton
queue markers, editor action order, statement fallback and slash opening, and added an
orphan module.27 cases failed across seven suites. All mutations were restored before
the final clean suite/build. These are selective mutation proofs, not a claim that all49
new cases failed on main.

Excluded as test-harness mistakes: initially running Node's build check through DOM-only
storage setup; selecting a Playground-only copy button in Analysis; counting unrelated
fake timers; an incorrect switcher accessible name. The Node setup guard preserves DOM
storage isolation. Sharing now checks the exact2500ms timer and restores timer spies
in safe order. None of those initial failures is counted as a production defect.

### Checks, measurements and boundaries

- Fresh `npm ci`; frontend final:79 files /784 tests passed. Backend baseline:8,023
  tests,0 failures/errors,500 existing skips; backend/goldens unchanged.
- Production build: JS382.54kB /gzip121.90; CSS95.79kB /gzip17.43. Baseline:
  JS383.15/gzip122.13; CSS106.95/gzip19.10. The old modules were already tree-shaken;
  the material bundle reduction comes from retiring shipped global CSS, not deleting tests.
- Served final assets: `index-hqbHudI4.js` / `index-DuiaNkhD.css`.
- Chromium editor parity:40/40 cases, four genuine fresh backend default responses
  replayed unchanged × five widths × both themes. Computed editor/form/field/label/button
  geometry/styles and field-before-action order match exactly; view switches preserve
  input, step and URL and do not execute. These are replayed defaults, not custom-run proof.
- Chromium chrome journey:50/50 checks, same five widths/both themes;30 workspace
  geometry records match exactly, plus20 library/not-found overflow checks. Native Menu
  and learning-network disclosure/focus/keyboard behavior,44px chrome targets and errors checked.
- Real-backend phone input journey:14/14 checks across all seven field types;10 POST
  responses settle before navigation (seven200, three400), with field errors and URL
  preservation. The reusable probe now respects normal execution pacing, waits for the
  actual decoded response and writes a matching-build JSON report instead of only stdout.
- 14 Node evidence-helper tests pass (11 cleanup checks, two served-identity checks,
  one pacing check). The consolidator rejects incomplete/duplicate matrices, missing
  editor measurements, changed layout, changed production reachability and mixed bundles.
- Renderer sweep: clean baseline Chromium200/200; final Firefox200/200 and a fresh
  complete final Chromium200/200 over the same
  17 families/50 catalogue-derived picks, default/alternate, first/middle/final at1366 dark/
  390 light (600 position checks per sweep). These are drawing/overflow/error checks, not
  proof of every answer/input or pixel parity across engines. Two incomplete final Chromium
  attempts are excluded: one lost the backend; another lost the browser mid-sweep. The
  isolated Chromium heap alternate at the interruption point passes; no cause for the
  closure is claimed. The fresh full Chromium rerun closed the earlier incomplete-sweep
  gap without production changes. Its90 editor/chrome and14 input checks also pass.
  `evidence/p9/verification.json` records both complete final engines,
  exact retired-test counts, selective mutations, matching assets and excluded attempts.

The reusable evidence consolidator also needed to distinguish completed Vitest suite
lines from earlier stderr headers when accounting for retired tests. Its new parser
regression test fails if that distinction is removed; this is tooling, not an app defect.

Read-only saved-input target check (`evidence/p9/preset-targets.json`): at320 and1366,
Load is104×36px and Remove24×36px. This pre-existing gap follows the old workspace
section's same36px preset rule; no preset sizing change is made here. Fix and verify it
in step5/P8 target polish. The40-case editor parity matrix used defaults without saved
presets; it is not a claim to have measured every populated editor state.

The two inspected final screenshots (320 light,1366 dark) agree with unchanged chrome
geometry. The desktop stage-top exception remains unchanged: Two Sum/BFS frame y358.39,
LCS y391.39 at1366, not the target~280. No new layout/DP/performance package is stacked.
The output is not screen-reader, real-device/virtual-keyboard, human-learner or performance
certification. P7 acceptance, P8 and final P9 release/rollback/sign-off remain open.
Next safe step: review/publish this medium cleanup package under that owner approval;
only after it lands, continue step5 layout polish and the separately verified DP batches.

Publication preflight (2026-10-10): main is still `9de01cd`; the owner authorized
commit/push/PR/merge conditional on clean checks. All local gates for this bounded slice
are clean, including the final Chromium200-row rerun. Required frontend/backend CI
must pass on the exact PR head before merge. Rollback base is #217 / `9de01cd`;
revert the focused cleanup squash through a reviewed PR if a regression is found.
