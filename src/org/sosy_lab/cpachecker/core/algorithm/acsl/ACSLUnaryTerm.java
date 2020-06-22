package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class ACSLUnaryTerm implements ACSLTerm{

    private final ACSLTerm term;
    private final UnaryOperator operator;

    public ACSLUnaryTerm(ACSLTerm pTerm, UnaryOperator op) {
        term = pTerm;
        operator = op;
    }

    @Override
    public String toString() {
        if (operator.equals(UnaryOperator.SIZEOF)) {
            return operator.toString() + "(" + term.toString() + ")";
        }
        return operator.toString() + term.toString();
    }

    @Override
    public ACSLTerm toPureC() {
        return new ACSLUnaryTerm(term.toPureC(), operator);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof ACSLUnaryTerm) {
            ACSLUnaryTerm other = (ACSLUnaryTerm) o;
            return term.equals(other.term) && operator.equals(other.operator);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 7 * term.hashCode() + operator.hashCode();
    }
}
