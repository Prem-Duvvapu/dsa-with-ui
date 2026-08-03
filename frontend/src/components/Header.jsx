import React from 'react';
import { Network, Cpu, Menu, X, User, HelpCircle, BookOpen } from 'lucide-react';

export default function Header({ totalProblems, isSidebarOpen, onToggleSidebar }) {
  return (
    <header className="glass-panel" style={{ margin: 'var(--space-xs) var(--space-md) 0 var(--space-md)', padding: '8px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', border: '1px solid var(--border-color)', flexShrink: 0, height: '48px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-md)' }}>
        {/* Mobile/Tablet Hamburger Toggle */}
        <button 
          className="btn btn-secondary" 
          onClick={onToggleSidebar}
          aria-label={isSidebarOpen ? "Close navigation menu" : "Open navigation menu"}
          style={{ padding: '4px 8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
        >
          {isSidebarOpen ? <X size={18} /> : <Menu size={18} />}
        </button>

        <div style={{
          width: '32px',
          height: '32px',
          borderRadius: 'var(--radius-md)',
          background: 'linear-gradient(135deg, var(--accent-indigo), var(--accent-purple))',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: 'var(--glow-indigo)'
        }}>
          <Network size={18} color="#ffffff" />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-xs)' }}>
            <h1 style={{ fontSize: '0.98rem', fontWeight: '800', background: 'linear-gradient(90deg, #ffffff, #cbd5e1)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', letterSpacing: '-0.3px', margin: 0 }}>
              DSA Visualizer
            </h1>
            <span style={{ fontSize: '0.62rem', padding: '2px 7px', borderRadius: 'var(--radius-full)', background: 'rgba(168, 85, 247, 0.25)', color: '#c084fc', border: '1px solid rgba(168, 85, 247, 0.4)', fontWeight: '800', letterSpacing: '0.6px', textTransform: 'uppercase' }}>
              PRO ENGINE
            </span>
          </div>
          <p style={{ fontSize: '0.68rem', color: 'var(--text-muted)', marginTop: '0px', margin: 0 }}>
            Interactive Algorithm Execution, Memory Tracing & Mathematical Proofs
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-sm)' }}>
        {/* Module Counter Badge */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          background: 'rgba(255, 255, 255, 0.04)',
          padding: '4px 10px',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-color)'
        }}>
          <BookOpen size={14} color="var(--accent-cyan)" />
          <div style={{ fontSize: '0.72rem' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Library: </span>
            <strong style={{ color: 'var(--state-sorted)', fontWeight: '700' }}>{totalProblems || 426}+ Algorithms</strong>
          </div>
        </div>

        {/* User Profile Button */}
        <button 
          title="User Profile" 
          aria-label="User profile options"
          style={{ width: '30px', height: '30px', borderRadius: '50%', background: 'rgba(255, 255, 255, 0.08)', border: '1px solid var(--border-color)', color: '#ffffff', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}
        >
          <User size={15} />
        </button>

        {/* Help Button */}
        <button 
          title="Help & Guidance" 
          aria-label="Help and guidance documentation"
          style={{ padding: '4px 10px', borderRadius: 'var(--radius-md)', background: 'rgba(255, 255, 255, 0.06)', border: '1px solid var(--border-color)', color: 'var(--text-secondary)', fontSize: '0.72rem', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '4px', cursor: 'pointer' }}
        >
          <HelpCircle size={14} />
          <span>Help</span>
        </button>
      </div>
    </header>
  );
}
