// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.monitor;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/** Keeps track of the resources spent on a path. */
public class MonitorState extends AbstractSingleWrapperState implements AvoidanceReportingState {

  enum TimeoutState implements AbstractState {
    INSTANCE;

    @Override
    public String toString() {
      return "Dummy element because computation timed out";
    }
  }

  /// The total time spent on this path
  private final long totalTimeOnPath;

  /// The cause why the analysis did not proceed after this state (may be null)
  @Nullable private final Pair<PreventingHeuristic, Long> preventingCondition;

  protected MonitorState(AbstractState pWrappedState, long totalTimeOnPath) {
    this(pWrappedState, totalTimeOnPath, null);
  }

  protected MonitorState(
      AbstractState pWrappedState,
      long totalTimeOnPath,
      Pair<PreventingHeuristic, Long> preventingCondition) {

    super(pWrappedState);

    Preconditions.checkArgument(
        !(pWrappedState instanceof MonitorState),
        "Don't wrap a MonitorCPA in a MonitorCPA, this makes no sense!");
    Preconditions.checkArgument(
        !(pWrappedState == TimeoutState.INSTANCE && preventingCondition == null),
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
      MonitorState otherElem = (MonitorState) pObj;
      return getWrappedState().equals(otherElem.getWrappedState());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getWrappedState().hashCode();
  }

  /**
   * Return whether the current element is the same as BOTTOM? (no successor states because there
   * was a reason that prevented it)
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
    return String.format("Total time: %s Wrapped elem: %s", totalTimeOnPath, getWrappedStates());
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
