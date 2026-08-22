---
name: dsa-review
description: >
  Project-invariant review for the dsa-with-ui repo — run it on a branch, a diff, or a PR
  before merging. Checks the things this codebase has actually broken before: a
  reintroduced `default:` fallback that makes one problem play another's animation, a
  dead or dangling `// @a` anchor, an `alternateInput()` copied from the spec defaults, a
  new npm dependency, a `var()` or className that `index.css` does not define, a legacy
  controller that lost its 404 guard, a moved pinned number (433 problems / 7 duplicates),
  or work committed on `main`. Use alongside — not instead of — the built-in /code-review.
---

# dsa-review

## Why this exists and is not `/code-review`

`/code-review` reads a diff for correctness bugs and simplification. It is good at that and
you should still run it. It knows nothing about *this repo's* invariants, which are not
bugs in the ordinary sense — they are honesty properties. The failure mode here was never a
crash. It was 303 problems that returned another algorithm's animation while 90 tests
passed green, because the only per-problem assertion was `!steps.isEmpty()` and a
`default:` branch guaranteed it.

That class of defect is invisible to a general-purpose reviewer: the code compiles, the
tests pass, the API returns 200, and the animation plays. `dsa-review` is a checklist of
the specific ways that has happened, each with a command that produces an answer.

Order: `/code-review` for bugs → `dsa-review` for invariants → `review-trace-simulation`
if the diff adds or changes a tracer.

---

## 0. Scope the diff

```bash
git branch --show-current      # MUST NOT be `main`
git diff --stat main...HEAD
```

If the branch is `main`, stop and fix that first — `git switch -c <topic>`; uncommitted
changes carry across. Merges into `main` go through a PR so CI runs.

---

## 1. No fallback was reintroduced

The one rule the architecture exists to enforce: **a problem never shows another problem's
trace.**

```bash
# Expect: only the two Javadoc mentions in AlgorithmTracer.java and TracerRegistry.java
# that describe the old behaviour. Any real `default:` in the tracer layer is a finding.
grep -rn "default:" backend/src/main/java/com/dsa/ui/tracer/ \
        backend/src/main/java/com/dsa/ui/catalog/ \
        backend/src/main/java/com/dsa/ui/controller/ProblemsController.java
```

```bash
# Expect 18 lines, each `...Service.java:1` — one legacy `default:` per service. Tracked
# debt (HANDOFF PROMPT C deletes them as problems get traced), so the count must go DOWN
# or stay flat. A service showing 2, or a 19th file, means a new fallback was added.
grep -rc "default:" backend/src/main/java/com/dsa/ui/service/ | grep -v ":0"
```

```bash
# Expect: only orElseThrow(...) and one orElse(null) for an untraced problem's inputSpec.
# An `orElse(someOtherTracer)` or `orElseGet(...)` returning a tracer is the bug.
grep -rn "orElse" backend/src/main/java/com/dsa/ui/tracer/ \
        backend/src/main/java/com/dsa/ui/catalog/ \
        backend/src/main/java/com/dsa/ui/controller/ProblemsController.java
```

Also reject, by inspection of the diff:
- a `catch` that returns an empty or placeholder step list instead of propagating
- `501` softened to `200` with a synthetic "coming soon" step — the UI is supposed to say
  "not yet traced", and the honest signal is the status code
- a new one-line delegate generator in a service (`{ return generateSomethingElseSteps(); }`)

```bash
# Existing delegate census. 122 today, in four clusters. Must never grow.
grep -rhoP 'private List<ExecutionStep> \w+\(\) \{ return \K\w+' \
     backend/src/main/java/com/dsa/ui/service/ | sort | uniq -c | sort -rn
#      60 generateGraphIntroSteps
#      31 generateBs1dSteps
#      29 generateReverseSteps
#       2 generateClimbingStairsSteps
```

---

## 2. The legacy 404 guard is intact on all 18 controllers

Eight of the eighteen controllers once dropped this, so an unknown id returned 200 with
whatever the service's `default:` produced. `ApiContractTest` is parameterized over all 18
to keep that from recurring — check nobody weakened it.

```bash
# Expect: 2 for every *Controller.java except ProblemsController (0 — it raises
# ResponseStatusException instead). A legacy controller showing 0 or 1 lost a guard.
grep -c "notFound()" backend/src/main/java/com/dsa/ui/controller/*Controller.java
```

```bash
cd backend && mvn test -Dtest=ApiContractTest
```

---

## 3. Anchors resolve, and no new dead ones

