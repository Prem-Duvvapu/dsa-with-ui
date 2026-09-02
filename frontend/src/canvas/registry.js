import ArrayCanvas from '../components/ArrayCanvas';
import DpTableCanvas from '../components/DpTableCanvas';
import DsuCanvas from '../components/DsuCanvas';
import GraphCanvas from '../components/GraphCanvas';
import GridCanvas from '../components/GridCanvas';
import LinkedListCanvas from '../components/LinkedListCanvas';
import QueueCanvas from '../components/QueueCanvas';
import RecursionTreeCanvas from '../components/RecursionTreeCanvas';
import TreeCanvas from '../components/TreeCanvas';
import TrieCanvas from '../components/TrieCanvas';

/**
 * The sole dsType-to-renderer routing table.
 *
 * Types whose dedicated canvas has not landed yet retain a placeholder renderer until
 * their PROMPT-F-visual-fidelity.md slice does. `Queue` is the hero mapping for a problem
 * whose queue IS the picture; bfs-traversal is `Graph`-hero with a queue companion pane
 * instead (see canvas/companions.js) because its graph topology is the point.
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
  Queue: QueueCanvas,
  PriorityQueue: ArrayCanvas,
  Trie: TrieCanvas,
  RecursionTree: RecursionTreeCanvas,
  Dsu: DsuCanvas
});
