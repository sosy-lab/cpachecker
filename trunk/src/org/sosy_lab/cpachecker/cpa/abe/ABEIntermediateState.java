// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.abe;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

/** Intermediate class in ABE, which simply represents the formula between two blocks. */
public final class ABEIntermediateState<A extends ABEAbstractedState<A>> implements ABEState<A> {

  private final PathFormula pathFormula;
  private final ABEAbstractedState<A> startingAbstraction;
  private final CFANode node;
  private transient ABEIntermediateState<A> mergedInto;
  private transient int hashCache = 0;

  private ABEIntermediateState(
      PathFormula pPathFormula, ABEAbstractedState<A> pStartingAbstraction, CFANode pNode) {
    pathFormula = pPathFormula;
    startingAbstraction = pStartingAbstraction;
    node = pNode;
  }

  /**
   * Construct a new intermediate class.
   *
   * @param node Node associated with the state.
   * @param pPathFormula Current formula.
   * @param backpointer Backpointer to the last abstracted state.
   * @param <A> class of the abstracted state.
   */
  public static <A extends ABEAbstractedState<A>> ABEIntermediateState<A> of(
      CFANode node, PathFormula pPathFormula, ABEAbstractedState<A> backpointer) {
    return new ABEIntermediateState<>(pPathFormula, backpointer, node);
  }

  public ABEAbstractedState<A> getBackpointerState() {
    return startingAbstraction;
  }

  /** Optimizations for coverage checking. */
  public void setMergedInto(ABEIntermediateState<A> other) {
    mergedInto = other;
  }

  public boolean isMergedInto(ABEIntermediateState<A> other) {
    return other == mergedInto;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public String toString() {
    return pathFormula + "\nLength: " + pathFormula.getLength();
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public CFANode getNode() {
    return node;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    ABEIntermediateState<?> that = (ABEIntermediateState<?>) pO;
    return Objects.equals(pathFormula, that.pathFormula)
        && Objects.equals(startingAbstraction, that.startingAbstraction)
        && Objects.equals(node, that.node);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(pathFormula, startingAbstraction, node);
    }
    return hashCache;
  }
}
