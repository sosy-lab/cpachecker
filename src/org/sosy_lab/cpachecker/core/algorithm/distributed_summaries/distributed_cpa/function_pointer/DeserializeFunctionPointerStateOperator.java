// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
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
  public AbstractState deserialize(DssMessage pMessage) {
    String serialized = pMessage.getAbstractStateContent(FunctionPointerState.class).get(STATE_KEY);
    Preconditions.checkNotNull(serialized, "If entry is contained, it cannot be null");
    if (serialized.isBlank()) {
      CFANode location = DeserializeOperator.startLocationFromMessageType(pMessage, blockNode);
      return functionPointerCPA.getInitialState(
          location, StateSpacePartition.getDefaultPartition());
    }
    FunctionPointerState.Builder builder = FunctionPointerState.createEmptyState().createBuilder();
    for (String s : Splitter.on(", ").splitToList(serialized)) {
      if (s.isBlank()) {
        continue;
      }
      List<String> parts = Splitter.on(":").limit(3).splitToList(s);
      assert parts.size() >= 2;
      switch (parts.getFirst()) {
        case "I" ->
            builder.setTarget(parts.get(1), FunctionPointerState.InvalidTarget.getInstance());
        case "0" -> builder.setTarget(parts.get(1), FunctionPointerState.NullTarget.getInstance());
        case "U" ->
            builder.setTarget(parts.get(1), FunctionPointerState.UnknownTarget.getInstance());
        case "N" -> builder.setTarget(parts.get(1), new NamedFunctionTarget(parts.get(2)));
        default -> throw new AssertionError("Unknown FunctionPointerState");
      }
    }
    return builder.build();
  }
}
