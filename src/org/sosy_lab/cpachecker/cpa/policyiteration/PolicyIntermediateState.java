package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Map;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.collect.ImmutableMap;

public final class PolicyIntermediateState extends PolicyState {

  /**
   * Formula + SSA associated with the state.
   */
  private final PathFormula pathFormula;

  /**
   * Abstract states used for generating this state.
   *
   * locationID -> PolicyAbstractedState.
   */
  private final ImmutableMap<Integer, PolicyAbstractedState> generatingStates;

  /**
   * Meta-information for determining the coverage.
   */
  private transient PolicyIntermediateState mergedInto;

  private PolicyIntermediateState(
      CFANode node,
      PathFormula pPathFormula,
      Map<Integer, PolicyAbstractedState> pGeneratingStates
      ) {
    super(node);

    pathFormula = pPathFormula;
    generatingStates = ImmutableMap.copyOf(pGeneratingStates);
  }

  public static PolicyIntermediateState of(
      CFANode node,
      PathFormula pPathFormula,
      Map<Integer, PolicyAbstractedState> generatingStates
  ) {
    return new PolicyIntermediateState(
        node, pPathFormula, generatingStates);
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
  public ImmutableMap<Integer, PolicyAbstractedState> getGeneratingStates() {
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
    return "";
  }

  @Override
  public String toString() {
    return pathFormula.toString() + "\nLength: " + pathFormula.getLength();
  }
}
