package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ACSLLoopAnnotation implements ACSLAnnotation {

  private final LoopInvariant lInvariant;
  private final ImmutableMap<List<Behavior>, LoopInvariant> additionalInvariants;

  public ACSLLoopAnnotation(LoopInvariant invariant) {
    this(invariant, ImmutableMap.of());
  }

  public ACSLLoopAnnotation(Map<List<Behavior>, LoopInvariant> pAdditionalInvariants) {
    this(new LoopInvariant(ACSLPredicate.getTrue()), pAdditionalInvariants);
  }

  public ACSLLoopAnnotation(
      LoopInvariant invariant, Map<List<Behavior>, LoopInvariant> pAdditionalInvariants) {
    lInvariant = invariant;
    additionalInvariants = ImmutableMap.copyOf(pAdditionalInvariants);
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    ACSLPredicate predicateRepresentation = lInvariant.getPredicate();
    for (List<Behavior> behaviors : additionalInvariants.keySet()) {
      ACSLPredicate enclosingConjunction = ACSLPredicate.getTrue();
      ACSLPredicate enclosingDisjunction = ACSLPredicate.getFalse();
      for (Behavior behavior : behaviors) {
        AssumesClause assumesClause = behavior.getAssumesClause();
        enclosingConjunction = enclosingConjunction.and(assumesClause.getPredicate().negate());
        enclosingDisjunction = enclosingDisjunction.or(assumesClause.getPredicate());
      }
      enclosingConjunction = enclosingConjunction.simplify();
      enclosingDisjunction = enclosingDisjunction.simplify();
      ACSLPredicate behaviorRepresentation =
          new ACSLLogicalPredicate(
              enclosingDisjunction,
              additionalInvariants.get(behaviors).getPredicate(),
              BinaryOperator.AND);
      behaviorRepresentation =
          new ACSLLogicalPredicate(behaviorRepresentation, enclosingConjunction, BinaryOperator.OR);
      predicateRepresentation =
          new ACSLLogicalPredicate(
              predicateRepresentation, behaviorRepresentation, BinaryOperator.AND);
    }
    return predicateRepresentation.simplify();
  }

  @Override
  public ACSLPredicate getCompletenessPredicate() {
    return ACSLPredicate.getTrue();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(lInvariant.toString());
    if (!additionalInvariants.isEmpty()) {
      for (Entry<List<Behavior>, LoopInvariant> entry : additionalInvariants.entrySet()) {
        builder.append('\n').append("for ");
        Joiner.on(", ").appendTo(builder, entry.getKey().stream().map(x -> x.getName()).iterator());
        builder.append(':');
        builder.append(entry.getValue().toString());
      }
    }
    return builder.toString();
  }
}
