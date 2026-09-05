# PROMPT-O — Binary Trees, second pass (batch 8 of the full roadmap)

Status: plan only, no implementation yet. This is batch 8 of `PROMPT-J-full-roadmap.md`.

## Scope

| PR | Id | Title |
|---|---|---|
| 1 | `tree-burn-time` | Minimum Time to Burn Binary Tree |
| 1 | `vertical-order-traversal` | Vertical Order Traversal |
| 2 | `morris-inorder` | Morris Inorder Traversal |
| 2 | `correct-bst-swap` | Correct BST with Two Nodes Swapped |

All four candidates from `PROMPT-J-full-roadmap.md`'s second Trees pass are Hard — no
Medium was identified for this pass, so both PRs pair two Hard ids rather than the usual
Hard/Medium split.

All four live in `TreeService.java`, registered in the bulk `String[][]` table in
`populateRemainingTreeProblems()` — the same table `tree-lca`, `zigzag-traversal`,
`tree-max-path-sum` and `serialize-deserialize-bt` (batch 2, already traced) come from.

## Current bug

- `tree-burn-time` has its own explicit `case`, but it delegates to
  `generatePreorderSteps()` — a fallback delegate, not hardcoded narration.
- `vertical-order-traversal`, `morris-inorder`, and `correct-bst-swap` have no `case` at
  all; they fall through to `default: return generatePreorderSteps();` — confirmed by
  reading the switch statement directly.

`generatePreorderSteps()` is a plain preorder walk, unrelated to any of these four
problems' actual logic (BFS burn simulation, column-grouped traversal, threaded O(1)-space
traversal, and inorder-violation tracking respectively).

## Catalogue metadata fix needed: `bulkDsType()`'s allowlist

`TreeService.bulkDsType(id)` is an explicit allowlist, not a category-derived default (unlike
every bulk-table batch so far this session):

```java
private static DsType bulkDsType(String id) {
    return switch (id) {
        case "tree-postorder", "tree-level-order", "tree-max-path-sum",
                "serialize-deserialize-bt", "zigzag-traversal", "tree-lca" -> DsType.TREE;
        default -> DsType.STACK;
    };
}
```

All six already-traced Tree ids are in the allowlist; none of this batch's four are —
their catalogue `dsType` is currently the wrong placeholder (`"Stack"`), the same "stale
placeholder dsType" issue the DP and first Trees batch needed fixing for. This batch adds
all four to the `DsType.TREE` case. Confirmed correct at the code level (`DsType.TREE` is
the only wire value `BinaryTreeLayout` — the shared tree-rendering helper every tree tracer
already uses — is designed to feed) before committing to it.

## Tracer design

Every tracer below shares `BinaryTreeLayout` (`tracer/impl/BinaryTreeLayout.java`) — the
level-order-array-to-positioned-`TreeNode` helper every existing Tree tracer already uses.
No new rendering machinery needed; this batch carries no canvas risk.

### `tree-burn-time` (Hard)

- `InputSpec`: `BINARY_TREE` field `"tree"` (level order, length 1–31, values -99–99),
  `INT` field `"start"` (the value fire begins at).
- Two phases: (1) build a parent map via one traversal from the root (any node's neighbors
  for burning purposes are its parent, left child, and right child — a tree's undirected
  adjacency); (2) multi-source-style BFS from the start node outward across that adjacency,
  incrementing a minute counter each full frontier, tracking the max minute reached.
- Anchors: `buildParentMap`, `foundStart`, `spreadToNeighbor`, `minuteComplete`, `done`.
- Default: a tree whose start node is off-center (not the root), so the fire has to spread
  both up through parents and down through children — the case a pure top-down BFS would
  get wrong. Verified by hand in Python before writing any Java.

### `vertical-order-traversal` (Hard)

