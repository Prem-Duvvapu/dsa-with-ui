export const DEFAULT_FALLBACK_PROBLEMS = [
  {
    id: 'two-sum',
    title: 'Two Sum',
    category: 'Arrays & Hashing',
    difficulty: 'Easy',
    dsType: 'Array',
    defaultArray: [
      { value: 2, state: 'default' },
      { value: 7, state: 'current' },
      { value: 11, state: 'target' },
      { value: 15, state: 'visited' }
    ],
    javaCode: `public int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> map = new HashMap<>();
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        if (map.containsKey(complement)) {
            return new int[] { map.get(complement), i };
        }
        map.put(nums[i], i);
    }
    return new int[0];
}`,
    complexity: {
      timeComplexity: 'O(N)',
      spaceComplexity: 'O(N)',
      timeExplanation: 'Single pass through array using Hash Map lookups.',
      spaceExplanation: 'Hash map stores up to N element complement mappings.'
    },
    executionSteps: [
      {
        stepNumber: 1,
        activeLine: 3,
        description: 'Initialize empty HashMap. Iterate index i = 0, current value = 2.',
        arrayState: [
          { value: 2, state: 'current' },
          { value: 7, state: 'default' },
          { value: 11, state: 'default' },
          { value: 15, state: 'default' }
        ],
        variables: { i: 0, val: 2, target: 9, complement: 7 }
      },
      {
        stepNumber: 2,
        activeLine: 7,
        description: 'Iterate index i = 1, current value = 7. Complement 9 - 7 = 2 exists in map at index 0!',
        arrayState: [
          { value: 2, state: 'done' },
          { value: 7, state: 'target' },
          { value: 11, state: 'default' },
          { value: 15, state: 'default' }
        ],
        variables: { i: 1, val: 7, target: 9, complement: 2, result: '[0, 1]' }
      }
    ]
  },
  {
    id: 'longest-substring-without-repeating',
    title: 'Longest Substring Without Repeating Characters',
    category: 'Sliding Window',
    difficulty: 'Medium',
    dsType: 'Array',
    defaultArray: [
      { value: 97, state: 'visited' },
      { value: 98, state: 'current' },
      { value: 99, state: 'target' },
      { value: 97, state: 'default' }
    ],
    javaCode: `public int lengthOfLongestSubstring(String s) {
    HashMap<Character, Integer> map = new HashMap<>();
    int left = 0, right = 0, maxLen = 0;
    while (right < s.length()) {
        char ch = s.charAt(right);
        if (map.containsKey(ch)) {
            left = Math.max(map.get(ch) + 1, left);
        }
        map.put(ch, right);
        maxLen = Math.max(maxLen, right - left + 1);
        right++;
    }
    return maxLen;
}`,
    complexity: {
      timeComplexity: 'O(N)',
      spaceComplexity: 'O(min(m, n))',
      timeExplanation: 'Single pass sliding window pointers right and left.',
      spaceExplanation: 'Hash map stores unique characters bounded by alphabet size.'
    },
    executionSteps: [
      {
        stepNumber: 1,
        activeLine: 4,
        description: 'Input string s = "abcabcbb". Initialize sliding window pointers left = 0, right = 0, maxLen = 0.',
        variables: { left: 0, right: 0, maxLen: 0, s: '"abcabcbb"' }
      }
    ]
  }
];
