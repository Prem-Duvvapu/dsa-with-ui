import React from 'react';
import { Layers, Menu, X, BookOpen } from 'lucide-react';

export default function Header({ totalProblems, isSidebarOpen, onToggleSidebar }) {
  return (
    <header className="glass-panel" style={{ margin: 'var(--space-xs) var(--space-md) 0 var(--space-md)', padding: '8px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', border: '1px solid var(--border-default)', flexShrink: 0, height: '46px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        {/* Mobile/Tablet Hamburger Toggle */}
        <button 
          className="btn btn-outline" 
          onClick={onToggleSidebar}
          aria-label={isSidebarOpen ? "Close navigation menu" : "Open navigation menu"}
          style={{ padding: '4px 8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
        >
          {isSidebarOpen ? <X size={16} /> : <Menu size={16} />}
        </button>

        <div style={{
          width: '28px',
          height: '28px',
          borderRadius: 'var(--radius-sm)',
          background: 'var(--accent-violet)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: 'var(--accent-violet-glow)'
        }}>
          <Layers size={16} color="#ffffff" />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <h1 style={{ fontSize: '0.94rem', fontWeight: '800', color: 'var(--text-primary)', letterSpacing: '-0.3px', margin: 0 }}>
              DSA Visualizer
            </h1>
            <span style={{ fontSize: '0.6rem', padding: '1px 6px', borderRadius: 'var(--radius-full)', background: 'var(--accent-violet-tint)', color: 'var(--accent-violet)', border: '1px solid var(--border-accent)', fontWeight: '700', letterSpacing: '0.5px', textTransform: 'uppercase' }}>
              PRO ENGINE
            </span>
          </div>
          <p style={{ fontSize: '0.67rem', color: 'var(--text-muted)', marginTop: '0px', margin: 0 }}>
            Interactive algorithm execution, memory tracing and mathematical proofs
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          background: 'rgba(255, 255, 255, 0.03)',
          padding: '4px 10px',
          borderRadius: 'var(--radius-sm)',
          border: '1px solid var(--border-default)',
          fontSize: '0.72rem'
        }}>
          <BookOpen size={13} color="var(--text-muted)" />
          <span style={{ color: 'var(--text-muted)' }}>Library: </span>
          <strong style={{ color: 'var(--text-primary)', fontWeight: '700' }}>{totalProblems || 426} algorithms</strong>
        </div>
      </div>
    </header>
  );
}
