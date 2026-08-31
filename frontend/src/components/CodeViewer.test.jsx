import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import '@testing-library/jest-dom';
import CodeViewer from './CodeViewer';

describe('CodeViewer', () => {
  it('shows an explicit unavailable state when problem code is missing', () => {
    render(
      <CodeViewer
        problem={{ id: 'missing-detail', title: 'Missing Detail' }}
        currentStep={{ activeLine: 4 }}
      />
    );

    expect(screen.getByRole('status')).toHaveTextContent('Code unavailable for this problem.');
    expect(screen.queryByText(/Java sliding window \(LeetCode 3\)/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Active line:/)).not.toBeInTheDocument();
  });
});
