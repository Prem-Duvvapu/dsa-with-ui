/**
 * Generates a random value satisfying one InputField's declared constraints, and a full
 * value map for an InputSpec. Pure and side-effect free — `rng` defaults to Math.random
 * but tests inject a seeded function so "randomize" is actually testable, not just "runs
 * without throwing."
 *
 * Constraint keys mirror the backend's InputValidator exactly (min/max, minLength/
 * maxLength, minValue/maxValue, requireSorted, requireDistinct, maxVertices/maxEdges,
 * maxRows/maxCols, weighted) — this file has no independent opinion about what a field
 * is allowed to hold. The server is still authoritative; this only has to produce values
 * that clear the client-side hints, since the point of "randomize" is a plausible input,
 * not an adversarial one.
 */

function intBetween(rng, min, max) {
  if (max < min) return min;
  return min + Math.floor(rng() * (max - min + 1));
}

function randomInt(rng, field) {
  const c = field.constraints || {};
  const min = c.min ?? -100;
  const max = c.max ?? 100;
  return intBetween(rng, min, max);
}

function randomIntList(rng, field, { allowNulls = false } = {}) {
  const c = field.constraints || {};
  const minLen = c.minLength ?? (allowNulls ? 1 : 1);
  // Cap the generated length well under maxLength so a "randomize" click stays readable,
  // even when the spec permits up to 40+ elements.
  const maxLen = Math.min(c.maxLength ?? 10, Math.max(minLen, 10));
  const length = intBetween(rng, minLen, maxLen);
  const minVal = c.minValue ?? -50;
  const maxVal = c.maxValue ?? 50;

  const values = [];
  const seen = new Set();
  for (let i = 0; i < length; i++) {
    if (allowNulls && i > 0 && rng() < 0.25) {
      values.push(null);
      continue;
    }
    let v = intBetween(rng, minVal, maxVal);
    if (c.requireDistinct) {
      // Bounded retries: a tiny range with requireDistinct can't always satisfy length,
      // so fall through to whatever was drawn last rather than looping forever.
      let attempts = 0;
      while (seen.has(v) && attempts < 20) {
        v = intBetween(rng, minVal, maxVal);
        attempts += 1;
      }
      seen.add(v);
    }
    values.push(v);
  }

  if (c.requireSorted) {
    values.sort((a, b) => {
      if (a === null) return -1;
      if (b === null) return 1;
      return a - b;
    });
  }
  return values;
}

function randomString(rng, field) {
  const c = field.constraints || {};
  const minLen = c.minLength ?? 3;
  const maxLen = Math.min(c.maxLength ?? 10, Math.max(minLen, 10));
  const length = intBetween(rng, minLen, maxLen);
  const alphabet = 'abcdefghijklmnopqrstuvwxyz';
  let out = '';
  for (let i = 0; i < length; i++) {
    out += alphabet[Math.floor(rng() * alphabet.length)];
  }
  return out;
}

function randomGrid(rng, field) {
  const c = field.constraints || {};
  const rows = intBetween(rng, c.minRows ?? 2, Math.min(c.maxRows ?? 6, 8));
  const cols = intBetween(rng, 2, Math.min(c.maxCols ?? 6, 8));
  const minVal = c.minValue ?? 0;
  const maxVal = c.maxValue ?? 1;
  const grid = [];
  for (let r = 0; r < rows; r++) {
    const row = [];
    for (let col = 0; col < cols; col++) {
      row.push(intBetween(rng, minVal, maxVal));
    }
    grid.push(row);
  }
  return grid;
}

function randomGraph(rng, field) {
  const c = field.constraints || {};
  const maxVertices = Math.min(c.maxVertices ?? 10, 10);
  const vertices = intBetween(rng, Math.min(3, maxVertices), maxVertices);
  const maxEdges = Math.min(c.maxEdges ?? vertices * 2, vertices * (vertices - 1));
  const weighted = c.weighted === true;

  // A random spanning chain first, so the graph is always connected and never blank —
  // an edgeless graph "randomizes" nothing visible.
  const order = Array.from({ length: vertices }, (_, i) => i);
  for (let i = order.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1));
    [order[i], order[j]] = [order[j], order[i]];
  }
  const edges = [];
  const edgeKey = (a, b) => `${Math.min(a, b)}-${Math.max(a, b)}`;
  const seen = new Set();
  const pushEdge = (a, b) => {
    const key = edgeKey(a, b);
    if (seen.has(key) || a === b) return;
    seen.add(key);
    edges.push(weighted ? [a, b, intBetween(rng, 1, 20)] : [a, b]);
  };

  for (let i = 1; i < order.length; i++) {
    pushEdge(order[i - 1], order[i]);
  }

  // A few extra edges for branching, bounded by maxEdges.
  const extra = Math.max(0, Math.min(maxEdges - edges.length, vertices));
  for (let i = 0; i < extra && rng() < 0.6; i++) {
    pushEdge(intBetween(rng, 0, vertices - 1), intBetween(rng, 0, vertices - 1));
  }

  return { vertices, edges };
}

function randomBinaryTree(rng, field) {
  const c = field.constraints || {};
  const minLen = Math.max(1, c.minLength ?? 1);
  const maxLen = Math.min(c.maxLength ?? 15, 15);
  const length = intBetween(rng, minLen, Math.max(minLen, maxLen));
  const minVal = c.minValue ?? -50;
  const maxVal = c.maxValue ?? 50;
  const values = [intBetween(rng, minVal, maxVal)];
  for (let i = 1; i < length; i++) {
    values.push(rng() < 0.25 ? null : intBetween(rng, minVal, maxVal));
  }
  return values;
}

/** One random value for a single field, per its declared FieldType. */
export function randomizeValue(field, rng = Math.random) {
  switch (field.type) {
    case 'INT':
      return randomInt(rng, field);
    case 'INT_ARRAY':
      return randomIntList(rng, field, { allowNulls: false });
    case 'LINKED_LIST':
      return randomIntList(rng, field, { allowNulls: false });
    case 'STRING':
      return randomString(rng, field);
    case 'INT_GRID':
      return randomGrid(rng, field);
    case 'GRAPH':
      return randomGraph(rng, field);
    case 'BINARY_TREE':
      return randomBinaryTree(rng, field);
    default:
      return field.defaultValue;
  }
}

/** A full { fieldName: value } map for every field in an InputSpec. */
export function randomizeInput(inputSpec, rng = Math.random) {
  const values = {};
  for (const field of inputSpec?.fields || []) {
    values[field.name] = randomizeValue(field, rng);
  }
  return values;
}

/** The spec's own declared defaults, as a { fieldName: value } map. */
export function defaultInput(inputSpec) {
  const values = {};
  for (const field of inputSpec?.fields || []) {
    values[field.name] = field.defaultValue;
  }
  return values;
}
