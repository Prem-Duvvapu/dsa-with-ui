public class Solution {
    public long solve(int[] nums, int k) {
        int n = nums.length;
        long[] best = new long[n + 1];
        // choice[i] records the winning last-block length for prefix i.
        // @a init
        int[] choice = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            long value = 0;
            int chosenLength = 0, blockMax = 0;
            for (int length = 1; length <= Math.min(k, i); length++) {
                blockMax = Math.max(blockMax, nums[i - length]);
                long candidate = best[i - length] + (long) length * blockMax;
                // @a fill
                if (candidate > value) {
                    value = candidate;
                    chosenLength = length;
                }
                best[i] = value;
                choice[i] = chosenLength;
            }
        }
        // @a done
        return best[n];
    }
}
