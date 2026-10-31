// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  DssHeaderPayload.SENDER_ID,
  DssHeaderPayload.TIMESTAMP,
  DssHeaderPayload.IDENTIFIER
})
public record DssHeaderPayload(
    @JsonProperty(value = SENDER_ID, required = true) String senderId,
    @JsonProperty(TIMESTAMP) @Nullable String timestamp,
    @JsonProperty(value = IDENTIFIER, required = true) int identifier) {

  public static final String HEADER = "header";
  public static final String SENDER_ID = "senderId";
  public static final String TIMESTAMP = "timestamp";
  public static final String IDENTIFIER = "identifier";

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
