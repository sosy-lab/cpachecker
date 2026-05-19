// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.symbolic_execution;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.SerializePredicateStateOperator.READABLE_KEY;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashSet;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.symbolicExecution.SymbolicExecutionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

public class SerializeSymbolicExecutionStateOperator implements SerializeOperator {
  public static final String CONSTRAINTS_KEY = "constraints";
  public static final String VALUE_KEY = "value";

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    SymbolicExecutionState state = (SymbolicExecutionState) pState;
    ValueAnalysisState valueState = state.valueAnalysisState();
    ConstraintsState constraintsState = state.constraintsState();

    String serializedValueState;
    try {
      serializedValueState = DssSerializeObjectUtil.serialize(valueState);
    } catch (IOException e) {
      throw new AssertionError("Unable to serialize value analysis state " + valueState);
    }

    String serializedConstraints;

    try {
      serializedConstraints = DssSerializeObjectUtil.serialize(new HashSet<>(constraintsState));
    } catch (IOException e) {
      throw new AssertionError("Unable to serialize constraints " + state);
    }
    return ContentBuilder.builder()
        .pushLevel(SymbolicExecutionState.class.getName())
        .put(VALUE_KEY, serializedValueState)
        .put(CONSTRAINTS_KEY, serializedConstraints)
        .put(
            READABLE_KEY,
            "Value state: "
                + state.valueAnalysisState().getConstants()
                + "\n"
                + "Constraints: "
                + new HashSet<>(state.constraintsState()))
        .popLevel()
        .build();
  }
}
