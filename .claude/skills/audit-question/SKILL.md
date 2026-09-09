---
name: audit-question
description: >
  Audit one problem id against this repo's definition of done — catalogued, traced, its
  catalogue `dsType` agreeing with its tracer's, an `InputSpec` that actually drives
  `run()` rather than a fixture, a materially different `alternateInput()`, every declared
  `// @a` anchor reachable, a payload the promised canvas can render, a golden file that
  was read, a legacy endpoint that answers 410, and budget headroom. Run it before
  claiming a single problem is finished, and on every id a review flags. Use
  `review-trace-simulation` for the narration quality pass; use `audit-topic` to sweep a
  whole category.
---

# audit-question

## Why this exists

"Traced" is a boolean on a catalogue entry, and it is set by the mere existence of a
`@Component` whose `id()` matches. Nothing about that boolean promises the animation is
the right one, that the input form does anything, or that the legacy path stopped serving
substitute steps. This repo's founding defect was 303 problems that were *catalogued,
answered 200, and returned another algorithm's trace* — and a suite of 90 tests that all
passed, because their only per-problem assertion was `!steps.isEmpty()`.

So this skill audits **one id** end to end and answers a single question: **if a learner
opened this problem right now, would everything they see be true of this problem?**

**What this is not.** It does not judge whether a step's prose is good — that is
`review-trace-simulation`, and it is the natural follow-up when this audit passes. It does
not review a diff — that is `dsa-review`. It does not count coverage — that is
`trace-coverage`. It also is not a substitute for `mvn test`: several checks below exist
precisely because the contract tests have holes, and the holes are named.

Throughout: **do not quote a value from a source file if an endpoint will tell you.** The
running backend is the ground truth; the docs have been wrong before.

---

## Setup

```bash
cd backend && mvn spring-boot:run          # in another shell; http://localhost:8923
ID=combination-sum-2                       # the id under audit
```

Everything below assumes `$ID` and a live backend on 8923.

---

## 1. It exists, it is traced, and the metadata is honest

```bash
curl -s "http://localhost:8923/api/problems/$ID" | python3 -m json.tool | head -30
```

Three things must hold, and each has failed here before:

- **`traced` is `true`.** If it is `false`, `GET /api/problems/$ID/execute` answers **501**
  and there is nothing further to audit — the honest state, not a bug. If the id is not
  catalogued at all you get **404**. Those two codes are deliberately distinct; a 404 for a
  catalogued id means `ProblemCatalog` lost the entry.
- **`dsType` matches the tracer's `dsType()`.** `CatalogTracerMetadataTest.catalogueDsTypesMatchEveryRegisteredTracer`
  enforces this, and it is the check that caught ten Recursion & Backtracking ids at once:
  their bulk catalogue registration hardcoded `"Stack"` while `WordSearchTracer` promised
  `MATRIX` and `AtoiRecursiveTracer` promised `STRING`. Read the two values yourself
  anyway — the test only proves they agree, not that either is *right* for the algorithm.
- **`inputSpec` is present.** `ProblemsApiTest.inputSpecPresentOnlyWhenTraced` pins the
  correspondence in both directions: traced ⇒ spec, untraced ⇒ no spec.

Then confirm the id is not one of the seven claimed twice:

```bash
curl -s http://localhost:8923/api/problems/stats | python3 -m json.tool
```

If `$ID` appears in `duplicateIds`, the entry you just read is whichever provider
registered first, and a second `ProblemDetail` for the same id is being silently
discarded. `ProblemCatalog` surfaces these rather than hiding them; an audited id that
shows up there needs the losing entry deleted, not the winner blessed.

## 2. The input spec is a contract, not decoration

```bash
curl -s "http://localhost:8923/api/problems/$ID/input-spec" | python3 -m json.tool
```

Read the spec beside `run()` in `backend/src/main/java/com/dsa/ui/tracer/impl/`:

- **Every field the spec declares is read by `run()`** via `in.getInt` / `in.getIntArray` /
  `in.getGrid` / `in.getString` / `in.getGraph` / `in.getList` / `in.getTree`. A declared
  field nothing reads is a form control that does nothing.
