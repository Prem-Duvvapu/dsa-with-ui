import React, { useState, useEffect } from 'react';
import { Play, Shuffle, RotateCcw } from 'lucide-react';
import IntArrayField from './IntArrayField';
import GridField from './GridField';
import GraphField from './GraphField';
import { randomizeInput, defaultInput } from '../input/randomizeInput';
import styles from './InputPanel.module.css';

/**
 * A form rendered generically from a problem's inputSpec, so a learner runs their own
 * input rather than watching a fixed default. One editor per FieldType — no per-problem
 * form code, ever, or this becomes 433 hand-built forms.
 *
 * Field-level errors come from the server (InputValidator's per-field 400s) via
 * `fieldErrors`, keyed by field name — the same contract useTrace.runInput surfaces.
 * Client-side bounds shown here (min/max on the native inputs, Add/Remove disabling at
 * length caps) are a convenience only; the server remains authoritative.
 */
export default function InputPanel({ problemId, inputSpec, fieldErrors, running, onRun }) {
  const [values, setValues] = useState(() => defaultInput(inputSpec));

  // A stale value from the previous problem must never appear to belong to this one.
  useEffect(() => {
    setValues(defaultInput(inputSpec));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [problemId]);

  if (!inputSpec?.fields?.length) {
    return (
      <div className={styles.emptyState}>
        No editable input for this problem.
      </div>
    );
  }

  const setField = (name, next) => setValues((v) => ({ ...v, [name]: next }));

  return (
    <div className={styles.panel}>
      <div className={styles.actionBar}>
        <button
          type="button"
          className="btn btn-primary"
          disabled={running}
          onClick={() => onRun(values)}
          aria-label="Run with this input"
          style={{ opacity: running ? 0.6 : 1 }}
        >
          <Play size={12} /> Run
        </button>
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => setValues(randomizeInput(inputSpec))}
          aria-label="Randomize input"
        >
          <Shuffle size={12} /> Randomize
        </button>
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => setValues(defaultInput(inputSpec))}
          aria-label="Reset input to default"
        >
          <RotateCcw size={12} /> Reset
        </button>
      </div>

      <div className={styles.fieldsContainer}>
        {inputSpec.fields.map((field) => (
          <div key={field.name}>
            <div className={styles.fieldLabelRow}>
              <label className={styles.fieldLabel}>
                {field.label}
              </label>
            </div>
            {field.help && (
              <p className={styles.fieldHelp}>{field.help}</p>
            )}

            <FieldEditor field={field} value={values[field.name]} onChange={(v) => setField(field.name, v)} />

            {fieldErrors?.[field.name] && (
              <p role="alert" className={styles.fieldError}>
                {fieldErrors[field.name]}
              </p>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

function FieldEditor({ field, value, onChange }) {
  switch (field.type) {
    case 'INT':
      return (
        <input
          type="number"
          className="ip-input"
          style={{ width: '100px' }}
          min={field.constraints?.min}
          max={field.constraints?.max}
          value={value ?? 0}
          onChange={(e) => onChange(e.target.value === '' ? 0 : Number(e.target.value))}
          aria-label={field.label}
        />
      );
    case 'STRING':
      return (
        <input
          type="text"
          className="ip-input"
          style={{ width: '100%' }}
          maxLength={field.constraints?.maxLength}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
          aria-label={field.label}
        />
      );
    case 'INT_ARRAY':
    case 'LINKED_LIST':
      return <IntArrayField field={field} value={value} onChange={onChange} allowNulls={false} />;
    case 'BINARY_TREE':
      return <IntArrayField field={field} value={value} onChange={onChange} allowNulls />;
    case 'INT_GRID':
      return <GridField field={field} value={value} onChange={onChange} />;
    case 'GRAPH':
      return <GraphField field={field} value={value} onChange={onChange} />;
    default:
      return null;
  }
}
