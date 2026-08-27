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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  DssMessageFormat.HEADER_KEY,
  DssMessageFormat.STATUS_KEY,
  DssMessageFormat.CONTENT_KEY
})
public record DssMessagePayload(
    @JsonProperty(DssMessageFormat.HEADER_KEY) DssHeaderPayload header,
    @JsonProperty(DssMessageFormat.STATUS_KEY) @Nullable DssStatusPayload status,
    @JsonProperty(DssMessageFormat.CONTENT_KEY) ImmutableMap<String, String> content) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public DssMessagePayload(
      DssHeaderPayload header,
      @Nullable DssStatusPayload status,
      ImmutableMap<String, String> content) {
    this.header = requireHeader(header);
    this.status = status;
    this.content = requireContent(content);
  }

  @JsonCreator
  DssMessagePayload(
      @JsonProperty(DssMessageFormat.HEADER_KEY) DssHeaderPayload pHeader,
      @JsonProperty(DssMessageFormat.STATUS_KEY) @Nullable DssStatusPayload pStatus,
      @JsonProperty(DssMessageFormat.CONTENT_KEY) Map<String, String> pContent) {
    this(requireHeader(pHeader), pStatus, ImmutableMap.copyOf(requireContent(pContent)));
  }

  public static DssMessagePayload fromJson(Path pJson) throws IOException {
    return OBJECT_MAPPER.readValue(pJson.toFile(), DssMessagePayload.class);
  }

  public DssMessagePayload withoutTimestamp() {
    return new DssMessagePayload(header.withoutTimestamp(), status, content);
  }

  public ImmutableMap<String, ImmutableMap<String, String>> asLegacyMap() {
    return ImmutableMap.of(
        DssMessageFormat.HEADER_KEY,
        header.asLegacyHeader(),
        DssMessageFormat.CONTENT_KEY,
        legacyContent());
  }

  public ImmutableMap<String, String> legacyContent() {
    ImmutableMap.Builder<String, String> legacyContent = ImmutableMap.builder();
    if (status != null) {
      legacyContent.putAll(status.asLegacyContent());
    }
    legacyContent.putAll(content);
    return legacyContent.buildKeepingLast();
  }

  public void writeJson(Path pPath) throws IOException {
    Path parent = pPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    OBJECT_MAPPER.writeValue(pPath.toFile(), this);
  }

  private static DssHeaderPayload requireHeader(DssHeaderPayload pHeader) {
    return Preconditions.checkNotNull(pHeader, "Message JSON does not contain header");
  }

  private static <T extends Map<String, String>> T requireContent(T pContent) {
    return Preconditions.checkNotNull(pContent, "Message JSON does not contain content");
  }
}
