// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.google.common.base.Splitter;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;

public class DeserializeFunctionPointerStateOperator implements DeserializeOperator {

  private final BlockNode block;
  private final FunctionPointerCPA functionPointerCPA;

  public DeserializeFunctionPointerStateOperator(
      FunctionPointerCPA pFunctionPointerCPA, BlockNode pBlockNode) {
    block = pBlockNode;
    functionPointerCPA = pFunctionPointerCPA;
  }

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) {
    String serialized = pMessage.getAbstractStateString(functionPointerCPA.getClass()).orElse("");
    if (serialized.isBlank()) {
      return functionPointerCPA.getInitialState(
          block.getNodeWithNumber(pMessage.getTargetNodeNumber()),
          StateSpacePartition.getDefaultPartition());
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
}
