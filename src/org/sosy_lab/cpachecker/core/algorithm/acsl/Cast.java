package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Cast implements ACSLTerm {

  private final Type type;
  private final ACSLTerm term;

  public Cast(Type pType, ACSLTerm pTerm) {
    type = pType;
    term = pTerm;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Cast) {
      Cast other = (Cast) o;
      return type.equals(other.type) && term.equals(other.term);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 17 * term.hashCode() + type.hashCode();
  }

  @Override
  public String toString() {
    return "(" + type.toString() + ") " + term.toString();
  }

  public Type getType() {
    return type;
  }

  public ACSLTerm getTerm() {
    return term;
  }

  @Override
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public ACSLTerm useOldValues() {
    return new Cast(type, term.useOldValues());
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return term.isAllowedIn(clauseType);
  }
}
