package net.prsv.rengine;

import java.util.List;

public final class RegexQuantifierTest {

    private static int assertions;

    private RegexQuantifierTest() {
    }

    private static StateMachine compile(String pattern) {
        return NFABuilder.build(
                RegexParser.infixToPostfix(RegexParser.tokenize(pattern))
        );
    }

    private static void assertMatches(String pattern,
                                      String[] accepted,
                                      String[] rejected) {
        StateMachine machine = compile(pattern);
        for (String input : accepted) {
            assertions++;
            if (!machine.run(input)) {
                throw new AssertionError(
                        "Expected pattern " + pattern + " to accept: " + input
                );
            }
        }
        for (String input : rejected) {
            assertions++;
            if (machine.run(input)) {
                throw new AssertionError(
                        "Expected pattern " + pattern + " to reject: " + input
                );
            }
        }
    }

    private static void assertQuantifierToken(String pattern, int min, int max) {
        List<RToken> postfix = RegexParser.infixToPostfix(RegexParser.tokenize(pattern));
        RToken token = postfix.get(postfix.size() - 1);
        assertions++;
        if (token.type != RToken.RTokenType.QUANTIFIER
                || token.min != min
                || token.max != max) {
            throw new AssertionError(
                    "Unexpected quantifier token for " + pattern
                            + ": type=" + token.type
                            + ", min=" + token.min
                            + ", max=" + token.max
            );
        }
    }

