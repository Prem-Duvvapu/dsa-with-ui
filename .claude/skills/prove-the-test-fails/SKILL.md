---
name: prove-the-test-fails
description: >
  The dsa-with-ui repo's negative-verification discipline — before accepting any bug fix,
  prove the new test goes RED against the unfixed code, then restore the fix and watch it
  go green. Use whenever you write a regression test, fix a bug, are asked "does this test
  actually catch it?", are about to report a fix as done, or are tempted to accept a test
  that passed on the first run. This repo shipped 303 broken problems under 90 green tests;
  a test that would not have caught the bug is worth nothing here.
---

# Prove the test fails first

## Why this repo insists

The old suite had 90 passing tests and 303 of 440 catalogued problems returning another
algorithm's animation. The tests were not wrong; they were vacuous. The only per-problem
assertion was:

```java
assertFalse(steps.isEmpty());
```

and every service's `switch (problemId)` ended in `default: return generateSomethingElseSteps();`
— which guaranteed a non-empty list for every id, including typos. The suite could not
distinguish a working implementation from a stub, and nobody noticed for months.

So the rule, from `HANDOFF.md`'s non-negotiable working rules:

> Every fix gets a test, and you **must** verify the test FAILS against the broken code
> before accepting it. Temporarily revert the fix, watch the test go red, restore. A green
> test that would not have caught the bug is worth nothing.

A test that passes on its very first run has proved nothing yet.

---

## The loop

**1. Write the test against the broken code, before the fix.** Run it. It must fail, and
the failure message must name the actual defect.

```bash
# Quote the argument — `#` selects one method. Verified: this runs the 8 parameterized
# cases of that one test, not the whole 37-test class.
cd backend && mvn test -Dtest='TracerContractTest#traceRespondsToItsInput'
```

**2. If it passes, the test is wrong — not the bug.** Do not proceed. Strengthen the
assertion until it fails for the right reason.

**3. Apply the fix. Re-run the same command.** It must go green.

**4. Run the full suite.**

```bash
cd backend && mvn test                     # expect Tests run: 308, Failures: 0, Errors: 0
cd frontend && npx vitest run              # expect Test Files 3 passed, Tests 16 passed
```

**5. Report the red output, not a claim.** Paste the failure from step 1. "I verified it
fails first" without the output is the thing this rule exists to prevent.

---

## When the fix is already in

Revert it temporarily. Keep it surgical and always restore.

```bash
# Make the smallest possible edit that reintroduces the defect, run the one test, restore.
cd backend && mvn test -Dtest=<Class>#<method>          # expect RED
git checkout -- backend/src/main/java/com/dsa/ui/...    # restore
cd backend && mvn test -Dtest=<Class>#<method>          # expect GREEN
```

Or, to check a whole branch's tests against the pre-fix tree:

```bash
git stash                                  # or: git switch --detach main
cd backend && mvn test -Dtest=<Class>       # expect RED
git stash pop                               # or: git switch -
```

Never leave a reverted fix in the tree. Re-run the full suite afterwards.

---

## Worked example, from `HANDOFF.md` PROMPT A

To prove the harness still detects fake work, break a tracer on purpose:

```java
// KadaneTracer.run — temporarily replace the body with a fixed narration
@Override
public void run(Inputs in, StepEmitter emit) {
    emit.at("init").say("Start.").step();
    emit.at("done").say("Done.").step();
}
```

```bash
cd backend && mvn test -Dtest='TracerContractTest#traceRespondsToItsInput'
```

Expected: **red**, naming `kadane-algo`, with the message

> produced an identical trace for two different inputs, so it is not executing the
> algorithm — it is replaying a fixed narration

Restore, re-run, green. Now the harness has been shown to work, and a passing run means
something.

---

## Assertions that are vacuous in this codebase

Recognise these — each is a version of the mistake that hid the 303 stubs.

| Vacuous | Why | Instead |
|---|---|---|
| `assertFalse(steps.isEmpty())` | The `default:` fallback guaranteed it for every id | Compare traces across two materially different inputs |
| `assertEquals(200, status)` | The old controllers returned 200 with someone else's steps | Assert 404 for unknown, 501 for untraced |
| `assertNotNull(description)` | A wrong description is non-null | Assert the described value appears in the step's variables |
| `assertTrue(activeLine > 0)` | Line 51 in a 9-line snippet is > 0 | Assert `1 <= activeLine <= lineCount` — and that the anchor was reachable |
| Alternate input = a permutation of the default | Still a different fingerprint; proves nothing | Different length, different answer, different branch profile |
| Golden file regenerated after the change | Records the bug as expected | Regenerate only when the change is intentional, and say so |

---

## Existing tests that already work this way — copy them

- `TracerContractTest.traceRespondsToItsInput` — two inputs, different fingerprints. Cannot
  be survived by a canned narration.
- `TracerContractTest.noTwoTracersProduceIdenticalTraces` — catches a copy-pasted trace
  across the whole registry.
- `TracerContractTest.stepBudgetTruncates` — builds an intentionally runaway tracer inline
  and asserts it is capped at 25 steps.
- `ApiContractTest` — parameterized over all 18 base paths, because testing one controller
  by hand is exactly how eight of them silently lost their 404 guard.
- `designTokens.test.js` — a static guard over `index.css`, because CSS silently drops a
  declaration whose `var()` cannot resolve and no runtime test can see it.

---

## Also non-negotiable

- **Do not weaken or delete an existing assertion to make something pass.** If
  `registryMatchesThisTestsExpectations` fails, add the `ALTERNATE_INPUT` entry — that
  failure is the design working.
- **Do not move a pinned number to make a test pass.** `ProblemsApiTest`'s `433` and `7`
  are tripwires. Changing one is a deliberate act that belongs in the commit message,
  alongside the `README.md` update.
- **Report honestly.** If a part is unfinished, say which part. `traced` is an honesty
  flag; so is your summary.
