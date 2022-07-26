// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import java.time.Instant;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;

public class ErrorMessage extends ActorMessage {

  private final String errorMessage;

  protected ErrorMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Payload pPayload, Instant pTimeStamp) {
    super(MessageType.ERROR, pUniqueBlockId, pTargetNodeNumber, pPayload, pTimeStamp);
    errorMessage =
        getPayload()
            .getOrDefault(Payload.EXCEPTION, "Error message received without exception message.");
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  protected ActorMessage replacePayload(Payload pPayload) {
    return new ErrorMessage(getUniqueBlockId(), getTargetNodeNumber(), pPayload, getTimestamp());
  }
}
