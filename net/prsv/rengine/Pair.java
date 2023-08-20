package net.prsv.rengine;

import java.util.Objects;

public class Pair {
    private final String state;
    private final Character symbol;

    public Pair(String state, Character symbol) {
        this.state = state;
        this.symbol = symbol;
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
        return state + "," + symbol;
    }
}