# Complete source helper repairs, batch 1

Base: #219 / `e68e6f9`. Sources are derived from `solutions/core/*.java` and their
registered `CompleteSourceTracer` owners; no second problem-routing list is introduced.

Verification commands (from repository root):

```sh
cd backend
mvn -B test -Dtest=DisplayedCoreSolutionTest
mvn -B test
```

```sh
node docs/ui-revamp/evidence/source-code/compare-goldens.cjs e68e6f9 core docs/ui-revamp/evidence/source-code/core-batch1/golden-comparison.json
SOURCE_RESOURCE_FAMILY=core PLAYWRIGHT_MODULE=/path/to/playwright node docs/ui-revamp/evidence/source-code/complete-source-journey.cjs docs/ui-revamp/evidence/source-code/core-batch1
SOURCE_RESOURCE_FAMILY=core PLAYWRIGHT_MODULE=/path/to/playwright node docs/ui-revamp/evidence/source-code/custom-source-journey.cjs docs/ui-revamp/evidence/source-code/core-batch1
```

Browser commands require the real backend and frontend. Default URLs are 8923/5180;
override `BACKEND_URL`/`FRONTEND_URL` for isolated services. Never run full Maven tests
while the same worktree's backend is serving a browser journey.

RED: retaining the five old annotated listings after declaring the family caused
12/17 new contract cases to fail (compilation/execution, resource agreement, phase
anchors). The five HTTP identity cases passed on the old code: those assert delivery
agreement, not completeness in isolation. Two compiling mutations were also rejected:
reversed survivor order and an unsorted Quick Sort result. Mutations were restored.

The original `compile-inventory.json` remains a dated baseline, not silently refreshed
with repaired hashes. Five of its 53 non-platform Java failures are repaired here;
the remaining 48, 77 platform contexts, C++ labelling and semantic review of compiler
successes remain outstanding. This is not all-source certification or DP D5 rollout.
