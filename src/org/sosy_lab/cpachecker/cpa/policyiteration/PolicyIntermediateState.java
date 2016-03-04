package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.solver.api.BooleanFormula;

public final class PolicyIntermediateState extends PolicyState implements Partitionable {

  /**
   * Formula + SSA associated with the state.
   */
  private final PathFormula pathFormula;

  /**
   * Abstract state representing start of the trace.
   */
  private final PolicyAbstractedState startingAbstraction;

  /**
   * Meta-information for determining the coverage.
   */
  private transient PolicyIntermediateState mergedInto;

  private transient BooleanFormula extraInvariant;

  private PolicyIntermediateState(
      CFANode node,
      PathFormula pPathFormula,
      PolicyAbstractedState pStartingAbstraction
      ) {
    super(node);

    pathFormula = pPathFormula;
    startingAbstraction = pStartingAbstraction;
    extraInvariant = getGeneratingState().getExtraInvariant();
  }

  public static PolicyIntermediateState of(
      CFANode node,
      PathFormula pPathFormula,
      PolicyAbstractedState generatingState
  ) {
    return new PolicyIntermediateState(
        node, pPathFormula, generatingState);
  }

  public void setPartitionKey(BooleanFormula pExtraInvariant) {
    extraInvariant = pExtraInvariant;
  }

  public void resetPartitionKey() {
    extraInvariant = startingAbstraction.getExtraInvariant();
  }

  public void setMergedInto(PolicyIntermediateState other) {
    mergedInto = other;
  }

  public boolean isMergedInto(PolicyIntermediateState other) {
    return other == mergedInto;
  }

  /**
   * @return Starting {@link PolicyAbstractedState} for the starting location.
   */
  public PolicyAbstractedState getGeneratingState() {
    return startingAbstraction;
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

  @Override
  public Object getPartitionKey() {
    return extraInvariant;
  }
}
