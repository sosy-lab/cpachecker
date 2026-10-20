// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.base.Preconditions;
import java.time.Instant;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

public final class DssResultMessage implements DssMessage {
  private final String senderId;
  private final Instant timestamp;
  private final Result result;

  public DssResultMessage(String pSenderId, Result pResult) {
    this(pSenderId, Instant.now(), pResult);
  }

  DssResultMessage(String pSenderId, Instant pTimestamp, Result pResult) {
    senderId = Preconditions.checkNotNull(pSenderId);
    timestamp = Preconditions.checkNotNull(pTimestamp);
    result = Preconditions.checkNotNull(pResult);
  }

  @Override
  public DssMessageType getType() {
    return DssMessageType.RESULT;
  }

  @Override
  public String getSenderId() {
    return senderId;
  }

  @Override
  public Instant getTimestamp() {
    return timestamp;
  }

  public Result getResult() {
    return result;
  }

  @Override
  public DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier) {
    return new DssResultPayload(
        DssHeaderPayload.forMessage(senderId, timestamp, pIdentifier), result);
  }
}
