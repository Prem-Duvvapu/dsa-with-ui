# Complete the DSA UI revamp — execution plan

Prepared: 2026-10-05. Inspected main: `dad2640`, merged PR #191.

### Current execution order — owner directed, 2026-10-09

The original sequence below is historical planning. P5/P6 implementations have merged;
P7 acceptance, P8 and P9 remain open. The owner requested the following easy-to-hard
order, with medium packages merged one at a time:

1. Reconcile documentation/trackers against live code and verified evidence (done locally).
2. Complete the local Frog Jump production-browser gates (done:28 final rows; see DP plan).
3. Request publishing approval; commit/push/PR/CI/merge that package before new work.
4. Remove proven unreachable code, retaining useful live-workspace regression guards.
5. Finish responsive/layout polish, including the documented desktop stage exception
   and the measured saved-input Load/Remove target gap (`evidence/p9/preset-targets.json`).
6. Expand simpler DP recurrences, then grid/path problems, in individually verified packages.
7. Close second-engine/device/screen-reader gaps where available; record unavailable human checks.
8. Expand medium DP families (strings, sum/knapsack/coins), with independent references.
9. Benchmark and fix demonstrated long-trace performance friction.
10. Expand harder multi-state, interval and reconstruction-heavy DP families.
11. Perform genuine learner/owner task review, then retest observed improvements.
12. Reconcile all ledgers and complete final release/deployment/rollback checks.

Steps1–3 are complete: Frog Jump merged as #216 / `4ad9fc3`, followed by the separate
MIT license package #217 / `9de01cd`. The owner resumed work on 2026-10-10.
Step4 is the current unpublished package on `chore/retire-legacy-ui`: actual production
reachability, dead UI retirement and useful live-workspace regression guards. Publication
was authorized on 2026-10-10 conditional on clean checks; do not stack step5 or another DP implementation
before this package lands.54 other DP candidate rows are
unimplemented. Applicability must be reviewed rather than forcing three options on every
problem. Completing an early cleanup or pilot check does not close P8/P9 or DP D6/D7.

## 1. Objective and source of truth

Complete every remaining UI revamp package and close the acceptance gaps in already
implemented packages. Deliver the approved spacious learning workspace with every
existing capability preserved, truthful algorithm data, and measured verification.

`IMPLEMENTATION_HANDOFF.md` remains the authoritative product specification. This file
turns its remaining requirements into an implementation sequence; it does not weaken
the handoff or replace its F01–F35 feature definitions. Use `REFERENCE_DESIGN.md` and
`playground-concept.png` for visual decisions, and the tracker/log for actual evidence.

A phase is complete only when its acceptance gate has evidence. An unavailable human
screen-reader or learner review stays explicitly pending; a successful build cannot
substitute for it. Never claim “best UX,” accessibility certification or user research
without supporting evidence.

## 2. What already exists

| Area | Merged implementation | Remaining responsibility |
| --- | --- | --- |
| P0 | Baseline, authoritative handoff, design reference and concept | Refresh checks against the actual starting revision |
| P1 | Atomic run identity, controlled drafts, truthful sharing, request retirement and restoration | Preserve all lifecycle guards during later UI changes |
| P2 | Library, shared catalogue, URL filters, load more, return context, tokens | Regression verification; no library rewrite |
| P3 | Approved workspace and Playground | Close remaining layout/visual acceptance gaps |
| P4 | Code split/subviews/follow, Analysis, default workspace route | Verify final integration and reconcile the stale “Review” ledger status |
| Review fixes | #149–#150, #155–#157, #191 | Preserve cancellation, no-run/offline honesty, source keyboard ownership and tab/drag/error behavior |
| Beyond revamp | Statement rollout, source syntax colors and multiple tracer improvements | Preserve them; do not reverse them to match an old screenshot |

PR #191's implementation passed 616 frontend tests, a production build, backend CI,
and 40 fixture-backed browser checks. These are historical results, not a fresh baseline
for the implementing agent. Fetch main and measure again.

The original Windows-mounted checkout may still be on `docs/ui-revamp-agent-handoff`
with untracked handoff/review files. Do not implement against that old revision, switch
over user changes, or delete those files to make a checkout succeed. Use a clean working
branch/worktree from current main and preserve unrelated work.

