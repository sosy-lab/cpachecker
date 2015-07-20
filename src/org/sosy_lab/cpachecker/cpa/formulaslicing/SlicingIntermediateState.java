package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

/**
 * Intermediate state: a formula describing all possible executions at a point.
 */
public class SlicingIntermediateState extends SlicingState {
  /** Formula describing state-space. */
  private final PathFormula pathFormula;

  /** Starting point for the formula */
  private final SlicingAbstractedState start;

  /** Checking coverage */
  private transient SlicingIntermediateState mergedInto;

  private SlicingIntermediateState(PathFormula pPathFormula,
      SlicingAbstractedState pStart) {
    pathFormula = pPathFormula;
    start = pStart;
  }

  public static SlicingIntermediateState of(PathFormula pPathFormula,
      SlicingAbstractedState pStart) {
    return new SlicingIntermediateState(pPathFormula, pStart);
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public SlicingAbstractedState getAbstraction() {
    return start;
  }

  /** Coverage checking for intermediate states */
  public void setMergedInto(SlicingIntermediateState other) {
    mergedInto = other;
  }

  public boolean isMergedInto(SlicingIntermediateState other) {
    return mergedInto == other;
  }


  @Override
  public boolean isAbstracted() {
    return false;
  }
}
