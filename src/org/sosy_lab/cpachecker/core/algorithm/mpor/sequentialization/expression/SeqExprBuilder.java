// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.ArrayElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class SeqExprBuilder {

  public static ArrayElement createPcUpdate(int pThreadId) {
    return new ArrayElement(SeqVars.pc, new Value(Integer.toString(pThreadId)));
  }

  /**
   * Creates an expression assigning the {@code pc} of {@code pThreadEdge}s successor node to the
   * variable declared in {@code pDecExpr}.
   */
  public static AssignExpr createReturnPcAssign(ThreadEdge pThreadEdge, DeclareExpr pDecExpr) {
    assert pThreadEdge.cfaEdge instanceof CFunctionSummaryEdge;
    return new AssignExpr(
        pDecExpr.variableExpr.variable, new Value(Integer.toString(pThreadEdge.getSuccessor().pc)));
  }

  public static AssignExpr createPcNextThreadAssign(int pThreadId, int pPc) {
    return new AssignExpr(createPcUpdate(pThreadId), new Value(Integer.toString(pPc)));
  }
}
