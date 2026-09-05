// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import java.time.Instant;

public interface DssMessage {
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
