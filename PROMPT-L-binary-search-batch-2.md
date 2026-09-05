# PROMPT-L — Binary Search, second pass (batch 5 of the full roadmap)

Status: plan only, no implementation yet. This is batch 5 of `PROMPT-J-full-roadmap.md`.

## Scope

| PR | Difficulty | Id | Title |
|---|---|---|---|
| 1 | Medium | `koko-eating-bananas` | Koko Eating Bananas |
| 1 | Hard | `split-array-largest-sum` | Split Array — Largest Sum |
| 2 | Hard | `median-2-sorted-arrays` | Median of 2 Sorted Arrays |
| 2 | Hard | `kth-element-2-sorted-arrays` | Kth Element of 2 Sorted Arrays |

All four ids live in `BinarySearchService.java`, registered via the bulk `String[][] list`
table in `populateBsAnswersAnd2DProblems()` (the same table `aggressive-cows` and
`book-allocation` — already traced, batch 1 — come from). Category is `"Binary Search -
Answers"` for all four, which does not end with `"2D Arrays"`, so `dsType` wire value
is already `"Array"` — confirmed by reading the registration loop directly (line ~409:
`cat.endsWith("2D Arrays") ? "Matrix" : "Array"`). No catalogue metadata fix needed.

## Current bug: fallback delegation (the classic pattern)

Unlike the Arrays batch just finished (hardcoded narration), all four of these are the
original fallback-delegate bug this whole project exists to fix — confirmed by reading each
generator method directly:

```java
private List<ExecutionStep> generateKokoSteps() { return generateBs1dSteps(); }
private List<ExecutionStep> generateSplitArrayLargestSumSteps() { return generateBs1dSteps(); }
private List<ExecutionStep> generatePaintersPartitionSteps() { return generateBs1dSteps(); }
private List<ExecutionStep> generateMedian2SortedArraysSteps() { return generateBs1dSteps(); }
private List<ExecutionStep> generateKthElement2SortedArraysSteps() { return generateBs1dSteps(); }
```

`generateBs1dSteps()` is a plain binary-search-for-a-value animation on a hardcoded sorted
array `[1,3,5,7,9,11,13]` — completely unrelated to any of these four problems, each of
which is "binary search on the answer" or partition-based, not a value lookup.

## Why this batch pairs across difficulty instead of by it

Every prior batch this project paired same-difficulty ids into one PR (Hard pair, then
Medium pair). This batch pairs by *mechanism* instead, because the four ids split into two
genuinely different techniques rather than four independent ones:

- **PR 1 — binary search on the answer.** `koko-eating-bananas` (Medium) and
  `split-array-largest-sum` (Hard) both binary-search a candidate answer value, using a
  greedy feasibility check over the array to decide which half of the search space survives.
  This is the exact mechanism `aggressive-cows`/`book-allocation` (batch 1) already trace —
  Koko teaches it with the simplest possible feasibility check (per-pile division), Split
  Array reuses it with a harder one (greedy subarray partitioning). Same reasoning as the
  Strings batch's Medium-then-Hard pairing: the Hard half is not a new idea, it is the
  Medium half's mechanism under a harder feasibility check.
- **PR 2 — partition-based binary search across two arrays.** `median-2-sorted-arrays` and
  `kth-element-2-sorted-arrays` binary-search a *partition point* in the smaller array
  (not a value), using the four boundary elements around the cut to decide which side to
  shrink. `kth-element` is a direct generalization of `median` — median is exactly
  `k = (n+m+1)/2` — so tracing them together mirrors batch 2's DP interval-pair reasoning
  (`matrix-chain-multiplication` → `burst-balloons`): same idea, more general.

`painters-partition` is the fifth id in this table's Hard cluster and is deliberately
**skipped**: it is `split-array-largest-sum`'s identical "binary search on the answer,
greedy feasibility check" recurrence under a different cover story (minimize the maximum
paint-time per painter vs. minimize the maximum subarray sum — the same greedy partition
check either way). Tracing it immediately after `split-array-largest-sum` would be a
near-clone, not new pedagogical content, so it stays untraced pending a future batch that
pairs it with something that actually differs (or is retired from the roadmap entirely).

## Tracer design

### PR 1a: `koko-eating-bananas` (Medium)

- `InputSpec`: `INT_ARRAY` field `"piles"` (length 1–10, values 1–1000), `INT` field `"h"`
  (range matching pile count to total hours).
- Binary search range `[1, max(piles)]`. Feasibility: `sum(ceil(pile / speed))  <=  h`.
  This is a **minimize** search — feasible means "try smaller", the mirror image of
  `aggressive-cows`' maximize search (feasible means "try larger"). Worth narrating
  explicitly since it is this pair's first appearance of that shape.
- Anchors: `init`, `mid`, `hoursTally` (per-pile accumulation, mirrors `aggressive-cows`'
  `place`/`skip` — but koko's inner loop has no branch, so a single anchor per pile
  suffices), `feasible`, `infeasible`, `done`.
