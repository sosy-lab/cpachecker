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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/** Tracks already seen states at loop-head locations */
public class TerminationToReachState implements Graphable, AbstractQueryableState, Targetable {
  private static final ImmutableSet<TargetInformation> TERMINATION_PROPERTY =
      SimpleTargetInformation.singleton("termination");
  private boolean isTarget;

  /** The constraints on values of the variables that has already been seen in a loop-head */
  private Map<LocationState, Map<Integer, List<Formula>>> storedValues;

  /** We store number of times that we have iterated over a loop */
  private Map<LocationState, Integer> numberOfIterations;

  /** Stores assumptions from path formula after i iterations of the loop */
  private List<BooleanFormula> pathFormulaForIteration;

  public TerminationToReachState(
      Map<LocationState, Map<Integer, List<Formula>>> pStoredValues,
      Map<LocationState, Integer> pNumberOfIterations,
      List<BooleanFormula> pPathFormulaForIteration) {

    storedValues = pStoredValues;
    numberOfIterations = pNumberOfIterations;
    pathFormulaForIteration = pPathFormulaForIteration;
    isTarget = false;
  }

  public void increaseNumberOfIterationsAtLoopHead(LocationState pLoopHead) {
    if (numberOfIterations.containsKey(pLoopHead)) {
      numberOfIterations.put(pLoopHead, numberOfIterations.get(pLoopHead) + 1);
    } else {
      numberOfIterations.put(pLoopHead, 1);
    }
  }

  public int getNumberOfIterationsAtLoopHead(LocationState pLoopHead) {
    if (numberOfIterations.containsKey(pLoopHead)) {
      return numberOfIterations.get(pLoopHead);
    }
    return 0;
  }

  public Map<LocationState, Integer> getNumberOfIterationsMap() {
    return numberOfIterations;
  }

  public void setNewStoredValues(
      LocationState pLoopHead, List<Formula> pNewStoredValues, int index) {
    if (storedValues.containsKey(pLoopHead)) {
      Map<Integer, List<Formula>> assumptions = storedValues.get(pLoopHead);
      assumptions.put(index, pNewStoredValues);
    } else {
      Map<Integer, List<Formula>> newValues = new HashMap<>();
      newValues.put(0, pNewStoredValues);
      storedValues.put(pLoopHead, newValues);
    }
  }

  public Map<LocationState, Map<Integer, List<Formula>>> getStoredValues() {
    return storedValues;
  }

  public void putNewPathFormula(BooleanFormula pPathFormula) {
    pathFormulaForIteration.add(pPathFormula);
  }

  public List<BooleanFormula> getPathFormulas() {
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
    for (Map.Entry<LocationState, Map<Integer, List<Formula>>> entry :
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
