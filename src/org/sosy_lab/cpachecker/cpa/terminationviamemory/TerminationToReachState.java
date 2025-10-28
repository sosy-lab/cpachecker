// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.Formula;

/** Tracks already seen states at loop-head locations */
public class TerminationToReachState implements Graphable, AbstractQueryableState, Targetable {
  private static final ImmutableSet<TargetInformation> TERMINATION_PROPERTY =
      SimpleTargetInformation.singleton("termination");
  private boolean isTarget;

  /** The constraints on values of the variables that has already been seen in a loop-head */
  private Map<Pair<LocationState, CallstackState>, Map<Integer, Set<Formula>>> storedValues;

  /** We store number of times that we have iterated over a loop */
  private Map<Pair<LocationState, CallstackState>, Integer> numberOfIterations;

  /** Stores assumptions from path formula after i iterations of the loop */
  private Map<Pair<LocationState, CallstackState>, PathFormula> pathFormulaForIteration;

  public TerminationToReachState(
      Map<Pair<LocationState, CallstackState>, Map<Integer, Set<Formula>>> pStoredValues,
      Map<Pair<LocationState, CallstackState>, Integer> pNumberOfIterations,
      Map<Pair<LocationState, CallstackState>, PathFormula> pPathFormulaForIteration) {

    storedValues = pStoredValues;
    numberOfIterations = pNumberOfIterations;
    pathFormulaForIteration = pPathFormulaForIteration;
    isTarget = false;
  }

  public void increaseNumberOfIterationsAtLoopHead(Pair<LocationState, CallstackState> pKeyPair) {
    if (numberOfIterations.containsKey(pKeyPair)) {
      numberOfIterations.put(pKeyPair, numberOfIterations.get(pKeyPair) + 1);
    } else {
      numberOfIterations.put(pKeyPair, 1);
    }
  }

  public int getNumberOfIterationsAtLoopHead(Pair<LocationState, CallstackState> pKeyPair) {
    if (numberOfIterations.containsKey(pKeyPair)) {
      return numberOfIterations.get(pKeyPair);
    }
    return 0;
  }

  public Map<Pair<LocationState, CallstackState>, Integer> getNumberOfIterationsMap() {
    return numberOfIterations;
  }

  public void setNewStoredValues(
      LocationState pLoopHead,
      CallstackState pCallstackState,
      Set<Formula> pNewStoredValues,
      int index) {
    Pair<LocationState, CallstackState> newKey = Pair.of(pLoopHead, pCallstackState);
    if (storedValues.containsKey(newKey)) {
      Map<Integer, Set<Formula>> assumptions = storedValues.get(newKey);
      assumptions.put(index, pNewStoredValues);
    } else {
      Map<Integer, Set<Formula>> newValues = new HashMap<>();
      newValues.put(0, pNewStoredValues);
      storedValues.put(newKey, newValues);
    }
  }

  public Map<Pair<LocationState, CallstackState>, Map<Integer, Set<Formula>>> getStoredValues() {
    return storedValues;
  }

  public void putNewPathFormula(
      Pair<LocationState, CallstackState> pKeyPair, PathFormula pPathFormula) {
    pathFormulaForIteration.put(pKeyPair, pPathFormula);
  }

  public Map<Pair<LocationState, CallstackState>, PathFormula> getPathFormulas() {
    return pathFormulaForIteration;
  }

  public void makeTarget() {
    isTarget = true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(storedValues, numberOfIterations, isTarget);
  }

  @Override
  public boolean isTarget() {
    return isTarget;
  }

  @Override
  public Set<TargetInformation> getTargetInformation() {
    checkState(isTarget);
    return TERMINATION_PROPERTY;
  }

  @Override
  public String toString() {
    return "TerminationState{storedValues=[" + getReadableStoredValues() + "]" + '}';
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof TerminationToReachState other
        && storedValues.equals(other.getStoredValues());
  }

  private String getReadableStoredValues() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Pair<LocationState, CallstackState>, Map<Integer, Set<Formula>>> entry :
        getStoredValues().entrySet()) {
      sb.append(entry);
    }
    return sb.toString();
  }

  @Override
  public String toDOTLabel() {
    return "Stored Values:\n" + getReadableStoredValues().replace(", ", "\n");
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String getCPAName() {
    return "TerminationToReachCPA";
  }
}
