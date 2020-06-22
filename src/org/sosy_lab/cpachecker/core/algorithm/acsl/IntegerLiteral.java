package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class IntegerLiteral implements ACSLTerm {

    private final Integer literal;

    public IntegerLiteral(Integer i) {
        literal = i;
    }

    @Override
    public String toString() {
        return String.valueOf(literal);
    }

    @Override
    public ACSLTerm toPureC() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof IntegerLiteral) {
            IntegerLiteral other = (IntegerLiteral) o;
            return literal.equals(other.literal);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 23 * literal.hashCode();
    }
}
