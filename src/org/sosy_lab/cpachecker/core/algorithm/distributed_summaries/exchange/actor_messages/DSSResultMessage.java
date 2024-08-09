// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import java.time.Instant;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSMessagePayload;

public class DSSResultMessage extends DSSMessage {

  private final Result result;

  protected DSSResultMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DSSMessagePayload pPayload,
      Instant pTimeStamp) {
    super(MessageType.FOUND_RESULT, pUniqueBlockId, pTargetNodeNumber, pPayload, pTimeStamp);
    result = Result.valueOf((String) getPayload().get(DSSMessagePayload.RESULT));
  }

  public Result getResult() {
    return result;
  }
}
