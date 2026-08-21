---
name: review-trace-simulation
description: >
  Quality gate for an AlgorithmTracer's emitted trace in the dsa-with-ui repo. Use when
  reviewing, accepting, or debugging a tracer in backend/src/main/java/com/dsa/ui/tracer/impl/,
  when someone says a trace "looks fine" or "the animation works", when a step description
  seems generic, when checking that every `// @a` anchor is actually reachable, when a
  branch of the algorithm is never exercised by the default input, or before marking a
  problem `traced`. Catches plausible-looking but shallow traces that TracerContractTest
  passes anyway.
---

# Reviewing a trace simulation

`TracerContractTest` proves a tracer is *not a canned narration*. It does **not** prove the
trace is a *good* simulation. Three of its holes, all real:

| Test | What it actually asserts | What it misses |
|---|---|---|
| `anchorsAreAllReachable` | `usedLines` is non-empty | Despite the name, it never compares declared anchors to highlighted lines. **6 of the 8 current tracers have dead anchors and it passes.** |
| `stepsAreWellFormed` | description non-blank, `activeLine` in range | A description that is non-blank and wrong |
| `traceRespondsToItsInput` | two inputs → two different fingerprints | One differing step out of twenty is enough to pass |

This skill is the pass that closes those. Two parts: a mechanical sweep you run, and a
reading pass only a human/model can do.

---

## Part 0 — set up

```bash
cd backend && mvn spring-boot:run          # leave running; serves http://localhost:8923
```

Verify it is up (expect a JSON object with `catalogued`, `traced`, `untraced`):

```bash
curl -s http://localhost:8923/api/problems/stats
```

---

## Part 1 — mechanical sweep

```bash
python3 .claude/skills/review-trace-simulation/check_trace.py <problem-id>
python3 .claude/skills/review-trace-simulation/check_trace.py          # every traced problem
python3 .claude/skills/review-trace-simulation/check_trace.py binary-search-1d \
    --alt '{"nums":[2,4,6,8,10,12,14,16],"target":16}'
```

stdlib Python only — **do not add a dependency to run a review.**

It reports per problem: step count, `truncated`, declared-vs-highlighted anchors, the
source line of every dead anchor, and whether `--alt` changes the trace. Exit 1 on a dead
anchor or an identical trace.

### Baseline as of this writing (run it, do not trust this table)

```
number-of-islands   12 steps   dead: scan
two-sum              5 steps   dead: check, loop, none
kadane-algo         17 steps   dead: loop
binary-search-1d     3 steps   dead: left, loop, miss, right
bfs-traversal       21 steps   dead: loop, neighbours
reverse-linked-list 14 steps   dead: loop
tree-preorder       12 steps   dead: none
tree-inorder        12 steps   dead: none
```

Read the table the way a reviewer should:

- **`loop` on five tracers is one bug repeated.** The `for`/`while` header is anchored but
  never emitted, so the code viewer never highlights the line where iteration advances.
  Either emit it once per iteration, or delete the anchor. Do not leave it declared.
- **`binary-search-1d` is the serious one.** Three steps, and `left`, `right` and `miss`
  are all dead — because its default is `nums=[1,3,5,7,9,11,13], target=7`, so the very
  first `mid` is a hit. **Neither branch of the comparison ever runs, and the not-found
  path never runs.** `HANDOFF.md` PROMPT C says this in advance: "a binary search default
  whose target sits at the midpoint teaches nothing." A dead anchor is usually a symptom of
  a badly chosen default, not of a missing `emit` call — fix the default first.
- **`two-sum`'s dead `none`** is the return-empty path: legitimately unreachable when the
  default input has a solution. That is the one case where the honest fix is a comment
  saying so, or a different default. Decide, do not ignore.

---

## Part 2 — the reading pass

Open the tracer beside its trace output:

