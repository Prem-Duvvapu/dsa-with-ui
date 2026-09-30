# Independent review — UI revamp through `db8683b`

Date: 2026-09-30

## Purpose and implementation instructions

This records the independent review of PRs #149–#153 and is a remediation handoff for the implementing agent. Findings and line references describe **`db8683b`**, not necessarily the current working tree. Check the current revision and existing changes before implementing; do not overwrite unrelated work.

Read `CLAUDE.md`, `REVIEW.md`, `RCA.md` (especially RCA-054), and `docs/ui-revamp/IMPLEMENTATION_HANDOFF.md` first. The implementation handoff remains the authoritative product specification. This review identifies defects; it does not replace the remaining P5–P9 plan.

For each fix:

1. Reproduce the finding against the affected revision or confirm whether subsequent work already fixes it.
2. Add a regression test that fails before the fix and passes afterward. Assert outcomes, not merely the existence of new controls.
3. Preserve the single execution/session owner and the backend's authority over algorithm data.
4. Verify the fix under the actual `ProblemWorkspace`, not only an isolated hook.
5. Use browser checks for geometry, scrolling, pointer interaction, responsive behavior, and native keyboard behavior.
6. Update the tracker and implementation log with actual evidence and remaining limitations.
7. Follow the owner's current authorization for commits, pushes, and PRs. This document does not authorize publishing.

Suggested order: B1/B2/S1 session correctness; B3 Analysis honesty; S2/S3/S4 interaction continuity; S5/S6 accessibility and resizing; S7/S8 error-state parity; test and documentation reconciliation. Keep changes in coherent, reviewable packages. Do not defer blocking correctness fixes behind P5 feature work.

## Scope and verdict

Reviewed each commit and the combined range `3db06a5..db8683b`:

| PR | Commit | Scope |
| --- | --- | --- |
| #149 | `de94c1c` | Previous review findings #1–#8: session/URL lifecycle |
| #150 | `b9f114e` | Control-boundary contrast, 44px targets, tracker reconciliation |
| #151 | `b0da81f` | P3 workspace behind a flag |
| #152 | `ce99754` | P4a Code walkthrough and Analysis |
| #153 | `db8683b` | P4b default workspace and legacy-shell retirement |

**Blocking issues remain.** The previous fixes are substantial, but same-page navigation still breaks run/link identity, and Analysis fabricates container semantics for some real algorithms.

Review verification:

- `npm ci`, the full frontend suite, and production build passed in an isolated copy.
- **69 test files / 560 tests passed.**
- Build: **337.34 kB JS, 106.89 kB gzip**.
- Chromium with the local Java API: default Array, BFS and 2D-DP pages had no horizontal overflow at **320, 390, 768, 1366 and 1440px**, both themes.
- Temporary adversarial probes reproduced the failures below. These probes were diagnostic files outside the repository, not committed regression coverage; recreate durable tests as part of remediation.
- Backend tests were not rerun.
- The review made no repository changes, pushes, or PRs. This Markdown document was added afterward at the owner's request.

Unless otherwise indicated, paths below are relative to `frontend/src/`, with lines at **`db8683b`**.

## Blocking

### B1. Same-page navigation does not retire an obsolete execution

**Files:** `hooks/useProblemSession.js:72`, `:123`, `:165`

**Failing sequence:**

1. Open a shared `{n:7}` link; hold its POST response.
2. Navigate to the same problem without input.
3. Release the old response.
4. `{n:7}` replaces the default execution, although the new URL specifies defaults.

A pending ordinary submission has a similar failure: its eventual success can overwrite the newer navigation's share parameters.

Replacing `pendingRestore` cancels the restoration continuation, **not the execution**. `runInput` commits before the `alive()` check. Ordinary submission checks only mounted status and problem ID, not navigation identity.

**Evidence:** Reproduced both through the session hook and through `ProblemWorkspace`.

**Why it matters:** The displayed execution can belong to an abandoned navigation.

**Suggested fix:** Retire the execution generation on every external session navigation, including same-page navigation requiring no new request. Check that generation before committing and sharing.

### B2. Rejected or malformed same-page links mislabel a retained custom run as defaults

**Files:** `hooks/useProblemSession.js:117`, `:129`; `workspace/sessionCopy.js:24`

**Failing sequence:**

1. Successfully restore custom input `{n:7}`.
2. Navigate to a same-problem link containing rejected `{n:99}`, or malformed input.
3. The previous `{n:7}` run remains displayed.
4. The application removes `input` from the URL and announces **“Showing the default input.”**

