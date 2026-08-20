// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

@JsonPropertyOrder({DssMessage.DSS_MESSAGE_HEADER_ID, DssMessage.DSS_MESSAGE_CONTENT_ID})
public record DssMessageProxy(
    @JsonProperty(DssMessage.DSS_MESSAGE_HEADER_ID) ImmutableMap<String, String> header,
    @JsonProperty(DssMessage.DSS_MESSAGE_CONTENT_ID) ImmutableMap<String, String> content
    ) {

  @JsonCreator
  DssMessageProxy(
      @JsonProperty(DssMessage.DSS_MESSAGE_HEADER_ID) Map<String, String> pHeader,
      @JsonProperty(DssMessage.DSS_MESSAGE_CONTENT_ID) Map<String, String> pContent) {
    this(
        ImmutableMap.copyOf(
            Preconditions.checkNotNull(pHeader, "Message JSON does not contain header")
        ),
        ImmutableMap.copyOf(
            Preconditions.checkNotNull(pContent, "Message JSON does not contain content")
        )
    );
  }

  public ImmutableMap<String, ImmutableMap<String, String>> asLegacyMap() {
    return ImmutableMap.of(
        DssMessage.DSS_MESSAGE_HEADER_ID, header,
        DssMessage.DSS_MESSAGE_CONTENT_ID, content
    );
  }
}
