// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

/** Tracks already seen states at loop-head locations*/
public class TerminationToReachState
    implements Graphable, AbstractQueryableState, Targetable {
  /** We store values of variables that we have seen at the concrete loop-head location*/
  private boolean isTarget;
  private Map<LocationState, BooleanFormula> storedValues;
  /** We store number of times that we have iterated over a loop*/
  private Map<LocationState, Integer> numberOfIterations;
  private static final String PROPERTY_TERMINATION = "termination";
  public TerminationToReachState(Map<LocationState, BooleanFormula> pStoredValues,
                                 Map<LocationState, Integer> pNumberOfIterations) {
    storedValues = pStoredValues;
    numberOfIterations = pNumberOfIterations;
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
  public void setNewStoredValues(LocationState pLoopHead, BooleanFormula pNewStoredValues) {
    storedValues.put(pLoopHead, pNewStoredValues);
  }
  public Map<LocationState, BooleanFormula> getStoredValues() {
    return storedValues;
  }

  public void makeTarget() { isTarget = true; }

  @Override
  public int hashCode() {
    return Objects.hash(storedValues, isTarget);
  }

  @Override
  public boolean isTarget() {
    return isTarget;
  }

  @Override
  public Set<TargetInformation> getTargetInformation() {
    return ImmutableSet.of();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    TerminationToReachState that = (TerminationToReachState) pO;
    return storedValues.equals(that.getStoredValues());
  }

  @Override
  public String toString() {
    return "TerminationState{storedValues=["
        + getReadableStoredValues()
        + "]"
        + '}';
  }

  private String getReadableStoredValues() {
    return getReadableStoredValues(this);
  }

  private static String getReadableStoredValues(TerminationToReachState s) {
    String rs = "";
    for (LocationState locationState : s.getStoredValues().keySet()) {
      rs += locationState.toString() + ":";
      rs += s.getStoredValues().get(locationState).toString() + ",";
    }
    return rs;
  }

  @Override
  public String toDOTLabel() {
    return "Stored Values:\n" + getReadableStoredValues(this).replace(", ", "\n");
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
