# Prompt for Opus 5.5

Paste the block below into Opus. If the plan exists only in a local worktree, give the
agent its absolute path too; do not assume it has already been merged into main.

```text
Complete the remaining DSA UI/UX revamp, including acceptance gaps in the implemented
phases. Carry the authorized implementation through P5–P9; do not stop after one package.

Read first:
- CLAUDE.md, REVIEW.md, RCA.md (RCA-054 and its follow-up).
- docs/ui-revamp/IMPLEMENTATION_HANDOFF.md — authoritative product specification.
- docs/ui-revamp/REMAINING_PHASES_EXECUTION_PLAN.md — detailed implementation sequence.
- docs/ui-revamp/REFERENCE_DESIGN.md and playground-concept.png — owner design target.
- docs/ui-revamp/REVAMP_TRACKER.md and IMPLEMENTATION_LOG.md.
- The two independent review reports and their implementation dispositions.

The completion plan may currently be at:
/home/prem/dsa-doc-cleanup/docs/ui-revamp/REMAINING_PHASES_EXECUTION_PLAN.md
Read that file if it is absent from your checkout. Carry the active planning files into
your working branch as needed; preserve the accompanying obsolete-document cleanup.

Inspect current Git state, fetch main and preserve unrelated/untracked work. The
Windows-mounted checkout may be on an old handoff branch. Use a clean working branch
or worktree based on current main; do not implement against the old checkout.

P0–P4 implementations and the review fixes are already merged. Latest inspected main
was dad2640 (#191); fetch again. Do not rebuild completed features. Refresh verification
and reconcile acceptance gaps, then execute the plan's medium-sized change sets:
1. P5: switcher/modal ownership, Focus mode, shortcuts and adapted guidance.
2. P6: bounded all-family history, truthful comparison and completion flows.
3. P7: every renderer and all seven input contracts, including real phone journeys.
4. P8: visual/responsive/accessibility fixes, measured performance and learner tasks.
5. P9: reachability cleanup, complete documentation/evidence and release handoff.

Preserve every existing feature, storage key, URL contract, approved visual direction,
statement, syntax color and real tracer behavior. Keep one session/playback owner and
one primary diagram. View, Focus, theme and layout changes must not execute again.
Protect drafts, committed input, step, anchors, source position/follow and shared links.
Never fabricate data, constraints, complexity, container semantics or completion.
Missing runs and offline samples must remain unmistakable. Preserve all audit regressions.

Use small-to-medium coherent changes, not one enormous rewrite. Reuse current hooks,
components, tokens, search and comparison. Avoid new dependencies and unrelated backend
work. A demonstrated backend contract defect needs its own scoped, tested change.

Reproduce defects and prove meaningful regression tests fail before fixing them. Use
real-browser verification for focus, native scrolling, drag, geometry and responsive
behavior. Test both themes, required widths, zoom, reduced motion, long/boundary inputs,
navigation races, failures and denied storage. Use real backend journeys for acceptance;
label fixtures honestly. Run the full frontend/backend/build/startup checks as required.

Update the existing tracker/log after each package. Do not weaken tests, invent evidence,
silently skip gates or mark a phase done because its controls exist. Reconcile the y389
stage-position gap with measurements; preserve readable content and useful diagram area.
Unavailable screen-reader/user/second-engine checks remain explicit pending limitations.
Continue independent implementation even if a human-only check is unavailable.

Publishing follows my authorization and the repository rules. If I have authorized
commits/pushes/PRs/merges for this run, publish consecutive medium-sized PRs, wait for all
required checks, merge without bypasses, then continue from merged main. Otherwise
finish the implementation and verification locally and report the concrete changes.

At completion report P0–P9 status, F01–F35 and renderer/input coverage, changes/PRs/commits,
test and browser evidence, performance/visual results, pending human decisions, publication
state and rollback instructions. Do not claim the full revamp is complete while a gate
remains unresolved. Implement now; planning alone does not satisfy this request.
```
