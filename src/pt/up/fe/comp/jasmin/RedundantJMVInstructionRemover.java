package pt.up.fe.comp.jasmin;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedundantJMVInstructionRemover {
    private final String jasminCode;

    public RedundantJMVInstructionRemover(String jasminCode) {
        this.jasminCode = jasminCode;
    }

    public String getOptimizedCode() {
        StringBuilder code = new StringBuilder();
        List<String> lines = List.of(jasminCode.split("\n"));

        for (int i = 0; i < lines.size(); i++) {
            if (redundantStoreAndLoad(lines, i)) {
                i++;
            }
            else {
                code.append(lines.get(i)).append("\n");
            }
        }

        return code.toString();
    }

    private boolean redundantStoreAndLoad(List<String> lines, int position) {
        if (lines.size() <= position + 1) return false;
        String currentLine = lines.get(position).trim();
        String nextLine = lines.get(position + 1).trim();
        if (Pattern.matches("([ai])store \\d+", currentLine) && Pattern.matches("[ai]load \\d+", nextLine)) {
            String register = currentLine.split(" ")[1];
            if (Objects.equals(register, nextLine.split(" ")[1])) {
                return countMatches(jasminCode, " " + register +"\n") == 2;
            }
        }
        return false;
    }

    /* Checks if a string is empty ("") or null. */
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    /* Counts how many times the substring appears in the larger string. */
    public static int countMatches(String text, String str)
    {
        if (isEmpty(text) || isEmpty(str)) {
            return 0;
        }

        Matcher matcher = Pattern.compile(str).matcher(text);

        int count = 0;
        while (matcher.find()) {
            count++;
        }

        return count;
    }
}
