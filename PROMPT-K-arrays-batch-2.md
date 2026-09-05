# PROMPT-K — Arrays, second pass (batch 4 of the full roadmap)

Status: plan only, no implementation yet. This is batch 4 of `PROMPT-J-full-roadmap.md`.

## Scope

| Difficulty | Id | Title |
|---|---|---|
| Hard | `repeating-missing-number` | Find Repeating and Missing Number |
| Hard | `merge-two-sorted-arrays` | Merge Two Sorted Arrays Without Extra Space |
| Medium | `three-sum` | 3 Sum (Triplets with Sum 0) |
| Medium | `four-sum` | 4 Sum (Quadruplets with Target Sum) |

All four ids live in `ArrayService.java` (the legacy `service/` package, not a tracer),
registered with catalogue `dsType` wire value `"Array"` already (confirmed by reading each
`ProblemDetail` constructor call directly — no dsType metadata fix needed, unlike the
DP/Tree batches earlier this project). `DsType.STRING`/`INT_ARRAY` both already map to the
existing `ArrayCanvas`, so this batch carries no canvas risk.

## Current bug: hardcoded narration, not fallback delegation

Unlike the classic "delegate to another problem's generator" bug, all four of these have a
**real, correct algorithm** implemented in their generator method — but it always runs on
one hardcoded array baked into the method body, ignores any caller input entirely, and
returns a canned step list every time. Confirmed by reading all four generator bodies
directly:

- `generateThreeSumSteps()` (`ArrayService.java:2012`) — real sort + two-pointer 3Sum,
  hardcoded on `{-1, 0, 1, 2, -1, -4}`.
- `generateFourSumSteps()` (`ArrayService.java:2074`) — real sort + nested-loop + two-pointer
  4Sum, hardcoded on `{1, 0, -1, 0, -2, 2}`, target `0`.
- `generateMergeTwoSortedArraysSteps()` (`ArrayService.java:2261`) — real gap-method
  (Shell-sort-derived) in-place merge, hardcoded on `arr1=[1,3,5,7]`, `arr2=[0,2,6,8]`.
- `generateRepeatingMissingSteps()` (`ArrayService.java:2303`) — real sum/sum-of-squares
  math-equation method, hardcoded on `{3, 1, 2, 5, 3}`.

This is the "hardcoded narration" pattern named in `PROMPT-J-full-roadmap.md`: functionally
indistinguishable from the fallback-delegate bug (the API always returns the same trace
regardless of input), but each one needs a straightforward *rewrite* of an already-correct
algorithm into the tracer contract, not a from-scratch reimplementation.

## Tracer design

All four become `@Component`s in `tracer/impl/`, `dsType()` → `DsType.INT_ARRAY`, using
`StepEmitter.array(int[])` / the existing primary/secondary highlighting exactly as every
prior Arrays-topic tracer in this project already does (batch 1's `count-inversions`,
`reverse-pairs`, `sort-0-1-2`, `next-permutation` are the precedent).

### `three-sum` (Medium)
- `InputSpec`: single `INT_ARRAY` field `"nums"`, length 1–12 (O(N^2), keep it small),
  element bound e.g. `[-100, 100]`.
- `alternateInput()`: an array with a different number of triplets than the default (not
  just a permutation) — verified by hand before committing to defaults.
- Anchors: `sort`, `outerSkipDup`, `sumZero`, `sumNeg`, `sumPos`, `innerSkipDup`, `done`.
- Default: LeetCode's own `[-1,0,1,2,-1,-4]` (1 unique triplet, exercises dup-skip).
  Alternate: an array producing 0 or 2+ triplets, chosen to additionally exercise the
  inner dup-skip branches the default may not reach — verify with a Python simulation
  before writing Java, same discipline used for the Strings batch.

### `four-sum` (Medium)
- `InputSpec`: `INT_ARRAY` field `"nums"` (length 1–10, O(N^3) — must stay small),
  `INT` field `"target"`.
- `alternateInput()`: different `nums`/`target` producing a different quadruplet count.
- Anchors mirror three-sum's but with the extra nesting level: `sort`, `iSkipDup`,
  `jSkipDup`, `sumTarget`, `sumLess`, `sumMore`, `kSkipDup`, `done`.
- Default: LeetCode's own `[1,0,-1,0,-2,2]`, target `0`.

### `merge-two-sorted-arrays` (Hard)
- `InputSpec`: two `INT_ARRAY` fields, `"arr1"` and `"arr2"`, each already sorted
  (`InputValidator` constraint), length 1–10 each.
- Design question resolved: the algorithm operates on one *conceptually combined* buffer
  (`arr1` followed by `arr2`) — the legacy generator uses a single backing `int[] arr`
  with `n`/`m` boundary math. The tracer keeps the same combined-buffer model and emits
  `.array()` on the whole combined buffer each step, so the swap-across-the-boundary
  visual (the actual point of the gap method) stays visible. This mirrors how
  `unique-paths-2`/`triangle-min-path-sum` chose one faithful representation over
  splitting into two canvases.
- `alternateInput()`: different-length arrays (not just different values) so the gap
  sequence itself differs, not merely the numbers being swapped.
- Anchors: `initGap`, `compare`, `swap`, `noSwap`, `shrinkGap`, `done`.

### `repeating-missing-number` (Hard)
- `InputSpec`: single `INT_ARRAY` field `"a"`, values constrained to `1..n` with exactly
  one repeat and one missing (`InputValidator` constraint — same "must already satisfy the
  algorithm's precondition" pattern used for `search-rotated-sorted`'s rotation constraint).
  Length 2–15.
- `alternateInput()`: different `n`, different repeated/missing values.
- Anchors: `sumPass` (the single O(N) loop accumulating `S`/`S2`), `solveEquations`,
  `done`. Only 3 anchors — this is a short, arithmetic-heavy algorithm; a `TracerContractTest`
  concern is whether 3 anchors is enough step variety to satisfy `stepCountGrowsWithInput`
  — mitigated by emitting one step per array element during the sum pass (so step count
  scales with `n` directly), not one combined step.

## Sequencing

Two PRs, Hard first (this project's default convention — the Strings batch reversed it only
because the Hard pair *was* the Medium pair's exact mechanism, which is not the case here;
these four are two unrelated technique pairs):

1. `feat/trace-arrays2-hard` — `repeating-missing-number`, `merge-two-sorted-arrays`.
2. `feat/trace-arrays2-medium` — `three-sum`, `four-sum`.

Each PR: implement tracers, retire the corresponding `ArrayService` cases via
`LegacyTraceRetiredException` (removing the now-dead `generate*Steps()` bodies), update
`ArrayServiceTest`'s retired-id set, update `ApiContractTest`'s `RETIRED_IDS`/
`retiredTraces()` for `/api/arrays`, regenerate golden files and read the diffs, update
README's traced list and RCA.md if a new pattern surfaces, run `mvn test` full suite,
push, open PR, poll CI, merge, sync, delete branch — the same workflow used for every prior
batch this session.

## Explicitly out of scope

- `set-matrix-zeroes` / `spiral-matrix` — flagged in `PROMPT-J-full-roadmap.md` as good
  follow-up candidates (both Medium, both `MATRIX`-shaped) but deliberately excluded here
  to keep this batch at four ids.
- Any Linked List, Trie, or stateful-API-sequence problem — out of scope for an Arrays batch
  regardless of roadmap sequencing.
