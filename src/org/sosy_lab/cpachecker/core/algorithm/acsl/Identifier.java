package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLBuiltin.Old;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Identifier implements ACSLTerm {

  private final String name;
  private final String functionName;
  // TODO: Needs a type! Perhaps use MemoryLocation instead altogether?

  public Identifier(String pName, String pFunctionName) {
    name = pName;
    functionName = pFunctionName;
  }

  @Override
  public String toString() {
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
    return 29 * name.hashCode() * name.hashCode() + 29;
  }

  public String getName() {
    return name;
  }

  public String getFunctionName() {
    return functionName;
  }

  @Override
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public ACSLTerm useOldValues() {
    return new Old(this);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return true;
  }
}
