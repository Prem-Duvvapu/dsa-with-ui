#!/usr/bin/env python3
"""Mechanical half of a topic audit.

Talks to a running backend (default http://localhost:8923) and reports, for every
problem in one catalogue category: whether it is traced, its dsType, its step count,
which declared anchors are never highlighted, whether it is a duplicate id, and what
its legacy endpoint answers (410 = retired, 200 = still serving substitute steps).

It also fingerprints every traced problem in the topic and reports pairs whose traces
are identical or near-identical — the collision `TracerContractTest` only catches at
exact equality, and which two structurally similar tracers in one topic can easily hit.

"ENDS MID-RECURSION" means the last step's callStack is non-empty. The pushes and pops
usually do balance; what is missing is a step emitted *after* the unwinding, so the
sidebar freezes on frames the viewer never sees drain. Judge it, do not assume a bug.

It cannot judge whether a description is *true*, or whether a default reaches the
interesting branch. That is the reading pass in SKILL.md.

Usage
    cd backend && mvn spring-boot:run              # in another shell
    python3 .claude/skills/audit-topic/audit_topic.py                     # list categories
    python3 .claude/skills/audit-topic/audit_topic.py "Binary Search"
    python3 .claude/skills/audit-topic/audit_topic.py "Binary Search" --no-legacy

Exit code 1 if any problem in the topic has a dead anchor, a legacy endpoint that still
answers 200, a duplicate id, or a trace that collides with another in the same topic.
stdlib Python only — do not add a dependency to run an audit.
"""

import argparse
import collections
import difflib
import json
import sys
import urllib.error
import urllib.request

DEFAULT_BASE = "http://localhost:8923"

# Catalogue category -> the legacy controller that used to serve it. Matched as a
# lowercase substring, longest first, so "Binary Search - Answers" finds "binary search".
# An unmapped category falls back to probing every base path.
LEGACY_PATHS = {
    "advanced graphs": "/api/graphs/advanced",
    "graph bfs/dfs": "/api/graphs/bfs-dfs",
    "graphs": "/api/graphs/bfs-dfs",
    "binary search": "/api/binarysearch",
    "binary tree": "/api/trees",
    "bst": "/api/trees",
    "tries": "/api/tries",
    "sorting": "/api/sorting",
    "dynamic programming": "/api/dp",
    "greedy": "/api/greedy",
    "heaps": "/api/heaps",
    "linked list": "/api/linkedlist",
    "recursion & backtracking": "/api/recursion-backtracking",
    "sliding window": "/api/slidingwindow",
    "stack & queue": "/api/stackqueue",
    "strings": "/api/strings",
    "bit manipulation": "/api/bitmanipulation",
    "learn the basics": "/api/basic-recursion",
    "maths": "/api/maths",
    "arrays": "/api/arrays",
}

ALL_PATHS = sorted(set(LEGACY_PATHS.values()))


def get(url):
    with urllib.request.urlopen(url, timeout=30) as r:
        return json.load(r)


def status(url):
    """HTTP status of a GET, without raising."""
    try:
        with urllib.request.urlopen(url, timeout=30) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return e.code
    except OSError:
        return 0


def legacy_path_for(category):
    hits = [(k, v) for k, v in LEGACY_PATHS.items() if k in category.lower()]
    if not hits:
        return None
    return max(hits, key=lambda kv: len(kv[0]))[1]


def legacy_status(base, pid, path):
    """(status, path) for the legacy endpoint, probing every controller if unmapped."""
    if path:
        code = status(f"{base}{path}/execute/{pid}")
        if code != 404:
            return code, path
    for candidate in ALL_PATHS:
        code = status(f"{base}{candidate}/execute/{pid}")
        if code != 404:
            return code, candidate
    return 404, path


def fingerprint(trace):
    """What a viewer perceives — mirrors TracerContractTest.fingerprint."""
    return "\n".join(
        f"{s['activeLine']}|{s['description']}|{s.get('variables')}" for s in trace["steps"]
    )


