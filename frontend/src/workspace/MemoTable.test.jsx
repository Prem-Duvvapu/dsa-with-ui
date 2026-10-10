import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import MemoTable from './MemoTable';

const table = { rowLabels: ['col1=0'], colLabels: ['col2=0'], cells: [[{ value: '0', state: 'known' }]] };
describe('3D memo slice identity', () => {
  it('labels the current slice explicitly and changes it with the actual step', () => {
    const first = { dpTable: table, variables: { memoSlice: 'row 2' } };
    const { rerender } = render(<MemoTable step={first} steps={[first]} dsType="RecursionTree" />);
    fireEvent.click(screen.getByText('Memo table'));
    expect(screen.getByText('Showing memo slice: row 2. Other slices remain cached.')).toBeVisible();
    rerender(<MemoTable step={{ ...first, variables: { memoSlice: 'row 0' } }} steps={[first]} dsType="RecursionTree" />);
    expect(screen.getByText('Showing memo slice: row 0. Other slices remain cached.')).toBeVisible();
    expect(screen.queryByText(/row 2\. Other/)).not.toBeInTheDocument();
  });
  it('does not invent a slice label for a 1D/2D memo or missing payload', () => {
    const step = { dpTable: table, variables: {} };
    render(<MemoTable step={step} steps={[step]} dsType="RecursionTree" />);
    expect(screen.queryByText(/Showing memo slice/)).not.toBeInTheDocument();
  });
});
