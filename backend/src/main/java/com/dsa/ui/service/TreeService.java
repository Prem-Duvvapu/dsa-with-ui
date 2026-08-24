package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TreeService implements ProblemProvider {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public TreeService() {
        initProblems();
    }

    public List<ProblemDetail> getAllProblems() {
        return new ArrayList<>(problems.values());
    }

    public ProblemDetail getProblemById(String id) {
        return problems.get(id);
    }

    public List<ExecutionStep> generateSteps(String problemId) {
        switch (problemId) {
            // tree-preorder, tree-inorder, tree-postorder and tree-level-order have real
            // tracers now (tracer/impl). Their generators are gone; refusing loudly beats
            // falling into default: and serving another tree's animation. The rest of
            // this switch dies with PROMPT D.
            case "tree-preorder":
            case "tree-inorder":
            case "tree-postorder":
            case "tree-level-order":
                throw new LegacyTraceRetiredException(problemId);
            case "tree-height": return generatePreorderSteps();
            case "tree-balanced": return generatePreorderSteps();
            case "tree-diameter": return generatePreorderSteps();
            case "tree-max-path-sum": return generatePreorderSteps();
            case "tree-lca": return generatePreorderSteps();
            case "tree-burn-time": return generatePreorderSteps();
            case "bst-search": return generatePreorderSteps();
            case "bst-validate": return generatePreorderSteps();
            case "bst-kth-smallest": return generatePreorderSteps();
            default: return generatePreorderSteps();
        }
    }

    private void initProblems() {
        // 1. Binary Tree Preorder Traversal
        problems.put("tree-preorder", new ProblemDetail(
            "tree-preorder", "Preorder Traversal of Binary Tree", "Binary Trees - Traversals", "Binary Trees", "Easy",
            "Perform Preorder Traversal (Root -> Left -> Right) on a Binary Tree.",
            """
            // Java Preorder Traversal (Striver A2Z Sheet)
            public List<Integer> preorderTraversal(TreeNode root) {
                List<Integer> preorder = new ArrayList<>();
                preorderHelper(root, preorder);
                return preorder;
            }

            private void preorderHelper(TreeNode node, List<Integer> preorder) {
                if (node == null) return;
                preorder.add(node.val);             // 1. Visit Root
                preorderHelper(node.left, preorder); // 2. Traverse Left
                preorderHelper(node.right, preorder);// 3. Traverse Right
            }
            """,
            null, null, createDefaultTreeNodes(), null,
            new ComplexityDetail("O(N)", "Time Complexity: Every node in binary tree visited once.", "Preorder Traversal", "O(H)", "Space Complexity: Recursion stack height H.", "Call Stack", "Auxiliary Space: O(H)", "Memory"), "Stack"
        ));

        // 2. Binary Tree Inorder Traversal
        problems.put("tree-inorder", new ProblemDetail(
            "tree-inorder", "Inorder Traversal of Binary Tree", "Binary Trees - Traversals", "Binary Trees", "Easy",
            "Perform Inorder Traversal (Left -> Root -> Right) on a Binary Tree.",
            """
            // Java Inorder Traversal (Striver A2Z Sheet)
            public List<Integer> inorderTraversal(TreeNode root) {
                List<Integer> inorder = new ArrayList<>();
                inorderHelper(root, inorder);
                return inorder;
            }

            private void inorderHelper(TreeNode node, List<Integer> inorder) {
                if (node == null) return;
                inorderHelper(node.left, inorder);   // 1. Traverse Left
                inorder.add(node.val);               // 2. Visit Root
                inorderHelper(node.right, inorder);  // 3. Traverse Right
            }
            """,
            null, null, createDefaultTreeNodes(), null,
            new ComplexityDetail("O(N)", "Time Complexity: Single pass iteration over N tree nodes.", "Inorder Traversal", "O(H)", "Space Complexity: Recursion call stack height H.", "Call Stack", "Auxiliary Space: O(H)", "Memory"), "Stack"
        ));

        // Bulk register remaining 52 Tree & BST problems
        populateRemainingTreeProblems();
    }

    private void populateRemainingTreeProblems() {
        String[][] list = new String[][]{
            {"tree-intro", "Introduction to Trees", "Binary Trees - Traversals", "Easy", "Tree structure terminology (Root, Parent, Child, Leaf, Height)."},
            {"tree-rep-java", "Binary Tree Representation in Java", "Binary Trees - Traversals", "Easy", "Class TreeNode with val, left, and right pointers."},
            {"pre-post-in-one-traversal", "Pre, Post, Inorder in One Traversal", "Binary Trees - Traversals", "Medium", "Single pass state stack for Pre, In, Post traversals."},
            {"tree-postorder", "Postorder Traversal of Binary Tree", "Binary Trees - Traversals", "Easy", "Postorder Traversal (Left -> Right -> Root)."},
            {"tree-level-order", "Level Order Traversal (BFS)", "Binary Trees - Traversals", "Easy", "Level order BFS traversal using Queue."},
            {"iterative-preorder", "Iterative Preorder Traversal", "Binary Trees - Traversals", "Medium", "Preorder traversal using explicit Stack."},
            {"iterative-inorder", "Iterative Inorder Traversal", "Binary Trees - Traversals", "Medium", "Inorder traversal using explicit Stack."},
            {"postorder-2-stacks", "Postorder Traversal Using 2 Stacks", "Binary Trees - Traversals", "Medium", "Iterative postorder traversal using 2 Stacks."},
            {"postorder-1-stack", "Postorder Traversal Using 1 Stack", "Binary Trees - Traversals", "Hard", "Iterative postorder traversal using 1 Stack & lastVisited pointer."},
            {"traversals-in-one-pass", "Pre, In, and Postorder in One Pass", "Binary Trees - Traversals", "Medium", "Single stack pass tracking node visiting state 1, 2, 3."},
            {"tree-height", "Maximum Depth (Height) in BT", "Binary Trees - Medium", "Easy", "Find max depth 1 + max(leftHeight, rightHeight)."},
            {"tree-balanced", "Check for Balanced Binary Tree", "Binary Trees - Medium", "Easy", "Check abs(leftHeight - rightHeight) <= 1 for all nodes."},
            {"tree-diameter", "Diameter of Binary Tree", "Binary Trees - Medium", "Easy", "Longest path between any 2 nodes max(leftHeight + rightHeight)."},
            {"tree-max-path-sum", "Maximum Path Sum in BT", "Binary Trees - Medium", "Hard", "Find maximum path sum from any node to any node."},
            {"identical-trees", "Check if Two Trees Are Identical", "Binary Trees - Medium", "Easy", "Check node.val equality and recursive left/right subtree equality."},
            {"zigzag-traversal", "Zig Zag (Spiral) Level Order Traversal", "Binary Trees - Medium", "Medium", "Level order traversal alternating left-to-right and right-to-left."},
            {"boundary-traversal", "Boundary Traversal of Binary Tree", "Binary Trees - Medium", "Medium", "Traverse Left Boundary + Leaves + Right Boundary in anti-clockwise order."},
            {"vertical-order-traversal", "Vertical Order Traversal", "Binary Trees - Medium", "Hard", "Group nodes by vertical column coordinate X."},
            {"top-view-bt", "Top View of Binary Tree", "Binary Trees - Medium", "Medium", "First node seen at each vertical coordinate X using Queue BFS."},
            {"bottom-view-bt", "Bottom View of Binary Tree", "Binary Trees - Medium", "Medium", "Last node seen at each vertical coordinate X using Queue BFS."},
            {"right-left-view-bt", "Right / Left View of Binary Tree", "Binary Trees - Medium", "Medium", "First node seen at each level depth using Reverse Preorder."},
            {"symmetric-tree", "Symmetric Binary Tree", "Binary Trees - Medium", "Easy", "Check if tree is a mirror reflection of itself."},
            {"root-to-leaf-path", "Print Root to Leaf Paths in BT", "Binary Trees - Hard", "Medium", "Collect all paths from root node to leaf nodes using DFS."},
            {"tree-lca", "Lowest Common Ancestor (LCA) in BT", "Binary Trees - Hard", "Medium", "Find LCA node where left and right subtrees return non-null."},
            {"max-width-bt", "Maximum Width of Binary Tree", "Binary Trees - Hard", "Medium", "Max nodes per level using index indexing position calculation."},
            {"children-sum-property", "Children Sum Property in BT", "Binary Trees - Hard", "Medium", "Modify tree such that node.val = node.left.val + node.right.val."},
            {"nodes-distance-k", "Print All Nodes at Distance K in BT", "Binary Trees - Hard", "Medium", "BFS outwards from target node using Parent Map pointers."},
            {"tree-burn-time", "Minimum Time to Burn Binary Tree", "Binary Trees - Hard", "Hard", "BFS burn simulation from starting target node using Parent Map."},
            {"count-complete-tree-nodes", "Count Total Nodes in Complete BT", "Binary Trees - Hard", "Medium", "Count complete binary tree nodes in O(log^2 N) using height matching."},
            {"unique-bt-requirements", "Requirements Needed for Unique BT", "Binary Trees - Hard", "Easy", "Inorder + (Preorder or Postorder) needed to build unique BT."},
            {"construct-bt-pre-in", "Construct BT from Preorder & Inorder", "Binary Trees - Hard", "Medium", "Reconstruct BT using Preorder root and Inorder index partitioning."},
            {"construct-bt-post-in", "Construct BT from Postorder & Inorder", "Binary Trees - Hard", "Medium", "Reconstruct BT using Postorder root and Inorder index partitioning."},
            {"serialize-deserialize-bt", "Serialize and Deserialize Binary Tree", "Binary Trees - Hard", "Hard", "Convert BT to string and reconstruct BT from string using Queue."},
            {"morris-preorder", "Morris Preorder Traversal", "Binary Trees - Hard", "Hard", "O(N) time O(1) space traversal using Threaded Binary Tree pointers."},
            {"morris-inorder", "Morris Inorder Traversal", "Binary Trees - Hard", "Hard", "O(N) time O(1) space inorder traversal using Threaded Binary Tree pointers."},
            {"flatten-bt-to-ll", "Flatten Binary Tree to Linked List", "Binary Trees - Hard", "Medium", "Flatten BT in-place to right-skewed linked list in Preorder sequence."},
            {"bst-intro", "Introduction to Binary Search Tree (BST)", "BST - Concepts", "Easy", "BST Property: Left < Node < Right for all subtrees."},
            {"bst-search", "Search in Binary Search Tree", "BST - Concepts", "Easy", "Search target in BST in O(H) time using BST property."},
            {"bst-min-max", "Find Min and Max in BST", "BST - Concepts", "Easy", "Min is leftmost node, Max is rightmost node in BST."},
            {"bst-floor-ceil", "Floor and Ceil in BST", "BST - Practice", "Medium", "Find Floor (largest val <= X) and Ceil (smallest val >= X) in BST."},
            {"bst-floor", "Floor in Binary Search Tree", "BST - Practice", "Medium", "Find largest node value <= X in BST."},
            {"bst-insert", "Insert Node in BST", "BST - Practice", "Medium", "Insert new key into correct leaf position preserving BST property."},
            {"bst-delete", "Delete Node in BST", "BST - Practice", "Medium", "Delete key from BST handling 0, 1, and 2 children cases."},
            {"bst-kth-smallest", "Kth Smallest / Largest Element in BST", "BST - Practice", "Medium", "Kth element using Inorder Traversal O(H + K) time."},
            {"bst-validate", "Validate Binary Search Tree", "BST - Practice", "Medium", "Validate if binary tree satisfies BST range [min, max] property."},
            {"bst-lca", "LCA in Binary Search Tree", "BST - Practice", "Medium", "Find LCA in BST using value comparisons O(H) time."},
            {"construct-bst-preorder", "Construct BST from Preorder Traversal", "BST - Practice", "Medium", "Reconstruct BST from preorder traversal array in O(N) time."},
            {"bst-inorder-successor", "Inorder Successor / Predecessor in BST", "BST - Practice", "Medium", "Find next larger / smaller node value in BST."},
            {"merge-two-bsts", "Merge Two BSTs", "BST - Practice", "Hard", "Merge 2 BSTs into single sorted list / balanced BST."},
            {"two-sum-bst", "Two Sum in BST", "BST - Practice", "Medium", "Find if pair with sum K exists using BST Iterator (next & before)."},
            {"correct-bst-swap", "Correct BST with Two Nodes Swapped", "BST - Practice", "Hard", "Recover BST with 2 swapped nodes using Inorder traversal pointers (first, middle, last)."},
            {"largest-bst-in-bt", "Largest BST in Binary Tree", "BST - Practice", "Hard", "Find max size valid BST subtree inside arbitrary Binary Tree in O(N) time."}
        };

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            problems.put(id, new ProblemDetail(
                id, title, cat, cat.startsWith("BST") ? "BST" : "Binary Trees", diff, desc,
                String.format("// Java Implementation for %s\npublic TreeNode solve(TreeNode root) {\n    return root;\n}", title),
                null, null, createDefaultTreeNodes(), null,
                new ComplexityDetail("O(N) / O(H)", "Time Complexity: Traversal over tree nodes.", "Tree Traversal", "O(H)", "Space Complexity: Recursion call stack height H.", "Call Stack", "Auxiliary Space: O(H)", "Memory"), "Stack"
            ));
        }
    }

    // Step Generators
    private List<ExecutionStep> generatePreorderSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        List<TreeNode> nodes = createDefaultTreeNodes();
        steps.add(new ExecutionStep(1, 4, "Preorder Traversal (Root -> Left -> Right): Visit Root (1).", List.of("1"), Map.of(), List.of(), Map.of("visited", "1"), "Stack", null, null, null, null, nodes));
        steps.add(new ExecutionStep(2, 62, "Traverse Left Subtree of 1 -> Node 2. Visit Node 2.", List.of("1", "2"), Map.of(), List.of(), Map.of("visited", "1, 2"), "Stack", null, null, null, null, nodes));
        steps.add(new ExecutionStep(3, 63, "Preorder Traversal Complete! Result: [1, 2, 4, 5, 3].", List.of(), Map.of(), List.of(), Map.of("Result", "[1, 2, 4, 5, 3]"), "Stack", null, null, null, null, nodes));
        return steps;
    }

    private List<TreeNode> createDefaultTreeNodes() {
        return List.of(
            new TreeNode(1, "1", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "2", 110, 110, 4, 5, "unvisited"),
            new TreeNode(3, "3", 270, 110, null, null, "unvisited"),
            new TreeNode(4, "4", 70, 180, null, null, "unvisited"),
            new TreeNode(5, "5", 150, 180, null, null, "unvisited")
        );
    }
}
