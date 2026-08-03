import React from 'react';
import { Network, Cpu, Menu, X } from 'lucide-react';

export default function Header({ totalProblems, isSidebarOpen, onToggleSidebar }) {
  return (
    <header className="glass-panel" style={{ margin: 'var(--space-sm) var(--space-md) 0 var(--space-md)', padding: 'var(--space-sm) var(--space-lg)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', border: '1px solid var(--border-color)', flexShrink: 0 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-md)' }}>
        {/* Mobile/Tablet Hamburger Toggle */}
        <button 
          className="btn btn-secondary" 
          onClick={onToggleSidebar}
          aria-label={isSidebarOpen ? "Close navigation menu" : "Open navigation menu"}
          style={{ padding: 'var(--space-xs) var(--space-sm)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
        >
          {isSidebarOpen ? <X size={20} /> : <Menu size={20} />}
        </button>

        <div style={{
          width: '38px',
          height: '38px',
          borderRadius: 'var(--radius-md)',
          background: 'linear-gradient(135deg, var(--accent-indigo), var(--accent-purple))',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: 'var(--glow-indigo)'
        }}>
          <Network size={22} color="#ffffff" />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-xs)' }}>
            <h1 style={{ fontSize: 'var(--text-lg)', fontWeight: '800', background: 'linear-gradient(90deg, #ffffff, #cbd5e1)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', letterSpacing: '-0.3px' }}>
              DSA Visualizer
            </h1>
            <span style={{ fontSize: 'var(--text-2xs)', padding: '2px 8px', borderRadius: 'var(--radius-full)', background: 'rgba(99, 102, 241, 0.25)', color: '#a5b4fc', border: '1px solid rgba(99, 102, 241, 0.4)', fontWeight: '700', letterSpacing: '0.5px', textTransform: 'uppercase' }}>
              PRO ENGINE
            </span>
          </div>
          <p style={{ fontSize: 'var(--text-xs)', color: 'var(--text-secondary)', marginTop: '1px' }}>
            Interactive Algorithm Execution, Memory Tracing & Mathematical Proofs
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-md)' }}>
        {/* Module Counter Badge */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 'var(--space-xs)',
          background: 'rgba(255, 255, 255, 0.04)',
          padding: 'var(--space-xs) var(--space-md)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-color)'
        }}>
          <Cpu size={16} color="var(--accent-indigo)" />
          <div style={{ fontSize: 'var(--text-xs)' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Library: </span>
            <strong style={{ color: 'var(--state-sorted)', fontWeight: '700' }}>{totalProblems || 406}+ Algorithms</strong>
          </div>
        </div>
      </div>
    </header>
  );
}
