---
name: add-a-problem
description: >
  Add or trace a DSA problem in the dsa-with-ui repo, given a LeetCode URL, a GeeksforGeeks
  URL, or just a problem name ("add Dijkstra", "trace merge intervals", "make climbing
  stairs actually animate"). Covers finding whether the id is already catalogued (425 of
  433 are catalogued but untraced — that is the usual case), registering a genuinely new
  ProblemDetail in the right service, writing the AlgorithmTracer with `// @a` anchored
  code and an InputSpec, adding the TracerContractTest.ALTERNATE_INPUT entry that the
  build requires, deleting the legacy delegate, and the verification runs.
---

# Adding a problem

Two very different jobs share this name:

| Situation | How many | What you do |
|---|---|---|
| **Catalogued but untraced** | 425 of 433 | Write only the tracer. The `ProblemDetail` already exists — do not add a second one. |
| **Genuinely new** | rare | Register a `ProblemDetail` first, then the tracer, then move the pinned `433`. |

Step 1 decides which.

Start on a branch: `git switch main && git pull && git switch -c feat/trace-<id>`.

---

## Step 1 — find the id

From a URL, take the slug: `leetcode.com/problems/maximum-subarray/` → `maximum-subarray`.
The repo's ids are **not** LeetCode slugs (`maximum-subarray` is `kadane-algo` here), so
search by words from the title too.

```bash
# Offline. Finds both individually-registered and bulk-registered entries.
grep -rin "maximum subarray\|kadane" backend/src/main/java/com/dsa/ui/service/
```

```bash
# Authoritative — needs `cd backend && mvn spring-boot:run` in another shell.
curl -s http://localhost:8923/api/problems | python3 -c "
import json,sys
q='kadane'
for p in json.load(sys.stdin):
    if q in p['id'] or q in p['title'].lower():
        print(p['id'],'|',p['title'],'| traced=',p['traced'],'|',p['category'],'| dsType=',p['dsType'])"
# kadane-algo | Kadane's Algorithm (Max Subarray Sum) | traced= True | Arrays | dsType= Array
```

- **A hit with `traced=False`** → the common case. Skip to Step 3, reusing that exact id.
- **A hit with `traced=True`** → already done. Improve it via `review-trace-simulation`
  instead of adding a second tracer; two tracers claiming one id **fail application
  startup**, deliberately.
- **No hit** → genuinely new. Do Step 2 first.

Before inventing an id, check it is not one of the seven already claimed twice
(`dfs-traversal`, `flood-fill`, `longest-common-prefix`,
`longest-substring-without-repeating`, `merge-intervals`, `number-of-islands`,
`surrounded-regions`). `ProblemCatalog` gives the id to the first provider and records the
loser in `stats.duplicateIds`; a new collision would silently make your problem
unreachable.

---

## Step 2 — register a new `ProblemDetail` (only if genuinely new)

Pick the service by topic, not by guessing from `category`. All 18 implement
`catalog/ProblemProvider` and live in `backend/src/main/java/com/dsa/ui/service/`:

| Topic | Service | Legacy base path |
|---|---|---|
| Arrays | `ArrayService` (3,151 lines) | `/api/arrays` |
| Binary trees & BST | `TreeService` | `/api/trees` |
| Graph BFS/DFS | `GraphBfsDfsService` | `/api/graphs/bfs-dfs` |
| Dijkstra, MST, SCC, string-matching | `AdvancedGraphService` | `/api/graphs/advanced` |
| Binary search | `BinarySearchService` | `/api/binarysearch` |
| DP | `DpService` | `/api/dp` |
| Linked list | `LinkedListService` | `/api/linkedlist` |
| Stack & queue | `StackQueueService` | `/api/stackqueue` |
| Heaps | `HeapService` | `/api/heaps` |
| Greedy | `GreedyService` | `/api/greedy` |
| Sliding window | `SlidingWindowService` | `/api/slidingwindow` |
| Strings | `StringService` | `/api/strings` |
| Tries | `TrieService` | `/api/tries` |
| Sorting | `SortingService` | `/api/sorting` |
| Recursion & backtracking | `RecursionBacktrackingService` | `/api/recursion-backtracking` |
| Basic recursion | `BasicRecursionService` | `/api/basic-recursion` |
| Basic maths | `BasicMathService` | `/api/maths` |
| Bit manipulation | `BitManipulationService` | `/api/bitmanipulation` |

Add to that service's `initProblems()`, following the surrounding calls:

```java
problems.put("kadane-algo", new ProblemDetail(
    "kadane-algo",                       // id — must equal the tracer's id()
    "Kadane's Algorithm (Max Subarray Sum)",
    "Arrays - Medium",                   // striverSheetSection
    "Arrays",                            // category — see the list in ProblemDetail.java
    "Medium",
    "Find the maximum sum of a contiguous subarray.",
    "// superseded by the tracer's annotatedCode()",   // javaCode
    null, null, null,                    // graph nodes / edges / tree nodes
    createArrayState(new int[]{-2,1,-3,4,-1,2,1,-5,4}, -1, -1),
    null, null, null,                    // list / trie / grid
    new ComplexityDetail("O(N)", "…", "Kadane", "O(1)", "…", "Memory", "Auxiliary Space: O(1)", "Memory"),
    "Array"                              // dsType — drives canvas selection
));
```

