import React from 'react';
import { ChevronRight, Star, Check } from 'lucide-react';
import styles from './Breadcrumb.module.css';

/**
 * `watched` is set by App when the learner reaches the last step - sitting through a trace,
 * not merely opening one. `onToggleStar` is absent in the tests and in any caller that has
 * no progress to show, and the control simply is not rendered then.
 */
export default function Breadcrumb({ problem, watched, starred, onToggleStar }) {
  const category = problem?.category || 'Arrays & math';
  const title = problem?.title || 'Longest substring without repeating characters';
  const difficulty = problem?.difficulty || 'Easy';

  const getDiffClass = (diff) => {
    switch (diff?.toLowerCase()) {
      case 'easy':
        return styles.diffEasy;
      case 'medium':
        return styles.diffMedium;
      case 'hard':
        return styles.diffHard;
      default:
        return styles.diffEasy;
    }
  };

  return (
    <div className={styles.container}>
      <span className={styles.category}>
        {category}
      </span>
      
      <ChevronRight size={13} color="var(--text-muted)" />
      
      <span 
        className={styles.title}
        title={title}
      >
        {title}
      </span>

      {/* Bench: "a neutral outline plus a letter (E / M / H)". The full word stays for
          screen readers - the letter is a space decision, not an information one. */}
      <span
        className={`${styles.difficultyPill} ${getDiffClass(difficulty)}`}
        title={difficulty}
      >
        <span aria-hidden="true">{difficulty.charAt(0).toUpperCase()}</span>
        <span className="sr-only">{difficulty}</span>
      </span>

      {watched && (
        <span className={styles.watched} title="You have watched this one through to the end">
          <Check size={12} aria-hidden="true" />
          <span className="sr-only">Watched</span>
        </span>
      )}

      {onToggleStar && (
        <button
          type="button"
          className={`${styles.star} ${starred ? styles.starOn : ''}`}
          onClick={onToggleStar}
          aria-pressed={starred ? 'true' : 'false'}
          aria-label={starred ? 'Remove from starred' : 'Star this problem'}
          title={starred ? 'Remove from starred' : 'Star this problem to come back to it'}
        >
          <Star size={13} fill={starred ? 'currentColor' : 'none'} aria-hidden="true" />
        </button>
      )}
    </div>
  );
}
