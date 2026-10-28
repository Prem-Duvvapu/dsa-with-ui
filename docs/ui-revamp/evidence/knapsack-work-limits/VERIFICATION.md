# Knapsack work-limit contract verification

2026-10-28 · base `6548b87a396b072688e061f3e9ef581346d54c0a` (pairing #238)
· branch `fix/knapsack-work-limits`.

## Scope

The current Knapsack01 and Unbounded Knapsack forms already refuse tables above225 cells
(canonical),80 cells (memoization), or uncached trees above220 calls (recursion). Pairing
alone could still produce a generated draft refused by those guards.

All six specs now declare their existing limits as optional immutable workLimits. The
two supported kinds have fixed meanings: CAPACITY_TABLE_CELLS counts the base-inclusive
table; SKIP_TAKE_CALLS counts actual uncached calls with explicit reuse. Validated groups
and limits survive builder ordering and both budget-copy methods. Empty metadata is
omitted for other owners. Known field/type/range/message checks reject malformed metadata.

InputValidator checks normalized work shape after fields and pairing. Old duplicate
owner preflights are removed; other families' DpTraceSupport guards remain. A bounded
iterative counter visits at most221 nodes for the current recursion policy and uses
long arithmetic for exact table products/sentinel values. No answer or trace is invented.

Randomize generates ordered pairs first and defers the affected scalar, including when
capacity appears first in the field list. It samples capacity directly within its original
range intersected with the feasible range; no retry, user-draft repair, defaults substitution
or ID/help-text heuristic is used. Fixed positive-weight call counts are nondecreasing in
capacity. At capacity1 all allowed lengths up to8 fit every current limit, so all six
current declarations have a feasible capacity for every generated weight array.

Per-field domains/defaults/alternates, measured limits, global5000-step/2MB budgets,
displayed algorithms, source digests and accepted trace events remain unchanged. Eight
weight20 items at capacity30 stay accepted by recursion (45/53 calls) even though their
279-cell table is refused in canonical/memo forms. Only two canonical/four alternative
inventory specs gain workLimits; DP remains23/56 verified and merged,33 outside verification.

## Regression-first checks

- Backend baseline8 cases:7 failures/1 control,0 errors. Actual owner-wire and shared
  validator tests independently expose missing behavior while baseline helpers keep the
  new schema test compilable. Final tests use direct typed APIs.
- Focused final101 backend cases pass, retaining InputValidator, Inputs, capacity and
  table-budget tests. Independent enumeration checks uncached call counts, exact220/221
  boundaries, table225/234 and80/81, order-sensitive weights, saturation, long products,
  huge-capacity bounded work, metadata/copy/HTTP seams and honest tiny-policy truncation.
- Independent review catches a missing declared minLength check in the new builder.
  Its targeted regression fails1/1 against that check's absence, then passes after the
  narrow alignment with the frontend schema. This does not change actual owner bounds.
- Generator baseline112 cases:37 failures/75 controls. Afterward112/112 pass, including
  the prior68 cases unchanged. The44 new cases include5,400 seeded six-spec-like draws,
  independent recursive oracles, exact feasible maxima, order-sensitive calls, cheap
  full-domain recursion, scalar-first order, immutable metadata and pre-RNG schema errors.
- One prior pairing assertion changes because it expected the shared validator to allow
  a279-cell map until run(). It now requires a capacity validation error at the earlier
  boundary, retaining run refusal, cheap recursion acceptance and every other guard.
  Per-field domain checks still prove that no validation maximum was lowered.
- All432 golden cases regenerate with no fixture changes.

## Final gates

| Gate | Observed result |
|---|---|
| Full backend |8,351 cases,0 failures/errors,500 unchanged skips |
| Normal frontend |992 tests/89 files,0 skips,137.67 seconds |
| Corrected generator matrix |112/112 targeted cases pass after the fixture correction described below |
| Vite build / launcher cleanup |Pass |
| Golden regeneration |432 cases; no fixture changes |
| Packaged HTTP |60 executions, including generated drafts and accepted/refused boundaries |
| Browser |24 journeys,72 actual executions across both engines and all6 forms |
| Visual review |8 selected viewport screenshots inspected |

The full frontend run uses the final production code. Review afterward notices that its
seeded fixture matrix labels memoization but uses225, and labels single-use recursion
but uses80. Those fixtures are corrected to the actual80/220 policies; the corrected112
generator cases pass. No production change is required. Fresh exact-head full CI is a
publication gate and must pass before merge; this local run is not misrepresented as
occurring after that test-only correction.

`packaged-identity.json` records SHA-256
`046641c3f2cddf8f4b2e805fa4da8929ba87f1924cada332b30e015d002934b3` and byte comparison
of all618 compiled production classes. No Maven command runs while this JAR serves API
and browser phases. All final manifests agree on that JAR; browser manifests also identify
actual served `index-DnePTFKv.js` / `index-DL2roZqZ.css` assets. No services are left running.

`api-results.json` fetches all6 actual specs and imports the actual frontend generator.
Three profiles per form (seeded/high/low-weight) run in full/delta, plus6 accepted and6
refused boundary cases in both encodings:60 executions total. Eighteen malformed frontend
metadata controls reject before RNG. Independent subset/quantity enumeration checks
answers; a separate bounded call oracle checks work, source/input/approach/pair identity
and decoded encoding equivalence. Largest measured full/delta responses are1,580,966 /
1,545,626 bytes, under the unchanged2MB cap; successful cases finish untruncated.

`chromium-results.json` and `firefox-results.json` contain12 rows each:6 forms ×phone320
dark/desktop1366 light. Real high-RNG Randomize keeps8 weights20 and8 values100, choosing
capacity24 canonical,7 memo,30 recursion. It changes only the draft until explicit Run;
the prior step/echo/URL/execution count remain. Actual successful requests preserve those
values and pair order, focus the stage, update echo/sharing and keep source/highlight/step
through view changes without reexecution. Capacity text is measured against clipping
ancestors; no page overflow/runtime errors are observed. Selected screenshots show both
the draft and completed run. Long arrays keep bounded ellipses on phones.

Catalogue stats remain431 catalogued/traced with0 duplicates/orphans, and all6 inventory
spec snapshots match the actual API. Candidate statuses/counts and source digests are unchanged.

## Limits

This package concerns six existing Knapsack DP contracts. Fractional Knapsack retains
pairing only. Coins, subset/partition derived-target budgets, meeting ordering and other
catalogue constraints are not inferred or certified. Generic future call-limit declarations
with no feasible generated capacity produce an explicit error rather than a repaired input;
this sampler is certified here only for the current six declarations.

Bounded verification does not certify all catalogue algorithms/sources, every input,
public deployment, real devices, screen readers, learner/performance acceptance or release
rollback. No new dependency, endpoint, worker, timeout or global cap is introduced.
