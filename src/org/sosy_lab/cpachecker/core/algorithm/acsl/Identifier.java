package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Identifier implements ACSLTerm {

  public static final String RESULT = "\\result";

  private final String name;
  private final String functionName;
  private final boolean useOldValue;
  // TODO: Needs a type! Perhaps use MemoryLocation instead altogether?
  // TODO 2: Identifiers can still hold non-C expressions if useOldValue is set

  public Identifier(String pName, String pFunctionName) {
    this(pName, pFunctionName, false);
  }

  private Identifier(String pName, String pFunctionName, boolean pUseOldValue) {
    name = pName;
    functionName = pFunctionName;
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
    return (29 * name.hashCode() * name.hashCode() + 29) * Boolean.hashCode(useOldValue);
  }

  public String getName() {
    return name;
  }

  public String getFunctionName() {
    return functionName;
  }

  public boolean shouldUseOldValue() {
    return useOldValue;
  }

  @Override
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public ACSLTerm useOldValues() {
    if (name.equals(RESULT)) {
      throw new UnsupportedOperationException(
          "\\old should not be used on term containing \\result");
    }
    if (useOldValue) {
      return this;
    }
    return new Identifier(name, functionName, true);
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    if (useOldValue || name.equals(RESULT)) {
      return clauseType.equals(EnsuresClause.class);
    }
    return true;
  }
}
