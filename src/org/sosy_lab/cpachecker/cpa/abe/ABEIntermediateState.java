/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.abe;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import java.util.Objects;

/**
 * Intermediate class in ABE, which simply represents the formula between two
 * blocks.
 */
public class ABEIntermediateState<A extends ABEAbstractedState<A>> implements ABEState<A> {

  private final PathFormula pathFormula;
  private final ABEAbstractedState<A> startingAbstraction;
  private final CFANode node;
  private transient ABEIntermediateState<A> mergedInto;
  private transient int hashCache = 0;

  private ABEIntermediateState(
      PathFormula pPathFormula,
      ABEAbstractedState<A> pStartingAbstraction,
      CFANode pNode) {
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
      CFANode node,
      PathFormula pPathFormula,
      ABEAbstractedState<A> backpointer
  ) {
    return new ABEIntermediateState<>(pPathFormula, backpointer, node);
  }

  public ABEAbstractedState<A> getBackpointerState() {
    return startingAbstraction;
  }

  /**
   * Optimizations for coverage checking.
   */
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
    return pathFormula.toString() + "\nLength: " + pathFormula.getLength();
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
    return Objects.equals(pathFormula, that.pathFormula) &&
        Objects.equals(startingAbstraction, that.startingAbstraction) &&
        Objects.equals(node, that.node);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(pathFormula, startingAbstraction, node);
    }
    return hashCache;
  }
}
