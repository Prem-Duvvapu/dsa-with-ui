import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import '@testing-library/jest-dom';
import TrieCanvas from './TrieCanvas';
import { decodeTrace } from '../trace/decodeTrace';

/**
 * RCA-012 regression guard: a fixture shaped exactly like the backend's real
 * `TrieNodeModel` serialization — `character`/`endOfWord`/a char-to-id `children` map —
 * not the pre-fix canvas assumption of `char`/`isEnd`/an id array. This is the fixture
 * RCA-012 calls for: it proves the exact wire shape renders, not a shape the canvas
 * happens to already like.
 */
function backendShapedTrieState() {
  return [
    { id: 0, character: null, endOfWord: false, x: 190, y: 40, children: { c: 1 }, state: 'visited' },
    { id: 1, character: 'c', endOfWord: false, x: 150, y: 110, children: { a: 2 }, state: 'visited' },
    { id: 2, character: 'a', endOfWord: false, x: 150, y: 180, children: { t: 3 }, state: 'visited' },
    { id: 3, character: 't', endOfWord: true, x: 150, y: 250, children: {}, state: 'done' }
  ];
}

describe('TrieCanvas (RCA-012 canonical shape)', () => {
  it('renders the exact backend-shaped node set: character labels, endOfWord marker, children map edges', () => {
    const currentStep = { trieState: backendShapedTrieState() };
    render(<TrieCanvas currentStep={currentStep} />);

    // Root has no character; rendered as the placeholder glyph, not "undefined".
    expect(screen.getByText('•')).toBeInTheDocument();
    expect(screen.getByText('c')).toBeInTheDocument();
    expect(screen.getByText('a')).toBeInTheDocument();
    expect(screen.getByText('t')).toBeInTheDocument();

    // One edge per children-map entry: root->c->a->t is 3 edges, not 4 (no self-loop,
    // no phantom edge from an id that was never a value in a children map).
    const lines = document.querySelectorAll('svg line');
    expect(lines.length).toBe(3);

    // The word-ending node ('t', endOfWord: true) carries the small end-of-word marker
    // circle in addition to its own node circle; non-ending nodes carry only one circle.
    const tNode = screen.getByText('t');
    const tCircles = tNode.parentElement.querySelectorAll('circle');
    expect(tCircles.length).toBe(2);
    const cNode = screen.getByText('c');
    const cCircles = cNode.parentElement.querySelectorAll('circle');
    expect(cCircles.length).toBe(1);
  });

  it('positions nodes using the backend-supplied x/y, the same convention TreeCanvas uses, rather than computing its own layout', () => {
    const currentStep = { trieState: backendShapedTrieState() };
    render(<TrieCanvas currentStep={currentStep} />);

    const tNode = screen.getByText('t');
    expect(tNode.parentElement).toHaveAttribute('transform', 'translate(150, 250)');
  });

  it('round-trips a delta-encoded response carrying the backend shape end to end', () => {
    const trieState = backendShapedTrieState();
    const decoded = decodeTrace({
      encoding: 'delta',
      steps: [
        { stepNumber: 1, keyframe: true, dsType: 'Trie', trieState },
        { stepNumber: 2, description: 'no trie change this step' }
      ]
    });

    // Field carried forward unchanged, exactly as it arrived from the backend.
    expect(decoded[1].trieState).toBe(trieState);

    render(<TrieCanvas currentStep={decoded[1]} />);
    expect(screen.getByText('t')).toBeInTheDocument();
    expect(screen.getByText('c')).toBeInTheDocument();
  });

  it('renders a placeholder instead of crashing on an empty trieState', () => {
    render(<TrieCanvas currentStep={{ trieState: [] }} />);
    expect(screen.getByText('No trie data available')).toBeInTheDocument();
  });

  it('renders a placeholder instead of crashing when trieState is absent (malformed/untraced step)', () => {
    render(<TrieCanvas currentStep={{}} />);
    expect(screen.getByText('No trie data available')).toBeInTheDocument();
  });
});
