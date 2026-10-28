/**
 * Generates a random value satisfying one InputField's declared constraints, and a full
 * value map for an InputSpec. Pure and side-effect free — `rng` defaults to Math.random
 * but tests inject a seeded function so "randomize" is actually testable, not just "runs
 * without throwing."
 *
 * Constraint keys mirror the backend's InputValidator exactly (min/max, minLength/
 * maxLength, minValue/maxValue, requireSorted, requireDistinct, maxVertices/maxEdges,
 * maxRows/maxCols, weighted). STRING generation also reads declared alphabet and
 * randomMaxLength hints; the latter limits samples without changing validation bounds.
 * This file has no independent opinion about what a field is allowed to hold.
 * The server is still authoritative; this only has to produce values
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

function randomIntList(rng, field, { allowNulls = false, length: sharedLength } = {}) {
  const c = field.constraints || {};
  const minLen = c.minLength ?? (allowNulls ? 1 : 1);
  // Cap the generated length well under maxLength so a "randomize" click stays readable,
  // even when the spec permits up to 40+ elements.
  const maxLen = Math.min(c.maxLength ?? 10, Math.max(minLen, 10));
  const length = sharedLength ?? intBetween(rng, minLen, maxLen);
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
  if (c.requireInfixExpression === true) return randomInfix(rng, c);
  const hasAlphabet = Object.prototype.hasOwnProperty.call(c, 'alphabet');
  const hasSampleCap = Object.prototype.hasOwnProperty.call(c, 'randomMaxLength');
  if (hasAlphabet && (typeof c.alphabet !== 'string' || c.alphabet.length === 0)) {
    throw new RangeError('STRING alphabet must be a nonempty string.');
  }
  if (hasSampleCap && (!Number.isSafeInteger(c.randomMaxLength) || c.randomMaxLength < 0)) {
    throw new RangeError('STRING randomMaxLength must be a nonnegative integer.');
  }
  const minLen = c.minLength ?? 3;
  const sampleCap = hasSampleCap ? c.randomMaxLength : Math.max(minLen, 10);
  const maxLen = Math.min(c.maxLength ?? (hasSampleCap ? sampleCap : 10), sampleCap);
  if ((hasAlphabet || hasSampleCap) && (
    !Number.isSafeInteger(minLen) || minLen < 0 ||
    !Number.isSafeInteger(maxLen) || maxLen < minLen
  )) {
    throw new RangeError('STRING randomMaxLength and validation bounds must permit a valid length.');
  }
  const length = intBetween(rng, minLen, maxLen);
  const alphabet = hasAlphabet ? c.alphabet : 'abcdefghijklmnopqrstuvwxyz';
  let out = '';
  for (let i = 0; i < length; i++) {
    out += alphabet[Math.floor(rng() * alphabet.length)];
  }
  return out;
}

// Build a short legal binary expression; no retry loop and no problem-id routing.
// A valid expression has odd length (2*operands - 1, plus pairs of parentheses).
function randomInfix(rng, constraints) {
  const minLength = Math.max(1, constraints.minLength ?? 1);
  const maxLength = Math.min(constraints.maxLength ?? 15, Math.max(minLength, 15));
  const minOperands = Math.ceil((minLength + 1) / 2);
  const maxOperands = Math.floor((maxLength + 1) / 2);
  if (minOperands > maxOperands) throw new RangeError('Infix bounds must permit an odd-length expression.');
  const operands = intBetween(rng, minOperands, maxOperands);
  const letter = () => String.fromCharCode(intBetween(rng, 65, 90));
  let result = letter();
  for (let index = 1; index < operands; index++) {
    // Reserve two characters for every remaining operator/operand pair.
    if (result.length > 1 && result.length + 2 + 2 * (operands - index) <= maxLength && rng() < 0.5) {
      result = `(${result})`;
    }
    result += '+-*/^'[intBetween(rng, 0, 4)] + letter();
  }
  return result;
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

