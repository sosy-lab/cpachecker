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
import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  DssMessageFormat.SENDER_ID_KEY,
  DssMessageFormat.HEADER_TYPE_KEY,
  DssMessageFormat.HEADER_TIMESTAMP_KEY,
  DssMessageFormat.HEADER_IDENTIFIER_KEY
})
public record DssHeaderPayload(
    @JsonProperty(DssMessageFormat.SENDER_ID_KEY) String senderId,
    @JsonProperty(DssMessageFormat.HEADER_TYPE_KEY) DssMessageType messageType,
    @JsonProperty(DssMessageFormat.HEADER_TIMESTAMP_KEY) @Nullable String timestamp,
    @JsonProperty(DssMessageFormat.HEADER_IDENTIFIER_KEY) int identifier) {

  @JsonCreator
  public DssHeaderPayload {}

  public DssHeaderPayload withoutTimestamp() {
    return new DssHeaderPayload(senderId, messageType, null, identifier);
  }

  public ImmutableMap<String, String> asLegacyHeader() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    builder.put(DssMessageFormat.SENDER_ID_KEY, senderId);
    builder.put(DssMessageFormat.HEADER_TYPE_KEY, messageType.name());
    if (timestamp != null) {
      builder.put(DssMessageFormat.HEADER_TIMESTAMP_KEY, timestamp);
    }
    builder.put(DssMessageFormat.HEADER_IDENTIFIER_KEY, identifier + "");
    return builder.buildOrThrow();
  }
}
