// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqNameBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.ArrayElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class SeqExprBuilder {

  private static final Variable pcs = new Variable(SeqToken.PCS);

  private static final Variable nextThread = new Variable(SeqToken.NEXT_THREAD);

  public static final ArrayElement pcsNextThread = new ArrayElement(pcs, nextThread);

  public static final AssignExpr setExitPc =
      new AssignExpr(pcsNextThread, new Value(Integer.toString(SeqUtil.EXIT_PC)));

  /**
   * Creates an expression assigning the {@code pc} of {@code pThreadEdge}s successor node to the
   * variable declared in {@code pDecExpr}.
   */
  public static AssignExpr createReturnPcAssign(ThreadEdge pThreadEdge, DeclareExpr pDecExpr) {
    assert pThreadEdge.cfaEdge instanceof CFunctionSummaryEdge;
    return new AssignExpr(
        pDecExpr.variableExpr.variable, new Value(Integer.toString(pThreadEdge.getSuccessor().pc)));
  }

  /**
   * Creates a declaration expression of the form {@code int t{pThreadId}_{pFuncName}_return_pc;}.
   */
  public static DeclareExpr createReturnPcDec(int pThreadId, String pFuncName) {
    String varName = SeqNameBuilder.createReturnPcName(pThreadId, pFuncName);
    return new DeclareExpr(
        new VariableExpr(Optional.of(SeqDataType.INT), new Variable(varName)), Optional.empty());
  }

  public static AssignExpr createPcsNextThreadAssign(int pPc) {
    return new AssignExpr(pcsNextThread, new Value(Integer.toString(pPc)));
  }
}
