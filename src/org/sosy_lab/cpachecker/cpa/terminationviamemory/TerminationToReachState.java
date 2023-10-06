// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import java.util.Map;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

/** Tracks already seen states at loop-head locations*/
public class TerminationToReachState
    implements Graphable, AbstractQueryableState {
  /** We store values of variables that we have seen at the concrete loop-head location*/
  private Map<LocationState, BooleanFormula> storedValues;
  /** We store number of times that we have iterated over a loop*/
  private Map<LocationState, Integer> numberOfIterations;
  public TerminationToReachState(Map<LocationState, BooleanFormula> pStoredValues,
                                 Map<LocationState, Integer> pNumberOfIterations) {
    storedValues = pStoredValues;
    numberOfIterations = pNumberOfIterations;
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
}