// Validate the whole declaration before drawing anything. The shared length is chosen
// from actual validation bounds; ten is only a readability preference, never a limit
// that can override a member's minimum. No generated list is trimmed or padded.
function equalLengthRanges(inputSpec) {
  if (!Object.prototype.hasOwnProperty.call(inputSpec ?? {}, 'equalLengthGroups')) return [];
  const groups = inputSpec.equalLengthGroups;
  if (!Array.isArray(groups)) throw new RangeError('equalLengthGroups must be an array of groups.');
  const fields = inputSpec.fields ?? [];
  const used = new Set();
  return groups.map(names => {
    if (!Array.isArray(names) || names.length < 2) {
      throw new RangeError('Each equalLengthGroups group must name at least two INT_ARRAY fields.');
    }
    let min = 0;
    let max = Infinity;
    for (const name of names) {
      const matches = fields.filter(field => field.name === name);
      if (typeof name !== 'string' || !name.trim() || used.has(name) ||
          matches.length !== 1 || matches[0].type !== 'INT_ARRAY') {
        throw new RangeError('equalLengthGroups requires disjoint, unique, known INT_ARRAY field names.');
      }
      used.add(name);
      const constraints = matches[0].constraints ?? {};
      for (const key of ['minLength', 'maxLength']) {
        if (Object.prototype.hasOwnProperty.call(constraints, key) &&
            (!Number.isSafeInteger(constraints[key]) || constraints[key] < 0)) {
          throw new RangeError('equalLengthGroups length bounds must be nonnegative integers.');
        }
      }
      min = Math.max(min, constraints.minLength ?? 0);
      max = Math.min(max, constraints.maxLength ?? Infinity);
    }
    if (min > max) throw new RangeError('equalLengthGroups length bounds have no common intersection.');
    // Unspecified minima permit empty arrays on the server. Prefer the existing
    // nonempty sample style when possible, while allowing an empty-only contract.
    const sampleMin = Math.min(max, Math.max(1, min));
    return { names, min: sampleMin, max: Math.min(max, Math.max(10, sampleMin)) };
  });
}

// Count calls, including base calls, without executing the result algorithm or
// recursively using the JS call stack. Stop as soon as the declared limit is exceeded.
function skipTakeCalls(length, capacity, weightAt, reuse, limit) {
  const stack = [[length, capacity]];
  let count = 0;
  while (stack.length) {
    const [n, remaining] = stack.pop();
    if (++count > limit) return limit + 1;
    if (n === 0 || remaining === 0) continue;
    stack.push([n - 1, remaining]);
    const weight = weightAt(n - 1);
    if (weight <= remaining) stack.push([reuse ? n : n - 1, remaining - weight]);
  }
  return count;
}

