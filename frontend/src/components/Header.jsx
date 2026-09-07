import React from 'react';
import { Layers, Menu, X, BookOpen } from 'lucide-react';
import styles from './Header.module.css';

export default function Header({ totalProblems, isSidebarOpen, onToggleSidebar }) {
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
          <Layers size={16} color="#ffffff" />
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
        <div className={styles.libraryBadge}>
          <BookOpen size={13} color="var(--text-muted)" />
          <span className={styles.libraryLabel}>Library: </span>
          <strong className={styles.libraryCount}>{totalProblems || 0} algorithms</strong>
        </div>
      </div>
    </header>
  );
}
