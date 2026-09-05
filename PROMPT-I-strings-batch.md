# PROMPT I: Strings tracer batch (KMP/Z-function family)

**Status at time of writing:** planned, not yet implemented. `traced` is 76 of 433
(batch 2, `PROMPT-H-hard-medium-batch-2.md`, just shipped: Dynamic Programming, Binary
Trees, Recursion & Backtracking). This file scopes a single-topic batch: two Medium and
two Hard string-algorithm tracers that all build on the same core mechanism.

## Where these problems actually live

All four ids carry category `"Strings - Algorithms"`, but **they are not registered in
`StringService.java`** — that class only owns a different, smaller set of string
problems (`longest-palindromic-substring`, `reverse-words-string`, etc.), none of which
this batch touches. The four ids below are registered in `AdvancedGraphService.java`,
alongside the graph curriculum, because that service's bulk-registration array happens to
carry a trailing "Strings - Algorithms" section (ids 54–62: `bracket-reversals`,
`count-and-say`, `string-hashing-theory`, `rabin-karp-algo`, `z-function-algo`,
`kmp-lps-algo`, `shortest-palindrome`, `longest-happy-prefix`,
`count-palindromic-subsequences`). This was confirmed by grep before writing anything
else in this doc — an earlier draft of the research behind this batch assumed
`StringService` and was wrong.

**Current bug, confirmed by reading the actual generator methods:**

```java
private List<ExecutionStep> generateRabinKarpSteps() { return generateGraphIntroSteps(); }
private List<ExecutionStep> generateZFunctionSteps() { return generateGraphIntroSteps(); }
private List<ExecutionStep> generateKmpLpsSteps() { return generateGraphIntroSteps(); }
private List<ExecutionStep> generateShortestPalindromeSteps() { return generateGraphIntroSteps(); }
private List<ExecutionStep> generateLongestHappyPrefixSteps() { return generateGraphIntroSteps(); }
private List<ExecutionStep> generateCountPalindromicSubsequencesSteps() { return generateGraphIntroSteps(); }
```

Every one of these six string algorithms currently plays **"1. Introduction to Graph"**
— two steps of generic graph-node narration with no relationship to the actual problem.
This is the textbook fallback-bug pattern this whole migration exists to fix, and
arguably the most visible instance of it found so far: a pattern-matching algorithm
literally animating an unrelated graph.

The switch statement (line ~79) already has one case per id (not a `default:` fallback),
so retiring is a same-line edit — replace the delegate call with
`throw new LegacyTraceRetiredException(problemId)` — not an added case.

## Why these four, and in this pairing

- **`kmp-lps-algo`** (Medium) and **`z-function-algo`** (Medium) are the two
  foundational "build a preprocessing array by scanning the string against itself"
  algorithms — different techniques (failure-function backtracking vs. a `[l, r]`
  window reused across positions), same payoff: once you've traced one, the other's
  logic reads as a variation, not new material.
- **`longest-happy-prefix`** (Hard) and **`shortest-palindrome`** (Hard) are not a
  separate technique — they are the KMP-LPS computation applied to a derived string
  and read differently at the end. `longest-happy-prefix` computes the LPS array of `s`
  itself and returns the prefix named by its last cell. `shortest-palindrome` computes
  the LPS array of `s + '#' + reverse(s)` and uses the last cell to decide how many
  characters must be prepended. Tracing the Medium pair first and getting the LPS
  mechanics right once means the Hard pair is "the same table, a different final
  question" rather than two more unknowns.

This is a deliberate departure from batch 1/2's Hard-first PR ordering, explained below
under PR structure.

All four already carry the correct `dsType` in the catalogue —
`AdvancedGraphService.bulkDsType(id, category)` returns `DsType.STRING` whenever
`category.startsWith("Strings")`, which is true for all four. **No metadata fix is
needed here**, unlike the DP/Tree batches, where individually-registered entries had
carried a stale placeholder type.

## Topics and problems

| Difficulty | Ids | Shared technique |
|---|---|---|
| Medium (2) | `kmp-lps-algo`, `z-function-algo` | Build a length-N int array from a single string via a linear scan that compares the string against itself; failure-function backtracking vs. box-reuse are the two classic ways to do it. |
| Hard (2) | `longest-happy-prefix`, `shortest-palindrome` | Both *are* the KMP-LPS computation (line-for-line the same core loop as `kmp-lps-algo`), applied to `s` itself or to a derived `s + '#' + reverse(s)`, with a different final read-off. |

## dsType and canvas: `DsType.STRING`, no frontend risk

