---
name: audit-topic
description: >
  Audit a whole catalogue category against this repo's definition of done — every id
  either traced or honestly untraced, no id still falling through a service `switch`'s
  `default:` to another algorithm's steps, no dead legacy generators left behind, the
  topic's catalogue `dsType`s not a blanket bulk value, no two tracers in the topic
  emitting near-identical traces, the `ProblemsApiTest` canonical-untraced-example chain
  still pointing at a genuinely untraced id, and every coverage number in the docs
  reconciled against the live stats endpoint. Run it before declaring a topic complete,
  and after any batch PR that traces more than one id.
---

# audit-topic

## Why this exists

`audit-question` proves one problem is wired correctly. A topic can be made entirely of
correct problems and still be broken, because the failures that survive a per-id audit are
the ones that live *between* ids:

- One id in a batch of twenty that nobody added a `case` for, so it still falls through
  `default:` and streams another algorithm's steps under its own name.
- A bulk catalogue registration that hardcodes one `dsType` for the whole topic — ten
  Recursion & Backtracking ids were registered as `"Stack"` while their tracers promised
  `MATRIX`, `STRING` and `BITS`.
- Two tracers in the same topic that are structurally so alike their traces are
  interchangeable — `min-bit-flips` and `count-set-bits`, `painters-partition` and
  `book-allocation`, `power-set` and `subsets-i`.
- A `ProblemsApiTest` assertion that pins "this catalogued id is untraced" pointing at an
  id a *later* batch traced. That chain has moved three times in four PRs:
  `bracket-reversals` → `search-insert-position` → `middle-linked-list` →
  `longest-common-subsequence`. Each move was a test failure, not a design decision.
- A coverage number in `README.md` that no longer matches `/api/problems/stats`.

**What this is not.** It is not `audit-question` run in a loop — it assumes that, and
tells you which ids still need it. It does not review the narration
(`review-trace-simulation`), a diff (`dsa-review`), or repo-wide totals
(`trace-coverage`).

The doctrine from `trace-coverage` holds here and is the whole discipline: **never quote a
count from a document. Run the command.**

---

## Setup

```bash
cd backend && mvn spring-boot:run                             # in another shell
python3 .claude/skills/audit-topic/audit_topic.py             # every category, traced/total
```

Pick the exact category string from that listing — the catalogue's own, not a heading from
`PROJECT_COMPLETION_PLAN.md`.

```bash
TOPIC="Recursion & Backtracking"
```

---

## 1. The mechanical sweep

```bash
python3 .claude/skills/audit-topic/audit_topic.py "$TOPIC"
```

One row per problem: traced, `dsType`, step count, legacy status, and flags. It exits 1 on
a dead anchor, a legacy endpoint still answering 200, a duplicate id, truncation on
defaults, a trace that ends mid-recursion, or a near-identical trace pair inside the topic.

Run against `Recursion & Backtracking` immediately after that topic was declared complete,
it reported 25/25 traced, every legacy path 410 — and five findings no test had caught:

```
m-coloring                    DEAD ANCHORS ['undo']
sudoku-solver                 DEAD ANCHORS ['backtrack', 'deadEnd']
word-break                    DEAD ANCHORS ['memoHit', 'noSegmentation']
word-search                   DEAD ANCHORS ['exhausted']
subsequences-patterns-theory  ENDS MID-RECURSION (call stack 4 deep on the last step)
```

That is the expected yield of a first audit on a "finished" topic, and the reason the
verdict below distinguishes *complete* from *green*.

Read the columns for patterns, not just for red:

- **`ENDS MID-RECURSION`** means the final step's call stack is non-empty. The `push`/`pop`
  calls usually *do* balance — what is missing is a summary step emitted after the
  unwinding, so the sidebar freezes showing frames the viewer never watches drain.
  `Subsets2Tracer` emits a closing step from `run()` after `backtrack` returns and ends at
  depth 0; `SubsequencesPatternsTheoryTracer` does not. Decide which is right for the
  algorithm; do not assume a bug.

- **The same dead anchor on several rows is one bug repeated.** An anchored loop header
  that nothing emits shows up across a whole batch when the tracers were written together.
- **A whole column of identical `dsType` values** is the bulk-registration smell of §4 —
  legitimate for a genuinely homogeneous topic (Bit Manipulation really is all `BITS`),
  suspicious for one that spans matrices, strings and stacks.
- **Step counts clustered at a suspiciously round number** near 5000 mean the budget is
  truncating, not that the algorithms agree.

