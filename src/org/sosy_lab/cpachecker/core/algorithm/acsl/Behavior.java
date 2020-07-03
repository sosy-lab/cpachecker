package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class Behavior {

  private String name;
  private EnsuresClause ensuresClause;
  private RequiresClause requiresClause;
  private AssumesClause assumesClause;

  // this should be a valid C expression
  private ACSLPredicate predicateRepresentation;

  public Behavior(String pName, EnsuresClause ens, RequiresClause req, AssumesClause ass) {
    name = pName;
    ensuresClause = ens;
    requiresClause = req;
    assumesClause = ass;
  }

  public String getName() {
    return name;
  }

  private void makePredicateRepresentation() {
    ACSLPredicate inner;
    ACSLPredicate left = ensuresClause.getPredicate();
    if (left != ACSLPredicate.getTrue()) {
      ACSLPredicate right = requiresClause.getPredicate();
      if (right != ACSLPredicate.getTrue()) {
        right.negate();
        inner = new ACSLLogicalPredicate(left, right, BinaryOperator.OR);
      } else {
        inner = left;
      }
    } else {
      inner = ACSLPredicate.getTrue();
    }
    ACSLPredicate assumesPredicate = assumesClause.getPredicate();
    ACSLPredicate negatedAssumesPredicate = assumesPredicate.getCopy().negate();
    predicateRepresentation =
        new ACSLLogicalPredicate(
            new ACSLLogicalPredicate(assumesPredicate, inner, BinaryOperator.AND),
            negatedAssumesPredicate,
            BinaryOperator.OR);
  }

  public ACSLPredicate getPredicateRepresentation() {
    return predicateRepresentation;
  }

  public AssumesClause getAssumesClause() {
    return assumesClause;
  }

  public void toPureC() {
    ensuresClause.toPureC();
    requiresClause.toPureC();
    assumesClause.toPureC();
    makePredicateRepresentation();
  }
}
