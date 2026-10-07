public class Solution {
    public int climbStairs(int n) {
        // @a init
        int[] ways = new int[n + 1];
        // @a base
        ways[0] = 1; ways[1] = 1;
        for (int i = 2; i <= n; i++) {
            // @a combine
            ways[i] = ways[i - 1] + ways[i - 2];
        }
        // @a done
        return ways[n];
    }
}
