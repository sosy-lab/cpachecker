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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  DssMessageKeys.HEADER,
  DssMessageKeys.STATUS,
  DssMessageKeys.STATES,
  DssMessageKeys.CONTENT
})
public record DssMessagePayload(
    @JsonProperty(DssMessageKeys.HEADER) DssHeaderPayload header,
    @JsonProperty(DssMessageKeys.STATUS) @Nullable DssStatusPayload status,
    @JsonProperty(DssMessageKeys.STATES)
        ImmutableList<ImmutableMap<String, DssStateAndPrecisionPayload>> states,
    @JsonProperty(DssMessageKeys.CONTENT) ImmutableMap<String, String> content) {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new DssPayloadModule());

  public DssMessagePayload(
      DssHeaderPayload header,
      @Nullable DssStatusPayload status,
      ImmutableList<ImmutableMap<String, DssStateAndPrecisionPayload>> states,
      ImmutableMap<String, String> content) {
    this.header = requireHeader(header);
    this.status = status;
    this.states = copyStates(states);
    this.content = requireContent(content);
  }

  @JsonCreator
  DssMessagePayload(
      @JsonProperty(DssMessageKeys.HEADER) DssHeaderPayload pHeader,
      @JsonProperty(DssMessageKeys.STATUS) @Nullable DssStatusPayload pStatus,
      @JsonProperty(DssMessageKeys.STATES)
          @Nullable List<? extends Map<String, DssStateAndPrecisionPayload>> pStates,
      @JsonProperty(DssMessageKeys.CONTENT) Map<String, String> pContent) {
    this(
        requireHeader(pHeader),
        pStatus,
        copyStates(pStates),
        ImmutableMap.copyOf(requireContent(pContent)));
  }

  public static DssMessagePayload fromJson(Path pJson) throws IOException {
    return OBJECT_MAPPER.readValue(pJson.toFile(), DssMessagePayload.class);
  }

  public DssMessagePayload withoutTimestamp() {
    return new DssMessagePayload(header.withoutTimestamp(), status, states, content);
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

  private static ImmutableList<ImmutableMap<String, DssStateAndPrecisionPayload>> copyStates(
      @Nullable List<? extends Map<String, DssStateAndPrecisionPayload>> pStates) {
    if (pStates == null) {
      return ImmutableList.of();
    }
    return pStates.stream().map(ImmutableMap::copyOf).collect(ImmutableList.toImmutableList());
  }
}
