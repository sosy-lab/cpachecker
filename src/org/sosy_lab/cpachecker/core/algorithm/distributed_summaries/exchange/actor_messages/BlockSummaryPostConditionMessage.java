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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.SerializeUtil;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

public class BlockSummaryPostConditionMessage extends BlockSummaryMessage {

  private final boolean fullPath;
  private final boolean reachable;
  private final Set<String> visited;

  BlockSummaryPostConditionMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Payload pPayload, Instant pInstant) {
    super(MessageType.BLOCK_POSTCONDITION, pUniqueBlockId, pTargetNodeNumber, pPayload, pInstant);
    fullPath = extractFlag(Payload.FULL_PATH, false);
    reachable = extractFlag(Payload.REACHABLE, true);
    visited = extractVisited();
  }

  public boolean representsFullPath() {
    return fullPath;
  }

  public Set<String> visitedBlockIds() {
    return visited;
  }

  public SSAMap getSSAMap() {
    if (getPayload().containsKey(Payload.SSA)) {
      return SerializeUtil.deserialize(
          (String) Objects.requireNonNull(getPayload().get(Payload.SSA)), SSAMap.class);
    }
    return SSAMap.emptySSAMap();
  }

  public PointerTargetSet getPointerTargetSet() {
    if (getPayload().containsKey(Payload.PTS)) {
      return SerializeUtil.deserialize(
          (String) Objects.requireNonNull(getPayload().get(Payload.PTS)), PointerTargetSet.class);
    }
    return PointerTargetSet.emptyPointerTargetSet();
  }

  public boolean isReachable() {
    return reachable;
  }

  @Override
  protected BlockSummaryMessage replacePayload(Payload pPayload) {
    return new BlockSummaryPostConditionMessage(
        getUniqueBlockId(), getTargetNodeNumber(), pPayload, getTimestamp());
  }
}