Everything the sweep flags is a finding; everything it does not flag still needs §2–§8.

## 2. Coverage is complete or honestly incomplete

The sweep's footer gives `traced/total` for the topic. There are only two acceptable
states, and "mostly done" is not one of them:

- **Complete** — every id traced. Then §3's legacy sweep must show 410 for *all* of them,
  and the topic's controller answers 410 for everything it claims. Arrays, Sorting,
  Sliding Window, Bit Manipulation and Strings are already in this state; it is real and
  permanent, and `ApiContractTest` documents it.
- **Partial** — some ids untraced. Then each untraced id must answer **501** from
  `/api/problems/{id}/execute` (catalogued, not yet traced) and must have **no**
  `inputSpec`. `traced` is an honesty flag, not a feature flag: the UI says "not yet
  traced" rather than animating the wrong thing.

The state that is never acceptable is an id the sweep marks `410 but no tracer` — the
legacy generator was retired without a tracer replacing it, so the problem is unreachable
from both APIs.

## 3. Every traced id's legacy path refuses

The sweep already probed this; confirm it is *pinned by a test*, which the sweep cannot
see. Open the topic's service and its test:

```bash
grep -n 'case "' backend/src/main/java/com/dsa/ui/service/<Topic>Service.java | head -40
grep -n "LegacyTraceRetiredException" backend/src/test/java/com/dsa/ui/**/<Topic>*Test.java
```

Three things must agree, and drift between them is the classic straggler:

1. Every traced id in the topic has a `case` in the `switch` falling into
   `throw new LegacyTraceRetiredException(problemId)`.
2. The service test asserts `LegacyTraceRetiredException` for those ids — the whole set,
   not a sample. `RecursionBacktrackingTracingTest` is the shape to copy: every test in it
   is an `assertThrows` with a message saying why.
3. `ApiContractTest`, parameterized over all 18 base paths, carries the topic's route
   cases.

Read what `default:` still returns. In a partially traced topic it legitimately serves the
remaining untraced ids' legacy steps. In a **fully** traced topic a `default:` that returns
steps is a live fallback waiting for the next id someone forgets to add — the tracer layer
exists to make that impossible, so it should throw.

## 4. The catalogue `dsType`s are per-problem, not blanket

```bash
grep -n "bulkDsType\|DsType\." backend/src/main/java/com/dsa/ui/service/<Topic>Service.java
```

Topics registered in bulk pass one `dsType` through a helper. If that helper returns a
constant, every problem in the topic claims the same canvas regardless of what its tracer
emits. This is exactly what `CatalogTracerMetadataTest.catalogueDsTypesMatchEveryRegisteredTracer`
caught across ten Recursion & Backtracking ids at once, and the fix is a `switch` that
names the exceptions:

```java
private static DsType bulkDsType(String id) {
    return switch (id) {
        case "atoi-recursive", "generate-binary-strings", "generate-parentheses",
                "letter-combinations-phone", "word-break" -> DsType.STRING;
        case "count-good-numbers", "pow-x-n-recursive" -> DsType.BITS;
        case "permutations" -> DsType.ARRAY;
        case "word-search" -> DsType.MATRIX;
        default -> DsType.STACK;
    };
}
```

Note that the test only proves catalogue and tracer *agree*. Agreement on a wrong value is
still wrong: check each `dsType` in the sweep's column against what the algorithm actually
draws. And remember `ARRAY` and `BITS` have no payload requirement in
`DsTypePayloadContractTest` — an `ARRAY`-tagged tracer in this topic that never calls
`.array(...)` or `.arrayState(...)` passes every test and renders nothing.

## 5. No dead legacy generators left behind

Retiring an id means deleting the generator it used to call, not just refusing to call it.

```bash
grep -n "private .* generate.*Steps()" backend/src/main/java/com/dsa/ui/service/<Topic>Service.java
```

Every method still present must be reachable — from a surviving `case`, from `default:`,
or from another generator. Anything else is dead code that keeps a wrong animation alive
in the repository, ready to be wired back in. The Bit Manipulation retirement deleted 11
such methods and deliberately kept one, `generateCheckNumberOddSteps()`, because
`default:` still calls it; that reasoning belongs in the diff, not in the reader's head.

The same applies to the standalone algorithm classes some topics carry:

```bash
ls backend/src/main/java/com/dsa/ui/algorithm/<area>/
```

