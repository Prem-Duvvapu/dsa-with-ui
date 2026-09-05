# PROMPT G: Hard + Medium tracer batch (Arrays, Binary Search, Stack & Queue)

**Status at time of writing:** planned, not yet implemented. `traced` is 52 of 433. This
file is the plan for the next batch of tracers, written up front per request before any
code for it was touched, so the scope and reasoning are on record rather than only in
scattered PR descriptions.

## Why this batch

The request was "two hard and two medium problems from each topic." Doing that
exhaustively across all ~18 categories is dozens of tracers; this batch is a first
concrete slice — three topics, two Hard and two Medium problems each (12 tracers
total) — with the same problem chosen only after confirming it fits an existing,
already-rendering canvas path. Further topics are a follow-up, not part of this file.

## A ruled-out option: Sliding Window

Sliding Window's untraced Hard set (`subarrays-k-different-integers`,
`minimum-window-substring`, `minimum-window-subsequence`) is two-thirds string problems.
Checked before picking topics: no tracer in this codebase has ever used `DsType.STRING`.
`StepEmitter.chars(String)` exists and produces `ArrayElement`s with the actual character
in the `label` field, but `frontend/src/components/ArrayCanvas.jsx` never reads `label` —
it only renders `el.value` (the numeric codepoint) as the bar's displayed number and bar
height. A string tracer today would pass every backend test (`TracerContractTest`,
`GoldenTraceTest`, ...) while showing ASCII codes instead of letters on screen, and there
is no way to verify that live without also changing the frontend — out of scope for a
backend tracer batch. Stack & Queue was substituted: its Hard/Medium problems used here
are plain `int[]` inputs with no such gap.

## Topics and problems

| Topic | Hard (2) | Medium (2) | Shared technique family |
|---|---|---|---|
| Arrays | `count-inversions`, `reverse-pairs` | `sort-0-1-2`, `next-permutation` | Hard pair: merge-sort inversion counting. Medium pair: single-pass in-place rearrangement. |
| Binary Search | `aggressive-cows`, `book-allocation` | `find-min-rotated-sorted`, `single-element-sorted` | Hard pair: binary-search-on-the-answer with a greedy feasibility check. Medium pair: classic 1D binary search variants, distinct from the already-traced `lower-bound`/`upper-bound`. |
| Stack & Queue | `trapping-rainwater`, `largest-rectangle-histogram` | `next-greater-element-2`, `asteroid-collision` | Hard pair: monotonic stack. Medium pair: stack simulation. |

All twelve are `int[]`-only inputs, rendering through `ArrayCanvas` (`DsType.ARRAY` or
`DsType.STACK`, both already mapped and already tested) — no frontend change needed for
any of them.

## Per-problem verification (unchanged from the `lower-bound`/`upper-bound` work earlier
in this session)

1. Write the tracer: the real algorithm in `run()`, `// @a`-anchored `annotatedCode()`,
   and `alternateInput()` that is materially different from the default (a different
   branch profile, not a permutation).
2. Retire the legacy delegate in the owning service with
   `throw new LegacyTraceRetiredException(problemId)`, matching the existing
   `search-rotated-sorted` / `lower-bound` / `upper-bound` pattern — never leave the old
   switch case silently serving a different problem's steps under this id.
3. Add the id to that service's test's retired-id set.
4. `mvn test -Dtest=TracerContractTest,ApiContractTest,ProblemsApiTest,<ServiceTest>` green.
5. Full `mvn test` green.
6. `mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true`, then `git status` to confirm
   only the new golden file(s) changed. Read the file and hand-verify the substitution
   arithmetic against a known worked example — not just "the test passed."
7. Live `curl` against a running server for both the default and alternate input,
   confirming the branch profile and final answer match the hand-verification.
8. Update the README "Traced so far" list.

## PR structure

Six PRs — one per (topic × difficulty) pair, bundling the two same-family problems per
PR rather than shipping 12 separate PRs (which would mean 12 serialized CI cycles for no
real review benefit, since each pair is two closely related tracers a reviewer looks at
together anyway — the same shape as the `lower-bound` + `upper-bound` pairing already
merged this session, except those happened to land as two PRs rather than one):

1. `feat/trace-arrays-hard` — `count-inversions`, `reverse-pairs`
2. `feat/trace-arrays-medium` — `sort-0-1-2`, `next-permutation`
3. `feat/trace-binary-search-hard` — `aggressive-cows`, `book-allocation`
4. `feat/trace-binary-search-medium` — `find-min-rotated-sorted`, `single-element-sorted`
5. `feat/trace-stack-queue-hard` — `trapping-rainwater`, `largest-rectangle-histogram`
6. `feat/trace-stack-queue-medium` — `next-greater-element-2`, `asteroid-collision`

Each PR: cut branch → implement both tracers → run the verification steps above → commit
→ push → open PR → poll CI to green → merge → sync local `main` → delete the branch →
move to the next PR. `traced` goes from 52 to 64 across the batch.

## Explicitly excluded from this batch, and why

- **`merge-intervals`** — one of the seven ids claimed by two services
  (`stats.duplicateIds`). Resolving a duplicate claim is a separate concern from adding a
  tracer and isn't being mixed into this batch.
- **`min-stack`** — a data-structure-with-operations problem (push/pop/top/getMin as a
  sequence of API calls), not a single before/after array trace. Doesn't fit this batch's
  shape.
- **Infix/prefix/postfix conversions, `remove-k-digits`** — string-parsing problems,
  excluded for the same `DsType.STRING` rendering-gap reason Sliding Window was dropped.

If a chosen id turns out, once actually opened, to already be traced, to be a duplicate
id, or to need a canvas feature that does not exist, that single problem gets dropped from
the batch with a note in its PR — not silently forced in.
