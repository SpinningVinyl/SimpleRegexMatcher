package net.prsv.rengine;

import java.util.*;

import static net.prsv.rengine.RToken.RTokenType;

public class RegexParser {

    private static final Map<RToken.RTokenType, Integer> precedence = new HashMap<>();

    private static final Map<Character, RTokenType> specialChars = new HashMap<>();

    static {
        // populate the precedence map
        precedence.put(RTokenType.L_PAR, 1);
        precedence.put(RTokenType.UNION, 2);
        precedence.put(RTokenType.CONCAT, 3);
        precedence.put(RTokenType.QMARK, 4);
        precedence.put(RTokenType.STAR, 4);
        precedence.put(RTokenType.PLUS, 4);

        // populate the special character map
        specialChars.put('(', RTokenType.L_PAR);
        specialChars.put(')', RTokenType.R_PAR);
        specialChars.put('[', RTokenType.RANGE_START);
        specialChars.put(']', RTokenType.RANGE_END);
        specialChars.put('?', RTokenType.QMARK);
        specialChars.put('*', RTokenType.STAR);
        specialChars.put('+', RTokenType.PLUS);
        specialChars.put('|', RTokenType.UNION);
        specialChars.put('.', RTokenType.ANY_CHAR);
    }

    // do not instantiate
    private RegexParser () {
    }

    public static List<RToken> tokenize(String pattern) {

        if (pattern == null) {
            throw new IllegalArgumentException("Regex parser: pattern can't be null");
        }

        ArrayList<RToken> temporaryTokenStream = new ArrayList<>();

        int position = 0;

        // go over the whole pattern and tokenize it
        while (position < pattern.length()) {

            char c = pattern.charAt(position);
            RToken token = null;

            // if we encounter a backslash
            if (c == '\\') {
                if (position + 1 < pattern.length()) {
                    char c2 = pattern.charAt(position + 1);
                    // if the next character in the pattern is a special character or another slash, 
                    if (specialChars.containsKey(c2) || c2 == c) {
                        // create a new literal token and skip the next character
                        token = new RToken(RTokenType.LITERAL, c2);
                        position = position + 1;
                    }
                // otherwise do nothing -- swallow the backslash
                }
            } else {
                // if a special character -- create an operator token
                // if not a special character -- create a literal token
                token = new RToken(specialChars.getOrDefault(c, RTokenType.LITERAL), c);
            }
            temporaryTokenStream.add(token);
            position = position + 1;
        }

        List<RToken> tokens = new ArrayList<>();

        // insert explicit concatenation tokens
        int i = 0;
        while (i < temporaryTokenStream.size()) {
            RToken t = temporaryTokenStream.get(i);
            if (t.type == RTokenType.RANGE_START) {
                // search for RANGE_END
                int rangeEndPos = -1;
                for (int k = 1; k < temporaryTokenStream.size() - i; k++) {
                    if (temporaryTokenStream.get(i + k).type == RTokenType.RANGE_END) {
                        rangeEndPos = i + k;
                        break;
                    }
                }
                if (rangeEndPos == -1) {
                    throw new IllegalArgumentException("Invalid pattern: unbalanced '['");
                }
                int rangeBeginPos = i;
                tokens.add(new RToken(RTokenType.L_PAR, '('));
                i += 1;
                RToken next, prev, current;
                while (i < rangeEndPos) {
                    current = temporaryTokenStream.get(i);
                    if (current.literal == '-' && i + 1 < rangeEndPos && i - 1 > rangeBeginPos) {
                        prev = temporaryTokenStream.get(i - 1);
                        next = temporaryTokenStream.get(i + 1);
                        char fromChar = prev.literal;
                        char toChar = next.literal;
                        for (char c = fromChar; c < toChar; c++) {
                            tokens.add(new RToken(RTokenType.LITERAL, c));
                            tokens.add(new RToken(RTokenType.UNION, '|'));
                        }
                        tokens.add(new RToken(RTokenType.LITERAL, toChar));
                        i += 1;
                    } else {
                        tokens.add(current);
                        if (i + 1 < rangeEndPos) {
                            tokens.add(new RToken(RTokenType.UNION, '|'));
                        }
                    }
                    i += 1;
                }

                tokens.add(new RToken(RTokenType.R_PAR, ')'));
            } else {
                tokens.add(t);
            }
            if (i + 1 < temporaryTokenStream.size()) {
                RToken t2 = temporaryTokenStream.get(i + 1);
                if (t.type != RTokenType.L_PAR && t.type != RTokenType.UNION
                        && (t2.type == RTokenType.LITERAL || t2.type == RTokenType.L_PAR || t2.type == RTokenType.ANY_CHAR)) {
                    tokens.add(new RToken(RTokenType.CONCAT, '&'));
                }
            }
            i += 1;
        }

        return tokens;

    }

    public static List<RToken> infixToPostfix(List<RToken> tokens) {
        Deque<RToken> stack = new ArrayDeque<>();
        List<RToken> postfixStream = new ArrayList<>();

        for (RToken t : tokens) {
            switch (t.type) {
                case LITERAL:
                case ANY_CHAR:
                    postfixStream.add(t);
                    break;
                case L_PAR:
                    stack.push(t);
                    break;
                case R_PAR:
                    while (true) {
                        assert stack.peek() != null;
                        if (stack.peek().type.equals(RTokenType.L_PAR)) break;
                        postfixStream.add(stack.pop());
                    }
                    stack.pop();
                    break;

                default:
                    while (stack.size() > 0) {
                        RToken topToken = stack.peek();
                        if (precedence.get(topToken.type) >= precedence.get(t.type)) {
                            postfixStream.add(stack.pop());
                        } else {
                            break;
                        }
                    }
                    stack.push(t);
                    break;
            }
        }

        while (stack.size() > 0) {
            postfixStream.add(stack.pop());
        }
        return postfixStream;
    }

}

