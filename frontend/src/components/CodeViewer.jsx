import React from 'react';
import { Code2 } from 'lucide-react';

/**
 * Lines the algorithm CAN reach but this particular run did not.
 *
 * Every `// @a` anchor marks a branch the tracer can highlight. On a given input, some of
 * them never fire - a search that always succeeds never runs its "not found" line, a cycle
 * check on an acyclic graph never runs its "cycle detected" line. 93 of the catalogue's
 * problems have at least one such line on their default input.
 *
 * Leaving them unmarked reads as if the animation skipped something. Marking them says the
 * true thing: this branch exists, and your input did not take it. That turns the gap into
 * the lesson - an algorithm's shape is its branches, and which ones ran is a property of
 * the data, not of the visualiser.
 */
function unreachedAnchorLines(anchors, steps) {
  if (!anchors || !Array.isArray(steps) || steps.length === 0) return new Set();
  const visited = new Set(steps.map((s) => s.activeLine));
  return new Set(Object.values(anchors).filter((line) => !visited.has(line)));
}

export default function CodeViewer({ problem, currentStep, anchors, steps }) {
  const javaCode = typeof problem?.javaCode === 'string' && problem.javaCode.trim()
    ? problem.javaCode
    : null;
  const activeLine = Number.isInteger(currentStep?.activeLine) && currentStep.activeLine > 0
    ? currentStep.activeLine
    : null;
  const lines = javaCode?.split('\n') || [];
  const unreached = unreachedAnchorLines(anchors, steps);

  return (
    <div className="glass-panel" style={{ width: '100%', height: '100%', padding: '12px', display: 'flex', flexDirection: 'column', gap: '8px', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--border-default)', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Code2 size={15} color="var(--accent-violet)" />
          <span style={{ fontSize: '0.82rem', fontWeight: '700', color: 'var(--text-primary)' }}>Java interview solution</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          {unreached.size > 0 && javaCode && (
            <span
              data-testid="unreached-note"
              title="These branches exist in the algorithm but this input never took them. Change the input to see them run."
              style={{ fontSize: '0.62rem', padding: '1px 6px', borderRadius: 'var(--radius-sm)', color: 'var(--text-muted)', border: '1px solid var(--border-default)', fontWeight: '600', fontFamily: 'var(--font-code)' }}
            >
              ◦ {unreached.size} branch{unreached.size === 1 ? '' : 'es'} not taken
            </span>
          )}
          {activeLine !== null && javaCode && (
            <span style={{ fontSize: '0.66rem', padding: '1px 6px', borderRadius: 'var(--radius-sm)', background: 'var(--accent-violet-tint)', color: 'var(--accent-violet)', border: '1px solid var(--border-accent)', fontWeight: '700', fontFamily: 'var(--font-code)' }}>
              Active line: {activeLine}
            </span>
          )}
        </div>
      </div>

      {/* Code Editor Body */}
      <div style={{
        flex: 1,
        background: 'var(--bg-page)',
        borderRadius: 'var(--radius-sm)',
        border: '1px solid var(--border-default)',
        overflowY: 'auto',
        fontFamily: 'var(--font-code)',
        fontSize: '0.78rem',
        lineHeight: '1.5',
        padding: '6px 0'
      }}>
        {!javaCode ? (
          <div
            role="status"
            style={{ height: '100%', minHeight: '96px', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px', color: 'var(--text-muted)', textAlign: 'center', fontStyle: 'italic' }}
          >
            Code unavailable for this problem.
          </div>
        ) : lines.map((lineText, idx) => {
          const lineNumber = idx + 1;
          const isHighlighted = lineNumber === activeLine;
          // An anchored line the run never reached. Never both: the active line was, by
          // definition, reached.
          const isUnreached = !isHighlighted && unreached.has(lineNumber);

          return (
            <div
              key={lineNumber}
              data-unreached={isUnreached ? 'true' : undefined}
              title={isUnreached
                ? 'This branch exists but the current input never took it. Change the input to see it run.'
                : undefined}
              style={{
                display: 'flex',
                alignItems: 'center',
                padding: '1px 12px',
                background: isHighlighted ? 'var(--accent-violet-tint)' : 'transparent',
                borderLeft: isHighlighted
                  ? '3px solid var(--accent-violet)'
                  : isUnreached
                    ? '3px dotted var(--border-strong)'
                    : '3px solid transparent',
                color: isHighlighted ? 'var(--text-primary)' : lineText.trim().startsWith('//') ? 'var(--text-muted)' : 'var(--text-secondary)',
                // Dimmed, not hidden: the branch is part of the algorithm and belongs on
                // screen. A dotted rule and a gutter glyph carry the meaning too, because
                // state here never rides on colour alone.
                opacity: isUnreached ? 0.55 : 1,
                fontWeight: isHighlighted ? '600' : '400',
                transition: 'all 0.15s ease'
              }}
            >
              {/* A glyph, not just dimming: a reader who cannot see the opacity difference
                  still gets the state. Same rule the canvas legend follows. */}
              <span aria-hidden="true" style={{ width: '10px', minWidth: '10px', color: 'var(--text-muted)', fontSize: '0.6rem', userSelect: 'none' }}>
                {isUnreached ? '◦' : ''}
              </span>
              <span style={{ width: '28px', minWidth: '28px', color: isHighlighted ? 'var(--accent-violet)' : 'var(--text-muted)', fontSize: '0.7rem', userSelect: 'none' }}>
                {lineNumber}
              </span>
              <span style={{ whiteSpace: 'pre' }}>{lineText}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