- `InputSpec`: `BINARY_TREE` field `"tree"` (level order, length 1–31, values -99–99).
- BFS carrying `(node, row, col)`; the root's column is 0, a left child is `col - 1`, a
  right child is `col + 1`. Collect nodes into a column-keyed map; within a column, nodes
  are ordered by row first, then by value for same-row ties (the specific tie-break that
  makes this problem non-trivial — LeetCode 987's own rule). Final answer sorts columns by
  their key.
- Anchors: `visit`, `assignColumn`, `tieBreakByValue` (only reachable on an input with a
  genuine same-row-same-column collision), `done`.
- Default: an input with at least one same-row, same-column value collision (not every
  tree has one), verified in Python first so `tieBreakByValue` is provably reachable rather
  than assumed.

### `morris-inorder` (Hard)

- `InputSpec`: `BINARY_TREE` field `"tree"` (level order, length 1–31, values -99–99).
- Classic threaded-traversal trick: at each node with a left child, walk to the rightmost
  node of that left subtree (the inorder predecessor). If its right pointer is empty,
  thread it to the current node and descend left (deferring the visit). If the thread is
  already there, that means the left subtree has been fully explored — remove the thread
  (restore the tree), visit the current node now, and move right. A node with no left
  child is visited immediately and moved right. The whole point worth narrating: the
  "threads" are temporary, self-erasing pointers, not a permanent structural change — the
  tree is bit-for-bit the same after the traversal finishes as before it started.
- Anchors: `noLeftVisitNow`, `findPredecessor`, `threadCreated`, `threadFollowedRemoved`,
  `done`.
- Default: a tree with at least one node whose left subtree is more than one node deep (so
  `findPredecessor` actually walks more than one step) and at least one node with no left
  child at all (`noLeftVisitNow`) — verified in Python against a reference recursive
  inorder traversal, not just "produces some order," since Morris traversal reproducing
  the wrong sequence quietly is exactly the kind of bug the golden-file discipline exists
  to catch.

### `correct-bst-swap` (Hard)

- `InputSpec`: `BINARY_TREE` field `"tree"` (level order, length 1–31, values -99–99) — a
  BST with exactly two values swapped (the caller's responsibility to supply; same
  unenforceable-precondition category as `repeating-missing-number`, documented in
  `help()` rather than validated).
- Standard inorder traversal tracking a `prev` pointer; every time `prev.val > curr.val` is
  found, that is an inversion. The **first** inversion records `(first = prev, middle =
  curr)`; if a **second** inversion is found, its second element becomes `last`. At the
  end: if a second inversion happened, swap `first` and `last` (the two swapped values are
  non-adjacent in inorder order); if only one inversion was ever found, swap `first` and
  `middle` instead (the swapped values are adjacent in inorder order — a genuinely
  different code path, not just a smaller version of the same one).
- Anchors: `visitInorder`, `firstInversion`, `secondInversion`, `swapNonAdjacent`,
  `swapAdjacent`, `done`.
- Default and alternate chosen so between them both the adjacent-swap and non-adjacent-swap
  paths are exercised — verified by hand in Python (construct a correct BST, swap two
  values, confirm the algorithm recovers the original tree) before writing any Java.

## Sequencing

Two PRs, pairing by table order rather than mechanism (all four ids are unrelated
techniques — BFS-on-adjacency, BFS-with-coordinates, pointer-threading, and inorder-anomaly-
tracking — so there is no shared-mechanism rationale to pair by, unlike the Strings or
Binary Search second-pass batches):

1. `feat/trace-trees2-burn-vertical` — `tree-burn-time`, `vertical-order-traversal`.
2. `feat/trace-trees2-morris-swap` — `morris-inorder`, `correct-bst-swap`.

Each PR: implement tracers, retire the corresponding `TreeService` cases via
`LegacyTraceRetiredException`, add all four ids to `bulkDsType()`'s `DsType.TREE` case in
PR 1 (confirmed safe to do all at once: `CatalogTracerMetadataTest.catalogueDsTypesMatchEveryRegisteredTracer`
only iterates *existing* tracers and checks each one's dsType against the catalogue's — it
never asserts anything about a catalogued-but-untraced id, so PR 2's still-untraced pair
correctly reporting `DsType.TREE` in the meantime is not a test risk), update
`TreeServiceTest`'s retired-id set, update `ApiContractTest`'s
`RETIRED_IDS`/`retiredTraces()` for `/api/trees` (confirm the exact base path before
assuming), regenerate golden files and read the diffs, update README's traced list, run
`mvn test` full suite, live-verify via curl, push, open PR, poll CI, merge, sync, delete
branch — the same workflow used for every prior batch this session.
