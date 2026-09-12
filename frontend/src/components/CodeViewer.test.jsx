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

  describe('branches the current input never took', () => {
    const problem = { javaCode: 'int find(int[] a, int t) {\n  for (int x : a) {\n    if (x == t) return 1;\n  }\n  return -1;\n}' };
    // Line 3 is the hit, line 5 the miss. A run that finds the target never reaches line 5.
    const anchors = { hit: 3, miss: 5 };
    const steps = [{ activeLine: 2 }, { activeLine: 3 }];

    it('marks an anchored line the run never reached', () => {
      const { container } = render(
        <CodeViewer problem={problem} currentStep={{ activeLine: 3 }} anchors={anchors} steps={steps} />
      );
      const marked = container.querySelectorAll('[data-unreached="true"]');
      expect(marked).toHaveLength(1);
      expect(marked[0].textContent).toContain('return -1');
    });

    it('says how many branches were not taken', () => {
      render(<CodeViewer problem={problem} currentStep={{ activeLine: 3 }} anchors={anchors} steps={steps} />);
      expect(screen.getByTestId('unreached-note').textContent).toContain('1 branch not taken');
    });

    it('never marks the line currently executing', () => {
      // The active line was reached by definition, whatever the anchor set says.
      const { container } = render(
        <CodeViewer problem={problem} currentStep={{ activeLine: 5 }} anchors={anchors} steps={steps} />
      );
      const marked = [...container.querySelectorAll('[data-unreached="true"]')];
      expect(marked.every((el) => !el.textContent.includes('return -1'))).toBe(true);
    });

    it('marks nothing when the run reached every anchor', () => {
      const { container } = render(
        <CodeViewer
          problem={problem}
          currentStep={{ activeLine: 3 }}
          anchors={anchors}
          steps={[{ activeLine: 3 }, { activeLine: 5 }]}
        />
      );
      expect(container.querySelectorAll('[data-unreached="true"]')).toHaveLength(0);
      expect(screen.queryByTestId('unreached-note')).not.toBeInTheDocument();
    });

    it('marks nothing when there is no trace to compare against', () => {
      // An offline or legacy trace carries no anchors. Marking every anchored line as
      // "not taken" there would be a confident lie.
      const { container } = render(
        <CodeViewer problem={problem} currentStep={{ activeLine: 3 }} anchors={null} steps={steps} />
      );
      expect(container.querySelectorAll('[data-unreached="true"]')).toHaveLength(0);
    });
  });
});
