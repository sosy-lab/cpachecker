// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;

public class DeserializeFunctionPointerStateOperator implements DeserializeOperator {

  private final FunctionPointerCPA functionPointerCPA;
  private final ImmutableMap<Integer, CFANode> integerCFANodeMap;

  public DeserializeFunctionPointerStateOperator(
      FunctionPointerCPA pFunctionPointerCPA, ImmutableMap<Integer, CFANode> pIntegerCFANodeMap) {
    functionPointerCPA = pFunctionPointerCPA;
    integerCFANodeMap = pIntegerCFANodeMap;
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) {
    String serialized =
        pMessage.getAbstractState(functionPointerCPA.getClass()).map(Object::toString).orElse("");
    if (serialized.isBlank()) {
      return functionPointerCPA.getInitialState(
          integerCFANodeMap.get(pMessage.getTargetNodeNumber()),
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
        case "0":
          builder.setTarget(parts.get(1), FunctionPointerState.NullTarget.getInstance());
          break;
        case "U":
          builder.setTarget(parts.get(1), FunctionPointerState.UnknownTarget.getInstance());
          break;
        case "N":
          builder.setTarget(parts.get(1), new NamedFunctionTarget(parts.get(2)));
          break;
        default:
          throw new AssertionError("Unknown FunctionPointerState");
      }
    }
    return builder.build();
  }
}
