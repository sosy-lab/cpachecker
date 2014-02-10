/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.monitor;

import java.io.IOException;
import java.io.NotSerializableException;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;

public class MonitorState extends AbstractSingleWrapperState implements AvoidanceReportingState {
  /* Boilerplate code to avoid serializing this class */
  private static final long serialVersionUID = 0xDEADBEEF;
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    throw new NotSerializableException();
  }

  static enum TimeoutState implements AbstractState {
    INSTANCE;

    @Override
    public String toString() {
      return "Dummy element because computation timed out";
    }
  }

  private final long totalTimeOnPath;

  // stores what caused the element to go further (may be null)
  private final Pair<PreventingHeuristic, Long> preventingCondition;

  protected MonitorState(AbstractState pWrappedState, long totalTimeOnPath) {
    this(pWrappedState, totalTimeOnPath, null);
  }

  protected MonitorState(AbstractState pWrappedState, long totalTimeOnPath,
      Pair<PreventingHeuristic, Long> preventingCondition) {
    super(pWrappedState);
    Preconditions.checkArgument(!(pWrappedState instanceof MonitorState), "Don't wrap a MonitorCPA in a MonitorCPA, this makes no sense!");
    Preconditions.checkArgument(!(pWrappedState == TimeoutState.INSTANCE && preventingCondition == null), "Need a preventingCondition in case of TimeoutState");
    this.totalTimeOnPath = totalTimeOnPath;
    this.preventingCondition = preventingCondition; // may be null
  }

  public long getTotalTimeOnPath() {
    return totalTimeOnPath;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    } else if (pObj instanceof MonitorState) {
      MonitorState otherElem = (MonitorState)pObj;
      return this.getWrappedState().equals(otherElem.getWrappedState());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getWrappedState().hashCode();
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    // returns true if the current element is the same as bottom
    return preventingCondition != null;
  }

  Pair<PreventingHeuristic, Long> getPreventingCondition() {
    return preventingCondition;
  }

  @Override
  public String toString() {
    return "Total time: " + this.totalTimeOnPath
    + " Wrapped elem: " + getWrappedStates();
  }


  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView manager) {
    if (mustDumpAssumptionForAvoidance()) {
      return preventingCondition.getFirst().getFormula(manager, preventingCondition.getSecond());
    } else {
      BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
      return bfmgr.makeBoolean(true);
    }
  }

}