## 3. Non-negotiable invariants

1. One execution/playback owner and at most one active primary diagram.
2. Run, resolved input, submitted snapshot, anchors, selected step and URL describe
   the same execution. Draft edits alone never alter the committed run or share identity.
3. View/subview/Focus changes, layout changes and theme changes never execute again.
4. View/subview changes, dialogs, Focus transitions and run submissions pause;
   returning to a visible browser document never automatically resumes.
5. Rejected/failed reruns retain valid prior data with truthful notices. If no run exists,
   say so. Offline input is unknown unless supplied by an authoritative sample contract.
6. Truncated, sample, malformed, empty and failed executions never masquerade as
   successful learning completion or mark watched incorrectly.
7. No invented summary, constraints, complexity, allocations, frame order or FIFO/LIFO
   semantics. Graph/Matrix/Array container snapshots remain neutral without metadata.
8. Keep `/problem/:id`, input/step/view encoding, library params, persisted keys, presets,
   stars, watched progress, streak, Continue and authored curriculum order compatible.
9. Exactly one URL writer per active route. Share its adapter with new controls.
10. Extend React/JSX, existing hooks, CSS modules and tokens. No framework migration,
    general-purpose editor, analytics, account system or unrelated algorithm work.

## 4. Delivery sequence

Use medium, coherent change sets. Split a package further if it becomes difficult to
review, but keep a usable product after every merge. Do not ship P5–P9 as one large PR.

| Order | Change set | Dependency | Exit evidence |
| --- | --- | --- | --- |
| 0 | Reconcile current state and baseline | Current main | Accurate tracker, baseline, remaining gaps |
| 1 | P5a switcher and modal ownership | 0 | Keyboard selection/cancel/current-problem, inertness and focus return |
| 2 | P5b Focus mode | P5a | Session, source/split state and exit-focus restoration |
| 3 | P5c guidance and shortcut policy | P5a–b | Real visible tour targets; no keyboard theft |
| 4 | P6a bounded textual history | P5 | All steps reachable, bounded DOM and synchronized seek |
| 5 | P6b comparison and completion | P6a | Real second traces, correct scope and truthful completion |
| 6 | P7a linear/string/search/heap families | P6 | Real desktop/phone default/custom/boundary evidence |
| 7 | P7b spatial/tree/graph/list/recursive families | P7a | Layout, semantics and input coverage |
| 8 | P7c grid/DP/companions and editor gaps | P7b | Entire registry and seven input contracts covered |
| 9 | P8a responsive/visual/accessibility fixes | P7 | Required viewport/theme/keyboard/zoom/state matrix |
| 10 | P8b performance and learner-task iteration | P8a | Timings, bundle deltas and observed friction resolved |
| 11 | P9 cleanup and release handoff | All gates | Reachability cleanup, complete ledgers and release checks |

Publish only under the owner's authorization. When publishing is authorized: commit the
coherent change set, push, open a PR, wait for all required checks and merge, then base the
next package on the merged main. Never use administrative bypass to avoid failing CI.

## 5. Order 0 — establish the actual starting state

1. Read repository instructions, REVIEW.md, RCA-054 and its follow-up, handoff, reference,
   tracker/log and both independent reviews. Check current history, worktrees and dirt.
2. Fetch main. Identify changes newer than this plan before implementing anything.
3. Inventory actual production imports and current registry/input contracts. Do not
   quote problem counts from documentation; derive them from the running API.
4. Run frontend tests/build, backend tests and startup smoke. Investigate failures;
   distinguish existing failures from newly introduced ones with reproducible evidence.
5. Capture the current library and Array, Graph+container, 2D DP workspaces in both themes
   at phone/laptop widths. Record the actual stage coordinate and useful diagram area.
6. Reconcile P4 and stale feature-ledger descriptions with code. Passing previously
   restored integration tests does not alone prove every input type was browser-tested.
7. Start an accurate remaining-work checklist in the existing tracker. Avoid another
   competing roadmap document or an unbounded collection of per-PR Markdown reports.

## 6. P5 — switching, Focus and guidance

### P5a: upgrade the existing switcher

