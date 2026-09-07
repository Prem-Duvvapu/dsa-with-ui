import React, { useState, useMemo } from 'react';
import { 
  Layers, FolderTree, Network, Cpu, 
  GitBranch, BarChart3, Binary, Link2, Search, Brain, Hash, 
  RefreshCcw, Zap, Type, Binary as BitIcon, Layers3, X, Filter, 
  Sparkles, ChevronDown, ChevronUp
} from 'lucide-react';
import SearchBox from './SearchBox';
import { normalizeCategory } from '../search/normalizeCategory';

export default function Sidebar({ problems = [], activeProblemId, activeCategory, onSelectCategory, onSelectProblem, onRetry }) {
  const [showCategoryGrid, setShowCategoryGrid] = useState(true);

  // Alphabetically sorted (A-Z) Category definitions
  const categories = useMemo(() => [
    { id: 'Advanced Graphs', label: 'Adv Graphs', fullLabel: 'Advanced Graphs', icon: Cpu },
    { id: 'Arrays', label: 'Arrays', fullLabel: 'Arrays', icon: Binary },
    { id: 'Binary Search', label: 'Binary Search', fullLabel: 'Binary Search', icon: Search },
    { id: 'Binary Search Trees', label: 'BST', fullLabel: 'Binary Search Trees', icon: GitBranch },
    { id: 'Binary Trees', label: 'Binary Trees', fullLabel: 'Binary Trees', icon: FolderTree },
    { id: 'Bit Manipulation', label: 'Bit Logic', fullLabel: 'Bit Manipulation', icon: BitIcon },
    { id: 'Dynamic Programming', label: 'Dynamic Prog', fullLabel: 'Dynamic Programming', icon: Brain },
    { id: 'Graph BFS/DFS', label: 'Graph BFS/DFS', fullLabel: 'Graph BFS & DFS', icon: Network },
    { id: 'Greedy Algorithms', label: 'Greedy', fullLabel: 'Greedy Algorithms', icon: Zap },
    { id: 'Heaps & PriorityQueue', label: 'Heaps & PQ', fullLabel: 'Heaps & PriorityQueue', icon: Layers3 },
    { id: 'Linked List', label: 'Linked Lists', fullLabel: 'Linked Lists', icon: Link2 },
    { id: 'Recursion & Backtracking', label: 'Backtracking', fullLabel: 'Recursion & Backtracking', icon: RefreshCcw },
    { id: 'Sliding Window', label: 'Sliding Window', fullLabel: 'Sliding Window', icon: Filter },
    { id: 'Sorting Algorithms', label: 'Sorting', fullLabel: 'Sorting Algorithms', icon: BarChart3 },
    { id: 'Stack & Queue', label: 'Stack & Queue', fullLabel: 'Stack & Queue', icon: Layers },
    { id: 'Strings', label: 'Strings', fullLabel: 'Strings', icon: Type },
    { id: 'Tries & Prefixes', label: 'Tries', fullLabel: 'Tries & Prefixes', icon: Hash }
  ], []);


  // Category problem count map
  const categoryCounts = useMemo(() => {
    const counts = {};
    if (Array.isArray(problems)) {
      problems.forEach(p => {
        const normCat = normalizeCategory(p.category);
        counts[normCat] = (counts[normCat] || 0) + 1;
      });
    }
    return counts;
  }, [problems]);

  return (
    <aside 
      className="glass-panel sidebar-panel"
    >
      {/* Header & Title */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h3 className="sidebar-title">
          <Search size={16} color="var(--accent-violet)" />
          Search & Explore
        </h3>

        {activeCategory && (
          <button
            type="button"
            onClick={() => onSelectCategory(null)}
            style={{
              fontSize: '0.7rem',
              fontWeight: '700',
              color: 'var(--bench-ink-secondary)',
              background: 'var(--bench-fill)',
              border: '1px solid var(--bench-rule-strong)',
              borderRadius: '6px',
              padding: '3px 7px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '4px'
            }}
          >
            All <X size={12} />
          </button>
        )}
      </div>

      {/* Main SearchBox with Category Grid injected as child */}
      <SearchBox
        problems={problems}
        activeProblemId={activeProblemId}
        activeCategory={activeCategory}
        onSelectCategory={onSelectCategory}
        onSelectProblem={onSelectProblem}
        onRetry={onRetry}
      >
        {/* 2-Column Categories Grid with Popular Tags */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span className="sb-eyebrow">
                Categories
              </span>
              <span style={{ fontSize: '0.65rem', padding: '2px 6px', borderRadius: '4px', background: 'var(--bench-fill)', color: 'var(--bench-ink-secondary)', border: '1px solid var(--bench-rule-strong)', fontWeight: '700' }}>
                Popular Tags
              </span>
            </div>
            <button
              type="button"
              onClick={() => setShowCategoryGrid(!showCategoryGrid)}
              className="sb-disclosure"
              aria-expanded={showCategoryGrid}
              aria-label={showCategoryGrid ? 'Hide categories' : 'Show categories'}
            >
              {showCategoryGrid ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
            </button>
          </div>

          {showCategoryGrid && (
            <div className="sb-cat-grid">
              {/* "All" Category Tile */}
              <button
                type="button"
                className={!activeCategory ? 'sb-cat sb-cat-on' : 'sb-cat'}
                onClick={() => onSelectCategory(null)}
              >
                <span className="sb-cat-label">
                  <Sparkles size={12} color={!activeCategory ? 'var(--probe)' : 'var(--bench-ink-dim)'} />
                  <span>All topics</span>
                </span>
                <span className="sb-cat-count">{problems.length}</span>
              </button>

              {categories.map((cat) => {
                const Icon = cat.icon;
                const isCatActive = activeCategory === cat.id;
                const count = categoryCounts[cat.id] || 0;

                return (
                  <button
                    key={cat.id}
                    type="button"
                    className={isCatActive ? 'sb-cat sb-cat-on' : 'sb-cat'}
                    aria-pressed={isCatActive}
                    onClick={() => onSelectCategory(isCatActive ? null : cat.id)}
                  >
                    <span className="sb-cat-label">
                      <Icon size={12} color={isCatActive ? 'var(--probe)' : 'var(--bench-ink-dim)'} />
                      <span>{cat.label}</span>
                    </span>
                    {count > 0 && <span className="sb-cat-count">{count}</span>}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </SearchBox>
    </aside>
  );
}
