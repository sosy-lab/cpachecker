// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location;

import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.location.CachedLocationStateProvider;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

public class DeserializeLocationState implements DeserializeOperator {

  private final CachedLocationStateProvider locationStateFactory;
  private final Map<Integer, CFANode> availableNodes;

  public DeserializeLocationState(
      CachedLocationStateProvider pLocationStateFactory, Map<Integer, CFANode> pAvailableNodes) {
    locationStateFactory = pLocationStateFactory;
    availableNodes = pAvailableNodes;
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    int nodeNumber =
        Integer.parseInt(pMessage.getAbstractStateContent(LocationState.class).get(STATE_KEY));
    return locationStateFactory.getState(availableNodes.get(nodeNumber));
  }
}
