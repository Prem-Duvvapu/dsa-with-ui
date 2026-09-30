import React, { useRef } from 'react';
import styles from './ProblemWorkspace.module.css';

export const VIEWS = [
  { id: 'playground', label: 'Playground' },
  { id: 'code', label: 'Code walkthrough' },
  { id: 'analysis', label: 'Analysis' }
];

export const panelId = 'workspace-view-panel';
export const tabId = (view) => `workspace-tab-${view}`;

/**
 * The three learning views as manual-activation tabs: arrows, Home and End move focus;
 * Enter, Space or a click selects. Moving focus alone never re-renders a view, and the keys
 * are marked handled so the player's own arrow shortcuts do not also step the trace.
 */
export default function ViewRail({ view, onSelect }) {
  const refs = useRef({});

  const onKeyDown = (event) => {
    const keys = ['ArrowLeft', 'ArrowRight', 'Home', 'End'];
    if (!keys.includes(event.key)) return;
    event.preventDefault();
    const current = VIEWS.findIndex((v) => refs.current[v.id] === document.activeElement);
    const from = current < 0 ? VIEWS.findIndex((v) => v.id === view) : current;
    const to = event.key === 'Home' ? 0
      : event.key === 'End' ? VIEWS.length - 1
        : (from + (event.key === 'ArrowRight' ? 1 : -1) + VIEWS.length) % VIEWS.length;
    refs.current[VIEWS[to].id]?.focus();
  };

  return (
    <nav className={styles.rail} aria-label="Learning views">
      <div className={styles.railInner} role="tablist" aria-label="Learning views" onKeyDown={onKeyDown}>
        {VIEWS.map((item) => (
          <button
            key={item.id}
            ref={(node) => { refs.current[item.id] = node; }}
            id={tabId(item.id)}
            type="button"
            role="tab"
            aria-selected={view === item.id}
            aria-controls={panelId}
            tabIndex={view === item.id ? 0 : -1}
            className={styles.tab}
            onClick={() => onSelect(item.id)}
          >
            {item.label}
          </button>
        ))}
      </div>
    </nav>
  );
}