- Default: LeetCode's own `piles=[3,6,7,11], h=8` (answer 4). Alternate: a pile
  distribution/h combination verified in Python to differ materially (different answer,
  different feasible/infeasible split point) before writing any Java.

### PR 1b: `split-array-largest-sum` (Hard)

- `InputSpec`: `INT_ARRAY` field `"nums"` (length 1–10, values 0–1000), `INT` field `"m"`
  (number of subarrays, range 1–10).
- Binary search range `[max(nums), sum(nums)]`. Feasibility: greedily accumulate a running
  subarray sum, starting a new subarray whenever adding the next element would exceed the
  candidate; feasible iff the resulting subarray count `<= m`.
- Anchors: `init`, `mid`, `extend` (element joins the current subarray), `split` (element
  starts a new subarray), `feasible`, `infeasible`, `done`.
- Default: LeetCode's own `nums=[7,2,5,10,8], m=2` (answer 18). Alternate verified in Python
  to hit both `extend` and `split` and produce a different answer.

### PR 2a: `median-2-sorted-arrays` (Hard)

- `InputSpec`: two `INT_ARRAY` fields, `"nums1"` and `"nums2"`, both `.sorted()` (the
  algorithm's precondition, same as `merge-two-sorted-arrays`), length 0–10 each (at least
  one non-empty — enforce via a combined-length check in `run()` if `InputValidator` cannot
  express "at least one of two fields is non-empty" declaratively; if not expressible,
  document the precondition in `help()` instead, same pattern used for
  `repeating-missing-number`'s unenforceable precondition).
- Always partitions the **smaller** of the two arrays; if `nums1` is larger, the tracer
  swaps its own local references (not the caller's fields) before searching, exactly as the
  textbook algorithm does — this needs its own narrated step so the swap is not invisible.
- Anchors: `swapToSmaller` (only emitted when the swap actually happens — needs an
  alternate/default pair where at least one triggers it), `partition`, `boundaries`,
  `found`, `shrinkHigh`, `shrinkLow`.
- Default and alternate chosen so between them: one has `nums1` larger than `nums2`
  (exercises the swap), the other doesn't; combined they hit both parities (odd and even
  total length) since the median formula branches on that. All hand-verified against a
  Python simulation before writing Java.

### PR 2b: `kth-element-2-sorted-arrays` (Hard)

- `InputSpec`: two `INT_ARRAY` fields `"nums1"`, `"nums2"` (both `.sorted()`, length 0–10
  each), `INT` field `"k"` (1-indexed, range validated against combined length in `run()`).
- Shares `median-2-sorted-arrays`' exact partition mechanism, generalized: the cut point is
  derived from `k` instead of always bisecting to the halfway point, and the low/high
  binary-search bounds on the smaller array's cut become `[max(0, k-n2), min(k, n1)]`
  instead of `[0, n1]`.
- Anchors mirror `median-2-sorted-arrays`': `swapToSmaller`, `partition`, `boundaries`,
  `found`, `shrinkHigh`, `shrinkLow`.
- Default: an LeetCode-style example with `k` in the middle of the combined range.
  Alternate: a different `k` (near an edge, e.g. `k=1` or `k=n+m`) so the two together
  exercise both bound-clamping directions of `[max(0,k-n2), min(k,n1)]`.

## Sequencing

Two PRs, exactly as scoped above — Koko+SplitArray first (the simpler mechanism), then
Median+KthElement (the harder one, reusing nothing from the first PR at the code level, so
no branch-cut ordering risk the way Strings' Medium-before-Hard had):

1. `feat/trace-binary-search2-answer` — `koko-eating-bananas`, `split-array-largest-sum`.
2. `feat/trace-binary-search2-partition` — `median-2-sorted-arrays`,
   `kth-element-2-sorted-arrays`.

Each PR: implement tracers, retire the corresponding `BinarySearchService` cases via
`LegacyTraceRetiredException`, update `BinarySearchServiceTest`'s retired-id set (if one
exists — confirm before assuming batch 1's Aggressive Cows/Book Allocation PR already
created it), update `ApiContractTest`'s `RETIRED_IDS`/`retiredTraces()` for
`/api/binary-search`, regenerate golden files and read the diffs, update README's traced
list, run `mvn test` full suite, live-verify via curl, push, open PR, poll CI, merge, sync,
delete branch — the same workflow used for every prior batch this session.

## Explicitly out of scope

- `painters-partition` — see above; deliberately skipped as a near-duplicate of
  `split-array-largest-sum`.
- The Binary Search "2D Arrays" cluster (`row-max-ones`, `search-2d-matrix`,
  `search-2d-matrix-2`, `find-peak-element-2d`, `matrix-median`) — different `dsType`
  (`Matrix`) and out of scope for this batch regardless.
