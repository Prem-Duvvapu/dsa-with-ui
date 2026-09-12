import React from 'react';
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
 */
export default function ProblemStatement({ problem }) {
  const description = problem?.description?.trim();
  const constraints = problem?.constraints ?? [];

  if (!description && constraints.length === 0) return null;

  return (
    <section className={styles.container} aria-label="Problem statement">
      {description && <p className={styles.description}>{description}</p>}

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
