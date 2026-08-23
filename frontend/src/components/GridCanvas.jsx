import React from 'react';
import { Crown } from 'lucide-react';

/**
 * 2D grid / matrix visualizer.
 *
 * Previously part of GraphCanvas.jsx (lines 160–265), which held five visualizers
 * behind ad-hoc predicates. The grid, sudoku, and chessboard renderers all share
 * the same row×col structure, so they belong in one component keyed on the grid's
 * shape and the problem's identity.
 */
export default function GridCanvas({ problem, currentStep, step }) {
  const activeStep = currentStep || step;
  const gridState = activeStep?.gridState || problem?.defaultGrid;

  if (!gridState || !gridState.length) {
    return (
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--bench-ink-dim)' }}>
        No grid data available
      </div>
    );
  }

  const isSudoku = gridState.length === 9 && gridState[0]?.length === 9;
  const isChessboard = gridState.length === 4 && gridState[0]?.length === 4
    && (problem?.id === 'n-queens' || problem?.title?.toLowerCase().includes('queen'));

  return (
    <div style={{ flex: 1, width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'auto', padding: '16px' }}>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: `repeat(${gridState[0].length}, minmax(${isSudoku ? '24px' : isChessboard ? '44px' : '36px'}, 1fr))`,
          gap: isSudoku ? '2px' : 'var(--space-xs)',
          padding: 'var(--space-md)',
          background: isSudoku ? 'var(--bench-ground)' : 'transparent',
          borderRadius: 'var(--radius-md, 12px)',
          border: isSudoku ? '2px solid var(--bench-rule-strong)' : 'none',
          maxWidth: '100%',
          overflow: 'auto'
        }}
      >
        {gridState.map((row, rIdx) =>
          row.map((val, cIdx) => {
            if (isChessboard) return renderQueenCell(rIdx, cIdx, val);
            if (isSudoku) return renderSudokuCell(rIdx, cIdx, val);
            return renderMatrixCell(rIdx, cIdx, val);
          })
        )}
      </div>
    </div>
  );
}

function renderQueenCell(rIdx, cIdx, val) {
  const isLightSquare = (rIdx + cIdx) % 2 === 0;
  return (
    <div
      key={`queen-${rIdx}-${cIdx}`}
      style={{
        width: '100%',
        aspectRatio: '1',
        minWidth: '44px',
        minHeight: '44px',
        borderRadius: 'var(--radius-xs, 4px)',
        background: val === 1 ? 'rgba(61, 220, 151, 0.25)' : isLightSquare ? 'var(--bench-rule-strong)' : 'var(--bench-fill)',
        border: val === 1 ? '2px solid var(--settled)' : '1px solid var(--bench-rule)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        transition: 'all var(--motion-normal, 300ms) var(--ease-standard, ease)',
        boxShadow: val === 1 ? '0 0 14px rgba(61, 220, 151, 0.4)' : 'none'
      }}
    >
      {val === 1 ? (
        <Crown size={24} color="var(--probe)" style={{ filter: 'drop-shadow(0 0 8px var(--probe))' }} />
      ) : (
        <span style={{ fontSize: 'var(--text-xs, 0.72rem)', color: 'var(--bench-ink-dim)' }}>({rIdx},{cIdx})</span>
      )}
    </div>
  );
}

function renderSudokuCell(rIdx, cIdx, val) {
  const borderRight = (cIdx + 1) % 3 === 0 && cIdx !== 8 ? '2px solid var(--probe)' : '1px solid var(--bench-rule)';
  const borderBottom = (rIdx + 1) % 3 === 0 && rIdx !== 8 ? '2px solid var(--probe)' : '1px solid var(--bench-rule)';

  return (
    <div
      key={`sudoku-${rIdx}-${cIdx}`}
      style={{
        width: '100%',
        aspectRatio: '1',
        minWidth: '24px',
        minHeight: '24px',
        background: val !== 0 ? 'rgba(61, 220, 151, 0.15)' : 'var(--bench-fill)',
        borderRight,
        borderBottom,
        color: val !== 0 ? 'var(--bench-ink)' : 'var(--bench-ink-dim)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontWeight: '800',
        fontFamily: 'var(--font-code)',
        fontSize: 'var(--text-sm, 0.8rem)'
      }}
    >
      {val !== 0 ? val : '.'}
    </div>
  );
}

function renderMatrixCell(rIdx, cIdx, val) {
  return (
    <div
      key={`grid-${rIdx}-${cIdx}`}
      style={{
        width: '55px',
        height: '55px',
        borderRadius: '8px',
        background: val === 2 ? 'rgba(61, 220, 151, 0.25)' : val === 1 || val === 99 ? 'rgba(255, 176, 0, 0.15)' : 'var(--bench-fill)',
        border: val === 2 ? '1px solid var(--settled)' : val === 1 || val === 99 ? '1px solid var(--probe)' : '1px solid var(--bench-rule)',
        boxShadow: val === 2 ? '0 0 14px rgba(61, 220, 151, 0.4)' : val === 1 || val === 99 ? '0 0 14px rgba(255, 176, 0, 0.4)' : 'none',
        color: val !== 0 ? 'var(--bench-ink)' : 'var(--bench-ink-dim)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        fontWeight: '700',
        fontFamily: 'var(--font-code)',
        fontSize: '0.9rem',
        transition: 'all var(--motion-normal, 300ms) var(--ease-standard, ease)'
      }}
    >
      <span>{val}</span>
      <span style={{ fontSize: '0.6rem', opacity: 0.6 }}>({rIdx},{cIdx})</span>
    </div>
  );
}
