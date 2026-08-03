import React, { useState, useMemo } from 'react';
import { 
  Layers, ChevronRight, PlayCircle, FolderTree, Network, Cpu, 
  GitBranch, BarChart3, Binary, Link2, Search, Brain, Hash, 
  RefreshCcw, Zap, Type, Binary as BitIcon, Layers3, X, Filter, 
  Sparkles, ChevronDown, ChevronUp
} from 'lucide-react';

export default function Sidebar({ problems, activeProblemId, activeCategory, onSelectCategory, onSelectProblem }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [showCategoryGrid, setShowCategoryGrid] = useState(true);

  // Alphabetically sorted (A-Z) Category definitions
  const categories = useMemo(() => [
    { id: 'Advanced Graphs', label: 'Adv Graphs', fullLabel: 'Advanced Graphs', icon: Cpu, color: '#a855f7' },
    { id: 'Arrays', label: 'Arrays & Math', fullLabel: 'Arrays & Math', icon: Binary, color: '#ec4899' },
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

  const normalizeCategory = (cat) => {
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
  };

  // Dynamic problem filtering based on search query
  const searchedProblems = useMemo(() => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return problems;
    
    // 1. Primary: Match on title, ID, or subcategory for specific algorithm search
    const titleMatches = problems.filter(p => 
      (p.title && p.title.toLowerCase().includes(q)) ||
      (p.id && p.id.toLowerCase().includes(q)) ||
      (p.subcategory && p.subcategory.toLowerCase().includes(q))
    );

    if (titleMatches.length > 0) return titleMatches;

    // 2. Secondary fallback: Match on category or difficulty if no title match
    return problems.filter(p => 
      (p.category && p.category.toLowerCase().includes(q)) ||
      (p.difficulty && p.difficulty.toLowerCase().includes(q))
    );
  }, [problems, searchQuery]);

  // Category problem count map updated dynamically with search results
  const categoryCounts = useMemo(() => {
    const counts = {};
    searchedProblems.forEach(p => {
      const normCat = normalizeCategory(p.category);
      counts[normCat] = (counts[normCat] || 0) + 1;
    });
    return counts;
  }, [searchedProblems]);

  // Final filtered problems list to display (searches globally when searchQuery is present)
  const filteredProblems = useMemo(() => {
    const q = searchQuery.toLowerCase().trim();
    if (q) {
      // When searching, if user clicked a specific category, respect category filter if it has matches
      if (activeCategory) {
        const catMatches = searchedProblems.filter((p) => normalizeCategory(p.category) === activeCategory);
        if (catMatches.length > 0) return catMatches;
      }
      return searchedProblems; // Global search fallback
    }
    if (!activeCategory) return problems;
    return problems.filter((p) => normalizeCategory(p.category) === activeCategory);
  }, [problems, searchedProblems, activeCategory, searchQuery]);

  const getBadgeClass = (difficulty) => {
    switch (difficulty?.toLowerCase()) {
      case 'easy': return 'badge-easy';
      case 'medium': return 'badge-medium';
      case 'hard': return 'badge-hard';
      default: return 'badge-easy';
    }
  };

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
          <Search size={16} color="var(--accent-indigo)" />
          Search & Explore
        </h3>

        {activeCategory && (
          <button
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

      {/* Prominent Search Input */}
      <div style={{ position: 'relative', width: '100%' }}>
        <Search size={14} color="var(--text-muted)" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search Algorithms, Problems, or Code..."
          style={{
            width: '100%',
            padding: '9px 30px 9px 34px',
            fontSize: '0.78rem',
            borderRadius: '8px',
            border: searchQuery ? '1px solid var(--accent-indigo)' : '1px solid var(--border-color)',
            background: 'rgba(0, 0, 0, 0.35)',
            color: '#ffffff',
            outline: 'none',
            transition: 'all 0.2s ease',
            boxShadow: searchQuery ? '0 0 12px rgba(99, 102, 241, 0.25)' : 'none'
          }}
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            style={{ position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
          >
            <X size={13} />
          </button>
        )}
      </div>

      {/* Recent Searches Section */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
        <span style={{ fontSize: '0.68rem', fontWeight: '800', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px' }}>
          Recent Searches
        </span>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
          {['Two Sum', 'Binary Tree Traversal', 'HashMap Lookup', 'HashMap Traversal'].map((item) => (
            <button
              key={item}
              onClick={() => setSearchQuery(item)}
              style={{
                fontSize: '0.72rem',
                color: 'var(--text-secondary)',
                background: 'rgba(255, 255, 255, 0.035)',
                border: '1px solid var(--border-color)',
                borderRadius: '6px',
                padding: '4px 9px',
                cursor: 'pointer',
                transition: 'all 0.15s ease'
              }}
            >
              {item}
            </button>
          ))}
        </div>
      </div>

      {/* 2-Column Categories Grid with Popular Tags */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '0.68rem', fontWeight: '800', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px' }}>
              Categories
            </span>
            <span style={{ fontSize: '0.65rem', padding: '2px 6px', borderRadius: '4px', background: 'rgba(168, 85, 247, 0.2)', color: '#c084fc', border: '1px solid rgba(168, 85, 247, 0.3)', fontWeight: '700' }}>
              Popular Tags
            </span>
          </div>
          <button
            onClick={() => setShowCategoryGrid(!showCategoryGrid)}
            style={{ fontSize: '0.7rem', color: 'var(--text-muted)', background: 'none', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '2px' }}
          >
            {showCategoryGrid ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
          </button>
        </div>

        {showCategoryGrid && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px' }}>
            {/* "All" Category Tile */}
            <button
              onClick={() => onSelectCategory(null)}
              style={{
                display: 'flex',
                alignItems: 'center',
                justify: 'space-between',
                gap: '4px',
                padding: '7px 9px',
                borderRadius: '8px',
                border: !activeCategory ? '1px solid var(--accent-indigo)' : '1px solid var(--border-color)',
                background: !activeCategory ? 'rgba(99, 102, 241, 0.22)' : 'rgba(255, 255, 255, 0.025)',
                color: !activeCategory ? '#ffffff' : 'var(--text-secondary)',
                fontSize: '0.75rem',
                fontWeight: !activeCategory ? '700' : '500',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                boxShadow: !activeCategory ? '0 0 10px rgba(99, 102, 241, 0.3)' : 'none'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '5px', overflow: 'hidden' }}>
                <Sparkles size={13} color={!activeCategory ? '#38bdf8' : 'var(--text-muted)'} />
                <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>All Topics</span>
              </div>
              <span style={{ fontSize: '0.65rem', fontWeight: '700', color: 'var(--text-muted)' }}>{problems.length}</span>
            </button>

            {categories.map((cat) => {
              const Icon = cat.icon;
              const isCatActive = activeCategory === cat.id;
              const count = categoryCounts[cat.id] || 0;

              return (
                <button
                  key={cat.id}
                  onClick={() => onSelectCategory(isCatActive ? null : cat.id)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justify: 'space-between',
                    gap: '4px',
                    padding: '7px 9px',
                    borderRadius: '8px',
                    border: isCatActive ? `1px solid ${cat.color}` : '1px solid var(--border-color)',
                    background: isCatActive ? `${cat.color}25` : 'rgba(255, 255, 255, 0.025)',
                    color: isCatActive ? '#ffffff' : 'var(--text-secondary)',
                    fontSize: '0.75rem',
                    fontWeight: isCatActive ? '700' : '500',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    boxShadow: isCatActive ? `0 0 10px ${cat.color}35` : 'none'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '5px', overflow: 'hidden' }}>
                    <Icon size={13} color={isCatActive ? cat.color : 'var(--text-muted)'} />
                    <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{cat.label}</span>
                  </div>
                  {count > 0 && (
                    <span style={{ fontSize: '0.65rem', fontWeight: '700', color: isCatActive ? cat.color : 'var(--text-muted)' }}>
                      {count}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Problems List */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px' }}>
            {activeCategory || 'All'} Problems ({filteredProblems.length})
          </span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {filteredProblems.length > 0 ? (
            filteredProblems.map((prob) => {
              const isProblemActive = activeProblemId === prob.id;
              return (
                <div
                  key={prob.id}
                  onClick={() => onSelectProblem(prob.id)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justify: 'space-between',
                    padding: '9px 11px',
                    borderRadius: '9px',
                    border: isProblemActive ? '1px solid var(--accent-indigo)' : '1px solid rgba(255, 255, 255, 0.05)',
                    background: isProblemActive ? 'rgba(99, 102, 241, 0.15)' : 'rgba(255, 255, 255, 0.02)',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    boxShadow: isProblemActive ? '0 0 10px rgba(99, 102, 241, 0.25)' : 'none'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '9px', overflow: 'hidden' }}>
                    <PlayCircle size={15} color={isProblemActive ? 'var(--accent-purple)' : 'var(--text-muted)'} style={{ flexShrink: 0 }} />
                    <div style={{ overflow: 'hidden' }}>
                      <div style={{ fontSize: '0.81rem', fontWeight: isProblemActive ? '700' : '500', color: isProblemActive ? '#ffffff' : 'var(--text-secondary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {prob.title}
                      </div>
                      <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {prob.subcategory || prob.category}
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '5px', flexShrink: 0 }}>
                    <span className={`badge ${getBadgeClass(prob.difficulty)}`}>
                      {prob.difficulty}
                    </span>
                    <ChevronRight size={14} color="var(--text-muted)" />
                  </div>
                </div>
              );
            })
          ) : (
            <div style={{ textAlign: 'center', padding: '20px 10px', color: 'var(--text-muted)', fontSize: '0.8rem', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <span>No matching algorithms in <strong>{activeCategory || 'catalog'}</strong> for "{searchQuery}".</span>
              {activeCategory && searchedProblems.length > 0 && (
                <button
                  onClick={() => onSelectCategory(null)}
                  style={{ fontSize: '0.75rem', padding: '4px 10px', background: 'var(--accent-indigo)', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: '600' }}
                >
                  Search across all {searchedProblems.length} matching algorithms
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </aside>
  );
}