Chose `DsType.STRING` (wire value `"String"`), which the frontend's
`CANVAS_BY_DSTYPE` registry already maps to the existing `ArrayCanvas` (`String:
ArrayCanvas` in `frontend/src/canvas/registry.js`) — the same canvas every `ARRAY`-typed
tracer already renders through. Backend-side, `StepEmitter.Step` already has a
`chars(String value, int primary, int secondary)` helper (confirmed by reading
`StepEmitter.java`) that turns a string into the exact same `arrayState` shape
`.array(int[])` produces — one `ArrayElement` per character, labelled with the actual
glyph, `primary`/`secondary` indices marked `"current"`/`"target"`. Because it reuses
the proven `arrayState` field rather than inventing a new one, there is no
frontend-shape risk here the way there was for `RecursionTree` (considered and rejected
for the Recursion & Backtracking batch) or `Trie` (see below) — this is exercising an
existing, already-correct data path for the first time, not activating an unverified
one.

**One real risk, checked against `RCA.md` before committing to this design:**
`RCA-013` ("New wire helpers still need activation conventions", status **Open**)
specifically flags that `chars()` indexes Unicode *code points* while Java string
algorithms normally index UTF-16 *char offsets* — these diverge for astral characters
(surrogate pairs), and no tracer has exercised `chars()` yet to prove out the
convention. This batch does not attempt to resolve RCA-013 in general. Instead, every
`STRING` `InputField` in this batch is constrained with `.constraint("pattern",
"[a-z]+")` (lowercase ASCII only) — every character is a single UTF-16 code unit *and*
a single code point, so `String.charAt(i)` and `value.codePoints().toArray()[i]` agree
trivially for these inputs. This sidesteps the divergence rather than fixing it; the
plan explicitly does **not** claim RCA-013 resolved, and the PR that lands `kmp-lps-algo`
should update RCA-013's note to record that `chars()` is now exercised (by ASCII-only
callers) rather than mark it closed.

**Growability side effect, worth stating plainly:** `FieldType.STRING` is not in
`TracerContractTest`'s `GROWABLE` set (`INT_ARRAY`, `INT_GRID`, `LINKED_LIST`,
`BINARY_TREE`). `stepCountGrowsWithInput` will `assumeTrue`-skip every tracer in this
batch, the same way it already skips `n-queens` (scalar `n`) and `sudoku-solver`
(`STRING` puzzle field). That removes an entire class of risk this session hit twice
already (the `tree-lca` always-at-root collision, the Sudoku 9x9-vs-grown-grid shape
mismatch) — there is no growth path to reason about here at all.

## Hand-verified defaults and alternates

Computed with a throwaway Python port of each exact algorithm before writing any Java,
same discipline as every previous batch. All four cross-checked against LeetCode's own
published examples where the problem has one.

- **`kmp-lps-algo`** — default `"abababca"` → `lps = [0,0,1,2,3,4,0,1]`. Chosen because
  it hits all three branches of the algorithm in one input: character match (`len++`),
  mismatch-with-fallback (`len = lps[len-1]`, including one case that falls back
  *twice* in a row, from 4→2→0 at index 6), and mismatch-with-`len==0`. Alternate:
  `"abcde"` → `lps = [0,0,0,0,0]` — no self-overlap at all, a deliberately contrasting
  "the pattern doesn't repeat" case.
- **`z-function-algo`** — default `"aabxaabxcaabxaabxay"` → `z =
  [0,1,0,0,4,1,0,0,0,8,1,0,0,5,1,0,0,1,0]` (a standard reference example for this
  algorithm; exercises both the "reuse the existing `[l,r]` box" branch and the
  "expand past the box" branch repeatedly). Alternate: `"aaaaa"` → `z = [0,4,3,2,1]`, the
  degenerate all-one-character case.
- **`longest-happy-prefix`** — default `"level"` → `"l"`; alternate `"ababab"` →
  `"abab"`. Both are LeetCode 1392's own example 1 and example 2.
- **`shortest-palindrome`** — default `"aacecaaa"` → `"aaacecaaa"`; alternate `"abcd"` →
  `"dcbabcd"`. Both are LeetCode 214's own example 1 and example 2.

## Retirement mechanics (same shape as every prior batch, specifics noted)

1. In `AdvancedGraphService.generateSteps`, replace each of the four cases' delegate
   call with `throw new LegacyTraceRetiredException(problemId)`. Leave
   `generateGraphIntroSteps()` itself untouched — dozens of other still-unmigrated ids
   in this same service still delegate to it as filler.
2. `AdvancedGraphServiceTest` currently has **no** retired-id `Set` at all (confirmed by
   grep — this service has had no tracer land yet). The PR that ships the first pair
   adds that `Set` for the first time, following the pattern every other `*ServiceTest`
   already has (`DpServiceTest`, `TreeServiceTest`, `RecursionBacktrackingServiceTest`).
