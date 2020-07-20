package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ACSLBinaryTerm implements ACSLTerm {

  private final ACSLTerm left;
  private final ACSLTerm right;
  private final BinaryOperator operator;

  public ACSLBinaryTerm(ACSLTerm pLeft, ACSLTerm pRight, BinaryOperator op) {
    assert BinaryOperator.isArithmeticOperator(op)
            || BinaryOperator.isBitwiseOperator(op)
            || BinaryOperator.isComparisonOperator(op)
        : String.format(
            "ACSLTerm may only hold arithmetic, bitwise or comparison operator, %s is neither", op);
    switch (op) {
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
      case EQ:
      case NEQ:
      case LEQ:
      case GEQ:
      case LT:
      case GT:
        left = pLeft;
        right = pRight;
        operator = op;
        break;
      case BIMP:
        left = new ACSLUnaryTerm(pLeft, UnaryOperator.BNEG);
        right = pRight;
        operator = BinaryOperator.BOR;
        break;
      case BEQV:
        left = new ACSLUnaryTerm(pLeft, UnaryOperator.BNEG);
        right = pRight;
        operator = BinaryOperator.BXOR;
        break;
      default:
        throw new AssertionError("Unknown operator: " + op);
    }
  }

  @Override
  public String toString() {
    return left.toString() + operator.toString() + right.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLBinaryTerm) {
      ACSLBinaryTerm other = (ACSLBinaryTerm) o;
      return left.equals(other.left)
          && right.equals(other.right)
          && operator.equals(other.operator);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * left.hashCode() + 17 * right.hashCode() + operator.hashCode();
  }

  public ACSLTerm getLeft() {
    return left;
  }

  public ACSLTerm getRight() {
    return right;
  }

  public BinaryOperator getOperator() {
    return operator;
  }

  @Override
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public ACSLTerm useOldValues() {
    return new ACSLBinaryTerm(left.useOldValues(), right.useOldValues(), operator);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return left.isAllowedIn(clauseType) && right.isAllowedIn(clauseType);
  }
}
