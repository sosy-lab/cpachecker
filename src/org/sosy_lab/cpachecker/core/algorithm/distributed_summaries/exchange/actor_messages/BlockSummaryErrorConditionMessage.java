// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.SerializeSSAMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

public class BlockSummaryErrorConditionMessage extends BlockSummaryMessage {

  private final Set<String> visited;
  private final boolean first;

  BlockSummaryErrorConditionMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Payload pPayload, Instant pInstant) {
    super(MessageType.ERROR_CONDITION, pUniqueBlockId, pTargetNodeNumber, pPayload, pInstant);
    visited = extractVisited();
    first = extractFlag(Payload.FIRST, false);
  }

  public SSAMap getSSAMap() {
    if (getPayload().containsKey(Payload.SSA)) {
      return SerializeSSAMap.deserialize(
          (String) Objects.requireNonNull(getPayload().get(Payload.SSA)));
    }
    return SSAMap.emptySSAMap();
  }

  public Set<String> visitedBlockIds() {
    return visited;
  }

  public boolean isFirst() {
    return first;
  }

  @Override
  protected BlockSummaryMessage replacePayload(Payload pPayload) {
    return new BlockSummaryErrorConditionMessage(
        getUniqueBlockId(), getTargetNodeNumber(), pPayload, getTimestamp());
  }
}
