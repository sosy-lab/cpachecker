// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageWithStates;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;

public class DeserializeFunctionPointerStateOperator implements DeserializeOperator {

  private final FunctionPointerCPA functionPointerCPA;
  private final BlockNode blockNode;

  public DeserializeFunctionPointerStateOperator(
      FunctionPointerCPA pFunctionPointerCPA, BlockNode pBlockNode) {
    functionPointerCPA = pFunctionPointerCPA;
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState deserialize(DssMessageWithStates pMessage, int pStateIndex) {
    DssFunctionPointerStatePayload payload =
        (DssFunctionPointerStatePayload)
            Objects.requireNonNull(
                    pMessage
                        .getStates()
                        .get(pStateIndex)
                        .get(DssComponentKey.serializedNameFor(FunctionPointerState.class)))
                .state();

    if (payload.entries().isEmpty()) {
      CFANode location = DeserializeOperator.startLocationFromMessageType(pMessage, blockNode);
      return functionPointerCPA.getInitialState(
          location, StateSpacePartition.getDefaultPartition());
    }
    FunctionPointerState.Builder builder = FunctionPointerState.createEmptyState().createBuilder();
    for (DssFunctionPointerEntry entry : payload.entries()) {
      switch (entry.kind()) {
        case INVALID ->
            builder.setTarget(
                entry.pointerVariable(), FunctionPointerState.InvalidTarget.getInstance());
        case NULL ->
            builder.setTarget(
                entry.pointerVariable(), FunctionPointerState.NullTarget.getInstance());
        case UNKNOWN ->
            builder.setTarget(
                entry.pointerVariable(), FunctionPointerState.UnknownTarget.getInstance());
        case NAMED ->
            builder.setTarget(
                entry.pointerVariable(), new NamedFunctionTarget(entry.functionName()));
      }
    }
    return builder.build();
  }
}
