import ArrayCanvas from '../components/ArrayCanvas';
import StringCanvas from '../components/StringCanvas';
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
import IntervalCanvas from '../components/IntervalCanvas';
import WindowCanvas from '../components/WindowCanvas';
import SearchSpaceCanvas from '../components/SearchSpaceCanvas';
import HeapCanvas from '../components/HeapCanvas';

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
 *
 * `Window` and `SearchSpace` had both been placeholders routed to ArrayCanvas. Bars show
 * the values but not the thing being taught: for a window, the bounds moving and
 * stretching; for a binary search, the space halving. Each now has the canvas its own
 * question needs.
 *
 * `String` was the same placeholder, with the same symptom: StepEmitter.chars() writes
 * a character's Unicode code point into the same `value` field .array() uses for a
 * number, so ArrayCanvas drew it as a bar sized by code point with the letter itself
 * demoted to a small caption. StringCanvas reads the same arrayState and draws the
 * letters as the structure - a row of character cells - instead of a number chart.
 *
 * `Bits` stays on ArrayCanvas by design, not as a placeholder: StepEmitter.bits()
 * renders a fixed 32-wide MSB-to-LSB track over the same arrayState shape, and that
 * track reads fine as bars (each cell is 0 or 1, so height carries no false precision).
 */
export const CANVAS_BY_DSTYPE = Object.freeze({
  Array: ArrayCanvas,
  Window: WindowCanvas,
  SearchSpace: SearchSpaceCanvas,
  Matrix: GridCanvas,
  DpTable: DpTableCanvas,
  String: StringCanvas,
  Bits: ArrayCanvas,
  Tree: TreeCanvas,
  Graph: GraphCanvas,
  LinkedList: LinkedListCanvas,
  Stack: StackCanvas,
  Queue: QueueHeroCanvas,
  PriorityQueue: HeapCanvas,
  Trie: TrieCanvas,
  RecursionTree: RecursionTreeCanvas,
  Dsu: DsuCanvas,
  Interval: IntervalCanvas
});
