package com.dsa.ui.tracer;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.GraphEdge;
import com.dsa.ui.model.GraphNode;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.model.TrieNodeModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a tracer writes its execution log to.
 *
 * <p>Steps are built fluently and named by code anchor, never by line number:
 *
 * <pre>
 * emit.at("loop.compare")
 *     .say("i = %d: sum %d beats maxi %d", i, sum, maxi)
 *     .var("i", i).var("sum", sum)
 *     .array(nums, i)
 *     .step();
 * </pre>
 *
 * <p>The emitter enforces the step budget, so a tracer never has to think about it.
 */
public final class StepEmitter {

    private final AnnotatedCode code;
    private final int maxSteps;
    private final long maxBytes;
    private final List<ExecutionStep> steps = new ArrayList<>();
    private final List<String> callStack = new ArrayList<>();
    private final DsType dsType;
    private Inputs.GraphInput laidOutGraph;
    private List<GraphNode> laidOutGraphNodes;
    private List<GraphEdge> laidOutGraphEdges;
    private long bytesSoFar;

    StepEmitter(AnnotatedCode code, int maxSteps, long maxBytes, DsType dsType) {
        this.code = code;
        this.maxSteps = maxSteps;
        this.maxBytes = maxBytes;
        this.dsType = dsType;
    }

    public Step at(String anchor) {
        return new Step(code.resolve(anchor));
    }

    /** Pushes a frame onto the call stack shown alongside recursive traces. */
    public void push(String frame) {
        callStack.add(frame);
    }

    public void pop() {
        if (!callStack.isEmpty()) {
            callStack.remove(callStack.size() - 1);
        }
    }

    public int count() {
        return steps.size();
    }

    List<ExecutionStep> collected() {
        return steps;
    }

    /** One step under construction. */
    public final class Step {
        private final int line;
        private String description = "";
        private final Map<String, String> variables = new LinkedHashMap<>();
        private List<ArrayElement> arrayState;
        private int[][] gridState;
        private List<ListNode> listState;
        private List<String> queueOrStackState;
        private List<TrieNodeModel> trieState;
        private List<TreeNode> treeNodes;
        private List<GraphNode> graphNodes;
        private List<GraphEdge> graphEdges;
        private DpTable dpTable;
        private Map<Integer, String> nodeStates;
        private List<String> activeEdges;

        private Step(int line) {
            this.line = line;
        }

        /** The human-readable narration. Accepts {@link String#format} arguments. */
        public Step say(String format, Object... args) {
            this.description = args.length == 0 ? format : String.format(format, args);
            return this;
        }

        public Step var(String name, Object value) {
            variables.put(name, String.valueOf(value));
            return this;
        }

        /** Highlights up to two positions in the array being traced. */
        public Step array(int[] values, int primary, int secondary) {
            List<ArrayElement> state = new ArrayList<>(values.length);
            for (int i = 0; i < values.length; i++) {
                String s = i == primary ? "current" : i == secondary ? "target" : "default";
                state.add(new ArrayElement(i, values[i], s));
            }
            this.arrayState = state;
            return this;
        }

        public Step array(int[] values, int primary) {
            return array(values, primary, -1);
        }

        public Step array(int[] values) {
            return array(values, -1, -1);
        }

        /** A character track whose labels preserve the actual Unicode characters. */
        public Step chars(String value, int primary, int secondary) {
            int[] codePoints = value.codePoints().toArray();
            List<ArrayElement> state = new ArrayList<>(codePoints.length);
            for (int i = 0; i < codePoints.length; i++) {
                String elementState = i == primary
                        ? "current"
                        : i == secondary ? "target" : "default";
                state.add(new ArrayElement(
                        i,
                        codePoints[i],
                        elementState,
                        new String(Character.toChars(codePoints[i]))));
            }
            this.arrayState = state;
            return this;
        }

        public Step chars(String value, int primary) {
            return chars(value, primary, -1);
        }

        public Step chars(String value) {
            return chars(value, -1, -1);
        }

        /** A 32-bit track ordered from the most-significant bit to the least-significant. */
        public Step bits(int value, int primaryBit, int secondaryBit) {
            List<ArrayElement> state = new ArrayList<>(Integer.SIZE);
            for (int bit = Integer.SIZE - 1; bit >= 0; bit--) {
                String elementState = bit == primaryBit
                        ? "current"
                        : bit == secondaryBit ? "target" : "default";
                state.add(new ArrayElement(
                        bit,
                        (value >>> bit) & 1,
                        elementState,
                        String.valueOf(bit)));
            }
            this.arrayState = state;
            return this;
        }

        public Step bits(int value, int primaryBit) {
            return bits(value, primaryBit, -1);
        }

        public Step bits(int value) {
            return bits(value, -1, -1);
        }

        /** Full control over per-index states when the four defaults are not enough. */
        public Step arrayState(List<ArrayElement> state) {
            this.arrayState = state;
            return this;
        }

