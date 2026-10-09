import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import '@testing-library/jest-dom';
import CompareStrip from './CompareStrip';

function execResponse(stepCount, problemId = 'two-sum') {
  return {
    ok: true,
    json: () => Promise.resolve({
      encoding: 'full',
      problemId, approachId: 'canonical', truncated: false,
      resolvedInput: { n: stepCount, nums: [1] },
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

    expect(screen.getByRole('heading', { name: /^Default input/ })).toHaveTextContent('3 steps');
    expect(screen.getByRole('heading', { name: /^Other case/ })).toHaveTextContent('7 steps');
  });

  it('offers a retry when either run fails to load', async () => {
    fetch.mockResolvedValueOnce(execResponse(3)).mockResolvedValueOnce({ ok: false, status: 500 });

    render(<CompareStrip problemId="two-sum" dsType="Array" alternateInput={{ nums: [1] }} />);

    await waitFor(() => expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument());

    fetch.mockResolvedValueOnce(execResponse(3)).mockResolvedValueOnce(execResponse(7));
    fireEvent.click(screen.getByRole('button', { name: /retry/i }));

    await waitFor(() => expect(screen.getByRole('heading', { name: /^Other case/ })).toHaveTextContent('7 steps'));
  });

  it('labels each side with its own input and position, and reads a graph run as text', async () => {
    fetch.mockResolvedValueOnce(execResponse(3, 'bfs')).mockResolvedValueOnce(execResponse(7, 'bfs'));
    render(<CompareStrip problemId="bfs" dsType="Graph" alternateInput={{ n: 7 }} showCapture={false} />);
    const other = await screen.findByRole('region', { name: 'Other case' });
    expect(other).toHaveTextContent('n7');
    expect(other).toHaveTextContent('Step 1 of 7');
    expect(other).toHaveTextContent('step 1');
    expect(screen.queryByLabelText(/Execution capture/)).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Other case: next step' }));
    expect(other).toHaveTextContent('Step 2 of 7');
    expect(other).toHaveTextContent('step 2');
    // Moving one side never moves the other.
    expect(screen.getByRole('region', { name: 'Default input' })).toHaveTextContent('Step 1 of 3');
  });

  it('states its scope, and that a submitted main run is not part of it', async () => {
    fetch.mockResolvedValueOnce(execResponse(3)).mockResolvedValueOnce(execResponse(7));
    render(<CompareStrip problemId="two-sum" dsType="Array" alternateInput={{ n: 7 }} mainIsCustom />);
    expect(screen.getByText(/default input with its other case/)).toHaveTextContent('The run above uses your own input and is not part of this comparison.');
    expect(screen.getByText(/default input with its other case/)).toHaveTextContent('not matching moments of the algorithm');
  });

  it('labels each cut-short run explicitly, without hiding its available events', async () => {
    const data = { problemId: 'stairs', approachId: 'canonical', truncated: true,
      resolvedInput: { n: 7 }, encoding: 'full', steps: [{ stepNumber: 1, description: 'Partial event' }] };
    fetch.mockResolvedValue({ ok: true, json: async () => data });
    render(<CompareStrip problemId="stairs" dsType="DpTable" alternateInput={{ n: 7 }} showCapture={false} />);
    const other = await screen.findByRole('region', { name: 'Other case' });
    expect(other).toHaveTextContent(/incomplete.*cut short/i);
    expect(other).toHaveTextContent('Partial event');
    expect(screen.getByRole('region', { name: 'Default input' })).toHaveTextContent(/incomplete.*cut short/i);
  });
});