- Reuse CommandPalette and the existing search ranking/highlighting/recents adapters.
  Preserve its commands as well as problem selection.
- Add bounded results, useful empty/loading states and View all results carrying the
  query into the library. No permanently visible sidebar returns to the workspace.
- Keep Ctrl/Cmd+K route-specific: workspace opens switcher; library focuses its query.
- Opening pauses playback. Initial focus is search; keyboard selection scrolls into view;
  Enter chooses; Escape cancels; selecting the current problem closes without rerunning.
- Make the background inert and trap focus. Restore the invoking control on dismissal,
  or a sensible visible fallback if it disappeared. Release inertness and scroll locks
  on close, route change and unmount. Do not stack independent modal traps.
- Selecting another problem intentionally drops the old input/step and retires requests.

Gate: pointer and keyboard open/select/cancel; current-problem selection; filtered
View all results; Help/menu invocation; not-found recovery; pending-run navigation;
denied storage; StrictMode cleanup; focus containment/return in a browser.

### P5b: implement in-page Focus mode

- Add Focus at the visualization and a visible Exit focus. Keep problem identity, primary
  structure/companions, narration, step indicator, playback and truthful status visible.
- Hide optional context/editor/history surfaces while retaining their session state.
  Reuse the existing stage; do not introduce a second primary canvas or execution owner.
- Pause on entry and exit. Preserve run, step, dirty draft, speed, source position/follow,
  Code subview and split preference. Restore applicable presentation and trigger focus.
- Escape exits only after higher-priority dialogs are closed. Problem/view changes exit
  Focus. Resizing safely selects split or subviews without resetting execution.
- Use existing native page scrolling, tokens and responsive rules; browser fullscreen
  is not required. Keep the virtual keyboard and sticky rail from obscuring controls.

Gate: nontrivial custom run + dirty draft → Focus → resize/theme/modal → exit → inspect
source and draft. Same run/step/input/URL, no extra execution, correct focus and pause.

### P5c: finish keyboard/help/tour integration

- Define one explicit ownership policy: handled events and native editing/scrolling
  controls first, active modal, composite widget, source, then player commands.
- Preserve J/K/L, comma/period, arrows, Home/End, speed keys and search/help commands
  where applicable. Source arrows scroll natively; buttons never double-activate.
- Update help to match actual bindings and current Focus/History actions.
- Keep welcome dismissal and replay storage compatible. Tour navigation must reveal a
  target's view/disclosure/subview before spotlighting it. Adapt narrow screens rather
  than pointing at absent elements; finish/cancel restores sensible focus and context.
- Verify behavior with a missing/not-found problem, unavailable run and pending request.

Gate: complete keyboard-only journey; nested Escape priority; no accidental seek behind
dialogs; no hidden focus target; every tour step points to a visible mounted element.

## 7. P6 — history, comparison and completion

### P6a: all-family textual history

- History exists for every valid trace family, independently of CaptureStrip exclusions.
  Preserve useful existing captures; do not force Graph/Tree/DP into unsuitable strips.
- Add 50-entry pages with actual range, total, Previous/Next page and Jump to current.
  All steps are reachable, including the final step of a near-limit trace.
- Label each entry with step and real narration; mark the selected entry accessibly.
  Selection pauses and calls the existing seek owner, synchronizing diagram/source/
  Analysis/URL. Paging and opening alone do not seek or execute.
- Keep disclosure focus and announcements predictable. Reset history presentation on
  new run/problem identity; preserve it across view changes where the session requires.
- Render no more than 50 textual entries per page and no diagram per history entry.

Gate: first/middle/final and cross-page seeks; current outside page; rerun reset; view
re-entry; pending/failed run; truncated run; bounded DOM on a near-limit execution.

### P6b: comparison and truthful end-of-run actions

- Keep comparison on demand and make its supported scope discoverable for all families.
  Reuse useComparisonTrace's independently fetched default/alternate runs and retry.
- Label both input identities and selected positions; clearly distinguish default-vs-
  alternate comparison from a custom main run. Never claim ordinal step N is the same
  algorithm phase on both sides. Do not synthesize or silently reuse the main trace.
- For families without a suitable capture, provide truthful textual inspection instead
  of pretending the capability does not exist. Opening comparison does not replace the
  main run, mutate its draft, or fetch it again.
