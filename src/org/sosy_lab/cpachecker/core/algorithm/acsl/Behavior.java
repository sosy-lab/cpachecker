package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class Behavior {

  private final String name;
  private final EnsuresClause ensuresClause;
  private final RequiresClause requiresClause;
  private final AssumesClause assumesClause;

  public Behavior(String pName, EnsuresClause ens, RequiresClause req, AssumesClause ass) {
    name = pName;
    ensuresClause = ens;
    requiresClause = req;
    assumesClause = ass;
  }

  public String getName() {
    return name;
  }

  public AssumesClause getAssumesClause() {
    return assumesClause;
  }

  public ACSLPredicate getPreStatePredicate() {
    ACSLPredicate requiresPredicate = requiresClause.getPredicate();
    ACSLPredicate assumesPredicate = assumesClause.getPredicate();
    ACSLPredicate negatedAssumesPredicate = assumesPredicate.negate();
    return new ACSLLogicalPredicate(
        new ACSLLogicalPredicate(assumesPredicate, requiresPredicate, BinaryOperator.AND),
        negatedAssumesPredicate,
        BinaryOperator.OR);
  }

  public ACSLPredicate getPostStatePredicate() {
    ACSLPredicate ensuresPredicate = ensuresClause.getPredicate();
    ACSLPredicate assumesPredicate = assumesClause.getPredicate().useOldValues();
    ACSLPredicate negatedAssumesPredicate = assumesPredicate.negate();
    return new ACSLLogicalPredicate(
        new ACSLLogicalPredicate(assumesPredicate, ensuresPredicate, BinaryOperator.AND),
        negatedAssumesPredicate,
        BinaryOperator.OR);
  }

  @Override
  public String toString() {
    return "behavior "
        + name
        + ":\n"
        + assumesClause.toString()
        + '\n'
        + requiresClause.toString()
        + '\n'
        + ensuresClause.toString();
  }
}
