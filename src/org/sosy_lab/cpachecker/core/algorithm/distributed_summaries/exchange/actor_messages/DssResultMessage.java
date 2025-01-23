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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;

public class DssResultMessage extends DssMessage {

  private final Result result;

  protected DssResultMessage(
      String pUniqueBlockId, int pTargetNodeNumber, DssMessagePayload pPayload) {
    this(pUniqueBlockId, pTargetNodeNumber, pPayload, null);
  }

  /**
   * Creates a new instance of this object.
   *
   * @deprecated for debug mode only. use {@link #DssResultMessage(String, int,
   *     DssMessagePayload)} instead.
   */
  @Deprecated
  protected DssResultMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DssMessagePayload pPayload,
      @Nullable Instant pTimeStamp) {
    super(MessageType.FOUND_RESULT, pUniqueBlockId, pTargetNodeNumber, pPayload, pTimeStamp);
    result = Result.valueOf((String) getPayload().get(DssMessagePayload.RESULT));
  }

  public Result getResult() {
    return result;
  }
}
