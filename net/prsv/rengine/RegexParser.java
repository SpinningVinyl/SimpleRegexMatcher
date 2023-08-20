package net.prsv.rengine;

import java.util.*;

import net.prsv.rengine.RToken.RTokenType;

public class RegexParser {

    private static final Map<RTokenType, Integer> precedence = new HashMap<>();

    private static final Map<Character, RTokenType> specialChars = new HashMap<>();

    static {
        // populate the precedence map
        precedence.put(RTokenType.LPAR, 1);
        precedence.put(RTokenType.UNION, 2);
        precedence.put(RTokenType.CONCAT, 3);
        precedence.put(RTokenType.QMARK, 4);
        precedence.put(RTokenType.STAR, 4);
        precedence.put(RTokenType.PLUS, 4);

        // populate the special character map
        specialChars.put('(', RTokenType.LPAR);
        specialChars.put(')', RTokenType.RPAR);
        specialChars.put('?', RTokenType.QMARK);
        specialChars.put('*', RTokenType.STAR);
        specialChars.put('+', RTokenType.PLUS);
        specialChars.put('|', RTokenType.UNION);
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
        for (int i = 0; i < temporaryTokenStream.size(); i++) {
            RToken t = temporaryTokenStream.get(i);
            tokens.add(t);
            if (i + 1 < temporaryTokenStream.size()) {
                RToken t2 = temporaryTokenStream.get(i + 1);
                if (t.type != RTokenType.LPAR && t.type != RTokenType.UNION && (t2.type == RTokenType.LITERAL || t2.type == RTokenType.LPAR)) {
                    tokens.add(new RToken(RTokenType.CONCAT, '&'));
                }
            }
        }

        return tokens;

    }

    public static List<RToken> infixToPostfix(List<RToken> tokens) {
        Deque<RToken> stack = new ArrayDeque<>();
        List<RToken> postfixStream = new ArrayList<>();

        for (RToken t : tokens) {
            switch (t.type) {
                case LITERAL:
                    postfixStream.add(t);
                    break;
                case LPAR:
                    stack.push(t);
                    break;
                case RPAR:
                    while (true) {
                        assert stack.peek() != null;
                        if (stack.peek().type.equals(RTokenType.LPAR)) break;
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

