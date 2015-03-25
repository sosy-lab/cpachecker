package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.collect.ImmutableSet;

public final class PolicyIntermediateState extends PolicyState {

  /**
   * Formula + SSA associated with the state.
   */
  private final PathFormula pathFormula;

  /**
   * Abstract states used for generating this state.
   */
  private final ImmutableSet<PolicyAbstractedState> generatingStates;

  private transient PolicyIntermediateState mergedInto;

  private PolicyIntermediateState(
      CFANode node,
      Set<Template> pTemplates,
      PathFormula pPathFormula,
      Set<PolicyAbstractedState> pGeneratingStates
      ) {
    super(pTemplates, node);

    pathFormula = pPathFormula;
    generatingStates = ImmutableSet.copyOf(pGeneratingStates);
  }

  public static PolicyIntermediateState of(
      CFANode node,
      Set<Template> pTemplates,
      PathFormula pPathFormula,
      Set<PolicyAbstractedState> generatingStates
  ) {
    return new PolicyIntermediateState(
        node, pTemplates, pPathFormula, generatingStates);
  }

  public void setMergedInto(PolicyIntermediateState other) {
    mergedInto = other;
  }

  public boolean isMergedInto(PolicyIntermediateState other) {
    return other == mergedInto;
  }

  /**
   * @return Starting {@link PathFormula} for possible starting locations.
   */
  public ImmutableSet<PolicyAbstractedState> getGeneratingStates() {
    return generatingStates;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public String toDOTLabel() {
    return String.format(
        "%n%s%n%s%n", pathFormula, pathFormula.getSsa()
    );
  }

  @Override
  public String toString() {
    return pathFormula.toString();
  }
}
