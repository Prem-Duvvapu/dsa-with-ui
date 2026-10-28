# Paired input contract verification

2026-10-28 · base `03abc797c5cec4f0bee1aaf7dfc1830a1133f416` (Count Squares #237)
· branch `fix/paired-input-contract`.

## Scope and behavior

Randomize previously sampled weights and values independently. Recorded RNG draws
`[.125,.1,.1,.25,.3,.4,.5,0]` produced weights `[3,3]`, values `[31,41,51]`, capacity1:
each field was valid but the run could not accept their different lengths.

Three canonical owners and four capacity variants now declare their existing pairing:
Knapsack01, Unbounded Knapsack and Fractional Knapsack; the first two's recursion and
memoization forms. `InputSpec.equalLengthGroups` is an immutable list of disjoint INT_ARRAY
field-name groups. It is omitted when empty, retained by budget copies and checked at
construction for names/types/overlap/exact numeric bounds/common length intersection.

Shared validation checks normalized sizes after existing field validation and defaults.
Error keys stay values for the DP owners and weights for Fractional Knapsack. Old duplicate
run-local checks are removed; field, table, call and global budgets remain unchanged.
The generator samples one common length, then each field's own values in declaration order.
It does not infer relationships from IDs or prose, mutate metadata, resize a user's draft,
sort paired user input, substitute defaults or execute a run.

Only the two relevant inventory rows change: relation metadata in two canonical and four
published alternative specs. Source digests, fields/defaults/bounds, algorithms and DP
statuses stay unchanged. DP remains23/56 verified and merged,33 outside verification.

## Tests before and after the fix

- Backend baseline:10 cases,7 failures/3 controls,0 errors. Actual owner wire metadata
  and the shared validator fail independently of the absent builder API. Reflection kept
  that initial run compilable; final tests call the new API directly.
- Generator baseline:68 cases,29 failures/39 controls. After the fix68/68 pass, including
  the original37 cases unchanged. New cases cover many seeds, reversed declaration order,
  three-member/disjoint groups, raw value/index order, immutable metadata, empty-only and
  minimum-above10 domains, malformed declarations and unrelated deterministic behavior.
- Actual controlled InputPanel:2/2 pass. Unequal manually entered data is preserved through
  editing/submission; Randomize changes only the visible draft until explicit Run.
  An ignore-groups mutation fails the intended test (1 fail/1 control). Generator bytes
  are restored before the final2/2 pass and complete frontend suite.
- Independent review finds raw length metadata could be narrowed/ignored by intConstraint.
  The added case fails before fixing this. Group construction now uses exact nonnegative
  32-bit conversion, rejecting strings, fractions, overflow, NaN/infinity and sub-double
  precision fractions while retaining whole numeric forms such as3L/3.0.
- Focused backend192 cases pass before that final narrow schema hardening. Existing
  InputValidator, capacity, table-budget and answer regression assertions are retained.

## Final gates

| Gate | Final observed result |
|---|---|
| Full backend |8,343 cases,0 failures/errors,500 unchanged skips |
| Normal frontend |948 tests/88 files,0 skips,146.95 seconds |
| Production build / startup cleanup |Pass |
| Golden regeneration |432 cases pass; no fixture changes |
| Packaged HTTP |48 execution requests:28 successes and20 refusals |
| Browser journeys |8 main rows across Chromium/Firefox,320/1366px,both themes |
| Browser executions |20 total, including2 manual-refusal/recovery sequences |
| Short summary visibility |4 independent Chromium measurement rows pass; all8 journey rows also check actual visibility |
| Visual review |8 final viewport screenshots inspected |

`packaged-identity.json` records frozen JAR SHA-256
`d3f2d98ca2e688ecb5db312c902978174d000f5acdfc8d35b6639851101afe22` and byte comparison
of all615 compiled production classes. No Maven command runs while that JAR serves the
API/browser phases. API evidence remains valid across the later frontend-only layout fix
because the backend binary is unchanged. Final browser manifests identify actual served
`index-DGacmtMX.js` and `index-DL2roZqZ.css`; every final manifest agrees on the JAR hash.

`api-results.json` covers7 specs ×2 small fixtures ×full/delta,14 pairing refusals and6
existing budget controls. Independent subset enumeration, quantity vectors and fractional
polytope vertices check answers; final canonical tables and original item indices are
checked too. The largest full/delta responses are87,098/50,770 bytes. Source, metadata,
submitted pair order and decoded full/delta steps match; refusals contain no steps.

The browser uses the real Randomize button with RNG0 restored immediately afterward.
Actual draft values become weights[1],values[1],capacity1 while the prior run/step/echo/URL
and execution count stay unchanged until explicit Run. Run submits that exact map,
finishes with answer1, focuses the stage and updates sharing. Source/highlight and view
continuity are checked without reexecution. Phone-dark in each engine additionally submits
manual unequal weights[1,1]/values[1], checks400 summary focus and exact preserved draft,
prior run/source/step/echo/URL, then verifies draft-only Randomize and successful recovery.
All8 rows have no page overflow/runtime errors and fully visible short input values.

Catalogue stats remain431 catalogued/traced,0 duplicates/orphans. Actual specs match the
two canonical and four alternative inventory snapshots. Counts/statuses remain unchanged.

## Visual finding and final-artifact policy

The first complete browser phases pass their behavior assertions but visual review finds
that a320px input echo hides all three short values. Title/DOM assertions and no-page-overflow
checks did not notice. `summary-red.json` records both phone themes:0px capacity value
box for9px text;5.19/7.16px weights/values boxes for27px text. Desktop controls fit.

Two comfortable-summary CSS rules wrap entries and bound their width. Long values keep
their own ellipsis/title; compact legacy styling and all input data remain unchanged.
The full normal frontend suite/build and both complete engine journeys repeat after this
fix. The strengthened journey checks real short text ranges against value boxes and
clipping ancestors. `summary-visibility.cjs` is a reusable independent measurement.
Initial browser screenshots/manifests and first frontend build are archived outside
the repository and excluded from final visual/artifact evidence. RCA-074 records the defect.

`summary-green.json` measures full9px capacity text and27px array values in both phone
themes, matching their text ranges. Desktop controls remain readable. One first GREEN
probe starts before the restarted backend is ready: Vite records connection refusal at
19:16:41Z and the backend starts accepting at19:16:44Z. That timeout is excluded, not a
product pass. Readiness is checked before the unchanged measurement repeats successfully;
the reusable probe now fails early when the API is unavailable. No service restart,
product change, worker or timeout adjustment is used to hide that attempt.

## Remaining work

Equal lengths do not imply that a generated DP input fits its animation budget. Table
area225, memo area80 and recursion220-call guards remain mandatory and can still refuse
a paired draft. Their generator contracts are a subsequent package, not silently replaced
by tighter per-field bounds. Meeting ordering/platform relations and other owners remain
outside this package. No dependency, endpoint, timeout, test worker or global cap changes.

Bounded browser/API evidence does not certify all inputs, source compilation, public
deployment, real devices, screen readers, performance, learner acceptance or release/rollback.
