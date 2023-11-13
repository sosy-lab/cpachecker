// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import scala.concurrent.impl.FutureConvertersImpl.P;

/** Tracks already seen states at loop-head locations*/
public class TerminationToReachState
    implements Graphable, AbstractQueryableState, Targetable {
  /** We store values of variables that we have seen at the concrete loop-head location*/
  private boolean isTarget;
  private Map<LocationState, List<BooleanFormula>> storedValues;
  /** We store number of times that we have iterated over a loop*/
  private Map<LocationState, Integer> numberOfIterations;
  /** Stores assumptions from path formula after i iterations of the loop*/
  private Set<BooleanFormula> pathFormulaForIteration;
  private static final String PROPERTY_TERMINATION = "termination";
  public TerminationToReachState(Map<LocationState, List<BooleanFormula>> pStoredValues,
                                 Map<LocationState, Integer> pNumberOfIterations,
                                 Set<BooleanFormula> pPathFormulaForIteration) {

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
  public void setNewStoredValues(LocationState pLoopHead, BooleanFormula pNewStoredValues) {
    if (storedValues.containsKey(pLoopHead)) {
      storedValues.get(pLoopHead).add(pNewStoredValues);
    } else {
      List<BooleanFormula> newValues = new ArrayList<>();
      newValues.add(pNewStoredValues);
      storedValues.put(pLoopHead, newValues);
    }
  }
  public Map<LocationState, List<BooleanFormula>> getStoredValues() {
    return storedValues;
  }
  public void putNewPathFormula(BooleanFormula pPathFormula) {
    pathFormulaForIteration.add(pPathFormula);
  }
  public Set<BooleanFormula> getPathFormulas() {
    return pathFormulaForIteration;
  }
  public void makeTarget() { isTarget = true; }

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
    return ImmutableSet.of();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof TerminationToReachState)) {
      return false;
    }
    TerminationToReachState that = (TerminationToReachState) pO;

    for (LocationState loc : storedValues.keySet()) {
      if (!that.getStoredValues().keySet().contains(loc) ||
          storedValues.get(loc) != that.getStoredValues().get(loc)) {
        return false;
      }
    }

    return true;
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