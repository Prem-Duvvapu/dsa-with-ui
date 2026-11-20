# Longest Palindromic Subsequence — verification record

2026-11-19–20 · base `dc1c04a` (#241) · `feat/lps-solution-approaches`.

## Scope

Recursion and nullable memoization now evaluate this owner's existing prefix LCS of
`text` and its fixed reversal. Coordinates are prefix lengths, not interval endpoints.
All three forms return a length; they do not reconstruct a palindromic sequence.
The canonical implementation, complete source, input spec, examples and trace fixtures
are preservation controls. Its previously generic complexity metadata is corrected to
O(N²) time and auxiliary space for the full square table, with reversed-text storage,
scalar output and excluded animation snapshots stated separately.

Recursion accepts 1–5 lowercase letters, memoization 1–8, canonical 1–12. Every form
keeps `bbbab` / `cbbd` as its default / alternate. No dependency, problem ID, endpoint,
frontend production code, renderer policy or global step/byte budget changes.

## Regression and independent checks

- Missing three-form registration and incorrect canonical complexity each fail before
  production changes: 2/2 intended RED results. Nine focused backend tests then pass.
- Tests compile each exact displayed source, compare real executions with a direct
  palindrome-subsequence oracle, check actual child order, stack, anchors, counters,
  nullable-zero caching, HTTP source/spec/complexity seams, invalid inputs and honest
  truncation under tiny step/byte budgets.
- A separate source check uses an independent interval-LPS oracle over the original
  text, rather than prefix LCS against reversal: 6,282 cases for recursion, 6,382 each
  for memoization and canonical. This includes all equality patterns through length 8,
  ternary strings through 6, empty public-source input and seeded controls.
- `bounds/` preserves reproducible finite-domain arithmetic, explicitly distinguished
  from runtime and wire measurements. Equality patterns exhaust the branch shapes under
  symbol renaming. Recursion's length-5 maximum is 155 calls / 552 events; memoization's
  length-8 maximum is 107 calls / 383 events. Focused runtime checks verify the witnesses;
  a different dense witness computes 76 states. Unvisited cells remain unknown.

## Completed build gates

Clean backend: 8,407 cases, zero failures/errors and 500 unchanged skips. All 432 golden
checks pass; all 431 fixture files and the five preserved canonical/shared source controls
remain byte-identical. Package and ephemeral-port startup pass. Every one of the 1,110
packaged files (621 classes, 489 resources) equals its tested counterpart. Frozen JAR:
`16ae9ad09fca6276e291d48eb363fe9df588bdf078671b63b09f18e6a4b38435`.

The normal full frontend rerun passes all 1,056 tests across 93 files; Vite build passes.
No frontend production or unit file changes. Assets are `index-BW7qIb8i.js` and
`index-DL2roZqZ.css`, with exact hashes in `packaged-identity.json`. The verification
crosses a long wall-clock gap; raw elapsed log times are not performance evidence.
No test assertion, worker configuration or timeout changes.

## API gates

All 24 standard scenarios pass in full and delta encodings (48 requests). Nine profiles
of the actual frontend sampler add 18 requests. All 66 requests agree on independent
answers, known intermediate prefix values, source, identity and decoded encodings;
accepted runs finish untruncated. Maximum measured full/delta responses are
1,236,792 / 801,690 bytes, below the unchanged 2 MB cap. Both wrappers capture exit 0.

## Browser and reconciliation gates

All 52 browser journeys pass: 34 standard/stress/native-zoom rows, four comparisons,
eight recovery rows, four navigation rows and two bounded prior-LCS compatibility rows.
Chromium covers 320/390/768/1366/1440 widths and native 200% zoom; Firefox and both
light/dark themes are covered. Recovery checks real Randomize, retained over-limit text,
400 focus/prior-run/link preservation and explicit-example recovery (40 requests).
Navigation adds 12 contexts and 64 actual executions, including dirty-draft canonical{}
default reset, empty-map defaults in both directions, Back/Forward, late metadata and
edits, unchanged held actual response bodies and rejected links. Independent review
found the original reverse-default claim overstated; two new tests fail before the
strengthened guard, then all 143 Node evidence tests pass and the actual scenario runs.

All 12 selected screenshots are viewed directly and their hashes recorded. Phone trees
and long source lines scroll within their local panes; every actual node is rendered
and the root reachable, rather than all nodes fitting on a phone simultaneously.
Desktop source captures can scroll past the graph root; those images certify source
readability, while maximum-tree render/reachability checks are separate. Input echo,
dirty draft, focus rings and active source lines are readable. Automated journeys see
no whole-page overflow or runtime errors. All 33 historical LCS files remain exact.

Final audit captures exit 0, checking current sources, post-JAR timestamps, mandatory
sampler/recovery/navigation proofs and matching assets. Only LPS's alternative
status/specs/safety and corrected canonical complexity change in the inventory; its
canonical digest/spec and all other rows stay exact. The count is 25 locally verified,
30 planned and one different formulation. Owned services are stopped; see cleanup.json.
The refreshed ledger's backend contract passes (one case, zero failures/errors/skips).
Exact-head CI gates publication; GitHub records the final merge outcome. Final staged diff checking
removes only extra trailing LF bytes from four new probe/validator files; runtime hashes
are preserved and mapped to the committed hashes in probe-whitespace-provenance.json.
No application or artifact bytes change, and all 143 Node guards pass after cleanup.

The first frontend run passes 1,055 cases and times out in the unchanged 30-second
production-reachability build check during shared-host contention. Its logs/status are
retained externally, without changing workers, timeouts or assertions. The isolated
9-case reachability file passes under the original settings. The complete rerun and build
subsequently pass as recorded above; the failed attempt is excluded from final proof.

This package does not certify all inputs, exact public binary identity, real devices,
human screen readers or learners, broad performance, or final project release/rollback.
DP D6/D7 and those broader acceptance gates remain open.
