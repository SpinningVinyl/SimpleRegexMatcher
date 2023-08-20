package net.prsv.rengine;

public class RToken {

    public enum RTokenType {
        LITERAL,
        LPAR,
        RPAR,
        UNION,
        CONCAT,
        QMARK,
        STAR,
        PLUS
    }

    public final RTokenType type;

    public final char literal;

    public RToken(RTokenType type, char symbol) {
        this.type = type;
        literal = symbol;
    }

    
}
