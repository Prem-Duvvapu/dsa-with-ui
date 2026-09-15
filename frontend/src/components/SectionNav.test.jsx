import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi } from 'vitest';
import SectionNav from './SectionNav';

// Catalogue order IS curriculum order - the backend serves problems in the sequence each
// service authored them, and that sequence is already a progression (see
// ProblemCatalog's LinkedHashMap). This component walks that order rather than inventing
// its own, so these fixtures matter in the order they're written.
const problems = [
  { id: 'frog-jump', title: 'Frog Jump', striverSheetSection: 'DP - Basic DP' },
  { id: 'max-sum-non-adjacent', title: 'Max Sum', striverSheetSection: 'DP - Basic DP' },
  { id: 'house-robber-2', title: 'House Robber II', striverSheetSection: 'DP - Basic DP' },
  { id: 'ninjas-training', title: "Ninja's Training", striverSheetSection: 'DP - Grids' }
];

describe('SectionNav', () => {
  it('states position within the section, in catalogue order', () => {
    render(<SectionNav problems={problems} activeProblemId="max-sum-non-adjacent" onSelectProblem={() => {}} />);
    expect(screen.getByText('DP - Basic DP')).toBeInTheDocument();
    expect(screen.getByText('2 of 3')).toBeInTheDocument();
  });

  it('navigates to the next problem in the section', () => {
    const onSelectProblem = vi.fn();
    render(<SectionNav problems={problems} activeProblemId="frog-jump" onSelectProblem={onSelectProblem} />);
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(onSelectProblem).toHaveBeenCalledWith('max-sum-non-adjacent');
  });

  it('navigates to the previous problem in the section', () => {
    const onSelectProblem = vi.fn();
    render(<SectionNav problems={problems} activeProblemId="house-robber-2" onSelectProblem={onSelectProblem} />);
    fireEvent.click(screen.getByRole('button', { name: /previous/i }));
    expect(onSelectProblem).toHaveBeenCalledWith('max-sum-non-adjacent');
  });

  it('has no previous button at the start of a section', () => {
    render(<SectionNav problems={problems} activeProblemId="frog-jump" onSelectProblem={() => {}} />);
    expect(screen.queryByRole('button', { name: /previous/i })).not.toBeInTheDocument();
    // Still at the front of the section, not merely missing a control.
    expect(screen.getByRole('button', { name: /next/i })).toBeInTheDocument();
  });

  it('has no next button at the end of a section', () => {
    // A separate render, not a second render() call in the same test: RTL appends to
    // document.body rather than replacing it, so reusing the frog-jump render above would
    // leave its own "Next" button in the document and this assertion would pass by
    // accident even if the end-of-section case were broken.
    render(<SectionNav problems={problems} activeProblemId="house-robber-2" onSelectProblem={() => {}} />);
    expect(screen.queryByRole('button', { name: /next/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /previous/i })).toBeInTheDocument();
  });

  it('never crosses into a different section', () => {
    // Ninja's Training is alone in "DP - Grids" in this fixture. Both neighbours must be
    // absent, not silently borrowed from the adjacent section.
    render(<SectionNav problems={problems} activeProblemId="ninjas-training" onSelectProblem={() => {}} />);
    expect(screen.queryByRole('button', { name: /previous/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /next/i })).not.toBeInTheDocument();
  });

  it('renders nothing for a section of one - there is nowhere to go', () => {
    const { container } = render(
      <SectionNav problems={problems} activeProblemId="ninjas-training" onSelectProblem={() => {}} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when the active problem carries no section', () => {
    const noSectionProblems = [{ id: 'two-sum', title: 'Two Sum' }];
    const { container } = render(
      <SectionNav problems={noSectionProblems} activeProblemId="two-sum" onSelectProblem={() => {}} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when the catalogue has not loaded yet', () => {
    const { container } = render(
      <SectionNav problems={[]} activeProblemId="two-sum" onSelectProblem={() => {}} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('marks a neighbour already watched, so the choice is not blind', () => {
    render(
      <SectionNav
        problems={problems}
        activeProblemId="frog-jump"
        progress={{ 'max-sum-non-adjacent': { watched: true } }}
        onSelectProblem={() => {}}
      />
    );
    const next = screen.getByRole('button', { name: /next/i });
    expect(next).toHaveTextContent(/watched/i);
  });
});
