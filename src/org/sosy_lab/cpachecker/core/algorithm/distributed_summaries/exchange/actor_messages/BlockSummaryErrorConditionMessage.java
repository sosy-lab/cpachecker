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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.SerializeUtil;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

public class BlockSummaryErrorConditionMessage extends BlockSummaryMessage {

  private final Set<String> visited;
  private final boolean first;

  BlockSummaryErrorConditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BlockSummaryMessagePayload pPayload,
      Instant pInstant) {
    super(MessageType.ERROR_CONDITION, pUniqueBlockId, pTargetNodeNumber, pPayload, pInstant);
    visited = extractVisited();
    first = extractFlag(BlockSummaryMessagePayload.FIRST, false);
  }

  public SSAMap getSSAMap() {
    if (getPayload().containsKey(BlockSummaryMessagePayload.SSA)) {
      return SerializeUtil.deserialize(
          (String) Objects.requireNonNull(getPayload().get(BlockSummaryMessagePayload.SSA)),
          SSAMap.class);
    }
    return SSAMap.emptySSAMap();
  }

  public PointerTargetSet getPointerTargetSet() {
    if (getPayload().containsKey(BlockSummaryMessagePayload.PTS)) {
      return SerializeUtil.deserialize(
          (String) Objects.requireNonNull(getPayload().get(BlockSummaryMessagePayload.PTS)),
          PointerTargetSet.class);
    }
    return PointerTargetSet.emptyPointerTargetSet();
  }

  public Set<String> visitedBlockIds() {
    return visited;
  }

  public boolean isFirst() {
    return first;
  }

  @Override
  protected BlockSummaryMessage replacePayload(BlockSummaryMessagePayload pPayload) {
    return new BlockSummaryErrorConditionMessage(
        getUniqueBlockId(), getTargetNodeNumber(), pPayload, getTimestamp());
  }
}
