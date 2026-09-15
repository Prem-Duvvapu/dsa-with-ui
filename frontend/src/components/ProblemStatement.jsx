import React from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import styles from './ProblemStatement.module.css';

/**
 * What the problem actually asks, shown above the animation.
 *
 * The app explained its own complexity analysis long before it stated the question: a
 * learner could watch kadane-algo step through "Track the best sum seen (-2)..." without
 * ever being told the task was to find the largest contiguous sum. The description has been
 * on every problem all along and was simply never rendered.
 *
 * `constraints` are the ORIGINAL problem's, from ProblemConstraints on the backend - not the
 * input panel's own limits, which cap an array at 40 elements to protect the step budget.
 * They are labelled "Problem constraints" for exactly that reason. Both are true; they
 * differ by orders of magnitude. Absent constraints render nothing at all rather than a
 * plausible-looking invention.
 *
 * Collapsible for the same reason the input editor and complexity card are: once you know
 * what the problem asks, restating it every step is vertical space the canvas wants. The
 * collapsed state keeps a one-line affordance rather than vanishing, so the statement is
 * always one click away and never silently absent.
 */
export default function ProblemStatement({ problem, open = true, onToggle }) {
  const description = problem?.description?.trim();
  const constraints = problem?.constraints ?? [];

  if (!description && constraints.length === 0) return null;

  if (!open) {
    return (
      <div className={styles.collapsedRow}>
        <button
          type="button"
          onClick={onToggle}
          aria-expanded={false}
          className={styles.toggleBtn}
          title="Show the problem statement"
        >
          <ChevronRight size={12} /> Problem
        </button>
      </div>
    );
  }

  return (
    <section className={styles.container} aria-label="Problem statement">
      <div className={styles.headerRow}>
        {description && <p className={styles.description}>{description}</p>}
        {onToggle && (
          <button
            type="button"
            onClick={onToggle}
            aria-expanded
            className={styles.toggleBtn}
            title="Hide the problem statement"
          >
            <ChevronDown size={12} /> Hide
          </button>
        )}
      </div>

      {constraints.length > 0 && (
        <div className={styles.constraints}>
          <span className={styles.constraintsLabel}>Problem constraints</span>
          <ul className={styles.constraintsList}>
            {constraints.map((c) => (
              <li key={c} className={styles.constraint}>{c}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
