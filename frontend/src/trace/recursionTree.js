/**
 * Rebuilds the recursion tree from the call stacks the tracers already emit.
 *
 * None of the twenty-five Recursion & Backtracking tracers emits `treeNodes`; every one of
 * them emits `callStack`. That turns out to be enough, because a call stack over time IS
 * the tree: each step's stack is one root-to-current path, and the sequence of paths is the
 * traversal. Deriving it here costs no tracer changes and cannot drift from the trace,
 * since it is computed from the trace.
 *
 * The subtle part is re-entry. A stack going [A,B] -> [A] -> [A,B] has NOT revisited the
 * same B: backtracking abandoned one branch and started another that happens to be named
 * the same. Matching on label alone would merge those two explorations into one node and
 * quietly halve the tree. So a node is identified by where it diverged from the previous
 * step's path, not by its label - the moment a stack differs from its predecessor at some
 * depth, everything below that point is new.
 */

/** How many leading frames two stacks share. */
function commonPrefix(a, b) {
  let i = 0;
  while (i < a.length && i < b.length && a[i] === b[i]) i += 1;
  return i;
}

/**
 * @param {Array} steps        the trace
 * @param {number} upTo        index of the step being shown
 * @param {number} maxNodes    cap, so a 5000-step run cannot try to draw 5000 nodes
 * @returns {{nodes: Array, truncated: boolean}} nodes carry id, label, depth, parentId, state
 */
export function buildRecursionTree(steps, upTo, maxNodes = 220) {
  if (!Array.isArray(steps) || steps.length === 0) return { nodes: [], truncated: false };

  const nodes = [];
  let previous = [];
  let path = [];          // node ids for the frames currently on the stack
  let truncated = false;

  const last = Math.min(upTo ?? steps.length - 1, steps.length - 1);
  for (let i = 0; i <= last; i += 1) {
    const stack = steps[i]?.callStack;
    if (!Array.isArray(stack) || stack.length === 0) continue;

    const shared = commonPrefix(previous, stack);

    // Frames that left the stack are finished, not merely inactive.
    for (let d = shared; d < path.length; d += 1) {
      const done = nodes[path[d]];
      if (done) done.state = 'done';
    }
    path = path.slice(0, shared);

    for (let d = shared; d < stack.length; d += 1) {
      if (nodes.length >= maxNodes) { truncated = true; break; }
      const id = nodes.length;
      nodes.push({
        id,
        label: stack[d],
        depth: d,
        parentId: d === 0 ? null : path[d - 1] ?? null,
        state: 'open'
      });
      path.push(id);
    }
    previous = stack;
  }

  // Whatever is still on the stack at the step being shown is the live path; its deepest
  // frame is where the algorithm actually is.
  path.forEach((id, d) => {
    const node = nodes[id];
    if (!node) return;
    node.state = d === path.length - 1 ? 'current' : 'active';
  });

  return { nodes, truncated };
}

/**
 * Lays the tree out for drawing: leaves take consecutive columns and a parent centres over
 * its children, which is what makes a backtracking tree readable rather than a list.
 */
export function layoutRecursionTree(nodes) {
  if (!nodes.length) return { positions: new Map(), width: 0, depth: 0 };

  const childrenOf = new Map();
  for (const n of nodes) {
    if (n.parentId === null) continue;
    if (!childrenOf.has(n.parentId)) childrenOf.set(n.parentId, []);
    childrenOf.get(n.parentId).push(n.id);
  }

  const positions = new Map();
  let nextColumn = 0;

  const place = (id) => {
    const kids = childrenOf.get(id) || [];
    if (kids.length === 0) {
      positions.set(id, nextColumn);
      nextColumn += 1;
      return positions.get(id);
    }
    const spans = kids.map(place);
    positions.set(id, (Math.min(...spans) + Math.max(...spans)) / 2);
    return positions.get(id);
  };

  nodes.filter((n) => n.parentId === null).forEach((n) => place(n.id));

  return {
    positions,
    width: Math.max(1, nextColumn),
    depth: Math.max(...nodes.map((n) => n.depth)) + 1
  };
}
