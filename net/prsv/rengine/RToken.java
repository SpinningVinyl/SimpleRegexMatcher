package net.prsv.rengine;

public class RToken {

    public enum RTokenType {
        LITERAL,
        ANY_CHAR,
        L_PAR,
        R_PAR,
        RANGE_START,
        RANGE_END,
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