```bash
curl -s http://localhost:8923/api/problems/<id>/execute \
  | python3 -c "import json,sys;d=json.load(sys.stdin);print(d['code']);print('---');[print(s['stepNumber'],'L'+str(s['activeLine']),s['description'],s['variables']) for s in d['steps']]"
```

Then check each of these against `run()` in `tracer/impl/<X>Tracer.java`:

**1. Every branch of the real code is exercised by the default input.**
List the `if` / `else` / early-`return` / recursion-base-case arms in `run()`. Walk the
default input by hand. Any arm not taken is untested pedagogy. This is the check that
found `binary-search-1d`.

**2. Every declared anchor is emitted — and every emitted anchor is declared.**
The second half is already enforced (`AnnotatedCode.resolve` throws on an unknown name, so
a typo fails a test rather than highlighting the wrong line). The first half is what Part 1
finds.

**3. The description matches what the code did at that step, not what it is about to do.**
The narration is the product. Compare against the good example in `KadaneTracer`:

```
"Running sum went negative (-2), so any subarray is better off starting fresh. Reset to 0."
```

It states the value, the consequence, and *why*. Reject narration that restates the
statement ("reset running to 0"), and reject narration that is constant across steps —
if `say()` takes no `%d` arguments inside a loop, that is a smell, not a style choice.

Also check the anchor is the *right* one, not merely a live one. `tree-inorder`'s final
step says "Traversal complete… [4, 2, 5, 1, 3, 6]" while highlighting line 2,
`if (node == null) return;`. No test can catch that — the anchor resolves, the line is in
range, the description is non-blank. Only reading both together catches it.

**4. Variables shown are the ones that changed.**
`.var(...)` should carry the state a learner is tracking. Note the deliberate trick in
`KadaneTracer`'s reset step: it emits `.var("running", 0)` *before* assigning `running = 0`,
so the panel shows the post-condition. Check whether a tracer got that ordering right —
emitting the pre-assignment value is a real and easy mistake.

**5. `emit.using(...)` matches the canvas that will render it.**
Only five canvases exist: `Array`, `Tree`, `Graph`, `LinkedList`, `RecursionTree`. There is
no `TrieCanvas`. A `dsType` outside that set renders blank.

**6. Recursive tracers push and pop.**
`emit.push(frame)` / `emit.pop()` must balance, or the call-stack panel drifts. Compare
`TreeInorderTracer`.

**7. Step count is proportional to the work.**
A 40-element array producing 6 steps means most iterations are silent. Conversely check the
budget: `truncated: true` on the *default* input already fails `runsOnDefaults`, but a
default sized just under the cap makes the animation unwatchable.

**8. The trace responds to input in more than one step.**
```bash
curl -s -X POST http://localhost:8923/api/problems/<id>/execute \
  -H 'Content-Type: application/json' -d '<alternate input>'
```
Compare the alternate trace to the default one step by step. `traceRespondsToItsInput`
passes on a single differing step; a genuine simulation differs almost everywhere.

**9. Invalid input is refused per field, not swallowed.**
```bash
curl -s -X POST http://localhost:8923/api/problems/binary-search-1d/execute \
  -H 'Content-Type: application/json' -d '{"nums":[9,3,7],"target":3}'
# expect: {"error":"invalid_input", ... "fieldErrors":{"nums":"... sorted ..."}}
```
If `run()` defends against bad input itself, the constraint belongs in the `InputSpec`
instead — `InputValidator` is the only trust boundary.

---

## Verdict

State the outcome as one of:

- **Accept** — no dead anchors, every branch exercised, descriptions verified against the
  code line by line.
- **Accept with a named follow-up** — e.g. "`none` anchor is genuinely unreachable on a
  solvable default; documented."
- **Reject** — name the anchor, the branch, or the step number. Never "the trace looks
  shallow."

If the fix changes behaviour, the repo rule applies: prove the new assertion fails against
the old code before accepting it (see the `prove-the-test-fails` skill), then
`cd backend && mvn test` — 308 tests, all green.
