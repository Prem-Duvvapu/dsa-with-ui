import React from 'react';
import { Layers, ChevronRight, PlayCircle, FolderTree, Network, Cpu, GitBranch, BarChart3, Binary, Link2, Search, Brain, Hash, RefreshCcw } from 'lucide-react';

export default function Sidebar({ problems, activeProblemId, activeCategory, onSelectCategory, onSelectProblem }) {
  const categories = [
    { id: 'Graph BFS/DFS', label: 'Graph BFS & DFS', icon: Network },
    { id: 'Advanced Graphs', label: 'Advanced Graphs', icon: Cpu },
    { id: 'Binary Trees', label: 'Binary Trees', icon: FolderTree },
    { id: 'Binary Search Trees', label: 'Binary Search Trees', icon: GitBranch },
    { id: 'Recursion & Backtracking', label: 'Recursion & Backtracking', icon: RefreshCcw },
    { id: 'Sorting Algorithms', label: 'Sorting', icon: BarChart3 },
    { id: 'Arrays', label: 'Arrays & Math', icon: Binary },
    { id: 'Linked List', label: 'Linked Lists', icon: Link2 },
    { id: 'Binary Search', label: 'Binary Search', icon: Search },
    { id: 'Dynamic Programming', label: 'Dynamic Programming', icon: Brain },
    { id: 'Tries', label: 'Tries & Prefixes', icon: Hash }
  ];

  const filteredProblems = problems.filter((p) => !activeCategory || p.category === activeCategory);

  const getBadgeClass = (difficulty) => {
    switch (difficulty?.toLowerCase()) {
      case 'easy': return 'badge-easy';
      case 'medium': return 'badge-medium';
      case 'hard': return 'badge-hard';
      default: return 'badge-easy';
    }
  };

  return (
    <aside className="glass-panel" style={{ width: '340px', minWidth: '340px', height: 'calc(100vh - 100px)', overflowY: 'auto', padding: '20px', display: 'flex', flexDirection: 'column', gap: '18px' }}>
      {/* Category Grid */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px' }}>
          Algorithm Categories
        </span>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px' }}>
          {categories.map((cat) => {
            const Icon = cat.icon;
            const isCatActive = activeCategory === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => onSelectCategory(cat.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '8px 10px',
                  borderRadius: '8px',
                  border: isCatActive ? '1px solid var(--accent-indigo)' : '1px solid var(--border-color)',
                  background: isCatActive ? 'rgba(99, 102, 241, 0.22)' : 'rgba(255, 255, 255, 0.025)',
                  color: isCatActive ? '#ffffff' : 'var(--text-secondary)',
                  fontSize: '0.75rem',
                  fontWeight: isCatActive ? '700' : '500',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  boxShadow: isCatActive ? '0 0 12px rgba(99, 102, 241, 0.3)' : 'none'
                }}
              >
                <Icon size={14} color={isCatActive ? '#a5b4fc' : 'var(--text-muted)'} />
                <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{cat.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Problems List */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px' }}>
            {activeCategory || 'All'} Problems ({filteredProblems.length})
          </span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {filteredProblems.map((prob) => {
            const isProblemActive = activeProblemId === prob.id;
            return (
              <div
                key={prob.id}
                onClick={() => onSelectProblem(prob.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justify: 'space-between',
                  padding: '10px 12px',
                  borderRadius: '10px',
                  border: isProblemActive ? '1px solid var(--accent-indigo)' : '1px solid rgba(255, 255, 255, 0.05)',
                  background: isProblemActive ? 'rgba(99, 102, 241, 0.15)' : 'rgba(255, 255, 255, 0.02)',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <PlayCircle size={15} color={isProblemActive ? 'var(--accent-purple)' : 'var(--text-muted)'} />
                  <div>
                    <div style={{ fontSize: '0.82rem', fontWeight: isProblemActive ? '700' : '500', color: isProblemActive ? '#ffffff' : 'var(--text-secondary)' }}>
                      {prob.title}
                    </div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                      {prob.subcategory}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span className={`badge ${getBadgeClass(prob.difficulty)}`}>
                    {prob.difficulty}
                  </span>
                  <ChevronRight size={14} color="var(--text-muted)" />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </aside>
  );
}
