// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.conditions.path;

import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public final class AssumeEdgesInPathConditionState
    implements AbstractState, AvoidanceReportingState {

  private final int assumeEdgesInPath;
  private final boolean thresholdReached;

  AssumeEdgesInPathConditionState(int pPathLength, boolean pThresholdReached) {
    assumeEdgesInPath = pPathLength;
    thresholdReached = pThresholdReached;
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return thresholdReached;
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView pMgr) {
    return PreventingHeuristic.ASSUMEEDGESINPATH.getFormula(pMgr, assumeEdgesInPath);
  }

  public int getPathLength() {
    return assumeEdgesInPath;
  }

  boolean isThresholdReached() {
    return thresholdReached;
  }

  @Override
  public String toString() {
    return "path length: " + assumeEdgesInPath + (thresholdReached ? " (threshold reached)" : "");
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    AssumeEdgesInPathConditionState that = (AssumeEdgesInPathConditionState) pO;
    return assumeEdgesInPath == that.assumeEdgesInPath && thresholdReached == that.thresholdReached;
  }

  @Override
  public int hashCode() {
    return Objects.hash(assumeEdgesInPath, thresholdReached);
  }
}
