// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableMap;

@JsonPropertyOrder({DssMessage.DSS_MESSAGE_HEADER_ID, DssMessage.DSS_MESSAGE_CONTENT_ID})
public record DssMessagePayload(
    @JsonProperty(DssMessage.DSS_MESSAGE_HEADER_ID) ImmutableMap<String, String> header,
    @JsonProperty(DssMessage.DSS_MESSAGE_CONTENT_ID) ImmutableMap<String, String> content
    ) {
  public ImmutableMap<String, ImmutableMap<String, String>> asLegacyMap() {
    return ImmutableMap.of(
        DssMessage.DSS_MESSAGE_HEADER_ID, header,
        DssMessage.DSS_MESSAGE_CONTENT_ID, content
    );
  }
}
