// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DssFunctionPointerEntryPayload.TargetKind;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;

public class SerializeFunctionPointerStateOperator implements SerializeOperator {

  @Override
  public DssFunctionPointerStatePayload serialize(AbstractState pState) {
    FunctionPointerState state = (FunctionPointerState) pState;
    FunctionPointerState.Builder builder = state.createBuilder();
    ImmutableList.Builder<DssFunctionPointerEntryPayload> entries =
        ImmutableList.builderWithExpectedSize(builder.getValues().size());

    for (String value : builder.getValues()) {
      if (FunctionPointerState.InvalidTarget.getInstance().equals(builder.getTarget(value))) {
        entries.add(new DssFunctionPointerEntryPayload(value, TargetKind.INVALID, null));
      } else if (FunctionPointerState.NullTarget.getInstance().equals(builder.getTarget(value))) {
        entries.add(new DssFunctionPointerEntryPayload(value, TargetKind.NULL, null));
      } else if (FunctionPointerState.UnknownTarget.getInstance()
          .equals(builder.getTarget(value))) {
        entries.add(new DssFunctionPointerEntryPayload(value, TargetKind.UNKNOWN, null));
      } else {
        NamedFunctionTarget namedTarget = (NamedFunctionTarget) builder.getTarget(value);
        entries.add(
            new DssFunctionPointerEntryPayload(
                value, TargetKind.NAMED, namedTarget.getFunctionName()));
      }
    }

    return new DssFunctionPointerStatePayload(entries.build());
  }
}
