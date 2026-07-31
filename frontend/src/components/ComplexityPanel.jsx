import React from 'react';
import { Cpu, HardDrive, HelpCircle, CheckCircle2, Zap } from 'lucide-react';

export default function ComplexityPanel({ complexity }) {
  if (!complexity) return null;

  return (
    <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', paddingBottom: '10px', borderBottom: '1px solid var(--border-color)' }}>
        <Zap size={20} color="var(--accent-amber)" />
        <h3 style={{ fontSize: '1.05rem', fontWeight: '700' }}>
          Time & Space Complexity Analysis (How & Why)
        </h3>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
        {/* Time Complexity Card */}
        <div style={{
          background: 'rgba(99, 102, 241, 0.06)',
          borderRadius: '14px',
          border: '1px solid rgba(99, 102, 241, 0.25)',
          padding: '20px',
          display: 'flex',
          flexDirection: 'column',
          gap: '12px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--accent-indigo)' }}>
              <Cpu size={20} />
              <span style={{ fontWeight: '700', fontSize: '0.95rem' }}>Time Complexity</span>
            </div>
            <span style={{
              fontSize: '1.15rem',
              fontWeight: '800',
              fontFamily: 'var(--font-code)',
              color: '#818cf8',
              background: 'rgba(99, 102, 241, 0.2)',
              padding: '4px 12px',
              borderRadius: '8px',
              border: '1px solid rgba(99, 102, 241, 0.4)'
            }}>
              {complexity.timeComplexity}
            </span>
          </div>

          <div style={{ fontSize: '0.86rem', lineHeight: '1.5', color: 'var(--text-primary)' }}>
            <strong>Explanation: </strong> {complexity.timeExplanation}
          </div>

          <div style={{ background: 'rgba(0, 0, 0, 0.25)', borderRadius: '10px', padding: '12px', borderLeft: '3px solid var(--accent-indigo)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', fontWeight: '700', color: 'var(--accent-cyan)', marginBottom: '4px' }}>
              <HelpCircle size={14} /> WHY is it {complexity.timeComplexity}?
            </div>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: '1.45' }}>
              {complexity.timeWhy}
            </p>
          </div>
        </div>

        {/* Space Complexity Card */}
        <div style={{
          background: 'rgba(16, 185, 129, 0.06)',
          borderRadius: '14px',
          border: '1px solid rgba(16, 185, 129, 0.25)',
          padding: '20px',
          display: 'flex',
          flexDirection: 'column',
          gap: '12px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#34d399' }}>
              <HardDrive size={20} />
              <span style={{ fontWeight: '700', fontSize: '0.95rem' }}>Space Complexity</span>
            </div>
            <span style={{
              fontSize: '1.15rem',
              fontWeight: '800',
              fontFamily: 'var(--font-code)',
              color: '#34d399',
              background: 'rgba(16, 185, 129, 0.2)',
              padding: '4px 12px',
              borderRadius: '8px',
              border: '1px solid rgba(16, 185, 129, 0.4)'
            }}>
              {complexity.spaceComplexity}
            </span>
          </div>

          <div style={{ fontSize: '0.86rem', lineHeight: '1.5', color: 'var(--text-primary)' }}>
            <strong>Explanation: </strong> {complexity.spaceExplanation}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
            <div>🔹 <strong>Auxiliary Memory: </strong> {complexity.auxiliarySpace}</div>
            <div>🔹 <strong>Data Structure Memory: </strong> {complexity.dataStructureSpace}</div>
          </div>

          <div style={{ background: 'rgba(0, 0, 0, 0.25)', borderRadius: '10px', padding: '12px', borderLeft: '3px solid #34d399' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', fontWeight: '700', color: '#34d399', marginBottom: '4px' }}>
              <HelpCircle size={14} /> WHY is auxiliary space {complexity.spaceComplexity}?
            </div>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: '1.45' }}>
              {complexity.spaceWhy}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
