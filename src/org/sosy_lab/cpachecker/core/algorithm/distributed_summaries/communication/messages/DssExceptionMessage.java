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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssExceptionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssHeaderPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssMessagePayload;

/**
 * Message for exceptions that occur during distributed summary computation. The content contains a
 * single key-value pair with the key "exception" and the value being the exception message.
 */
public final class DssExceptionMessage implements DssMessage {
  private final String senderId;
  private final Instant timestamp;
  private final String exceptionMessage;

  public DssExceptionMessage(String pSenderId, String pExceptionMessage) {
    this(pSenderId, Instant.now(), pExceptionMessage);
  }

  DssExceptionMessage(String pSenderId, Instant pTimestamp, String pExceptionMessage) {
    senderId = Preconditions.checkNotNull(pSenderId);
    timestamp = Preconditions.checkNotNull(pTimestamp);
    exceptionMessage = Preconditions.checkNotNull(pExceptionMessage);
  }

  @Override
  public String getSenderId() {
    return senderId;
  }

  @Override
  public Instant getTimestamp() {
    return timestamp;
  }

  public String getExceptionMessage() {
    return exceptionMessage;
  }

  @Override
  public DssMessageType getType() {
    return DssMessageType.EXCEPTION;
  }

  @Override
  public DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier) {
    return new DssExceptionPayload(
        DssHeaderPayload.forMessage(senderId, timestamp, pIdentifier), exceptionMessage);
  }
}
