// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.symbolicExecution;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class SymbolicExecutionDomain implements AbstractDomain {

  private final AbstractDomain valueDomain;
  private final AbstractDomain constraintsDomain;

  public SymbolicExecutionDomain(AbstractDomain pValueDomain, AbstractDomain pConstraintsDomain) {
    valueDomain = pValueDomain;
    constraintsDomain = pConstraintsDomain;
  }

  @Override
  public AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    if (state1
            instanceof
            SymbolicExecutionState(
                ValueAnalysisState pValueAnalysisState,
                ConstraintsState pConstraintsState)
        && state2
            instanceof
            SymbolicExecutionState(ValueAnalysisState pAnalysisState, ConstraintsState pState)) {
      return new SymbolicExecutionState(
          (ValueAnalysisState) valueDomain.join(pValueAnalysisState, pAnalysisState),
          (ConstraintsState) constraintsDomain.join(pConstraintsState, pState));
    }
    throw new AssertionError("Expected both states to be of type SymbolicExecutionState");
  }

  @Override
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    if (state1
            instanceof
            SymbolicExecutionState(
                ValueAnalysisState pValueAnalysisState,
                ConstraintsState pConstraintsState)
        && state2
            instanceof
            SymbolicExecutionState(ValueAnalysisState pAnalysisState, ConstraintsState pState)) {
      if (!valueDomain.isLessOrEqual(pValueAnalysisState, pAnalysisState)) {
        return false;
      }
      return constraintsDomain.isLessOrEqual(pConstraintsState, pState);
    }
    throw new AssertionError("Expected both states to be of type SymbolicExecutionState");
  }
}
