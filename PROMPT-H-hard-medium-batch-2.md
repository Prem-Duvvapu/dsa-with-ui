# PROMPT H: Hard + Medium tracer batch 2 (Dynamic Programming, Binary Trees, Recursion & Backtracking)

**Status at time of writing:** planned, not yet implemented. `traced` is 64 of 433 (batch 1,
`PROMPT-G-hard-medium-batch.md`, just shipped: Arrays, Binary Search, Stack & Queue). This
file scopes the second slice of the same "two Hard and two Medium per topic" request.

## Why these three topics

Same selection rule as batch 1: pick topics whose Hard/Medium candidates render through an
already-working canvas, and check every candidate individually before committing to it —
some categories look fine from the outside and turn out thin once you check what their
`ProblemDetail`s actually need.

- **Dynamic Programming** — has its own dedicated `DpTableCanvas` and 18 tracers already
  landed there (13 with the D3 recurrence-substitution design). All four candidates below
  are plain `int[]` inputs.
- **Binary Trees** — has `TreeCanvas` and `BinaryTreeLayout`, plus four traversal tracers
  already landed (`tree-inorder`, `tree-preorder`, `tree-postorder`, `tree-level-order`).
  All four candidates are `BINARY_TREE` inputs, no new field type needed.
- **Recursion & Backtracking** — has `GridCanvas` (for the two Hard board problems) and
  `RecursionTreeCanvas`/`ArrayCanvas` (for the two Medium subsequence problems); no tracer
  has landed in this category yet, so this is the first one.

## Two categories checked and rejected before landing on the above

- **Linked List**: only three Hard-difficulty ids exist
  (`reverse-ll-group-k`, `flattening-ll`, `clone-ll-random-pointer`), and `ListNode` only
  has `nextId`/`prevId` fields — no child pointer, no random pointer. Two of the three Hard
  ids need a pointer type that doesn't exist on the model, leaving exactly one viable Hard
  candidate. Not enough to fill the Hard slot honestly, so the whole topic was dropped
  rather than force a second, weaker pick.
- **Heaps & PriorityQueue**: same shape of problem — the three Hard ids are
  `merge-k-sorted-lists` (needs multiple linked lists merged, not a single array/tree
  trace), `design-twitter` (a sequence of API calls on a stateful object, not a single
  before/after trace), and `median-data-stream` (a running-stream problem, same issue).
  None fit this batch's "one input, one trace" shape well; dropped for the same reason.

## Topics and problems

| Topic | Hard (2) | Medium (2) | Shared technique family |
|---|---|---|---|
| Dynamic Programming | `matrix-chain-multiplication`, `burst-balloons` | `knapsack-01`, `unbounded-knapsack` | Hard pair: interval DP (partition a range at every possible split point). Medium pair: 0/1 vs. unbounded knapsack — same recurrence shape, one decrements the item index on take, the other doesn't. |
| Binary Trees | `tree-max-path-sum`, `serialize-deserialize-bt` | `zigzag-traversal`, `tree-lca` | Hard pair: a post-order DFS that returns one thing to its parent while separately tracking a global best (path sum), paired with a BFS-based encode/decode round trip. Medium pair: level-order traversal variants and a bottom-up recursive search — genuinely different from the four already-traced plain traversals. |
| Recursion & Backtracking | `n-queens`, `sudoku-solver` | `subsets-i`, `combination-sum-i` | Hard pair: constraint-checked backtracking over a 2D board (place/check/undo). Medium pair: include/exclude recursion over a flat array — the first tracers in this category. |

All twelve are `int[]`/`int[][]`/`BINARY_TREE` inputs, rendering through `ArrayCanvas`,
`GridCanvas`, or `TreeCanvas` — no frontend change needed for any of them.

## Step-count risk for the two backtracking Hard problems

`n-queens` and `sudoku-solver` explore exponential state spaces; an incautious default
input can blow the 5000-step budget or produce an unreadably long trace. Mitigation:
- `n-queens`: cap the board size field low (e.g. max 8, default 4) — n=4 has exactly 2
  solutions and a small enough search tree to narrate every placement/conflict/backtrack.
- `sudoku-solver`: use a puzzle with very few empty cells for the default (most cells
  pre-filled, only a handful of real decision points), so the trace stays short while
  still exercising place/conflict/backtrack. The alternate input gets slightly more empty
  cells to prove the trace differs, not a full blank grid.
Both also get `.withMaxSteps(...)` set below the global default if the chosen inputs still
run long once actually measured — decided per-tracer once the real step count is known,
not guessed in advance here.

## Per-problem verification (unchanged from batch 1)

1. Write the tracer: the real algorithm in `run()`, `// @a`-anchored `annotatedCode()`,
   and `alternateInput()` that is materially different from the default (a different
   branch profile, not a permutation).
2. Retire the legacy delegate in the owning service with
   `throw new LegacyTraceRetiredException(problemId)` — check the switch first, since some
   ids (as batch 1 found in `StackQueueService`) have no case at all and fall straight to
   `default:`, which needs an added case rather than a replaced one.
3. Add the id to that service's test's retired-id set.
4. `mvn test -Dtest=TracerContractTest,ApiContractTest,ProblemsApiTest,<ServiceTest>` green.
5. Full `mvn test` green.
6. `mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true`, then `git status` to confirm
   only the new golden file(s) changed. Read the file and hand-verify the arithmetic
   against a known worked example (LeetCode's own published examples where available,
   same as every tracer in batch 1).
7. Live `curl` against a running server for both the default and alternate input.
8. Update the README "Traced so far" list.

## PR structure

Six PRs, one per (topic × difficulty) pair, same rationale as batch 1 (two closely related
tracers per PR, not twelve separate CI cycles):

1. `feat/trace-dp-hard-2` — `matrix-chain-multiplication`, `burst-balloons`
2. `feat/trace-dp-medium-2` — `knapsack-01`, `unbounded-knapsack`
3. `feat/trace-binary-trees-hard` — `tree-max-path-sum`, `serialize-deserialize-bt`
4. `feat/trace-binary-trees-medium` — `zigzag-traversal`, `tree-lca`
5. `feat/trace-recursion-hard` — `n-queens`, `sudoku-solver`
6. `feat/trace-recursion-medium` — `subsets-i`, `combination-sum-i`

Each PR: cut branch → implement both tracers → run the verification steps above → commit
→ push → open PR → poll CI to green → merge → sync local `main` → delete the branch →
move to the next PR. `traced` goes from 64 to 76 across the batch.

If a chosen id turns out, once actually opened, to already be traced, to be a duplicate
id, or to need a canvas feature that does not exist, that single problem gets dropped from
the batch with a note in its PR — not silently forced in, same rule as batch 1.
