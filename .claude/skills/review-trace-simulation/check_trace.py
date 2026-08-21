#!/usr/bin/env python3
"""Mechanical half of the trace-simulation review.

Talks to a running backend (default http://localhost:8923) and reports, per traced
problem: step count, which declared anchors were never highlighted, and whether an
alternate input actually changes the trace.

It cannot judge whether a description is *true* — that is the reading pass in
SKILL.md. It only finds the failures a machine can see.

Usage
    cd backend && mvn spring-boot:run          # in another shell
    python3 .claude/skills/review-trace-simulation/check_trace.py
    python3 .claude/skills/review-trace-simulation/check_trace.py kadane-algo
    python3 .claude/skills/review-trace-simulation/check_trace.py kadane-algo \
        --alt '{"nums": [5, -1, 5, -20, 3]}'

Exit code 1 if any checked problem has a dead anchor or an input-insensitive trace.
stdlib only — this project adds no dependencies.
"""

import argparse
import json
import sys
import urllib.error
import urllib.request

DEFAULT_BASE = "http://localhost:8923"


def get(url):
    with urllib.request.urlopen(url, timeout=30) as r:
        return json.load(r)


def post(url, payload):
    body = json.dumps(payload).encode()
    req = urllib.request.Request(
        url, data=body, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def fingerprint(trace):
    """What a viewer perceives — mirrors TracerContractTest.fingerprint."""
    return "\n".join(
        f"{s['activeLine']}|{s['description']}|{s.get('variables')}"
        for s in trace["steps"]
    )


def check(base, pid, alt):
    trace = get(f"{base}/api/problems/{pid}/execute")
    anchors = trace["anchors"]
    used = {s["activeLine"] for s in trace["steps"]}
    dead = sorted(name for name, line in anchors.items() if line not in used)

    print(f"\n=== {pid} ===")
    print(f"  steps      : {len(trace['steps'])}   truncated: {trace['truncated']}")
    print(f"  anchors    : {len(anchors)} declared, {len(used)} lines highlighted")
    print(f"  dead       : {dead if dead else 'none'}")

    lines = trace["code"].split("\n")
    for name in dead:
        n = anchors[name]
        print(f"      // @a {name}  -> line {n}: {lines[n - 1].strip()}")

    sensitive = None
    if alt is not None:
        try:
            other = post(f"{base}/api/problems/{pid}/execute", alt)
            sensitive = fingerprint(trace) != fingerprint(other)
            print(f"  alt input  : {'trace changed' if sensitive else 'IDENTICAL TRACE'}")
            print(f"  alt steps  : {len(other['steps'])} (default {len(trace['steps'])})")
        except urllib.error.HTTPError as e:
            print(f"  alt input  : rejected ({e.code}) {e.read().decode()[:200]}")
            sensitive = None

    ok = not dead and sensitive is not False
    return ok


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("problem_ids", nargs="*", help="default: every traced problem")
    ap.add_argument("--base", default=DEFAULT_BASE)
    ap.add_argument("--alt", help="JSON object posted as the alternate input")
    args = ap.parse_args()

    alt = json.loads(args.alt) if args.alt else None

    try:
        catalogue = get(f"{args.base}/api/problems")
    except OSError as e:
        sys.exit(f"Cannot reach {args.base} ({e}). Start it: cd backend && mvn spring-boot:run")

    traced = [p["id"] for p in catalogue if p["traced"]]
    ids = args.problem_ids or traced

    unknown = [i for i in ids if i not in traced]
    if unknown:
        sys.exit(f"Not traced (or not catalogued): {unknown}. Traced ids: {traced}")

    if alt and len(ids) != 1:
        sys.exit("--alt applies to exactly one problem id")

    results = {pid: check(args.base, pid, alt) for pid in ids}

    bad = [pid for pid, ok in results.items() if not ok]
    print(f"\n{len(results) - len(bad)}/{len(results)} clean.")
    if bad:
        print(f"Needs attention: {bad}")
        sys.exit(1)


if __name__ == "__main__":
    main()
