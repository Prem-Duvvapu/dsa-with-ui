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
