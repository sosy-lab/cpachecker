package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

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
  public IntegerLiteral toPureC() {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof IntegerLiteral) {
      IntegerLiteral other = (IntegerLiteral) o;
      return literal.equals(other.literal);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 23 * literal.hashCode();
  }

  public Integer getLiteral() {
    return literal;
  }

  @Override
  public CExpression accept(ACSLToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(toPureC());
  }
}
