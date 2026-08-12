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
        PLUS,
        QUANTIFIER
    }

    public final RTokenType type;

    public final char literal;

    public final int min;
    public final int max;


    public RToken(RTokenType type, char symbol) {
        if (type == RTokenType.QUANTIFIER) {
            throw new IllegalArgumentException("Use the other constructor to create a QUANTIFIER token");
        }
        this.type = type;
        literal = symbol;
        this.min = 0;
        this.max = 0;
    }

    /*
     max == -1 means that there is no upper limit -- the quantifier is unbounded (e.g. {3+})
     */
    public RToken(int min, int max) {
        if (min < 0) {
            throw new IllegalArgumentException("Quantifier minimum cannot be negative");
        }
        if (max != -1 && max < min) {
            throw new IllegalArgumentException("Quantifier maximum cannot be less than minimum");
        }
        this.type = RTokenType.QUANTIFIER;
        this.min = min;
        this.max = max;
        this.literal = '\0';
    }

    public boolean isUnbounded() {
        return type == RTokenType.QUANTIFIER && max == -1;
    }

}