- **`run()` reads no data the spec does not declare.** Constants baked into `run()` that a
  learner sees on screen but cannot change are the fixture-shaped version of the original
  defect. The legacy `ProblemDetail` carries `defaultArray` / `defaultGrid` /
  `defaultTreeNodes` fields for exactly that old style — a v2 tracer must not depend on
  them.
- **Constraints live in the spec, not in `run()`.** `InputValidator` is the only trust
  boundary; it rejects unknown fields rather than ignoring them and collects *all* field
  errors into one 400. If `run()` throws `InputValidationException` for something a
  `.range()`, `.sorted()`, `.distinct()`, `.length()` or `.constraint("pattern", …)` could
  have expressed, move it. Reserve runtime rejection for genuinely cross-field or
  structural conditions (`RatInAMazeTracer` requires the maze be square — no `InputField`
  constraint can say that).
- **A per-field size ceiling exists.** Together with the global 5000-step budget this is
  mandatory, not optional: without it a caller sets `n = 20` on a factorial-time problem
  and takes the server down.

Prove the boundary works, per field:

```bash
curl -s -X POST "http://localhost:8923/api/problems/$ID/execute" \
  -H 'Content-Type: application/json' -d '{"nonsenseField": 1}'
# expect 400 with fieldErrors naming the unknown field — never a silent ignore
```

## 3. The default input exercises the algorithm

```bash
curl -s "http://localhost:8923/api/problems/$ID/execute" \
  | python3 -c "import json,sys;d=json.load(sys.stdin);print(d['stepCount'],'steps, truncated:',d['truncated']);print(d['resolvedInput'])"
```

- `truncated` must be `false` on defaults — `TracerContractTest.runsOnDefaults` fails
  otherwise — but comfortably false, not one step under 5000. A default sized near the cap
  makes the animation unwatchable.
- The step count should be proportional to the work. A 40-element array producing 6 steps
  means most iterations are silent.
- The default must reach the interesting branches. `binary-search-1d` shipped with
  `nums=[1,3,5,7,9,11,13], target=7`, so the very first `mid` was a hit: three steps, and
  neither comparison branch nor the not-found path ever ran. Section 4 catches this
  mechanically.

## 4. Every declared anchor is reachable

```bash
python3 .claude/skills/review-trace-simulation/check_trace.py "$ID"
```

A dead anchor means the code viewer shows a line the highlight never lands on. It is
almost always a symptom of a **badly chosen default, not a missing `emit` call** — fix the
default first. `search-rotated-sorted-2` declared seven anchors and never highlighted
`miss`, because neither its default nor its alternate target was absent from the array;
the fix was changing the alternate from `0` to `5`, not adding an emit.

The converse is already enforced: `AnnotatedCode` rejects duplicate, unnamed and dangling
anchor names at parse time, so a typo fails a test rather than highlighting a wrong line.

Live anchors are not automatically *correct* anchors. `tree-inorder` once ended with
"Traversal complete… [4, 2, 5, 1, 3, 6]" while highlighting `if (node == null) return;` —
resolvable, in range, non-blank description, and wrong. Only reading the step beside the
code catches that, which is `review-trace-simulation`'s job.

## 5. `alternateInput()` is materially different — and the trace grows

```bash
python3 .claude/skills/review-trace-simulation/check_trace.py "$ID" --alt '{"…"}'
```

`alternateInputDiffersFromDefaults` only rejects an alternate *pasted from the spec
defaults*, and `traceRespondsToItsInput` passes on a **single** differing step. Both are
weaker than they look. "Materially different" means a different shape of execution — an
absent target where the default hits, an empty result where the default finds one, a
pruned subtree where the default explores it — not a permutation of the same numbers.

Then the check that finds canned work, `TracerContractTest.stepCountGrowsWithInput`. Know
what it actually does before trusting or debugging it:

