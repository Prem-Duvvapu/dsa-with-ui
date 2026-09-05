# PROMPT-Q — Dynamic Programming, second pass (batch 10 of the full roadmap)

Status: plan only, no implementation yet. This is batch 10 of `PROMPT-J-full-roadmap.md`.

## Scope

| Difficulty | Id | Title |
|---|---|---|
| Hard | `edit-distance` | Edit Distance |
| Hard | `wildcard-matching` | Wildcard Matching |

Both live in `DpService.java`. `ninja-and-his-friends` — the third remaining Hard DP
candidate — is **not** included, per `PROMPT-J-full-roadmap.md`'s finding #3: it is a 3D DP
problem (state is `(row, col1, col2)`, two people moving through the same grid at once) and
the existing `DpTable` model is inherently 2D. Left for its own design pass on how to render
a 3D recurrence (e.g. one row-slice of the `col1`-by-`col2` table at a time) rather than
folded into a routine pair.

## Current bug

Both fall through the `default:` branch of `DpService.generateSteps()`:

```java
default: return generateClimbingStairsSteps();
```

`generateClimbingStairsSteps()` is a 1D staircase-counting trace — nothing about it relates
to either string-alignment problem it is currently standing in for.

## Metadata gap found while reading `DpService.bulkDsType`

`edit-distance` and `wildcard-matching` are registered through the bulk catalogue table
(the `{"id", "title", "category", "difficulty", "desc"}` row array), whose `dsType` is
resolved by `bulkDsType(id)` — an explicit id allowlist, `default -> DsType.ARRAY`. Neither
id is in that allowlist today, so the catalogue currently reports `ARRAY` for both — the
same category of gap `TreeService.bulkDsType` had for all four batch-8 ids (that one was
also an explicit allowlist rather than category-derived, and missing entries silently fell
to a wrong default). Both ids need adding to the `DP_TABLE` case in this PR, independent of
and before the tracer work, exactly as batch 8 did for `TreeService`.

## Tracer design

Neither problem has a same-shape predecessor in this codebase: every existing `DP_TABLE`
tracer (`GridDpTable`, `CoinChangeDpTable`, `SubsetSumDpTable`, `SeriesDpTable`) keys its
table by numeric grid coordinates or array indices. These two are the first
**string-alignment** DP tables — row *i* and column *j* correspond to a prefix of one input
string each, and the natural row/column labels are the strings' own characters (plus an
empty-prefix row/column 0), not `"r0"`/`"c0"`. A new shared helper, `StringDpTable` (mirrors
`GridDpTable`'s `probe`/`read`/`known`/`resolved`/`void` cell-state contract, package-private
in `tracer/impl/`), builds that table from the two input strings — used by both tracers in
this PR, the same one-helper-two-tracers shape `GridDpTable` itself established for the
first grid-DP batch.

Input alphabet: both fields are `FieldType.STRING` with an ASCII-lowercase constraint
(`RCA-013`'s lesson from the Strings batch — a `.constraint("pattern", regex)` +
`.constraint("<field>Hint", …)` pair, same as `KmpLpsTracer`). `wildcard-matching`'s pattern
field additionally allows `?` and `*`.

### `edit-distance` (Hard)

- `InputSpec`: two `STRING` fields, `"word1"` (`[a-z]+`, length 1-15) and `"word2"`
  (`[a-z]+`, length 1-15).
- Recurrence: `dp[i][j]` = edits to turn `word1[0..i)` into `word2[0..j)`. Base cases
  `dp[i][0] = i` (delete everything), `dp[0][j] = j` (insert everything). Transition: if the
  two prefixes' last characters match, `dp[i][j] = dp[i-1][j-1]` (no operation); otherwise
  `dp[i][j] = 1 + min(dp[i-1][j-1], dp[i-1][j], dp[i][j-1])` — replace, delete, insert
  respectively, and the narration says which of the three won.
- Anchors: `init` (base row/column fill), `match` (characters already equal), `replace`,
  `delete`, `insert` (whichever of the three predecessors was cheapest), `done`.
- Default: `word1="horse"`, `word2="ros"` — LeetCode's own example 1, answer 3. Verified by
  hand-porting the recurrence to Python before writing any Java: this single pair already
  exercises all six anchors (`match`, `replace`, `delete`, `insert` all occur at least once
  while filling its 6x4 table). Alternate: `word1="intention"`, `word2="execution"` —
  LeetCode's own example 2, answer 5, a materially larger table (10x10) that also
  independently exercises all six anchors, so `traceRespondsToItsInput` sees a different
  table shape and a different result, not a same-shape restatement of the default.

### `wildcard-matching` (Hard)

- `InputSpec`: `STRING` field `"s"` (`[a-z]+`, length 1-15, the text) and `STRING` field
  `"p"` (`[a-z?*]+`, length 1-15, the pattern).
- Recurrence: `dp[i][j]` = does `p[0..j)` match `s[0..i)`? `dp[0][0] = true`; `dp[0][j] =
  dp[0][j-1]` when `p[j-1] == '*'` (a leading run of stars can match the empty string) and
  `false` otherwise; `dp[i][0] = false` for `i > 0`. Transition for `i, j >= 1`: if
  `p[j-1] == '*'`, `dp[i][j] = dp[i-1][j] || dp[i][j-1]` (the star consumes one more
  character of `s`, or matches nothing more); if `p[j-1] == '?'` or `p[j-1] == s[i-1]`,
  `dp[i][j] = dp[i-1][j-1]`; otherwise `dp[i][j] = false`.
- Anchors: `init` (base row/column), `starMatch` (the OR-of-two-reads transition),
  `directMatch` (`?` or exact character match, copies the diagonal), `mismatch` (neither
  applies — dead cell), `done`.
- Default: `s="adceb"`, `p="*a*b"` → `true`. Alternate: `s="acdcb"`, `p="a*c?b"` → `false`.
  Both hand-verified against a Python port of the recurrence before writing any Java; each
  input independently exercises all five anchors (including both `starMatch` and
  `mismatch`), and the two inputs disagree on the final answer (`true` vs `false`), not just
  on table size.

## Sequencing

One PR — `feat/trace-dp-batch3` — implementing both, plus the `bulkDsType` fix (that fix has
no tracer dependency and is safe to land in the same commit, same precedent as batch 8's
`TreeService.bulkDsType` fix landing alongside its own tracers rather than as a separate PR).

## Explicitly out of scope

- `ninja-and-his-friends` — deferred, see above. Needs its own design pass on rendering a 3D
  recurrence before it can be scoped with the same confidence as this pair.
