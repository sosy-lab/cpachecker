package org.sosy_lab.cpachecker.core.algorithm.acsl;

public enum BinaryOperator {
    AND("&&"), OR("||"), IMP("==>"), EQV("<==>"), XOR("^^"), EQ("=="), NEQ("!="), LEQ("<="),
    GEQ(">="), LT("<"), GT(">"), BAND("&"), BOR("|"), BIMP("-->"), BEQV("<-->"), BXOR("^"),
    PLUS("+"), MINUS("-"), TIMES("*"), DIVIDE("/"), MOD("%"), LSHIFT("<<"), RSHIFT(">>");

    private String operator;

    BinaryOperator(String s) {
        operator = s;
    }

    @Override
    public String toString() {
        return operator;
    }

    public static boolean isComparisonOperator(BinaryOperator op) {
        switch(op) {
            case EQ:
            case NEQ:
            case LEQ:
            case GEQ:
            case LT:
            case GT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isLogicOperator(BinaryOperator op) {
        switch (op) {
            case AND:
            case OR:
            case IMP:
            case EQV:
            case XOR:
                return true;
            default:
                return false;
        }
    }

    public static boolean isBitwiseOperator(BinaryOperator op) {
        switch (op) {
            case BAND:
            case BOR:
            case BIMP:
            case BEQV:
            case BXOR:
                return true;
            default:
                return false;
        }
    }

    public static boolean isArithmeticOperator(BinaryOperator op) {
        switch (op) {
            case PLUS:
            case MINUS:
            case TIMES:
            case DIVIDE:
            case MOD:
            case LSHIFT:
            case RSHIFT:
                return true;
            default:
                return false;
        }
    }
}
