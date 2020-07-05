package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Identifier implements ACSLTerm {

  private final String name;
  private boolean useOldValue;
  // TODO: Needs a type! Perhaps use MemoryLocation instead altogether?

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
  public Identifier toPureC() {
    //TODO: This should not use old value, but the generous amount of calls to toPureC() would
    // currently remove the flag even if it is desired
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
    return visitor.visit(this);
  }

  @Override
  public ACSLTerm useOldValues() {
    return new Identifier(name, true);
  }
}
