import React from 'react';
import { Layers } from 'lucide-react';

/**
 * Hero canvas for a step's own stack (`queueOrStackState`, via StepEmitter.stack()) —
 * not `arrayState`. min-stack, next-greater-element-2, trapping-rainwater and every other
 * DsType.STACK tracer already emit this field every step; before this canvas existed,
 * `Stack` routed to ArrayCanvas, so the actual stack was computed but never drawn — only
 * the input array was, which is a different structure than the one the narration
 * describes ("push 4", "pop the top").
 *
 * Index 0 is the top, matching StepEmitter.stack()'s push-to-front convention (an
 * ArrayDeque used as a stack iterates head-first). Drawn growing downward from the top so
 * "push" reads as "a new box appears above the others", the way a hand-drawn stack does.
 */
export default function StackCanvas({ step, currentStep, title = 'Stack' }) {
  const activeStep = currentStep || step;
  const items = activeStep?.queueOrStackState || [];

  return (
    <div style={{ flex: 1, padding: '12px 16px', display: 'flex', flexDirection: 'column', width: '100%', height: '100%', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Layers size={16} color="var(--accent-violet)" />
          <span style={{ fontSize: '0.86rem', fontWeight: '800', letterSpacing: '0.3px', color: 'var(--text-primary)' }}>
            {title}
          </span>
          <span style={{ fontSize: '0.66rem', padding: '2px 7px', background: 'var(--accent-violet-tint)', color: 'var(--accent-violet)', borderRadius: 'var(--radius-full)', border: '1px solid var(--border-accent)', fontWeight: '700' }}>
            {items.length} item{items.length === 1 ? '' : 's'}
          </span>
        </div>
      </div>

      <div
        style={{
          flex: 1, width: '100%', display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: items.length ? 'flex-start' : 'center',
          gap: '8px', padding: '20px', overflowY: 'auto',
          background: 'radial-gradient(ellipse at center, rgba(15, 23, 42, 0.6), rgba(9, 13, 22, 0.9))',
          borderRadius: 'var(--radius-md)', border: '1px solid var(--border-default)',
          borderBottom: '2px solid var(--border-strong)'
        }}
      >
        {items.length === 0 ? (
          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontFamily: 'var(--font-code)' }}>
            empty
          </span>
        ) : (
          items.map((value, idx) => (
            <div
              key={`${idx}-${value}`}
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                gap: '12px', width: '160px', padding: '8px 14px',
                borderRadius: 'var(--radius-sm)', fontFamily: 'var(--font-code)',
                fontSize: '0.85rem', fontWeight: 600,
                background: idx === 0 ? 'linear-gradient(180deg, var(--state-current), #d97706)' : 'linear-gradient(180deg, #334155, #1e293b)',
                border: idx === 0 ? '1.5px solid var(--state-current)' : '1px solid var(--border-default)',
                boxShadow: idx === 0 ? 'var(--state-current-glow)' : 'none',
                color: 'var(--text-primary)'
              }}
            >
              {idx === 0 && (
                <span style={{ fontSize: '0.62rem', letterSpacing: '0.4px', textTransform: 'uppercase' }}>
                  top
                </span>
              )}
              <span style={{ marginLeft: idx === 0 ? 0 : 'auto' }}>{value}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
