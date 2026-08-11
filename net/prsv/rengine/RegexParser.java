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
            throw new IllegalArgumentException("Parsing error: pattern can't be null");
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
            if (token != null) {
                temporaryTokenStream.add(token);
            }
            position = position + 1;
        }

        boolean inRange = false;
        List<RToken> tokens = new ArrayList<>();
        int idx = 0;

        /*
         * The parser handles character ranges by transforming them into groups/unions:
         * e.g. [abc] is transformed into (a|b|c) and [a-e] is transformed into (a|b|c|d|e)
         */
        while (idx < temporaryTokenStream.size()) {
            RToken t = temporaryTokenStream.get(idx);
            if (t.type == RTokenType.RANGE_START) {
                if (inRange) {
                    throw new IllegalArgumentException("Parsing error: unbalanced [");
                }
                inRange = true;
                // create the bracket/union expression
                tokens.add(new RToken(RTokenType.L_PAR, '('));
            } else if (t.type == RTokenType.RANGE_END) {
                if (!inRange) {
                    throw new IllegalArgumentException("Parsing error: unbalanced ]");
                }
                inRange = false;
                // if the last token before the closing bracket is a union, remove it
                if (tokens.get(tokens.size() - 1).type == RTokenType.UNION) {
                    tokens.remove(tokens.size() - 1);
                }
                // close the bracket
                t = new RToken(RTokenType.R_PAR, ')');
                tokens.add(t);
            } else if (inRange) {
                if (t.literal == '-' && idx > 0 && idx + 1 < temporaryTokenStream.size()) {
                    RToken next = temporaryTokenStream.get(idx + 1);
                    RToken prev = temporaryTokenStream.get(idx - 1);
                    if (next.type != RTokenType.RANGE_END && prev.type != RTokenType.RANGE_START) {
                        char fromChar = prev.literal;
                        char toChar = next.literal;
                        if (fromChar > toChar) {
                            throw new IllegalArgumentException("Parsing error: invalid range " + fromChar + "-" + toChar);
                        }
                        for (int code = fromChar; code <= toChar; code++) { // use int to prevent overflow
                            tokens.add(new RToken(RTokenType.LITERAL, (char) code));
                            tokens.add(new RToken(RTokenType.UNION, '|'));
                        }
                        idx += 1;
                    } else {
                        tokens.add(t);
                        tokens.add(new RToken(RTokenType.UNION, '|'));
                    }
                } else {
                    tokens.add(new RToken(RTokenType.LITERAL, t.literal));
                    tokens.add(new RToken(RTokenType.UNION, '|'));
                }
            } else {
                tokens.add(t);
            }
            // insert explicit concatenation tokens
            if (!inRange && idx + 1 < temporaryTokenStream.size()) {
                RToken t2 = temporaryTokenStream.get(idx + 1);
                if (t.type != RTokenType.L_PAR && t.type != RTokenType.UNION
                        && (t2.type == RTokenType.LITERAL || t2.type == RTokenType.L_PAR || t2.type == RTokenType.RANGE_START || t2.type == RTokenType.ANY_CHAR)) {
                    tokens.add(new RToken(RTokenType.CONCAT, '&'));
                }
            }
            idx += 1;
        }

        if (inRange) {
            throw new IllegalArgumentException("Parsing error: unbalanced [");
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
                        if (stack.peek() == null) {
                            throw new IllegalArgumentException("Parsing error: unbalanced parentheses");
                        }
                        if (stack.peek().type.equals(RTokenType.L_PAR)) break;
                        postfixStream.add(stack.pop());
                    }
                    stack.pop();
                    break;

                default:
                    while (!stack.isEmpty()) {
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

        while (!stack.isEmpty()) {
            RToken token = stack.pop();
            if (token.type == RTokenType.L_PAR) {
                throw new IllegalArgumentException("Parsing error: unbalanced parentheses");
            }
            postfixStream.add(token);
        }
        return postfixStream;
    }

}

