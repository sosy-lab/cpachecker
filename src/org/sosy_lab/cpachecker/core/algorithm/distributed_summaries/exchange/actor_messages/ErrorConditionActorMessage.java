// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import java.time.Instant;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;

public class ErrorConditionActorMessage extends ActorMessage {

  private final Set<String> visited;
  private final boolean first;

  ErrorConditionActorMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Payload pPayload, Instant pInstant) {
    super(MessageType.ERROR_CONDITION, pUniqueBlockId, pTargetNodeNumber, pPayload, pInstant);
    visited = extractVisited();
    first = extractFlag(Payload.FIRST, false);
  }

  public Set<String> visitedBlockIds() {
    return visited;
  }

  public boolean isFirst() {
    return first;
  }

  @Override
  protected ActorMessage replacePayload(Payload pPayload) {
    return new ErrorConditionActorMessage(
        getUniqueBlockId(), getTargetNodeNumber(), pPayload, getTimestamp());
  }
}
