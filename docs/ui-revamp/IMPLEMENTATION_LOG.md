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
