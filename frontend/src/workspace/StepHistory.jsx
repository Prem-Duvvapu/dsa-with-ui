import React, { useRef, useState } from 'react';
import styles from './StepHistory.module.css';

export const HISTORY_PAGE_SIZE = 50;

const pageOf = (index) => Math.floor(Math.max(0, index) / HISTORY_PAGE_SIZE);

/**
 * Every step of the run as text: its number and the backend's own narration, one page of
 * 50 at a time. It exists for every trace family - a graph, a tree or a DP table has no
 * capture strip, but its steps still have to be reachable by hand - and it never renders a
 * diagram per step, so a 5,000-step run costs 50 buttons, not 5,000 canvases.
 *
 * Choosing an entry seeks through the session's one seek (which pauses), so the diagram,
 * source, Analysis and URL follow. Paging only changes which entries are listed: it never
 * seeks and never runs anything. The workspace keys it by run, so a new run starts again on
 * the page holding its current step.
 */
export default function StepHistory({ steps, current, onSeek }) {
  const total = steps.length;
  const pages = Math.max(1, Math.ceil(total / HISTORY_PAGE_SIZE));
  const [page, setPage] = useState(() => pageOf(current));
  const listRef = useRef(null);

  if (total === 0) return <p className={styles.empty}>There are no steps to list: no run is loaded.</p>;

  const shown = Math.min(page, pages - 1);
  const start = shown * HISTORY_PAGE_SIZE;
  const end = Math.min(total, start + HISTORY_PAGE_SIZE);
  const currentPage = pageOf(current);
  const currentShown = current >= start && current < end;

  const jumpToCurrent = () => {
    setPage(currentPage);
    // After the page renders, move focus to the current entry so the keyboard is where the eye is.
    requestAnimationFrame(() => listRef.current?.querySelector('[aria-current="step"]')?.focus());
  };

  return (
    <div className={styles.history}>
      <div className={styles.bar}>
        <p className={styles.range} aria-live="polite">
          Steps {start + 1}–{end} of {total}
          {!currentShown && <span> · the current step, {current + 1}, is on page {currentPage + 1}</span>}
        </p>
        <div className={styles.pager}>
          <button type="button" className={styles.control} onClick={() => setPage(shown - 1)} disabled={shown === 0}>
            Previous page
          </button>
          <button type="button" className={styles.control} onClick={() => setPage(shown + 1)} disabled={shown >= pages - 1}>
            Next page
          </button>
          <button type="button" className={styles.control} onClick={jumpToCurrent}>
            Jump to current step
          </button>
        </div>
      </div>
      <ol ref={listRef} className={styles.list} start={start + 1} aria-label={`Steps ${start + 1} to ${end} of ${total}`}>
        {steps.slice(start, end).map((step, offset) => {
          const index = start + offset;
          const selected = index === current;
          return (
            <li key={index}>
              <button
                type="button"
                className={`${styles.entry}${selected ? ` ${styles.selected}` : ''}`}
                aria-current={selected ? 'step' : undefined}
                onClick={() => onSeek(index)}
              >
                <span className={styles.number}>Step {index + 1}</span>
                <span className={styles.text}>{step?.description || 'No narration for this step.'}</span>
              </button>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
