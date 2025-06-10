// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;

public class PcVariables {

  private final ImmutableList<CLeftHandSide> pc;

  private final ImmutableList<CBinaryExpression> threadNotActiveExpressions;

  public PcVariables(
      ImmutableList<CLeftHandSide> pPc,
      ImmutableList<CBinaryExpression> pThreadNotActiveExpressions) {

    pc = pPc;
    threadNotActiveExpressions = pThreadNotActiveExpressions;
  }

  public CLeftHandSide getPcLeftHandSide(int pThreadId) {
    return pc.get(pThreadId);
  }

  public CBinaryExpression getThreadNotActiveExpression(int pThreadId) {
    return threadNotActiveExpressions.get(pThreadId);
  }
}
