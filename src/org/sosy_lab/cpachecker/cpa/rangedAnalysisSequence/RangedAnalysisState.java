// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedAnalysisSequence;

import java.io.Serializable;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.rangedAnalysisSequence.SequenceBoundAnalysis.SequenceState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

public class RangedAnalysisState
    implements LatticeAbstractState<RangedAnalysisState>, Serializable, Graphable {

  private static final long serialVersionUID = 6762491514691078996L;

  private final @Nullable SequenceState leftState;
  private final @Nullable SequenceState rightState;

  public RangedAnalysisState(SequenceState pLeftState, SequenceState pRightState) {
    leftState = pLeftState;
    rightState = pRightState;
  }

  public static RangedAnalysisState getMiddleState() {
    return new MiddleRangedAnalysisState();
  }

  @Override
  public boolean isLessOrEqual(RangedAnalysisState other) {
    return ((this.leftState != null
                && other.leftState != null
                && this.leftState.isLessOrEqual(other.leftState))
            || (this.leftState == null && other.leftState == null))
        && ((this.rightState != null
                && other.rightState != null
                && this.rightState.isLessOrEqual(other.rightState))
            || (this.rightState == null && other.rightState == null));
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
  public RangedAnalysisState join(RangedAnalysisState pOther) {
    if (pOther.isLessOrEqual(this)) {
      return this;
    }
    return pOther;
  }

  public SequenceState getLeftState() {
    return leftState;
  }

  public SequenceState getRightState() {
    return rightState;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof RangedAnalysisState)) {
      return false;
    }
    RangedAnalysisState that = (RangedAnalysisState) pO;
    return Objects.equals(leftState, that.leftState) && Objects.equals(rightState, that.rightState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(leftState, rightState);
  }
}
