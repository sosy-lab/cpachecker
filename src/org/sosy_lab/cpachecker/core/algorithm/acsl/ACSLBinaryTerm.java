package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Preconditions;

public class ACSLBinaryTerm implements ACSLTerm {

    private final ACSLTerm left;
    private final ACSLTerm right;
    private final BinaryOperator operator;

    public ACSLBinaryTerm(ACSLTerm pLeft, ACSLTerm pRight, BinaryOperator op) {
        left = pLeft;
        right = pRight;
        Preconditions.checkArgument(BinaryOperator.isBitwiseOperator(op) || BinaryOperator.isArithmeticOperator(op),
                "Unknown comparison operator: %s", op);
        operator = op;
    }

    @Override
    public String toString() {
        return left.toString() + operator.toString() + right.toString();
    }

    @Override
    public ACSLTerm toPureC() {
        ACSLTerm pureLeft = left.toPureC();
        ACSLTerm pureRight = right.toPureC();
        BinaryOperator newOperator = operator;
        switch(operator) {
            case PLUS:
            case MINUS:
            case DIVIDE:
            case TIMES:
            case MOD:
            case LSHIFT:
            case RSHIFT:
            case BAND:
            case BOR:
            case BXOR:
                //these are already C operators
                break;
            case BIMP:
                pureLeft = new ACSLUnaryTerm(pureLeft, UnaryOperator.BNEG);
                newOperator = BinaryOperator.BOR;
                break;
            case BEQV:
                pureLeft = new ACSLUnaryTerm(pureLeft, UnaryOperator.BNEG);
                newOperator = BinaryOperator.BXOR;
                break;
            default:
                throw new AssertionError("ACSLTerm should hold arithmetic or bitwise operation.");
        }
        return new ACSLBinaryTerm(pureLeft, pureRight, newOperator);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof ACSLBinaryTerm) {
            ACSLBinaryTerm other = (ACSLBinaryTerm) o;
            return left.equals(other.left) && right.equals(other.right) && operator.equals(other.operator);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * left.hashCode() + 17 * right.hashCode() + operator.hashCode();
    }
}
