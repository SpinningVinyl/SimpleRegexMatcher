package net.prsv.rengine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import net.prsv.rengine.RToken.RTokenType;

public class RegexParser {

    private static final Map<RTokenType, Integer> precedence;

    private static final Map<Character, RTokenType> specialChars;

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

    };

    // do not instantiate
    private RegexParser () {
    }

    private static List<RToken> tokenize(String pattern) {

        List<RToken> tokens = new ArrayList<RToken>();
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
            }

            if (specialChars.containsKey(c)) { // special character - create an operator token
                token = new RToken(specialChars.get(c), c);
            } else { // not a special character -- create a literal token
                token = new RToken(RTokenType.LITERAL, c);
            }
            tokens.add(token);
            position = position + 1;
        }

        // insert explicit concatenation tokens between literals
        for (int i = 0; i < tokens.size(); i ++) {
            RToken t = tokens.get(i);
            if (t.type == RTokenType.LITERAL && i + 1 < tokens.size()) {
                RToken t2 = tokens.get(i + 1);
                if (t2.type == RTokenType.LITERAL) {
                    tokens.add(i + 1, new RToken(RTokenType.CONCAT, '\u0000'));
                }
            }
        }
        return tokens;
    }

    public static List<RToken> postfix(String pattern) {
        List<RToken> tokens = tokenize(pattern);
        
        return null;
    }

}

