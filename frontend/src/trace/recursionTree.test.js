import { describe, expect, it } from 'vitest';
import { buildRecursionTree, layoutRecursionTree } from './recursionTree';

const stack = (...frames) => ({ callStack: frames });

describe('buildRecursionTree', () => {
  it('turns a descending stack into a chain', () => {
    const { nodes } = buildRecursionTree([stack('a'), stack('a', 'b'), stack('a', 'b', 'c')], 2);
    expect(nodes.map((n) => n.label)).toEqual(['a', 'b', 'c']);
    expect(nodes.map((n) => n.depth)).toEqual([0, 1, 2]);
    expect(nodes[2].parentId).toBe(1);
  });

  it('treats a re-entered frame as a NEW sibling, not the same node', () => {
    // The heart of it. [a,b] -> [a] -> [a,b] is backtracking abandoning one branch and
    // starting another that happens to share a name. Matching on the label alone would
    // merge them and quietly halve the tree.
    const { nodes } = buildRecursionTree([stack('a', 'b'), stack('a'), stack('a', 'b')], 2);
    const bs = nodes.filter((n) => n.label === 'b');
    expect(bs).toHaveLength(2);
    expect(bs[0].id).not.toBe(bs[1].id);
    expect(bs.every((n) => n.parentId === 0)).toBe(true);
  });

  it('marks a popped frame finished and the live path active', () => {
    const { nodes } = buildRecursionTree([stack('a', 'b'), stack('a')], 1);
    expect(nodes.find((n) => n.label === 'b').state).toBe('done');
    expect(nodes.find((n) => n.label === 'a').state).toBe('current');
  });

  it('puts the deepest live frame at current and its ancestors at active', () => {
    const { nodes } = buildRecursionTree([stack('a'), stack('a', 'b'), stack('a', 'b', 'c')], 2);
    expect(nodes.map((n) => n.state)).toEqual(['active', 'active', 'current']);
  });

  it('only builds the tree explored up to the step being shown', () => {
    const steps = [stack('a'), stack('a', 'b'), stack('a', 'c')];
    expect(buildRecursionTree(steps, 1).nodes).toHaveLength(2);
    expect(buildRecursionTree(steps, 2).nodes).toHaveLength(3);
  });

  it('ignores steps that carry no stack rather than breaking the path', () => {
    // Tracers interleave commentary steps. Treating those as an empty stack would mark the
    // whole tree finished and then rebuild it from scratch.
    const steps = [stack('a', 'b'), { variables: { n: '1' } }, stack('a', 'b', 'c')];
    const { nodes } = buildRecursionTree(steps, 2);
    expect(nodes.map((n) => n.label)).toEqual(['a', 'b', 'c']);
  });

  it('caps the node count so a long run cannot try to draw thousands', () => {
    const steps = Array.from({ length: 500 }, (_, i) => stack('root', `n${i}`));
    const { nodes, truncated } = buildRecursionTree(steps, 499, 50);
    expect(nodes.length).toBeLessThanOrEqual(50);
    expect(truncated).toBe(true);
  });

  it('returns nothing for a trace with no stacks at all', () => {
    expect(buildRecursionTree([{ variables: {} }], 0).nodes).toEqual([]);
  });
});

describe('layoutRecursionTree', () => {
  it('centres a parent over its children', () => {
    const { nodes } = buildRecursionTree(
      [stack('a', 'b'), stack('a'), stack('a', 'c')], 2
    );
    const { positions, width } = layoutRecursionTree(nodes);
    const [a, b, c] = nodes;
    expect(positions.get(b.id)).toBe(0);
    expect(positions.get(c.id)).toBe(1);
    expect(positions.get(a.id)).toBe(0.5);
    expect(width).toBe(2);
  });
});
