/**
 * Collapses the backend's free-text `category` strings onto the fixed set of
 * category ids the sidebar tiles use.
 *
 * Single source of truth on purpose. This ladder previously existed verbatim in
 * both Sidebar.jsx (for the tile counts) and SearchBox.jsx (for filtering), which
 * meant the counts and the filtered list could disagree the moment either copy
 * was edited.
 *
 * Order matters: the checks run most-specific first.
 */
export function normalizeCategory(cat) {
  if (!cat) return 'Arrays';
  if (cat === 'BST' || cat.startsWith('BST') || cat.includes('Binary Search Tree')) return 'Binary Search Trees';
  if (cat.startsWith('Binary Trees') || cat.includes('Tree')) return 'Binary Trees';
  if (cat === 'Graph BFS/DFS') return 'Graph BFS/DFS';
  if (cat.includes('Graph')) return 'Advanced Graphs';
  if (cat.includes('Recursion') || cat.includes('Backtracking')) return 'Recursion & Backtracking';
  if (cat.includes('Sort')) return 'Sorting Algorithms';
  if (cat.includes('Array')) return 'Arrays';
  if (cat.includes('List') || cat.includes('Linked')) return 'Linked List';
  if (cat.includes('Binary Search')) return 'Binary Search';
  if (cat.includes('DP') || cat.includes('Dynamic')) return 'Dynamic Programming';
  if (cat.includes('Trie')) return 'Tries & Prefixes';
  if (cat.includes('Greedy')) return 'Greedy Algorithms';
  if (cat.includes('String')) return 'Strings';
  if (cat.includes('Bit') || cat.includes('Math')) return 'Bit Manipulation';
  if (cat.includes('Heap') || cat.includes('Priority')) return 'Heaps & PriorityQueue';
  if (cat.includes('Sliding Window') || cat.includes('Window')) return 'Sliding Window';
  if (cat.includes('Stack') || cat.includes('Queue')) return 'Stack & Queue';
  return cat;
}

export default normalizeCategory;
