// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

public class SerializeLocationStateOperator implements SerializeOperator {

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    LocationState locationState = (LocationState) pState;
    return ContentBuilder.builder()
        .pushLevel(LocationState.class.getName())
        .put(STATE_KEY, Integer.toString(locationState.getLocationNode().getNodeNumber()))
        .build();
  }
}
