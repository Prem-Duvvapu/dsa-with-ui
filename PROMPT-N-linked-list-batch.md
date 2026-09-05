# PROMPT-N — Linked List (batch 7 of the full roadmap)

Status: plan only, no implementation yet. This is batch 7 of `PROMPT-J-full-roadmap.md`.

## Scope

| Difficulty | Id | Title |
|---|---|---|
| Medium | `find-starting-point-loop` | Find Starting Point of Loop in LL |
| Hard | `reverse-ll-group-k` | Reverse LL in Groups of Size K |

Not a 2+2 batch, per `PROMPT-J-full-roadmap.md`'s own finding — only two of the four
original Linked List candidates are buildable on the current `ListNode` model
(`id`, `val`, `nextId`, `prevId`, `state` — no `childId`/`randomId`), which is why
`flattening-ll` and `clone-ll-random-pointer` are excluded (see below). Both ids ship in
one PR.

Both live in `LinkedListService.java`, registered in the bulk `String[][]` table in
`populateRemainingLlProblems()` — the same table `reverse-linked-list` (already traced)
lives outside of, but the dsType derivation is identical: wire value `"LinkedList"` on
every entry, confirmed by reading the registration loop directly. No catalogue metadata
fix needed.

## Current bug

Both `find-starting-point-loop` and `reverse-ll-group-k` are one-line fallback delegates to
`generateReverseSteps()` — confirmed by reading the switch statement directly:

```java
private List<ExecutionStep> generateFindStartingPointLoopSteps() { return generateReverseSteps(); }
...
private List<ExecutionStep> generateReverseLlGroupKSteps() { return generateReverseSteps(); }
```

`generateReverseSteps()` is the same hardcoded-list simple reversal the already-traced
`reverse-linked-list`'s legacy generator uses — completely unrelated to either of these
two problems' actual logic.

## A pre-existing gap found while touching this file: `reverse-linked-list`'s own legacy endpoint

While confirming this batch's current bug, `LinkedListService.generateSteps()` was read in
full: `reverse-linked-list` already has a real tracer (`ReverseLinkedListTracer`,
traced earlier in this project) — but its own legacy `case` still returns
`generateReverseSteps()` directly, with no `LegacyTraceRetiredException`. Its legacy
`/api/linkedlist/execute/reverse-linked-list` endpoint currently serves a real trace
correctly by coincidence (the fallback narration and the real tracer both happen to
reverse the same hardcoded example), not because it is guarded — a change to either one
independently would silently start serving a stale animation under this id, exactly the
failure mode this project's entire migration exists to prevent. `LinkedListServiceTest`
also has no retired-id branch at all yet (every other Service test added one the moment
its first id was traced). Fixed in the same PR as this batch, since both fixes touch the
same switch statement and the same test file — not deferred to its own PR for a one-line,
zero-risk addition.

## Design question: representing a cycle in `find-starting-point-loop`'s input

`FieldType.LINKED_LIST` today validates as a plain value array (`InputValidator`'s
`checkIntArray` case) — there is no existing way to say "and the tail points back to
index 3." Two things make this tractable without inventing new wire types:

1. `ListNode.nextId` is already a plain `Integer` id reference, not constrained to form a
   null-terminated chain — a genuine cycle (`lastNode.nextId == someEarlierNode.id`) is
   representable in the existing model with zero changes.
2. The *input* only needs one more scalar alongside the existing `values` array: an `INT`
   field `"loopPos"` — the 0-indexed node the tail connects back to, with `-1` meaning "no
   loop." This is the same "array field plus one auxiliary scalar" shape `koko-eating-bananas`
   (`piles` + `h`) and `split-array-largest-sum` (`nums` + `m`) already use — not a new
   input-contract pattern, just a new pair of fields.

