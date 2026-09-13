import styles from './GridCanvas.module.css';
import React from 'react';
import { Crown } from 'lucide-react';
import { lastPayload } from '../trace/lastPayload';

/**
 * 2D grid / matrix visualizer.
 *
 * Previously part of GraphCanvas.jsx (lines 160–265), which held five visualizers
 * behind ad-hoc predicates. The grid, sudoku, and chessboard renderers all share
 * the same row×col structure, so they belong in one component keyed on the grid's
 * shape and the problem's identity.
 *
 * `variant="companion"` (via the exported {@link GridCompanion}) renders the same
 * `gridState` at a fraction of the size, inside a `.companion-pane`, for a problem
 * whose hero hasn't got a matrix as its main structure but still carries one on the
 * wire — `maximum-rectangles-binary-matrix` emits `.grid(matrix)` on every step
 * alongside its `Stack` hero (the row's histogram), and before this the matrix
 * itself was invisible: `Stack`/`Queue` heroes had no companion that read
 * `gridState` at all. See `canvas/companions.js`'s `runHasGrid` check.
 */
export default function GridCanvas({ problem, currentStep, step, variant = 'hero', title = 'Grid', steps, currentStepIndex }) {
  const activeStep = currentStep || step;
  // Absence is not "show the default". Tracers restate a structure only on the steps
  // that change it, so falling through to the catalogue default drew the CATALOGUE's
  // data over the caller's own input. Measured app-wide: 1506 steps across 142 of 232
  // problems. The default is honest only before any step has emitted anything.
  const gridState = lastPayload(steps, currentStepIndex, 'gridState', activeStep)
    || problem?.defaultGrid;
  const isCompanion = variant === 'companion';

  if (!gridState || !gridState.length) {
    if (isCompanion) {
      return (
        <div className="companion-pane" aria-label={title}>
          <div className="companion-title">{title}</div>
          <div className="companion-empty">empty</div>
        </div>
      );
    }
    return (
      <div className={styles.empty}>
        No grid data available
      </div>
    );
  }

  const isSudoku = gridState.length === 9 && gridState[0]?.length === 9;
  const isChessboard = gridState.length === 4 && gridState[0]?.length === 4
    && (problem?.id === 'n-queens' || problem?.title?.toLowerCase().includes('queen'));
  const cellSize = isCompanion ? 22 : null;

  const grid = (
    <div
      className={[
        styles.grid,
        isSudoku ? styles.gridSudoku : '',
        isCompanion ? styles.gridCompanion : ''
      ].filter(Boolean).join(' ')}
      style={{
        '--grid-columns': gridState[0].length,
        '--grid-cell': cellSize ? `${cellSize}px` : isSudoku ? '24px' : isChessboard ? '44px' : '36px'
      }}
    >
      {gridState.map((row, rIdx) =>
        row.map((val, cIdx) => {
          if (isChessboard) return renderQueenCell(rIdx, cIdx, val);
          if (isSudoku) return renderSudokuCell(rIdx, cIdx, val);
          return renderMatrixCell(rIdx, cIdx, val, cellSize);
        })
      )}
    </div>
  );

  if (isCompanion) {
    return (
      <div className="companion-pane" aria-label={title}>
        <div className="companion-title">{title}</div>
        <div className={styles.scroller}>{grid}</div>
      </div>
    );
  }

  return (
    <div className={styles.wrap}>
      {grid}
    </div>
  );
}

/**
 * The `Stack`/`Queue`-companion mapping — a distinct export so `canvas/registry.js`
 * (the plain dsType→hero table) is untouched and `canvas/companions.js` opts a run
 * into it purely from whether `gridState` is populated, the same way it already
 * does for `QueueCanvas`.
 */
export function GridCompanion(props) {
  return <GridCanvas {...props} variant="companion" title={props.title || 'Grid'} />;
}

function renderQueenCell(rIdx, cIdx, val) {
  const isLightSquare = (rIdx + cIdx) % 2 === 0;
  return (
    <div
      key={`queen-${rIdx}-${cIdx}`}
      className={[
        styles.queenCell,
        val === 1 ? styles.queenCellPlaced : isLightSquare ? styles.queenCellLight : ''
      ].filter(Boolean).join(' ')}
    >
      {val === 1 ? (
        <Crown size={24} color="var(--probe)" className={styles.queenIcon} />
      ) : (
        <span className={styles.coord}>({rIdx},{cIdx})</span>
      )}
    </div>
  );
}

function renderSudokuCell(rIdx, cIdx, val) {
  const boxRight = (cIdx + 1) % 3 === 0 && cIdx !== 8;
  const boxBottom = (rIdx + 1) % 3 === 0 && rIdx !== 8;

  return (
    <div
      key={`sudoku-${rIdx}-${cIdx}`}
      className={[
        styles.sudokuCell,
        val !== 0 ? styles.sudokuCellFilled : '',
        boxRight ? styles.sudokuBoxRight : '',
        boxBottom ? styles.sudokuBoxBottom : ''
      ].filter(Boolean).join(' ')}
    >
      {val !== 0 ? val : '.'}
    </div>
  );
}

function renderMatrixCell(rIdx, cIdx, val, cellSize) {
  const compact = Boolean(cellSize);
  return (
    <div
      key={`grid-${rIdx}-${cIdx}`}
      className={[
        styles.matrixCell,
        compact ? styles.matrixCellCompact : '',
        val === 2 ? styles.matrixCellDone : val === 1 || val === 99 ? styles.matrixCellActive : '',
        val !== 0 ? styles.matrixCellValue : ''
      ].filter(Boolean).join(' ')}
      style={compact ? { '--grid-cell': `${cellSize}px` } : undefined}
    >
      <span>{val}</span>
      {!compact && <span className={styles.coordSmall}>({rIdx},{cIdx})</span>}
    </div>
  );
}
