// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import java.time.Instant;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssMessagePayloadModule;

/**
 * A distributed-summaries message exchanged between workers.
 *
 * <p>When introducing a new message type, add:
 *
 * <ul>
 *   <li>a dedicated {@link DssMessage} implementation;
 *   <li>a matching {@link DssMessagePayload};
 *   <li>the corresponding payload subtype registration in {@link DssMessagePayloadModule};
 *   <li>conversion logic in {@link DssMessageCodec};
 *   <li>construction logic in {@link DssMessageFactory}.
 * </ul>
 */
public sealed interface DssMessage
    permits DssExceptionMessage,
        DssMessageWithStates,
        DssMessageWithStatus,
        DssResultMessage,
        DssWitnessMessage {
  DssMessageType getType();

  String getSenderId();

  Instant getTimestamp();

  DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier);

  default DssMessagePayload asJsonPayload() {
    return asJsonPayloadWithIdentifier(0);
  }

  enum DssMessageType {
    POST_CONDITION,
    VIOLATION_CONDITION,
    EXCEPTION,
    RESULT,
    WITNESS
  }
}
