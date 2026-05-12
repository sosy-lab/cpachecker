// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.SerializePredicateStateOperator.READABLE_KEY;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;

public class SerializeConstraintsStateOperator implements SerializeOperator {

  public static final String CONSTRAINTS_KEY = "constraints";
  private final BlockNode blocknode;
  public static ConcurrentMap<String, Set<SymbolicIdentifier>> assignedIDs =
      new ConcurrentHashMap<>();

  public SerializeConstraintsStateOperator(BlockNode pBlocknode) {
    blocknode = pBlocknode;
  }

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    ConstraintsState state = simplifyState((ConstraintsState) pState);
    String serializedConstraints;

    try {
      serializedConstraints = DssSerializeObjectUtil.serialize(new HashSet<>(state));
    } catch (IOException e) {
      throw new AssertionError("Unable to serialize constraints " + state);
    }

    return ContentBuilder.builder()
        .pushLevel(ConstraintsState.class.getName())
        .put(CONSTRAINTS_KEY, serializedConstraints)
        .put(READABLE_KEY, new HashSet<>(state).toString())
        .build();
  }

  private ConstraintsState simplifyState(ConstraintsState pConstraintsState) {
    Map<Constraint, Set<SymbolicIdentifier>> constraintIDs =
        pConstraintsState.stream()
            .collect(
                Collectors.toMap(
                    constraint -> constraint,
                    constraint ->
                        new HashSet<>(SymbolicValues.getContainedSymbolicIdentifiers(constraint))));
    Set<Constraint> relevant = new HashSet<>();
    Set<Constraint> irrelevant = new HashSet<>(pConstraintsState);
    Set<SymbolicIdentifier> oldIDs = new HashSet<>();
    Set<SymbolicIdentifier> newIDs = new HashSet<>(assignedIDs.get(blocknode.getId()));

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
    return new ConstraintsState(relevant);
  }
}
