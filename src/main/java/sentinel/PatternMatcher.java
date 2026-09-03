package sentinel;

import java.util.regex.Pattern;

/**
 * Glob-style wildcard pattern matcher for JVM internal class names.
 * <p>
 * Supports two wildcards:
 * <ul>
 *   <li>{@code *}  — matches any sequence of characters <em>within</em> a single package segment
 *       (does not cross {@code /} boundaries)</li>
 *   <li>{@code **} — matches any sequence of characters including {@code /} separators
 *       (crosses package boundaries)</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>
 *   demo/target/*        matches demo/target/TargetService, demo/target/FooService
 *   com/example/**       matches com/example/Foo, com/example/sub/Bar
 *   **&#47;Service         matches any class named Service in any package
 *   demo/target/Target*  matches demo/target/TargetService, demo/target/TargetRepo
 * </pre>
 *
 * @author JOJIN JOHN
 */
public final class PatternMatcher {

    private final Pattern regex;
    private final String original;

    /**
     * Compiles a glob pattern into an internal regex {@link Pattern}.
     *
     * @param glob the glob pattern (slash-separated, supports {@code *} and {@code **})
     */
    public PatternMatcher(String glob) {
        this.original = glob;
        this.regex    = toPattern(glob);
    }

    /**
     * Tests whether the given internal class name matches this pattern.
     *
     * @param internalClassName the JVM internal class name (e.g., {@code demo/target/TargetService})
     * @return {@code true} if the class name matches this pattern
     */
    public boolean matches(String internalClassName) {
        if (internalClassName == null) {
            return false;
        }
        return regex.matcher(internalClassName).matches();
    }

    /**
     * Returns the original glob string this matcher was compiled from.
     *
     * @return original glob string
     */
    public String getPattern() {
        return original;
    }

    @Override
    public String toString() {
        return "PatternMatcher{" + original + "}";
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private static Pattern toPattern(String glob) {
        StringBuilder sb = new StringBuilder("^");
        int i = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            if (c == '*' && i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                // ** — match anything including slashes
                sb.append(".*");
                i += 2;
                // skip optional trailing slash after **
                if (i < glob.length() && glob.charAt(i) == '/') {
                    i++;
                }
            } else if (c == '*') {
                // * — match anything except slash
                sb.append("[^/]*");
                i++;
            } else if (c == '?') {
                sb.append("[^/]");
                i++;
            } else {
                // Escape regex meta-characters
                sb.append(Pattern.quote(String.valueOf(c)));
                i++;
            }
        }
        sb.append("$");
        return Pattern.compile(sb.toString());
    }
}
