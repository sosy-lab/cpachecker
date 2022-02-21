// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import com.google.common.base.Splitter;
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
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedFunctionPointerCPA extends AbstractDistributedCPA {

  public DistributedFunctionPointerCPA(
      String pId,
      BlockNode pNode,
      UpdatedTypeMap pTypeMap,
      Precision pPrecision,
      AnalysisDirection pDirection,
      AnalysisOptions pOptions)
      throws CPAException {
    super(pId, pNode, pTypeMap, pPrecision, pDirection, pOptions);
  }

  @Override
  public AbstractState deserialize(Message pPayload) throws InterruptedException {
    String serialized = pPayload.getPayload().getOrDefault(parentCPA.getClass().getName(), "");
    if (serialized.isBlank()) {
      return getInitialState(block.getNodeWithNumber(pPayload.getTargetNodeNumber()), StateSpacePartition.getDefaultPartition());
    }
    FunctionPointerState.Builder builder = FunctionPointerState.createEmptyState().createBuilder();
    for (String s : Splitter.on(", ").splitToList(serialized)) {
      if (s.isBlank()) {
        continue;
      }
      List<String> parts = Splitter.on(":").limit(3).splitToList(s);
      assert parts.size() >= 2;
      switch (parts.get(0)) {
        case "I":
          builder.setTarget(parts.get(1), FunctionPointerState.InvalidTarget.getInstance());
          break;
        case "U":
          builder.setTarget(parts.get(1), FunctionPointerState.UnknownTarget.getInstance());
          break;
        case "N":
          builder.setTarget(parts.get(1), new NamedFunctionTarget(parts.get(2)));
          break;
        default:
          throw new AssertionError("Unknwon FunctionPointerState");
      }
    }
    return builder.build();
  }

  @Override
  public Payload serialize(AbstractState pState) {
    FunctionPointerState state = (FunctionPointerState) pState;
    FunctionPointerState.Builder builder = state.createBuilder();
    StringBuilder serialized = new StringBuilder();
    for (String value : builder.getValues()) {
      if (FunctionPointerState.InvalidTarget.getInstance().equals(builder.getTarget(value))) {
        serialized.append("I:").append(value).append(", ");
      } else if (FunctionPointerState.UnknownTarget.getInstance().equals(builder.getTarget(value))) {
        serialized.append("U:").append(value).append(", ");
      } else {
        NamedFunctionTarget namedTarget = (NamedFunctionTarget) builder.getTarget(value);
        serialized.append("N:").append(value).append(":").append(namedTarget.getFunctionName()).append(", ");
      }
    }
    return Payload.builder().addEntry(parentCPA.getClass().getName(), serialized.toString()).build();
  }


  @Override
  protected MessageProcessing proceedForward(Message newMessage)
      throws InterruptedException, SolverException {
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
    return FunctionPointerState.class.equals(pClass);
  }
}
