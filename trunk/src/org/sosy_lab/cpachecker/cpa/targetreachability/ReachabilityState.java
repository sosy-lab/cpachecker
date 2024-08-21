// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.targetreachability;

import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public enum ReachabilityState implements AvoidanceReportingState {
  RELEVANT_TO_TARGET(true),
  IRRELEVANT_TO_TARGET(false);

  private final boolean isRelevantToTarget;

  ReachabilityState(boolean pIsRelevantToTarget) {
    isRelevantToTarget = pIsRelevantToTarget;
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return !isRelevantToTarget;
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView mgr) {
    return mgr.getBooleanFormulaManager().makeVariable("IRRELEVANT_FOR_SPECIFICATION");
  }
}