- Only `INT_ARRAY`, `INT_GRID`, `LINKED_LIST` and `BINARY_TREE` fields are grown. A tracer
  whose only inputs are scalar `INT` or `STRING` is `assumeTrue`-skipped — **the check does
  not run at all**, and you must reason about growth by hand.
- `growList` prepends copies of `filler = maxValue` (honouring `requireSorted` /
  `requireDistinct`). `growGrid` tiles rows and columns by modulo, capped by `maxRows` /
  `maxCols`, and **has no concept of sortedness**.

Both growers are structure-blind, so any algorithm with a *structural* prune can be immune
to growth while being perfectly honest. Every such failure in this repo was fixed by
choosing a better default, never by weakening the harness:

- `combination-sum-2` emitted 51 steps for default and grown input alike — `filler = 30`
  sorted to the end and always exceeded the target of 8, so `pruneRest` skipped it and the
  search tree never changed. Fixed by `.values(1, 30)` → `.values(1, 8)` plus a new default.
- `minimize-max-distance-gas-station` ran a fixed 15 iterations, so array size was
  irrelevant; fixed by emitting a per-gap step *inside* each iteration.
- `search-2d-matrix` grew to *fewer* steps, and `search-2d-matrix-2` and
  `find-peak-element-2d` did not move at all, because `growGrid`'s tiling destroys the
  sortedness those searches rely on. Fixed by simulating the exact tiling in Python and
  brute-forcing defaults that provably grow.

When a growth failure is subtle, model the grower in a throwaway script and search for a
default, rather than editing shared test infrastructure.

## 6. The payload matches the canvas the `dsType` promises

```bash
curl -s "http://localhost:8923/api/problems/$ID/execute" \
  | python3 -c "
import json,sys
d=json.load(sys.stdin); s=d['steps'][0]
print(s['dsType'])
print({k:('set' if v not in (None,[],{}) else 'empty')
       for k,v in s.items() if k.endswith('State') or k in ('dpTable','treeNodes','graphNodes')})"
```

`DsTypePayloadContractTest` registers a required payload for **STACK/QUEUE, MATRIX,
LINKED_LIST, TREE, GRAPH, DP_TABLE and TRIE only**. `ARRAY` and `BITS` have no requirement
and are `assumeTrue`-skipped, so an `ARRAY` tracer that emits no `arrayState` at all
passes every test and renders an empty canvas. Check those two by hand — that is the whole
reason this section exists.

`SEARCH_SPACE` is declared on the enum but has no `StepEmitter` method and no tracer uses
it; tagging a problem with it produces a step nothing can draw.

Canvas selection has one source of truth, `frontend/src/canvas/registry.js`, keyed by the
backend's `DsType` contract. Some values intentionally map to a generic renderer pending
Phase 3 — that is explicit mapping, not a fallback. Confirm the id's `dsType` has a
registry entry that renders something meaningful; retagging metadata without supplying the
matching trace payload *and* canvas behaviour is the failure this rule exists to prevent.

Recursive tracers must balance `emit.push(frame)` / `emit.pop()`, or the call-stack sidebar
drifts. The sidebar is independent of `dsType`, so no payload test covers it:

```bash
curl -s "http://localhost:8923/api/problems/$ID/execute" \
  | python3 -c "
import json,sys
d=json.load(sys.stdin)
print('final call stack:', d['steps'][-1]['callStack'])   # expect [] for a completed run"
```

## 7. The golden file exists and someone read it

```bash
ls backend/src/test/resources/golden/$ID.json
python3 -c "
import json
d=json.load(open('backend/src/test/resources/golden/$ID.json'))
print(d['stepCount'],'steps  anchors:',d['anchors'])
print('final:', d['steps'][-1]['description'], d['steps'][-1]['variables'])"
```

Golden files pin trace **content** — descriptions, variables, highlighted lines — which
every other test is blind to. `GoldenTraceTest.everyGoldenFileHasATracer` and
`traceMatchesItsGoldenFile` guarantee only that the file and the tracer agree, which a
regeneration also guarantees. So the audit question is not "does it pass" but **"is the
final answer in it correct?"** Compute the expected result independently and compare.
Regenerating a golden without reading the diff records a bug as expected.

