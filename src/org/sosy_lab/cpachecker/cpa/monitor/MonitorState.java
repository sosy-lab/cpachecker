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

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import com.google.common.base.Preconditions;

/**
 * Keeps track of the resources spent on a path.
 */
public class MonitorState extends AbstractSingleWrapperState implements AvoidanceReportingState {

  private static final long serialVersionUID = 0xDEADBEEF;

  /**
   * javadoc to remove unused parameter warning
   * @param out the output stream
   */
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

  /// The total time spent on this path
  private final long totalTimeOnPath;

  /// The cause why the analysis did not proceed after this state (may be null)
  @Nullable
  private final Pair<PreventingHeuristic, Long> preventingCondition;

  protected MonitorState(AbstractState pWrappedState, long totalTimeOnPath) {
    this(pWrappedState, totalTimeOnPath, null);
  }

  protected MonitorState(AbstractState pWrappedState, long totalTimeOnPath,
      Pair<PreventingHeuristic, Long> preventingCondition) {

    super(pWrappedState);

    Preconditions.checkArgument(!(pWrappedState instanceof MonitorState),
        "Don't wrap a MonitorCPA in a MonitorCPA, this makes no sense!");
    Preconditions.checkArgument(!(pWrappedState == TimeoutState.INSTANCE && preventingCondition == null),
        "Need a preventingCondition in case of TimeoutState");

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

  /**
   * @return  Is the current element the same as BOTTOM?
   *  (no successor states because there was a reason that prevented it)
   */
  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return preventingCondition != null;
  }

  Pair<PreventingHeuristic, Long> getPreventingCondition() {
    return preventingCondition;
  }

  @Override
  public String toString() {
    return String.format("Total time: %s Wrapped elem: %s",
        this.totalTimeOnPath, getWrappedStates());
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView manager) {

    if (mustDumpAssumptionForAvoidance()) {
      return preventingCondition.getFirst().getFormula(manager, preventingCondition.getSecond());

    } else {

      BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
      return bfmgr.makeTrue();
    }
  }

}