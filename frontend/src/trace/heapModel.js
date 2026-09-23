/**
 * A heap's two views, from whichever one the tracer emitted.
 *
 * The defining idea of a binary heap is that the tree and the array are the SAME thing:
 * the node at index i has its children at 2i+1 and 2i+2, and its parent at (i-1)/2. There
 * is no pointer anywhere. A canvas that draws only the tree hides the array, and one that
 * draws only the array hides the shape - and the relationship between them is the lesson.
 *
 * So both are always available. Tracers that model heap mechanics emit `treeNodes`; those
 * that use a heap as a tool emit `arrayState`. Either one determines the other, because
 * the mapping is pure arithmetic.
 */

export const parentOf = (i) => (i <= 0 ? null : Math.floor((i - 1) / 2));
export const leftOf = (i) => 2 * i + 1;
export const rightOf = (i) => 2 * i + 2;

/** Level-order from an explicit tree, so index arithmetic applies to it as well. */
function levelOrder(treeNodes) {
  if (!Array.isArray(treeNodes) || treeNodes.length === 0) return [];
  const byId = new Map(treeNodes.map((n) => [n.id, n]));
  const childIds = new Set();
  treeNodes.forEach((n) => {
    if (n.leftId !== null && n.leftId !== undefined) childIds.add(n.leftId);
    if (n.rightId !== null && n.rightId !== undefined) childIds.add(n.rightId);
  });
  const root = treeNodes.find((n) => !childIds.has(n.id)) ?? treeNodes[0];

  const out = [];
  const queue = [root];
  const seen = new Set();
  while (queue.length) {
    const node = queue.shift();
    if (!node || seen.has(node.id)) continue;
    seen.add(node.id);
    out.push({ value: node.val ?? node.value ?? '', state: node.state || 'default' });
    [node.leftId, node.rightId].forEach((id) => {
      if (id !== null && id !== undefined && byId.has(id)) queue.push(byId.get(id));
    });
  }
  return out;
}

/**
 * @returns {{slots: Array<{value: string, state: string}>, source: 'tree'|'array'|null}}
 */
export function heapSlots(step) {
  if (Array.isArray(step?.treeNodes) && step.treeNodes.length) {
    return { slots: levelOrder(step.treeNodes), source: 'tree' };
  }
  if (Array.isArray(step?.arrayState) && step.arrayState.length) {
    return {
      slots: step.arrayState.map((c) => ({
        value: String(c.label ?? c.value ?? ''),
        state: c.state || 'default'
      })),
      source: 'array'
    };
  }
  // Stated and empty is a real algorithmic state, and a common one: a heap drained by its
  // last pop, a priority queue emptied while every remaining item cools down. The source
  // survives so the canvas can say "empty" rather than "missing" - the same distinction
  // trace/lastPayload.js draws, here without the look-back, because an empty heap is the
  // truth for THIS step and carrying the previous one forward would show contents the
  // algorithm has just removed.
  if (Array.isArray(step?.treeNodes)) return { slots: [], source: 'tree' };
  if (Array.isArray(step?.arrayState)) return { slots: [], source: 'array' };
  return { slots: [], source: null };
}

/** Row/column position per index, so the tree can be drawn from the array alone. */
export function heapLayout(count) {
  const rows = [];
  for (let i = 0; i < count; i += 1) {
    const depth = Math.floor(Math.log2(i + 1));
    (rows[depth] ??= []).push(i);
  }
  return rows;
}
