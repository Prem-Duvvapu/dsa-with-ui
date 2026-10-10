public class Solution {
    public int solve(int[] nums) {
        int n = nums.length;
        // @a start
        int answer = solve(0, 0, nums);
        // @a done
        return answer;
    }

    private int solve(int i, int p, int[] nums) {
        // @a enter
        if (i == nums.length) {
            // @a base
            return 0;
        }
        // @a branch
        int skip = solve(i + 1, p, nums);
        int take = p == 0 || nums[i] > nums[p - 1]
                ? 1 + solve(i + 1, i + 1, nums) : Integer.MIN_VALUE;
        // @a combine
        return Math.max(skip, take);
    }
}
