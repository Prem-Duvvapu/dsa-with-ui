public class Solution {
    public String solve(String s) {
        char[] chars = s.toCharArray();
        int start = 0;
        while (start < chars.length) {
            if (chars[start] == ' ') {
                start++;
                continue;
            }
            int end = start;
            while (end < chars.length && chars[end] != ' ') end++;
            // @a reverse
            reverse(chars, start, end - 1);
            start = end + 1;
        }
        // @a done
        return new String(chars);
    }

    private static void reverse(char[] chars, int left, int right) {
        while (left < right) {
            char temporary = chars[left];
            chars[left++] = chars[right];
            chars[right--] = temporary;
        }
    }
}
