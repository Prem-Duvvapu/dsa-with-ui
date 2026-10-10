public class Solution {
    public int[] solve(int[] nums) {
        int n = nums.length, ind = -1;
        for (int i = n - 2; i >= 0; i--) {
            if (nums[i] < nums[i + 1]) {
                // @a foundBreak
                ind = i;
                break;
            } else {
                // @a checkBreak
                continue;
            }
        }
        if (ind == -1) {
            // @a noBreak
            reverse(nums, 0, n - 1);
        } else {
            for (int i = n - 1; i > ind; i--) {
                if (nums[i] > nums[ind]) {
                    // @a foundSwap
                    swap(nums, i, ind);
                    break;
                } else {
                    // @a checkSwap
                    continue;
                }
            }
            // @a reverseSuffix
            reverse(nums, ind + 1, n - 1);
        }
        // @a done
        return nums;
    }

    private void swap(int[] nums, int first, int second) {
        int value = nums[first];
        nums[first] = nums[second];
        nums[second] = value;
    }

    private void reverse(int[] nums, int left, int right) {
        while (left < right) swap(nums, left++, right--);
    }
}
