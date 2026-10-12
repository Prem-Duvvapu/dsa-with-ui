/**
 * Advisory only: explain declared size/value limits for a preserved draft. This is not
 * InputValidator: missing/type/grammar/cross-field constraints are still the server's.
 * No warnings is deliberately NOT a promise that a submission will be accepted.
 */
export function inputLimitWarnings(inputSpec, values) {
  const warnings = [];
  for (const field of inputSpec?.fields ?? []) {
    const value = values?.[field.name];
    const c = field.constraints ?? {};
    const label = field.label ?? field.name;
    const add = message => warnings.push({ field: field.name, message });
    const size = (actual, min, max, unit) => {
      const noun = actual === 1 ? unit.slice(0, -1) : unit;
      if (Number.isFinite(max) && actual > max) add(`${label} has ${actual} ${noun}; maximum ${max}.`);
      else if (Number.isFinite(min) && actual < min) add(`${label} has ${actual} ${noun}; minimum ${min}.`);
    };
    const numbers = (items, min, max) => {
      if (Number.isFinite(min) && items.some(v => typeof v === 'number' && v < min))
        add(`${label} contains values below ${min}.`);
      if (Number.isFinite(max) && items.some(v => typeof v === 'number' && v > max))
        add(`${label} contains values above ${max}.`);
    };
    if (field.type === 'INT' && typeof value === 'number' && Number.isFinite(value)) {
      if (Number.isFinite(c.min) && value < c.min) add(`${label} is ${value}; minimum ${c.min}.`);
      if (Number.isFinite(c.max) && value > c.max) add(`${label} is ${value}; maximum ${c.max}.`);
    } else if (field.type === 'STRING' && typeof value === 'string') {
      size(value.length, c.minLength, c.maxLength, 'characters');
    } else if (['INT_ARRAY', 'LINKED_LIST', 'BINARY_TREE'].includes(field.type) && Array.isArray(value)) {
      size(value.length, c.minLength, c.maxLength, 'values');
      numbers(value, c.minValue, c.maxValue);
    } else if (field.type === 'INT_GRID' && Array.isArray(value)) {
      size(value.length, c.minRows, c.maxRows, 'rows');
      const rows = value.filter(Array.isArray);
      if (rows.length > 0) {
        const widths = rows.map(row => row.length);
        if (Number.isFinite(c.maxCols) && Math.max(...widths) > c.maxCols)
          add(`${label} has a row with ${Math.max(...widths)} ${Math.max(...widths) === 1 ? 'column' : 'columns'}; maximum ${c.maxCols}.`);
        if (Number.isFinite(c.minCols) && Math.min(...widths) < c.minCols)
          add(`${label} has a row with ${Math.min(...widths)} ${Math.min(...widths) === 1 ? 'column' : 'columns'}; minimum ${c.minCols}.`);
        numbers(rows.flat(), c.minValue, c.maxValue);
      }
    }
  }
  return warnings;
}
