// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.SerializePredicateStateOperator.READABLE_KEY;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

public class SerializeValueAnalysisStateOperator implements SerializeOperator {
  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    ValueAnalysisState state = (ValueAnalysisState) pState;
    String serializedState;
    try {
      serializedState = DssSerializeObjectUtil.serialize(state);
    } catch (IOException e) {
      throw new AssertionError("Unable to serialize value analysis state " + state.toString());
    }
    return ContentBuilder.builder()
        .pushLevel(ValueAnalysisState.class.getName())
        .put(STATE_KEY, serializedState)
        .put(READABLE_KEY, state.getConstants().toString())
        .build();
  }
}
