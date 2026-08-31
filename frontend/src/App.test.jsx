import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import React from 'react';
import '@testing-library/jest-dom';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import Controls from './components/Controls';
import LiveTraceTicker from './components/LiveTraceTicker';

describe('Frontend Component Tests', () => {
  it('renders Header with the product name and catalog size', () => {
    render(<Header totalProblems={11} />);
    expect(screen.getByText('DSA Visualizer')).toBeInTheDocument();
    expect(screen.getByText('11 algorithms')).toBeInTheDocument();
  });

  it('renders Sidebar with list of graph problems', () => {
    const mockProblems = [
      { id: 'bfs-traversal', title: 'BFS Traversal of Graph', category: 'Graph BFS/DFS', difficulty: 'Easy', dsType: 'Queue' },
      { id: 'dfs-traversal', title: 'DFS Traversal of Graph', category: 'Graph BFS/DFS', difficulty: 'Easy', dsType: 'Stack' }
    ];
    render(
      <Sidebar
        problems={mockProblems}
        activeProblemId="bfs-traversal"
        activeCategory="Graph BFS/DFS"
        onSelectCategory={() => {}}
        onSelectProblem={() => {}}
      />
    );
    expect(screen.getByText('BFS Traversal of Graph')).toBeInTheDocument();
    expect(screen.getByText('DFS Traversal of Graph')).toBeInTheDocument();
  });

  it('renders Controls player buttons', () => {
    render(
      <Controls
        isPlaying={false}
        currentStepIndex={0}
        totalSteps={5}
        speed={800}
        onPlayPause={() => {}}
        onStepNext={() => {}}
        onStepPrev={() => {}}
        onStepSelect={() => {}}
        onReset={() => {}}
        onSpeedChange={() => {}}
      />
    );
    expect(screen.getByText('Play')).toBeInTheDocument();
    expect(screen.getByText('Reset')).toBeInTheDocument();
  });

  it('disables playback and reports 0 of 0 when a trace has no steps', () => {
    render(
      <Controls
        isPlaying={false}
        currentStepIndex={0}
        totalSteps={0}
        speed={1000}
        onPlayPause={() => {}}
        onStepNext={() => {}}
        onStepPrev={() => {}}
        onStepSelect={() => {}}
        onReset={() => {}}
        onSpeedChange={() => {}}
      />
    );

    expect(screen.getByLabelText('Playback position')).toHaveTextContent('Step 0 of 0');
    expect(screen.getByRole('slider')).toBeDisabled();
    for (const name of ['Reset', 'Prev', 'Play', 'Next', '0.5x', '1x', '2x', '4x']) {
      expect(screen.getByRole('button', { name })).toBeDisabled();
    }
  });

  it('renders the current step description in LiveTraceTicker', () => {
    render(<LiveTraceTicker stepDescription="Test Step" />);
    expect(screen.getByText('Live trace')).toBeInTheDocument();
    expect(screen.getByText('Test Step')).toBeInTheDocument();
  });

  it('uses neutral copy when there is no trace step', () => {
    render(<LiveTraceTicker />);
    expect(screen.getByText('Trace status')).toBeInTheDocument();
    expect(screen.getByText('No trace steps available.')).toBeInTheDocument();
    expect(screen.queryByText(/abcabcbb/)).not.toBeInTheDocument();
  });
});
