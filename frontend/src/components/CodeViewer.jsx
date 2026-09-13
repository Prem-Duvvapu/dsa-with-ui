import styles from './CodeViewer.module.css';
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
    <div className={`glass-panel ${styles.panel}`}>
      <div className={styles.header}>
        <div className={styles.headerGroup}>
          <Code2 size={15} color="var(--bench-ink-secondary)" />
          <span className={styles.headerTitle}>Java interview solution</span>
        </div>
        <div className={styles.headerGroup}>
          {unreached.size > 0 && javaCode && (
            <span
              data-testid="unreached-note"
              title="These branches exist in the algorithm but this input never took them. Change the input to see them run."
              className={styles.unreachedBadge}
            >
              ◦ {unreached.size} branch{unreached.size === 1 ? '' : 'es'} not taken
            </span>
          )}
          {activeLine !== null && javaCode && (
            <span className={styles.anchorBadge}>
              Active line: {activeLine}
            </span>
          )}
        </div>
      </div>

      {/* Code Editor Body */}
      <div className={styles.source}>
        {!javaCode ? (
          <div
            role="status"
            className={styles.unavailable}
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
              data-active-line={isHighlighted ? 'true' : undefined}
              data-unreached={isUnreached ? 'true' : undefined}
              title={isUnreached
                ? 'This branch exists but the current input never took it. Change the input to see it run.'
                : undefined}
              className={[
                styles.line,
                isHighlighted ? styles.lineActive : '',
                isUnreached ? styles.lineUnreached : '',
                !isHighlighted && lineText.trim().startsWith('//') ? styles.lineComment : ''
              ].filter(Boolean).join(' ')}
            >
              {/* A glyph, not just dimming: a reader who cannot see the opacity difference
                  still gets the state. Same rule the canvas legend follows. */}
              <span aria-hidden="true" className={styles.gutterGlyph}>
                {isUnreached ? '◦' : ''}
              </span>
              <span className={`${styles.lineNumber}${isHighlighted ? ` ${styles.lineNumberActive}` : ''}`}>
                {lineNumber}
              </span>
              <span className={styles.lineText}>{lineText}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
