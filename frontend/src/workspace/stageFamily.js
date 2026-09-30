/**
 * How much room each diagram family starts with, and what the stage calls it.
 *
 * Keyed by the same `dsType` contract as canvas/registry.js. `sequence` structures read
 * along one axis (cells, a list, a stack); `spatial` ones need height to show relationships
 * (trees, graphs, tables). The sizes are starting points from IMPLEMENTATION_HANDOFF.md §4.3
 * - canvases may grow beyond them or scroll locally - and are applied in CSS by family, so
 * nothing here depends on a problem's id or title.
 */
export const STAGE_BY_DSTYPE = Object.freeze({
  Array: { label: 'Array', family: 'sequence' },
  Bits: { label: 'Bits', family: 'sequence' },
  Window: { label: 'Sliding window', family: 'sequence' },
  SearchSpace: { label: 'Search space', family: 'sequence' },
  String: { label: 'String', family: 'sequence' },
  Stack: { label: 'Stack', family: 'sequence' },
  Queue: { label: 'Queue', family: 'sequence' },
  LinkedList: { label: 'Linked list', family: 'sequence' },
  Interval: { label: 'Intervals', family: 'sequence' },
  PriorityQueue: { label: 'Heap', family: 'spatial' },
  Matrix: { label: 'Grid', family: 'spatial' },
  DpTable: { label: 'DP table', family: 'spatial' },
  Tree: { label: 'Tree', family: 'spatial' },
  Graph: { label: 'Graph', family: 'spatial' },
  Trie: { label: 'Trie', family: 'spatial' },
  RecursionTree: { label: 'Recursion tree', family: 'spatial' },
  Dsu: { label: 'Disjoint sets', family: 'spatial' }
});

export function stageFor(dsType) {
  return STAGE_BY_DSTYPE[dsType] ?? { label: 'Visualization', family: 'sequence' };
}
