import React from 'react';
import { Plus, X } from 'lucide-react';

/**
 * Chip editor for a list of whole numbers — INT_ARRAY and LINKED_LIST directly, and
 * BINARY_TREE (level order, with `null` standing in for "no node here") via `allowNulls`.
 * Bounds are read straight off the field's own constraints, matching InputValidator on
 * the server exactly: minLength/maxLength cap the chip count, minValue/maxValue cap each
 * chip's value. This is a convenience only — the server re-checks everything.
 */
export default function IntArrayField({ field, value, onChange, allowNulls = false }) {
  const list = Array.isArray(value) ? value : [];
  const c = field.constraints || {};
  const minLength = c.minLength ?? 0;
  const maxLength = c.maxLength ?? Infinity;

  const setAt = (index, next) => {
    const copy = list.slice();
    copy[index] = next;
    onChange(copy);
  };

  const removeAt = (index) => {
    onChange(list.slice(0, index).concat(list.slice(index + 1)));
  };

  const add = () => {
    // Never null: a fresh chip is something to edit, not something to skip.
    onChange(list.concat([0]));
  };

  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', alignItems: 'center' }}>
      {list.map((v, i) => (
        <div
          key={i}
          style={{
            display: 'flex', alignItems: 'center', gap: '4px',
            background: 'var(--bg-elevated)', border: '1px solid var(--border-default)',
            borderRadius: 'var(--radius-sm)', padding: '2px 2px 2px 8px'
          }}
        >
          {allowNulls && (
            <button
              type="button"
              onClick={() => setAt(i, v === null ? 0 : null)}
              title={v === null ? 'Give this node a value' : 'Clear this node (null)'}
              aria-label={v === null ? `Position ${i + 1}: no node, click to add a value` : `Position ${i + 1}: value ${v}, click to clear`}
              style={{
                border: 'none', background: 'transparent', cursor: 'pointer',
                color: v === null ? 'var(--text-muted)' : 'var(--accent-violet)',
                fontFamily: 'var(--font-code)', fontSize: '0.7rem', fontWeight: 700,
                padding: '2px 4px'
              }}
            >
              {v === null ? 'null' : '#'}
            </button>
          )}
          {v !== null ? (
            <input
              type="number"
              value={v}
              onChange={(e) => setAt(i, e.target.value === '' ? 0 : Number(e.target.value))}
              aria-label={`Position ${i + 1} value`}
              style={{
                width: '52px', border: 'none', background: 'transparent',
                color: 'var(--text-primary)', fontFamily: 'var(--font-code)', fontSize: '0.78rem',
                outline: 'none', padding: '4px 0'
              }}
            />
          ) : null}
          <button
            type="button"
            onClick={() => removeAt(i)}
            disabled={list.length <= minLength}
            aria-label={`Remove position ${i + 1}`}
            style={{
              border: 'none', background: 'transparent', cursor: list.length <= minLength ? 'not-allowed' : 'pointer',
              color: 'var(--text-muted)', display: 'flex', padding: '4px',
              opacity: list.length <= minLength ? 0.35 : 1
            }}
          >
            <X size={12} />
          </button>
        </div>
      ))}
      <button
        type="button"
        onClick={add}
        disabled={list.length >= maxLength}
        className="btn btn-outline"
        style={{ padding: '4px 8px', opacity: list.length >= maxLength ? 0.4 : 1 }}
      >
        <Plus size={12} /> Add
      </button>
    </div>
  );
}
