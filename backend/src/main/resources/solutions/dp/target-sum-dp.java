import java.util.Arrays;

public class Solution {
    public long solve(int[] nums, int target) {
        int total = Arrays.stream(nums).sum(), width = 2 * total + 1;
        long[][] dp = new long[nums.length + 1][width];
        // Column total represents sum zero; one empty assignment reaches it.
        // @a init
        dp[0][total] = 1;
        for (int i = 1; i <= nums.length; i++) {
            for (int col = 0; col < width; col++) {
                int plus = col - nums[i - 1], minus = col + nums[i - 1];
                // A zero still has two choices: +0 and -0.
                // @a fill
                dp[i][col] = (plus >= 0 ? dp[i - 1][plus] : 0)
                        + (minus < width ? dp[i - 1][minus] : 0);
            }
        }
        // @a done
        return Math.abs((long) target) <= total ? dp[nums.length][target + total] : 0;
    }
}