The Recursion & Backtracking retirement deleted seven orphaned classes there and kept
`NQueens.java` and `SudokuSolver.java` because the surviving tracers use them. An
unreferenced class in that package is the legacy layer's residue.

## 6. No two tracers in the topic tell the same story

`TracerContractTest.noTwoTracersProduceIdenticalTraces` only fires on **exact** equality of
the whole fingerprint, repo-wide. Two tracers in one topic that differ in a single step
number pass it while being pedagogically interchangeable. The sweep reports pairs above
0.98 similarity; investigate every one.

The fix is never to perturb a value until the ratio drops. It is to make the two traces
teach different things, by changing what each one *narrates*:

- `PowerSetTracer` captures at every node; `SubsetsTracer` captures at the pick/non-pick
  leaves. Same output set, genuinely different tree.
- `MinBitFlipsTracer` narrates the XOR-then-count-bits framing; `CountSetBitsTracer`
  narrates Brian Kernighan's clear-lowest-set-bit loop.
- `SetUnsetRightmostBitTracer` and `CheckPowerOf2Tracer` both hinge on `n & (n-1)`, so
  each says explicitly what that expression means *for its own question*.

## 7. The cross-topic assertions this batch just invalidated

Tracing a topic can break tests that name ids in *other* topics. Two known chains:

**The canonical untraced example.** `ProblemsApiTest` needs a catalogued-but-untraced id to
prove 501 and the absent `inputSpec`. When a batch traces that id, three assertions fail at
once:

```bash
grep -n "untraced\|isNotImplemented\|legacyEndpointsUnaffected" -A4 \
  backend/src/test/java/com/dsa/ui/ProblemsApiTest.java
```

Repoint them at an id that is *currently* untraced and unlikely to be traced next —
confirm with the sweep, not from memory. `legacyEndpointsUnaffected` needs the same
treatment: it asserts a legacy endpoint still answers 200, so it must name an id whose
topic still has a live `default:`.

**The pinned numbers.** `ProblemsApiTest` asserts 433 unique ids and 7 duplicates. Tracing
does not move either, so if they move, a `ProblemDetail` was added or removed — that is a
separate change needing its own justification and a `README.md` coverage-table update in
the same commit.

## 8. Regenerate, run, and reconcile the docs

```bash
cd backend && mvn test -Djacoco.skip=true \
  -Dtest=TracerContractTest,DsTypePayloadContractTest,CatalogTracerMetadataTest,ApiContractTest,ProblemsApiTest
cd backend && mvn test                                     # the whole suite, green
cd frontend && npx vitest run && npx vite build
```

`designTokens.test.js` and the canvas registry cross-tier test are part of that frontend
run: a topic whose tracers introduced a new `dsType` must have a registry entry, or the
canvas selection test fails.

Then take every number in the docs from the live endpoint, never from another document:

```bash
curl -s http://localhost:8923/api/problems/stats | python3 -m json.tool
```

Reconcile `README.md` (status line and coverage table), `HANDOFF.md` (tracer count),
`PROJECT_CONTEXT.md`, and `PROJECT_COMPLETION_PLAN.md` (topic status table, completed-work
bullets, and the **cascading expected-result numbers of every later topic** — those are
written as running totals, so completing one topic invalidates the projections of all the
ones after it). Stale projections were the single most common doc defect across the last
four topic PRs.

---

## Verdict

State the outcome as exactly one of:

- **Topic complete** — N/N traced, every legacy path 410 and pinned by a test, `default:`
  throws, no dead generators or orphaned algorithm classes, per-problem `dsType`s, no
  near-identical pairs, cross-topic assertions repointed, full suite green, docs
  reconciled against live stats. Give the numbers you read from `/stats`.
- **Topic honestly partial** — M/N traced; every untraced id answers 501 with no
  `inputSpec`; the remaining ids listed by name so the next batch has its worklist.
- **Topic not done** — name the id and the observation: "`§3` `count-good-numbers` has a
  tracer but no `case` in `RecursionBacktrackingService`, so
  `/api/recursion-backtracking/execute/count-good-numbers` returns 200 serving
  `generateNQueensSteps()`." Never "the topic looks incomplete."

Any fix follows the repo rule: prove the new assertion fails against the old code first
(`prove-the-test-fails`), regenerate affected goldens with
`mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true` and **read the diff** —
confirming `git status` shows only the fixtures you expected — then the full suite green
before it lands. Record recurring classes of defect in `RCA.md`; a straggler left in a
`default:` and a blanket bulk `dsType` are both recurring.
