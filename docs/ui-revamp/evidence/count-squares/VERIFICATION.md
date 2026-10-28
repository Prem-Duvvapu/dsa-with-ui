# Count Squares explanation and metadata verification

Base: `eae15a4b1ac90d32926d7372c5e7103ef281d2dc` (Phone #236).
Branch: `fix/count-squares-explanation`, 2026-10-28.

## Defect and scope

For a 2×2 all-ones input, the last cell adds two new squares: its own 1×1 and the
2×2 ending there. The previous explanation incorrectly said the smaller square had
already been counted. The count narration now describes one new square of each side
length from 1 through the cell's value. The recurrence and actual trace data are unchanged.

The owner metadata now describes O(R*C) time for R rows and C columns and O(1)
algorithm auxiliary space for in-place tabulation. It separately discloses O(R*C)
input-conversion storage and O(1) scalar output; animation snapshots are excluded.
Catalogue, detail, execution and canonical approach metadata are checked at their
actual HTTP seams. The inventory's canonical metadata is reconciled without changing
its source digest, input contract or alternative status.

This package does not add recursion/memoization variants or certify the remaining DP
owners. DP rollout remains 23/56 verified and merged, with 33 outside verification.

## Regression evidence

The corrected pre-fix run has 8 cases: 3 intended failures, 5 passing controls, no errors.
An earlier test-authoring run also compared Jackson LongNode and IntNode representations
of the same wire value; normalizing both through serialized JSON removes that unrelated
failure. No production code had changed at the corrected RED checkpoint.

After the fix, all 8 cases and the inventory guard pass (9 total). Direct square enumeration
uses top-left positions and side lengths, independently of the DP recurrence. It checks
all 16 binary 2×2 matrices, mixed/zero/default cases, a 3×3 all-ones case and the 8×8
maximum (204 squares). Snapshot assertions protect before/after assignment timing.
A three-step policy still produces an honest partial run without a completed answer.

The exact displayed Java compiles in an isolated Java 17 classloader with only an outer
class when needed. Default/custom/maximum answers, authored examples, source/anchor
identity, full/delta equivalence and field-specific refusals are checked independently.

All 432 golden cases regenerate successfully. Only Count Squares changes: 10 descriptions,
with its source, anchors, default input, all 18 events, grids, variables and answers identical.
See `golden-comparison.json` for each old/new sentence and the structural comparison.

## Final verification

| Gate | Observed result |
|---|---|
| Full backend |8,332 cases,0 failures/errors,500 unchanged skips |
| Golden regeneration |432 cases pass; only10 descriptions change |
| Frontend production build |Pass; unchanged `index-BfO8d-Ww.js` / `index-D4rQyCzb.css` |
| Startup cleanup |Pass |
| Packaged API |14 execution requests pass:6 cases ×full/delta, plus invalid-binary400 ×both |
| Chromium / Firefox |4 journeys each:320/1366px ×both themes |
| Visual review |8 viewport screenshots, narration and complexity in selected engine/theme/width combinations |

The frontend source tree is unchanged from #236, whose local normal run passed915 tests
in86 files. That prior run is not a newly executed local suite; publication requires fresh
exact-head frontend/backend CI. No dependencies, test workers or timeouts changed.

`packaged-identity.json` records SHA-256
`f1f490c98fcb166bc8a41c664b18715034cec2db25887b5e5d51ac8e9a7e3acc` and byte comparison
of all615 compiled production classes. No Maven command ran while this JAR served API
and browser evidence. Every final manifest records that same JAR and actual served assets.

`api-results.json` checks independent ending-square contributions, every compute/count
snapshot and narration, final answers, source/metadata/input/anchor identity and decoded
full/delta equivalence. Default15/18 steps, alternate5/9,2×2 ones5/7, mixed3/6,
zero0/2 and maximum204/115 all complete. Largest measured full response:59,239 bytes.
Catalogue stats remain431 catalogued/traced,0 duplicates/orphans; the inventory owner
spec/complexity equals the actual detail response.

`chromium-results.json` and `firefox-results.json` check the real shared2×2 input,
last-cell count at step6/7, displayed matrix `[[1,1],[1,2]]`, two newly counted squares,
accessible grid description, exact source/highlight and visible Analysis metadata.
View changes preserve the step without another execution; no page overflow or runtime
errors are observed. Screenshot metadata records viewport, scroll and target positions.

## Exclusions and limits

An initial golden-comparison script used a repository-root path from the backend directory
and failed before its assertions. The corrected comparison runs from the root and passes.

Initial complete browser phases saved full-page screenshots. At nonzero scroll positions,
stitched captures misleadingly showed the otherwise offscreen fixed skip link over content.
A normal-viewport probe confirmed it was unfocused at y−66 through−16; no product CSS was
changed. Those full-page artifacts are archived outside the repository and excluded from
final visual evidence. Both complete phases repeat with viewport captures scrolled to the
actual narration/space explanation; only these final captures are retained.

This is bounded local verification, not all-input, public-browser, real-device, native-zoom,
screen-reader, performance, release/rollback or whole-project certification. Phone's
`phone-public-oct28.json` separately records bounded deployed API checks after #236;
it is not an exact deployed-binary proof or analytics collection confirmation.
