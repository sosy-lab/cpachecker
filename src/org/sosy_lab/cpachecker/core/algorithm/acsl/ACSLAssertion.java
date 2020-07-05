package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class ACSLAssertion implements ACSLAnnotation {

  private final AssertionKind kind;
  private final ACSLPredicate predicate;

  public ACSLAssertion(AssertionKind pKind, ACSLPredicate p) {
    kind = pKind;
    predicate = p.simplify();
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    return predicate;
  }

  @Override
  public String toString() {
    return kind.toString() + ' ' + predicate.toString() + ';';
  }

  public enum AssertionKind {
    ASSERT("assert"),
    CHECK("check");

    private final String name;

    AssertionKind(String pName) {
      name = pName;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
