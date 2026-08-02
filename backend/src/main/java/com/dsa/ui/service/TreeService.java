package com.dsa.ui.service;

import com.dsa.ui.algorithm.tree.*;
import com.dsa.ui.model.*;
import com.dsa.ui.trace.ListTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TreeService {

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
            case "tree-preorder": return generatePreorderSteps();
            case "tree-inorder": return generateInorderSteps();
            case "tree-postorder": return generatePostorderSteps();
            case "tree-level-order": return generateLevelOrderSteps();
            case "tree-height": return generateHeightSteps();
            case "tree-balanced": return generateBalancedSteps();
            case "tree-diameter": return generateDiameterSteps();
            case "tree-max-path-sum": return generateMaxPathSumSteps();
            case "tree-lca": return generateLcaSteps();
            case "tree-burn-time": return generateBurnTimeSteps();
            case "bst-search": return generateBstSearchSteps();
            case "bst-validate": return generateBstValidateSteps();
            case "bst-kth-smallest": return generateBstKthSmallestSteps();
            default: return generatePreorderSteps();
        }
    }

    private void initProblems() {
        // 1. Binary Tree Preorder Traversal
        problems.put("tree-preorder", new ProblemDetail(
            "tree-preorder", "Preorder Traversal of Binary Tree", "Binary Trees - Traversals", "Binary Trees", "Easy",
            "Perform Preorder Traversal (Root -> Left -> Right) on a Binary Tree using recursion and stack.",
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
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Every node in the binary tree is visited exactly once.",
                "Why O(N)? In Preorder Traversal, we perform O(1) processing at each node before making recursive calls to left and right children. Total time = N * O(1) = O(N).",
                "O(H)",
                "Space Complexity: Auxiliary stack space equal to the height of the binary tree H. Worst case skewed tree H = O(N), balanced tree H = O(log N).",
                "Why O(H)? Maximum call stack depth equals maximum depth from root to leaf.",
                "Auxiliary Space: O(H) (Recursion Stack)",
                "Tree Output Array: O(N)"
            ),
            "Stack"
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
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Each node is visited once during the in-order traversal.",
                "Why O(N)? The recursion touches every node once to process left subtree, root, and right subtree.",
                "O(H)",
                "Space Complexity: O(H) recursion call stack depth where H is tree height.",
                "Why O(H)? Stack memory depends on tree height H.",
                "Auxiliary Space: O(H)",
                "Result Array: O(N)"
            ),
            "Stack"
        ));

        // 3. Binary Tree Postorder Traversal
        problems.put("tree-postorder", new ProblemDetail(
            "tree-postorder", "Postorder Traversal of Binary Tree", "Binary Trees - Traversals", "Binary Trees", "Easy",
            "Perform Postorder Traversal (Left -> Right -> Root) on a Binary Tree.",
            """
            // Java Postorder Traversal (Striver A2Z Sheet)
            public List<Integer> postorderTraversal(TreeNode root) {
                List<Integer> postorder = new ArrayList<>();
                postorderHelper(root, postorder);
                return postorder;
            }

            private void postorderHelper(TreeNode node, List<Integer> postorder) {
                if (node == null) return;
                postorderHelper(node.left, postorder);  // 1. Traverse Left
                postorderHelper(node.right, postorder); // 2. Traverse Right
                postorder.add(node.val);                // 3. Visit Root
            }
            """,
            null, null, createDefaultTreeNodes(), null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Visits all N nodes in the tree.",
                "Why O(N)? Postorder processes both subtrees before recording the root node value.",
                "O(H)",
                "Space Complexity: O(H) call stack height.",
                "Why O(H)? Max recursion stack size equals tree height H.",
                "Auxiliary Space: O(H)",
                "Result List: O(N)"
            ),
            "Stack"
        ));

        // 4. Level Order Traversal
        problems.put("tree-level-order", new ProblemDetail(
            "tree-level-order", "Level Order Traversal (BFS)", "Binary Trees - Traversals", "Binary Trees", "Easy",
            "Given the root of a binary tree, return the level order traversal of its nodes' values level by level using Queue.",
            """
            // Java Level Order Traversal (LeetCode 102)
            public List<List<Integer>> levelOrder(TreeNode root) {
                List<List<Integer>> wrapList = new ArrayList<>();
                if (root == null) return wrapList;
                Queue<TreeNode> queue = new LinkedList<>();
                queue.offer(root);

                while (!queue.isEmpty()) {
                    int levelNum = queue.size();
                    List<Integer> subList = new ArrayList<>();

                    for (int i = 0; i < levelNum; i++) {
                        if (queue.peek().left != null) queue.offer(queue.peek().left);
                        if (queue.peek().right != null) queue.offer(queue.peek().right);
                        subList.add(queue.poll().val);
                    }
                    wrapList.add(subList);
                }
                return wrapList;
            }
            """,
            null, null, createDefaultTreeNodes(), null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Every node is added to and removed from the queue exactly once.",
                "Why O(N)? Inner loop processes 'levelNum' nodes per level, summing up to N across all tree levels.",
                "O(N)",
                "Space Complexity: Queue memory stores at most the maximum width of the tree level (O(N) for complete binary tree bottom level).",
                "Why O(N)? Bottom level of a full binary tree contains N/2 leaf nodes.",
                "Auxiliary Space: O(N) (Queue)",
                "Result Space: O(N)"
            ),
            "Queue"
        ));

        // 5. Maximum Depth / Height
        problems.put("tree-height", new ProblemDetail(
            "tree-height", "Maximum Depth of Binary Tree", "Binary Trees - Medium", "Binary Trees", "Easy",
            "Find the height/max depth of a binary tree.",
            """
            // Java Height of Binary Tree (LeetCode 104)
            public int maxDepth(TreeNode root) {
                if (root == null) return 0;
                int lh = maxDepth(root.left);
                int rh = maxDepth(root.right);
                return 1 + Math.max(lh, rh);
            }
            """,
            null, null, createDefaultTreeNodes(), null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Recurse through all N nodes.",
                "Why O(N)? Evaluates height for every subtree recursively.",
                "O(H)",
                "Space Complexity: O(H) recursion stack height.",
                "Why O(H)? Stack memory matches height H.",
                "Auxiliary Space: O(H)",
                "Return Space: O(1)"
            ),
            "Stack"
        ));

        // 6. Check for Balanced Tree
        problems.put("tree-balanced", new ProblemDetail(
            "tree-balanced", "Check if Binary Tree is Balanced", "Binary Trees - Medium", "Binary Trees", "Easy",
            "A binary tree is height-balanced if depth of two subtrees of every node never differs by more than 1.",
            """
            // Java Balanced Tree Check - O(N) Solution (LeetCode 110)
            public boolean isBalanced(TreeNode root) {
                return dfsHeight(root) != -1;
            }

            private int dfsHeight(TreeNode node) {
                if (node == null) return 0;
                int leftHeight = dfsHeight(node.left);
                if (leftHeight == -1) return -1;
                int rightHeight = dfsHeight(node.right);
                if (rightHeight == -1) return -1;

                if (Math.abs(leftHeight - rightHeight) > 1) return -1;
                return 1 + Math.max(leftHeight, rightHeight);
            }
            """,
            null, null, createDefaultTreeNodes(), null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: O(N) optimized height calculation with early return -1.",
                "Why O(N) instead of O(N^2)? Calculating height inline during post-order traversal eliminates duplicate height calls.",
                "O(H)",
                "Space Complexity: O(H) recursion stack memory.",
                "Why O(H)? Max call stack depth is tree height H.",
                "Auxiliary Space: O(H)",
                "Return Space: O(1)"
            ),
            "Stack"
        ));

        // 7. Diameter of Binary Tree
        problems.put("tree-diameter", new ProblemDetail(
            "tree-diameter", "Diameter of Binary Tree", "Binary Trees - Medium", "Binary Trees", "Easy",
            "The diameter is the length of the longest path between any two nodes in a tree.",
            """
            // Java Diameter of Binary Tree - O(N) (LeetCode 543)
            public int diameterOfBinaryTree(TreeNode root) {
                int[] diameter = new int[1];
                height(root, diameter);
                return diameter[0];
            }

            private int height(TreeNode node, int[] diameter) {
                if (node == null) return 0;
                int lh = height(node.left, diameter);
                int rh = height(node.right, diameter);
                diameter[0] = Math.max(diameter[0], lh + rh);
                return 1 + Math.max(lh, rh);
            }
            """,
            null, null, createDefaultTreeNodes(), null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass postorder traversal updates maximum diameter `lh + rh` at every node.",
                "Why O(N)? Avoids calculating height separately for each subtree.",
                "O(H)",
                "Space Complexity: O(H) recursion stack.",
                "Why O(H)? Max stack depth is tree height.",
                "Auxiliary Space: O(H)",
                "Return Space: O(1)"
            ),
            "Stack"
        ));

        // 8. Maximum Path Sum
        problems.put("tree-max-path-sum", new ProblemDetail(
            "tree-max-path-sum", "Maximum Path Sum in Binary Tree", "Binary Trees - Medium", "Binary Trees", "Hard",
            "A path in a binary tree is a sequence of nodes where each pair of adjacent nodes has an edge. Find max path sum.",
            """
            // Java Maximum Path Sum (LeetCode 124)
            public int maxPathSum(TreeNode root) {
                int[] maxi = new int[]{Integer.MIN_VALUE};
                maxPathDown(root, maxi);
                return maxi[0];
            }

            private int maxPathDown(TreeNode node, int[] maxi) {
                if (node == null) return 0;
                int left = Math.max(0, maxPathDown(node.left, maxi));
                int right = Math.max(0, maxPathDown(node.right, maxi));
                maxi[0] = Math.max(maxi[0], left + right + node.val);
                return node.val + Math.max(left, right);
            }
            """,
            null, null, createDefaultTreeNodes(), null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Every node is processed once to calculate max gain from left & right subtrees.",
                "Why Math.max(0, ...)? Negative sum subtrees are ignored (clamped to 0).",
                "O(H)",
                "Space Complexity: O(H) recursion call stack depth.",
                "Why O(H)? Stack memory depends on tree height H.",
                "Auxiliary Space: O(H)",
                "Return Space: O(1)"
            ),
            "Stack"
        ));

        // 9. Lowest Common Ancestor (LCA)
        problems.put("tree-lca", new ProblemDetail(
            "tree-lca", "Lowest Common Ancestor (LCA)", "Binary Trees - Medium", "Binary Trees", "Medium",
            "Find the lowest common ancestor of two given nodes p and q in a binary tree.",
            """
            // Java Lowest Common Ancestor (LeetCode 236)
            public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
                if (root == null || root == p || root == q) return root;

                TreeNode left = lowestCommonAncestor(root.left, p, q);
                TreeNode right = lowestCommonAncestor(root.right, p, q);

                if (left == null) return right;
                if (right == null) return left;
                return root; // Both left and right non-null -> root is LCA!
            }
            """,
            null, null, createDefaultTreeNodes(), null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: In worst case, visits all N nodes to locate p and q.",
                "Why LCA logic works? If p is in left subtree and q is in right subtree, current node is the lowest common ancestor.",
                "O(H)",
                "Space Complexity: O(H) call stack height.",
                "Why O(H)? Call stack depth bounded by tree height.",
                "Auxiliary Space: O(H)",
                "Return Space: O(1)"
            ),
            "Stack"
        ));

        // 10. Minimum Time to Burn Tree
        problems.put("tree-burn-time", new ProblemDetail(
            "tree-burn-time", "Minimum Time to Burn Tree from Target Node", "Binary Trees - Hard", "Binary Trees", "Hard",
            "Find minimum time needed to burn the entire binary tree starting from a given target node.",
            """
            // Java Burn Binary Tree (Striver A2Z Sheet - Parent Pointer + BFS)
            public int minTimeToBurn(TreeNode root, int target) {
                Map<TreeNode, TreeNode> parentMap = new HashMap<>();
                TreeNode targetNode = markParents(root, parentMap, target);

                Map<TreeNode, Boolean> visited = new HashMap<>();
                Queue<TreeNode> q = new LinkedList<>();
                q.add(targetNode);
                visited.put(targetNode, true);

                int time = 0;
                while (!q.isEmpty()) {
                    int size = q.size();
                    boolean flag = false;
                    for (int i = 0; i < size; i++) {
                        TreeNode node = q.poll();
                        if (node.left != null && !visited.containsKey(node.left)) {
                            flag = true; visited.put(node.left, true); q.add(node.left);
                        }
                        if (node.right != null && !visited.containsKey(node.right)) {
                            flag = true; visited.put(node.right, true); q.add(node.right);
                        }
                        if (parentMap.get(node) != null && !visited.containsKey(parentMap.get(node))) {
                            flag = true; visited.put(parentMap.get(node), true); q.add(parentMap.get(node));
                        }
                    }
                    if (flag) time++;
                }
                return time;
            }
            """,
            null, null, createDefaultTreeNodes(), null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Parent mapping DFS takes O(N). Radial BFS burning outward from target visits each node once O(N). Total = O(N).",
                "Why Parent Map needed? Fire burns 3 directions: Left child, Right child, and Parent node! Mapping parent references allows 3-way BFS expansion.",
                "O(N)",
                "Space Complexity: Parent Map takes O(N), Visited Map takes O(N), Queue takes O(N).",
                "Why O(N)? Storing parent pointers for N nodes requires O(N) space.",
                "Auxiliary Space: O(N) (Parent Map & BFS Queue)",
                "Result Time: O(1)"
            ),
            "Queue"
        ));

        // 11. Search in BST
        problems.put("bst-search", new ProblemDetail(
            "bst-search", "Search in Binary Search Tree (BST)", "BST - Concepts", "Binary Search Trees", "Easy",
            "Given the root of a BST and a target value val, return the node with value val.",
            """
            // Java Search in BST (LeetCode 700)
            public TreeNode searchBST(TreeNode root, int val) {
                while (root != null && root.val != val) {
                    root = val < root.val ? root.left : root.right;
                }
                return root;
            }
            """,
            null, null, createBstNodes(), null,
            new ComplexityDetail(
                "O(H)",
                "Time Complexity: In a balanced BST, H = O(log N). In skewed BST, H = O(N).",
                "Why O(H)? Binary search property eliminates half of the remaining subtrees at every step.",
                "O(1)",
                "Space Complexity: Iterative while loop takes O(1) extra space.",
                "Why O(1)? No recursion call stack required.",
                "Auxiliary Space: O(1)",
                "Return Node: O(1)"
            ),
            "Queue"
        ));

        // 12. Validate BST
        problems.put("bst-validate", new ProblemDetail(
            "bst-validate", "Validate Binary Search Tree", "BST - Practice", "Binary Search Trees", "Medium",
            "Check if a binary tree is a valid Binary Search Tree (BST) using range limits [min, max].",
            """
            // Java Validate BST (LeetCode 98)
            public boolean isValidBST(TreeNode root) {
                return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
            }

            private boolean validate(TreeNode node, long min, long max) {
                if (node == null) return true;
                if (node.val <= min || node.val >= max) return false;
                return validate(node.left, min, node.val) && validate(node.right, node.val, max);
            }
            """,
            null, null, createBstNodes(), null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Every node's value is checked against its allowed (min, max) boundary.",
                "Why Range Check vs Simple Parent Check? Simply checking left < root < right is INSUFFICIENT because sub-nodes deep in left subtree must ALL be < root!",
                "O(H)",
                "Space Complexity: O(H) recursion stack depth.",
                "Why O(H)? Height of BST H.",
                "Auxiliary Space: O(H)",
                "Return Boolean: O(1)"
            ),
            "Stack"
        ));

        // 13. Kth Smallest in BST
        problems.put("bst-kth-smallest", new ProblemDetail(
            "bst-kth-smallest", "Kth Smallest Element in BST", "BST - Practice", "Binary Search Trees", "Medium",
            "Find the kth smallest element in a BST using Inorder Traversal.",
            """
            // Java Kth Smallest Element in BST (LeetCode 230)
            public int kthSmallest(TreeNode root, int k) {
                int[] count = new int[]{0};
                int[] result = new int[]{-1};
                inorder(root, k, count, result);
                return result[0];
            }

            private void inorder(TreeNode node, int k, int[] count, int[] result) {
                if (node == null || count[0] >= k) return;
                inorder(node.left, k, count, result);
                count[0]++;
                if (count[0] == k) {
                    result[0] = node.val;
                    return;
                }
                inorder(node.right, k, count, result);
            }
            """,
            null, null, createBstNodes(), null,
            new ComplexityDetail(
                "O(H + K)",
                "Time Complexity: Inorder traversal visits BST nodes in sorted order. We stop immediately after visiting K nodes.",
                "Why BST Inorder is Sorted? Left < Root < Right property guarantees that inorder yields monotonically increasing sequence.",
                "O(H)",
                "Space Complexity: O(H) call stack height.",
                "Why O(H)? Recursion depth bounded by tree height.",
                "Auxiliary Space: O(H)",
                "Return Value: O(1)"
            ),
            "Stack"
        ));
    }

    // Step Generators
    private List<ExecutionStep> generatePreorderSteps() {
        TreePreorderTraversal.Node root = new TreePreorderTraversal.Node(1);
        root.left = new TreePreorderTraversal.Node(2);
        root.right = new TreePreorderTraversal.Node(3);
        root.left.left = new TreePreorderTraversal.Node(4);
        root.left.right = new TreePreorderTraversal.Node(5);

        ListTraceRecorder recorder = new ListTraceRecorder();
        new TreePreorderTraversal().solve(root, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateInorderSteps() {
        TreeInorderTraversal.Node root = new TreeInorderTraversal.Node(1);
        root.left = new TreeInorderTraversal.Node(2);
        root.right = new TreeInorderTraversal.Node(3);
        root.left.left = new TreeInorderTraversal.Node(4);
        root.left.right = new TreeInorderTraversal.Node(5);

        ListTraceRecorder recorder = new ListTraceRecorder();
        new TreeInorderTraversal().solve(root, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generatePostorderSteps() {
        TreePostorderTraversal.Node root = new TreePostorderTraversal.Node(1);
        root.left = new TreePostorderTraversal.Node(2);
        root.right = new TreePostorderTraversal.Node(3);
        root.left.left = new TreePostorderTraversal.Node(4);
        root.left.right = new TreePostorderTraversal.Node(5);

        ListTraceRecorder recorder = new ListTraceRecorder();
        new TreePostorderTraversal().solve(root, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateLevelOrderSteps() {
        TreeLevelOrderTraversal.Node root = new TreeLevelOrderTraversal.Node(1);
        root.left = new TreeLevelOrderTraversal.Node(2);
        root.right = new TreeLevelOrderTraversal.Node(3);
        root.left.left = new TreeLevelOrderTraversal.Node(4);
        root.left.right = new TreeLevelOrderTraversal.Node(5);

        ListTraceRecorder recorder = new ListTraceRecorder();
        new TreeLevelOrderTraversal().solve(root, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateHeightSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> states = new HashMap<>();
        for (int i = 1; i <= 7; i++) states.put(i, "unvisited");

        states.put(1, "visiting");
        steps.add(new ExecutionStep(1, 3, "Compute height for root 1. Recurse left and right", List.of("maxDepth(1)"), new HashMap<>(states), List.of(), Map.of("node", "1"), "Stack", null));

        states.put(2, "visiting");
        steps.add(new ExecutionStep(2, 4, "lh = maxDepth(2)", List.of("maxDepth(1)", "maxDepth(2)"), new HashMap<>(states), List.of(), Map.of("lh", "2"), "Stack", null));

        states.put(3, "visiting");
        steps.add(new ExecutionStep(3, 5, "rh = maxDepth(3)", List.of("maxDepth(1)", "maxDepth(3)"), new HashMap<>(states), List.of(), Map.of("rh", "2"), "Stack", null));

        for (int i = 1; i <= 7; i++) states.put(i, "visited");
        steps.add(new ExecutionStep(4, 6, "1 + Math.max(lh, rh) = 1 + Math.max(2, 2) = 3. Height = 3", List.of(), new HashMap<>(states), List.of(), Map.of("Height", "3"), "Stack", null));

        return steps;
    }

    private List<ExecutionStep> generateBalancedSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> states = new HashMap<>();
        for (int i = 1; i <= 7; i++) states.put(i, "unvisited");

        states.put(1, "visiting");
        steps.add(new ExecutionStep(1, 7, "dfsHeight(1): Left height = 2, Right height = 2", List.of("dfsHeight(1)"), new HashMap<>(states), List.of(), Map.of("|lh - rh|", "|2 - 2| = 0 <= 1"), "Stack", null));

        for (int i = 1; i <= 7; i++) states.put(i, "visited");
        steps.add(new ExecutionStep(2, 13, "Abs difference <= 1 for all nodes. Tree IS BALANCED!", List.of(), new HashMap<>(states), List.of(), Map.of("Is Balanced", "TRUE"), "Stack", null));

        return steps;
    }

    private List<ExecutionStep> generateDiameterSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> states = new HashMap<>();
        for (int i = 1; i <= 7; i++) states.put(i, "unvisited");

        states.put(1, "visiting");
        steps.add(new ExecutionStep(1, 10, "At root 1: lh = 2 (left depth), rh = 2 (right depth)", List.of("height(1)"), new HashMap<>(states), List.of(), Map.of("lh + rh", "2 + 2 = 4"), "Stack", null));

        for (int i = 1; i <= 7; i++) states.put(i, "visited");
        steps.add(new ExecutionStep(2, 11, "Maximum diameter = lh + rh = 4 edges", List.of(), Map.of(), List.of(), Map.of("Diameter", "4"), "Stack", null));

        return steps;
    }

    private List<ExecutionStep> generateMaxPathSumSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> states = new HashMap<>();
        for (int i = 1; i <= 7; i++) states.put(i, "unvisited");

        states.put(1, "visiting");
        steps.add(new ExecutionStep(1, 9, "maxPathDown(1): left gain = 6, right gain = 10", List.of("maxPathDown(1)"), new HashMap<>(states), List.of(), Map.of("pathSum", "6 + 10 + 1 = 17"), "Stack", null));

        for (int i = 1; i <= 7; i++) states.put(i, "visited");
        steps.add(new ExecutionStep(2, 11, "Maximum Path Sum across tree = 17", List.of(), new HashMap<>(states), List.of(), Map.of("Max Path Sum", "17"), "Stack", null));

        return steps;
    }

    private List<ExecutionStep> generateLcaSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> states = new HashMap<>();
        for (int i = 1; i <= 7; i++) states.put(i, "unvisited");

        states.put(4, "active"); states.put(5, "active");
        steps.add(new ExecutionStep(1, 2, "Find LCA for target nodes p=4 and q=5", List.of("LCA(1, 4, 5)"), new HashMap<>(states), List.of(), Map.of("p", "4", "q", "5"), "Stack", null));

        states.put(2, "visited");
        steps.add(new ExecutionStep(2, 9, "Both left (4) and right (5) return non-null to node 2. Node 2 IS THE LCA!", List.of("LCA(2, 4, 5)"), new HashMap<>(states), List.of(), Map.of("LCA Node", "2"), "Stack", null));

        return steps;
    }

    private List<ExecutionStep> generateBurnTimeSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> states = new HashMap<>();
        for (int i = 1; i <= 7; i++) states.put(i, "unvisited");

        states.put(4, "active");
        steps.add(new ExecutionStep(1, 7, "Start fire at target leaf node 4 at time t=0", List.of("4"), new HashMap<>(states), List.of(), Map.of("time", "0"), "Queue", null));

        states.put(4, "burned"); states.put(2, "active");
        steps.add(new ExecutionStep(2, 23, "t=1: Fire spreads upward to parent node 2", List.of("2"), new HashMap<>(states), List.of(), Map.of("time", "1"), "Queue", null));

        states.put(2, "burned"); states.put(5, "active"); states.put(1, "active");
        steps.add(new ExecutionStep(3, 23, "t=2: Fire spreads from 2 to child 5 and parent 1", List.of("5", "1"), new HashMap<>(states), List.of(), Map.of("time", "2"), "Queue", null));

        states.put(5, "burned"); states.put(1, "burned"); states.put(3, "active");
        steps.add(new ExecutionStep(4, 23, "t=3: Fire spreads from 1 to node 3", List.of("3"), new HashMap<>(states), List.of(), Map.of("time", "3"), "Queue", null));

        states.put(3, "burned"); states.put(6, "burned"); states.put(7, "burned");
        steps.add(new ExecutionStep(5, 27, "t=4: Entire binary tree is burned in 4 time units!", List.of(), new HashMap<>(states), List.of(), Map.of("Burn Time", "4"), "Queue", null));

        return steps;
    }

    private List<ExecutionStep> generateBstSearchSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> states = new HashMap<>();
        for (int i : List.of(10, 5, 15, 3, 7, 12, 18)) states.put(i, "unvisited");

        states.put(10, "visiting");
        steps.add(new ExecutionStep(1, 3, "Search val = 7. Start at BST root 10. Since 7 < 10, go LEFT", List.of("10"), new HashMap<>(states), List.of(), Map.of("root", "10", "target", "7"), "Queue", null));

        states.put(10, "visited"); states.put(5, "visiting");
        steps.add(new ExecutionStep(2, 3, "At node 5. Since 7 > 5, go RIGHT", List.of("5"), new HashMap<>(states), List.of(), Map.of("root", "5", "target", "7"), "Queue", null));

        states.put(5, "visited"); states.put(7, "active");
        steps.add(new ExecutionStep(3, 5, "At node 7. root.val == target val (7 == 7). FOUND TARGET NODE!", List.of("7"), new HashMap<>(states), List.of(), Map.of("Found", "7"), "Queue", null));

        return steps;
    }

    private List<ExecutionStep> generateBstValidateSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> states = new HashMap<>();
        for (int i : List.of(10, 5, 15, 3, 7, 12, 18)) states.put(i, "unvisited");

        states.put(10, "visiting");
        steps.add(new ExecutionStep(1, 7, "validate(10, min=-INF, max=+INF) -> Valid", List.of("10"), new HashMap<>(states), List.of(), Map.of("range", "(-INF, +INF)"), "Stack", null));

        states.put(5, "visiting");
        steps.add(new ExecutionStep(2, 8, "validate(5, min=-INF, max=10) -> Valid", List.of("5"), new HashMap<>(states), List.of(), Map.of("range", "(-INF, 10)"), "Stack", null));

        for (int i : List.of(10, 5, 15, 3, 7, 12, 18)) states.put(i, "visited");
        steps.add(new ExecutionStep(3, 8, "All node values satisfy BST range constraints. IS VALID BST!", List.of(), new HashMap<>(states), List.of(), Map.of("Valid BST", "TRUE"), "Stack", null));

        return steps;
    }

    private List<ExecutionStep> generateBstKthSmallestSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> states = new HashMap<>();
        for (int i : List.of(10, 5, 15, 3, 7, 12, 18)) states.put(i, "unvisited");

        // BST Inorder: 3, 5, 7, 10, 12, 15, 18
        states.put(3, "visited");
        steps.add(new ExecutionStep(1, 12, "Inorder Step 1: Visit 3 (1st smallest)", List.of("inorder(3)"), new HashMap<>(states), List.of(), Map.of("count", "1"), "Stack", null));

        states.put(5, "visited");
        steps.add(new ExecutionStep(2, 12, "Inorder Step 2: Visit 5 (2nd smallest)", List.of("inorder(5)"), new HashMap<>(states), List.of(), Map.of("count", "2"), "Stack", null));

        states.put(7, "active");
        steps.add(new ExecutionStep(3, 14, "Inorder Step 3: Visit 7. count == k (3 == 3). Found 3rd Smallest = 7!", List.of("inorder(7)"), new HashMap<>(states), List.of(), Map.of("kthSmallest", "7"), "Stack", null));

        return steps;
    }

    // Helper builders
    private List<TreeNode> createDefaultTreeNodes() {
        return List.of(
            new TreeNode(1, "1", 180, 50, 2, 3, "unvisited"),
            new TreeNode(2, "2", 100, 130, 4, 5, "unvisited"),
            new TreeNode(3, "3", 260, 130, 6, 7, "unvisited"),
            new TreeNode(4, "4", 60, 220, null, null, "unvisited"),
            new TreeNode(5, "5", 140, 220, null, null, "unvisited"),
            new TreeNode(6, "6", 220, 220, null, null, "unvisited"),
            new TreeNode(7, "7", 300, 220, null, null, "unvisited")
        );
    }

    private List<TreeNode> createBstNodes() {
        return List.of(
            new TreeNode(10, "10", 180, 50, 5, 15, "unvisited"),
            new TreeNode(5, "5", 100, 130, 3, 7, "unvisited"),
            new TreeNode(15, "15", 260, 130, 12, 18, "unvisited"),
            new TreeNode(3, "3", 60, 220, null, null, "unvisited"),
            new TreeNode(7, "7", 140, 220, null, null, "unvisited"),
            new TreeNode(12, "12", 220, 220, null, null, "unvisited"),
            new TreeNode(18, "18", 300, 220, null, null, "unvisited")
        );
    }
}
