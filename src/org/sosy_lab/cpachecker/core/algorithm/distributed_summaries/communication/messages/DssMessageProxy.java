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
import java.util.Objects;

@JsonPropertyOrder({DssMessage.DSS_MESSAGE_HEADER_ID, DssMessage.DSS_MESSAGE_CONTENT_ID})
public record DssMessageProxy(
    @JsonProperty(DssMessage.DSS_MESSAGE_HEADER_ID) ImmutableMap<String, String> header,
    @JsonProperty(DssMessage.DSS_MESSAGE_CONTENT_ID) ImmutableMap<String, String> content
    ) {

  public DssMessageProxy(ImmutableMap<String, String> header, ImmutableMap<String, String> content) {
    this.header = requireHeader(header);
    this.content = requireContent(content);
  }

  @JsonCreator
  DssMessageProxy(
      @JsonProperty(DssMessage.DSS_MESSAGE_HEADER_ID) Map<String, String> pHeader,
      @JsonProperty(DssMessage.DSS_MESSAGE_CONTENT_ID) Map<String, String> pContent) {
    this(
        ImmutableMap.copyOf(requireHeader(pHeader)),
        ImmutableMap.copyOf(requireContent(pContent))
    );
  }

  public static DssMessageProxy fromJson(Path pJson) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(pJson.toFile(), DssMessageProxy.class);
  }

  public static DssMessageProxy fromLegacyMap(ImmutableMap<String, ImmutableMap<String, String>> pJson) {
    ImmutableMap<String, String> headerMap =
        Objects.requireNonNull(
            pJson.get(DssMessage.DSS_MESSAGE_HEADER_ID), "Message JSON does not contain header: " + pJson);
    ImmutableMap<String, String> contentMap =
        Objects.requireNonNull(
            pJson.get(DssMessage.DSS_MESSAGE_CONTENT_ID), "Message JSON does not contain content: " + pJson);

    return new DssMessageProxy(headerMap, contentMap);
  }

  public ImmutableMap<String, ImmutableMap<String, String>> asLegacyMap() {
    return ImmutableMap.of(
        DssMessage.DSS_MESSAGE_HEADER_ID, header,
        DssMessage.DSS_MESSAGE_CONTENT_ID, content
    );
  }

  private static <T extends Map<String, String>> T requireHeader(T pHeader) {
    return Preconditions.checkNotNull(pHeader, "Message JSON does not contain header");
  }

  private static <T extends Map<String, String>> T requireContent(T pContent) {
    return Preconditions.checkNotNull(pContent, "Message JSON does not contain content");
  }
}