function workLimitRules(inputSpec, lengthRanges) {
  if (!Object.prototype.hasOwnProperty.call(inputSpec ?? {}, 'workLimits')) return [];
  if (!Array.isArray(inputSpec.workLimits)) throw new RangeError('workLimits must be an array.');
  const fields = inputSpec.fields ?? [];
  const scalarNames = new Set();
  const integer = value => Number.isInteger(value) && value >= 0 && value <= 2147483647;
  const fieldNamed = name => {
    const matches = fields.filter(field => field.name === name);
    return typeof name === 'string' && name.trim() && matches.length === 1 ? matches[0] : null;
  };
  return inputSpec.workLimits.map(rule => {
    if (!rule || typeof rule !== 'object' || Array.isArray(rule) ||
        !['CAPACITY_TABLE_CELLS', 'SKIP_TAKE_CALLS'].includes(rule.kind) ||
        !integer(rule.limit) || rule.limit === 0 || typeof rule.reuse !== 'boolean' ||
        (rule.kind === 'CAPACITY_TABLE_CELLS' && rule.reuse) ||
        typeof rule.message !== 'string' || !rule.message.trim() || !fieldNamed(rule.errorField)) {
      throw new RangeError('Invalid workLimits declaration.');
    }
    const array = fieldNamed(rule.arrayField), scalar = fieldNamed(rule.scalarField);
    if (array?.type !== 'INT_ARRAY' || scalar?.type !== 'INT' || scalarNames.has(rule.scalarField)) {
      throw new RangeError('workLimits requires known INT_ARRAY/INT fields and a unique scalar target.');
    }
    scalarNames.add(rule.scalarField);
    const a = array.constraints ?? {}, s = scalar.constraints ?? {};
    if (![a.minLength, a.maxLength, s.min, s.max].every(integer) ||
        a.minLength > a.maxLength || s.min > s.max ||
        (rule.kind === 'SKIP_TAKE_CALLS' &&
          (!integer(a.minValue) || a.minValue === 0 ||
            (Object.prototype.hasOwnProperty.call(a, 'maxValue') &&
              (!integer(a.maxValue) || a.maxValue < a.minValue))))) {
      throw new RangeError('workLimits requires declared length/capacity bounds and positive recursive weights.');
    }
    let range = lengthRanges.find(candidate => candidate.names.includes(array.name));
    const declaredMin = range
      ? Math.max(...range.names.map(name => fieldNamed(name).constraints?.minLength ?? 0))
      : a.minLength;
    if (rule.kind === 'CAPACITY_TABLE_CELLS') {
      const feasibleLength = Math.floor(rule.limit / (s.min + 1)) - 1;
      if (feasibleLength < declaredMin) throw new RangeError('workLimits has no feasible array length.');
      if (!range) {
        range = { names: [array.name], min: a.minLength, max: Math.min(a.maxLength, Math.max(10, a.minLength)) };
        lengthRanges.push(range);
      }
      range.max = Math.min(range.max, feasibleLength);
      range.min = Math.min(range.min, range.max);
    } else {
      const heaviest = Math.max(a.minValue, a.maxValue ?? 50);
      if (skipTakeCalls(declaredMin, s.min, () => heaviest, rule.reuse, rule.limit) > rule.limit) {
        throw new RangeError('workLimits has no feasible recursive input.');
      }
    }
    return { ...rule, scalar };
  });
}

function feasibleCapacity(rule, values) {
  const weights = values[rule.arrayField];
  const { min, max } = rule.scalar.constraints;
  if (rule.kind === 'CAPACITY_TABLE_CELLS') {
    const ceiling = Math.min(max, Math.floor(rule.limit / (weights.length + 1)) - 1);
    if (ceiling < min) throw new RangeError('Generated arrays have no feasible work-limited capacity.');
    return ceiling;
  }
  if (!weights.every(weight => Number.isInteger(weight) && weight > 0)) {
    throw new RangeError('Work-limited recursive weights must be positive integers.');
  }
  const fits = capacity => skipTakeCalls(weights.length, capacity,
    index => weights[index], rule.reuse, rule.limit) <= rule.limit;
  if (!fits(min)) throw new RangeError('Generated arrays have no feasible work-limited capacity.');
  let low = min, high = max;
  while (low < high) {
    const mid = low + Math.ceil((high - low) / 2);
    if (fits(mid)) low = mid;
    else high = mid - 1;
  }
  return low;
}

/** A full { fieldName: value } map for every field in an InputSpec. */
export function randomizeInput(inputSpec, rng = Math.random) {
  const lengthRanges = equalLengthRanges(inputSpec);
  const rules = workLimitRules(inputSpec, lengthRanges);
  const deferred = new Set(rules.map(rule => rule.scalarField));
  const lengths = new Map();
  for (const group of lengthRanges) {
    const length = intBetween(rng, group.min, group.max);
    for (const name of group.names) lengths.set(name, length);
  }
  const values = {};
  for (const field of inputSpec?.fields || []) {
    if (deferred.has(field.name)) continue;
    values[field.name] = lengths.has(field.name)
      ? randomIntList(rng, field, { length: lengths.get(field.name) })
      : randomizeValue(field, rng);
  }
  for (const rule of rules) {
    values[rule.scalarField] = intBetween(rng, rule.scalar.constraints.min, feasibleCapacity(rule, values));
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
