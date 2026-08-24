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
import javax.annotation.Nullable;

@JsonPropertyOrder({DssMessageFormat.HEADER_KEY, DssMessageFormat.STATUS_KEY, DssMessageFormat.CONTENT_KEY})
public record DssMessagePayload(
    @JsonProperty(DssMessageFormat.HEADER_KEY) ImmutableMap<String, String> header,
    @JsonProperty(DssMessageFormat.STATUS_KEY) @Nullable DssStatusPayload status,
    @JsonProperty(DssMessageFormat.CONTENT_KEY) ImmutableMap<String, String> content
    ) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public DssMessagePayload(ImmutableMap<String, String> header, DssStatusPayload status, ImmutableMap<String, String> content) {
    this.header = requireHeader(header);
    this.status = status;
    this.content = requireContent(content);
  }

  public DssMessagePayload(ImmutableMap<String, String> pHeader, ImmutableMap<String, String> pContent) {
    this(pHeader, null, pContent);
  }

  @JsonCreator
  DssMessagePayload(
      @JsonProperty(DssMessageFormat.HEADER_KEY) Map<String, String> pHeader,
      @JsonProperty(DssMessageFormat.STATUS_KEY) DssStatusPayload pStatus,
      @JsonProperty(DssMessageFormat.CONTENT_KEY) Map<String, String> pContent) {
    this(
        ImmutableMap.copyOf(requireHeader(pHeader)),
        pStatus,
        ImmutableMap.copyOf(requireContent(pContent))
    );
  }

  public static DssMessagePayload fromJson(Path pJson) throws IOException {
    return OBJECT_MAPPER.readValue(pJson.toFile(), DssMessagePayload.class);
  }

  public ImmutableMap<String, ImmutableMap<String, String>> asLegacyMap() {
    ImmutableMap.Builder<String, String> legacyContent = ImmutableMap.builder();
    if (status != null) {
      legacyContent.putAll(status.asLegacyContent());
    }
    legacyContent.putAll(content);
    return ImmutableMap.of(
        DssMessageFormat.HEADER_KEY, header,
        DssMessageFormat.CONTENT_KEY, legacyContent.buildKeepingLast()
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