- On genuine final-step completion offer Replay, Other case and Next problem. Avoid a
  blocking congratulations overlay; use actual alternate input and curriculum neighbors.
- Keep truncation, offline, pending and retained-result failure statuses visible across
  views/Focus. A terminal visible step is not proof of a successful completed run.
- Preserve saved/alternate load-and-run behavior, copy link failures and long-input
  notices, progress, stars and presets.

Gate: independent trace/input pairs; custom-main identity retained; error/retry and
late-response cancellation; no eager compare fetch; complete/truncated/offline/no-run
states; accurate watched semantics and first/last curriculum boundary actions.

## 8. P7 — renderer and editor coverage

Generate a manifest from the actual registry plus current catalogue/specs. For each
entry record representative ID, structural variant, default/custom input, result,
viewport/theme, first/middle/final inspection and evidence location. The canonical
review focuses and seven input contracts are in handoff §8.

| Package | Families/variants to cover |
| --- | --- |
| P7a | Array/pointers, Bits, Window, SearchSpace, String/Unicode, PriorityQueue/heap |
| P7b | Tree/sparse/skewed, Graph/directed/weighted/disconnected, LinkedList variants, Stack, Queue, Trie, RecursionTree, DSU, Interval |
| P7c | Matrix, 1D/2D/slice DP, grid/container companions, all remaining editor gaps |

Use actual registered keys; reconcile new ones instead of assuming this table is exhaustive.
Test meaningful payload variants rather than only one arbitrarily selected problem.

- Every family: laptop and phone, both themes, real backend default and materially
  different valid input. Inspect first/middle/final, empty when allowed and large bounded
  states. Invalid inputs remain invalid; do not change backend limits to ease screenshots.
- Verify local diagram overflow, labels, resize/view re-entry, Focus and split/subviews.
  Fix containing grids/wrappers before touching algorithm-specific renderer semantics.
- Companion presence is run-derived and stable when empty; retain last-payload behavior.
  Unknown container semantics stay neutral. Unknown dsType renders unsupported explicitly.
- Inputs: INT temporary empty/invalid integer; STRING whitespace/Unicode/length; arrays
  add/remove/negative/duplicates; linked-list sequence; sparse tree nulls; rectangular
  grid edits/errors; graph weights, endpoints, isolated nodes and accessible labels.
- Test all seven editors on phones with error correction and the virtual keyboard where
  available. Associate labels, help and errors programmatically; never coerce absent/null
  data into an apparently valid zero or fabricated structure.
- Any demonstrated backend contract defect gets a separately scoped, tested change;
  no opportunistic catalogue/tracer rewrites in the frontend revamp.

Gate: no missing registry or input-type row; evidence proves reachability, truthful
semantics and usable sizing. Fixtures support deterministic regression tests, while
real API executions establish integration correctness.

## 9. P8 — finish layout, accessibility, performance and usability

### P8a: visual and accessibility acceptance

- Compare against the approved concept and HLD's implemented hierarchy. Keep spacious
  tasks and natural scrolling; do not solve crowding by deleting features or hiding them.
- Investigate the recorded y389 stage start versus ~y280 at 1366×768. First compact
  redundant context/gaps, with statement closed. Measure useful diagram area separately
  from its card top. Preserve readable title/controls and minimum useful diagram heights.
- Verify 320×568, 390×844, 768×1024, 1366×768 and 1440×900, plus short landscape and 200%
  zoom. No page-wide horizontal overflow; wide spatial structures may scroll locally.
- Both themes, system theme changes, reduced motion, long title/narration, max bounded
  input, tallest companions and expanded statements/history/errors must remain usable.
- Keyboard, skip link, landmarks/headings, tab semantics, dialog inertness/focus return,
  44px targets, visible unobscured focus, contrast and validation announcements.
- Preserve textual diagram alternatives. Announce manual steps without flooding screen
  readers during autoplay. Check at least one real screen reader when available.
- Include Chromium and a second engine when available. Record missing human/device/
  engine checks explicitly; do not silently turn them into passed release gates.

### P8b: measured performance and learner tasks

- Benchmark a near-limit real trace: decode/render, Play, beginning/middle/end seek,
  view switch, Focus, split resize and history paging. Record machine/build/trace size.
