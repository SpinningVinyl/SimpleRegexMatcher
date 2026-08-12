package net.prsv.rengine;

import java.util.Objects;

public class Pair implements Comparable<Pair> {
    private final String state;
    private final Character symbol;

    public Pair(String state, Character symbol) {
        this.state = Objects.requireNonNull(state);
        this.symbol = Objects.requireNonNull(symbol);
    }

    public String getState() {
        return state;
    }

    public Character getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, symbol);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair that = (Pair) o;
        return this.state.equals(that.state) && this.symbol.equals(that.symbol);
    }

    @Override
    public String toString() {
        return this.state + ", " + this.symbol;
    }

    @Override
    public int compareTo(Pair other) {
        int stateComparison = Integer.compare(
                stateNumber(this.state),
                stateNumber(other.state)
        );

        if (stateComparison != 0) {
            return stateComparison;
        }

        return Character.compare(this.symbol, other.symbol);
    }

    private static int stateNumber(String state) {
        if (state.length() < 2 || state.charAt(0) != 'q') {
            throw new IllegalArgumentException("Invalid state name: " + state);
        }

        return Integer.parseInt(state.substring(1));
    }
}
