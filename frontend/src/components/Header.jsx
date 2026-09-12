import React from 'react';
import { Layers, Menu, X, BookOpen, Sun, Moon, Monitor } from 'lucide-react';
import styles from './Header.module.css';

/** Three states, matching index.css: "system" leaves the media query in charge. */
const THEME_LABEL = {
  system: 'Theme: following your system. Click for light.',
  light: 'Theme: light. Click for dark.',
  dark: 'Theme: dark. Click to follow your system.'
};
const THEME_TEXT = { system: 'System', light: 'Light', dark: 'Dark' };

export default function Header({ totalProblems, isSidebarOpen, onToggleSidebar, theme = 'system', onCycleTheme }) {
  return (
    <header className={`glass-panel ${styles.header}`}>
      <div className={styles.leftSection}>
        {/* Mobile/Tablet Hamburger Toggle */}
        <button 
          className={`btn btn-outline ${styles.hamburgerBtn}`}
          onClick={onToggleSidebar}
          aria-label={isSidebarOpen ? "Close navigation menu" : "Open navigation menu"}
        >
          {isSidebarOpen ? <X size={16} /> : <Menu size={16} />}
        </button>

        <div className={styles.logoIcon}>
          <Layers size={16} color="var(--text-on-accent)" />
        </div>
        <div>
          <div className={styles.titleRow}>
            <h1 className={styles.title}>
              DSA Visualizer
            </h1>
            <span className={styles.proBadge}>
              PRO ENGINE
            </span>
          </div>
          <p className={styles.subtitle}>
            Interactive algorithm execution, memory tracing and mathematical proofs
          </p>
        </div>
      </div>

      <div className={styles.rightSection}>
        <button
          type="button"
          onClick={onCycleTheme}
          className={styles.themeBtn}
          title={THEME_LABEL[theme]}
          aria-label={THEME_LABEL[theme]}
        >
          {theme === 'light' ? <Sun size={13} /> : theme === 'dark' ? <Moon size={13} /> : <Monitor size={13} />}
          <span className={styles.themeLabel}>{THEME_TEXT[theme]}</span>
        </button>

        <div className={styles.libraryBadge}>
          <BookOpen size={13} color="var(--text-muted)" />
          <span className={styles.libraryLabel}>Library: </span>
          <strong className={styles.libraryCount}>{totalProblems || 0} algorithms</strong>
        </div>
      </div>
    </header>
  );
}
