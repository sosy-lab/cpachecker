// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import java.time.Instant;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;

public class BlockSummaryErrorConditionUnreachableMessage extends BlockSummaryMessage {

  private final String reason;

  protected BlockSummaryErrorConditionUnreachableMessage(
      String pUniqueBlockId, int pTargetNodeNumber, BlockSummaryMessagePayload pPayload) {
    this(pUniqueBlockId, pTargetNodeNumber, pPayload, null);
  }

  /**
   * Creates a new instance of this object.
   *
   * @deprecated for debug mode only. use {@link
   *     #BlockSummaryErrorConditionUnreachableMessage(String, int, BlockSummaryMessagePayload)}
   *     instead.
   */
  @Deprecated
  protected BlockSummaryErrorConditionUnreachableMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BlockSummaryMessagePayload pPayload,
      @Nullable Instant pTimeStamp) {
    super(
        MessageType.ERROR_CONDITION_UNREACHABLE,
        pUniqueBlockId,
        pTargetNodeNumber,
        pPayload,
        pTimeStamp);
    reason = (String) getPayload().getOrDefault(BlockSummaryMessagePayload.REASON, "");
  }

  public String getReason() {
    return reason;
  }
}