`createArrayState(...)` is a private per-service helper and exists in only 10 of the 18
services (`ArrayService`, `BasicMathService`, `BasicRecursionService`, `BitManipulationService`,
`DpService`, `GreedyService`, `HeapService`, `SlidingWindowService`, `StackQueueService`,
`StringService`). Elsewhere pass `null` for `defaultArray` or build the
`List<ArrayElement>` inline — the tracer's `InputSpec` default is what actually drives the
v2 animation.

Write a **real** `ComplexityDetail`. Bulk-registered problems all share one cloned generic
`O(N)/O(1)`, which is wrong for most of them; do not copy that pattern.

`dsType` must be one of `Array`, `Matrix`, `Tree`, `LinkedList`, `Queue`, `Stack`,
`PriorityQueue`, `Trie`. Note the frontend has only five canvases — `Array`, `Tree`,
`Graph`, `LinkedList`, `RecursionTree` — and **no TrieCanvas**, so a Trie problem currently
falls through to `GraphCanvas` and renders blank. Say so rather than pretending it works.

A new problem moves a pinned number:

```bash
# ProblemsApiTest asserts 433 unique ids. Update it in the SAME commit, with a reason,
# and update the README coverage table too.
grep -n "assertEquals(433" backend/src/test/java/com/dsa/ui/ProblemsApiTest.java
```

---

## Step 3 — write the tracer

New file `backend/src/main/java/com/dsa/ui/tracer/impl/<Name>Tracer.java`. Read
`KadaneTracer` (arrays), `TreeInorderTracer` + `BinaryTreeLayout` (trees),
`NumberOfIslandsTracer` (grids), `BfsTraversalTracer` (graphs) before writing.

```java
package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component                                   // discovered by TracerRegistry
public class KadaneTracer implements AlgorithmTracer {

    @Override public String id() { return "kadane-algo"; }   // must match a catalogue id

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Negative values are what make this interesting.")
                        .length(1, 40).values(-999, 999)     // MANDATORY caps
                        .defaultValue(List.of(-2, 1, -3, 4, -1, 2, 1, -5, 4))
                        .build());
    }

    @Override
    public String annotatedCode() {
        return """
               public int maxSubArray(int[] nums) {
                   // @a init
                   int best = nums[0], running = 0;
                   for (int i = 0; i < nums.length; i++) {
                       // @a extend
                       running += nums[i];
                       // @a best
                       if (running > best) best = running;
                       // @a reset
                       if (running < 0) running = 0;
                   }
                   // @a done
                   return best;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");        // getInt/getString/getGrid/
        int best = nums[0], running = 0;            // getLinkedList/getBinaryTree/getGraph
        emit.using("Array");                        // dsType for the canvas

        emit.at("init").say("Track the best sum seen (%d) and a running sum starting at 0.", best)
                .var("best", best).var("running", running).array(nums).step();
        // … one step per genuinely meaningful state change …
        emit.at("done").say("Largest contiguous subarray sum is %d.", best)
                .var("best", best).array(nums).step();
    }
}
```

**Tracers must be stateless.** One instance serves every concurrent request; all working
state lives in locals inside `run`.

### `annotatedCode()` rules

- `// @a name` sits on its own line, **above** the statement it names. `AnnotatedCode`
  strips the marker and resolves the name to the line below it.
- Duplicate, unnamed, or trailing anchors throw at parse time.
- `emit.at("typo")` throws — a mistyped anchor fails a test rather than highlighting the
  wrong line. **Never write a line number.**
- **Anchor only what you will emit.** Six of the eight existing tracers declare an anchor
  they never reach (five of them the loop header). `TracerContractTest` does not catch
  this despite `anchorsAreAllReachable`'s name; `review-trace-simulation` does.

### `InputSpec` rules

- Every collection field needs `.length(min, max)` and `.values(min, max)`. These are not
  stylistic: input is caller-supplied, so an unbounded `n` on a factorial-time algorithm is
  a denial of service.
- For permutations / n-queens / sudoku / subsets / combination-sum, set a small `n` ceiling
  **and** `.withMaxSteps(...)` below the 5000 default.
- Builders: `.range(min,max)` (scalar `INT`), `.length`, `.values`, `.sorted()`,
  `.distinct()`, `.directed()`, `.weighted()`. `InputValidator` interprets them, rejects
  unknown fields outright, and collects every field error into one 400.
