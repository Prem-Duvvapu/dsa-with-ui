import React from 'react';
import { ChevronRight } from 'lucide-react';
import styles from './Breadcrumb.module.css';

export default function Breadcrumb({ problem }) {
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

      <span className={`${styles.difficultyPill} ${getDiffClass(difficulty)}`}>
        {difficulty}
      </span>
    </div>
  );
}