Copying or refreshing that URL produces a different execution.

**Evidence:** Reproduced with hook and workspace integration probes.

**Why it matters:** Both the explanatory text and the share link contradict the retained run.

**Suggested fix:** Explicitly choose either to retain and re-share the previous committed run, with truthful wording, or successfully execute defaults before claiming defaults are displayed.

### B3. Analysis falsely identifies stacks and priority queues as FIFO queues

**File:** `workspace/StateInspector.jsx:71`

`containerKind` declares every Graph, Matrix and Array container a queue.

**Concrete counterexamples:**

- `flood-fill` is a Matrix tracer using a LIFO stack. Its Analysis view displays **“Queue contents”**, **Front/Back**, and “the next to leave is at the front.”
- `MstTheoryTracer` uses a priority queue while reporting Graph as its visualization type.

The payload's `queueOrStackState` does not encode those semantics. Copying the companion renderer's assumption does not make it authoritative.

**Evidence:** Confirmed against backend tracer implementations and the live Flood Fill Analysis page.

**Why it matters:** This teaches the wrong data-structure behavior—the repository's central honesty invariant.

**Suggested fix:** Use neutral container wording unless an authoritative contract establishes kind and ordering. Add explicit backend metadata separately if precise labels are required.

## Should fix

### S1. Same-page links without a step, or with an invalid step, retain the old position

**File:** `hooks/useProblemSession.js:77`, `:134`

**Sequence:** Load the default execution at step 3, then navigate to the same problem with no query parameters. It stays at step 3 instead of returning to step 1.

Navigating instead to `?step=400` also retains step 3 while the notice says **“Showing step 1.”**

**Why it matters:** Restoration and its explanation disagree.

**Suggested fix:** Define and apply the step transition for every external navigation: valid requested step, otherwise the documented reset. Do not depend on a new execution implicitly resetting playback.

### S2. Source scrolling and dialogs do not own their keyboard input

**Files:** `hooks/useKeyboardShortcuts.js:42`, `:95`; `workspace/SourcePane.jsx:41`

**Browser-confirmed sequences:**

- Focus Java source at BFS step 3 and press **End**. Playback jumps to the final step instead of only scrolling source.
- Open Help and press **Home** or **L**. Playback changes behind the dialog.
- The separator handles arrows/Home/End itself, but unhandled player shortcuts still pass through.

The global handler has no dialog guard, and source scrolling keys only suspend follow—they do not prevent the global player from consuming them.

**Why it matters:** Reading code or help unexpectedly changes execution and can mark a problem watched.

**Suggested fix:** Establish explicit shortcut ownership for dialogs, source scrolling and separators. Preserve player shortcuts outside those contexts. Respect `defaultPrevented` before modifier shortcuts too.

### S3. Code subview changes do not pause

**File:** `workspace/ProblemWorkspace.jsx:483`

**Sequence:** On a narrow screen, start playback in Code view and select Diagram or Source. Playback continues; the Pause button remains displayed.

`onSubview` directly calls `setCodeSubview`, bypassing the pause behavior used by top-level views.

**Why it matters:** The learner cannot switch how they inspect a step without execution continuing underneath them; this violates the session transition rules.

**Suggested fix:** Route subview selection through a pause-and-select callback. Add a test that begins with playback running.

### S4. Source presentation state is only partially preserved and reset

**Files:** `workspace/SourcePane.jsx:19`, `:25`; `workspace/ProblemWorkspace.jsx:114`

Two failures:

- Horizontally scroll source to `scrollLeft=100`, switch to Analysis and back: it resets to zero. Only `scrollTop` is saved.
- Back/Forward between different problems already in Code view can reuse the mounted `SourcePane` and retain its follow state. Resetting the memory ref does not reset the component's `following` state.

**Evidence:** Browser reproduction plus component lifecycle inspection.

**Why it matters:** The documented source-position/follow contract does not hold across all presentation and problem transitions.

**Suggested fix:** Preserve both axes and explicitly reset problem-bound presentation state using problem identity, rather than only changing a ref.

### S5. Separator drag cleanup and announced geometry are incorrect

**File:** `workspace/CodeWalkthrough.jsx:46`, `:80`

**Lifecycle failure:** Start dragging, then unmount Code view before `pointerup`. Global pointer listeners remain installed. There is no pointer capture, cancellation handling, or unmount cleanup.

**Geometry failure:** At a 1000px viewport, pressing End announces **Diagram 70%, source 30%**, while measured panes are **502px and 360px**—approximately 58/42. CSS minima override the stored ratio.

