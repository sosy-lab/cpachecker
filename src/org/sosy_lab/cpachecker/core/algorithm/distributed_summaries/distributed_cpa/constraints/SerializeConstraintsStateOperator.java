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
import java.util.HashSet;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;

public class SerializeConstraintsStateOperator implements SerializeOperator {

  public static final String CONSTRAINTS_KEY = "constraints";

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    ConstraintsState state = (ConstraintsState) pState;
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
}
