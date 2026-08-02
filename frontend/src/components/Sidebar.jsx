import React, { useState, useMemo } from 'react';
import { 
  Layers, ChevronRight, PlayCircle, FolderTree, Network, Cpu, 
  GitBranch, BarChart3, Binary, Link2, Search, Brain, Hash, 
  RefreshCcw, Zap, Type, Binary as BitIcon, Layers3, X, Filter, 
  Sparkles, ChevronDown, ChevronUp, SearchX, Folders, FolderOpen
} from 'lucide-react';

export default function Sidebar({ problems, activeProblemId, activeCategory, onSelectCategory, onSelectProblem }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedTopics, setExpandedTopics] = useState({});
  const [isCategoryFilterExpanded, setIsCategoryFilterExpanded] = useState(false);

  // Alphabetically sorted (A-Z) Category definitions
  const categories = useMemo(() => [
    { id: 'Advanced Graphs', label: 'Advanced Graphs', shortLabel: 'Adv Graphs', icon: Cpu, color: '#a855f7' },
    { id: 'Arrays', label: 'Arrays & Math', shortLabel: 'Arrays', icon: Binary, color: '#ec4899' },
    { id: 'Binary Search', label: 'Binary Search', shortLabel: 'Binary Search', icon: Search, color: '#f97316' },
    { id: 'Binary Search Trees', label: 'Binary Search Trees', shortLabel: 'BST', icon: GitBranch, color: '#34d399' },
    { id: 'Binary Trees', label: 'Binary Trees', shortLabel: 'Trees', icon: FolderTree, color: '#10b981' },
    { id: 'Bit Manipulation', label: 'Bit Manipulation', shortLabel: 'Bit Logic', icon: BitIcon, color: '#3b82f6' },
    { id: 'Dynamic Programming', label: 'Dynamic Programming', shortLabel: 'DP', icon: Brain, color: '#8b5cf6' },
    { id: 'Graph BFS/DFS', label: 'Graph BFS & DFS', shortLabel: 'Graphs', icon: Network, color: '#38bdf8' },
    { id: 'Greedy Algorithms', label: 'Greedy Algorithms', shortLabel: 'Greedy', icon: Zap, color: '#eab308' },
    { id: 'Heaps & PriorityQueue', label: 'Heaps & PriorityQueue', shortLabel: 'Heaps', icon: Layers3, color: '#f43f5e' },
    { id: 'Linked List', label: 'Linked Lists', shortLabel: 'Linked List', icon: Link2, color: '#06b6d4' },
    { id: 'Recursion & Backtracking', label: 'Recursion & Backtracking', shortLabel: 'Backtracking', icon: RefreshCcw, color: '#f59e0b' },
    { id: 'Sliding Window', label: 'Sliding Window', shortLabel: 'Sliding Window', icon: Filter, color: '#14b8a6' },
    { id: 'Sorting Algorithms', label: 'Sorting Algorithms', shortLabel: 'Sorting', icon: BarChart3, color: '#6366f1' },
    { id: 'Stack & Queue', label: 'Stack & Queue', shortLabel: 'Stack & Queue', icon: Layers, color: '#c084fc' },
    { id: 'Strings', label: 'Strings', shortLabel: 'Strings', icon: Type, color: '#64748b' },
    { id: 'Tries & Prefixes', label: 'Tries & Prefixes', shortLabel: 'Tries', icon: Hash, color: '#0ea5e9' }
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

  // Dynamic problem count per normalized category
  const categoryCounts = useMemo(() => {
    const counts = {};
    problems.forEach(p => {
      const normCat = normalizeCategory(p.category);
      counts[normCat] = (counts[normCat] || 0) + 1;
    });
    return counts;
  }, [problems]);

  // Filter problems by active category and search query
  const filteredProblems = useMemo(() => {
    return problems.filter((p) => {
      const normCat = normalizeCategory(p.category);
      const matchesCategory = !activeCategory || normCat === activeCategory;
      const q = searchQuery.toLowerCase().trim();
      const matchesSearch = !q || 
        (p.title && p.title.toLowerCase().includes(q)) ||
        (p.category && p.category.toLowerCase().includes(q)) ||
        (p.subcategory && p.subcategory.toLowerCase().includes(q)) ||
        (p.difficulty && p.difficulty.toLowerCase().includes(q));
      return matchesCategory && matchesSearch;
    });
  }, [problems, activeCategory, searchQuery]);

  // Group problems by Alphabetically Sorted Topic Categories
  const groupedByTopic = useMemo(() => {
    const topicMap = {};
    
    // Initialize empty arrays for all categories in alphabetical order
    categories.forEach(cat => {
      topicMap[cat.id] = [];
    });

    // Populate filtered problems into topicMap
    filteredProblems.forEach((prob) => {
      const normCat = normalizeCategory(prob.category);
      if (!topicMap[normCat]) topicMap[normCat] = [];
      topicMap[normCat].push(prob);
    });

    // Filter out topics with 0 problems when searching or filtering
    const sortedTopics = [];
    categories.forEach(cat => {
      const items = topicMap[cat.id] || [];
      if (items.length > 0) {
        sortedTopics.push({
          categoryObj: cat,
          problems: items
        });
      }
    });

    return sortedTopics;
  }, [categories, filteredProblems]);

  const toggleTopicExpand = (catId) => {
    setExpandedTopics(prev => ({
      ...prev,
      [catId]: !prev[catId] // Toggle individual topic expand/shrink state
    }));
  };

  const expandAllTopics = () => {
    const nextState = {};
    categories.forEach(c => { nextState[c.id] = true; });
    setExpandedTopics(nextState);
  };

  const collapseAllTopics = () => {
    setExpandedTopics({});
  };

  const getBadgeClass = (difficulty) => {
    switch (difficulty?.toLowerCase()) {
      case 'easy': return 'badge-easy';
      case 'medium': return 'badge-medium';
      case 'hard': return 'badge-hard';
      default: return 'badge-easy';
    }
  };

  const activeCategoryObj = categories.find(c => c.id === activeCategory);
  const areAllExpanded = categories.every(c => expandedTopics[c.id]);

  return (
    <aside 
      className="glass-panel" 
      style={{ 
        width: '360px', 
        minWidth: '360px', 
        height: 'calc(100vh - 96px)', 
        display: 'flex', 
        flexDirection: 'column', 
        padding: '16px',
        gap: '14px',
        position: 'relative'
      }}
    >
      {/* Sidebar Top Header & Stats */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '4px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{ width: '28px', height: '28px', borderRadius: '8px', background: 'linear-gradient(135deg, var(--accent-indigo), #38bdf8)', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 0 12px rgba(99, 102, 241, 0.4)' }}>
            <Layers size={16} color="#ffffff" />
          </div>
          <div>
            <h3 style={{ fontSize: '0.98rem', fontWeight: '800', letterSpacing: '0.3px', color: '#ffffff' }}>
              DSA Explorer
            </h3>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: '600' }}>
              {problems.length} Algorithms Cataloged
            </span>
          </div>
        </div>

        {activeCategory && (
          <button
            onClick={() => onSelectCategory(null)}
            style={{
              fontSize: '0.72rem',
              fontWeight: '700',
              color: '#38bdf8',
              background: 'rgba(56, 189, 248, 0.12)',
              border: '1px solid rgba(56, 189, 248, 0.3)',
              borderRadius: '6px',
              padding: '4px 8px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              transition: 'all var(--motion-fast) ease'
            }}
            title="Clear category filter"
          >
            All Categories <X size={12} />
          </button>
        )}
      </div>

      {/* Real-time Search Input Bar */}
      <div style={{ position: 'relative', width: '100%' }}>
        <Search size={15} color="var(--text-muted)" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search topics or algorithms (e.g. merge, dfs, bst)..."
          style={{
            width: '100%',
            padding: '9px 34px 9px 34px',
            fontSize: '0.8rem',
            borderRadius: '10px',
            border: searchQuery ? '1px solid var(--accent-indigo)' : '1px solid var(--border-color)',
            background: 'rgba(0, 0, 0, 0.3)',
            color: '#ffffff',
            outline: 'none',
            transition: 'all var(--motion-fast) ease',
            boxShadow: searchQuery ? '0 0 12px rgba(99, 102, 241, 0.25)' : 'none'
          }}
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            style={{ position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
          >
            <X size={14} />
          </button>
        )}
      </div>

      {/* Category Filter Chips Bar */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span style={{ fontSize: '0.7rem', fontWeight: '800', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px', display: 'flex', alignItems: 'center', gap: '5px' }}>
            <Filter size={12} color="var(--accent-indigo)" />
            Category Filter ({categories.length})
          </span>
          <button
            onClick={() => setIsCategoryFilterExpanded(!isCategoryFilterExpanded)}
            style={{ fontSize: '0.72rem', color: 'var(--accent-indigo)', background: 'none', border: 'none', cursor: 'pointer', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '4px' }}
          >
            {isCategoryFilterExpanded ? 'Compact' : 'Expand All Chips'}
            {isCategoryFilterExpanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
          </button>
        </div>

        {/* Categories Chip Selector */}
        <div 
          style={{ 
            display: 'flex', 
            flexWrap: isCategoryFilterExpanded ? 'wrap' : 'nowrap', 
            overflowX: isCategoryFilterExpanded ? 'visible' : 'auto', 
            gap: '6px', 
            paddingBottom: '4px',
            scrollbarWidth: 'thin'
          }}
        >
          {/* All Categories Chip */}
          <button
            onClick={() => onSelectCategory(null)}
            style={{
              padding: '6px 10px',
              borderRadius: '8px',
              border: !activeCategory ? '1px solid var(--accent-indigo)' : '1px solid var(--border-color)',
              background: !activeCategory ? 'linear-gradient(135deg, rgba(99, 102, 241, 0.3), rgba(56, 189, 248, 0.2))' : 'rgba(255, 255, 255, 0.03)',
              color: !activeCategory ? '#ffffff' : 'var(--text-secondary)',
              fontSize: '0.74rem',
              fontWeight: !activeCategory ? '800' : '600',
              cursor: 'pointer',
              whiteSpace: 'nowrap',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              transition: 'all var(--motion-fast) ease',
              boxShadow: !activeCategory ? '0 0 12px rgba(99, 102, 241, 0.3)' : 'none'
            }}
          >
            <Sparkles size={13} color={!activeCategory ? '#38bdf8' : 'var(--text-muted)'} />
            All ({problems.length})
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
                  padding: '6px 10px',
                  borderRadius: '8px',
                  border: isCatActive ? `1px solid ${cat.color}` : '1px solid var(--border-color)',
                  background: isCatActive ? `${cat.color}25` : 'rgba(255, 255, 255, 0.03)',
                  color: isCatActive ? '#ffffff' : 'var(--text-secondary)',
                  fontSize: '0.74rem',
                  fontWeight: isCatActive ? '800' : '600',
                  cursor: 'pointer',
                  whiteSpace: 'nowrap',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  transition: 'all var(--motion-fast) ease',
                  boxShadow: isCatActive ? `0 0 12px ${cat.color}40` : 'none'
                }}
              >
                <Icon size={13} color={isCatActive ? cat.color : 'var(--text-muted)'} />
                <span>{cat.shortLabel}</span>
                {count > 0 && (
                  <span 
                    style={{ 
                      fontSize: '0.65rem', 
                      padding: '1px 5px', 
                      borderRadius: '10px', 
                      background: isCatActive ? cat.color : 'rgba(255, 255, 255, 0.1)', 
                      color: isCatActive ? '#ffffff' : 'var(--text-muted)',
                      fontWeight: '800'
                    }}
                  >
                    {count}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Global Expand / Shrink Accordion Controls */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '4px', paddingBottom: '4px', borderBottom: '1px solid rgba(255, 255, 255, 0.06)' }}>
        <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.6px', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Folders size={13} color="#38bdf8" />
          Alphabetical Topics (A-Z)
        </span>
        <button
          onClick={areAllExpanded ? collapseAllTopics : expandAllTopics}
          style={{
            fontSize: '0.7rem',
            fontWeight: '700',
            color: '#38bdf8',
            background: 'rgba(56, 189, 248, 0.1)',
            border: '1px solid rgba(56, 189, 248, 0.25)',
            borderRadius: '6px',
            padding: '3px 8px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
            transition: 'all 0.2s ease'
          }}
        >
          {areAllExpanded ? 'Shrink All' : 'Expand All'}
          {areAllExpanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
        </button>
      </div>

      {/* Alphabetically Sorted Topics Accordion List */}
      <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '10px', paddingRight: '4px' }}>
        {groupedByTopic.length > 0 ? (
          groupedByTopic.map(({ categoryObj, problems: topicProblems }) => {
            const isTopicExpanded = expandedTopics[categoryObj.id] ?? (activeCategory === categoryObj.id || Boolean(searchQuery));
            const Icon = categoryObj.icon;

            return (
              <div 
                key={categoryObj.id} 
                style={{ 
                  display: 'flex', 
                  flexDirection: 'column', 
                  borderRadius: '12px',
                  border: isTopicExpanded ? `1px solid ${categoryObj.color}35` : '1px solid rgba(255, 255, 255, 0.06)',
                  background: isTopicExpanded ? 'rgba(0, 0, 0, 0.25)' : 'rgba(255, 255, 255, 0.015)',
                  overflow: 'hidden',
                  transition: 'all 0.2s ease'
                }}
              >
                {/* Topic Header Row (Expand / Shrink Toggle) */}
                <div
                  onClick={() => toggleTopicExpand(categoryObj.id)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '10px 12px',
                    cursor: 'pointer',
                    background: isTopicExpanded ? `${categoryObj.color}15` : 'rgba(255, 255, 255, 0.03)',
                    transition: 'all 0.2s ease',
                    userSelect: 'none'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '24px', height: '24px', borderRadius: '6px', background: `${categoryObj.color}20`, border: `1px solid ${categoryObj.color}40` }}>
                      <Icon size={14} color={categoryObj.color} />
                    </div>
                    <span style={{ fontSize: '0.85rem', fontWeight: '800', color: '#ffffff', letterSpacing: '0.2px' }}>
                      {categoryObj.label}
                    </span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span 
                      style={{ 
                        fontSize: '0.7rem', 
                        fontWeight: '800', 
                        color: categoryObj.color, 
                        background: `${categoryObj.color}20`,
                        border: `1px solid ${categoryObj.color}40`,
                        padding: '2px 8px', 
                        borderRadius: '12px' 
                      }}
                    >
                      {topicProblems.length}
                    </span>
                    {isTopicExpanded ? (
                      <ChevronDown size={15} color={categoryObj.color} />
                    ) : (
                      <ChevronRight size={15} color="var(--text-muted)" />
                    )}
                  </div>
                </div>

                {/* Sub-questions / Algorithms List under Topic */}
                {isTopicExpanded && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', padding: '8px', borderTop: '1px solid rgba(255, 255, 255, 0.05)' }}>
                    {topicProblems.map((prob) => {
                      const isProblemActive = activeProblemId === prob.id;

                      return (
                        <div
                          key={prob.id}
                          onClick={() => onSelectProblem(prob.id)}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            padding: '9px 10px 9px 12px',
                            borderRadius: '8px',
                            border: isProblemActive ? `1px solid ${categoryObj.color}` : '1px solid transparent',
                            background: isProblemActive ? `linear-gradient(135deg, ${categoryObj.color}25, rgba(56, 189, 248, 0.1))` : 'rgba(255, 255, 255, 0.02)',
                            cursor: 'pointer',
                            transition: 'all 0.2s ease',
                            position: 'relative',
                            overflow: 'hidden',
                            boxShadow: isProblemActive ? `0 0 14px ${categoryObj.color}35` : 'none',
                            transform: isProblemActive ? 'translateX(2px)' : 'none'
                          }}
                        >
                          {/* Active Glowing Left Accent Bar */}
                          {isProblemActive && (
                            <div 
                              style={{ 
                                position: 'absolute', 
                                left: 0, 
                                top: 0, 
                                bottom: 0, 
                                width: '4px', 
                                background: categoryObj.color
                              }} 
                            />
                          )}

                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', paddingLeft: isProblemActive ? '4px' : '0' }}>
                            <PlayCircle 
                              size={15} 
                              color={isProblemActive ? categoryObj.color : 'var(--text-muted)'} 
                              style={{ transition: 'all 0.2s ease', transform: isProblemActive ? 'scale(1.15)' : 'none' }}
                            />
                            <div>
                              <div style={{ fontSize: '0.8rem', fontWeight: isProblemActive ? '800' : '600', color: isProblemActive ? '#ffffff' : 'var(--text-primary)' }}>
                                {prob.title}
                              </div>
                              {prob.subcategory && (
                                <div style={{ fontSize: '0.66rem', color: 'var(--text-muted)', fontWeight: '600' }}>
                                  {prob.subcategory}
                                </div>
                              )}
                            </div>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span className={`badge ${getBadgeClass(prob.difficulty)}`}>
                              {prob.difficulty}
                            </span>
                            <ChevronRight size={13} color={isProblemActive ? categoryObj.color : 'var(--text-muted)'} />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })
        ) : (
          /* Empty Search / Filter State */
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '30px 10px', textAlign: 'center', gap: '12px' }}>
            <SearchX size={36} color="var(--text-muted)" />
            <div style={{ fontSize: '0.88rem', fontWeight: '700', color: 'var(--text-secondary)', textAlign: 'center' }}>
              No algorithms match "{searchQuery}"
            </div>
            <button
              onClick={() => { setSearchQuery(''); onSelectCategory(null); }}
              className="btn btn-secondary"
              style={{ fontSize: '0.78rem', padding: '6px 14px' }}
            >
              Reset Search & Filters
            </button>
          </div>
        )}
      </div>
    </aside>
  );
}
