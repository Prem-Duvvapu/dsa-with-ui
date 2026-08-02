import React, { useState, useMemo } from 'react';
import { 
  Layers, ChevronRight, PlayCircle, FolderTree, Network, Cpu, 
  GitBranch, BarChart3, Binary, Link2, Search, Brain, Hash, 
  RefreshCcw, Zap, Type, Binary as BitIcon, Layers3, X, Filter, 
  Sparkles, CheckCircle2, ChevronDown, ChevronUp, SearchX 
} from 'lucide-react';

export default function Sidebar({ problems, activeProblemId, activeCategory, onSelectCategory, onSelectProblem }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [isCategoryExpanded, setIsCategoryExpanded] = useState(false);
  const [collapsedGroups, setCollapsedGroups] = useState({});

  const categories = [
    { id: 'Graph BFS/DFS', label: 'Graph BFS & DFS', shortLabel: 'Graphs', icon: Network, color: '#38bdf8' },
    { id: 'Advanced Graphs', label: 'Advanced Graphs', shortLabel: 'Adv Graphs', icon: Cpu, color: '#a855f7' },
    { id: 'Binary Trees', label: 'Binary Trees', shortLabel: 'Trees', icon: FolderTree, color: '#10b981' },
    { id: 'Binary Search Trees', label: 'Binary Search Trees', shortLabel: 'BST', icon: GitBranch, color: '#34d399' },
    { id: 'Recursion & Backtracking', label: 'Recursion & Backtracking', shortLabel: 'Backtracking', icon: RefreshCcw, color: '#f59e0b' },
    { id: 'Sorting Algorithms', label: 'Sorting Algorithms', shortLabel: 'Sorting', icon: BarChart3, color: '#6366f1' },
    { id: 'Arrays', label: 'Arrays & Math', shortLabel: 'Arrays', icon: Binary, color: '#ec4899' },
    { id: 'Linked List', label: 'Linked Lists', shortLabel: 'Linked List', icon: Link2, color: '#06b6d4' },
    { id: 'Binary Search', label: 'Binary Search', shortLabel: 'Binary Search', icon: Search, color: '#f97316' },
    { id: 'Dynamic Programming', label: 'Dynamic Programming', shortLabel: 'DP', icon: Brain, color: '#8b5cf6' },
    { id: 'Tries & Prefixes', label: 'Tries & Prefixes', shortLabel: 'Tries', icon: Hash, color: '#14b8a6' },
    { id: 'Greedy Algorithms', label: 'Greedy Algorithms', shortLabel: 'Greedy', icon: Zap, color: '#eab308' },
    { id: 'Strings', label: 'Strings', shortLabel: 'Strings', icon: Type, color: '#64748b' },
    { id: 'Bit Manipulation', label: 'Bit Manipulation', shortLabel: 'Bit Logic', icon: BitIcon, color: '#3b82f6' },
    { id: 'Heaps & PriorityQueue', label: 'Heaps & PriorityQueue', shortLabel: 'Heaps', icon: Layers3, color: '#f43f5e' }
  ];

  // Dynamic problem count per category
  const categoryCounts = useMemo(() => {
    const counts = {};
    problems.forEach(p => {
      if (p.category) {
        counts[p.category] = (counts[p.category] || 0) + 1;
      }
    });
    return counts;
  }, [problems]);

  // Filter problems by active category and search query
  const filteredProblems = useMemo(() => {
    return problems.filter((p) => {
      const matchesCategory = !activeCategory || p.category === activeCategory;
      const q = searchQuery.toLowerCase().trim();
      const matchesSearch = !q || 
        (p.title && p.title.toLowerCase().includes(q)) ||
        (p.category && p.category.toLowerCase().includes(q)) ||
        (p.subcategory && p.subcategory.toLowerCase().includes(q)) ||
        (p.difficulty && p.difficulty.toLowerCase().includes(q));
      return matchesCategory && matchesSearch;
    });
  }, [problems, activeCategory, searchQuery]);

  // Group filtered problems by subcategory or category
  const groupedProblems = useMemo(() => {
    const groups = {};
    filteredProblems.forEach((prob) => {
      const key = prob.subcategory || prob.category || 'General Algorithms';
      if (!groups[key]) groups[key] = [];
      groups[key].push(prob);
    });
    return groups;
  }, [filteredProblems]);

  const toggleGroupCollapse = (groupKey) => {
    setCollapsedGroups(prev => ({ ...prev, [groupKey]: !prev[groupKey] }));
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
          placeholder="Search algorithms (e.g. merge, dfs, bst)..."
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

      {/* Category Pills & Drawer Control */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span style={{ fontSize: '0.7rem', fontWeight: '800', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px', display: 'flex', alignItems: 'center', gap: '5px' }}>
            <Filter size={12} color="var(--accent-indigo)" />
            Category Filter ({categories.length})
          </span>
          <button
            onClick={() => setIsCategoryExpanded(!isCategoryExpanded)}
            style={{ fontSize: '0.72rem', color: 'var(--accent-indigo)', background: 'none', border: 'none', cursor: 'pointer', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '4px' }}
          >
            {isCategoryExpanded ? 'Compact' : 'Expand All'}
            {isCategoryExpanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
          </button>
        </div>

        {/* Categories Chip Selector */}
        <div 
          style={{ 
            display: 'flex', 
            flexWrap: isCategoryExpanded ? 'wrap' : 'nowrap', 
            overflowX: isCategoryExpanded ? 'visible' : 'auto', 
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

      {/* Active Filter Header */}
      {activeCategoryObj && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px', background: 'rgba(99, 102, 241, 0.12)', borderRadius: '8px', border: `1px solid ${activeCategoryObj.color}40` }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <activeCategoryObj.icon size={15} color={activeCategoryObj.color} />
            <span style={{ fontSize: '0.78rem', fontWeight: '800', color: '#ffffff' }}>
              {activeCategoryObj.label}
            </span>
          </div>
          <span style={{ fontSize: '0.72rem', color: activeCategoryObj.color, fontWeight: '800' }}>
            {filteredProblems.length} Problem{filteredProblems.length !== 1 ? 's' : ''}
          </span>
        </div>
      )}

      {/* Problems Accordion List */}
      <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '12px', paddingRight: '4px' }}>
        {Object.keys(groupedProblems).length > 0 ? (
          Object.entries(groupedProblems).map(([groupName, groupProblems]) => {
            const isGroupCollapsed = collapsedGroups[groupName];

            return (
              <div key={groupName} style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {/* Group Subcategory Header */}
                <div
                  onClick={() => toggleGroupCollapse(groupName)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '6px 8px',
                    borderRadius: '6px',
                    cursor: 'pointer',
                    background: 'rgba(255, 255, 255, 0.02)',
                    transition: 'background var(--motion-fast) ease'
                  }}
                >
                  <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--accent-indigo)', textTransform: 'uppercase', letterSpacing: '0.6px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    {isGroupCollapsed ? <ChevronRight size={13} /> : <ChevronDown size={13} />}
                    {groupName}
                  </span>
                  <span style={{ fontSize: '0.68rem', fontWeight: '700', color: 'var(--text-muted)', background: 'rgba(255, 255, 255, 0.06)', padding: '1px 6px', borderRadius: '10px' }}>
                    {groupProblems.length}
                  </span>
                </div>

                {/* Group Items */}
                {!isGroupCollapsed && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', paddingLeft: '6px' }}>
                    {groupProblems.map((prob) => {
                      const isProblemActive = activeProblemId === prob.id;

                      return (
                        <div
                          key={prob.id}
                          onClick={() => onSelectProblem(prob.id)}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            padding: '10px 12px',
                            borderRadius: '10px',
                            border: isProblemActive ? '1px solid var(--accent-indigo)' : '1px solid rgba(255, 255, 255, 0.05)',
                            background: isProblemActive ? 'linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(56, 189, 248, 0.1))' : 'rgba(255, 255, 255, 0.02)',
                            cursor: 'pointer',
                            transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                            position: 'relative',
                            overflow: 'hidden',
                            boxShadow: isProblemActive ? '0 0 16px rgba(99, 102, 241, 0.3)' : 'none',
                            transform: isProblemActive ? 'translateX(2px)' : 'none'
                          }}
                        >
                          {/* Active Glowing Left Accent Line */}
                          {isProblemActive && (
                            <div 
                              style={{ 
                                position: 'absolute', 
                                left: 0, 
                                top: 0, 
                                bottom: 0, 
                                width: '4px', 
                                background: 'linear-gradient(180deg, var(--accent-indigo), #38bdf8)' 
                              }} 
                            />
                          )}

                          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', paddingLeft: isProblemActive ? '4px' : '0' }}>
                            <PlayCircle 
                              size={16} 
                              color={isProblemActive ? '#38bdf8' : 'var(--text-muted)'} 
                              style={{ transition: 'all 0.2s ease', transform: isProblemActive ? 'scale(1.15)' : 'none' }}
                            />
                            <div>
                              <div style={{ fontSize: '0.83rem', fontWeight: isProblemActive ? '800' : '600', color: isProblemActive ? '#ffffff' : 'var(--text-primary)' }}>
                                {prob.title}
                              </div>
                              {prob.dsType && (
                                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontWeight: '600' }}>
                                  Data Structure: <span style={{ color: 'var(--text-secondary)' }}>{prob.dsType}</span>
                                </div>
                              )}
                            </div>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span className={`badge ${getBadgeClass(prob.difficulty)}`}>
                              {prob.difficulty}
                            </span>
                            <ChevronRight size={14} color={isProblemActive ? '#38bdf8' : 'var(--text-muted)'} />
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
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '30px 10px', textAlignment: 'center', gap: '12px' }}>
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
