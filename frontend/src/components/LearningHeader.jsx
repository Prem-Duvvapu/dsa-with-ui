import React from 'react';
import { Link } from 'react-router-dom';
import { Code2, Monitor, Moon, Sun } from 'lucide-react';
import useTheme from '../hooks/useTheme';
import styles from './LearningLayout.module.css';

export default function LearningHeader({ children }) {
  const { theme, cycleTheme } = useTheme();
  const Icon = theme === 'dark' ? Moon : theme === 'light' ? Sun : Monitor;
  return <header className={styles.header}>
    <Link to="/" className={styles.brand} aria-label="DSA Visualizer home"><span className={styles.brandMark}><Code2 size={22} /></span><span>DSA Visualizer<small>UNDERSTAND EVERY STEP</small></span></Link>
    <div className={styles.headerActions}>{children}<button type="button" className={styles.button} onClick={cycleTheme} aria-label={`Theme: ${theme}. Change theme`} data-tour="theme"><Icon size={16} /><span>{theme[0].toUpperCase() + theme.slice(1)}</span></button></div>
  </header>;
}