**Why it matters:** An abandoned drag retains listeners and can continue modifying preferences; assistive output misstates the actual layout.

**Suggested fix:** Use pointer capture and cleanup for release, cancellation, lost capture and unmount. Derive effective ratio bounds from measured available width and pane minima, and announce the resulting geometry.

The separator also reintroduces the decorative `--rule-strong` as an interactive boundary at `workspace/ProblemWorkspace.module.css:125`, and its hit area is only 24px wide. Apply the control-boundary token and provide a larger hit area.

### S6. Tab relationships and roving focus are incomplete

**Files:** `workspace/ViewRail.jsx:43`, `:44`; `workspace/ProblemWorkspace.jsx:399`

- `aria-controls` points to `workspace-view-panel`, but that element has no `tabpanel` role. The role is on its parent.
- Arrowing to another tab moves focus but leaves its `tabIndex=-1`; the selected tab retains `0`.
- Assigning `role="tabpanel"` to `<main>` replaces its main landmark semantics.

**Evidence:** Browser and regression probes.

**Why it matters:** The accessible relationships and keyboard focus model do not match the declared manual-activation tab pattern.

**Suggested fix:** Keep a semantic `<main>`, place the labeled tabpanel role on the referenced inner element, and track the focused tab independently from the selected tab.

### S7. Initial execution errors disappear outside the diagram

**Files:** `workspace/ProblemWorkspace.jsx:252`, `:282`, `:490`

**Sequence:** Open `?view=analysis` while `/execute` returns 500. The page shows generic “No trace steps available,” but not the specific backend failure.

The differentiated initial-error messages exist only inside `stageBody`, which Analysis and narrow Code/Source do not render.

**Why it matters:** Switching presentation hides the reason execution is unavailable. The same concern applies to validation arriving after the editor has been unmounted by a view change.

**Suggested fix:** Render session-level failure status independently of the canvas. Preserve an actionable route to validation errors in the editor.

### S8. Not-found pages expose nonfunctional Help and Switch problem controls

**File:** `workspace/ProblemWorkspace.jsx:222`

**Sequence:** Open `/problem/does-not-exist`, then click Help or Switch problem. Nothing opens.

The early return renders header controls but omits their dialogs.

**Evidence:** Confirmed in Chromium.

**Why it matters:** Recovery/navigation actions advertised on the error page are dead controls.

**Suggested fix:** Keep shared overlays outside the conditional page body, or omit unsupported controls from that state.

## Notes

### N1. The y389 exception is accurately documented, but not inevitable

**Files:** `workspace/ProblemWorkspace.module.css:19`, `:51`; `docs/ui-revamp/REVAMP_TRACKER.md` (repository root)

Measured the stage frame at **y389.39**, matching the documentation. The diagram interior starts around **y423**, below its legend.

The larger stage is a real improvement. However, preserving the owner's hierarchy does not inherently require all the current context and card spacing. Compacting those gaps is a cheaper next experiment than removing context or reducing diagram height.

**Suggested follow-up:** Keep this recorded as an unmet target with an explicit acceptance decision—not as a passed visual gate.

### N2. The URL hook remains a single-writer abstraction

**File:** `hooks/useLatestSearchParams.js:68`

Single-instance probes passed for StrictMode, 100 coalesced writes, external navigation carrying a previously committed token, and navigation replacing state.

Two instances writing different parameters in the same tick still lose one write because each owns a separate local snapshot.

**Why it matters:** The current route structure avoids this, but P5 could accidentally introduce a second writer.

**Suggested fix:** Document/enforce one route-level instance, or share the coordinator if multiple consumers need write access.

### N3. Deferred cleanup inventory is incomplete

**Files:** `search/groupBySection.js:1`, `search/normalizeCategory.js:1`; `docs/ui-revamp/IMPLEMENTATION_LOG.md:341` (repository root)

Both search helpers are outside the production entry-point import graph in addition to the listed legacy components. `SearchBox` is still imported by the dead `Sidebar`, so “unused except by tests” is imprecise; they are transitively unreachable from the application.

**Suggested fix:** Include these dependencies in P9's reachability-based cleanup. No immediate runtime impact.

## Status of previous review findings #1–#10

