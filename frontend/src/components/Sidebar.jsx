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
    { id: 'Advanced Graphs', label: 'Adv Graphs', fullLabel: 'Advanced Graphs', icon: Cpu, color: '#a855f7' },
    { id: 'Arrays', label: 'Arrays', fullLabel: 'Arrays', icon: Binary, color: '#ec4899' },
    { id: 'Binary Search', label: 'Binary Search', fullLabel: 'Binary Search', icon: Search, color: '#f97316' },
    { id: 'Binary Search Trees', label: 'BST', fullLabel: 'Binary Search Trees', icon: GitBranch, color: '#34d399' },
    { id: 'Binary Trees', label: 'Binary Trees', fullLabel: 'Binary Trees', icon: FolderTree, color: '#10b981' },
    { id: 'Bit Manipulation', label: 'Bit Logic', fullLabel: 'Bit Manipulation', icon: BitIcon, color: '#3b82f6' },
    { id: 'Dynamic Programming', label: 'Dynamic Prog', fullLabel: 'Dynamic Programming', icon: Brain, color: '#8b5cf6' },
    { id: 'Graph BFS/DFS', label: 'Graph BFS/DFS', fullLabel: 'Graph BFS & DFS', icon: Network, color: '#38bdf8' },
    { id: 'Greedy Algorithms', label: 'Greedy', fullLabel: 'Greedy Algorithms', icon: Zap, color: '#eab308' },
    { id: 'Heaps & PriorityQueue', label: 'Heaps & PQ', fullLabel: 'Heaps & PriorityQueue', icon: Layers3, color: '#f43f5e' },
    { id: 'Linked List', label: 'Linked Lists', fullLabel: 'Linked Lists', icon: Link2, color: '#06b6d4' },
    { id: 'Recursion & Backtracking', label: 'Backtracking', fullLabel: 'Recursion & Backtracking', icon: RefreshCcw, color: '#f59e0b' },
    { id: 'Sliding Window', label: 'Sliding Window', fullLabel: 'Sliding Window', icon: Filter, color: '#14b8a6' },
    { id: 'Sorting Algorithms', label: 'Sorting', fullLabel: 'Sorting Algorithms', icon: BarChart3, color: '#6366f1' },
    { id: 'Stack & Queue', label: 'Stack & Queue', fullLabel: 'Stack & Queue', icon: Layers, color: '#c084fc' },
    { id: 'Strings', label: 'Strings', fullLabel: 'Strings', icon: Type, color: '#64748b' },
    { id: 'Tries & Prefixes', label: 'Tries', fullLabel: 'Tries & Prefixes', icon: Hash, color: '#0ea5e9' }
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
      className="glass-panel" 
      style={{ 
        width: '320px', 
        minWidth: '320px', 
        height: 'calc(100vh - 90px)', 
        overflowY: 'auto', 
        padding: '16px', 
        display: 'flex', 
        flexDirection: 'column', 
        gap: '14px' 
      }}
    >
      {/* Header & Title */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h3 style={{ fontSize: '0.96rem', fontWeight: '800', letterSpacing: '0.3px', color: '#ffffff', margin: 0, display: 'flex', alignItems: 'center', gap: '6px' }}>
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
              color: '#38bdf8',
              background: 'rgba(56, 189, 248, 0.12)',
              border: '1px solid rgba(56, 189, 248, 0.3)',
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
              <span style={{ fontSize: '0.65rem', padding: '2px 6px', borderRadius: '4px', background: 'rgba(168, 85, 247, 0.2)', color: '#c084fc', border: '1px solid rgba(168, 85, 247, 0.3)', fontWeight: '700' }}>
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
                  <Sparkles size={12} color={!activeCategory ? 'var(--accent-violet)' : 'var(--text-muted)'} />
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
                      <Icon size={12} color={isCatActive ? 'var(--accent-violet)' : 'var(--text-muted)'} />
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
