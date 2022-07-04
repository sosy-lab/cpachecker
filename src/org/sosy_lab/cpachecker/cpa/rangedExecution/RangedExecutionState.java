// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedExecution;

import java.io.Serializable;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

public class RangedExecutionState
    implements LatticeAbstractState<RangedExecutionState>, Serializable, Graphable {


  private static final long serialVersionUID = 6762491514691078996L;


  private final ValueAnalysisState leftState;
  private final ValueAnalysisState rightState;

  public RangedExecutionState(ValueAnalysisState pLeftState, ValueAnalysisState pRightState) {
    leftState = pLeftState;
    rightState = pRightState;

  }

  public static RangedExecutionState getMiddleState() {
    return new MiddleRangedExecutionState();
  }


  @Override
  public boolean isLessOrEqual(RangedExecutionState other) {
    return false;
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  @Override
  public String toDOTLabel() {
    return leftState + " | " + rightState;
  }


  @Override
  public RangedExecutionState join(RangedExecutionState pOther) {
    return pOther;
  }

  public ValueAnalysisState getLeftState() {
    return leftState;
  }

  public ValueAnalysisState getRightState() {
    return rightState;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }


}