| Previous finding | Status | Evidence |
| --- | --- | --- |
| #1 Previous problem's input restored into next problem | Fixed | Navigation counter avoids stale restoration; workspace lifecycle regression passes. |
| #2 Late request navigates back after leaving | Fixed | Request retirement and mounted guards; workspace leave-mid-run regression passes. B1 is a remaining same-page variant. |
| #3 Restoration overwrites newer edits | Fixed | Draft seeded before execution; success and rejection workspace cases preserve edits. |
| #4 Superseded restoration stays stuck | Fixed | Submission releases its restoration claim; regression passes. |
| #5 Query-string identity and unbounded pending list | Fixed for current single-writer usage | Tokens replace string matching; list bounded; additional coalescing/state probes pass. See N2. |
| #6 Same-problem external navigation ignored | Partially fixed | Valid custom/default restoration works; cancellation and absent/out-of-range step transitions fail. |
| #7 Malformed links silently substitute defaults | Partially fixed | Initial-load notices work; same-page malformed/rejected links mislabel retained custom runs. |
| #8 Previous run exposed under new problem ID | Fixed | Run-derived fields are identity-gated; regression passes. |
| #9 Library control contrast | Fixed | New token defined in all themes and contrast guard passes. The new separator has a separate gap. |
| #10 Library 36px targets | Fixed | CSS guards pass; browser measured filter chip height at 44px. |

## Tests weakened or ineffective

The migration is mostly meaningful, but **“every behavioural assertion is kept” is inaccurate**.

- **`workspace/Workspace.integration.test.jsx:216`:** the parameterized DP/Graph/Tree test drops the old DP-table presence assertion and its one-shell/one-legend checks. Removing the DP renderer could now leave this test green.
- **`workspace/Workspace.integration.test.jsx:501`:** “code, analysis and editor on a phone” only checks two tab buttons. It neither opens their content nor asserts the editor.
- **`workspace/Workspace.integration.test.jsx:115`:** checking two searchable examples does not replace the old full catalogue cardinality assertion. Search remains a useful successor journey, but coverage is narrower.
- **`workspace/ProblemWorkspace.test.jsx:155`:** checks the panel's label, not whether each tab controls that panel or whether the main landmark remains.
- **`hooks/useProblemSession.test.jsx:49`:** claims to verify mirroring, but only asserts the local step index—not the URL.
- **`workspace/CodeAnalysis.test.jsx`:** scroll preservation covers vertical position only; subview tests start paused; separator tests do not cover pointer cleanup or actual pane geometry.

The P4 regression claim is supported: transplanting its test file into the P3 implementation produced **11/11 failures**, including all ten originally claimed cases.

Missing effective coverage includes B1/B2, non-FIFO real containers, source keyboard ownership, modal shortcut isolation, running subview changes, horizontal source restoration, drag cancellation, and initial errors outside Playground.

## Documentation and verification limits

Confirmed:

- Final **560-test** count and successful build.
- Bundle below the recorded P0 baseline.
- No dependency or backend changes in the reviewed range.
- Documented stage-frame position.
- Normal view changes preserve execution without another request.
- Only one primary diagram is mounted by the main view branches.

Claims contradicted by the findings:

- Complete same-page link correctness.
- Graph/Matrix/Array containers necessarily being queues.
- Complete source-state continuity and shortcut ownership.
- Preservation of every migrated assertion.

Not independently verified:

- All historical intermediate test counts and browser-capture totals.
- Maximum-input/widest-structure coverage across every renderer.
- All seven input contracts exercised on phones.
- Second-engine, screen-reader, 200% zoom and full reduced-motion journeys.
- Exhaustive interrupted-concurrent-render/token-eviction interleavings.

No newly introduced unresolved CSS variable/class reference was identified.

## Exit criteria for remediation

- [ ] B1–B3 fixed with meaningful regression tests.
- [ ] S1–S8 fixed, or explicitly dispositioned with evidence and owner acceptance.
- [ ] Same-page and cross-problem navigation tested with success, rejection, malformed input, delayed responses and Back/Forward.
- [ ] Sharing checked against the displayed committed run after every failure/cancellation path.
- [ ] Analysis checked against real non-FIFO tracers, not only BFS fixtures.
- [ ] Source/dialog/separator/tab keyboard ownership verified in a browser.
- [ ] Source scroll/follow, split geometry and drag cleanup verified across view and problem transitions.
- [ ] Weakened migration assertions restored at their appropriate new feature locations.
- [ ] Full frontend suite and production build pass; relevant browser journeys recorded.
- [ ] Tracker/log claims reconciled, with remaining limits stated explicitly.

**P5's riskiest area is shortcut and navigation ownership.** Fix the remaining session identity failures first, then establish one explicit interaction policy for source panes, dialogs, tabs, separators and Focus mode.
