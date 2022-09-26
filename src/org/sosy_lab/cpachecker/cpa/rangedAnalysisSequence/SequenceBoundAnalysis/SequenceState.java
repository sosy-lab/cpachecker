// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedAnalysisSequence.SequenceBoundAnalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class SequenceState implements LatticeAbstractState<SequenceState>, Targetable, Graphable {

  private final List<Boolean> remainingDecisionsToTake;
  private final boolean stopIfUnderspecifiedTestcase;

  public SequenceState(List<Boolean> pDecisionNodes, boolean pStopIfUnderspecifiedTestcase) {
    this.remainingDecisionsToTake = pDecisionNodes;
    this.stopIfUnderspecifiedTestcase = pStopIfUnderspecifiedTestcase;
  }

  SequenceState copy() {
    return new SequenceState(
        new ArrayList<>(remainingDecisionsToTake), stopIfUnderspecifiedTestcase);
  }

  @Override
  public boolean isTarget() {
    return false;
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return new HashSet<>();
  }

  @Override
  public String toString() {
    return remainingDecisionsToTake.toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof SequenceState)) {
      return false;
    }
    SequenceState that = (SequenceState) pO;
    return Objects.equals(remainingDecisionsToTake, that.remainingDecisionsToTake);
  }

  @Override
  public int hashCode() {
    return Objects.hash(remainingDecisionsToTake);
  }

  @Override
  public String toDOTLabel() {
    return toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public SequenceState join(SequenceState other) throws CPAException, InterruptedException {
    if (other.isLessOrEqual(this)) {
      return this;
    } else return other;
  }

  @Override
  public boolean isLessOrEqual(SequenceState other) {
    if (remainingDecisionsToTake.size() > other.remainingDecisionsToTake.size()) {
      return false;
    }
    for (int i = 0; i < remainingDecisionsToTake.size(); i++) {
      if (remainingDecisionsToTake.get(i) != other.remainingDecisionsToTake.get(i)) {
        return false;
      }
    }
    return true;
  }

  public boolean thisEdgeShouldBeTaken(AssumeEdge pCfaEdge) {

    if (remainingDecisionsToTake.size() == 0) {
      // Do not take the edge if there are no decisions left to take and we should stop if the
      // testcase is underspecified.
      return !stopIfUnderspecifiedTestcase;
    }
    if (remainingDecisionsToTake.get(0) == pCfaEdge.getTruthAssumption()
        || (remainingDecisionsToTake.get(0) != pCfaEdge.getTruthAssumption()
            && pCfaEdge.isSwapped())) {
      return true;
    }
    return false;
  }

  public SequenceState takeEdge(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof AssumeEdge) {
      AssumeEdge assumeEdge = (AssumeEdge) pCfaEdge;
      if (thisEdgeShouldBeTaken(assumeEdge) && remainingDecisionsToTake.size() > 0) {
        return new SequenceState(
            remainingDecisionsToTake.subList(1, remainingDecisionsToTake.size()),
            stopIfUnderspecifiedTestcase);
      }
    }
    return copy();
  }
}
