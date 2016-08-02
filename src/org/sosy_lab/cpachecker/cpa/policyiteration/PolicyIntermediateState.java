package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import java.util.Objects;

public final class PolicyIntermediateState extends PolicyState {

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
  private transient int hashCache = 0;

  private PolicyIntermediateState(
      CFANode node,
      PathFormula pPathFormula,
      PolicyAbstractedState pStartingAbstraction) {
    super(node);

    pathFormula = pPathFormula;
    startingAbstraction = pStartingAbstraction;
  }

  public static PolicyIntermediateState of(
      CFANode node,
      PathFormula pPathFormula,
      PolicyAbstractedState generatingState
  ) {
    return new PolicyIntermediateState(
        node, pPathFormula, generatingState);
  }

  public PolicyIntermediateState withPathFormula(
      PathFormula pPathFormula
  ) {
    return new PolicyIntermediateState(
        getNode(), pPathFormula, startingAbstraction
    );
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
  public PolicyAbstractedState getBackpointerState() {
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
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof PolicyIntermediateState)) {
      return false;
    }
    PolicyIntermediateState that = (PolicyIntermediateState) pO;
    return Objects.equals(pathFormula, that.pathFormula) &&
        Objects.equals(startingAbstraction, that.startingAbstraction) &&
        Objects.equals(mergedInto, that.mergedInto) &&
        Objects.equals(getNode(), that.getNode());
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(pathFormula, startingAbstraction, mergedInto);
    }
    return hashCache;
  }
}
