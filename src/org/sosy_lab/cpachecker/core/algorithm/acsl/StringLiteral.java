package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

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
  public boolean equals(Object o) {
    if (o instanceof StringLiteral) {
      StringLiteral other = (StringLiteral) o;
      return literal.equals(other.literal);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 23 * literal.hashCode() * literal.hashCode() + 23;
  }

  public String getLiteral() {
    return literal;
  }

  @Override
  public CExpression accept(ACSLToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public ACSLTerm useOldValues() {
    return this;
  }
}
