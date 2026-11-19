# Coin/subset generated-input verification

2026-11-19 · base `a5c31d7e7535e3aced28cae997c2b6c023d7ccf2` (#239)
· branch `fix/coin-subset-work-limits`.

## Scope

Minimum Coins and Coin Change II now declare their existing225-cell canonical,
80-cell memo and220-call recursion limits. Subset Sum Equal Target and Count Subsets
With Sum K declare their existing225/80-cell canonical/memo limits. The existing
InputWorkLimit kinds represent these exact shapes; no new schema kind is added.
The two coin recurrences share the existing positive-value reusable skip/take base
cases. Shared validation replaces only equivalent duplicate guards, and the unused
CoinTraceBounds helper is removed.

Subset recursion remains limited by its original six-item cap. Subset feasibility
continues to accept zero values; subset counting remains positive-valued and preserves
indexed duplicates. Derived partition targets/parity/impossibility guards are untouched.
Original domains, defaults, alternate inputs, source algorithms, accepted trace events
and global5000-step/2MB limits remain intact. Candidate rollout status does not advance.

The generic INT_ARRAY/LINKED_LIST distinct sampler now draws without replacement,
using a sparse partial shuffle with O(selected length) storage. A repeated RNG value
cannot cause the old bounded-retry duplicate fallthrough. Sample lengths intersect
numeric cardinality and paired length constraints. Impossible/malformed sample metadata
fails explicitly before drawing; submitted drafts and server domains are never repaired.
Sparse binary-tree generation and grid/graph relations are separate paths.

## Regression-first evidence

- Backend:41 cases,20 failures/0 errors/21 controls on the base implementation. All10
  served metadata checks and all10 shared-validator refusal checks expose the missing
  declarations. Existing HTTP boundaries/answers, field domains and zero/parity controls
  pass before the change.
- Distinct sampler: initial21 cases produce19 failures/2 controls. Targeted subsequent
  RED checks cover empty-only and omitted-maximum length fallback semantics.
- Coin/subset fixture tests exercise existing generic TABLE/CALLS support. They are
  controls, not falsely described as evidence that metadata already reached the API.
  Actual served declarations are independently verified at backend and packaged HTTP seams.

## Verification status

Focused backend111 cases and frontend158 cases pass. The clean full backend run passes
8,392 cases with0 failures/errors and500 unchanged skips. The Vite build passes with
1,588 modules: JS395.86kB/126.21gzip and CSS99.09kB/17.96gzip. The frozen assets are
`index-CAbCzZ8l.js` and `index-DL2roZqZ.css`.

Two frontend full attempts are not counted as passes: the preliminary run has1 deliberately
RED omitted-maximum regression plus2 unchanged integration failures; the next run has
2 unchanged Sharing readiness failures. Those scenarios pass unchanged in focused runs,
including all7 Sharing cases. Logs retain both failed runs. Workers, test assertions,
timeouts and product lifecycle code are unchanged. After Maven finishes, the final normal
`npx vitest run` passes1,038/1,038 cases across91 files in246.04 seconds, with0 skips.
All previously failing integration scenarios pass unchanged in that full run. Host load
was elevated in earlier attempts, but that alone is not proof of their cause.

All432 GoldenTraceTest cases pass; all431 fixture files remain byte-identical. Package
and ephemeral-port startup checks pass. Independent freeze compares all617 production
class files and all93 Java solution resources byte-for-byte with the JAR. The retired
CoinTraceBounds class is absent. `packaged-identity.json` records JAR SHA-256
`e244fc2a8c3ab7fa8e1edb64bd776fa3e64a07c894d08bc42ea823be7bb9af25` and the final sampler
hash. No Maven command runs while this JAR serves live evidence.

The real API probe completes124 executions:50 accepted cases in both full/delta
encodings,20 boundary refusals, and4 parity-order control requests. Thirty malformed
frontend metadata controls fail before RNG; unchanged subset recursion specs are also
checked. Three actual sampler profiles, semantic inputs and exact accepted boundaries
exercise all10 served declarations. Independent BFS, quantity-vector and indexed-subset
enumeration verify answers; an independent recursive oracle verifies call work. All
accepted runs finish untruncated and decoded encodings agree. Largest measured full/delta
responses are1,566,704/1,531,008 bytes. The API's direct PID launcher did not capture an
exit code: final asserted manifest plus terminal process observation is the recorded proof;
no captured exit0 is claimed.

Chromium and Firefox each pass20 journeys (both wrappers capture exit0):10 forms ×
phone320 dark/desktop1366 light. Actual high-RNG editor controls produce runnable drafts,
including eight distinct Coin Change II denominations. Randomize changes only the draft;
explicit Run preserves actual input order, completes, focuses the stage, updates echo/share
state, and keeps the source/highlight/step through view and candidate-approach changes.
Each engine also checks a manual table overflow400, prior run/step/URL retention, focused
error summary and Randomize/Run recovery. No page-wide overflow or runtime error is seen.
All8 selected viewport screenshots are visually inspected; short amounts/targets remain
readable and long arrays retain bounded ellipses. The browsers observe124 execution
requests in total; this is a request count, not124 independently validated responses.

All API/browser manifests agree on the frozen JAR/sampler; both engines serve the frozen
build assets. All10 actual served specs match their inventory snapshots. Only the10
workLimits additions change that inventory; all56 statuses/digests remain unchanged.
Stats remain431/431 with0 duplicates/orphans. The owned backend/preview processes are
stopped after verification, with8923/5180 free. See `test-summary.json`,
`packaged-identity.json`, `api-results.json`, engine manifests, `catalog-inventory.json`
and `cleanup.json`. Publication is governed by exact-head CI; this local record is not
an exact public deployment certificate.

## Limits

This package covers ten existing direct-budget contracts and numeric distinct sampling.
It does not certify all generated catalogue inputs, derived-target budgets, source
completeness, public deployment, real devices, screen readers, learner/performance
acceptance or release/rollback. No dependency, endpoint, worker, timeout or global cap
is introduced.