def audit(base, category, check_legacy):
    catalogue = get(f"{base}/api/problems")
    stats = get(f"{base}/api/problems/stats")
    duplicates = set(stats.get("duplicateIds") or [])

    entries = [p for p in catalogue if p["category"] == category]
    if not entries:
        near = sorted({p["category"] for p in catalogue if category.lower() in p["category"].lower()})
        sys.exit(f"No category exactly named {category!r}." + (f" Did you mean: {near}" if near else ""))

    path = legacy_path_for(category)
    print(f"\n=== {category} ===")
    print(f"{len(entries)} catalogued   legacy base: {path or '(probing)'}\n")
    print(f"{'id':<36} {'traced':<7} {'dsType':<14} {'steps':>6}  {'legacy':<7} notes")
    print("-" * 104)

    prints, problems = {}, []
    for p in sorted(entries, key=lambda e: e["id"]):
        pid, notes, flagged = p["id"], [], False
        if pid in duplicates:
            notes.append("DUPLICATE ID")
            flagged = True

        steps_col, ds = "-", p.get("dsType") or "-"
        if p["traced"]:
            trace = get(f"{base}/api/problems/{pid}/execute")
            used = {s["activeLine"] for s in trace["steps"]}
            dead = sorted(n for n, line in trace["anchors"].items() if line not in used)
            steps_col = str(trace["stepCount"])
            prints[pid] = fingerprint(trace)
            if dead:
                notes.append(f"DEAD ANCHORS {dead}")
                flagged = True
            if trace["truncated"]:
                notes.append("TRUNCATED ON DEFAULTS")
                flagged = True
            depth = len(trace["steps"][-1].get("callStack") or []) if trace["steps"] else 0
            if depth:
                notes.append(f"ENDS MID-RECURSION (call stack {depth} deep on the last step)")
                flagged = True
        else:
            notes.append("untraced")

        legacy_col = "-"
        if check_legacy:
            code, used_path = legacy_status(base, pid, path)
            legacy_col = str(code)
            if p["traced"] and code == 200:
                notes.append(f"LEGACY STILL SERVES ({used_path})")
                flagged = True
            elif not p["traced"] and code == 410:
                notes.append("410 BUT NO TRACER - unreachable from both APIs")
                flagged = True

        if flagged:
            problems.append(pid)
        print(f"{pid:<36} {str(p['traced']):<7} {ds:<14} {steps_col:>6}  {legacy_col:<7} {'; '.join(notes)}")

    collisions = []
    ids = sorted(prints)
    for i, a in enumerate(ids):
        for b in ids[i + 1:]:
            ratio = difflib.SequenceMatcher(None, prints[a], prints[b]).quick_ratio()
            if ratio > 0.98:
                collisions.append((a, b, ratio))
    if collisions:
        print("\nNear-identical traces within this topic:")
        for a, b, ratio in collisions:
            print(f"  {a}  <->  {b}   similarity {ratio:.3f}"
                  + ("   IDENTICAL" if prints[a] == prints[b] else ""))
        problems.extend(a for a, _, _ in collisions)

    traced = sum(1 for p in entries if p["traced"])
    print(f"\n{traced}/{len(entries)} traced in {category}."
          f"  Repo-wide: {stats['traced']}/{stats['catalogued']}.")
    if problems:
        print(f"Needs attention: {sorted(set(problems))}")
        return False
    print("No mechanical problems found. The reading pass in SKILL.md is still required.")
    return True


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("category", nargs="?", help="exact catalogue category; omit to list them")
    ap.add_argument("--base", default=DEFAULT_BASE)
    ap.add_argument("--no-legacy", action="store_true",
                    help="skip the legacy-endpoint sweep (one extra request per problem)")
    args = ap.parse_args()

    try:
        catalogue = get(f"{args.base}/api/problems")
    except OSError as e:
        sys.exit(f"Cannot reach {args.base} ({e}). Start it: cd backend && mvn spring-boot:run")

    if not args.category:
        counts = collections.Counter(p["category"] for p in catalogue)
        traced = collections.Counter(p["category"] for p in catalogue if p["traced"])
        print(f"{'category':<34} {'traced':>7} / {'total':<6}")
        for c, n in sorted(counts.items()):
            print(f"{c:<34} {traced[c]:>7} / {n:<6}")
        return

    if not audit(args.base, args.category, not args.no_legacy):
        sys.exit(1)


if __name__ == "__main__":
    main()
