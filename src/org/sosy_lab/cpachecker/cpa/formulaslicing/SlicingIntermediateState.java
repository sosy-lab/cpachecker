package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import java.util.Objects;

/**
 * Intermediate state: a formula describing all possible executions at a point.
 */
class SlicingIntermediateState extends SlicingState {

  private final CFANode node;

  /** Formula describing state-space. */
  private final PathFormula pathFormula;

  /** Starting point for the formula */
  private final SlicingAbstractedState start;

  /** Checking coverage */
  private transient SlicingIntermediateState mergedInto;

  private transient int hashCache = 0;

  private SlicingIntermediateState(
      CFANode pNode, PathFormula pPathFormula,
      SlicingAbstractedState pStart) {
    node = pNode;
    pathFormula = pPathFormula;
    start = pStart;
  }

  public static SlicingIntermediateState of(
      CFANode pNode,
      PathFormula pPathFormula,
      SlicingAbstractedState pStart
      ) {
    return new SlicingIntermediateState(pNode, pPathFormula, pStart);
  }

  public CFANode getNode() {
    return node;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public SlicingAbstractedState getAbstractParent() {
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

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    SlicingIntermediateState that = (SlicingIntermediateState) pO;
    return Objects.equals(node, that.node) &&
        Objects.equals(pathFormula, that.pathFormula) &&
        Objects.equals(start, that.start);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(node, pathFormula, start);
    }
    return hashCache;
  }
}