`loopPos` being a valid index for the supplied `values` is a caller precondition the
validator cannot check declaratively (no way to reference one field's length from another
field's bound) — same category as `repeating-missing-number`'s "exactly one repeat, one
missing" precondition: documented in `help()`, not enforced, and both this tracer's own
default and alternate will be hand-verified to respect it before being committed to.

## Tracer design

### `find-starting-point-loop` (Medium)

- `InputSpec`: `LINKED_LIST` field `"values"` (length 1–15, values -999–999), `INT` field
  `"loopPos"` (range -1–14, `-1` = no loop).
- Floyd's tortoise-and-hare, phase 1 (detect): slow advances one step, fast advances two,
  using a virtual `next(i)` that follows the array in order and wraps to `loopPos` past the
  last index (or terminates at `null` if `loopPos == -1`). If fast reaches `null`, there is
  no loop — report that and stop. If slow and fast ever land on the same index, a loop
  exists.
- Phase 2 (locate start): reset one pointer to the head; advance both pointers one step at
  a time; where they meet next is the loop's starting node — the classic proof (distance
  from head to loop start equals distance from the meeting point to the loop start, going
  forward) is exactly what makes this work and worth narrating, not just the mechanical
  steps.
- Anchors: `init`, `advanceSlowFast`, `noLoop` (fast hits the end), `metInLoop` (phase 1
  meeting point found), `resetToHead`, `advanceBoth` (phase 2), `foundStart`, `done`.
- Default: a list with a real loop (e.g. `values=[3,2,0,-4]`, `loopPos=1` — LeetCode 142's
  own example 1, loop starts at value `2`). Alternate: `loopPos=-1` (no loop at all),
  verified in Python first to confirm both the loop and no-loop paths, and that the
  meeting-point arithmetic is correct on the loop case (not just "some node in the loop").

### `reverse-ll-group-k` (Hard)

- `InputSpec`: `LINKED_LIST` field `"values"` (length 1–15, values -999–999), `INT` field
  `"k"` (range 1–15).
- Reverses every full group of exactly `k` consecutive nodes in place; a trailing group
  with fewer than `k` nodes is left in its original order (LeetCode 25's own rule) — this
  asymmetry is the one thing worth over-narrating, since it is where an off-by-one most
  likely hides.
- Anchors: `countGroup` (checking whether `k` more nodes exist before committing to
  reversing), `reverseWithinGroup`, `groupComplete`, `partialGroupLeftAsIs`, `done`.
- Default: `values=[1,2,3,4,5], k=2` (LeetCode's own example, answer `[2,1,4,3,5]` — the
  trailing single node `5` exercises `partialGroupLeftAsIs`). Alternate: a list whose
  length is an exact multiple of `k`, so `partialGroupLeftAsIs` never fires there — verified
  in Python that the default alone already reaches every anchor, since `anchorsAreAllReachable`
  is checked across default+alternate together but a clean single-input pass is preferred
  when achievable, same discipline as the Binary Search partition batch.

## Sequencing

One PR — `feat/trace-linked-list` — both ids plus the `reverse-linked-list` retirement fix,
since all three touch the same switch statement and the same test file. Update
`LinkedListServiceTest` to add its first-ever retired-id branch (covering all three ids),
update `ApiContractTest`'s `RETIRED_IDS`/`retiredTraces()` for `/api/linkedlist`, regenerate
golden files (including one for `reverse-linked-list` if none exists yet — confirm before
assuming) and read the diffs, update README's traced list, run `mvn test` full suite,
live-verify via curl, push, open PR, poll CI, merge, sync, delete branch — the same
workflow used for every prior batch this session.

## Explicitly out of scope

- `flattening-ll` / `clone-ll-random-pointer` — blocked on the `ListNode` model lacking a
  `childId`/`randomId` field; out of scope for this doc, revisit only after (and if) the
  model gains one.
- `rotate-ll` — a third Hard-cluster id in the same table, not included in `PROMPT-J`'s
  original candidate list for this batch; left for a possible future pass.
