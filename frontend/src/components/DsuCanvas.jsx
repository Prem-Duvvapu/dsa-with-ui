import styles from './DsuCanvas.module.css';
import React from 'react';

/**
 * Disjoint Set Union (DSU) visualizer.
 *
 * Previously part of GraphCanvas.jsx (lines 84–159), which held five visualizers
 * behind ad-hoc predicates. The DSU renderer is structurally different from all
 * other canvases — parent[] and rank[] tables, component cards — so it belongs
 * in its own file.
 *
 * Header and legend are removed: CanvasShell owns those now.
 */
export default function DsuCanvas({ problem, currentStep, step }) {
  const activeStep = currentStep || step;
  const variables = activeStep?.variables || {};

  // DSU state arrives as strings under these exact keys (DsTypePayloadContractTest pins
  // them). It used to default to a hardcoded 7-element DSU when a key was missing, which
  // drew a plausible, entirely fabricated structure instead of failing - the no-fallback
  // rule leaking into the render path. Say so instead.
  const parentStr = variables['parent[]'];
  const rankStr = variables['rank[]'];
  if (!parentStr || !rankStr) {
    return (
      <div
        data-testid="dsu-state-unavailable"
        className={styles.unavailable}
      >
        DSU state unavailable for this step — the trace did not emit
        <code className={styles.code}>parent[]</code>
        and
        <code className={styles.code}>rank[]</code>.
      </div>
    );
  }

  const dsuSetsStr = variables['Disjoint Sets'] || '';
  const dsuOpStr = variables['Operation'] || '';

  const parentArr = parentStr.replace(/[\[\]]/g, '').split(',').map(s => s.trim());
  const rankArr = rankStr.replace(/[\[\]]/g, '').split(',').map(s => s.trim());

  // How many elements (skip index 0 when it's just padding)
  const count = Math.max(parentArr.length - 1, 7);
  const indices = Array.from({ length: count }, (_, i) => i + 1);

  return (
    <div className={styles.wrap}>
      <div className={styles.stack} style={{ '--dsu-columns': count }}>
        {/* Operation banner */}
        <div className={styles.opBanner}>
          <span className={styles.opText}>
            {dsuOpStr}
          </span>
        </div>

        {/* Connected components */}
        <div className={styles.components}>
          <div className={styles.componentCard}>
            <span className={styles.componentLabel}>
              Connected Components
            </span>
            <span className={styles.componentValue}>
              {dsuSetsStr}
            </span>
          </div>
        </div>

        {/* Parent & Rank tables */}
        <div className={styles.tables}>
          {/* Element index header */}
          <div className={styles.row}>
            <span className={`${styles.rowLabel} ${styles.rowLabelIndex}`}>Element i</span>
            {indices.map(idx => (
              <div key={idx} className={styles.indexCell}>
                {idx}
              </div>
            ))}
          </div>

          {/* parent[i] row */}
          <div className={styles.row}>
            <span className={styles.rowLabel}>parent[i]</span>
            {indices.map(idx => {
              const val = parentArr[idx] || idx;
              const isRoot = String(val) === String(idx);
              return (
                <div
                  key={idx}
                  className={`${styles.parentCell}${isRoot ? ` ${styles.parentCellRoot}` : ''}`}
                >
                  {val}
                </div>
              );
            })}
          </div>

          {/* rank[i] row */}
          <div className={styles.row}>
            <span className={styles.rowLabel}>rank[i]</span>
            {indices.map(idx => {
              const rVal = rankArr[idx] || 0;
              return (
                <div key={idx} className={styles.rankCell}>
                  {rVal}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
