import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import ProblemStatement from './ProblemStatement';

describe('ProblemStatement', () => {
  it('states what the problem asks', () => {
    render(<ProblemStatement problem={{ description: 'Find the contiguous subarray with the largest sum.' }} />);
    expect(screen.getByText('Find the contiguous subarray with the largest sum.')).toBeInTheDocument();
  });

  it('lists the source problem constraints under a label that says whose they are', () => {
    render(
      <ProblemStatement
        problem={{ description: 'Find it.', constraints: ['1 <= nums.length <= 10^5', '-10^4 <= nums[i] <= 10^4'] }}
      />
    );
    // "Problem constraints", not just "Constraints" - the input panel has its own, much
    // smaller limits, and an unlabelled list would read as if it described this tool.
    expect(screen.getByText('Problem constraints')).toBeInTheDocument();
    expect(screen.getByText('1 <= nums.length <= 10^5')).toBeInTheDocument();
    expect(screen.getByText('-10^4 <= nums[i] <= 10^4')).toBeInTheDocument();
  });

  it('omits the constraints section entirely when none are recorded', () => {
    // Absent must look absent. Anything shown here is read as the real problem's bound.
    render(<ProblemStatement problem={{ description: 'Find it.' }} />);
    expect(screen.queryByText('Problem constraints')).not.toBeInTheDocument();
  });

  it('renders nothing at all when the problem has neither', () => {
    const { container } = render(<ProblemStatement problem={{}} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('survives a missing problem while the catalogue is still loading', () => {
    const { container } = render(<ProblemStatement problem={null} />);
    expect(container).toBeEmptyDOMElement();
  });
});
