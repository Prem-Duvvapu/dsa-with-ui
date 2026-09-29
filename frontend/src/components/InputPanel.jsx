import layout from './layout.module.css';
import React, { useState, useEffect, useId, useRef } from 'react';
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
 * Controlled: `values` is the problem session's draft and `onChange` replaces it. The
 * editor used to own its draft, so closing it threw the edits away, a spec arriving after
 * mount was never applied, and a loaded preset ran without the fields showing it. Every
 * action here edits the draft, and the two load-and-run actions (Other case, a saved
 * input) edit it AND run the very same values, so the fields always show what ran.
 *
 * `alternateInput` is the second input the tracer itself declares — required of every one
 * of them, and materially different from the defaults by contract. It used to exist only
 * for the test suite, which meant the branches only it reaches were greyed out in the code
 * panel with no way to go and take them: next-permutation's swap and suffix reverse are
 * unreachable from any growable default, word-break's memo can never hit on an input that
 * succeeds, and every not-found branch is mutually exclusive with its found branch.
 *
 * Field-level errors come from the server (InputValidator's per-field 400s) via
 * `fieldErrors`, keyed by field name — the same contract useTrace.runInput surfaces. They
 * are summarised at the top, and focus moves to that summary when a submission is rejected
 * so a keyboard or screen-reader user learns what to fix without hunting for it. Client-side
 * bounds shown here (min/max on the native inputs, Add/Remove disabling at length caps) are
 * a convenience only; the server remains authoritative.
 *
 * Saved inputs (`useInputPresets`) are what stop a hand-built case from being lost the
 * moment someone navigates away. Loading one runs it immediately, following the "Other
 * case" button's own precedent: the whole point of saving is to remove friction, and
 * loading-without-running would put it straight back.
 */
export default function InputPanel({ problemId, inputSpec, alternateInput, fieldErrors, running, values, onChange, onRun, draftChanged = false }) {
  const { presets, savePreset, removePreset } = useInputPresets(problemId);
  const [savingName, setSavingName] = useState(null);
  const summaryRef = useRef(null);
  const summaryId = useId();
  const draft = values ?? {};

  // A half-typed preset name belongs to the problem it was typed on.
  useEffect(() => { setSavingName(null); }, [problemId]);

  const errorEntries = Object.entries(fieldErrors ?? {});
  useEffect(() => {
    if (errorEntries.length > 0) summaryRef.current?.focus();
    // Keyed on the error object's identity: each rejected submission produces a new one.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fieldErrors]);

  if (!inputSpec?.fields?.length) {
    return (
      <div className={styles.emptyState}>
        No editable input for this problem.
      </div>
    );
  }

  const setField = (name, next) => onChange({ ...draft, [name]: next });
  const loadAndRun = (next) => {
    onChange(next);
    onRun(next);
  };
  const labelFor = (name) => inputSpec.fields.find((f) => f.name === name)?.label ?? name;

  return (
    <div className={styles.panel}>
      {errorEntries.length > 0 && (
        <div
          ref={summaryRef}
          role="alert"
          tabIndex={-1}
          aria-labelledby={summaryId}
          className={styles.errorSummary}
        >
          <p id={summaryId} className={styles.errorSummaryTitle}>This input could not run</p>
          <ul className={styles.errorSummaryList}>
            {errorEntries.map(([name, message]) => (
              <li key={name}>{labelFor(name)}: {message}</li>
            ))}
          </ul>
        </div>
      )}

      <div className={styles.actionBar}>
        <button
          type="button"
          className="btn btn-primary"
          disabled={running}
          onClick={() => onRun(draft)}
          aria-label="Run with this input"
          style={{ opacity: running ? 0.6 : 1 }}
        >
          <Play size={12} /> {running ? 'Running…' : 'Run'}
        </button>
        {alternateInput && (
          <button
            type="button"
            className="btn btn-outline"
            disabled={running}
            // Loads AND runs: the point is to see the other branch, and making that two
            // clicks is enough friction that most people would never take the second.
            onClick={() => loadAndRun({ ...defaultInput(inputSpec), ...alternateInput })}
            aria-label="Run the other case this problem declares"
            title="A second input chosen to take the branches the default never reaches"
          >
            <GitBranch size={12} /> Other case
          </button>
        )}
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => onChange(randomizeInput(inputSpec))}
          aria-label="Randomize input"
        >
          <Shuffle size={12} /> Randomize
        </button>
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => onChange(defaultInput(inputSpec))}
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
        {draftChanged && <span className={styles.draftChanged}>Changes not run</span>}
      </div>

      {savingName !== null && (
        <form
          className={styles.saveForm}
          onSubmit={(e) => {
            e.preventDefault();
            savePreset(savingName, draft);
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
                disabled={running}
                onClick={() => loadAndRun(preset.values)}
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

            <FieldEditor field={field} value={draft[field.name]} onChange={(v) => setField(field.name, v)} />

            {fieldErrors?.[field.name] && (
              <p className={styles.fieldError}>
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
