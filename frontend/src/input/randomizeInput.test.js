import { describe, it, expect } from 'vitest';
import { randomizeValue, randomizeInput, defaultInput } from './randomizeInput';

/** A tiny seeded PRNG so "randomize" is reproducible in tests, not just non-throwing. */
function seeded(seed) {
  let s = seed >>> 0;
  return () => {
    s = (s * 1664525 + 1013904223) >>> 0;
    return s / 4294967296;
  };
}

/** Every seed in this range, so a constraint check isn't one lucky draw. */
function forManySeeds(fn) {
  for (let seed = 1; seed <= 40; seed++) {
    fn(seeded(seed));
  }
}

describe('randomizeValue', () => {
  it('INT stays within min/max', () => {
    const field = { type: 'INT', constraints: { min: -5, max: 5 } };
    forManySeeds((rng) => {
      const v = randomizeValue(field, rng);
      expect(v).toBeGreaterThanOrEqual(-5);
      expect(v).toBeLessThanOrEqual(5);
      expect(Number.isInteger(v)).toBe(true);
    });
  });

  it('INT_ARRAY respects length and value bounds', () => {
    const field = {
      type: 'INT_ARRAY',
      constraints: { minLength: 2, maxLength: 6, minValue: -3, maxValue: 3 }
    };
    forManySeeds((rng) => {
      const v = randomizeValue(field, rng);
      expect(v.length).toBeGreaterThanOrEqual(2);
      expect(v.length).toBeLessThanOrEqual(6);
      v.forEach((n) => {
        expect(n).toBeGreaterThanOrEqual(-3);
        expect(n).toBeLessThanOrEqual(3);
      });
    });
  });

  it('INT_ARRAY never emits null (LINKED_LIST shares this path)', () => {
    const field = { type: 'INT_ARRAY', constraints: { minLength: 5, maxLength: 5 } };
    forManySeeds((rng) => {
      const v = randomizeValue(field, rng);
      expect(v.every((n) => n !== null)).toBe(true);
    });
  });

  it('requireSorted always produces a non-decreasing array', () => {
    const field = {
      type: 'INT_ARRAY',
      constraints: { minLength: 8, maxLength: 8, minValue: -5, maxValue: 5, requireSorted: true }
    };
    forManySeeds((rng) => {
      const v = randomizeValue(field, rng);
      for (let i = 1; i < v.length; i++) {
        expect(v[i]).toBeGreaterThanOrEqual(v[i - 1]);
      }
    });
  });

  it('requireDistinct produces no repeats when the range comfortably allows it', () => {
    const field = {
      type: 'INT_ARRAY',
      constraints: { minLength: 6, maxLength: 6, minValue: 0, maxValue: 99, requireDistinct: true }
    };
    forManySeeds((rng) => {
      const v = randomizeValue(field, rng);
      expect(new Set(v).size).toBe(v.length);
    });
  });

  it('STRING respects length bounds', () => {
    const field = { type: 'STRING', constraints: { minLength: 4, maxLength: 8 } };
    forManySeeds((rng) => {
      const v = randomizeValue(field, rng);
      expect(v.length).toBeGreaterThanOrEqual(4);
      expect(v.length).toBeLessThanOrEqual(8);
      expect(v).toMatch(/^[a-z]+$/);
    });
  });

  it('INT_GRID is rectangular and within row/col/value bounds', () => {
    const field = {
      type: 'INT_GRID',
      constraints: { minRows: 2, maxRows: 4, maxCols: 4, minValue: 0, maxValue: 1 }
    };
    forManySeeds((rng) => {
      const grid = randomizeValue(field, rng);
      expect(grid.length).toBeGreaterThanOrEqual(2);
      expect(grid.length).toBeLessThanOrEqual(4);
      const width = grid[0].length;
      expect(width).toBeLessThanOrEqual(4);
      grid.forEach((row) => {
        expect(row.length).toBe(width);
        row.forEach((cell) => {
          expect(cell).toBeGreaterThanOrEqual(0);
          expect(cell).toBeLessThanOrEqual(1);
        });
      });
    });
  });

  it('GRAPH stays within maxVertices/maxEdges and every edge references a real vertex', () => {
    const field = { type: 'GRAPH', constraints: { maxVertices: 6, maxEdges: 8 } };
    forManySeeds((rng) => {
      const g = randomizeValue(field, rng);
      expect(g.vertices).toBeLessThanOrEqual(6);
      expect(g.vertices).toBeGreaterThanOrEqual(1);
      expect(g.edges.length).toBeLessThanOrEqual(8);
      g.edges.forEach(([a, b]) => {
        expect(a).toBeGreaterThanOrEqual(0);
        expect(a).toBeLessThan(g.vertices);
        expect(b).toBeGreaterThanOrEqual(0);
        expect(b).toBeLessThan(g.vertices);
      });
    });
  });

  it('GRAPH is connected — a random graph should not be edgeless', () => {
    const field = { type: 'GRAPH', constraints: { maxVertices: 6, maxEdges: 8 } };
    forManySeeds((rng) => {
      const g = randomizeValue(field, rng);
      if (g.vertices > 1) {
        expect(g.edges.length).toBeGreaterThan(0);
      }
    });
  });

  it('GRAPH emits a third weight element only when weighted', () => {
    const plain = randomizeValue({ type: 'GRAPH', constraints: { maxVertices: 5 } }, seeded(3));
    const weighted = randomizeValue(
      { type: 'GRAPH', constraints: { maxVertices: 5, weighted: true } }, seeded(3));
    plain.edges.forEach((e) => expect(e).toHaveLength(2));
    weighted.edges.forEach((e) => expect(e).toHaveLength(3));
  });

  it('BINARY_TREE never puts null at the root', () => {
    const field = { type: 'BINARY_TREE', constraints: { minLength: 1, maxLength: 12, minValue: -9, maxValue: 9 } };
    forManySeeds((rng) => {
      const v = randomizeValue(field, rng);
      expect(v[0]).not.toBeNull();
    });
  });
});

describe('randomizeInput', () => {
  it('produces one entry per declared field', () => {
    const spec = {
      fields: [
        { name: 'nums', type: 'INT_ARRAY', constraints: {} },
        { name: 'target', type: 'INT', constraints: {} }
      ]
    };
    const values = randomizeInput(spec, seeded(7));
    expect(Object.keys(values).sort()).toEqual(['nums', 'target']);
  });
});

describe('defaultInput', () => {
  it('reads defaultValue straight off each field', () => {
    const spec = {
      fields: [
        { name: 'nums', type: 'INT_ARRAY', defaultValue: [1, 2, 3] },
        { name: 'target', type: 'INT', defaultValue: 5 }
      ]
    };
    expect(defaultInput(spec)).toEqual({ nums: [1, 2, 3], target: 5 });
  });

  it('is empty for a spec with no fields', () => {
    expect(defaultInput({ fields: [] })).toEqual({});
    expect(defaultInput(null)).toEqual({});
  });
});
