package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Identifier implements ACSLTerm {

  private final String name;
  private final boolean useOldValue;
  // TODO: Needs a type! Perhaps use MemoryLocation instead altogether?
  // TODO 2: Identifiers can still hold non-C expressions if useOldValue is set

  public Identifier(String pName) {
    this(pName, false);
  }

  private Identifier(String pName, boolean pUseOldValue) {
    name = pName;
    useOldValue = pUseOldValue;
  }

  @Override
  public String toString() {
    if (useOldValue) {
      return "\\old(" + name + ")";
    }
    return name;
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
    return visitor.visit(this);
  }

  @Override
  public ACSLTerm useOldValues() {
    if (useOldValue) {
      return this;
    }
    return new Identifier(name, true);
  }
}