    private static void assertCompilationFails(String pattern) {
        assertions++;
        try {
            compile(pattern);
            throw new AssertionError("Expected compilation to fail for: " + pattern);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void assertEquivalentForShortInputs(String leftPattern,
                                                       String rightPattern,
                                                       String alphabet,
                                                       int maximumLength) {
        StateMachine left = compile(leftPattern);
        StateMachine right = compile(rightPattern);
        compareInputs(leftPattern, rightPattern, left, right, alphabet, "", maximumLength);
    }

    private static void compareInputs(String leftPattern,
                                      String rightPattern,
                                      StateMachine left,
                                      StateMachine right,
                                      String alphabet,
                                      String input,
                                      int remainingLength) {
        assertions++;
        boolean leftResult = left.run(input);
        boolean rightResult = right.run(input);
        if (leftResult != rightResult) {
            throw new AssertionError(
                    "Patterns " + leftPattern + " and " + rightPattern
                            + " disagree on input: " + input
            );
        }

        if (remainingLength == 0) {
            return;
        }
        for (int i = 0; i < alphabet.length(); i++) {
            compareInputs(leftPattern,
                    rightPattern,
                    left,
                    right,
                    alphabet,
                    input + alphabet.charAt(i),
                    remainingLength - 1);
        }
    }

    private static void testQuantifierTokens() {
        assertQuantifierToken("a{5}", 5, 5);
        assertQuantifierToken("a{2-7}", 2, 7);
        assertQuantifierToken("a{5+}", 5, -1);
    }

    private static void testExactQuantifiers() {
        assertMatches("a{1}",
                new String[]{"a"},
                new String[]{"", "aa", "b"});
        assertMatches("a{5}",
                new String[]{"aaaaa"},
                new String[]{"", "a", "aaaa", "aaaaaa"});
        assertMatches("(ab){3}",
                new String[]{"ababab"},
                new String[]{"", "ab", "abab", "abababa"});
        assertMatches("(a|b){2}",
                new String[]{"aa", "ab", "ba", "bb"},
                new String[]{"", "a", "aaa", "ac"});
    }

    private static void testBoundedQuantifiers() {
        assertMatches("a{2-5}",
                new String[]{"aa", "aaa", "aaaa", "aaaaa"},
                new String[]{"", "a", "aaaaaa", "b"});
        assertMatches("(ab|c){2-3}",
                new String[]{"abab", "abc", "cab", "cc", "ababab", "abcc", "ccc"},
                new String[]{"", "ab", "c", "abababab", "ac"});
        assertMatches(".{2-3}",
                new String[]{"ab", "xyz", "12"},
                new String[]{"", "a", "abcd"});
    }

    private static void testUnboundedQuantifiers() {
        assertMatches("a{3+}",
                new String[]{"aaa", "aaaa", "aaaaaaaa"},
                new String[]{"", "a", "aa", "aaab"});
        assertMatches("(ab){2+}",
                new String[]{"abab", "ababab", "abababab"},
                new String[]{"", "ab", "aba", "abababc"});
        assertMatches("[0-9a-f]{2+}",
                new String[]{"00", "af", "deadbeef", "123abc"},
                new String[]{"", "0", "ag", "0g"});
    }

    private static void testZeroBounds() {
        assertMatches("a{0}",
                new String[]{""},
                new String[]{"a", "aa"});
        assertMatches("a{0-3}",
                new String[]{"", "a", "aa", "aaa"},
                new String[]{"aaaa", "b"});
        assertMatches("a{0+}",
                new String[]{"", "a", "aa", "aaaaaa"},
                new String[]{"b", "aab"});
        assertMatches("x(ab){0}y",
                new String[]{"xy"},
                new String[]{"xaby", "x", "y"});
    }

    private static void testCompositionAndEpsilonOperands() {
        assertMatches("xa{2-4}y",
                new String[]{"xaay", "xaaay", "xaaaay"},
                new String[]{"xy", "xay", "xaaaaay", "aa"});
        assertMatches("a{2}|b",
                new String[]{"aa", "b"},
                new String[]{"", "a", "ab", "bb"});
        assertMatches("(a?){2}",
                new String[]{"", "a", "aa"},
                new String[]{"aaa", "b"});
        assertMatches("(a*){2}",
                new String[]{"", "a", "aa", "aaaaa"},
                new String[]{"b", "aab"});
        assertMatches("(ab+){2}",
                new String[]{"abab", "abbabbb", "abbbbbab"},
                new String[]{"", "ab", "aba", "abba"});
        assertMatches("a{2}{3}",
                new String[]{"aaaaaa"},
                new String[]{"", "aa", "aaa", "aaaaaaaa"});
        assertMatches(".{3}",
                new String[]{"abc", "123", "..."},
                new String[]{"", "ab", "abcd"});
    }

    private static void testRangesAndLiteralBraces() {
        assertMatches("[0-9a-f]{2-7}",
                new String[]{"00", "af", "deadbe", "1234567"},
                new String[]{"", "0", "12345678", "ag", "0g"});
        assertMatches("[{}]",
                new String[]{"{", "}"},
                new String[]{"", "{}", "a"});
        assertMatches("\\{2\\}",
                new String[]{"{2}"},
                new String[]{"", "2", "{{"});
    }

    private static void testExistingOperatorsStillWork() {
        assertMatches("ab*c",
                new String[]{"ac", "abc", "abbbc"},
                new String[]{"", "ab", "abb"});
        assertMatches("ab+c",
                new String[]{"abc", "abbbc"},
                new String[]{"ac", "ab", "abb"});
        assertMatches("ab?c",
                new String[]{"ac", "abc"},
                new String[]{"abbc", "ab"});
        assertMatches("a|bc",
                new String[]{"a", "bc"},
                new String[]{"", "b", "abc"});
    }

    private static void testMalformedQuantifiers() {
        assertCompilationFails("{2}");
        assertCompilationFails("a{}");
        assertCompilationFails("a{3-2}");
        assertCompilationFails("a{2-3-}");
        assertCompilationFails("a{2");
        assertCompilationFails("a2}");
        assertCompilationFails("a{2147483648}");
    }

    private static void testExpansionEquivalenceExhaustively() {
        assertEquivalentForShortInputs(
                "(a|b){3}",
                "(a|b)(a|b)(a|b)",
                "abc",
                4
        );
        assertEquivalentForShortInputs(
                "(ab|c){1-3}",
                "(ab|c)(ab|c)?(ab|c)?",
                "abc",
                6
        );
        assertEquivalentForShortInputs(
                "(ab|c){2+}",
                "(ab|c)(ab|c)(ab|c)*",
                "abc",
                7
        );
        assertEquivalentForShortInputs(
                "(a?){2-3}",
                "(a?)(a?)(a?)?",
                "ab",
                4
        );
    }

    private static void testLargeExactQuantifier() {
        String oneThousand = "a".repeat(1_000);
        assertMatches("a{1000}",
                new String[]{oneThousand},
                new String[]{oneThousand.substring(1), oneThousand + "a"});
    }

    public static void main(String[] args) {
        testQuantifierTokens();
        testExactQuantifiers();
        testBoundedQuantifiers();
        testUnboundedQuantifiers();
        testZeroBounds();
        testCompositionAndEpsilonOperands();
        testRangesAndLiteralBraces();
        testExistingOperatorsStillWork();
        testMalformedQuantifiers();
        testExpansionEquivalenceExhaustively();
        testLargeExactQuantifier();

        System.out.println("RegexQuantifierTest passed (" + assertions + " assertions)");
    }
}
