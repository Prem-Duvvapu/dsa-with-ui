import ArrayCanvas from '../components/ArrayCanvas';
import DpTableCanvas from '../components/DpTableCanvas';
import DsuCanvas from '../components/DsuCanvas';
import GraphCanvas from '../components/GraphCanvas';
import GridCanvas from '../components/GridCanvas';
import LinkedListCanvas from '../components/LinkedListCanvas';
import { QueueHeroCanvas } from '../components/QueueCanvas';
import RecursionTreeCanvas from '../components/RecursionTreeCanvas';
import StackCanvas from '../components/StackCanvas';
import TreeCanvas from '../components/TreeCanvas';
import TrieCanvas from '../components/TrieCanvas';

/**
 * The sole dsType-to-renderer routing table.
 *
 * Types whose dedicated canvas has not landed yet retain a placeholder renderer until
 * their PROMPT-F-visual-fidelity.md slice does. `Stack` and `Queue` are the hero mappings
 * for a problem whose stack/queue IS the picture, reading `queueOrStackState`
 * (StepEmitter.stack()/.queue()) rather than `arrayState` — every DsType.STACK tracer
 * emits it already; ArrayCanvas would draw the wrong structure. `Queue` routes to the hero
 * variant of QueueCanvas, which widens the pane the companion usage keeps narrow.
 * bfs-traversal is `Graph`-hero with a queue companion pane instead (see
 * canvas/companions.js) because its graph topology is the point.
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
  Stack: StackCanvas,
  Queue: QueueHeroCanvas,
  PriorityQueue: ArrayCanvas,
  Trie: TrieCanvas,
  RecursionTree: RecursionTreeCanvas,
  Dsu: DsuCanvas
});
