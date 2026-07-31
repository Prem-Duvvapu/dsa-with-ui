import React from 'react';
import { Network, Sparkles, Layers, Cpu } from 'lucide-react';

export default function Header({ totalProblems }) {
  return (
    <header className="glass-panel" style={{ margin: '16px 24px 0 24px', padding: '14px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', border: '1px solid rgba(255, 255, 255, 0.1)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        <div style={{
          width: '44px',
          height: '44px',
          borderRadius: '12px',
          background: 'linear-gradient(135deg, #6366f1, #a855f7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 0 20px rgba(99, 102, 241, 0.5)'
        }}>
          <Network size={26} color="#ffffff" />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h1 style={{ fontSize: '1.45rem', fontWeight: '800', background: 'linear-gradient(90deg, #ffffff, #cbd5e1)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', letterSpacing: '-0.3px' }}>
              DSA Visualizer
            </h1>
            <span style={{ fontSize: '0.72rem', padding: '3px 10px', borderRadius: '12px', background: 'rgba(99, 102, 241, 0.25)', color: '#a5b4fc', border: '1px solid rgba(99, 102, 241, 0.4)', fontWeight: '700', letterSpacing: '0.5px', textTransform: 'uppercase' }}>
              PRO ENGINE
            </span>
          </div>
          <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Interactive Algorithm Execution, Memory Tracing & Mathematical Complexity Proofs
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {/* Module Counter Badge */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          background: 'rgba(255, 255, 255, 0.04)',
          padding: '8px 16px',
          borderRadius: '12px',
          border: '1px solid var(--border-color)'
        }}>
          <Cpu size={18} color="var(--accent-indigo)" />
          <div style={{ fontSize: '0.85rem' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Algorithm Library: </span>
            <strong style={{ color: '#10b981', fontWeight: '700' }}>{totalProblems || 40}+ Algorithms Active</strong>
          </div>
        </div>
      </div>
    </header>
  );
}
