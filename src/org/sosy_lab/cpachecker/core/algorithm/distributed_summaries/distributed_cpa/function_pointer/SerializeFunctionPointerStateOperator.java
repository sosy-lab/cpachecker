// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;

public class SerializeFunctionPointerStateOperator implements SerializeOperator {

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    FunctionPointerState state = (FunctionPointerState) pState;
    FunctionPointerState.Builder builder = state.createBuilder();
    StringBuilder serialized = new StringBuilder();
    for (String value : builder.getValues()) {
      if (FunctionPointerState.InvalidTarget.getInstance().equals(builder.getTarget(value))) {
        serialized.append("I:").append(value).append(", ");
      } else if (FunctionPointerState.UnknownTarget.getInstance()
          .equals(builder.getTarget(value))) {
        serialized.append("U:").append(value).append(", ");
      } else {
        NamedFunctionTarget namedTarget = (NamedFunctionTarget) builder.getTarget(value);
        serialized
            .append("N:")
            .append(value)
            .append(":")
            .append(namedTarget.getFunctionName())
            .append(", ");
      }
    }
    return new BlockSummaryMessagePayload.Builder()
        .addEntry(FunctionPointerState.class.getName(), serialized.toString())
        .buildPayload();
  }
}
