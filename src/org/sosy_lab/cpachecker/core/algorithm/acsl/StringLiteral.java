package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class StringLiteral implements ACSLTerm {

    private final String literal;

    public StringLiteral(String s) {
        literal = s;
    }

    @Override
    public String toString() {
        return literal;
    }

    @Override
    public ACSLTerm toPureC() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof StringLiteral) {
            StringLiteral other = (StringLiteral) o;
            return literal.equals(other.literal);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 23 * literal.hashCode();
    }
}
