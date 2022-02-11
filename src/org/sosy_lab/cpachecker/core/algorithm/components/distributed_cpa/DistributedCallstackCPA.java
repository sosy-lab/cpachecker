// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.AnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedCallstackCPA extends AbstractDistributedCPA {

  private static final String DELIMITER = ",  ";

  public DistributedCallstackCPA(
      String pId,
      BlockNode pNode,
      UpdatedTypeMap pTypeMap,
      Precision pPrecision,
      AnalysisDirection pDirection,
      AnalysisOptions pOptions) throws CPAException {
    super(pId, pNode, pTypeMap, pPrecision, pDirection, pOptions);
  }

  @Override
  public AbstractState deserialize(Message pPayload) throws InterruptedException {
    Payload payload = pPayload.getPayload();
    if (!payload.containsKey(parentCPA.getClass().getName())) {
      return getInitialState(block.getNodeWithNumber(pPayload.getTargetNodeNumber()),
          StateSpacePartition.getDefaultPartition());
    }
    String callstackJSON = payload.get(parentCPA.getClass().getName());
    List<String> parts = Splitter.on(DELIMITER).splitToList(callstackJSON);
    CallstackState previous = null;
    for (String part : parts) {
      List<String> properties = Splitter.on(".").limit(2).splitToList(part);
      previous =
          new CallstackState(previous, properties.get(1), block.getNodeWithNumber(Integer.parseInt(
              properties.get(0))));
    }
    return previous;
  }

  @Override
  public Payload serialize(AbstractState pState) {
    CallstackState curr = (CallstackState) pState;
    List<String> states = new LinkedList<>();
    while (curr != null) {
      states.add(curr.getCallNode().getNodeNumber() + "." + curr.getCurrentFunction());
      curr = curr.getPreviousState();
    }
    Collections.reverse(states);
    String result = Joiner.on(DELIMITER).join(states);
    return Payload.builder().addEntry(parentCPA.getClass().getName(), result).build();
  }

  @Override
  protected MessageProcessing proceedForward(Message newMessage) {
    return MessageProcessing.proceed();
  }

  @Override
  protected MessageProcessing proceedBackward(Message newMessage)
      throws SolverException, InterruptedException {
    return MessageProcessing.proceed();
  }

  @Override
  public AbstractState combine(
      AbstractState pState1, AbstractState pState2) throws InterruptedException, CPAException {
    return getMergeOperator().merge(pState1, pState2, precision);
  }

  @Override
  public boolean doesOperateOn(Class<? extends AbstractState> pClass) {
    return CallstackState.class.equals(pClass);
  }
}
