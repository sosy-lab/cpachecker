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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;

public class BlockSummaryResultMessage extends BlockSummaryMessage {

  private final Result result;
  private final Set<String> visited;

  protected BlockSummaryResultMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Payload pPayload, Instant pTimeStamp) {
    super(MessageType.FOUND_RESULT, pUniqueBlockId, pTargetNodeNumber, pPayload, pTimeStamp);
    result = Result.valueOf((String) getPayload().get(Payload.RESULT));
    visited = extractVisited();
  }

  public Result getResult() {
    return result;
  }

  public Set<String> getVisited() {
    return visited;
  }

  @Override
  protected BlockSummaryMessage replacePayload(Payload pPayload) {
    return new BlockSummaryResultMessage(
        getUniqueBlockId(), getTargetNodeNumber(), pPayload, getTimestamp());
  }
}