## 8. The legacy path refuses rather than substitutes

Find the id's legacy base path from its category:

| category | legacy base path |
| --- | --- |
| Arrays | `/api/arrays` |
| Graphs — BFS/DFS | `/api/graphs/bfs-dfs` |
| Advanced Graphs | `/api/graphs/advanced` |
| Binary Trees / BST | `/api/trees` |
| Tries | `/api/tries` |
| Sorting | `/api/sorting` |
| Binary Search | `/api/binarysearch` |
| Dynamic Programming | `/api/dp` |
| Greedy | `/api/greedy` |
| Heaps | `/api/heaps` |
| Linked List | `/api/linkedlist` |
| Recursion & Backtracking | `/api/recursion-backtracking` |
| Sliding Window | `/api/slidingwindow` |
| Stack & Queue | `/api/stackqueue` |
| Strings | `/api/strings` |
| Bit Manipulation | `/api/bitmanipulation` |
| Maths | `/api/maths` |
| Learn the Basics | `/api/basic-recursion` |

```bash
curl -s -o /dev/null -w '%{http_code}\n' "http://localhost:8923/api/<base>/execute/$ID"
```

- **410** — correct for a traced id. Its case in the service `switch` throws
  `LegacyTraceRetiredException`, and `ApiExceptionHandler` maps that to 410 Gone pointing
  callers at `/api/problems/{id}/execute`.
- **200** — the id still falls through to `default:`, which returns *another algorithm's*
  steps under this id's name. This is the original defect, alive. The tracer being correct
  does not excuse it: two endpoints now disagree about what this problem is.
- **404** — correct only for an id this controller never claimed.

Then confirm a test pins it, so the next refactor cannot quietly restore the fallback:
the topic's service test (`BitManipulationServiceTest`, `RecursionBacktrackingTracingTest`,
…) should assert `LegacyTraceRetiredException` for this id, and `ApiContractTest` — which
is parameterized over all 18 base paths — should carry its route case.

## 9. Budget headroom on the alternate too

```bash
curl -s -X POST "http://localhost:8923/api/problems/$ID/execute" \
  -H 'Content-Type: application/json' -d '<alternate input>' \
  | python3 -c "import json,sys;d=json.load(sys.stdin);print(d['stepCount'],d['truncated'])"
```

`stepBudgetTruncates`, `byteBudgetTruncates` and `byteEstimateTracksActualPayload` prove the
caps *work*; they say nothing about whether this problem's declared maxima can reach them.
Take the spec's largest legal input and check that a learner who maxes out the form gets a
finished trace rather than `truncated: true` — or, if truncation at the ceiling is genuine
and unavoidable, that the ceiling is low enough to be honest about it.

---

## Verdict

State the outcome as exactly one of:

- **Done** — traced, metadata agrees, the spec drives `run()`, every anchor reachable,
  alternate materially different and the trace grows, payload matches the canvas, golden
  read and its final answer verified, legacy path 410 with a test pinning it, headroom at
  the ceiling. Say which of these you verified *live* versus read from source.
- **Done with a named follow-up** — e.g. "the `none` anchor is genuinely unreachable on a
  solvable default; documented in the tracer's Javadoc." Name it; do not leave it implied.
- **Not done** — name the id, the section, and the observed value: "`§8`
  `/api/dp/execute/lis-1` returns 200, serving `generateLcsSteps()`." Never "the tracer
  looks incomplete."

If the audit produces a fix, the repo rule applies: prove the new assertion fails against
the old code first (see `prove-the-test-fails`), regenerate any affected golden and **read
the diff**, then `cd backend && mvn test` green before it lands. Log the defect in `RCA.md`
if it belongs to a recurring class — a dead anchor from a midpoint-hit default and a
`stepCountGrowsWithInput` immunity both do.

Finally, if this id passes, hand it to `review-trace-simulation`. This skill proves the
problem is wired correctly; that one proves the narration is worth reading.
