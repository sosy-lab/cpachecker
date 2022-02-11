// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedValueAnalysis extends AbstractDistributedCPA {

  public DistributedValueAnalysis(
      String pId,
      BlockNode pNode,
      UpdatedTypeMap pTypeMap,
      Precision pPrecision,
      AnalysisDirection pDirection)
      throws CPAException {
    super(pId, pNode, pTypeMap, pPrecision, pDirection);
  }

  @Override
  public AbstractState deserialize(Message pPayload) throws InterruptedException {
    return null;
  }

  @Override
  public Payload serialize(AbstractState pState) {
    return null;
  }

  @Override
  public MessageProcessing proceedForward(Message newMessage) {
    // continue if in block
    return null;
  }

  @Override
  public MessageProcessing proceedBackward(Message newMessage)
      throws SolverException, InterruptedException {
    // full Path variable values dont match
    return null;
  }

  @Override
  public boolean doesOperateOn(Class<? extends AbstractState> pClass) {
    return pClass.equals(ValueAnalysisState.class);
  }

  @Override
  public AbstractState combine(
      AbstractState pState1, AbstractState pState2) throws InterruptedException {
    return null;
  }

}
