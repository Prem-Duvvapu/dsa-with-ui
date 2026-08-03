package com.dsa.ui.model;

import java.util.List;

public class ProblemDetail {
    private String id;
    private String title;
    private String striverSheetSection;
    private String category; // "Graph BFS/DFS", "Advanced Graphs", "Binary Trees", "Binary Search Trees", "Sorting Algorithms", "Arrays", "Linked List", "Binary Search", "Dynamic Programming", "Tries"
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
    private String dsType; // "Queue" or "Stack" or "PriorityQueue" or "Matrix" or "Array" or "LinkedList" or "Trie"

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
        this.dsType = dsType;
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

    public String getDsType() { return dsType; }
    public void setDsType(String dsType) { this.dsType = dsType; }
}
