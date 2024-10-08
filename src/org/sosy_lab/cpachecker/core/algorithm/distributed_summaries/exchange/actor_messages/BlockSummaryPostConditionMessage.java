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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummarySerializeUtil;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

public class BlockSummaryPostConditionMessage extends BlockSummaryMessage {
  private final boolean reachable;

  BlockSummaryPostConditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BlockSummaryMessagePayload pPayload,
      Instant pInstant) {
    super(MessageType.BLOCK_POSTCONDITION, pUniqueBlockId, pTargetNodeNumber, pPayload, pInstant);
    reachable = extractFlag(BlockSummaryMessagePayload.REACHABLE, true);
  }

  public SSAMap getSSAMap() {
    if (getPayload().containsKey(BlockSummaryMessagePayload.SSA)) {
      return BlockSummarySerializeUtil.deserialize(
          (String) Objects.requireNonNull(getPayload().get(BlockSummaryMessagePayload.SSA)),
          SSAMap.class);
    }
    return SSAMap.emptySSAMap();
  }

  public String getAbstractionStrategy() {
    return (String) Objects.requireNonNull(getPayload().get(BlockSummaryMessagePayload.STRATEGY));
  }

  public PointerTargetSet getPointerTargetSet() {
    if (getPayload().containsKey(BlockSummaryMessagePayload.PTS)) {
      return BlockSummarySerializeUtil.deserialize(
          (String) Objects.requireNonNull(getPayload().get(BlockSummaryMessagePayload.PTS)),
          PointerTargetSet.class);
    }
    return PointerTargetSet.emptyPointerTargetSet();
  }

  public boolean isReachable() {
    return reachable;
  }
}