        public Step grid(int[][] values) {
            int[][] copy = new int[values.length][];
            for (int r = 0; r < values.length; r++) {
                copy[r] = values[r].clone();
            }
            this.gridState = copy;
            return this;
        }

        public Step list(List<ListNode> nodes) {
            this.listState = nodes;
            return this;
        }

        /** Carries the algorithm's own stack, independently of recursive call frames. */
        public Step stack(Iterable<?> values) {
            this.queueOrStackState = stringSnapshot(values);
            return this;
        }

        /** Carries the algorithm's queue in the iteration order supplied by the tracer. */
        public Step queue(Iterable<?> values) {
            this.queueOrStackState = stringSnapshot(values);
            return this;
        }

        public Step trie(List<TrieNodeModel> nodes) {
            this.trieState = nodes.stream()
                    .map(StepEmitter::snapshotTrieNode)
                    .toList();
            return this;
        }

        public Step tree(List<TreeNode> nodes) {
            this.treeNodes = nodes;
            return this;
        }

        /**
         * Carries the caller-supplied graph topology with this step.
         *
         * <p>The layout is deterministic and depends only on the validated input. Node
         * colour remains in {@link #nodes(Map)} so topology and algorithm state can delta
         * independently on the wire.
         */
        public Step graph(Inputs.GraphInput graph) {
            if (laidOutGraph == graph) {
                this.graphNodes = laidOutGraphNodes;
                this.graphEdges = laidOutGraphEdges;
                return this;
            }

            List<GraphNode> nodes = new ArrayList<>(graph.vertices());
            double centerX = 180;
            double centerY = 160;
            double radius = graph.vertices() <= 2 ? 80 : 120;
            for (int id = 0; id < graph.vertices(); id++) {
                double angle = -Math.PI / 2 + (2 * Math.PI * id / graph.vertices());
                double x = graph.vertices() == 1 ? centerX : centerX + radius * Math.cos(angle);
                double y = graph.vertices() == 1 ? centerY : centerY + radius * Math.sin(angle);
                nodes.add(new GraphNode(id, String.valueOf(id), x, y, "unvisited"));
            }

            List<GraphEdge> edges = new ArrayList<>(graph.edges().length);
            for (int[] edge : graph.edges()) {
                Integer weight = edge.length == 3 ? edge[2] : null;
                edges.add(new GraphEdge(edge[0], edge[1], weight, false, false));
            }
            laidOutGraph = graph;
            laidOutGraphNodes = List.copyOf(nodes);
            laidOutGraphEdges = List.copyOf(edges);
            this.graphNodes = laidOutGraphNodes;
            this.graphEdges = laidOutGraphEdges;
            return this;
        }

        /** Carries an already-laid-out graph topology with this step. */
        public Step graph(List<GraphNode> nodes, List<GraphEdge> edges) {
            this.graphNodes = nodes.stream()
                    .map(StepEmitter::snapshotGraphNode)
                    .toList();
            this.graphEdges = edges.stream()
                    .map(StepEmitter::snapshotGraphEdge)
                    .toList();
            return this;
        }

        /** Carries a labelled DP table with per-cell Bench state. */
        public Step dpTable(DpTable table) {
            this.dpTable = table;
            return this;
        }

        public Step nodes(Map<Integer, String> states) {
            this.nodeStates = new LinkedHashMap<>(states);
            return this;
        }

        public Step edges(List<String> active) {
            this.activeEdges = List.copyOf(active);
            return this;
        }

        /** Commits the step. Throws once either budget is spent. */
        public void step() {
            if (steps.size() >= maxSteps) {
                throw TraceBudgetExceededException.steps(maxSteps);
            }
            ExecutionStep committed = new ExecutionStep(
                    steps.size() + 1,
                    line,
                    description,
                    queueOrStackState,
                    nodeStates == null ? Map.of() : nodeStates,
                    activeEdges == null ? List.of() : activeEdges,
                    new LinkedHashMap<>(variables),
                    dsType,
                    gridState,
                    arrayState,
                    listState,
                    trieState,
                    treeNodes,
                    graphNodes,
                    graphEdges,
                    dpTable,
                    List.copyOf(callStack)
            );

            // Checked BEFORE adding, so a collected trace is always within budget.
            long cost = estimateBytes(committed);
            if (bytesSoFar + cost > maxBytes) {
                throw TraceBudgetExceededException.bytes(maxBytes, steps.size());
            }
            bytesSoFar += cost;
            steps.add(committed);
        }
    }

