package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class TernaryCondition extends ACSLPredicate {

  private final ACSLPredicate condition;
  private final ACSLPredicate then;
  private final ACSLPredicate otherwise;

  public TernaryCondition(ACSLPredicate p1, ACSLPredicate p2, ACSLPredicate p3) {
    super();
    condition = p1;
    then = p2;
    otherwise = p3;
  }

  @Override
  public String toString() {
    return condition.toString() + " ? " + then.toString() + " : " + otherwise.toString();
  }

  @Override
  public ACSLPredicate getCopy() {
    return new TernaryCondition(condition.getCopy(), then.getCopy(), otherwise.getCopy());
  }

  @Override
  public ACSLPredicate toPureC() {
    return new TernaryCondition(condition.toPureC(), then.toPureC(), otherwise.toPureC());
  }

  @Override
  public ACSLPredicate simplify() {
    return new TernaryCondition(condition.simplify(), then.simplify(), otherwise.simplify());
  }

  @Override
  public boolean equals(Object obj) {
    return equalsExceptNegation(obj, true);
  }

  @Override
  public int hashCode() {
    int sign = isNegated() ? -1 : 1;
    return sign * (19 * condition.hashCode() + 11 * then.hashCode() + otherwise.hashCode());
  }

  private boolean equalsExceptNegation(Object o, boolean shouldNegationMatch) {
    if (o instanceof TernaryCondition) {
      TernaryCondition other = (TernaryCondition) o;
      if (shouldNegationMatch == (isNegated() == other.isNegated())) {
        return condition.equals(other.condition)
            && then.equals(other.then)
            && otherwise.equals(other.otherwise);
      }
    }
    return false;
  }

  @Override
  public boolean isNegationOf(ACSLPredicate other) {
    return equalsExceptNegation(other, false);
  }
}
