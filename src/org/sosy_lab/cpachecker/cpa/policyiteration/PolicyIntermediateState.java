package org.sosy_lab.cpachecker.cpa.policyiteration;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Objects;

public final class PolicyIntermediateState extends PolicyState
    implements AvoidanceReportingState {

  /**
   * Formula + SSA associated with the state.
   */
  private final PathFormula pathFormula;

  /**
   * Abstract state representing start of the trace.
   */
  private final PolicyAbstractedState startingAbstraction;

  private final boolean isRelevantToTarget;

  /**
   * Meta-information for determining the coverage.
   */
  private transient PolicyIntermediateState mergedInto;
  private transient int hashCache = 0;

  private PolicyIntermediateState(
      CFANode node,
      PathFormula pPathFormula,
      PolicyAbstractedState pStartingAbstraction,
      boolean pIsRelevantToTarget) {
    super(node);

    pathFormula = pPathFormula;
    startingAbstraction = pStartingAbstraction;
    isRelevantToTarget = pIsRelevantToTarget;
  }

  public static PolicyIntermediateState of(
      CFANode node,
      PathFormula pPathFormula,
      PolicyAbstractedState generatingState,
      boolean pIsRelevantToTarget
  ) {
    return new PolicyIntermediateState(
        node, pPathFormula, generatingState, pIsRelevantToTarget);
  }

  public boolean getIsRelevantToTarget() {
    return isRelevantToTarget;
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
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    PolicyIntermediateState that = (PolicyIntermediateState) pO;
    return Objects.equals(pathFormula, that.pathFormula) &&
        Objects.equals(startingAbstraction, that.startingAbstraction) &&
        Objects.equals(mergedInto, that.mergedInto);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(pathFormula, startingAbstraction, mergedInto);
    }
    return hashCache;
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return !isRelevantToTarget;
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView mgr) {
    // TODO?
    return mgr.getBooleanFormulaManager().makeBoolean(true);
  }
}
