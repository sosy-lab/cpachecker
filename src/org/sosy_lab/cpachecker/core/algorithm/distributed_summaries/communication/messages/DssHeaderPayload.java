// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({DssMessageKeys.SENDER_ID, DssMessageKeys.TIMESTAMP, DssMessageKeys.IDENTIFIER})
public record DssHeaderPayload(
    @JsonProperty(value = DssMessageKeys.SENDER_ID, required = true) String senderId,
    @JsonProperty(DssMessageKeys.TIMESTAMP) @Nullable String timestamp,
    @JsonProperty(value = DssMessageKeys.IDENTIFIER, required = true) int identifier) {

  private static final long nanoConversionFactor = 1_000_000_000L;

  @JsonCreator
  public DssHeaderPayload {}

  public DssHeaderPayload withoutTimestamp() {
    return new DssHeaderPayload(senderId, null, identifier);
  }

  public static DssHeaderPayload forMessage(String pSenderId, Instant pTimestamp, int pIdentifier) {
    return new DssHeaderPayload(
        pSenderId,
        Long.toString(pTimestamp.getEpochSecond() * nanoConversionFactor + pTimestamp.getNano()),
        pIdentifier);
  }

  public Optional<Instant> timestampAsInstant() {
    if (timestamp == null) {
      return Optional.empty();
    }
    long epochNanos = Long.parseLong(timestamp);
    long epochSeconds = Math.floorDiv(epochNanos, nanoConversionFactor);
    int nanosAdjustment = (int) Math.floorMod(epochNanos, nanoConversionFactor);

    return Optional.of(Instant.ofEpochSecond(epochSeconds, nanosAdjustment));
  }
}