Never a raw line number. `activeLine` used to be hand-written and had drifted —
`LinkedListService` emitted line 51 into a nine-line snippet, and 29 problems delegated to
it.

```bash
# Any digit passed to at() is a finding — anchors are names.
grep -rnP 'emit\.at\(\s*\d' backend/src/main/java/com/dsa/ui/tracer/
```

Anchor *reachability* is not covered by the test suite despite
`TracerContractTest.anchorsAreAllReachable`'s name — that test only asserts the trace
emitted something. If the diff touches a tracer, run the real check:

```bash
cd backend && mvn spring-boot:run        # separate shell
python3 .claude/skills/review-trace-simulation/check_trace.py <changed-id>
```

Six of the eight current tracers already have dead anchors, so treat the baseline as known
debt — the standard is that a **changed** tracer does not add one.

---

## 4. `alternateInput()` is real, not a copy of the defaults

`alternateInput()` is abstract, so a missing one will not compile. The failure mode that
DOES get through review is an alternate pasted from the spec's own defaults: then
`traceRespondsToItsInput` compares a trace against itself, passes, and proves nothing.
`alternateInputDiffersFromDefaults` catches it — confirm the diff did not weaken that.

```bash
# Eyeball each new tracer's two inputs side by side. They must differ in LENGTH or in
# which branches run, not merely in order — a permutation changes the fingerprint while
# proving nothing about whether the algorithm executed.
grep -A6 'alternateInput()' backend/src/main/java/com/dsa/ui/tracer/impl/*.java
```

The alternate input must be **materially different**, not a permutation. Reordering
`[2,7,11,15]` into `[7,2,15,11]` still passes `traceRespondsToItsInput` while proving
nothing — reject it in review even though the test is green.

---

## 5. Pinned numbers moved only deliberately

`ProblemsApiTest` asserts 433 unique ids and 7 duplicate ids. They are tripwires against
accidental catalogue loss.

```bash
grep -n "433\|assertEquals(7" backend/src/test/java/com/dsa/ui/ProblemsApiTest.java
```

If the diff changes either, the same commit must (a) say why in the message and (b) update
the `README.md` coverage table. A number changed without a stated reason is a finding.

---

## 6. Frontend guards

```bash
# Expect no change to dependencies. This project adds none — the search work rejected even
# fuse.js and lodash by name, in favour of a hand-rolled, unit-tested module.
git diff main...HEAD -- frontend/package.json frontend/package-lock.json
```

```bash
cd frontend && npx vitest run src/designTokens.test.js
```

`designTokens.test.js` is a static guard, not a unit test: it fails the build on any
`var(--x)` or static `className` in `App.jsx`/`components/*.jsx` that `index.css` does not
define. Commit b553e56 rewrote `index.css` from 343 lines to 170 and updated 8 of 13
components; the other 5 kept referencing 15 now-missing custom properties. CSS silently
drops a declaration whose `var()` cannot resolve, so the header lost its margin and several
canvases their transitions with every test still green. If a diff makes this test fail, the
fix is to define the token — never to relax the matcher.

Reject in review:
- deleting a token from `index.css` without grepping for its uses
- `--state-current` and `--diff-medium` re-collapsing to the same value (both were
  `#f59e0b` meaning different things; the test now keeps the four `--state-*` tokens
  distinct from the difficulty tokens)
- state encoded by colour alone

---

## 7. Both suites green

```bash
cd backend && mvn test          # expect: Tests run: 308, Failures: 0, Errors: 0
cd frontend && npm ci && npx vitest run    # expect: Test Files 3 passed, Tests 16 passed
cd frontend && npx vite build              # what CI builds; catches unresolved imports
```

CI (`.github/workflows/ci.yml`) runs the same three on every branch push. Do not use the
root `package.json` scripts — `test:backend` and `build:backend` wrap themselves in
`wsl --exec`, which is a Windows-host helper and does not exist on a Linux runner or
inside WSL.

---

## 8. The honesty question

Last, and the one that matters most. For every behaviour the diff adds, ask:

> If this were not implemented, would anything in the repo notice?

If the answer is no, the diff is in the same category as the 303 stubs. Say so, and name
the assertion that is missing. A green test that would not have caught the bug is worth
nothing — see the `prove-the-test-fails` skill.

---

## Reporting

Group findings as **Blocking** / **Should fix** / **Note**, each with a file:line and the
command that produced it. Do not report the known baseline debt (18 legacy `default:`
branches, 122 delegates, 7 duplicate ids, 6 tracers with pre-existing dead anchors) as new
findings — say explicitly that they are unchanged.