    /**
     * Roughly what this step will weigh once serialised to JSON.
     *
     * <p>An estimate rather than a measurement on purpose: serialising every step to find
     * its true size would cost more than generating it. The constants are per-element
     * costs calibrated against real responses, and {@code byteEstimateTracksActualPayload}
     * keeps them honest — it serialises each tracer's real trace and fails if the estimate
     * drifts away from the measured size.
     *
     * <p>It leans high. Under-estimating would let a trace past the ceiling it exists to
     * enforce, which is the failure that matters.
     */
    static long estimateBytes(ExecutionStep s) {
        long bytes = 190;                                    // envelope: field names, numbers, dsType, nulls

        if (s.getDescription() != null) {
            bytes += jsonStringBytes(s.getDescription());
        }
        if (s.getVariables() != null) {
            for (Map.Entry<String, String> e : s.getVariables().entrySet()) {
                bytes += jsonStringBytes(e.getKey()) + jsonStringBytes(e.getValue()) + 8;
            }
        }
        if (s.getArrayState() != null) {
            bytes += s.getArrayState().size() * 52L;         // {"value":..,"state":"..","index":..}
            for (ArrayElement element : s.getArrayState()) {
                if (element.getLabel() != null) {
                    bytes += jsonStringBytes(element.getLabel()) + 11L; // ,"label":"..."
                }
            }
        }
        if (s.getGridState() != null) {
            for (int[] row : s.getGridState()) {
                bytes += row.length * 4L + 4;
            }
        }
        if (s.getListState() != null) {
            bytes += s.getListState().size() * 88L;
        }
        if (s.getTrieState() != null) {
            for (TrieNodeModel node : s.getTrieState()) {
                bytes += 104L;
                if (node.getCharacter() != null) {
                    bytes += jsonStringBytes(node.getCharacter());
                }
                if (node.getState() != null) {
                    bytes += jsonStringBytes(node.getState());
                }
                if (node.getChildren() != null) {
                    for (Map.Entry<String, Integer> child : node.getChildren().entrySet()) {
                        bytes += jsonStringBytes(child.getKey()) + 16L;
                    }
                }
            }
        }
        if (s.getTreeNodes() != null) {
            bytes += s.getTreeNodes().size() * 104L;
        }
        if (s.getGraphNodes() != null) {
            bytes += s.getGraphNodes().size() * 96L;
        }
        if (s.getGraphEdges() != null) {
            bytes += s.getGraphEdges().size() * 72L;
        }
        if (s.getDpTable() != null) {
            DpTable table = s.getDpTable();
            bytes += 36;                                   // field and table envelopes
            for (String label : table.rowLabels()) {
                bytes += jsonStringBytes(label) + 4L;
            }
            for (String label : table.colLabels()) {
                bytes += jsonStringBytes(label) + 4L;
            }
            for (List<DpCell> row : table.cells()) {
                bytes += 2;
                for (DpCell cell : row) {
                    bytes += 24L
                            + jsonStringBytes(cell.value())
                            + jsonStringBytes(cell.state());
                }
            }
        }
        if (s.getNodeStates() != null) {
            bytes += s.getNodeStates().size() * 24L;
        }
        for (List<String> strings : List.of(
                s.getActiveEdges() == null ? List.<String>of() : s.getActiveEdges(),
                s.getQueueOrStackState() == null ? List.<String>of() : s.getQueueOrStackState(),
                s.getCallStack() == null ? List.<String>of() : s.getCallStack())) {
            for (String value : strings) {
                bytes += jsonStringBytes(value) + 4;
            }
        }
        return bytes;
    }

    private static List<String> stringSnapshot(Iterable<?> values) {
        List<String> snapshot = new ArrayList<>();
        for (Object value : values) {
            snapshot.add(String.valueOf(value));
        }
        return List.copyOf(snapshot);
    }

    private static TrieNodeModel snapshotTrieNode(TrieNodeModel node) {
        Map<String, Integer> children = node.getChildren() == null
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(node.getChildren()));
        return new TrieNodeModel(
                node.getId(),
                node.getCharacter(),
                node.isEndOfWord(),
                node.getX(),
                node.getY(),
                children,
                node.getState());
    }

    private static GraphNode snapshotGraphNode(GraphNode node) {
        return new GraphNode(
                node.getId(),
                node.getLabel(),
                node.getX(),
                node.getY(),
                node.getState());
    }

    private static GraphEdge snapshotGraphEdge(GraphEdge edge) {
        return new GraphEdge(
                edge.getFrom(),
                edge.getTo(),
                edge.getWeight(),
                edge.isDirected(),
                edge.isHighlighted());
    }

    /**
     * Conservative byte cost for a String's contents in JSON, excluding its two quotes.
     * Quotes, backslashes and control characters grow when escaped. Non-ASCII characters
     * are charged as {@code \\uXXXX} escapes (one per UTF-16 unit): some JSON writers emit
     * shorter raw UTF-8, but the response budget must remain safe under either policy.
     */
    private static long jsonStringBytes(String value) {
        if (value == null) {
            return 4; // JSON null
        }

        long bytes = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);

            if (codePoint == '"' || codePoint == '\\'
                    || codePoint == '\b' || codePoint == '\f'
                    || codePoint == '\n' || codePoint == '\r' || codePoint == '\t') {
                bytes += 2;
            } else if (codePoint < 0x20) {
                bytes += 6; // \\u00xx
            } else if (codePoint <= 0x7f) {
                bytes += 1;
            } else {
                bytes += Character.charCount(codePoint) * 6L;
            }
        }
        return bytes;
    }
}
