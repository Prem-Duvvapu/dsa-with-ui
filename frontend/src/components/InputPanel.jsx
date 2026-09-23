import layout from './layout.module.css';
import React, { useState, useEffect } from 'react';
import { Play, Shuffle, RotateCcw, GitBranch, Bookmark, X } from 'lucide-react';
import useInputPresets from '../hooks/useInputPresets';
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
 * `alternateInput` is the second input the tracer itself declares — required of every one
 * of them, and materially different from the defaults by contract. It used to exist only
 * for the test suite, which meant the branches only it reaches were greyed out in the code
 * panel with no way to go and take them: next-permutation's swap and suffix reverse are
 * unreachable from any growable default, word-break's memo can never hit on an input that
 * succeeds, and every not-found branch is mutually exclusive with its found branch.
 *
 * Field-level errors come from the server (InputValidator's per-field 400s) via
 * `fieldErrors`, keyed by field name — the same contract useTrace.runInput surfaces.
 * Client-side bounds shown here (min/max on the native inputs, Add/Remove disabling at
 * length caps) are a convenience only; the server remains authoritative.
 *
 * Saved inputs (`useInputPresets`) are what stop a hand-built case from being lost the
 * moment someone navigates away. Loading one runs it immediately, following the "Other
 * case" button's own precedent: the whole point of saving is to remove friction, and
 * loading-without-running would put it straight back.
 */
export default function InputPanel({ problemId, inputSpec, alternateInput, fieldErrors, running, onRun }) {
  const [values, setValues] = useState(() => defaultInput(inputSpec));
  const { presets, savePreset, removePreset } = useInputPresets(problemId);
  const [savingName, setSavingName] = useState(null);

  // A stale value from the previous problem must never appear to belong to this one.
  useEffect(() => {
    setValues(defaultInput(inputSpec));
    setSavingName(null);
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
        {alternateInput && (
          <button
            type="button"
            className="btn btn-outline"
            disabled={running}
            // Loads AND runs: the point is to see the other branch, and making that two
            // clicks is enough friction that most people would never take the second.
            onClick={() => {
              const next = { ...defaultInput(inputSpec), ...alternateInput };
              setValues(next);
              onRun(next);
            }}
            aria-label="Run the other case this problem declares"
            title="A second input chosen to take the branches the default never reaches"
          >
            <GitBranch size={12} /> Other case
          </button>
        )}
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
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => setSavingName('')}
          aria-label="Save this input"
          title="Name this input to come back to it later"
        >
          <Bookmark size={12} /> Save
        </button>
      </div>

      {savingName !== null && (
        <form
          className={styles.saveForm}
          onSubmit={(e) => {
            e.preventDefault();
            savePreset(savingName, values);
            setSavingName(null);
          }}
        >
          <input
            type="text"
            className="ip-input"
            autoFocus
            placeholder="Name this input"
            aria-label="Preset name"
            value={savingName}
            onChange={(e) => setSavingName(e.target.value)}
          />
          <button type="submit" className="btn btn-primary" aria-label="Confirm save">
            Save
          </button>
          <button type="button" className="btn btn-outline" onClick={() => setSavingName(null)}>
            Cancel
          </button>
        </form>
      )}

      {presets.length > 0 && (
        <ul className={styles.presetList} aria-label="Saved inputs">
          {presets.map((preset) => (
            <li key={preset.id} className={styles.presetItem}>
              <button
                type="button"
                className={styles.presetLoad}
                onClick={() => onRun(preset.values)}
                aria-label={`Load ${preset.name}`}
                title={`Run the input saved as "${preset.name}"`}
              >
                {preset.name}
              </button>
              <button
                type="button"
                className={styles.presetRemove}
                onClick={() => removePreset(preset.id)}
                aria-label={`Remove preset ${preset.name}`}
                title="Remove this saved input"
              >
                <X size={11} />
              </button>
            </li>
          ))}
        </ul>
      )}

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
          className={`ip-input ${layout.inputNarrow}`}
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
          className={`ip-input ${layout.inputFull}`}
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
