import ArrayCanvas from '../components/ArrayCanvas';
import DpTableCanvas from '../components/DpTableCanvas';
import DsuCanvas from '../components/DsuCanvas';
import GraphCanvas from '../components/GraphCanvas';
import GridCanvas from '../components/GridCanvas';
import LinkedListCanvas from '../components/LinkedListCanvas';
import RecursionTreeCanvas from '../components/RecursionTreeCanvas';
import TreeCanvas from '../components/TreeCanvas';
import TrieCanvas from '../components/TrieCanvas';

/**
 * The sole dsType-to-renderer routing table.
 *
 * Phase 0 closes the vocabulary without adding canvases. Types whose dedicated canvas
 * belongs to Phase 3 therefore retain the renderer they use today until that phase lands.
 */
export const CANVAS_BY_DSTYPE = Object.freeze({
  Array: ArrayCanvas,
  Window: ArrayCanvas,
  SearchSpace: ArrayCanvas,
  Matrix: GridCanvas,
  DpTable: DpTableCanvas,
  String: ArrayCanvas,
  Bits: ArrayCanvas,
  Tree: TreeCanvas,
  Graph: GraphCanvas,
  LinkedList: LinkedListCanvas,
  Stack: ArrayCanvas,
  Queue: GraphCanvas,
  PriorityQueue: ArrayCanvas,
  Trie: TrieCanvas,
  RecursionTree: RecursionTreeCanvas,
  Dsu: DsuCanvas
});
