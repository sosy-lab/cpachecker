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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@JsonPropertyOrder({DssMessage.DSS_MESSAGE_HEADER_ID, DssMessage.DSS_MESSAGE_CONTENT_ID})
public record DssMessagePayload(
    @JsonProperty(DssMessage.DSS_MESSAGE_HEADER_ID) ImmutableMap<String, String> header,
    @JsonProperty(DssMessage.DSS_MESSAGE_CONTENT_ID) ImmutableMap<String, String> content
    ) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public DssMessagePayload(ImmutableMap<String, String> header, ImmutableMap<String, String> content) {
    this.header = requireHeader(header);
    this.content = requireContent(content);
  }

  @JsonCreator
  DssMessagePayload(
      @JsonProperty(DssMessage.DSS_MESSAGE_HEADER_ID) Map<String, String> pHeader,
      @JsonProperty(DssMessage.DSS_MESSAGE_CONTENT_ID) Map<String, String> pContent) {
    this(
        ImmutableMap.copyOf(requireHeader(pHeader)),
        ImmutableMap.copyOf(requireContent(pContent))
    );
  }

  public static DssMessagePayload fromJson(Path pJson) throws IOException {
    return OBJECT_MAPPER.readValue(pJson.toFile(), DssMessagePayload.class);
  }

  public ImmutableMap<String, ImmutableMap<String, String>> asLegacyMap() {
    return ImmutableMap.of(
        DssMessage.DSS_MESSAGE_HEADER_ID, header,
        DssMessage.DSS_MESSAGE_CONTENT_ID, content
    );
  }

  public void writeJson(Path pPath) throws IOException {
    OBJECT_MAPPER.writeValue(pPath.toFile(), this);
  }

  private static <T extends Map<String, String>> T requireHeader(T pHeader) {
    return Preconditions.checkNotNull(pHeader, "Message JSON does not contain header");
  }

  private static <T extends Map<String, String>> T requireContent(T pContent) {
    return Preconditions.checkNotNull(pContent, "Message JSON does not contain content");
  }
}
