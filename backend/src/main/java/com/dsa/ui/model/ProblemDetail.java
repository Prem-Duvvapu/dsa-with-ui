package com.dsa.ui.model;

import java.util.List;

public class ProblemDetail {
    private String id;
    private String title;
    private String striverSheetSection;
    private String category; // "Graphs", "Binary Trees", "BST", "Sorting Algorithms", "Arrays", "Linked List", "Binary Search", "Dynamic Programming", "Tries & Prefixes", ...
    private String difficulty; // "Easy", "Medium", "Hard"
    private String description;
    private String javaCode;
    private List<GraphNode> defaultGraphNodes;
    private List<GraphEdge> defaultGraphEdges;
    private List<TreeNode> defaultTreeNodes;
    private List<ArrayElement> defaultArray;
    private List<ListNode> defaultList;
    private List<TrieNodeModel> defaultTrie;
    private int[][] defaultGrid;
    private ComplexityDetail complexity;
    /**
     * The ORIGINAL problem's constraints, as the source (LeetCode/GFG) states them -
     * e.g. "1 <= nums.length <= 10^5". Deliberately NOT the visualiser's own input limits,
     * which live in InputSpec's field constraints and exist to protect the step budget.
     * The two differ by orders of magnitude, so they are kept apart and labelled apart.
     *
     * <p>Null or empty means "not recorded yet", and the UI omits the section rather than
     * inventing a plausible bound - the same honesty rule the catalogue applies to missing
     * complexity analysis. A wrong constraint is worse than an absent one.
     */
    private List<String> constraints;
    private DsType dsType;

    public ProblemDetail() {}

    public ProblemDetail(String id, String title, String striverSheetSection, String category, String difficulty,
                         String description, String javaCode, List<GraphNode> defaultGraphNodes,
                         List<GraphEdge> defaultGraphEdges, List<TreeNode> defaultTreeNodes, int[][] defaultGrid,
                         ComplexityDetail complexity, String dsType) {
        this(id, title, striverSheetSection, category, difficulty, description, javaCode, defaultGraphNodes, defaultGraphEdges, defaultTreeNodes, null, null, null, defaultGrid, complexity, dsType);
    }

    public ProblemDetail(String id, String title, String striverSheetSection, String category, String difficulty,
                         String description, String javaCode, List<GraphNode> defaultGraphNodes,
                         List<GraphEdge> defaultGraphEdges, List<TreeNode> defaultTreeNodes,
                         List<ArrayElement> defaultArray, List<ListNode> defaultList, List<TrieNodeModel> defaultTrie,
                         int[][] defaultGrid, ComplexityDetail complexity, String dsType) {
        this.id = id;
        this.title = title;
        this.striverSheetSection = striverSheetSection;
        this.category = category;
        this.difficulty = difficulty;
        this.description = description;
        this.javaCode = javaCode;
        this.defaultGraphNodes = defaultGraphNodes;
        this.defaultGraphEdges = defaultGraphEdges;
        this.defaultTreeNodes = defaultTreeNodes;
        this.defaultArray = defaultArray;
        this.defaultList = defaultList;
        this.defaultTrie = defaultTrie;
        this.defaultGrid = defaultGrid;
        this.complexity = complexity;
        this.dsType = DsType.fromWireValue(dsType);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStriverSheetSection() { return striverSheetSection; }
    public void setStriverSheetSection(String striverSheetSection) { this.striverSheetSection = striverSheetSection; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJavaCode() { return javaCode; }
    public void setJavaCode(String javaCode) { this.javaCode = javaCode; }

    public List<GraphNode> getDefaultGraphNodes() { return defaultGraphNodes; }
    public void setDefaultGraphNodes(List<GraphNode> defaultGraphNodes) { this.defaultGraphNodes = defaultGraphNodes; }

    public List<GraphEdge> getDefaultGraphEdges() { return defaultGraphEdges; }
    public void setDefaultGraphEdges(List<GraphEdge> defaultGraphEdges) { this.defaultGraphEdges = defaultGraphEdges; }

    public List<TreeNode> getDefaultTreeNodes() { return defaultTreeNodes; }
    public void setDefaultTreeNodes(List<TreeNode> defaultTreeNodes) { this.defaultTreeNodes = defaultTreeNodes; }

    public List<ArrayElement> getDefaultArray() { return defaultArray; }
    public void setDefaultArray(List<ArrayElement> defaultArray) { this.defaultArray = defaultArray; }

    public List<ListNode> getDefaultList() { return defaultList; }
    public void setDefaultList(List<ListNode> defaultList) { this.defaultList = defaultList; }

    public List<TrieNodeModel> getDefaultTrie() { return defaultTrie; }
    public void setDefaultTrie(List<TrieNodeModel> defaultTrie) { this.defaultTrie = defaultTrie; }

    public int[][] getDefaultGrid() { return defaultGrid; }
    public void setDefaultGrid(int[][] defaultGrid) { this.defaultGrid = defaultGrid; }

    public ComplexityDetail getComplexity() { return complexity; }
    public void setComplexity(ComplexityDetail complexity) { this.complexity = complexity; }

    public List<String> getConstraints() { return constraints; }
    public void setConstraints(List<String> constraints) { this.constraints = constraints; }

    public DsType getDsType() { return dsType; }
    public void setDsType(DsType dsType) { this.dsType = dsType; }
}
