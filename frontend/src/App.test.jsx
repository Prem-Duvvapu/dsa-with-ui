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

  it('renders the current step description in LiveTraceTicker', () => {
    render(<LiveTraceTicker stepDescription="Test Step" />);
    expect(screen.getByText('Live trace')).toBeInTheDocument();
    expect(screen.getByText('Test Step')).toBeInTheDocument();
  });
});
