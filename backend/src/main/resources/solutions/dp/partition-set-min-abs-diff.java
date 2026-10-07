import java.util.Arrays;

public class Solution {
    public long solve(int[] nums) {
        int total = Arrays.stream(nums).sum();
        // @a init
        boolean[][] reachable = new boolean[nums.length + 1][total + 1];
        for (int i = 0; i <= nums.length; i++) reachable[i][0] = true;
        for (int i = 1; i <= nums.length; i++) {
            for (int sum = 1; sum <= total; sum++) {
                // @a fill
                reachable[i][sum] = reachable[i - 1][sum]
                        || (sum >= nums[i - 1] && reachable[i - 1][sum - nums[i - 1]]);
            }
        }
        int best = total;
        for (int sum = 0; sum <= total / 2; sum++) {
            if (reachable[nums.length][sum]) best = Math.min(best, total - 2 * sum);
        }
        // @a done
        return best;
    }
}
