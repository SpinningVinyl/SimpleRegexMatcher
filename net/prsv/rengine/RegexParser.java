package net.prsv.rengine;

import java.util.*;

import net.prsv.rengine.RToken.RTokenType;

public class RegexParser {

    private final String pattern;
    private static final Map<RTokenType, Integer> precedence;

    private static final Map<Character, RTokenType> specialChars;

    private List<RToken> tokens = new ArrayList<>();

    static {
        Map<RTokenType, Integer> tokenPrecedenceMap = new HashMap<>();
        tokenPrecedenceMap.put(RTokenType.LPAR, 1);
        tokenPrecedenceMap.put(RTokenType.UNION, 2);
        tokenPrecedenceMap.put(RTokenType.CONCAT, 3);
        tokenPrecedenceMap.put(RTokenType.Q, 4);
        tokenPrecedenceMap.put(RTokenType.STAR, 4);
        tokenPrecedenceMap.put(RTokenType.PLUS, 4);
        precedence = Collections.unmodifiableMap(tokenPrecedenceMap);

        Map<Character, RTokenType> specialCharMap = new HashMap<>();
        specialCharMap.put('(', RTokenType.LPAR);
        specialCharMap.put(')', RTokenType.RPAR);
        specialCharMap.put('?', RTokenType.Q);
        specialCharMap.put('*', RTokenType.STAR);
        specialCharMap.put('+', RTokenType.PLUS);
        specialCharMap.put('|', RTokenType.UNION);
        specialChars = Collections.unmodifiableMap(specialCharMap);

    }

    // do not instantiate
    public RegexParser (String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Regex parser: pattern can't be null");
        }
        this.pattern = pattern;
        tokenize();
        inToPost();
    }

    private void tokenize() {

        ArrayList<RToken> tokenStream = new ArrayList<>();

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
            tokenStream.add(token);
            position = position + 1;
        }

        // insert explicit concatenation tokens
        for (int i = 0; i < tokenStream.size(); i++) {
            RToken t = tokenStream.get(i);
            tokens.add(t);
            if (i + 1 < tokenStream.size()) {
                RToken t2 = tokenStream.get(i + 1);
                if (t.type != RTokenType.LPAR && t.type != RTokenType.UNION && t2.type == RTokenType.LITERAL) {
                    tokens.add(new RToken(RTokenType.CONCAT, '.'));
                }
            }
        }
    }

    private void inToPost() {
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
        tokens = postfixStream;
    }

    public List<RToken> tokenStream() {
        return tokens;
    }


}

