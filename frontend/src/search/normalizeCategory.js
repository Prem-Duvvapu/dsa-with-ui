import CATEGORY_CONTRACT from '../../../contracts/categories.json';

/**
 * Maps a problem's `category` onto the fixed set the sidebar tiles use.
 *
 * <p>This used to be a twenty-line ladder of `cat.includes(...)` guesses, because the
 * backend's categories were free text that did not match the tile ids: `BST` had to become
 * `Binary Search Trees`, and anything containing "Graph" was folded into `Advanced Graphs`.
 * It worked, and it was one substring away from silently misfiling a whole topic - merging
 * the two graph categories into `Graphs` immediately hit `cat.includes('Graph')` and routed
 * all fifty-eight problems to a tile that no longer existed.
 *
 * The backend now serves exactly the categories in contracts/categories.json, asserted from
 * both sides (CategoryContractTest and Sidebar.categories.test.js). So there is nothing left
 * to guess: a known category passes through unchanged, and anything else is surfaced rather
 * than quietly folded into whichever tile happened to match first.
 */
const KNOWN = new Set(CATEGORY_CONTRACT.map(({ category }) => category));

export function normalizeCategory(cat) {
  if (!cat) return 'Arrays';
  if (KNOWN.has(cat)) return cat;
  // An unrecognised category means the backend added one without the contract. Returning it
  // unchanged leaves it out of every tile, which is visible, rather than misfiled, which is not.
  return cat;
}

export default normalizeCategory;
