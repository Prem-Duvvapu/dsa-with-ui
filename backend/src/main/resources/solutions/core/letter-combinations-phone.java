import java.util.ArrayList;
import java.util.List;

public class Solution {
    private static final String[] KEYPAD = {
        "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"
    };
    public List<String> letterCombinations(String digits) {
        List<String> results = new ArrayList<>();
        if (digits == null || digits.isEmpty()) return results;
        backtrack(0, digits, new StringBuilder(), results);
        // @a done
        return results;
    }
    private void backtrack(int index, String digits, StringBuilder path, List<String> results) {
        if (index == digits.length()) {
            // @a capture
            results.add(path.toString());
            return;
        }
        String letters = KEYPAD[digits.charAt(index) - '0'];
        for (char letter : letters.toCharArray()) {
            // @a choose
            path.append(letter);
            backtrack(index + 1, digits, path, results);
            path.deleteCharAt(path.length() - 1);
        }
    }
}
