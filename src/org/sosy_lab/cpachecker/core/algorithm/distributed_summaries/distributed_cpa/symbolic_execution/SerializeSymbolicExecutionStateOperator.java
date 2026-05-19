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
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.symbolicExecution.SymbolicExecutionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;

public class SerializeSymbolicExecutionStateOperator implements SerializeOperator {
  public static final String CONSTRAINTS_KEY = "constraints";
  public static final String VALUE_KEY = "value";

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    SymbolicExecutionState state = simplifyState((SymbolicExecutionState) pState);
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

  private SymbolicExecutionState simplifyState(SymbolicExecutionState pState) {
    Map<Constraint, Set<SymbolicIdentifier>> constraintIDs =
        pState.constraintsState().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    constraint -> constraint,
                    constraint ->
                        new HashSet<>(SymbolicValues.getContainedSymbolicIdentifiers(constraint))));
    Set<Constraint> relevant = new HashSet<>();
    Set<Constraint> irrelevant = new HashSet<>(pState.constraintsState());
    Set<SymbolicIdentifier> oldIDs = new HashSet<>();
    Set<SymbolicIdentifier> newIDs =
        new HashSet<>(getIdentifiersFromValueState(pState.valueAnalysisState()));

    while (!oldIDs.containsAll(newIDs)) {
      oldIDs.clear();
      oldIDs.addAll(newIDs);
      for (Constraint constraint : irrelevant) {
        if (!Collections.disjoint(oldIDs, constraintIDs.get(constraint))) {
          relevant.add(constraint);
          newIDs.addAll(constraintIDs.get(constraint));
        }
      }
      irrelevant.removeAll(relevant);
    }
    return new SymbolicExecutionState(pState.valueAnalysisState(), new ConstraintsState(relevant));
  }

  private Set<SymbolicIdentifier> getIdentifiersFromValueState(ValueAnalysisState pValueState) {

    return pValueState.getConstants().stream()
        .filter(value -> value.getValue().getValue() instanceof SymbolicValue)
        .map(
            value ->
                SymbolicValues.getContainedSymbolicIdentifiers(
                    (SymbolicValue) value.getValue().getValue()))
        .flatMap(Collection::stream)
        .collect(ImmutableSet.toImmutableSet());
  }
}
