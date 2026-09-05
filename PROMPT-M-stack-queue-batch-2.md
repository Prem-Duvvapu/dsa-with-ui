# PROMPT-M — Stack & Queue, second pass (batch 6 of the full roadmap)

Status: plan only, no implementation yet. This is batch 6 of `PROMPT-J-full-roadmap.md`.

## Scope

| Difficulty | Id | Title |
|---|---|---|
| Hard | `sliding-window-maximum` | Sliding Window Maximum |
| Medium | `min-stack` | Implement Min Stack |
| Medium | `sum-subarray-minimums` | Sum of Subarray Minimums |

All three ids live in `StackQueueService.java`, registered via the bulk `String[][]` table
in `populateRemainingStackQueueProblems()` — the same table `next-greater-element-2` and
`asteroid-collision` (already traced, batch 1) come from — with `dsType` wire value
`"Stack"` already correct on all three (confirmed by reading the registration loop
directly, same pattern as every prior bulk-table batch). `DsType.STACK` maps to the
existing `ArrayCanvas`, already proven by batch 1's pair — no canvas risk.

This is a 1 Hard + 2 Medium batch, not the usual 2+2 — `lru-cache` is deliberately excluded
(see below), and rather than force a fourth id to restore symmetry, this ships as one PR of
three, the same way batch 4's Linked List work shipped two ids in one PR when that was what
naturally fit.

## Current bug

- `sliding-window-maximum`, `min-stack`, and `sum-subarray-minimums` all fall through to
  `default: return generateBalancedParenthesesSteps();` in `StackQueueService.generateSteps()`
  — confirmed by reading the switch statement directly. None of the three has its own
  `case` at all; they are the plain, undisguised fallback bug this whole project exists to
  fix, not even a one-line delegate method.

## `lru-cache` is deliberately excluded from this batch

`PROMPT-J-full-roadmap.md` already flagged this as an open design question rather than
assuming an answer, and this doc resolves it: **excluded**, kept alongside the Heaps &
PriorityQueue trio `PROMPT-H` rejected for the same reason — LeetCode presents it as a
caller-controlled sequence of `put`/`get` calls whose interesting behavior (eviction order)
depends on the specific interleaving, which doesn't fit a single `InputSpec` → single
`run()` trace without inventing a new input shape.

`PROMPT-J` itself suggested a real way to make it fit — encode the operation sequence as a
`STRING` (or small custom type) and trace the whole thing in one `run()` call, which is
exactly what a hypothetical `LruCacheTracer` would need. That is a legitimate follow-up, but
it is new *contract* work (a wire encoding for "list of typed operations" nothing else in
this codebase has needed yet), not a routine tracer addition — scoping and testing that
encoding belongs in its own small PR, not folded silently into this batch. Deferred, not
rejected outright.

## Why `min-stack` does NOT have the same problem

`min-stack` looks superficially like the same "operation sequence" shape (`push`, `pop`,
`top`, `getMin` in the original problem statement) — worth stating explicitly why it does
not need the same treatment. The tracer does not accept a caller-controlled interleaving of
operations; it accepts a plain `INT_ARRAY` and demonstrates the O(1)-min mechanism on a
*fixed script this project chooses*: push every value in order, then pop them all off one
at a time, narrating the auxiliary running-min stack at each push and pop. The caller
controls *what values*, never *what sequence of operation types* — the same "one input,
one deterministic run()" shape every other tracer already has. No new input type needed.

## Tracer design

### `sliding-window-maximum` (Hard)

- `InputSpec`: `INT_ARRAY` field `"nums"` (length 1–12, values -1000–1000), `INT` field
  `"k"` (window size, range 1–12).
- Monotonic deque of *indices*, kept strictly decreasing by value. Each step: pop from the
  back while the back's value is `<=` the new element (it can never be the window's max
  again); push the new index; pop from the front if it has fallen out of the window
  (`front <= i - k`); once the window has filled (`i >= k - 1`), the front of the deque is
  this window's maximum.
