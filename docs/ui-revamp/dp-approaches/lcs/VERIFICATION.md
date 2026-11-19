# LCS solution approaches — verification record

2026-11-19 · base `f3490b5` (#240) · branch `feat/lcs-solution-approaches`.

## Scope

Real prefix recursion and nullable memoization complement the unchanged canonical
tabulation. All forms use the same LCS-length recurrence, rather than reconstruction.
Recursion accepts 1–4 lowercase characters per string; memoization 1–8; canonical 1–12.
Recursion owns the smaller `abcd`/`acd` example; memo keeps `abcde`/`ace`. Source,
complexity, teaching and input limits belong to each executable. No dependency, problem
ID, endpoint, global budget or canonical trace changes.

The session fix seeds a restored draft from the requested definition. An unresolved
explicit approach does not borrow canonical summary defaults/bounds, and a default-reset
link clears old draft ownership even before that definition arrives. Later user edits
remain owned; responses never rewrite them. Linked POST bodies remain exact.

## Regression and source proof

- Registration fails 1/1 before the new provider; focused backend 40 cases then pass.
- Initial differing-default session cases fail 6/9 before the first fix. Independent
  review finds the real-summary/late-definition variant that the initial fixture omitted.
  Corrected permanent fixtures fail 4/14 before the additional guard/reset fix; focused
  63 cases then pass. Both earlier failures also pass five independent review probes.
- The exact displayed Java is compiled/invoked independently. A second source reviewer
  checks 739 cases per alternative using a different subsequence oracle, including public
  empty-string bases. App validation retains its original nonempty domain.
- Maximum recursion is 139 actual calls/488 events; maximum memoization 129 calls/453 events,
  49 cache hits and 80 computed states. Unknown memo cells remain distinct from cached zero.

## Completed build and API gates

Clean backend: 8,398 cases, 0 failures/errors, 500 unchanged skips. All 432 golden checks pass;
all 431 fixture files and the canonical owner/source remain byte-identical. Package and
ephemeral-port startup pass. All 1,106 packaged files (619 classes/487 resources) match
the tested build. Frozen JAR SHA-256:
`63bde9ec02cf9ef5f0f560533d2cd4e81eddbdc602de63d34283946a53688a59`.

After the independent review correction, the normal frontend suite passes 1,056 tests
across 93 files; Vite build passes. Earlier 1,052-test proof is retained externally as
preliminary and does not certify revised source. Final assets are
`index-BW7qIb8i.js`/`index-DL2roZqZ.css`; source hashes stay unchanged across the accepted
run. The old preview is stopped before rebuilding. No Maven runs during live evidence.

The standard API probe passes 23 actual scenarios in both full/delta encodings (46
requests); the real frontend sampler adds 9 profiles / 18 requests. Both wrappers capture
exit 0. Independent answers, source identity and decoded encodings agree; accepted runs
finish untruncated. Maximum measured full response sizes: 265,432 bytes recursion,
1,465,450 memoization and 829,420 canonical. Memo maximum delta is 860,046 bytes.

All 86 Node evidence-contract tests pass and now run in CI. Missing/mismatched sampler,
source, artifact or recovery proofs refuse LCS verification in the inventory.

## Browser and reconciliation gates

All 54 browser journeys pass: 34 standard/stress/native-zoom rows, four comparisons,
eight input-recovery rows, four independent navigation rows and four bounded legacy
coin-family compatibility rows. Both engines and themes are covered; the Chromium matrix
includes 320/390/768/1366/1440 widths and native 200% zoom. Navigation adds 12 contexts and
56 real upstream requests, including actual held response bodies and late metadata.
Responses are fulfilled unchanged; no fabricated trace or substituted response is used.

All 12 selected viewport screenshots are inspected. Large maximum trees scroll locally;
every actual call is rendered and the root is reachable, rather than 139 nodes fitting
simultaneously on a phone. String fields, input echoes, focus rings and source highlights
remain readable; long source lines scroll within the pane. The reviewed navigation PNG
hashes match their recorded visual-review hashes. No page-wide overflow or runtime errors
are observed. Historical coin-family evidence remains byte-identical.

The final metadata-only audit captures exit 0 and confirms current served sources,
matching assets, post-package manifests and all mandatory sampler/recovery/navigation
proofs. Only LCS's inventory status, safety notes and published alternative specs change;
its canonical metadata/source remain exact. The inventory has 24 locally verified
candidates, 31 planned and one requiring a different formulation. The refreshed ledger's
backend contract check passes. Publication requires exact-head CI. Owned services are
stopped and ports 8923/5180 are free; see `cleanup.json`.

This is bounded automated verification, not every input, public binary identity, real
devices, a human screen reader or learner review, broad performance acceptance, or final
project release/rollback. DP D6/D7 and those broader gates remain open.
