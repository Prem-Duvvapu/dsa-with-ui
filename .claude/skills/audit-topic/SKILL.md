---
name: audit-topic
description: >
  Audit a whole catalogue category against this repo's definition of done — every id
  either traced or honestly untraced, nothing orphaned in `algorithm/`, the topic's
  catalogue `dsType`s not a blanket bulk value, no two tracers in the topic
  emitting near-identical traces, every recursion trace draining its call stack, and every
  coverage number in the docs reconciled against the live stats endpoint. Run it before declaring a topic complete,
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

Pick the exact category string from that listing — the catalogue's own, not a category
name half-remembered from a doc.

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

- **Complete** — every id traced. **All 18 topics are in this state** and have been since
  the catalogue finished migrating; it is real and permanent. Nothing about the legacy
  layer needs checking any more (§3).
- **Partial** — some ids untraced. Then each untraced id must answer **501** from
  `/api/problems/{id}/execute` (catalogued, not yet traced) and must have **no**
  `inputSpec`. `traced` is an honesty flag, not a feature flag: the UI says "not yet
  traced" rather than animating the wrong thing.

The state that is never acceptable is an id the sweep marks `410 but no tracer` — the
legacy generator was retired without a tracer replacing it, so the problem is unreachable
from both APIs.

## 3. The legacy layer is gone — nothing to check here

**Historical.** This step used to verify that every traced id's `/api/{topic}/execute/{id}`
refused rather than serving another algorithm's steps, and that a test pinned the refusal.

That layer no longer exists. The eighteen legacy controllers are deleted, `/api/{topic}/...`
404s, and no service has a `generateSteps` or a `switch (problemId)` at all. The services
survive only as `ProblemProvider`s owning `ProblemDetail` metadata.

So: **skip this step**, ignore the sweep's `legacy` column (it reads 404 for everything
now), and do not go looking for `LegacyTraceRetiredException` — it is deleted too. What
replaced the checks:

- `ProblemsApiTest.legacyRoutesNoLongerExist` asserts the routes stay gone.
- `ProblemProviderContractTest` covers all eighteen providers' catalogue contract.

The lesson worth keeping is why the straggler class existed: the refusals were pinned by
*hand-maintained lists* of retired ids, in `ApiContractTest` and in eighteen copy-pasted
`*ServiceTest` classes. Drift in those lists is what let ids keep falling through
`default:`. Wherever you are tempted to write a per-id list in a test, parameterize over
the live registry or catalogue instead.

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

## 5. Nothing orphaned in `algorithm/`

**Mostly historical.** This step used to hunt unreachable `generate*Steps()` methods in the
topic's service. Those are all deleted along with the legacy layer — no service has one.

What still applies is the standalone helper classes:

```bash
ls backend/src/main/java/com/dsa/ui/algorithm/
```

That package is down to `trie/ImplementTrie.java`, kept because `ImplementTrieTracer` uses
it, plus `trace/TraceEvent` and `trace/TraceRecorder` which it needs. Eleven other classes
were the legacy layer's residue and are gone. An unreferenced class there is dead code;
check reachability transitively, since deleting one caller can orphan its callees:

```bash
python3 - <<'EOF'
import os,re,glob
os.chdir('backend/src/main/java/com/dsa/ui')
srcs={p:open(p).read() for p in glob.glob('**/*.java',recursive=True)}
for t in sorted(p for p in srcs if p.startswith(('algorithm/','trace/'))):
    cls=os.path.basename(t)[:-5]
    if not [p for p,s in srcs.items() if p!=t and re.search(r'\b'+cls+r'\b',s)]:
        print('ORPHAN', t)
EOF
```

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

**Historical, but re-read if the catalogue ever grows again.** While the migration was in
progress, tracing a topic routinely broke tests that named ids in *other* topics:
`ProblemsApiTest` kept one hardcoded "canonical untraced id" to prove 501 and the absent
`inputSpec`, and that id moved three times across four PRs (`bracket-reversals` →
`search-insert-position` → `middle-linked-list` → `longest-common-subsequence`) as each
move's target got traced out from under it. A companion assertion,
`legacyEndpointsUnaffected`, similarly needed a topic with a still-live legacy `default:`.

Neither exists any more. `untraced` is 0 — there is no catalogued-but-untraced id left to
anchor a canonical example on — and the legacy layer that `legacyEndpointsUnaffected`
tested is deleted; `legacyRoutesNoLongerExist` (§3) replaced it with an unconditional
404 assertion that names no id and cannot go stale this way. So on the current catalogue,
this step is a no-op: `audit_topic.py` reporting `N/N` for your topic is not something
that needs a cross-topic assertion repointed.

**It comes back the moment someone catalogues a new problem ahead of its tracer** — a new
topic added to the catalogue, a new sheet of problems, anything that makes `untraced`
non-zero again. If that happens, check whether `ProblemsApiTest` grew a new
canonical-untraced-id assertion pointing at your topic's ids, and repoint it before your
batch traces them out from under it — confirm with the sweep, not from memory.

**The pinned numbers.** `ProblemsApiTest` asserts 431 unique ids and 0 duplicates. Tracing
does not move either, so if they move, a `ProblemDetail` was added or removed — that is a
separate change needing its own justification and a `README.md` coverage-table update in
the same commit.

## 8. Regenerate, run, and reconcile the docs

```bash
cd backend && mvn test -Djacoco.skip=true \
  -Dtest=TracerContractTest,DsTypePayloadContractTest,CatalogTracerMetadataTest,ProblemProviderContractTest,ProblemsApiTest
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

Reconcile `README.md` (status line and coverage table) and `PROJECT_CONTEXT.md`'s coverage
note against those numbers. If your batch is (re-)traversing already-migrated ground — the
normal case now that the catalogue is 431/431 — also check `AUDIT.md`'s findings and
`RCA.md`'s open items for anything your topic touches; a fix that resolves one of those
should update the entry rather than leave it reading as still-open. `HANDOFF.md` and
`PROJECT_COMPLETION_PLAN.md`, which used to carry a **cascading expected-result number for
every later topic** here, are deleted — stale projections in those two were the single most
common doc defect across the topic-PR era they covered; there is no equivalent running
total to maintain now that migration is complete.

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
