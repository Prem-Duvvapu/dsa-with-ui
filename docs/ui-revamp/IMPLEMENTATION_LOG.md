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
