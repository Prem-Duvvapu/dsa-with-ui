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
