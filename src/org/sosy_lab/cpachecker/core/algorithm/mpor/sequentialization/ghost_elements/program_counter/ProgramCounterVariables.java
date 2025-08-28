// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;

public class ProgramCounterVariables {

  private final ImmutableList<CLeftHandSide> programCounter;

  private final ImmutableList<CBinaryExpression> threadNotActiveExpressions;

  ProgramCounterVariables(
      ImmutableList<CLeftHandSide> pProgramCounter,
      ImmutableList<CBinaryExpression> pThreadNotActiveExpressions) {

    programCounter = pProgramCounter;
    threadNotActiveExpressions = pThreadNotActiveExpressions;
  }

  public CLeftHandSide getPcLeftHandSide(int pThreadId) {
    return programCounter.get(pThreadId);
  }

  public CBinaryExpression getThreadNotActiveExpression(int pThreadId) {
    return threadNotActiveExpressions.get(pThreadId);
  }
}