- Target ordinary control feedback around 100ms; investigate repeated stalls above
  200ms. Bound DOM and memory; never introduce unbounded run/draft caches.
- Confirm one timer/canvas, catalogue lifetime, no presentation-triggered execution,
  comparison fetch only on demand, observer/listener/scroll-lock cleanup.
- Compare raw/gzip bundle with recorded P0 and fresh baseline. Investigate >10% gzip
  growth; do not remove required UI merely to meet a bundle number.
- Perform all nine learner tasks in handoff §10, including phone and keyboard journeys.
  Record success, wrong turns, hints, lost context and timing. Label an agent walkthrough
  as expert evaluation; never invent participant feedback.
- Fix observed friction in a bounded iteration, then repeat affected tasks. Include
  before/after comparisons with baseline and the owner concept.

Gate: measured issues resolved, matrix complete, genuine human checks completed or
explicitly pending for owner acceptance. Unsatisfied visual targets remain open unless
the owner accepts a documented alternative; “looks good” is not automatic gate closure.

## 10. P9 — cleanup and release

1. Build a production import/reachability inventory, including transitive dependencies,
   lazy imports, CSS, tests and cross-tier guards. Never delete service providers merely
   because their names look legacy; they still own catalogue metadata.
2. Remove truly unreachable legacy leaf components/hooks/styles and tests that exist
   only for them. Move any still-useful behavioral guard to the live workspace first.
   Inspect the log's old Header/Sidebar/SearchBox/Controls/etc. list instead of trusting it.
3. Remove obsolete flags, duplicated shortcut handlers and abandoned CSS. Preserve active
   instructions, handoff, tracker/log, reference design, meaningful reviews and evidence.
   Six superseded September 23 planning files are removed in the accompanying cleanup.
4. Reconcile README, CLAUDE.md, architecture, tracker, implementation log and F01–F35.
   No stale “not implemented,” “behind flag,” wrong queue claims or obsolete test paths.
5. Run full frontend/backend suites, production build, startup cleanup and the final
   browser journeys on the exact release candidate. Review artifacts and dependency diff.
6. Record release commit, PRs, rollback boundary, known limitations, commands and evidence
   locations. Use a normal tested revert for rollback, not reset/force-push on main.
7. Publish only under current owner authorization; wait for frontend/backend/security/
   deployment checks. Distinguish merged code, preview deployment and production checks.

Gate: every phase and feature has a truthful disposition; no hidden unfinished feature;
reproducible final checks and a handoff that another maintainer can follow.

## 11. Verification and documentation discipline

For behavioral defects: add a test reproducing the user's sequence, prove it fails
against the old code, then fix. Do not replace semantic assertions with control presence.
Use targeted checks during changes, full relevant suites before publication, and browser
checks for focus, geometry, native scrolling, pointer behavior and responsive layouts.

```sh
npm --prefix frontend ci
npm --prefix frontend test
npm --prefix frontend run build
mvn -f backend/pom.xml test
bash start-smoke-test.sh
```

Ports at this baseline: Java 8923, Vite 5180. Inside WSL use direct Linux commands as
CLAUDE.md describes. Keep browser tooling out of runtime dependencies; reuse evidence
scripts/cached Playwright when suitable. Any new tool/dependency needs a specific reason.

After each package update tracker/log with the exact commit/build, tests, red-first proof,
real-backend-vs-fixture status, browser measurements, omissions and remaining work. Save
reusable probes and representative evidence; avoid an unnecessary Markdown file per fix.

## 12. Completion report

The implementing agent's final report must include:

- P0–P9 disposition, distinguishing already shipped, newly implemented and pending gates.
- F01–F35 and registry/input coverage completion, with links to evidence.
- Changed packages/PRs/commits, meaningful tests and final command results.
- Responsive/accessibility/performance measurements and unresolved human checks.
- Final visual comparison, remaining limitations and exact next steps if anything is blocked.
- Publication state and rollback instructions.

Do not stop merely because a package passed its unit tests. Continue through all authorized
work. A missing owner decision or unavailable human review should be reported explicitly
after exhausting independent work; it must not cause unrelated phases to be abandoned.
