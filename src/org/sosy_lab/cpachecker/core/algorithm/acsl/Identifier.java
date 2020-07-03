package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Identifier implements ACSLTerm {

  private final String name;
  // TODO: Needs a type! Perhaps use MemoryLocation instead altogether? (Currently no difference
  // between Identifier and StringLiteral!)

  public Identifier(String pName) {
    name = pName;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public Identifier toPureC() {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Identifier) {
      Identifier other = (Identifier) o;
      return name.equals(other.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 29 * name.hashCode();
  }

  public String getName() {
    return name;
  }

  @Override
  public CExpression accept(ACSLToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(toPureC());
  }
}
