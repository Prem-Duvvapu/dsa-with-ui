import React from 'react';
import { Layers, ChevronRight, PlayCircle, FolderTree, Network, Cpu, GitBranch, BarChart3, Binary, Link2, Search, Brain, Hash } from 'lucide-react';

export default function Sidebar({ problems, activeProblemId, activeCategory, onSelectCategory, onSelectProblem }) {
  const categories = [
    { id: 'Graph BFS/DFS', label: 'Graph BFS & DFS', icon: Network },
    { id: 'Advanced Graphs', label: 'Advanced Graphs', icon: Cpu },
    { id: 'Binary Trees', label: 'Binary Trees', icon: FolderTree },
    { id: 'Binary Search Trees', label: 'Binary Search Trees', icon: GitBranch },
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

      {/* Section Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', paddingBottom: '8px', borderBottom: '1px solid var(--border-color)' }}>
        <Layers size={18} color="var(--accent-indigo)" />
        <h2 style={{ fontSize: '0.92rem', fontWeight: '700' }}>
          {activeCategory || 'All Problems'} ({filteredProblems.length})
        </h2>
      </div>

      {/* Problem List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {filteredProblems.map((p, idx) => {
          const isActive = p.id === activeProblemId;
          return (
            <button
              key={p.id}
              onClick={() => onSelectProblem(p.id)}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '12px 14px',
                borderRadius: '12px',
                border: isActive ? '1px solid var(--accent-indigo)' : '1px solid transparent',
                background: isActive ? 'rgba(99, 102, 241, 0.16)' : 'rgba(255, 255, 255, 0.03)',
                color: isActive ? '#ffffff' : 'var(--text-secondary)',
                cursor: 'pointer',
                textAlign: 'left',
                transition: 'all 0.2s ease',
                boxShadow: isActive ? 'var(--glow-indigo)' : 'none'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: '700', color: isActive ? 'var(--accent-indigo)' : 'var(--text-muted)' }}>
                  {idx + 1}.
                </span>
                <div>
                  <div style={{ fontSize: '0.86rem', fontWeight: isActive ? '700' : '500', lineHeight: '1.3' }}>
                    {p.title}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '4px' }}>
                    <span className={`badge ${getBadgeClass(p.difficulty)}`}>
                      {p.difficulty}
                    </span>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {p.dsType}
                    </span>
                  </div>
                </div>
              </div>
              {isActive ? <PlayCircle size={18} color="var(--accent-indigo)" /> : <ChevronRight size={16} style={{ opacity: 0.4 }} />}
            </button>
          );
        })}
      </div>
    </aside>
  );
}
