import React from 'react';

/**
 * A text alternative for whatever the canvas is drawing right now.
 *
 * The app already announces the step NARRATION - LiveTraceTicker is a polite live region -
 * and CanvasShell labels the visualization and pairs every legend colour with a glyph and
 * a word. What none of that carries is the DATA: a screen-reader user hears "swap 4 and
 * 1" without ever learning what the array holds or where the pointer is.
 *
 * This renders that state as a sentence, from the same typed payload the canvas draws
 * from, so the two cannot describe different things. Visually hidden, inside the canvas
 * region, updated per step.
 *
 * Deliberately not a live region of its own: LiveTraceTicker is already polite and a
 * second announcer firing on the same tick would talk over it. This is here to be
 * navigated to and read on demand, which is how someone inspects state.
 */

const MAX_ITEMS = 60;

function listOf(values) {
  if (values.length <= MAX_ITEMS) return values.join(', ');
  return `${values.slice(0, MAX_ITEMS).join(', ')}, and ${values.length - MAX_ITEMS} more`;
}

/** Names the positions carrying a non-default state, so "where am I" is answerable. */
function marked(elements) {
  const notable = elements
    .filter((el) => el.state && el.state !== 'default')
    .map((el) => `${el.state} at index ${el.index}`);
  return notable.length ? ` ${listOf(notable)}.` : '';
}

export function describeStep(step, dsType) {
  if (!step) return '';

  if (Array.isArray(step.arrayState) && step.arrayState.length) {
    const a = step.arrayState;
    return `${dsType === 'Bits' ? 'Bits' : 'Array'} of ${a.length}: `
         + `${listOf(a.map((el) => (el.label ?? el.value)))}.${marked(a)}`;
  }

  if (Array.isArray(step.gridState) && step.gridState.length) {
    const rows = step.gridState.length;
    const cols = step.gridState[0]?.length ?? 0;
    return `Grid, ${rows} rows by ${cols} columns. `
         + `${step.gridState.map((r, i) => `Row ${i}: ${listOf(r)}`).join('. ')}.`;
  }

  if (Array.isArray(step.listState) && step.listState.length) {
    return `Linked list of ${step.listState.length}: `
         + `${listOf(step.listState.map((n) => n.label ?? n.value))}.`;
  }

  if (Array.isArray(step.treeNodes) && step.treeNodes.length) {
    const states = step.nodeStates ?? {};
    const current = Object.entries(states).find(([, s]) => s === 'visiting' || s === 'current');
    return `Tree with ${step.treeNodes.length} nodes.`
         + (current ? ` Node ${current[0]} is current.` : '');
  }

  if (Array.isArray(step.graphNodes) && step.graphNodes.length) {
    const edges = step.graphEdges?.length ?? 0;
    const states = step.nodeStates ?? {};
    const visited = Object.values(states).filter((s) => s === 'visited').length;
    return `Graph with ${step.graphNodes.length} nodes and ${edges} edges. `
         + `${visited} visited so far.`;
  }

  if (Array.isArray(step.queueOrStackState) && step.queueOrStackState.length) {
    const items = step.queueOrStackState;
    const noun = dsType === 'Queue' ? 'Queue' : 'Stack';
    const end = dsType === 'Queue' ? `Front is ${items[0]}.` : `Top is ${items[items.length - 1]}.`;
    return `${noun} holding ${items.length}: ${listOf(items)}. ${end}`;
  }

  if (step.dpTable?.rows?.length) {
    return `DP table, ${step.dpTable.rows.length} rows.`;
  }

  return '';
}

/** Variables carry the answer for problems whose whole state is scalar. */
function describeVariables(step) {
  const vars = step?.variables ?? {};
  const entries = Object.entries(vars);
  if (!entries.length) return '';
  return ` Values: ${entries.map(([k, v]) => `${k} is ${v}`).join(', ')}.`;
}

export default function StepStateSummary({ step, dsType }) {
  const structure = describeStep(step, dsType);
  const values = describeVariables(step);
  if (!structure && !values) return null;

  return (
    <p className="sr-only" data-testid="step-state-summary">
      {structure}{values}
    </p>
  );
}