3. `ApiContractTest.RETIRED_IDS` and `.retiredTraces()` both need the new ids added under
   base path `"/api/graphs/advanced"` (confirmed via
   `AdvancedGraphController`'s `@RequestMapping`) — this list has been the single most
   frequently under-updated one across every batch so far (it silently missed 16 ids in
   a row before batch 2 caught and backfilled it), so re-derive it from the actual
   switch statement rather than copy-pasting the previous batch's block.
4. Checked for a dedicated legacy test class exercising these ids' `generateSteps`
   directly (the `Knapsack01TracingTest`/`NQueensTracingTest` pattern): grepped
   `AdvancedGraphServiceTest.java` and `GraphTracingTest.java` for all four ids. Only one
   hit — `AdvancedGraphServiceTest` looks up `kmp-lps-algo`'s `ProblemDetail` by id (a
   metadata check, not a `generateSteps` assertion) — so no separate legacy test needs
   rewriting this time. Re-verify this with a fresh grep at implementation time in case
   the file has changed since this doc was written.

## Rejected or deferred, and why

- **Tries** (`implement-trie`, `word-break-trie`) — genuinely good tracing candidates
  (character-by-character descent through a tree), but blocked on a documented,
  unresolved gap: `RCA-012` ("Trie canvas and backend node shapes do not yet agree",
  status **Open — deferred to Phase 3**) states the frontend `TrieCanvas` expects
  `char`/`isEnd`/child-id-array fields while the backend's trie serializer emits
  `character`/`endOfWord`/char-to-id maps. A tracer written today would pass every
  backend test and render broken or blank in the actual UI. Fixing that shape mismatch
  is its own task (define one JSON shape, add a backend-fixture regression guard,
  activate the canvas) and does not belong folded silently into a routine tracer PR —
  excluded from this batch for that reason, not because the problems are unsuitable.
- **`rabin-karp-algo`** (Medium) — a legitimate fourth Medium candidate (rolling-hash
  update is pure state mutation, good tracing value) but a *different* technique family
  from the LPS/Z-function pair, which would break this batch's "one mechanism, two
  problems building on it" narrative. Left for a future Strings batch.
- **`count-palindromic-subsequences`** (Hard) — a 2D counting DP, same species as
  `edit-distance`/`count-palindromic-subsequences`-style tracers already in the DP
  topic, not the KMP/Z family. Left for a future batch (DP-adjacent, not Strings-family).
- **`longest-palindromic-substring`** (Medium) — lives in the *actual* `StringService`
  (unlike the four above) and already has a dedicated legacy generator there
  (`generateLongestPalindromicSubstringSteps`, not a `generateGraphIntroSteps()`-style
  delegate). Whether that generator is a real correct implementation reachable through
  the old `TraceRecorder` mechanism (the `knapsack-01` pattern) or another kind of bug
  needs its own check before it can be scoped — left out of this batch rather than
  assumed either way.

## Per-problem verification (unchanged from batch 1 and 2)

1. Write the tracer: the real algorithm in `run()`, `// @a`-anchored `annotatedCode()`,
   `alternateInput()` materially different from the default (verified above by hand for
   all four).
2. Retire the legacy delegate per the mechanics section above.
3. Add the id to `AdvancedGraphServiceTest`'s retired-id set (new `Set` for the Medium
   PR, extended for the Hard PR).
4. `mvn test -Dtest=TracerContractTest,ApiContractTest,ProblemsApiTest,AdvancedGraphServiceTest,CatalogTracerMetadataTest`
   green — pay particular attention to `anchorsAreAllReachable` (every declared anchor
   fires across default-or-alternate) and `byteEstimateTracksActualPayload` (bit this
   session once already, on `subsets-i`, from narration that was too terse relative to
   the shared estimate's calibration — write descriptions with real content from the
   start rather than fixing this after a failure).
5. Full `mvn test` green.
6. `mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true`, then `git status` to
   confirm only the new golden file(s) changed. Read each file and hand-verify against
   the worked values above.
7. Restart the backend cleanly (check for and kill any stale process holding port 8923
   first — this has bitten every batch so far) and live-`curl` both default and
   alternate for both problems in the PR, plus the legacy `/api/graphs/advanced/execute/{id}`
   path for both (expect 410).
8. Update the README "Traced so far" list.
9. Update `RCA-013`'s entry in `RCA.md` to note `chars()` is now exercised under an
   ASCII-only constraint (not full resolution) — the one documentation update this batch
   specifically owes, beyond the README list every batch already updates.

## PR structure

Two PRs, Medium first — a deliberate reversal of batch 1/2's Hard-first convention,
because the Hard pair in this batch is not independent material: it is the Medium
pair's exact mechanism applied to a derived string. Landing and verifying the LPS
mechanics once, on the simpler problem, before reusing it twice more is lower-risk than
implementing all four in parallel.

1. `feat/trace-strings-medium` — `kmp-lps-algo`, `z-function-algo`
2. `feat/trace-strings-hard` — `longest-happy-prefix`, `shortest-palindrome`

Each PR: cut branch → implement both tracers → run the verification steps above →
commit → push → open PR → poll CI to green → merge → sync local `main` → delete the
branch → move to the next PR. `traced` goes from 76 to 80 across the batch.

If a chosen id turns out, once actually opened, to already be traced, to be a duplicate
id, or to need something this plan didn't anticipate, that single problem gets dropped
from the batch with a note in its PR — not silently forced in, same rule as every prior
batch.
