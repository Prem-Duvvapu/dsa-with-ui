import fields from './fields.module.css';
import React from 'react';
import { Plus, X } from 'lucide-react';

/**
 * Vertex count plus an edge table for GRAPH. Matches the wire shape exactly:
 * {vertices, edges: [[from,to]]}, or [[from,to,weight]] when the spec is weighted.
 */
export default function GraphField({ field, value, onChange }) {
  const vertices = value?.vertices ?? 1;
  const edges = Array.isArray(value?.edges) ? value.edges : [];
  const c = field.constraints || {};
  const maxVertices = c.maxVertices ?? Infinity;
  const maxEdges = c.maxEdges ?? Infinity;
  const weighted = c.weighted === true;

  const setVertices = (n) => {
    const clamped = Math.max(1, Math.min(maxVertices, n));
    onChange({ vertices: clamped, edges });
  };

  const setEdge = (index, position, n) => {
    const next = edges.map((e) => e.slice());
    next[index][position] = n;
    onChange({ vertices, edges: next });
  };

  const removeEdge = (index) => {
    onChange({ vertices, edges: edges.slice(0, index).concat(edges.slice(index + 1)) });
  };

  const addEdge = () => {
    if (edges.length >= maxEdges) return;
    const fresh = weighted ? [0, 0, 1] : [0, 0];
    onChange({ vertices, edges: edges.concat([fresh]) });
  };

  return (
    <div className={fields.stack}>
      <label className={fields.label}>
        Vertices
        <input
          type="number"
          min={1}
          max={maxVertices === Infinity ? undefined : maxVertices}
          value={vertices}
          onChange={(e) => setVertices(Number(e.target.value) || 1)}
          className={fields.countInput}
        />
      </label>

      <div className={fields.stackTight}>
        {edges.map((edge, i) => (
          <div key={i} className={fields.row}>
            <span className={fields.ordinal}>{i + 1}</span>
            <input
              type="number" min={0} max={Math.max(0, vertices - 1)}
              value={edge[0]}
              onChange={(ev) => setEdge(i, 0, Number(ev.target.value) || 0)}
              aria-label={`Edge ${i + 1} from vertex`}
              className={`ip-input ${fields.numberInput}`}
            />
            <span className={fields.arrow}>→</span>
            <input
              type="number" min={0} max={Math.max(0, vertices - 1)}
              value={edge[1]}
              onChange={(ev) => setEdge(i, 1, Number(ev.target.value) || 0)}
              aria-label={`Edge ${i + 1} to vertex`}
              className={`ip-input ${fields.numberInput}`}
            />
            {weighted && (
              <input
                type="number"
                value={edge[2] ?? 1}
                onChange={(ev) => setEdge(i, 2, Number(ev.target.value) || 0)}
                aria-label={`Edge ${i + 1} weight`}
                title="weight"
                className={`ip-input ${fields.numberInput}`}
              />
            )}
            <button
              type="button"
              onClick={() => removeEdge(i)}
              aria-label={`Remove edge ${i + 1}`}
              className={fields.iconButton}
            >
              <X size={12} />
            </button>
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={addEdge}
        disabled={edges.length >= maxEdges}
        className={`btn btn-outline ${fields.addButton}${edges.length >= maxEdges ? ` ${fields.atCap}` : ''}`}
      >
        <Plus size={12} /> Add edge
      </button>
    </div>
  );
}
