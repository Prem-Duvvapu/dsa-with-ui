---
name: trace-coverage
description: >
  Report real animation coverage in the dsa-with-ui repo and confirm it stays complete.
  Use for "how many problems actually work", "is coverage still 100%", "did this change
  regress coverage", progress checks, or before editing any coverage claim in README.md /
  PROJECT_CONTEXT.md / CLAUDE.md. Reads the numbers from the code and the running API
  rather than from any document, because every document here is a snapshot that goes
  stale the moment something changes.
---

# Trace coverage

## The distinction that matters

- **Catalogued** — a `ProblemDetail` exists, so the problem appears in the UI.
- **Traced** — an `AlgorithmTracer` exists, so it actually animates its own algorithm.

**Both numbers are currently equal: 431 catalogued, 431 traced, 0 untraced.** The tracer
migration that made these two numbers diverge — 303 of 433 catalogued problems once
returned another algorithm's animation — is finished, and the legacy layer that made it
possible is deleted (`ARCHITECTURE.md`, `CLAUDE.md`). `traced` is kept as a field, not
because work is outstanding, but because it is what makes a *regression* visible: if a
future change adds a catalogue entry with no tracer, or a tracer with no catalogue entry
(`orphanedTracerIds`), this is the number that will move and the check that will catch it.

**Never quote a coverage number from a document.** `README.md` has historically carried
four different catalogue sizes, none matching the source, and stale figures have shown up
in `AUDIT.md`'s findings more than once. Run the commands.

---

## Authoritative: the API

```bash
cd backend && mvn spring-boot:run          # separate shell
curl -s http://localhost:8923/api/problems/stats
```

```json
{ "catalogued": 431, "traced": 431, "untraced": 0,
  "duplicateIds": {}, "orphanedTracerIds": [] }
```

Anything other than this shape is news:

- `untraced` above 0 — a catalogue entry with no tracer. Find it with the per-category
  breakdown below, then either write the tracer (`add-a-problem` skill) or, if the id was
  added deliberately ahead of its tracer, say so rather than letting it sit silently.
- `duplicateIds` non-empty — two `ProblemProvider`s claimed the same id. `DuplicateProblemTest`
  is supposed to fail startup on this; if it's non-empty on a running server, that test has
  a hole.
- `orphanedTracerIds` non-empty — a tracer with no catalogue entry. It runs but nothing
  lists it, so nobody can reach it. `TracerContractTest.noOrphanedTracers` enforces this.

### Per category

```bash
curl -s http://localhost:8923/api/problems | python3 -c "
import json,sys,collections
cat=collections.Counter(); tr=collections.Counter()
for p in json.load(sys.stdin):
    cat[p['category']]+=1
    if p['traced']: tr[p['category']]+=1
for c,n in cat.most_common(): print(f'{tr[c]:>3}/{n:<4} {c}')"
```

Every row should read `N/N` — a category with `M/N` where `M < N` is the thing this skill
exists to catch. Seventeen categories today (Graph BFS/DFS and Advanced Graphs merged into
one **Graphs** topic during the duplicate-id cleanup); re-run this rather than trusting
that count either.

---

## Confirming a coverage claim before you write it

Before any commit that changes `README.md`'s coverage table, `PROJECT_CONTEXT.md`'s
coverage note, or `CLAUDE.md`'s pinned-numbers section:

```bash
curl -s http://localhost:8923/api/problems/stats
```

and use exactly those numbers. If they don't match what's already written in the docs,
that's a separate finding worth its own line in the commit message — either the docs were
stale, or the count genuinely moved and every doc that quotes it needs updating together
(`CLAUDE.md`'s "Pinned numbers" section names which ones).

---

## If coverage ever regresses

This is the scenario the whole skill exists for, even though it hasn't happened since the
migration completed:

1. Run the per-category breakdown above to find which category dropped below `N/N`.
2. Find the specific id: `curl -s http://localhost:8923/api/problems | python3 -c "import
   json,sys; [print(p['id']) for p in json.load(sys.stdin) if p['category']=='<Category>'
   and not p['traced']]"`.
3. Confirm it against source: does `TracerRegistry` really have no tracer for that id, or
   is this a catalogue-vs-tracer id mismatch (`ProblemCatalog.getOrphanedTracerIds()` would
   show a mismatched pair)?
4. Write the tracer (see the `add-a-problem` skill) or revert whatever removed it.
5. `stats.traced` must return to 431 (or rise past it, if the catalogue itself grew) before
   the fix is done — never quietly ship a lower number.

---

## Reporting the number

- Give **traced / catalogued**, never "problems supported".
- If a change moves the catalogued count, that's a deliberate act — `ProblemsApiTest`
  pins `431` unique ids and `0` duplicates; update the assertion and the `README.md`
  coverage table in the same commit, and say so in the commit message.
- Use `audit-question` to check one problem's coverage is *real* (not just present), and
  `audit-topic` to sweep a whole category for the same. This skill answers "is everything
  present"; those two answer "is what's present actually correct."
