import React from 'react';
import styles from './LearningLayout.module.css';

/**
 * The page-level footer every learning page ends with: the library and the problem page.
 * One per page, so there is exactly one contentinfo landmark; a page's own closing line
 * (the library's tagline) is passed in rather than rendered as a second footer.
 */
export default function SiteFooter({ children }) {
  return (
    <footer className={styles.siteFooter}>
      <div className={styles.siteFooterInner}>
        {children && <p className={styles.siteFooterNote}>{children}</p>}
        <p>developed by Prem Duvvapu</p>
      </div>
    </footer>
  );
}
