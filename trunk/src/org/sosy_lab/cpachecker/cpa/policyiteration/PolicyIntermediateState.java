// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

public final class PolicyIntermediateState extends PolicyState {

  /** Formula + SSA associated with the state. */
  private final PathFormula pathFormula;

  /** Abstract state representing start of the trace. */
  private final PolicyAbstractedState startingAbstraction;

  private @Nullable transient ImmutableList<ValueAssignment> counterexample = null;
  /** Meta-information for determining the coverage. */
  private @Nullable transient PolicyIntermediateState mergedInto = null;

  private transient int hashCache = 0;

  private PolicyIntermediateState(
      CFANode node, PathFormula pPathFormula, PolicyAbstractedState pStartingAbstraction) {
    super(node);

    pathFormula = pPathFormula;
    startingAbstraction = pStartingAbstraction;
  }

  public static PolicyIntermediateState of(
      CFANode node, PathFormula pPathFormula, PolicyAbstractedState generatingState) {
    return new PolicyIntermediateState(node, pPathFormula, generatingState);
  }

  public void setCounterexample(ImmutableList<ValueAssignment> pCounterexample) {
    counterexample = pCounterexample;
  }

  public PolicyIntermediateState withPathFormula(PathFormula pPathFormula) {
    return new PolicyIntermediateState(getNode(), pPathFormula, startingAbstraction);
  }

  public void setMergedInto(PolicyIntermediateState other) {
    mergedInto = other;
  }

  public boolean isMergedInto(PolicyIntermediateState other) {
    return other == mergedInto;
  }

  /** Returns starting {@link PolicyAbstractedState} for the starting location. */
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

  /** Iterator for all states <em>including</em> this one, up to the analysis root. */
  Iterable<PolicyIntermediateState> allStatesToRoot() {
    PolicyIntermediateState pThis = this;
    Iterator<PolicyIntermediateState> it =
        new Iterator<>() {
          private Optional<PolicyIntermediateState> cursor = Optional.of(pThis);

          @Override
          public boolean hasNext() {
            return cursor.isPresent();
          }

          @Override
          public PolicyIntermediateState next() {
            PolicyIntermediateState toReturn = cursor.orElseThrow();
            cursor = cursor.orElseThrow().getBackpointerState().getGeneratingState();
            return toReturn;
          }
        };
    return () -> it;
  }

  @Override
  public String toDOTLabel() {
    if (counterexample == null) {
      return "";
    }
    return Joiner.on('\n').join(counterexample);
  }

  @Override
  public String toString() {
    return pathFormula + "\nLength: " + pathFormula.getLength();
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
    return Objects.equals(pathFormula, that.pathFormula)
        && Objects.equals(startingAbstraction, that.startingAbstraction)
        && Objects.equals(mergedInto, that.mergedInto)
        && Objects.equals(getNode(), that.getNode());
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(pathFormula, startingAbstraction, mergedInto);
    }
    return hashCache;
  }
}