- **Choose a default that illustrates the algorithm.** `binary-search-1d` ships
  `nums=[1,3,5,7,9,11,13], target=7`, so the first `mid` is a hit: three steps, and neither
  comparison branch nor the not-found path ever runs. Do not repeat that.

Wire representations (`FieldType`): `INT` a number · `INT_ARRAY` `[2,7,11,15]` · `STRING` ·
`INT_GRID` `[[1,0],[0,1]]` rectangular · `GRAPH`
`{"vertices":5,"edges":[[0,1],[1,2]]}` (a third element per edge when `.weighted()`) ·
`LINKED_LIST` `[1,2,3,4]` · `BINARY_TREE` level order with nulls, `[3,9,20,null,null,15,7]`.

### `run()` rules

- Actually run the algorithm. No precomputed narration.
- A step at each meaningful transition — comparison, swap, pointer move, constraint check,
  recursive call, backtrack.
- Narrate **why**: "running sum went negative, so any subarray is better off starting
  fresh" beats "reset running to 0".
- Recursive tracers: balance `emit.push(frame)` / `emit.pop()`.
- Let `StepBudgetExceededException` propagate; `TraceRunner` converts it to
  `truncated: true`.
- Payload per step: `.array(values[, primary[, secondary]])`, `.arrayState(...)`,
  `.grid(...)`, `.list(...)`, `.tree(...)`, `.nodes(map)`, `.edges(list)`.

---

## Step 4 — `ALTERNATE_INPUT` (the build fails without it)

`backend/src/test/java/com/dsa/ui/tracer/TracerContractTest.java`:

```java
"kadane-algo", Map.of("nums", List.of(5, -1, 5, -20, 3)),
```

`registryMatchesThisTestsExpectations` asserts the map's keys equal the registry's ids
exactly. **This failing is the design working** — an untested tracer must not be able to
ship. Add the entry; never relax the assertion.

The second input must be **materially different**, not a permutation. A reordering still
passes `traceRespondsToItsInput` while proving nothing. Aim for a different length, a
different answer, and a different branch profile — ideally one that reaches an anchor the
default does not.

`Map.of` caps at 10 entries; past that the map needs `Map.ofEntries(...)`. (HANDOFF PROMPT
A replaces this whole mechanism with an `alternateInput()` method on the tracer — if that
has landed, implement the method instead and this step is moot.)

---

## Step 5 — retire the legacy path for this id

Per HANDOFF PROMPT C, once a problem is traced:

- Delete its `case "<id>": return generate…Steps();` from the service's `generateSteps`
  switch, and delete the generator if nothing else calls it — otherwise the two paths
  diverge.
- **Keep** the `initProblems()` metadata. That still feeds `ProblemCatalog` until PROMPT D.
- Do **not** delete the switch's `default:` branch yet; other ids in that service still
  rely on it, and `ApiContractTest` exercises it. Removing it is PROMPT D.

---

## Step 6 — verify

```bash
cd backend && mvn test -Dtest=TracerContractTest    # 37 tests today; grows with tracers
cd backend && mvn test                             # expect Tests run: 308 (+ your new ones)
```

Then, with the app running:

```bash
cd backend && mvn spring-boot:run                                     # separate shell
curl -s http://localhost:8923/api/problems/stats                      # `traced` must be +1
curl -s http://localhost:8923/api/problems/<id>/execute                # 200 with real steps
curl -s -X POST http://localhost:8923/api/problems/<id>/execute \
     -H 'Content-Type: application/json' -d '<your alternate input>'   # a different trace
python3 .claude/skills/review-trace-simulation/check_trace.py <id>     # 0 dead anchors
```

Then run the `review-trace-simulation` skill in full — it is the acceptance gate, not this
checklist.

### Failure signatures

| Symptom | Cause |
|---|---|
| `registryMatchesThisTestsExpectations` fails | Step 4 skipped |
| App fails to start: "Two tracers claim problem id" | Duplicate `id()` — you added one for an already-traced problem |
| `noOrphanedTracers` fails | `id()` matches no catalogue entry; typo or Step 2 skipped |
| `Unknown code anchor 'x'. Declared: […]` | `emit.at` name not in `annotatedCode()` |
| `Anchor(s) [x] have no statement following them` | `// @a` was the last line |
| `noTwoTracersProduceIdenticalTraces` fails | Copy-pasted trace from another problem |
| `traceRespondsToItsInput` fails | The trace ignores `Inputs`, or your alternate is a permutation |
| `runsOnDefaults` fails on `isTruncated` | Default input is too big for the step budget |
| 400 with `fieldErrors` on your own default | The default violates its own `InputSpec` |

---

## Never

- Never make an unimplemented problem return another problem's steps. That is the defect
  the entire tracer layer exists to prevent: 303 of 440 ids once did exactly that, and all
  90 tests passed.
- Never add a `default:` to anything under `tracer/`.
- Never turn the 501 into a 200 with placeholder steps. "Not yet traced" is the honest
  answer and the UI is built to say it.
