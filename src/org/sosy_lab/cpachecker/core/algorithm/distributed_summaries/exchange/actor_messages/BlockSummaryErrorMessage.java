// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import java.time.Instant;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;

public class BlockSummaryErrorMessage extends BlockSummaryMessage {

  private final String errorMessage;

  protected BlockSummaryErrorMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BlockSummaryMessagePayload pPayload,
      Instant pTimeStamp) {
    super(MessageType.ERROR, pUniqueBlockId, pTargetNodeNumber, pPayload, pTimeStamp);
    if (!getPayload().containsKey(BlockSummaryMessagePayload.EXCEPTION)) {
      throw new AssertionError(
          "ErrorMessages are always required to contain the key " + MessageType.ERROR);
    }
    errorMessage = (String) getPayload().get(BlockSummaryMessagePayload.EXCEPTION);
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  protected BlockSummaryMessage replacePayload(BlockSummaryMessagePayload pPayload) {
    return new BlockSummaryErrorMessage(
        getUniqueBlockId(), getTargetNodeNumber(), pPayload, getTimestamp());
  }
}
