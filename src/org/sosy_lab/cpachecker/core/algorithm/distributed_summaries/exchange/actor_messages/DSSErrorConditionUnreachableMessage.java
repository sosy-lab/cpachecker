// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import java.time.Instant;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSMessagePayload;

public class DSSErrorConditionUnreachableMessage extends DSSMessage {

  private final String reason;

  protected DSSErrorConditionUnreachableMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DSSMessagePayload pPayload,
      Instant pTimeStamp) {
    super(
        MessageType.ERROR_CONDITION_UNREACHABLE,
        pUniqueBlockId,
        pTargetNodeNumber,
        pPayload,
        pTimeStamp);
    reason = (String) getPayload().getOrDefault(DSSMessagePayload.REASON, "");
  }

  public String getReason() {
    return reason;
  }
}
