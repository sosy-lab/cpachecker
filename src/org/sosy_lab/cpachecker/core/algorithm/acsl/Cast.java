package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class Cast implements ACSLTerm {

  private final Type type;
  private final ACSLTerm term;

  public Cast(Type pType, ACSLTerm pTerm) {
    type = pType;
    term = pTerm;
  }

  @Override
  public ACSLTerm toPureC() {
    return new Cast(type, term.toPureC());
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
}