- Anchors: `popBackSmaller`, `pushIndex`, `popFrontOutOfWindow`, `windowMax`, `done`.
- Default: LeetCode's own `nums=[1,3,-1,-3,5,3,6,7], k=3` (answer `[3,3,5,5,6,7]`) — hits
  every anchor per the original walkthrough. Alternate: a strictly monotonic array (e.g.
  increasing) so every push evicts the entire back of the deque, verified in Python first.

### `min-stack` (Medium)

- `InputSpec`: single `INT_ARRAY` field `"values"` (length 1–12, values -1000–1000) — the
  values to push, in order.
- `run()`: push every value, tracking a parallel `minStack` whose top is always the running
  minimum (`push(Math.min(value, minStack.isEmpty() ? value : minStack.peek()))`), narrating
  both stacks together; then pop everything back off in LIFO order, narrating how the min
  stack's own pop keeps `getMin()` correct as the real minimum changes.
- Anchors: `push`, `newMin` (the pushed value becomes the new running min) vs `sameMin`
  (running min unchanged), `pop`, `done`.
- Default: a sequence whose minimum changes multiple times on the way down and back up
  (e.g. `[3, 5, 1, 4, 1, 2]` — two ties at the minimum, exercising both `newMin` and
  `sameMin` and the pop-restores-a-previous-min case). Alternate: monotonically increasing
  values, so the minimum is set once on the first push and never changes again — verified
  in Python before writing Java.

### `sum-subarray-minimums` (Medium)

- `InputSpec`: `INT_ARRAY` field `"nums"` (length 1–10 — this is the `O(N)`-with-a-large-
  constant one, keep it small since the modulus arithmetic and duplicate-handling narration
  is the point, not raw scale), values 0–1000 (LeetCode's own domain).
- For each element, find how many subarrays have it as their minimum: `left[i]` = distance
  to the previous *strictly* smaller element (or start), `right[i]` = distance to the next
  *smaller-or-equal* element (or end) — the strict/non-strict asymmetry is deliberate and
  is what prevents double-counting a subarray whose minimum appears more than once; each
  element's contribution is `nums[i] * left[i] * right[i]`, summed modulo `1e9+7`.
- Anchors: `popStrictlyGreater` (maintaining the previous-smaller monotonic stack),
  `pushLeftBoundary`, `popGreaterOrEqual` (maintaining the next-smaller-or-equal monotonic
  stack), `pushRightBoundary`, `contribution` (per-element sum accumulation), `done`.
- Default: an array with at least one repeated value (LeetCode's own `[3,1,2,4]` has no
  repeats — use `[71,55,82,55]`, LeetCode's own example 2, which does) so the strict/
  non-strict distinction actually matters on the default input, not only the alternate.
  Alternate verified in Python to differ materially and to additionally exercise whichever
  branch the default doesn't.

All three inputs (and their alternates) will be hand-verified against a Python simulation
before any Java is written, per this project's standing discipline — this is especially
important for `sum-subarray-minimums`, where the strict/non-strict boundary asymmetry is
the easiest place to introduce an off-by-one that still happens to pass on a small example.

## Sequencing

One PR — `feat/trace-stack-queue2` — implementing all three, since the batch does not split
into a clean 2+2 and forcing an artificial split would just add PR overhead for no reason
(the "1 Hard + 2 Medium in one PR" shape `PROMPT-J-full-roadmap.md` itself proposed as the
fallback if `lru-cache` were excluded). Retire the three `StackQueueService` cases (or in
`sum-subarray-minimums`'/`min-stack`'s/`sliding-window-maximum`'s case, add their first-ever
explicit `case` at all — they never had one) via `LegacyTraceRetiredException`, update
`StackQueueServiceTest`'s retired-id set and `ApiContractTest`'s `RETIRED_IDS`/
`retiredTraces()` for `/api/stackqueue` (verify the exact base path from the controller
before assuming), regenerate golden files and read the diffs, update README's traced list,
run `mvn test` full suite, live-verify via curl, push, open PR, poll CI, merge, sync, delete
branch — the same workflow used for every prior batch this session.

## Explicitly out of scope

- `lru-cache` — deferred, see above.
- `remove-k-digits` / `celebrity-problem` — flagged in `PROMPT-J-full-roadmap.md` as good
  candidates for a follow-up Stack & Queue batch, not included here.
