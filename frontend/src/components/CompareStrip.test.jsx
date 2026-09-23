import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import '@testing-library/jest-dom';
import CompareStrip from './CompareStrip';

function execResponse(stepCount) {
  return {
    ok: true,
    json: () => Promise.resolve({
      encoding: 'full',
      resolvedInput: { n: stepCount },
      steps: Array.from({ length: stepCount }, (_, i) => ({
        stepNumber: i + 1,
        description: `step ${i + 1}`,
        arrayState: [{ index: 0, value: i, state: 'default' }]
      }))
    })
  };
}

describe('CompareStrip', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
  });

  it('shows a loading state, then both runs with their own step counts', async () => {
    fetch.mockResolvedValueOnce(execResponse(3)).mockResolvedValueOnce(execResponse(7));

    render(<CompareStrip problemId="two-sum" dsType="Array" alternateInput={{ nums: [1] }} />);

    expect(screen.getByText(/loading/i)).toBeInTheDocument();

    await waitFor(() => expect(screen.queryByText(/loading/i)).not.toBeInTheDocument());

    expect(screen.getByText(/default input/i)).toHaveTextContent('3 steps');
    expect(screen.getByText(/other case/i)).toHaveTextContent('7 steps');
  });

  it('offers a retry when either run fails to load', async () => {
    fetch.mockResolvedValueOnce(execResponse(3)).mockResolvedValueOnce({ ok: false, status: 500 });

    render(<CompareStrip problemId="two-sum" dsType="Array" alternateInput={{ nums: [1] }} />);

    await waitFor(() => expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument());

    fetch.mockResolvedValueOnce(execResponse(3)).mockResolvedValueOnce(execResponse(7));
    fireEvent.click(screen.getByRole('button', { name: /retry/i }));

    await waitFor(() => expect(screen.getByText(/other case/i)).toHaveTextContent('7 steps'));
  });
});
