import java.util.Arrays;

public class Solution {
    public long solve(int[] nums) {
        int n = nums.length;
        long[] increasing = new long[n], decreasing = new long[n];
        // @a init
        Arrays.fill(increasing, 1);
        Arrays.fill(decreasing, 1);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // @a forward
                if (nums[j] < nums[i]) increasing[i] = Math.max(increasing[i], 1 + increasing[j]);
            }
        }
        for (int i = n - 1; i >= 0; i--) {
            for (int j = n - 1; j > i; j--) {
                // @a reverse
                if (nums[j] < nums[i]) decreasing[i] = Math.max(decreasing[i], 1 + decreasing[j]);
            }
        }
        long best = 1;
        for (int i = 0; i < n; i++) best = Math.max(best, increasing[i] + decreasing[i] - 1);
        // @a done
        return best;
    }
}
