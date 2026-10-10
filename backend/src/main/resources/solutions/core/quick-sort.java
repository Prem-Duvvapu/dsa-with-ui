public class Solution {
    public int[] solve(int[] nums) {
        quickSort(nums, 0, nums.length - 1);
        // @a done
        return nums;
    }

    private void quickSort(int[] nums, int low, int high) {
        if (low < high) {
            // @a partition
            int pIndex = partition(nums, low, high);
            quickSort(nums, low, pIndex - 1);
            quickSort(nums, pIndex + 1, high);
        } else {
            // @a base
            return;
        }
    }

    private int partition(int[] nums, int low, int high) {
        // @a pivotChoice
        int pivot = nums[low];
        int i = low;
        for (int j = low + 1; j <= high; j++) {
            // @a compare
            if (nums[j] < pivot) {
                i++;
                // @a swapLess
                swap(nums, i, j);
            }
        }
        // @a placePivot
        swap(nums, low, i);
        return i;
    }

    private void swap(int[] nums, int first, int second) {
        int value = nums[first];
        nums[first] = nums[second];
        nums[second] = value;
    }
}
