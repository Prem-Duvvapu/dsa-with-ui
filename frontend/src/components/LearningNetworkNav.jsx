import React from 'react';
import styles from './LearningNetworkNav.module.css';

const subjects = [
  { id: 'dsa', name: 'DSA', href: 'https://dsa-with-ui.vercel.app/' },
  { id: 'lld', name: 'LLD', href: 'https://lld-with-ui.vercel.app/' },
  { id: 'hld', name: 'HLD', href: 'https://hld-with-ui.vercel.app/' },
  { id: 'cs', name: 'CS fundamentals', href: 'https://cs-fundamentals-with-ui.vercel.app/' },
];

export default function LearningNetworkNav() {
  return (
    <nav className={styles['learning-network']} aria-label="Learning network">
      <a className={styles['learning-network-home']} href="https://learning-hub-with-ui.vercel.app/">
        <span aria-hidden="true">↖</span> Learning Home
      </a>
      <div className={styles['learning-network-subjects']}>
        {subjects.map(subject => (
          <a key={subject.id} href={subject.href} aria-current={subject.id === 'dsa' ? 'location' : undefined}>
            {subject.name}
          </a>
        ))}
      </div>
    </nav>
  );
}
