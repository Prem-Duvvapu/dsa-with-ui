import React, { useMemo } from 'react';
import { ChevronLeft, ChevronRight, Check } from 'lucide-react';
import styles from './SectionNav.module.css';

/**
 * Where this problem sits in its curriculum section, and the two steps either side of it.
 *
 * The backend already carries a complete curriculum: every one of 431 problems has a
 * `striverSheetSection` ("DP - Basic DP", "BST - Practice", ...), and `GET /api/problems`
 * serves them in the order each service authored them - which is already a progression,
 * not an alphabetical accident (`ProblemCatalog` merges providers into one
 * insertion-ordered map). None of that was ever read by the frontend, so finishing a trace
 * ended in silence: no sense of position, no next action, nothing to bring someone back.
 *
 * This walks that existing order rather than inventing a new one - `problems` is expected
 * to be the catalogue exactly as served, so "3 of 13" means the same three-of-thirteen the
 * backend already implies.
 */
export default function SectionNav({ problems, activeProblemId, progress = {}, onSelectProblem }) {
  const { section, index, ordered } = useMemo(() => {
    const active = problems.find((p) => p.id === activeProblemId);
    const activeSection = active?.striverSheetSection;
    if (!activeSection) {
      return { section: null, index: -1, ordered: [] };
    }
    const inSection = problems.filter((p) => p.striverSheetSection === activeSection);
    return {
      section: activeSection,
      index: inSection.findIndex((p) => p.id === activeProblemId),
      ordered: inSection
    };
  }, [problems, activeProblemId]);

  // No section on this problem, the catalogue has not loaded, or this is the only problem
  // in its section - in every case there is nowhere to navigate to, so there is nothing to
  // show. A "1 of 1" reads as broken rather than as a fact worth stating.
  if (!section || ordered.length <= 1) {
    return null;
  }

  const prev = index > 0 ? ordered[index - 1] : null;
  const next = index < ordered.length - 1 ? ordered[index + 1] : null;

  return (
    <nav className={styles.bar} aria-label="Position in curriculum section">
      {prev ? (
        <button
          type="button"
          className={styles.step}
          onClick={() => onSelectProblem(prev.id)}
          aria-label={`Previous: ${prev.title}`}
        >
          <ChevronLeft size={14} aria-hidden="true" />
          <span className={styles.stepLabel}>
            {prev.title}
            {progress[prev.id]?.watched && (
              <span className={styles.watchedGlyph}>
                <Check size={11} aria-hidden="true" />
                <span className="sr-only">watched</span>
              </span>
            )}
          </span>
        </button>
      ) : (
        // No accessible button here at all - a disabled ghost control that announces
        // "previous" when there is nothing previous reads worse than empty space.
        <span className={styles.stepPlaceholder} aria-hidden="true" />
      )}

      <span className={styles.position}>
        <span className={styles.sectionName}>{section}</span>
        <span className={styles.count}>{index + 1} of {ordered.length}</span>
      </span>

      {next ? (
        <button
          type="button"
          className={styles.step}
          onClick={() => onSelectProblem(next.id)}
          aria-label={`Next: ${next.title}`}
        >
          <span className={styles.stepLabel}>
            {next.title}
            {progress[next.id]?.watched && (
              <span className={styles.watchedGlyph}>
                <Check size={11} aria-hidden="true" />
                <span className="sr-only">watched</span>
              </span>
            )}
          </span>
          <ChevronRight size={14} aria-hidden="true" />
        </button>
      ) : (
        <span className={styles.stepPlaceholder} aria-hidden="true" />
      )}
    </nav>
  );
}
